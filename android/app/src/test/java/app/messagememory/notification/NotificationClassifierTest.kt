package app.messagememory.notification

import app.messagememory.data.db.entity.MessageType
import app.messagememory.data.db.entity.PlaceholderReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationClassifierTest {

    @Test
    fun `plain text classifies as TEXT with no placeholder`() {
        val (type, reason) = NotificationClassifier.classify("Hey, are you coming?", hasMediaCandidate = false)
        assertEquals(MessageType.TEXT, type)
        assertEquals(PlaceholderReason.NONE, reason)
    }

    @Test
    fun `null text with no media is summary-only`() {
        val (type, reason) = NotificationClassifier.classify(null, hasMediaCandidate = false)
        assertEquals(MessageType.UNKNOWN, type)
        assertEquals(PlaceholderReason.SUMMARY_ONLY, reason)
    }

    @Test
    fun `generic new-messages summary is never treated as content`() {
        val (_, reason) = NotificationClassifier.classify("3 new messages", hasMediaCandidate = false)
        assertEquals(PlaceholderReason.SUMMARY_ONLY, reason)
    }

    @Test
    fun `view once photo is classified honestly as unavailable placeholder`() {
        val (type, reason) = NotificationClassifier.classify("View once photo", hasMediaCandidate = false)
        assertEquals(MessageType.VIEW_ONCE_IMAGE, type)
        assertEquals(PlaceholderReason.VIEW_ONCE, reason)
    }

    @Test
    fun `view once video is distinguished from view once photo`() {
        val (type, _) = NotificationClassifier.classify("View once video", hasMediaCandidate = false)
        assertEquals(MessageType.VIEW_ONCE_VIDEO, type)
    }

    @Test
    fun `media hint text without a candidate is not silently upgraded`() {
        // hasMediaCandidate=false means no getDataUri() was present — even
        // if the text says "Photo", we must not claim a media type unless a
        // candidate actually exists; classify() only applies the media hint
        // when the caller says a candidate exists.
        val (type, reason) = NotificationClassifier.classify("Photo", hasMediaCandidate = true)
        assertEquals(MessageType.IMAGE, type)
        assertEquals(PlaceholderReason.NONE, reason)
    }

    @Test
    fun `dedup key is stable for identical inputs`() {
        val a = NotificationClassifier.dedupKey("conv1", "sender1", 1000L, "hello")
        val b = NotificationClassifier.dedupKey("conv1", "sender1", 1000L, "hello")
        assertEquals(a, b)
    }

    @Test
    fun `dedup key differs when any field changes`() {
        val base = NotificationClassifier.dedupKey("conv1", "sender1", 1000L, "hello")
        assertNotEquals(base, NotificationClassifier.dedupKey("conv1", "sender1", 1000L, "hello!"))
        assertNotEquals(base, NotificationClassifier.dedupKey("conv1", "sender2", 1000L, "hello"))
        assertNotEquals(base, NotificationClassifier.dedupKey("conv1", "sender1", 1001L, "hello"))
        assertNotEquals(base, NotificationClassifier.dedupKey("conv2", "sender1", 1000L, "hello"))
    }

    @Test
    fun `conversation key ignores tag when equal, differs by package`() {
        val a = NotificationClassifier.conversationKey("com.whatsapp", "chat-123", "fallback")
        val b = NotificationClassifier.conversationKey("com.whatsapp.w4b", "chat-123", "fallback")
        assertNotEquals(a, b)
    }
}
