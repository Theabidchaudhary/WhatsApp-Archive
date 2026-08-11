package app.messagememory.util

import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.ln
import kotlin.math.pow

object Formatters {
    fun timestamp(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - millis
        val oneDay = TimeUnit.DAYS.toMillis(1)
        return when {
            diff < 0 -> DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
            diff < oneDay && isSameDay(millis, now) -> DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
            diff < oneDay * 2 -> "Yesterday"
            else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(millis))
        }
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = a
        val dayA = cal.get(java.util.Calendar.DAY_OF_YEAR)
        cal.timeInMillis = b
        val dayB = cal.get(java.util.Calendar.DAY_OF_YEAR)
        return dayA == dayB
    }

    fun remainingLabel(remainingMillis: Long): String {
        if (remainingMillis <= 0) return "Expired"
        val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
        return "Expires in ${hours}h ${minutes}m"
    }

    fun bytes(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (ln(size.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = size / 1024.0.pow(digitGroups.toDouble())
        return String.format("%.2f %s", value, units[digitGroups])
    }
}
