package app.messagememory.ui.media

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.messagememory.data.db.entity.MediaEntity
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.export.SaveToDeviceExporter
import app.messagememory.data.repo.ArchiveRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaViewerViewModel(
    messageId: Long,
    private val archiveRepository: ArchiveRepository,
    private val exporter: SaveToDeviceExporter,
) : ViewModel() {

    data class UiState(
        val message: MessageEntity? = null,
        val media: MediaEntity? = null,
        val saveConfirmed: Boolean = false,
    )

    private val messageFlow = archiveRepository.observeMessage(messageId)
    private val mediaFlow = messageFlow.flatMapLatest { message ->
        message?.let { archiveRepository.observeMedia(it.id) } ?: flowOf(null)
    }

    val uiState: StateFlow<UiState> = combine(messageFlow, mediaFlow) { message, media ->
        UiState(message, media)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun markOpened() {
        viewModelScope.launch {
            uiState.value.media?.let { archiveRepository.markMediaOpened(it) }
        }
    }

    fun saveToDevice(destination: Uri) {
        val media = uiState.value.media ?: return
        val localUri = media.localUri ?: return
        viewModelScope.launch {
            val success = exporter.copyToUserSelectedUri(localUri, destination)
            if (success) archiveRepository.markMediaSaved(media, destination.toString())
        }
    }
}
