package com.mindvault.ai.ui.home

data class HomeUiState(
    val isInitializingModels: Boolean = false,
    val modelsReady: Boolean = false,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val isSummarizing: Boolean = false,
    val transcript: String = "",
    val summary: String = "",
    val mood: String? = null,
    val waveLevels: List<Float> = emptyList(),
    val elapsedSeconds: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val audioPath: String? = null,
    val availableMoods: List<String> = listOf("calm", "happy", "anxious", "sad", "angry", "stressed", "grateful", "tired")
) {
    val isBusy: Boolean = isRecording || isTranscribing || isSummarizing || isInitializingModels
    val canSummarize: Boolean = transcript.isNotBlank() && !isTranscribing && !isSummarizing
    val canSave: Boolean = transcript.isNotBlank() && summary.isNotBlank() && mood != null && !isTranscribing && !isSummarizing
}
