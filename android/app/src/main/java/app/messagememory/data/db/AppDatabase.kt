package app.messagememory.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.messagememory.data.db.entity.ConversationEntity
import app.messagememory.data.db.entity.MediaEntity
import app.messagememory.data.db.entity.MessageEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, MediaEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun mediaDao(): MediaDao

    companion object {
        const val DB_NAME = "message_memory.db"

        fun build(context: Context): AppDatabase {
            val passphrase = DbPassphrase.getOrCreate(context)
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .build()
        }
    }
}
