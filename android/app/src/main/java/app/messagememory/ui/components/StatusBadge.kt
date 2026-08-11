package app.messagememory.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.messagememory.data.db.entity.CaptureStatus

/**
 * Renders exactly the label the capture pipeline recorded — never a
 * friendlier-sounding guess. See ARCHITECTURE.md §8.
 */
@Composable
fun captureStatusLabel(status: CaptureStatus, isViewOnce: Boolean = false): String = when {
    isViewOnce && status != CaptureStatus.SUCCESS ->
        "View Once content detected, but WhatsApp did not make the media available to this app."
    status == CaptureStatus.SUCCESS -> "Media captured"
    status == CaptureStatus.PARTIAL -> "Media unavailable"
    status == CaptureStatus.UNAVAILABLE -> "Media unavailable"
    status == CaptureStatus.FAILED -> "Capture failed"
    else -> "Media unavailable"
}

@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
