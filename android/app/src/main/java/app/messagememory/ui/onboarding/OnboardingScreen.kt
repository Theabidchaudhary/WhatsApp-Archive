package app.messagememory.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * First-launch explanation, shown until Notification Access is granted
 * (brief §20). Explains *why* before asking, and links straight to the
 * system settings screen — the app can never grant this permission to
 * itself.
 */
@Composable
fun OnboardingScreen(onOpenNotificationAccessSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Message Memory",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "A temporary, local memory for WhatsApp conversations. Nothing leaves your phone.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        SectionTitle("Notification Access")
        BodyText(
            "Allows this app to read WhatsApp notifications so it can temporarily remember messages that may " +
                "later disappear — for example if the sender deletes them. This works entirely through Android's " +
                "own notification system; this app never reads WhatsApp's database, never uses root, and never " +
                "modifies WhatsApp in any way.",
        )

        SectionTitle("What happens after you grant access")
        BodyText(
            "• Messages exposed through WhatsApp notifications are archived locally, with a 24-hour rolling " +
                "expiration — exactly 24 hours after each item was captured, it's automatically deleted.\n" +
                "• Media is only archived when WhatsApp itself makes it available through the notification. " +
                "When it doesn't (this is common for video, voice messages, and View Once content), the app says so " +
                "honestly instead of pretending to have captured it.\n" +
                "• Nothing is ever uploaded anywhere. There's no account, no cloud, no analytics, no ads.",
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
        Button(onClick = onOpenNotificationAccessSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Notification Access")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}
