package app.messagememory.retention

import android.net.Uri
import android.util.Log
import app.messagememory.data.db.ConversationDao
import app.messagememory.data.db.MediaDao
import app.messagememory.data.db.MessageDao
import app.messagememory.data.files.MediaStorage
import app.messagememory.diagnostics.DiagnosticsRepository
import java.io.File

/**
 * Idempotent, crash-safe rolling-expiry sweep (ARCHITECTURE.md §5). Safe to
 * call repeatedly, concurrently-with-itself is not expected (only ever
 * invoked from [CleanupWorker] or app-start, never overlapping in practice
 * since both go through the same single-flight guard in [CleanupWorker]'s
 * unique work name), and safe to interrupt at any point — a crash between
 * "file deleted" and "row deleted" just means the row is deleted on the
 * very next sweep instead, with no dangling reference in between because
 * file deletion always happens first.
 */
class CleanupEngine(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val mediaDao: MediaDao,
    private val mediaStorage: MediaStorage,
    private val diagnostics: DiagnosticsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun sweep(): SweepResult {
        val now = clock()
        var removedMessages = 0
        var removedMedia = 0
        var error: String? = null
        val touchedConversations = mutableSetOf<Long>()

        try {
            val expiredMedia = mediaDao.expired(now)
            for (media in expiredMedia) {
                touchedConversations += media.conversationId
                val uri = media.localUri
                if (uri != null) {
                    val stillReferenced = mediaDao.activeReferenceCount(uri, now) > 0
                    if (!stillReferenced) mediaStorage.delete(uri)
                }
                mediaDao.deleteById(media.id)
                removedMedia++
            }

            val expiredMessages = messageDao.expired(now)
            for (message in expiredMessages) {
                touchedConversations += message.conversationId
                messageDao.deleteById(message.id)
                removedMessages++
            }

            for (conversationId in touchedConversations) {
                conversationDao.recomputeAggregates(conversationId)
                conversationDao.deleteIfEmpty(conversationId)
            }

            sweepOrphanFiles()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup sweep failed", e)
            error = e.message ?: "Unknown cleanup failure"
        }

        diagnostics.recordCleanup(now, removedMessages, removedMedia, error)
        return SweepResult(removedMessages, removedMedia, error)
    }

    private suspend fun sweepOrphanFiles() {
        val referenced = mediaDao.allLocalUris().mapNotNullTo(mutableSetOf()) { uriString ->
            runCatching { Uri.parse(uriString).path }.getOrNull()
        }
        mediaStorage.listAllFiles().forEach { file: File ->
            val isTemp = file.extension == "tmp"
            val isReferenced = file.absolutePath in referenced
            if (isTemp || !isReferenced) {
                mediaStorage.deleteOrphan(file)
            }
        }
    }

    data class SweepResult(val removedMessages: Int, val removedMedia: Int, val error: String?)

    private companion object {
        const val TAG = "CleanupEngine"
    }
}
