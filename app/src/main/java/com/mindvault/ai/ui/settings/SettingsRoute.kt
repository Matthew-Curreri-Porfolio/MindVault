package com.mindvault.ai.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsRoute(
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onStatusConsumed()
        }
    }

    SettingsScreen(
        state = state,
        onToggleSync = viewModel::onToggleSync,
        onToggleAnalytics = viewModel::onToggleAnalytics,
        onToggleIndexing = viewModel::onToggleIndexing,
        onThreadsChanged = viewModel::onThreadsChanged,
        onClearCache = viewModel::onClearCache,
        contentPadding = contentPadding
    )
}
