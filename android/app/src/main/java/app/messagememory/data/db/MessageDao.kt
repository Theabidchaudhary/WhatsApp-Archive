package app.messagememory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.messagememory.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE dedupKey = :dedupKey LIMIT 1")
    suspend fun findByDedupKey(dedupKey: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeForConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE expiresAt <= :now")
    suspend fun expired(now: Long): List<MessageEntity>

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query(
        "SELECT * FROM messages WHERE expiresAt > :now AND (" +
            "text LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%'" +
            ") ORDER BY timestamp DESC",
    )
    suspend fun search(query: String, now: Long): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE expiresAt > :now")
    suspend fun activeCount(now: Long): Int

    @Query("SELECT MIN(capturedAt) FROM messages WHERE expiresAt > :now")
    suspend fun oldestActiveCapturedAt(now: Long): Long?
}
