package app.messagememory.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.messagememory.data.db.entity.ConversationEntity
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.repo.ArchiveRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val conversationId: Long,
    private val archiveRepository: ArchiveRepository,
) : ViewModel() {

    data class UiState(
        val conversation: ConversationEntity? = null,
        val messages: List<MessageEntity> = emptyList(),
    )

    val uiState: StateFlow<UiState> = combine(
        archiveRepository.observeConversation(conversationId),
        archiveRepository.observeMessages(conversationId),
    ) { conversation, messages -> UiState(conversation, messages) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch { archiveRepository.deleteMessage(message) }
    }
}
