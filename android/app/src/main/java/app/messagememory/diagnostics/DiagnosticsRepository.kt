package app.messagememory.diagnostics

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Non-sensitive operational bookkeeping only (timestamps, counters) — never
 * message content, per brief §23 ("don't expose private message content
 * unnecessarily in diagnostics"). Backed by plain SharedPreferences since
 * none of this data is sensitive on its own.
 */
data class DiagnosticsState(
    val lastNotificationReceivedAt: Long? = null,
    val lastSuccessfulCaptureAt: Long? = null,
    val lastCleanupAt: Long? = null,
    val lastCleanupRemovedMessages: Int = 0,
    val lastCleanupRemovedMedia: Int = 0,
    val lastCleanupError: String? = null,
)

class DiagnosticsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("message_memory_diagnostics", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<DiagnosticsState> = _state

    private fun load(): DiagnosticsState = DiagnosticsState(
        lastNotificationReceivedAt = prefs.getLong(KEY_LAST_NOTIFICATION, -1).takeIf { it >= 0 },
        lastSuccessfulCaptureAt = prefs.getLong(KEY_LAST_CAPTURE, -1).takeIf { it >= 0 },
        lastCleanupAt = prefs.getLong(KEY_LAST_CLEANUP, -1).takeIf { it >= 0 },
        lastCleanupRemovedMessages = prefs.getInt(KEY_LAST_CLEANUP_MSGS, 0),
        lastCleanupRemovedMedia = prefs.getInt(KEY_LAST_CLEANUP_MEDIA, 0),
        lastCleanupError = prefs.getString(KEY_LAST_CLEANUP_ERROR, null),
    )

    fun recordNotificationReceived(atMillis: Long) {
        prefs.edit().putLong(KEY_LAST_NOTIFICATION, atMillis).apply()
        _state.update { it.copy(lastNotificationReceivedAt = atMillis) }
    }

    fun recordCapture(atMillis: Long, capturedSomething: Boolean) {
        prefs.edit().putLong(KEY_LAST_NOTIFICATION, atMillis).apply()
        _state.update { it.copy(lastNotificationReceivedAt = atMillis) }
        if (capturedSomething) {
            prefs.edit().putLong(KEY_LAST_CAPTURE, atMillis).apply()
            _state.update { it.copy(lastSuccessfulCaptureAt = atMillis) }
        }
    }

    fun recordCleanup(atMillis: Long, removedMessages: Int, removedMedia: Int, error: String?) {
        prefs.edit()
            .putLong(KEY_LAST_CLEANUP, atMillis)
            .putInt(KEY_LAST_CLEANUP_MSGS, removedMessages)
            .putInt(KEY_LAST_CLEANUP_MEDIA, removedMedia)
            .putString(KEY_LAST_CLEANUP_ERROR, error)
            .apply()
        _state.update {
            it.copy(
                lastCleanupAt = atMillis,
                lastCleanupRemovedMessages = removedMessages,
                lastCleanupRemovedMedia = removedMedia,
                lastCleanupError = error,
            )
        }
    }

    private companion object {
        const val KEY_LAST_NOTIFICATION = "last_notification_received_at"
        const val KEY_LAST_CAPTURE = "last_successful_capture_at"
        const val KEY_LAST_CLEANUP = "last_cleanup_at"
        const val KEY_LAST_CLEANUP_MSGS = "last_cleanup_removed_messages"
        const val KEY_LAST_CLEANUP_MEDIA = "last_cleanup_removed_media"
        const val KEY_LAST_CLEANUP_ERROR = "last_cleanup_error"
    }
}
