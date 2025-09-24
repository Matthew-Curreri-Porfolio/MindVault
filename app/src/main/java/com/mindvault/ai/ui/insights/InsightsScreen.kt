package com.mindvault.ai.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(text = "Insights", style = MaterialTheme.typography.displayLarge)
            Text(text = "Review your mood trends, streaks, and standout themes.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            MoodTrendCard(state)
        }

        item {
            StreaksCard(state)
        }

        item {
            TopicsCard(topics = state.topTopics)
        }
    }

    AnimatedVisibility(visible = state.isLoading, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MoodTrendCard(state: InsightsUiState) {
    if (state.moodPoints.isEmpty()) return
    val points = state.moodPoints
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "30-day mood trend", style = MaterialTheme.typography.titleLarge)
            MoodLineChart(points = points)
            MoodLegend(points)
            val lastWeek = points.takeLast(7)
            val positiveDays = lastWeek.count { it.moodScore >= 0.6f }
            Text(text = "$positiveDays of last 7 days felt grounded or positive", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodLegend(points: List<MoodPoint>) {
    val palette = moodColorPalette(MaterialTheme.colorScheme)
    val topMoods = remember(points) {
        points.groupingBy { it.mood.lowercase() }
            .eachCount()
            .filterKeys { it.isNotBlank() }
            .entries
            .sortedByDescending { it.value }
            .take(4)
    }
    if (topMoods.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            topMoods.forEach { entry ->
                val label = entry.key.replaceFirstChar { it.titlecase() }
                val color = palette[entry.key] ?: palette["default"] ?: MaterialTheme.colorScheme.primary
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = color, radius = size.minDimension / 2)
                    }
                    Text(text = "$label (${entry.value})", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MoodLineChart(points: List<MoodPoint>) {
    val colors = MaterialTheme.colorScheme
    val palette = remember(colors) { moodColorPalette(colors) }
    val baselineColor = colors.outlineVariant
    val primary = colors.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        if (points.isEmpty()) return@Canvas
        val chartHeight = size.height
        val chartWidth = size.width
        val maxScore = 1f
        val minScore = 0f
        val xStep = chartWidth / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { index, moodPoint ->
            val normalized = (moodPoint.moodScore - minScore) / (maxScore - minScore)
            val x = index * xStep
            val y = chartHeight - (normalized * chartHeight)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = primary, style = Stroke(width = 6f, cap = StrokeCap.Round))
        points.forEachIndexed { index, moodPoint ->
            val normalized = (moodPoint.moodScore - minScore) / (maxScore - minScore)
            val x = index * xStep
            val y = chartHeight - (normalized * chartHeight)
            val color = palette[moodPoint.mood.lowercase()] ?: palette["default"] ?: primary
            drawCircle(color = color, radius = 8f, center = Offset(x, y))
        }
        // baseline
        drawLine(color = baselineColor, start = Offset(0f, chartHeight), end = Offset(chartWidth, chartHeight))
    }
}

@Composable
private fun StreaksCard(state: InsightsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Streaks", style = MaterialTheme.typography.titleLarge)
            Text(text = "Current streak: ${state.streakStats.currentStreak} days", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Longest streak: ${state.streakStats.longestStreak} days", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Entries this week: ${state.entryCountLastWeek}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicsCard(topics: List<TopicChip>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Recurring themes", style = MaterialTheme.typography.titleLarge)
            if (topics.isEmpty()) {
                Text(text = "Topics will appear as you record more entries.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topics.forEach { chip ->
                        AssistChip(
                            onClick = {},
                            label = { Text("${chip.label} (${chip.weight})") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun moodColorPalette(colors: androidx.compose.material3.ColorScheme): Map<String, Color> {
    return mapOf(
        "happy" to colors.primary,
        "grateful" to colors.primary,
        "calm" to colors.secondary,
        "anxious" to colors.tertiary,
        "stressed" to colors.tertiary,
        "sad" to Color(0xFF5C6BC0),
        "angry" to Color(0xFFE57373),
        "tired" to Color(0xFF8D6E63),
        "default" to colors.primary
    )
}

@Preview(showBackground = true)
@Composable
private fun InsightsPreview() {
    val state = InsightsUiState(
        isLoading = false,
        moodPoints = (0 until 30).map {
            MoodPoint(
                date = LocalDate.now().minusDays((29 - it).toLong()),
                mood = listOf("calm", "happy", "anxious")[it % 3],
                moodScore = listOf(0.7f, 0.9f, 0.3f)[it % 3]
            )
        },
        streakStats = StreakStats(currentStreak = 5, longestStreak = 12),
        topTopics = listOf(TopicChip("Breathing", 5), TopicChip("Gratitude", 3)),
        entryCountLastWeek = 4
    )
    com.mindvault.ai.ui.theme.MindVaultTheme {
        InsightsScreen(state = state)
    }
}
