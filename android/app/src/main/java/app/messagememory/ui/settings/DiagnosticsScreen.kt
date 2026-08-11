package app.messagememory.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.messagememory.di.AppContainer
import app.messagememory.notification.WhatsAppPackages
import app.messagememory.permissions.NotificationAccessState
import app.messagememory.util.Formatters

/**
 * Operational status only — never message content (brief §23).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(container: AppContainer, onBack: () -> Unit) {
    val state by container.diagnosticsRepository.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            DiagRow("Notification access", if (NotificationAccessState.isEnabled(context)) "Granted" else "Not granted")
            DiagRow("WhatsApp detected", WhatsAppPackages.installedVariants(context).ifEmpty { setOf("None") }.joinToString())
            DiagRow("Last notification received", state.lastNotificationReceivedAt?.let { Formatters.timestamp(it) } ?: "None yet")
            DiagRow("Last successful capture", state.lastSuccessfulCaptureAt?.let { Formatters.timestamp(it) } ?: "None yet")
            DiagRow("Last cleanup", state.lastCleanupAt?.let { Formatters.timestamp(it) } ?: "Not run yet")
            DiagRow("Last cleanup removed", "${state.lastCleanupRemovedMessages} messages, ${state.lastCleanupRemovedMedia} media")
            DiagRow("Last cleanup error", state.lastCleanupError ?: "None")
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
