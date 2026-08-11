package app.messagememory.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["conversationId"]),
        Index(value = ["expiresAt"]),
        Index(value = ["contentHash"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val conversationId: Long,
    val type: MediaType,
    val localUri: String?,
    val mimeType: String?,
    val filename: String?,
    val sizeBytes: Long?,
    val durationMs: Long?,
    val contentHash: String?,
    val capturedAt: Long,
    val expiresAt: Long,
    val isViewOnce: Boolean,
    val isOpened: Boolean,
    val manuallySaved: Boolean,
    val savedUri: String?,
    val captureStatus: CaptureStatus,
    val captureStatusDetail: String?,
)
