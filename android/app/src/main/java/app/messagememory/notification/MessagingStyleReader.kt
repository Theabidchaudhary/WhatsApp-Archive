package app.messagememory.notification

import android.app.Notification
import android.os.Build
import android.os.Parcelable
import android.service.notification.StatusBarNotification

/**
 * Thin Android-framework glue that pulls raw values out of a
 * `StatusBarNotification`. Deliberately does no classification/judgment —
 * that lives in [NotificationClassifier] so it can be unit tested without
 * a framework dependency. This class is exercised by Robolectric-backed
 * tests instead.
 */
object MessagingStyleReader {

    data class RawMessage(
        val senderName: String,
        val senderKey: String?,
        val isOutgoing: Boolean,
        val text: String?,
        val timestamp: Long,
        val dataUri: String?,
        val dataMimeType: String?,
    )

    data class RawExtraction(
        val conversationTitle: String,
        val isGroup: Boolean,
        val messages: List<RawMessage>,
        val avatarBytesAvailable: Boolean,
    )

    fun isGroupSummary(sbn: StatusBarNotification): Boolean =
        (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

    fun extract(sbn: StatusBarNotification): RawExtraction {
        val extras = sbn.notification.extras
        val messagesArray = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        val rawMessages = if (messagesArray != null) {
            fromMessagingStyle(messagesArray)
        } else {
            fromFlatFallback(extras)
        }

        val title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: "Unknown conversation"

        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
            rawMessages.mapNotNull { it.senderKey ?: it.senderName }.distinct().size > 1

        val avatarAvailable = rawMessages.any { it.senderKey != null }

        return RawExtraction(
            conversationTitle = title,
            isGroup = isGroup,
            messages = rawMessages,
            avatarBytesAvailable = avatarAvailable,
        )
    }

    private fun fromMessagingStyle(messagesArray: Array<Parcelable>): List<RawMessage> {
        val messages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(messagesArray)
        return messages.map { msg ->
            val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) msg.senderPerson else null
            RawMessage(
                senderName = person?.name?.toString() ?: "You",
                senderKey = person?.key,
                isOutgoing = person == null,
                text = msg.text?.toString(),
                timestamp = msg.timestamp,
                dataUri = msg.dataUri?.toString(),
                dataMimeType = msg.dataMimeType,
            )
        }
    }

    private fun fromFlatFallback(extras: android.os.Bundle): List<RawMessage> {
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown"
        if (text == null) return emptyList()
        return listOf(
            RawMessage(
                senderName = title,
                senderKey = null,
                isOutgoing = false,
                text = text,
                timestamp = System.currentTimeMillis(),
                dataUri = null,
                dataMimeType = null,
            ),
        )
    }
}
