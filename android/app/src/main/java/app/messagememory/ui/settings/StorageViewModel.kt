package app.messagememory.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.messagememory.data.repo.ArchiveRepository
import app.messagememory.data.repo.StorageSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StorageViewModel(private val archiveRepository: ArchiveRepository) : ViewModel() {

    private val _snapshot = MutableStateFlow<StorageSnapshot?>(null)
    val snapshot: StateFlow<StorageSnapshot?> = _snapshot.asStateFlow()

    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _snapshot.value = archiveRepository.storageSnapshot() }
    }

    fun clearArchive() {
        viewModelScope.launch {
            archiveRepository.clearArchive()
            _cleared.value = true
            refresh()
        }
    }

    fun clearMediaKeepMessages() {
        viewModelScope.launch {
            archiveRepository.clearMediaKeepMessages()
            refresh()
        }
    }
}
