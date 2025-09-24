package com.mindvault.ai.ui.entries

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.ai.data.db.JournalEntry
import com.mindvault.ai.data.repo.EntryRepository
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class EntriesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EntryRepository(application)

    private val searchQuery = MutableStateFlow("")
    private val filterState = MutableStateFlow(EntriesFilter())

    private val _uiState = MutableStateFlow(EntriesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeEntries()
    }

    private fun observeEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                repository.observeAll(),
                searchQuery.debounce(300).onStart { emit(searchQuery.value) },
                filterState
            ) { entries, debouncedQuery, filter ->
                val filtered = applyFilters(entries, debouncedQuery, filter)
                val availableMoods = buildMoodList(entries)
                _uiState.update {
                    it.copy(
                        searchQuery = searchQuery.value,
                        filter = filter,
                        entries = filtered,
                        availableMoods = availableMoods,
                        isLoading = false,
                        isEmpty = filtered.isEmpty()
                    )
                }
            }.collect {}
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onClearSearch() {
        onSearchQueryChange("")
    }

    fun onMoodFilterSelected(mood: String?) {
        filterState.update { current -> current.copy(mood = mood) }
    }

    fun onToggleHasAudio() {
        filterState.update { current -> current.copy(onlyWithAudio = !current.onlyWithAudio) }
    }

    fun onDateRangeChanged(startMillis: Long?, endMillis: Long?) {
        filterState.update { current -> current.copy(fromMillis = startMillis, toMillis = endMillis) }
    }

    fun onResetFilters() {
        filterState.value = EntriesFilter()
    }

    private fun applyFilters(
        entries: List<JournalEntry>,
        query: String,
        filter: EntriesFilter
    ): List<EntryListItem> {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        return entries.asSequence()
            .filter { entry ->
                val matchesQuery = if (normalizedQuery.isBlank()) {
                    true
                } else {
                    entry.summary.contains(normalizedQuery, ignoreCase = true) ||
                        entry.transcript.contains(normalizedQuery, ignoreCase = true)
                }
                val matchesMood = filter.mood?.let { mood -> entry.mood.equals(mood, ignoreCase = true) } ?: true
                val matchesAudio = if (filter.onlyWithAudio) entry.audioPath.isNotBlank() else true
                val matchesFrom = filter.fromMillis?.let { entry.createdAt >= it } ?: true
                val matchesTo = filter.toMillis?.let { entry.createdAt <= it } ?: true
                matchesQuery && matchesMood && matchesAudio && matchesFrom && matchesTo
            }
            .map { it.toListItem() }
            .toList()
    }

    private fun buildMoodList(entries: List<JournalEntry>): List<String> {
        val moods = entries.mapNotNull { entry -> entry.mood.takeIf { it.isNotBlank() } }
        return (moods + DefaultMoodOptions).distinct()
    }

    private fun JournalEntry.toListItem(): EntryListItem {
        val titleSource = summary.ifBlank { transcript }
        val title = titleSource.lineSequence().firstOrNull()?.take(MAX_TITLE_LENGTH)?.let {
            if (it.length < titleSource.length) "$it…" else it
        } ?: "Untitled entry"
        val snippetSource = transcript.ifBlank { summary }
        val snippet = snippetSource.trim().take(MAX_SNIPPET_LENGTH)
        return EntryListItem(
            id = id,
            title = title,
            snippet = snippet,
            mood = mood,
            createdAtMillis = createdAt,
            hasAudio = audioPath.isNotBlank(),
            summary = summary,
            transcript = transcript
        )
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 60
        private const val MAX_SNIPPET_LENGTH = 160
    }
}
