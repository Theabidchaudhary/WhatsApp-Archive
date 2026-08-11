package app.messagememory.notification

import app.messagememory.data.db.entity.CaptureStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class IngestLogicTest {

    @Test
    fun `text only message with no media candidate is SUCCESS`() {
        val result = IngestLogic.resolveMessageCaptureStatus(hasText = true, hasMediaCandidate = false, mediaCaptureStatus = null)
        assertEquals(CaptureStatus.SUCCESS, result)
    }

    @Test
    fun `summary-only placeholder with no media is UNAVAILABLE, never SUCCESS`() {
        val result = IngestLogic.resolveMessageCaptureStatus(hasText = false, hasMediaCandidate = false, mediaCaptureStatus = null)
        assertEquals(CaptureStatus.UNAVAILABLE, result)
    }

    @Test
    fun `text plus successfully captured media is SUCCESS`() {
        val result = IngestLogic.resolveMessageCaptureStatus(hasText = true, hasMediaCandidate = true, mediaCaptureStatus = CaptureStatus.SUCCESS)
        assertEquals(CaptureStatus.SUCCESS, result)
    }

    @Test
    fun `media-only message with successful capture is SUCCESS`() {
        val result = IngestLogic.resolveMessageCaptureStatus(hasText = false, hasMediaCandidate = true, mediaCaptureStatus = CaptureStatus.SUCCESS)
        assertEquals(CaptureStatus.SUCCESS, result)
    }

    @Test
    fun `text present but media unavailable is PARTIAL, never SUCCESS`() {
        val result = IngestLogic.resolveMessageCaptureStatus(hasText = true, hasMediaCandidate = true, mediaCaptureStatus = CaptureStatus.UNAVAILABLE)
        assertEquals(CaptureStatus.PARTIAL, result)
    }

    @Test
    fun `media-only message with unavailable media is UNAVAILABLE, never SUCCESS`() {
        // This is the View Once case: no text, WhatsApp didn't expose the bytes.
        val result = IngestLogic.resolveMessageCaptureStatus(hasText = false, hasMediaCandidate = true, mediaCaptureStatus = CaptureStatus.UNAVAILABLE)
        assertEquals(CaptureStatus.UNAVAILABLE, result)
    }

    @Test
    fun `media-only message with failed capture is FAILED, never SUCCESS`() {
        val result = IngestLogic.resolveMessageCaptureStatus(hasText = false, hasMediaCandidate = true, mediaCaptureStatus = CaptureStatus.FAILED)
        assertEquals(CaptureStatus.FAILED, result)
    }

    @Test
    fun `text present with failed media capture degrades to PARTIAL, not FAILED`() {
        val result = IngestLogic.resolveMessageCaptureStatus(hasText = true, hasMediaCandidate = true, mediaCaptureStatus = CaptureStatus.FAILED)
        assertEquals(CaptureStatus.PARTIAL, result)
    }
}
