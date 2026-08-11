package app.messagememory.data.repo

import app.messagememory.data.db.ConversationDao
import app.messagememory.data.db.MediaDao
import app.messagememory.data.db.MessageDao
import app.messagememory.data.db.entity.ConversationEntity
import app.messagememory.data.db.entity.MediaEntity
import app.messagememory.data.db.entity.MediaType
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.db.entity.MessageType
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

    fun observeMessage(messageId: Long): Flow<MessageEntity?> = messageDao.observeById(messageId)

    fun observeMedia(messageId: Long): Flow<MediaEntity?> = mediaDao.observeForMessage(messageId)

    suspend fun search(query: String): List<MessageEntity> {
        if (query.isBlank()) return emptyList()
        return messageDao.search(query.trim(), clock())
    }

    /**
     * Local-only browse across the currently retained 24h archive
     * (brief §16/§17) — query text plus a type filter. Bounded dataset size
     * (24h of one user's WhatsApp traffic) makes an in-memory filter pass
     * simpler and just as fast as a set of bespoke joined queries.
     */
    suspend fun browse(query: String, filter: ArchiveFilter): List<MessageEntity> {
        val now = clock()
        val base = if (query.isBlank()) messageDao.allActive(now) else messageDao.search(query.trim(), now)
        return when (filter) {
            ArchiveFilter.ALL -> base
            ArchiveFilter.MESSAGES -> base.filter { it.messageType == MessageType.TEXT }
            ArchiveFilter.IMAGES -> base.filter { it.messageType == MessageType.IMAGE || it.messageType == MessageType.VIEW_ONCE_IMAGE }
            ArchiveFilter.VIDEOS -> base.filter { it.messageType == MessageType.VIDEO || it.messageType == MessageType.VIEW_ONCE_VIDEO }
            ArchiveFilter.AUDIO -> base.filter { it.messageType == MessageType.AUDIO || it.messageType == MessageType.VOICE_NOTE }
            ArchiveFilter.DOCUMENTS -> base.filter { it.messageType == MessageType.DOCUMENT }
            ArchiveFilter.VIEW_ONCE -> base.filter { it.messageType == MessageType.VIEW_ONCE_IMAGE || it.messageType == MessageType.VIEW_ONCE_VIDEO }
            ArchiveFilter.SAVED -> base.filter { message ->
                message.mediaId?.let { mediaDao.getById(it)?.manuallySaved } ?: false
            }
        }
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
        messageDao.clearMediaReferences()
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
