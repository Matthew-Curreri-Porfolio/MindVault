package com.mindvault.ai.ui.entries

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EntriesRoute(
    contentPadding: PaddingValues = PaddingValues(),
    onOpenEntry: (String) -> Unit,
    viewModel: EntriesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EntriesScreen(
        state = state,
        onSearchChange = viewModel::onSearchQueryChange,
        onClearSearch = viewModel::onClearSearch,
        onMoodFilter = viewModel::onMoodFilterSelected,
        onHasAudioToggle = viewModel::onToggleHasAudio,
        onDateRangeChange = viewModel::onDateRangeChanged,
        onResetFilters = viewModel::onResetFilters,
        onOpenEntry = onOpenEntry,
        contentPadding = contentPadding
    )
}
