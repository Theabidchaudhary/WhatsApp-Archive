package app.messagememory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.messagememory.data.db.entity.MediaEntity
import app.messagememory.data.db.entity.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(media: MediaEntity): Long

    @Update
    suspend fun update(media: MediaEntity)

    @Query("SELECT * FROM media WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MediaEntity?

    @Query("SELECT * FROM media WHERE messageId = :messageId LIMIT 1")
    fun observeForMessage(messageId: Long): Flow<MediaEntity?>

    @Query(
        "SELECT * FROM media WHERE contentHash = :hash AND conversationId = :conversationId " +
            "AND expiresAt > :now LIMIT 1",
    )
    suspend fun findDuplicate(hash: String, conversationId: Long, now: Long): MediaEntity?

    @Query("SELECT * FROM media WHERE expiresAt <= :now")
    suspend fun expired(now: Long): List<MediaEntity>

    @Query("SELECT COUNT(*) FROM media WHERE localUri = :localUri AND expiresAt > :now")
    suspend fun activeReferenceCount(localUri: String, now: Long): Int

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media")
    suspend fun deleteAll()

    @Query("DELETE FROM media WHERE type != :keepType")
    suspend fun deleteAllExceptType(keepType: MediaType)

    @Query(
        "SELECT type, COUNT(*) as count FROM media WHERE expiresAt > :now GROUP BY type",
    )
    suspend fun countsByType(now: Long): List<MediaTypeCount>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media WHERE expiresAt > :now")
    suspend fun totalActiveBytes(now: Long): Long

    @Query("SELECT DISTINCT localUri FROM media WHERE localUri IS NOT NULL")
    suspend fun allLocalUris(): List<String>
}

data class MediaTypeCount(val type: MediaType, val count: Int)
