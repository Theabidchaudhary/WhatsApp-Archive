package app.messagememory.retention

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class RetentionPolicyTest {

    @Test
    fun `expiresAt is exactly 24 hours after capture, matching the brief's own example`() {
        // Captured 11 August 2026, 14:32:10 -> expires 12 August 2026, 14:32:10
        val captured = java.util.GregorianCalendar(2026, 7, 11, 14, 32, 10).timeInMillis
        val expected = java.util.GregorianCalendar(2026, 7, 12, 14, 32, 10).timeInMillis

        assertEquals(expected, RetentionPolicy.expiresAt(captured))
    }

    @Test
    fun `is not expired one millisecond before expiry`() {
        val captured = 1_000_000L
        val expiresAt = RetentionPolicy.expiresAt(captured)
        assertFalse(RetentionPolicy.isExpired(expiresAt, expiresAt - 1))
    }

    @Test
    fun `is expired exactly at the expiry instant`() {
        val captured = 1_000_000L
        val expiresAt = RetentionPolicy.expiresAt(captured)
        assertTrue(RetentionPolicy.isExpired(expiresAt, expiresAt))
    }

    @Test
    fun `remaining millis never goes negative`() {
        val expiresAt = 1_000_000L
        assertEquals(0L, RetentionPolicy.remainingMillis(expiresAt, expiresAt + TimeUnit.HOURS.toMillis(5)))
    }

    @Test
    fun `retention window is exactly 24 hours, not a calendar day`() {
        assertEquals(TimeUnit.HOURS.toMillis(24), RetentionPolicy.RETENTION_MILLIS)
    }
}
