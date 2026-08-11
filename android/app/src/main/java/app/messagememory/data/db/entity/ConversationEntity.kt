package app.messagememory.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["whatsappConversationKey"], unique = true)],
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val whatsappConversationKey: String,
    val title: String,
    val isGroup: Boolean,
    val profileIdentifier: String?,
    val avatarLocalUri: String?,
    val sourcePackage: String,
    val lastMessageTimestamp: Long,
    val createdAt: Long,
    val expiresAt: Long,
    val unreadCount: Int,
    val messageCount: Int,
    val mediaCount: Int,
    /** Denormalized for fast dashboard rendering — recomputed alongside the other aggregates. */
    val lastMessagePreview: String? = null,
)
