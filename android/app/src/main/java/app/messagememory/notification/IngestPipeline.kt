package app.messagememory.notification

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import app.messagememory.data.db.ConversationDao
import app.messagememory.data.db.MediaDao
import app.messagememory.data.db.MessageDao
import app.messagememory.data.db.entity.CaptureStatus
import app.messagememory.data.db.entity.ConversationEntity
import app.messagememory.data.db.entity.MediaEntity
import app.messagememory.data.db.entity.MediaType
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.db.entity.MessageType
import app.messagememory.data.db.entity.PlaceholderReason
import app.messagememory.data.files.MediaStorage
import app.messagememory.diagnostics.DiagnosticsRepository
import app.messagememory.retention.RetentionPolicy

/**
 * Turns a [ParsedNotification] into Room writes. See
 * ARCHITECTURE.md §3 for the full pipeline description, including why
 * message capture and media capture are independent (a failed media
 * probe never rolls back the already-inserted message row).
 */
class IngestPipeline(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val mediaDao: MediaDao,
    private val mediaStorage: MediaStorage,
    private val diagnostics: DiagnosticsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun ingest(parsed: ParsedNotification) {
        val now = clock()
        val conversation = upsertConversation(parsed, now)

        var insertedAny = false
        for (message in parsed.messages) {
            if (ingestMessage(conversation, parsed, message, now)) insertedAny = true
        }

        conversationDao.recomputeAggregates(conversation.id)
        diagnostics.recordCapture(now, insertedAny)
    }

    private suspend fun upsertConversation(parsed: ParsedNotification, now: Long): ConversationEntity {
        val existing = conversationDao.findByKey(parsed.conversationKey)
        if (existing != null) {
            val updated = existing.copy(
                title = parsed.conversationTitle,
                isGroup = parsed.isGroup,
            )
            if (updated != existing) conversationDao.update(updated)
            return updated
        }
        val fresh = ConversationEntity(
            whatsappConversationKey = parsed.conversationKey,
            title = parsed.conversationTitle,
            isGroup = parsed.isGroup,
            profileIdentifier = null,
            avatarLocalUri = null,
            sourcePackage = parsed.sourcePackage,
            lastMessageTimestamp = now,
            createdAt = now,
            expiresAt = RetentionPolicy.expiresAt(now),
            unreadCount = 0,
            messageCount = 0,
            mediaCount = 0,
        )
        val id = conversationDao.insert(fresh)
        return fresh.copy(id = id)
    }

    /** Returns true if a new message row was actually inserted (vs. a dedup skip). */
    private suspend fun ingestMessage(
        conversation: ConversationEntity,
        parsed: ParsedNotification,
        message: ParsedMessage,
        now: Long,
    ): Boolean {
        val senderKey = message.senderIdentifier ?: message.senderName
        val dedupKey = NotificationClassifier.dedupKey(
            conversation.whatsappConversationKey,
            senderKey,
            message.timestamp,
            message.text,
        )
        if (messageDao.findByDedupKey(dedupKey) != null) return false

        val capturedAt = now
        val expiresAt = RetentionPolicy.expiresAt(capturedAt)

        var mediaId: Long? = null
        var mediaCaptureStatus: CaptureStatus? = null

        if (message.mediaCandidate != null) {
            mediaId = captureMedia(conversation.id, message, capturedAt, expiresAt)
            mediaCaptureStatus = mediaId?.let { mediaDao.getById(it)?.captureStatus }
        }

        val overallStatus = IngestLogic.resolveMessageCaptureStatus(
            hasText = message.text != null,
            hasMediaCandidate = message.mediaCandidate != null,
            mediaCaptureStatus = mediaCaptureStatus,
        )

        val entity = MessageEntity(
            conversationId = conversation.id,
            dedupKey = dedupKey,
            senderName = message.senderName,
            senderIdentifier = message.senderIdentifier,
            isOutgoing = message.isOutgoing,
            text = message.text,
            placeholderReason = message.placeholderReason,
            messageType = message.messageType,
            timestamp = message.timestamp,
            capturedAt = capturedAt,
            expiresAt = expiresAt,
            originalNotificationKey = parsed.notificationKey,
            wasSeenDeletedInWhatsApp = false,
            hasMedia = mediaId != null,
            mediaId = mediaId,
            quotedText = null,
            quotedSender = null,
            captureStatus = overallStatus,
        )
        val insertedId = messageDao.insert(entity)
        if (insertedId != -1L && mediaId != null) {
            mediaDao.getById(mediaId)?.let { mediaDao.update(it.copy(messageId = insertedId)) }
        }
        return insertedId != -1L
    }

    private suspend fun captureMedia(
        conversationId: Long,
        message: ParsedMessage,
        capturedAt: Long,
        expiresAt: Long,
    ): Long? {
        val candidate = message.mediaCandidate ?: return null
        val probe = MediaProbe.probe(context, candidate)
        val mediaType = messageTypeToMediaType(message.messageType)

        if (probe.status != CaptureStatus.SUCCESS) {
            val entity = MediaEntity(
                messageId = 0,
                conversationId = conversationId,
                type = mediaType,
                localUri = null,
                mimeType = candidate.mimeType,
                filename = null,
                sizeBytes = null,
                durationMs = null,
                contentHash = null,
                capturedAt = capturedAt,
                expiresAt = expiresAt,
                isViewOnce = message.messageType == MessageType.VIEW_ONCE_IMAGE || message.messageType == MessageType.VIEW_ONCE_VIDEO,
                isOpened = false,
                manuallySaved = false,
                savedUri = null,
                captureStatus = probe.status,
                captureStatusDetail = probe.detail,
            )
            return mediaDao.insert(entity)
        }

        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(probe.resolvedMimeType) ?: "bin"
        val persistResult = mediaStorage.persist(conversationId, Uri.parse(candidate.uriString), extension)

        return when (persistResult) {
            is MediaStorage.PersistResult.Success -> {
                val existingDuplicate = mediaDao.findDuplicate(persistResult.contentHash, conversationId, capturedAt)
                val entity = MediaEntity(
                    messageId = 0,
                    conversationId = conversationId,
                    type = mediaType,
                    localUri = existingDuplicate?.localUri ?: persistResult.localUri,
                    mimeType = probe.resolvedMimeType,
                    filename = null,
                    sizeBytes = probe.sizeBytes ?: persistResult.sizeBytes,
                    durationMs = null,
                    contentHash = persistResult.contentHash,
                    capturedAt = capturedAt,
                    expiresAt = expiresAt,
                    isViewOnce = message.messageType == MessageType.VIEW_ONCE_IMAGE || message.messageType == MessageType.VIEW_ONCE_VIDEO,
                    isOpened = false,
                    manuallySaved = false,
                    savedUri = null,
                    captureStatus = CaptureStatus.SUCCESS,
                    captureStatusDetail = null,
                )
                mediaDao.insert(entity)
            }
            is MediaStorage.PersistResult.Failure -> {
                val entity = MediaEntity(
                    messageId = 0,
                    conversationId = conversationId,
                    type = mediaType,
                    localUri = null,
                    mimeType = probe.resolvedMimeType,
                    filename = null,
                    sizeBytes = null,
                    durationMs = null,
                    contentHash = null,
                    capturedAt = capturedAt,
                    expiresAt = expiresAt,
                    isViewOnce = message.messageType == MessageType.VIEW_ONCE_IMAGE || message.messageType == MessageType.VIEW_ONCE_VIDEO,
                    isOpened = false,
                    manuallySaved = false,
                    savedUri = null,
                    captureStatus = CaptureStatus.FAILED,
                    captureStatusDetail = persistResult.reason,
                )
                mediaDao.insert(entity)
            }
        }
    }

    private fun messageTypeToMediaType(type: MessageType): MediaType = when (type) {
        MessageType.IMAGE, MessageType.VIEW_ONCE_IMAGE -> MediaType.IMAGE
        MessageType.VIDEO, MessageType.VIEW_ONCE_VIDEO -> MediaType.VIDEO
        MessageType.VOICE_NOTE -> MediaType.VOICE_NOTE
        MessageType.AUDIO -> MediaType.AUDIO
        MessageType.DOCUMENT -> MediaType.DOCUMENT
        else -> MediaType.DOCUMENT
    }
}
