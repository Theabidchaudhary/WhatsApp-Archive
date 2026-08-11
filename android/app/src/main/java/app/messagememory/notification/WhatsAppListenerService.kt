package app.messagememory.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import app.messagememory.MessageMemoryApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The entire capture mechanism. A system-bound service — Android keeps it
 * bound and rebinds it after process death/reboot as long as the user has
 * granted Notification Access (TECHNICAL_LIMITATIONS.md §9). All callbacks
 * return quickly; real work is dispatched onto [serviceScope] on
 * [Dispatchers.IO] so we never risk an ANR or listener disconnect on the
 * main thread.
 */
class WhatsAppListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val container get() = (application as MessageMemoryApp).container

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
        // Reconcile against whatever's already posted (e.g. after boot or a
        // process restart) so we don't wait for the next notification update
        // to notice conversations that were already in the shade.
        serviceScope.launch {
            runCatching { activeNotifications }
                .getOrNull()
                ?.forEach { handle(it) }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (!WhatsAppPackages.isWhatsAppPackage(sbn.packageName)) return
        serviceScope.launch { handle(sbn) }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        // Deliberately a no-op for archive content: removal means the
        // notification left the shade (read, swiped, or cancelled), not
        // that the message was deleted in WhatsApp. See
        // TECHNICAL_LIMITATIONS.md §7 — the archive's only deletion
        // triggers are 24h expiry and explicit user action.
    }

    private suspend fun handle(sbn: StatusBarNotification) {
        try {
            val parsed = NotificationParser.parse(sbn) ?: return
            container.ingestPipeline.ingest(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ingest notification ${sbn.key}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "WhatsAppListener"
    }
}
