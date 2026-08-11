package app.messagememory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.messagememory.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE whatsappConversationKey = :key LIMIT 1")
    suspend fun findByKey(key: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query(
        "SELECT * FROM conversations WHERE messageCount > 0 OR mediaCount > 0 " +
            "ORDER BY lastMessageTimestamp DESC",
    )
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ConversationEntity?

    @Query(
        "UPDATE conversations SET messageCount = " +
            "(SELECT COUNT(*) FROM messages WHERE messages.conversationId = conversations.id), " +
            "mediaCount = (SELECT COUNT(*) FROM media WHERE media.conversationId = conversations.id), " +
            "lastMessageTimestamp = COALESCE((SELECT MAX(timestamp) FROM messages WHERE messages.conversationId = conversations.id), lastMessageTimestamp), " +
            "lastMessagePreview = (SELECT CASE WHEN text IS NOT NULL THEN text ELSE ('[' || messageType || ']') END " +
            "FROM messages WHERE messages.conversationId = conversations.id ORDER BY timestamp DESC LIMIT 1) " +
            "WHERE id = :id",
    )
    suspend fun recomputeAggregates(id: Long)

    @Query("DELETE FROM conversations WHERE id = :id AND messageCount = 0 AND mediaCount = 0")
    suspend fun deleteIfEmpty(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int
}
