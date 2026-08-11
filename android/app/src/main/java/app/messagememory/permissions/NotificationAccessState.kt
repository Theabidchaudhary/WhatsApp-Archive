package app.messagememory.permissions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationAccessState {
    fun isEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Opens the system's Notification Access settings screen; the user grants/revokes there, this app never can. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
