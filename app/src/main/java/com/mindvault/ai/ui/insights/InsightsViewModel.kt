package com.mindvault.ai.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.ai.data.db.JournalEntry
import com.mindvault.ai.data.repo.EntryRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EntryRepository(application)

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        observeEntries()
    }

    private fun observeEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeAll().collectLatest { entries ->
                val trend = moodPoints(entries)
                val streaks = calculateStreaks(entries)
                val topics = extractTopics(entries)
                val lastWeekCount = entries.count { entry ->
                    val date = entry.localDate
                    date.isAfter(LocalDate.now().minusDays(7)) || date.isEqual(LocalDate.now().minusDays(7))
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        moodPoints = trend,
                        streakStats = streaks,
                        topTopics = topics,
                        entryCountLastWeek = lastWeekCount
                    )
                }
            }
        }
    }

    private fun moodPoints(entries: List<JournalEntry>): List<MoodPoint> {
        val today = LocalDate.now()
        val start = today.minusDays(29)
        val grouped = entries
            .filter { entry ->
                val date = entry.localDate
                !date.isBefore(start) && !date.isAfter(today)
            }
            .groupBy { it.localDate }
        return (0..29).map { offset ->
            val date = start.plusDays(offset.toLong())
            val dayEntries = grouped[date].orEmpty()
            val dominantMood = dayEntries.groupingBy { it.mood.lowercase(Locale.getDefault()) }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: ""
            MoodPoint(
                date = date,
                mood = dominantMood,
                moodScore = if (dominantMood.isBlank()) 0f else moodToScore(dominantMood)
            )
        }
    }

    private fun calculateStreaks(entries: List<JournalEntry>): StreakStats {
        val dates = entries.map { it.localDate }.distinct().sorted()
        if (dates.isEmpty()) return StreakStats()
        var longest = 1
        var current = 1
        for (i in 1 until dates.size) {
            val prev = dates[i - 1]
            val currentDate = dates[i]
            if (prev.plusDays(1) == currentDate) {
                current += 1
            } else {
                longest = maxOf(longest, current)
                current = 1
            }
        }
        longest = maxOf(longest, current)
        val today = LocalDate.now()
        var currentStreak = 0
        var streakDate = today
        val dateSet = dates.toSet()
        while (dateSet.contains(streakDate)) {
            currentStreak += 1
            streakDate = streakDate.minusDays(1)
        }
        return StreakStats(currentStreak = currentStreak, longestStreak = longest)
    }

    private fun extractTopics(entries: List<JournalEntry>): List<TopicChip> {
        val stopWords = STOP_WORDS
        val counts = mutableMapOf<String, Int>()
        entries.forEach { entry ->
            val text = (entry.summary + " " + entry.transcript)
                .lowercase(Locale.getDefault())
                .replace("[\\d\\p{Punct}]".toRegex(), " ")
            text.split(" ")
                .map { it.trim() }
                .filter { it.length > 3 && it !in stopWords }
                .forEach { word -> counts[word] = counts.getOrDefault(word, 0) + 1 }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .take(12)
            .map { TopicChip(label = it.key.replaceFirstChar { ch -> ch.uppercaseChar() }, weight = it.value) }
    }

    private fun moodToScore(mood: String): Float {
        return when (mood.lowercase(Locale.getDefault())) {
            "grateful", "happy" -> 0.9f
            "calm" -> 0.7f
            "tired" -> 0.4f
            "anxious", "stressed" -> 0.2f
            "sad", "angry" -> 0.1f
            else -> 0.5f
        }
    }

    private val JournalEntry.localDate: LocalDate
        get() = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

    companion object {
        private val STOP_WORDS = setOf(
            "this", "that", "with", "from", "have", "about", "there", "they", "their",
            "been", "were", "what", "your", "into", "would", "could", "should", "might",
            "because", "while", "where", "which", "also"
        )
    }
}
