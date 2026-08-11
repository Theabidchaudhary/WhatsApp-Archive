package app.messagememory.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["dedupKey"], unique = true),
        Index(value = ["conversationId"]),
        Index(value = ["expiresAt"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val dedupKey: String,
    val senderName: String,
    val senderIdentifier: String?,
    val isOutgoing: Boolean,
    val text: String?,
    val placeholderReason: PlaceholderReason,
    val messageType: MessageType,
    val timestamp: Long,
    val capturedAt: Long,
    val expiresAt: Long,
    val originalNotificationKey: String,
    val wasSeenDeletedInWhatsApp: Boolean,
    val hasMedia: Boolean,
    val mediaId: Long?,
    val quotedText: String?,
    val quotedSender: String?,
    val captureStatus: CaptureStatus,
)
