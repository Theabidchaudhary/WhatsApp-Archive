package app.messagememory.notification

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.messagememory.data.db.entity.CaptureStatus

/**
 * Defensively confirms a [MediaCandidate]'s URI is actually openable before
 * the app ever claims media was captured. Never assumes `getDataUri()`
 * being non-null means bytes are retrievable — some WhatsApp/OEM
 * combinations expose a URI that is not (or no longer) grantable to this
 * listener. See TECHNICAL_LIMITATIONS.md §4.
 */
object MediaProbe {

    data class ProbeResult(
        val status: CaptureStatus,
        val detail: String?,
        val sizeBytes: Long?,
        val resolvedMimeType: String?,
    )

    fun probe(context: Context, candidate: MediaCandidate): ProbeResult {
        val uri = runCatching { Uri.parse(candidate.uriString) }.getOrNull()
            ?: return ProbeResult(CaptureStatus.UNAVAILABLE, "Malformed media URI", null, null)

        return try {
            context.contentResolver.openInputStream(uri)?.use { /* confirm openable, then close */ }
                ?: return ProbeResult(CaptureStatus.UNAVAILABLE, "WhatsApp did not make this media available", null, null)

            var size: Long? = null
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !cursor.isNull(idx)) size = cursor.getLong(idx)
                }
            }

            ProbeResult(
                status = CaptureStatus.SUCCESS,
                detail = null,
                sizeBytes = size,
                resolvedMimeType = candidate.mimeType ?: context.contentResolver.getType(uri),
            )
        } catch (e: SecurityException) {
            ProbeResult(CaptureStatus.UNAVAILABLE, "No read permission for this media URI", null, null)
        } catch (e: Exception) {
            ProbeResult(CaptureStatus.FAILED, e.message ?: "Unknown media probe failure", null, null)
        }
    }
}
