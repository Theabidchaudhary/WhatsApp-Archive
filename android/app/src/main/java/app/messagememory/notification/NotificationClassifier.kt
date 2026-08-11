package app.messagememory.notification

import app.messagememory.data.db.entity.MessageType
import app.messagememory.data.db.entity.PlaceholderReason
import java.security.MessageDigest

/**
 * Pure classification logic, deliberately kept free of any Android
 * framework type so it can be unit tested directly (see
 * NotificationClassifierTest). All the "is this text actually a message,
 * a summary, or a view-once placeholder" judgment calls live here.
 */
object NotificationClassifier {

    // Phrasing WhatsApp has been observed to use for View Once media in
    // notification text. Not guaranteed exhaustive or stable across
    // WhatsApp versions/locales (TECHNICAL_LIMITATIONS.md §2) — a miss here
    // only means the message is archived as a normal IMAGE/VIDEO instead of
    // specially labeled, never a loss of already-captured content.
    private val VIEW_ONCE_MARKERS = listOf(
        "view once photo",
        "view once video",
        "photo · view once",
        "video · view once",
    )

    private val SUMMARY_ONLY_MARKERS = listOf(
        "new messages",
        "new message",
    )

    fun classify(text: String?, hasMediaCandidate: Boolean): Pair<MessageType, PlaceholderReason> {
        val lower = text?.lowercase()?.trim()
        if (lower != null && VIEW_ONCE_MARKERS.any { lower.contains(it) }) {
            val type = if (lower.contains("video")) MessageType.VIEW_ONCE_VIDEO else MessageType.VIEW_ONCE_IMAGE
            return type to PlaceholderReason.VIEW_ONCE
        }
        if (lower.isNullOrBlank() || SUMMARY_ONLY_MARKERS.any { lower.contains(it) }) {
            return MessageType.UNKNOWN to PlaceholderReason.SUMMARY_ONLY
        }
        if (hasMediaCandidate) {
            return classifyMediaHint(lower) to PlaceholderReason.NONE
        }
        return MessageType.TEXT to PlaceholderReason.NONE
    }

    private fun classifyMediaHint(lowerText: String): MessageType = when {
        lowerText.contains("photo") || lowerText.contains("image") -> MessageType.IMAGE
        lowerText.contains("video") -> MessageType.VIDEO
        lowerText.contains("voice message") || lowerText.contains("audio") -> MessageType.VOICE_NOTE
        lowerText.contains("document") -> MessageType.DOCUMENT
        else -> MessageType.TEXT
    }

    /** Stable per-conversation key so notification updates map to the same [ConversationEntity]. */
    fun conversationKey(packageName: String, notificationTag: String?, groupKeyFallback: String): String {
        val stableTag = notificationTag ?: groupKeyFallback
        return sha256("$packageName|$stableTag")
    }

    /**
     * Strongest available de-duplication signal given WhatsApp exposes no
     * stable per-message ID through the notification API
     * (TECHNICAL_LIMITATIONS.md §3).
     */
    fun dedupKey(conversationKey: String, senderKey: String, timestamp: Long, text: String?): String =
        sha256("$conversationKey|$senderKey|$timestamp|${text.orEmpty()}")

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
