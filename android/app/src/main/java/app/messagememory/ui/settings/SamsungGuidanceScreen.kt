package app.messagememory.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.messagememory.permissions.SamsungBackgroundGuidance

/**
 * Deep-links only, every action shows a system confirmation the user
 * controls (brief §21 / TECHNICAL_LIMITATIONS.md §9) — this screen never
 * silently changes a setting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamsungGuidanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Background reliability") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                text = "Message Memory depends on notifications reaching this app reliably. Samsung's One UI " +
                    "battery management can delay or restart apps in the background more aggressively than stock " +
                    "Android. These steps make capture more reliable — none of them are required, and this app " +
                    "never changes them for you.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            GuidanceStep(
                title = "1. Allow unrestricted battery usage",
                body = "Prevents Android from restricting this app's background activity.",
                buttonText = "Open battery settings",
                onClick = { context.startActivity(SamsungBackgroundGuidance.requestIgnoreBatteryOptimizationsIntent(context)) },
            )

            GuidanceStep(
                title = "2. Remove from Sleeping apps (Samsung)",
                body = "Settings → Battery and device care → Background usage limits → Sleeping apps / Deep " +
                    "sleeping apps → remove Message Memory if it's listed. This screen isn't directly linkable " +
                    "by any app, so open the app's own settings page and navigate from there.",
                buttonText = "Open app settings",
                onClick = { context.startActivity(SamsungBackgroundGuidance.appDetailsSettingsIntent(context)) },
            )

            GuidanceStep(
                title = "3. Confirm Notification Access is still granted",
                body = "Occasionally revoked by OS updates or app resets — worth a periodic check.",
                buttonText = "Open notification access",
                onClick = { context.startActivity(app.messagememory.permissions.NotificationAccessState.settingsIntent()) },
            )
        }
    }
}

@Composable
private fun GuidanceStep(title: String, body: String, buttonText: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
        Button(onClick = onClick) { Text(buttonText) }
    }
}
