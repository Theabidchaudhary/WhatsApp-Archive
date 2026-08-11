package app.messagememory.retention

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.messagememory.data.db.AppDatabase
import app.messagememory.data.db.entity.CaptureStatus
import app.messagememory.data.db.entity.ConversationEntity
import app.messagememory.data.db.entity.MediaEntity
import app.messagememory.data.db.entity.MediaType
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.db.entity.MessageType
import app.messagememory.data.db.entity.PlaceholderReason
import app.messagememory.data.files.MediaStorage
import app.messagememory.diagnostics.DiagnosticsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Exercises the rolling-expiry sweep end-to-end against a real (in-memory)
 * Room database, verifying ARCHITECTURE.md §5's claims: expired rows and
 * their files are removed, non-expired rows survive, and empty
 * conversations are cleaned up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // API 35 shadows aren't available in the pinned Robolectric version yet.
class CleanupEngineTest {

    private lateinit var db: AppDatabase
    private lateinit var engine: CleanupEngine
    private lateinit var mediaStorage: MediaStorage
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaStorage = MediaStorage(context)
        engine = CleanupEngine(
            db.conversationDao(),
            db.messageDao(),
            db.mediaDao(),
            mediaStorage,
            DiagnosticsRepository(context),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `expired message and its media file are removed, active ones survive`() = runTest {
        val now = System.currentTimeMillis()
        val conversationId = db.conversationDao().insert(conversation(now))

        // An already-expired message with a real media file on disk.
        val expiredFile = File(mediaStorage.directoryFor(conversationId), "expired.jpg").apply { writeText("x") }
        val expiredMessageId = db.messageDao().insert(
            message(conversationId, now - TimeUnit.HOURS.toMillis(25), hasMedia = true),
        )
        db.mediaDao().insert(media(expiredMessageId, conversationId, now - TimeUnit.HOURS.toMillis(25), localUri = "file://${expiredFile.absolutePath}"))

        // A fresh, still-active message.
        val activeMessageId = db.messageDao().insert(message(conversationId, now, hasMedia = false))

        val result = engine.sweep()

        assertEquals(1, result.removedMessages)
        assertEquals(1, result.removedMedia)
        assertTrue("expired file should be deleted", !expiredFile.exists())
        assertTrue("no expired rows should remain after sweep", db.messageDao().expired(now).isEmpty())
        // Active message untouched.
        assertEquals(1, db.messageDao().activeCount(now))
        assertTrue(activeMessageId > 0)
    }

    @Test
    fun `conversation with zero remaining items is deleted`() = runTest {
        val now = System.currentTimeMillis()
        val conversationId = db.conversationDao().insert(conversation(now))
        db.messageDao().insert(message(conversationId, now - TimeUnit.HOURS.toMillis(25), hasMedia = false))
        db.conversationDao().recomputeAggregates(conversationId)

        engine.sweep()

        assertNull(db.conversationDao().getById(conversationId))
    }

    @Test
    fun `sweep is idempotent - running twice does not error or double count`() = runTest {
        val now = System.currentTimeMillis()
        val conversationId = db.conversationDao().insert(conversation(now))
        db.messageDao().insert(message(conversationId, now - TimeUnit.HOURS.toMillis(25), hasMedia = false))

        val first = engine.sweep()
        val second = engine.sweep()

        assertEquals(1, first.removedMessages)
        assertEquals(0, second.removedMessages)
    }

    private fun conversation(now: Long) = ConversationEntity(
        whatsappConversationKey = "key-${now}-${(0..999999).random()}",
        title = "Test conversation",
        isGroup = false,
        profileIdentifier = null,
        avatarLocalUri = null,
        sourcePackage = "com.whatsapp",
        lastMessageTimestamp = now,
        createdAt = now,
        expiresAt = RetentionPolicy.expiresAt(now),
        unreadCount = 0,
        messageCount = 0,
        mediaCount = 0,
    )

    private fun message(conversationId: Long, capturedAt: Long, hasMedia: Boolean) = MessageEntity(
        conversationId = conversationId,
        dedupKey = "dedup-$conversationId-$capturedAt-${(0..999999).random()}",
        senderName = "Tester",
        senderIdentifier = null,
        isOutgoing = false,
        text = "hello",
        placeholderReason = PlaceholderReason.NONE,
        messageType = MessageType.TEXT,
        timestamp = capturedAt,
        capturedAt = capturedAt,
        expiresAt = RetentionPolicy.expiresAt(capturedAt),
        originalNotificationKey = "key",
        wasSeenDeletedInWhatsApp = false,
        hasMedia = hasMedia,
        mediaId = null,
        quotedText = null,
        quotedSender = null,
        captureStatus = CaptureStatus.SUCCESS,
    )

    private fun media(messageId: Long, conversationId: Long, capturedAt: Long, localUri: String) = MediaEntity(
        messageId = messageId,
        conversationId = conversationId,
        type = MediaType.IMAGE,
        localUri = localUri,
        mimeType = "image/jpeg",
        filename = null,
        sizeBytes = 1,
        durationMs = null,
        contentHash = "hash-$messageId",
        capturedAt = capturedAt,
        expiresAt = RetentionPolicy.expiresAt(capturedAt),
        isViewOnce = false,
        isOpened = false,
        manuallySaved = false,
        savedUri = null,
        captureStatus = CaptureStatus.SUCCESS,
        captureStatusDetail = null,
    )
}
