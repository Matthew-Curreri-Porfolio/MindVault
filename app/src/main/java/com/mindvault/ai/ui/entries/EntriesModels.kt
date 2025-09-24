package com.mindvault.ai.ui.entries

import androidx.compose.runtime.Immutable

@Immutable
data class EntriesFilter(
    val mood: String? = null,
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
    val onlyWithAudio: Boolean = false
) {
    val isActive: Boolean = mood != null || fromMillis != null || toMillis != null || onlyWithAudio
}

@Immutable
data class EntryListItem(
    val id: String,
    val title: String,
    val snippet: String,
    val mood: String,
    val createdAtMillis: Long,
    val hasAudio: Boolean,
    val summary: String,
    val transcript: String
)

@Immutable
data class EntriesUiState(
    val searchQuery: String = "",
    val filter: EntriesFilter = EntriesFilter(),
    val entries: List<EntryListItem> = emptyList(),
    val isLoading: Boolean = true,
    val availableMoods: List<String> = DefaultMoodOptions,
    val isEmpty: Boolean = false
)

internal val DefaultMoodOptions = listOf("calm", "happy", "anxious", "sad", "angry", "stressed", "grateful", "tired")
