package app.messagememory.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.messagememory.di.AppContainer
import app.messagememory.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagementScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: StorageViewModel = viewModel(
        factory = viewModelFactory { initializer { StorageViewModel(container.archiveRepository) } },
    )
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var confirmClearAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage management") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Archive storage currently used", style = MaterialTheme.typography.titleMedium)
            Text(
                text = Formatters.bytes(snapshot?.totalBytes ?: 0),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            snapshot?.let { s ->
                StatRow("Messages", s.messageCount.toString())
                StatRow("Images", s.imageCount.toString())
                StatRow("Videos", s.videoCount.toString())
                StatRow("Audio", s.audioCount.toString())
                StatRow("Documents", s.documentCount.toString())
                StatRow("Oldest item", s.oldestCapturedAt?.let { Formatters.timestamp(it) } ?: "—")
            }

            Text(
                text = "All items expire automatically after 24 hours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )

            OutlinedButton(onClick = { viewModel.clearMediaKeepMessages() }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear media but keep messages")
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            Button(onClick = { confirmClearAll = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear Archive Now")
            }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear entire archive?") },
            text = { Text("This deletes every captured message and media file right now. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearArchive()
                    confirmClearAll = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
