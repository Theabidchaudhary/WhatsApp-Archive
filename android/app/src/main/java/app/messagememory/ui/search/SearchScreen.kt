package app.messagememory.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.repo.ArchiveFilter
import app.messagememory.di.AppContainer
import app.messagememory.ui.components.captureStatusLabel
import app.messagememory.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: SearchViewModel = viewModel(
        factory = viewModelFactory { initializer { SearchViewModel(container.archiveRepository) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search contacts, messages, media…") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ArchiveFilter.entries) { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }
            if (state.results.isEmpty()) {
                Text(
                    text = if (state.query.isBlank()) "No archived content matches this filter." else "No results for \"${state.query}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.results, key = { it.id }) { message -> SearchResultRow(message) }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(message: MessageEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(text = message.senderName, style = MaterialTheme.typography.titleMedium)
        Text(
            text = message.text ?: captureStatusLabel(message.captureStatus),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Formatters.timestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
