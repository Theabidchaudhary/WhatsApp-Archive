package app.messagememory.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtil {
    fun shareIntent(context: Context, localUri: String, mimeType: String?): Intent? {
        val path = runCatching { Uri.parse(localUri).path }.getOrNull() ?: return null
        val file = File(path)
        if (!file.exists()) return null
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType ?: "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
