package com.mindvault.ai.ui.detail

import androidx.compose.runtime.Immutable

@Immutable
data class EntryDetailUiState(
    val isLoading: Boolean = true,
    val entryId: String = "",
    val createdAtMillis: Long = 0L,
    val summary: String = "",
    val transcript: String = "",
    val mood: String? = null,
    val audioPath: String? = null,
    val hasAudio: Boolean = false,
    val isSummarizing: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Int = 0,
    val playbackDurationMs: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val availableMoods: List<String> = DEFAULT_MOODS,
    val isDirty: Boolean = false
) {
    val canSave: Boolean = isDirty && !isSaving && transcript.isNotBlank()
    val hasSummary: Boolean = summary.isNotBlank()
}

enum class EntryExportFormat { TXT, PDF }

sealed class EntryDetailEvent {
    data class Share(val content: String) : EntryDetailEvent()
    data class Export(val uri: android.net.Uri, val mimeType: String) : EntryDetailEvent()
    object Deleted : EntryDetailEvent()
}

private val DEFAULT_MOODS = listOf("calm", "happy", "anxious", "sad", "angry", "stressed", "grateful", "tired")
