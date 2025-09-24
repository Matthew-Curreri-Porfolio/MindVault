package com.mindvault.ai.ui.insights

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class MoodPoint(
    val date: LocalDate,
    val mood: String,
    val moodScore: Float
)

@Immutable
data class StreakStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

@Immutable
data class TopicChip(
    val label: String,
    val weight: Int
)

@Immutable
data class InsightsUiState(
    val isLoading: Boolean = true,
    val moodPoints: List<MoodPoint> = emptyList(),
    val streakStats: StreakStats = StreakStats(),
    val topTopics: List<TopicChip> = emptyList(),
    val entryCountLastWeek: Int = 0
)
