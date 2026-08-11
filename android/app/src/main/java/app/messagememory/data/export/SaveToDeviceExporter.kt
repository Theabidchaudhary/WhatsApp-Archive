package app.messagememory.data.export

import android.content.Context
import android.net.Uri

/**
 * "Save to device" (brief §7): copies an archived file to a
 * user-picked destination via the Storage Access Framework
 * (`ACTION_CREATE_DOCUMENT`), which needs no storage permission on any
 * supported API level — unlike a direct `MediaStore` insert, which still
 * requires `WRITE_EXTERNAL_STORAGE` on API 26-28. The destination [Uri] is
 * chosen by the user through the system picker the framework provides;
 * this class only streams bytes into wherever they pointed.
 *
 * The resulting copy is fully independent of the 24h archive — it survives
 * the archive item's expiry, exactly as the brief specifies.
 */
class SaveToDeviceExporter(private val context: Context) {

    fun copyToUserSelectedUri(sourceLocalUri: String, destinationUri: Uri): Boolean {
        return try {
            val source = Uri.parse(sourceLocalUri)
            context.contentResolver.openInputStream(source)?.use { input ->
                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    input.copyTo(output)
                } ?: return false
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun suggestedFileName(filename: String?, mimeType: String?, fallbackExtension: String): String {
        if (!filename.isNullOrBlank()) return filename
        val ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: fallbackExtension
        return "message_memory_${System.currentTimeMillis()}.$ext"
    }
}
