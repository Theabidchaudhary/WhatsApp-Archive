package app.messagememory.notification

import app.messagememory.data.db.entity.MessageType
import app.messagememory.data.db.entity.PlaceholderReason

/**
 * Output of [NotificationParser] — a pure, plain-data representation of a
 * WhatsApp `StatusBarNotification`, independent of Room/Android framework
 * types wherever possible so parsing logic is directly unit-testable
 * without instrumentation.
 */
data class ParsedNotification(
    val sourcePackage: String,
    val conversationKey: String,
    val conversationTitle: String,
    val isGroup: Boolean,
    val notificationKey: String,
    val avatarBytesAvailable: Boolean,
    val messages: List<ParsedMessage>,
)

data class ParsedMessage(
    val senderName: String,
    val senderIdentifier: String?,
    val isOutgoing: Boolean,
    val text: String?,
    val placeholderReason: PlaceholderReason,
    val messageType: MessageType,
    val timestamp: Long,
    val mediaCandidate: MediaCandidate?,
)

/** A media reference the notification *may* carry; must still be probed before trusting it. */
data class MediaCandidate(
    val uriString: String,
    val mimeType: String?,
)
