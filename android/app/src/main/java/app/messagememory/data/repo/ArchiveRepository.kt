package app.messagememory.data.repo

import app.messagememory.data.db.ConversationDao
import app.messagememory.data.db.MediaDao
import app.messagememory.data.db.MessageDao
import app.messagememory.data.db.entity.ConversationEntity
import app.messagememory.data.db.entity.MediaEntity
import app.messagememory.data.db.entity.MediaType
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.files.MediaStorage
import kotlinx.coroutines.flow.Flow

/** UI-facing façade over the DAOs. Every read filters live at query time by `expiresAt > now` where relevant. */
class ArchiveRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val mediaDao: MediaDao,
    private val mediaStorage: MediaStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    fun observeConversation(id: Long): Flow<ConversationEntity?> = conversationDao.observeById(id)

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.observeForConversation(conversationId)

    fun observeMedia(messageId: Long): Flow<MediaEntity?> = mediaDao.observeForMessage(messageId)

    suspend fun search(query: String): List<MessageEntity> {
        if (query.isBlank()) return emptyList()
        return messageDao.search(query.trim(), clock())
    }

    suspend fun markMediaOpened(media: MediaEntity) {
        if (!media.isOpened) mediaDao.update(media.copy(isOpened = true))
    }

    suspend fun markMediaSaved(media: MediaEntity, savedUri: String) {
        mediaDao.update(media.copy(manuallySaved = true, savedUri = savedUri))
    }

    suspend fun deleteMessage(message: MessageEntity) {
        messageDao.deleteById(message.id)
        conversationDao.recomputeAggregates(message.conversationId)
        conversationDao.deleteIfEmpty(message.conversationId)
    }

    suspend fun storageSnapshot(): StorageSnapshot {
        val now = clock()
        val counts = mediaDao.countsByType(now).associate { it.type to it.count }
        return StorageSnapshot(
            totalBytes = mediaDao.totalActiveBytes(now),
            messageCount = messageDao.activeCount(now),
            imageCount = counts[MediaType.IMAGE] ?: 0,
            videoCount = counts[MediaType.VIDEO] ?: 0,
            audioCount = (counts[MediaType.AUDIO] ?: 0) + (counts[MediaType.VOICE_NOTE] ?: 0),
            documentCount = counts[MediaType.DOCUMENT] ?: 0,
            oldestCapturedAt = messageDao.oldestActiveCapturedAt(now),
        )
    }

    suspend fun clearArchive() {
        mediaStorage.listAllFiles().forEach { mediaStorage.deleteOrphan(it) }
        mediaDao.deleteAll()
        messageDao.deleteAll()
        conversationDao.deleteAll()
    }

    suspend fun clearMediaKeepMessages() {
        mediaStorage.listAllFiles().forEach { mediaStorage.deleteOrphan(it) }
        mediaDao.deleteAll()
    }
}

data class StorageSnapshot(
    val totalBytes: Long,
    val messageCount: Int,
    val imageCount: Int,
    val videoCount: Int,
    val audioCount: Int,
    val documentCount: Int,
    val oldestCapturedAt: Long?,
)
