package app.messagememory.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.messagememory.di.AppContainer
import app.messagememory.ui.components.ConversationRow
import app.messagememory.ui.components.MonitoringIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    container: AppContainer,
    onOpenConversation: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DashboardViewModel(application, container.archiveRepository) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshMonitoringState() }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Message Memory") },
                    navigationIcon = {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                )
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    MonitoringIndicator(
                        enabled = state.monitoringEnabled,
                        onClick = onOpenNotificationAccessSettings,
                    )
                }
            }
        },
    ) { padding ->
        if (!state.whatsappInstalled) {
            EmptyState(
                modifier = Modifier.padding(padding).fillMaxSize(),
                title = "WhatsApp not found",
                message = "Install WhatsApp Messenger or WhatsApp Business to start archiving conversations.",
            )
        } else if (!state.monitoringEnabled) {
            EmptyState(
                modifier = Modifier.padding(padding).fillMaxSize(),
                title = "Monitoring disabled",
                message = "Grant Notification Access so Message Memory can remember messages before they disappear.",
            )
        } else if (state.conversations.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding).fillMaxSize(),
                title = "Nothing captured yet",
                message = "Conversations will appear here as WhatsApp notifications arrive. Everything expires automatically after 24 hours.",
            )
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(state.conversations, key = { it.id }) { conversation ->
                    ConversationRow(conversation = conversation, onClick = { onOpenConversation(conversation.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, title: String, message: String) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
