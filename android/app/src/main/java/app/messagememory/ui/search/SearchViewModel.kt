package app.messagememory.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.repo.ArchiveFilter
import app.messagememory.data.repo.ArchiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(private val archiveRepository: ArchiveRepository) : ViewModel() {

    data class UiState(
        val query: String = "",
        val filter: ArchiveFilter = ArchiveFilter.ALL,
        val results: List<MessageEntity> = emptyList(),
        val loading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        runQuery()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        runQuery()
    }

    fun onFilterChange(filter: ArchiveFilter) {
        _uiState.update { it.copy(filter = filter) }
        runQuery()
    }

    private fun runQuery() {
        val (query, filter) = _uiState.value.let { it.query to it.filter }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val results = archiveRepository.browse(query, filter)
            _uiState.update { it.copy(results = results, loading = false) }
        }
    }
}
