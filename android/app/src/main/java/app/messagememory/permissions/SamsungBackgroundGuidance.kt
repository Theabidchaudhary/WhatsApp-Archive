package app.messagememory.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Deep-links only — never silently changes a system setting
 * (TECHNICAL_LIMITATIONS.md §9 / brief §21). Every intent here opens a
 * system screen the user must act on themselves.
 */
object SamsungBackgroundGuidance {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Shows the system's own "Allow unrestricted battery usage?" confirmation — not a silent change. */
    fun requestIgnoreBatteryOptimizationsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

    /** Fallback / manual path: the app's own battery detail screen, where Samsung exposes its "Sleeping apps" controls. */
    fun appDetailsSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    val isSamsungDevice: Boolean
        get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}
