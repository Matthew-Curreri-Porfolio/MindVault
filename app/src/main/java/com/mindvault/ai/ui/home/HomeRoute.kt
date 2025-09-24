package com.mindvault.ai.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeRoute(
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onStatusConsumed()
        }
    }

    HomeScreen(
        state = state,
        onInitializeModels = viewModel::onInitializeModels,
        onRecordToggle = viewModel::onRecordToggle,
        onTranscriptChange = viewModel::onTranscriptChange,
        onSummarize = viewModel::onSummarize,
        onSave = viewModel::onSave,
        onMoodPick = viewModel::onMoodPick,
        onDismissError = viewModel::onErrorConsumed
    )
}
