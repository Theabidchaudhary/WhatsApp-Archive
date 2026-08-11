package app.messagememory.notification

import android.service.notification.StatusBarNotification

/**
 * Orchestrates [MessagingStyleReader] (raw extraction) and
 * [NotificationClassifier] (pure classification) into a [ParsedNotification]
 * ready for [IngestPipeline]. Returns null for notifications that should be
 * skipped entirely (group summaries, non-WhatsApp packages).
 */
object NotificationParser {

    fun parse(sbn: StatusBarNotification): ParsedNotification? {
        val packageName = sbn.packageName
        if (!WhatsAppPackages.isWhatsAppPackage(packageName)) return null
        if (MessagingStyleReader.isGroupSummary(sbn)) return null

        val raw = MessagingStyleReader.extract(sbn)
        if (raw.messages.isEmpty()) return null

        val conversationKey = NotificationClassifier.conversationKey(
            packageName = packageName,
            notificationTag = sbn.tag,
            groupKeyFallback = sbn.key,
        )

        val parsedMessages = raw.messages.map { rawMessage ->
            val hasMediaCandidate = rawMessage.dataUri != null
            val (type, placeholderReason) = NotificationClassifier.classify(rawMessage.text, hasMediaCandidate)
            app.messagememory.notification.ParsedMessage(
                senderName = rawMessage.senderName,
                senderIdentifier = rawMessage.senderKey,
                isOutgoing = rawMessage.isOutgoing,
                text = if (placeholderReason == app.messagememory.data.db.entity.PlaceholderReason.NONE) rawMessage.text else null,
                placeholderReason = placeholderReason,
                messageType = type,
                timestamp = rawMessage.timestamp,
                mediaCandidate = rawMessage.dataUri?.let { MediaCandidate(it, rawMessage.dataMimeType) },
            )
        }

        return ParsedNotification(
            sourcePackage = packageName,
            conversationKey = conversationKey,
            conversationTitle = raw.conversationTitle,
            isGroup = raw.isGroup,
            notificationKey = sbn.key,
            avatarBytesAvailable = raw.avatarBytesAvailable,
            messages = parsedMessages,
        )
    }
}
