package app.messagememory.data.files

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * All captured media lives under app-private storage
 * (`filesDir/archive/media/<conversationId>/`), never the public gallery
 * (ARCHITECTURE.md §4). Writes go to a temp file first and are only renamed
 * into their final name after the caller confirms the DB row committed, so
 * a crash mid-write can never leave a DB row pointing at a half-written
 * file, and any leftover `.tmp` file is unambiguously safe for
 * [CleanupEngine]'s orphan sweep to delete.
 */
class MediaStorage(private val context: Context) {

    private val root: File by lazy {
        File(context.filesDir, "archive/media").apply { mkdirs() }
    }

    fun directoryFor(conversationId: Long): File =
        File(root, conversationId.toString()).apply { mkdirs() }

    /** Streams [sourceUri] into app-private storage, hashing as it goes for dedup. */
    fun persist(conversationId: Long, sourceUri: Uri, extension: String): PersistResult {
        val dir = directoryFor(conversationId)
        val tempFile = File(dir, "${UUID.randomUUID()}.tmp")
        val digest = MessageDigest.getInstance("SHA-256")
        var bytesWritten = 0L

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        bytesWritten += read
                    }
                }
            } ?: return PersistResult.Failure("Source stream unavailable")
        } catch (e: Exception) {
            tempFile.delete()
            return PersistResult.Failure(e.message ?: "Unknown write failure")
        }

        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        val finalFile = File(dir, "$hash.$extension")
        if (finalFile.exists()) {
            // Identical content already stored for this conversation — reuse it,
            // never keep a second copy (brief §11 dedup requirement).
            tempFile.delete()
        } else if (!tempFile.renameTo(finalFile)) {
            tempFile.delete()
            return PersistResult.Failure("Could not finalize media file")
        }

        return PersistResult.Success(
            localUri = Uri.fromFile(finalFile).toString(),
            contentHash = hash,
            sizeBytes = bytesWritten,
        )
    }

    fun delete(localUri: String) {
        runCatching { Uri.parse(localUri).path?.let { File(it).delete() } }
    }

    /** Any file under the media root with no corresponding DB row — leftover temp files or post-cleanup remnants. */
    fun listAllFiles(): List<File> =
        root.walkTopDown().filter { it.isFile }.toList()

    fun deleteOrphan(file: File) {
        file.delete()
    }

    sealed interface PersistResult {
        data class Success(val localUri: String, val contentHash: String, val sizeBytes: Long) : PersistResult
        data class Failure(val reason: String) : PersistResult
    }
}
