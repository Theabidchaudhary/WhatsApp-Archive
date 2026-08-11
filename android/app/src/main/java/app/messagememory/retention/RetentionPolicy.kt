package app.messagememory.retention

import java.util.concurrent.TimeUnit

/**
 * Rolling 24h absolute retention, stamped once per item at capture time —
 * never a "delete everything at midnight" batch policy. Pure function,
 * directly unit tested against the brief's own example (captured
 * 14:32:10 -> expires next day 14:32:10).
 */
object RetentionPolicy {
    val RETENTION_MILLIS: Long = TimeUnit.HOURS.toMillis(24)

    fun expiresAt(capturedAtMillis: Long): Long = capturedAtMillis + RETENTION_MILLIS

    fun isExpired(expiresAtMillis: Long, nowMillis: Long): Boolean = expiresAtMillis <= nowMillis

    fun remainingMillis(expiresAtMillis: Long, nowMillis: Long): Long =
        (expiresAtMillis - nowMillis).coerceAtLeast(0)
}
