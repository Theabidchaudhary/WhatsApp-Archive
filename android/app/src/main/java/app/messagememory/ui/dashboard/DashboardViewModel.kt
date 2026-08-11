package app.messagememory.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.messagememory.data.db.entity.ConversationEntity
import app.messagememory.data.repo.ArchiveRepository
import app.messagememory.notification.WhatsAppPackages
import app.messagememory.permissions.NotificationAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class DashboardViewModel(
    application: Application,
    private val archiveRepository: ArchiveRepository,
) : AndroidViewModel(application) {

    private val monitoringEnabled = MutableStateFlow(NotificationAccessState.isEnabled(application))
    private val whatsappInstalled = MutableStateFlow(WhatsAppPackages.installedVariants(application).isNotEmpty())

    data class UiState(
        val conversations: List<ConversationEntity> = emptyList(),
        val monitoringEnabled: Boolean = false,
        val whatsappInstalled: Boolean = true,
    )

    val uiState: StateFlow<UiState> = combine(
        archiveRepository.observeConversations(),
        monitoringEnabled,
        whatsappInstalled,
    ) { conversations, monitoring, installed ->
        UiState(conversations, monitoring, installed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun refreshMonitoringState() {
        monitoringEnabled.update { NotificationAccessState.isEnabled(getApplication()) }
        whatsappInstalled.update { WhatsAppPackages.installedVariants(getApplication()).isNotEmpty() }
    }
}
