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

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<MessageEntity?>

    @Query("SELECT * FROM messages WHERE expiresAt <= :now")
    suspend fun expired(now: Long): List<MessageEntity>

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    /**
     * "Clear media but keep messages" must not leave a message row
     * claiming SUCCESS for media that no longer exists — see
     * ARCHITECTURE.md §8 ("never fake success"). Uses the raw enum name
     * since Converters stores CaptureStatus as its `.name` string.
     */
    @Query(
        "UPDATE messages SET hasMedia = 0, mediaId = NULL, " +
            "captureStatus = CASE WHEN text IS NOT NULL THEN 'PARTIAL' ELSE 'UNAVAILABLE' END " +
            "WHERE hasMedia = 1",
    )
    suspend fun clearMediaReferences()

    @Query(
        "SELECT * FROM messages WHERE expiresAt > :now AND (" +
            "text LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%'" +
            ") ORDER BY timestamp DESC",
    )
    suspend fun search(query: String, now: Long): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE expiresAt > :now")
    suspend fun activeCount(now: Long): Int

    @Query("SELECT * FROM messages WHERE expiresAt > :now ORDER BY timestamp DESC")
    suspend fun allActive(now: Long): List<MessageEntity>

    @Query("SELECT MIN(capturedAt) FROM messages WHERE expiresAt > :now")
    suspend fun oldestActiveCapturedAt(now: Long): Long?
}
