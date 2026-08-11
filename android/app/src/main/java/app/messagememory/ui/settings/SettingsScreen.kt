package app.messagememory.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSamsungGuidance: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SettingsRow("Notification access", "Grant or review WhatsApp notification permission", onOpenNotificationAccessSettings)
            SettingsRow("Storage management", "See archive size, clear archive or media", onOpenStorage)
            SettingsRow("Background reliability", "Samsung One UI battery/background guidance", onOpenSamsungGuidance)
            SettingsRow("Diagnostics", "Monitoring state, last capture, last cleanup", onOpenDiagnostics)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Everything captured stays on this device for exactly 24 hours, then it's deleted " +
                        "automatically. No account, no cloud, no analytics, no ads, no internet access at all.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
