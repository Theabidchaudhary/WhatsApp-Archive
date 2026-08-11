package app.messagememory.notification

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.messagememory.data.db.AppDatabase
import app.messagememory.data.db.entity.CaptureStatus
import app.messagememory.data.db.entity.MessageType
import app.messagememory.data.db.entity.PlaceholderReason
import app.messagememory.data.files.MediaStorage
import app.messagememory.diagnostics.DiagnosticsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * End-to-end check that a parsed notification with a real, openable media
 * URI is ingested without violating the messages->media foreign key
 * (regression coverage for the message-must-exist-before-media ordering in
 * IngestPipeline), and that re-ingesting the same notification content is
 * deduplicated rather than double-inserted.
 */
@RunWith(RobolectricTestRunner::class)
class IngestPipelineTest {

    private lateinit var db: AppDatabase
    private lateinit var pipeline: IngestPipeline
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pipeline = IngestPipeline(
            context = context,
            conversationDao = db.conversationDao(),
            messageDao = db.messageDao(),
            mediaDao = db.mediaDao(),
            mediaStorage = MediaStorage(context),
            diagnostics = DiagnosticsRepository(context),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `message with real media candidate is captured without FK violation`() = runTest {
        val sourceFile = File(context.cacheDir, "source.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val parsed = notificationWithMedia(sourceFile)

        pipeline.ingest(parsed)

        val conversation = db.conversationDao().findByKey(parsed.conversationKey)
        assertNotNull(conversation)
        val messages = db.messageDao().allActive(System.currentTimeMillis())
        assertEquals(1, messages.size)
        assertEquals(CaptureStatus.SUCCESS, messages.first().captureStatus)
        assertTrue(messages.first().hasMedia)
        assertNotNull(messages.first().mediaId)

        val media = db.mediaDao().getById(messages.first().mediaId!!)
        assertNotNull(media)
        assertEquals(CaptureStatus.SUCCESS, media!!.captureStatus)
        assertTrue("media file should have been copied into app-private storage", File(Uri.parse(media.localUri).path!!).exists())
    }

    @Test
    fun `re-ingesting the same notification does not duplicate messages`() = runTest {
        val sourceFile = File(context.cacheDir, "source2.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val parsed = notificationWithMedia(sourceFile)

        pipeline.ingest(parsed)
        pipeline.ingest(parsed)

        val messages = db.messageDao().allActive(System.currentTimeMillis())
        assertEquals("second ingest of identical content must be deduplicated", 1, messages.size)
    }

    private fun notificationWithMedia(sourceFile: File): ParsedNotification {
        val candidate = MediaCandidate(Uri.fromFile(sourceFile).toString(), "image/jpeg")
        return ParsedNotification(
            sourcePackage = "com.whatsapp",
            conversationKey = "conv-key-1",
            conversationTitle = "Test Chat",
            isGroup = false,
            notificationKey = "notif-key-1",
            avatarBytesAvailable = false,
            messages = listOf(
                ParsedMessage(
                    senderName = "Alex",
                    senderIdentifier = "alex-1",
                    isOutgoing = false,
                    text = null,
                    placeholderReason = PlaceholderReason.NONE,
                    messageType = MessageType.IMAGE,
                    timestamp = System.currentTimeMillis(),
                    mediaCandidate = candidate,
                ),
            ),
        )
    }
}
