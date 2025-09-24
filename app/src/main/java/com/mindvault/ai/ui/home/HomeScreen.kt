package com.mindvault.ai.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ButtonDefaults
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun HomeScreen(
    state: HomeUiState,
    onInitializeModels: () -> Unit,
    onRecordToggle: () -> Unit,
    onTranscriptChange: (String) -> Unit,
    onSummarize: () -> Unit,
    onSave: () -> Unit,
    onMoodPick: (String) -> Unit,
    onDismissError: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }
    val today = remember { LocalDate.now().format(dateFormatter) }

    Box(modifier = Modifier.fillMaxSize()) {
        val canToggleRecording = state.isRecording || (state.modelsReady && !state.isInitializingModels && !state.isTranscribing)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = today,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                AnimatedVisibility(visible = !state.modelsReady) {
                    ElevatedCard(onClick = onInitializeModels) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Prepare on-device models",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Copy Whisper + Qwen to your device so transcription and summaries work offline.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            AnimatedVisibility(visible = state.isInitializingModels) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            }
                        }
                    }
                }
            }

            item {
                if (state.isRecording) {
                    RecordingCard(state = state)
                } else if (state.isTranscribing) {
                    ElevatedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text(text = "Transcribing…", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            item {
                TranscriptSection(
                    transcript = state.transcript,
                    onChange = onTranscriptChange,
                    enabled = !state.isTranscribing,
                    isTranscribing = state.isTranscribing
                )
            }

            item {
                SummarySection(
                    summary = state.summary,
                    isSummarizing = state.isSummarizing,
                    onSummarize = onSummarize,
                    canSummarize = state.canSummarize
                )
            }

            item {
                MoodChips(
                    moods = state.availableMoods,
                    selectedMood = state.mood,
                    onMoodPick = onMoodPick
                )
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSave,
                    enabled = state.canSave
                ) {
                    Text(text = "Save to vault")
                }
            }

            if (state.errorMessage != null) {
                item {
                    ErrorMessage(message = state.errorMessage, onDismissError = onDismissError)
                }
            }
        }

        val fabLabel = when {
            state.isRecording -> "Stop"
            state.isInitializingModels -> "Preparing…"
            else -> "Start journaling"
        }
        val fabIcon = when {
            state.isRecording -> Icons.Outlined.Stop
            else -> Icons.Outlined.GraphicEq
        }
        val containerColor = if (state.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        val labelColor = if (state.isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            text = { Text(fabLabel) },
            icon = { Icon(imageVector = fabIcon, contentDescription = fabLabel) },
            onClick = { if (canToggleRecording) onRecordToggle() },
            expanded = true,
            containerColor = containerColor,
            contentColor = labelColor
        )
    }
}

@Composable
private fun RecordingCard(state: HomeUiState) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AssistChip(
                onClick = { },
                label = { Text("Recording") }
            )
            Text(
                text = formatDuration(state.elapsedSeconds),
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Speak freely—mind vault is listening.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Waveform(levels = state.waveLevels)
        }
    }
}

@Composable
private fun TranscriptSection(
    transcript: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    isTranscribing: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Transcript", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = transcript,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            enabled = enabled,
            placeholder = { Text("Your words will appear here…") }
        )
        AnimatedVisibility(visible = isTranscribing, enter = fadeIn(), exit = fadeOut()) {
            AssistChip(onClick = {}, label = { Text("transcribing…") })
        }
    }
}

@Composable
private fun SummarySection(
    summary: String,
    isSummarizing: Boolean,
    onSummarize: () -> Unit,
    canSummarize: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RowWithAction(
            title = "Summary",
            actionLabel = if (isSummarizing) "Summarizing…" else "Summarize",
            onAction = onSummarize,
            enabled = canSummarize && !isSummarizing,
            showProgress = isSummarizing
        )
        ElevatedCard {
            Column(modifier = Modifier.padding(20.dp)) {
                if (summary.isBlank()) {
                    Text(
                        text = "Tap summarize to generate a recap.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyLarge,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MoodChips(
    moods: List<String>,
    selectedMood: String?,
    onMoodPick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Mood", style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            moods.forEach { mood ->
                val isSelected = mood == selectedMood
                AssistChip(
                    onClick = { onMoodPick(mood) },
                    label = { Text(mood.replaceFirstChar { it.uppercase() }) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(imageVector = Icons.Outlined.GraphicEq, contentDescription = null)
                        }
                    } else null,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String, onDismissError: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Something went wrong", style = MaterialTheme.typography.titleLarge)
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDismissError) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun RowWithAction(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    enabled: Boolean,
    showProgress: Boolean
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = onAction,
            enabled = enabled,
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp
                )
            }
            Text(text = actionLabel)
        }
    }
}

@Composable
private fun Waveform(levels: List<Float>) {
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        if (levels.isEmpty()) return@Canvas
        val barCount = levels.size
        val sliceWidth = size.width / max(1, barCount)
        levels.forEachIndexed { index, level ->
            val centerX = index * sliceWidth + sliceWidth / 2f
            val clamped = level.coerceIn(0f, 1f)
            val barHeight = clamped * size.height
            drawLine(
                color = barColor,
                start = Offset(centerX, size.height / 2f - barHeight / 2f),
                end = Offset(centerX, size.height / 2f + barHeight / 2f),
                strokeWidth = sliceWidth * 0.6f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

@Preview(name = "Home")
@Composable
private fun HomePreview() {
    val sampleState = HomeUiState(
        modelsReady = true,
        isRecording = true,
        transcript = "Today I reflected on how mindful breathing helps me reset between meetings…",
        summary = "You described practicing mindful breathing between meetings to stay grounded.",
        mood = "calm",
        waveLevels = List(32) { 0.2f + (it % 4) * 0.15f },
        elapsedSeconds = 42
    )
    com.mindvault.ai.ui.theme.MindVaultTheme {
        HomeScreen(
            state = sampleState,
            onInitializeModels = {},
            onRecordToggle = {},
            onTranscriptChange = {},
            onSummarize = {},
            onSave = {},
            onMoodPick = {},
            onDismissError = {}
        )
    }
}
