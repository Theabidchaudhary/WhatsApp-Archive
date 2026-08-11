package app.messagememory.ui.conversation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.di.AppContainer
import app.messagememory.ui.components.MessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    container: AppContainer,
    conversationId: Long,
    onBack: () -> Unit,
    onOpenMedia: (Long) -> Unit,
) {
    val viewModel: ConversationViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ConversationViewModel(conversationId, container.archiveRepository) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.conversation?.title ?: "Conversation") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.messages, key = { it.id }) { message: MessageEntity ->
                val mediaState = if (message.hasMedia) {
                    container.archiveRepository.observeMedia(message.id).collectAsStateWithLifecycle(initialValue = null)
                } else null
                MessageBubble(
                    message = message,
                    media = mediaState?.value,
                    showSender = state.conversation?.isGroup == true,
                    now = now,
                    onOpenMedia = { onOpenMedia(message.id) },
                )
            }
        }
    }
}
