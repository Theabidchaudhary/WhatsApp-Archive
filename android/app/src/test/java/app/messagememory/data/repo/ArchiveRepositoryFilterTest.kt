package app.messagememory.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.messagememory.data.db.AppDatabase
import app.messagememory.data.db.entity.CaptureStatus
import app.messagememory.data.db.entity.ConversationEntity
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.db.entity.MessageType
import app.messagememory.data.db.entity.PlaceholderReason
import app.messagememory.data.files.MediaStorage
import app.messagememory.retention.RetentionPolicy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArchiveRepositoryFilterTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ArchiveRepository
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = ArchiveRepository(db.conversationDao(), db.messageDao(), db.mediaDao(), MediaStorage(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `filters partition messages by type, search narrows by text`() = runTest {
        val now = System.currentTimeMillis()
        val conversationId = db.conversationDao().insert(
            ConversationEntity(
                whatsappConversationKey = "k1", title = "Chat", isGroup = false, profileIdentifier = null,
                avatarLocalUri = null, sourcePackage = "com.whatsapp", lastMessageTimestamp = now,
                createdAt = now, expiresAt = RetentionPolicy.expiresAt(now), unreadCount = 0, messageCount = 0, mediaCount = 0,
            ),
        )

        db.messageDao().insert(textMessage(conversationId, now, "let's meet for the invoice review"))
        db.messageDao().insert(typedMessage(conversationId, now, MessageType.IMAGE))
        db.messageDao().insert(typedMessage(conversationId, now, MessageType.VIDEO))
        db.messageDao().insert(typedMessage(conversationId, now, MessageType.VIEW_ONCE_IMAGE))

        assertEquals(4, repository.browse("", ArchiveFilter.ALL).size)
        assertEquals(1, repository.browse("", ArchiveFilter.MESSAGES).size)
        assertEquals(2, repository.browse("", ArchiveFilter.IMAGES).size) // IMAGE + VIEW_ONCE_IMAGE
        assertEquals(1, repository.browse("", ArchiveFilter.VIDEOS).size)
        assertEquals(1, repository.browse("", ArchiveFilter.VIEW_ONCE).size)

        val searchResults = repository.browse("invoice", ArchiveFilter.ALL)
        assertEquals(1, searchResults.size)
        assertTrue(searchResults.first().text!!.contains("invoice"))
    }

    private fun textMessage(conversationId: Long, now: Long, text: String) = MessageEntity(
        conversationId = conversationId, dedupKey = "d-$text-$now", senderName = "A", senderIdentifier = null,
        isOutgoing = false, text = text, placeholderReason = PlaceholderReason.NONE, messageType = MessageType.TEXT,
        timestamp = now, capturedAt = now, expiresAt = RetentionPolicy.expiresAt(now), originalNotificationKey = "k",
        wasSeenDeletedInWhatsApp = false, hasMedia = false, mediaId = null, quotedText = null, quotedSender = null,
        captureStatus = CaptureStatus.SUCCESS,
    )

    private fun typedMessage(conversationId: Long, now: Long, type: MessageType) = MessageEntity(
        conversationId = conversationId, dedupKey = "d-$type-$now-${(0..999999).random()}", senderName = "A",
        senderIdentifier = null, isOutgoing = false, text = null, placeholderReason = PlaceholderReason.NONE,
        messageType = type, timestamp = now, capturedAt = now, expiresAt = RetentionPolicy.expiresAt(now),
        originalNotificationKey = "k", wasSeenDeletedInWhatsApp = false, hasMedia = false, mediaId = null,
        quotedText = null, quotedSender = null, captureStatus = CaptureStatus.UNAVAILABLE,
    )
}
