package app.messagememory.notification

import android.content.Context
import android.content.pm.PackageManager

/**
 * Identifies WhatsApp by installed package rather than assuming presence
 * (brief §4 / TECHNICAL_LIMITATIONS.md §8). No other packages are ever
 * treated as a capture source.
 */
object WhatsAppPackages {
    const val MESSENGER = "com.whatsapp"
    const val BUSINESS = "com.whatsapp.w4b"

    val KNOWN = setOf(MESSENGER, BUSINESS)

    fun isWhatsAppPackage(packageName: String): Boolean = packageName in KNOWN

    /** Which of the known WhatsApp variants are actually installed on this device, right now. */
    fun installedVariants(context: Context): Set<String> {
        val pm = context.packageManager
        return KNOWN.filterTo(mutableSetOf()) { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}
