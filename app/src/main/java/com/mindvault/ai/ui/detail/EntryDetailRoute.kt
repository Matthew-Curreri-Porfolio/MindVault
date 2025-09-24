package com.mindvault.ai.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EntryDetailRoute(
    entryId: String,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onEntryDeleted: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: EntryDetailViewModel = viewModel(factory = EntryDetailViewModel.provideFactory(app, entryId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EntryDetailEvent.Share -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.content)
                    }
                    val chooser = Intent.createChooser(intent, "Share entry")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
                is EntryDetailEvent.Export -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, "Export entry")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
                EntryDetailEvent.Deleted -> {
                    snackbarHostState.showSnackbar("Entry deleted")
                    onEntryDeleted()
                }
            }
        }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onStatusConsumed()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorConsumed()
        }
    }

    EntryDetailScreen(
        state = state,
        onTranscriptChange = viewModel::onTranscriptChange,
        onMoodChange = viewModel::onMoodChange,
        onSummarize = viewModel::onSummarize,
        onRedact = viewModel::onRedact,
        onSave = viewModel::onSave,
        onShare = viewModel::onShareRequested,
        onExportTxt = { viewModel.onExport(EntryExportFormat.TXT) },
        onExportPdf = { viewModel.onExport(EntryExportFormat.PDF) },
        onDelete = viewModel::onDelete,
        onPlaybackToggle = viewModel::onPlaybackToggle,
        onSeek = viewModel::onSeek,
        contentPadding = contentPadding
    )
}
