package com.mindvault.ai.ui.insights

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun InsightsRoute(
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    viewModel: InsightsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    InsightsScreen(state = state, contentPadding = contentPadding)
}
