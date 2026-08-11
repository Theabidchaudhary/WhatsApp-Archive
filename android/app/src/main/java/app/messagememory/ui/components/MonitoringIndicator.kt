package app.messagememory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun MonitoringIndicator(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(color)
                .padding(4.dp),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 4.dp))
        Text(
            text = if (enabled) "Monitoring Active" else "Monitoring Disabled",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
