package com.mindvault.ai.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun EntryDetailScreen(
    state: EntryDetailUiState,
    onTranscriptChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onSummarize: () -> Unit,
    onRedact: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onExportTxt: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit,
    onPlaybackToggle: () -> Unit,
    onSeek: (Int) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val dateLabel = remember(state.createdAtMillis) {
        if (state.createdAtMillis == 0L) "" else Instant.ofEpochMilli(state.createdAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = dateLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Entry", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.SemiBold)
                        state.mood?.takeIf { it.isNotBlank() }?.let { moodLabel ->
                            AnimatedVisibility(visible = true) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(moodLabel.replaceFirstChar { it.titlecase() }) },
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

            item {
                SummaryCard(
                    summary = state.summary,
                    isSummarizing = state.isSummarizing,
                    onSummarize = onSummarize,
                    onRedact = onRedact
                )
            }

            item {
                MoodSelector(
                    moods = state.availableMoods,
                    selectedMood = state.mood,
                    onMoodChange = onMoodChange
                )
            }

            item {
                TranscriptEditor(
                    transcript = state.transcript,
                    onTranscriptChange = onTranscriptChange
                )
            }

            if (state.hasAudio) {
                item {
                    AudioPlayerSection(
                        isPlaying = state.isPlaying,
                        durationMs = state.playbackDurationMs,
                        positionMs = state.playbackPositionMs,
                        onToggle = onPlaybackToggle,
                        onSeek = onSeek
                    )
                }
            }

            item {
                ActionRow(
                    onShare = onShare,
                    onExportTxt = onExportTxt,
                    onExportPdf = onExportPdf
                )
            }

            item {
                SaveRow(
                    canSave = state.canSave,
                    isSaving = state.isSaving,
                    onSave = onSave
                )
            }

            item {
                DeleteRow(onDelete = onDelete, isDeleting = state.isDeleting)
            }
        }

        AnimatedVisibility(visible = state.isLoading, modifier = Modifier.align(Alignment.Center)) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SummaryCard(
    summary: String,
    isSummarizing: Boolean,
    onSummarize: () -> Unit,
    onRedact: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Summary", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onSummarize, enabled = !isSummarizing) {
                        if (isSummarizing) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        }
                        Text(text = if (isSummarizing) "Summarizing…" else "Improve summary")
                    }
                    TextButton(onClick = onRedact, enabled = !isSummarizing) {
                        Icon(imageVector = Icons.Outlined.Shield, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Redact PII")
                    }
                }
            }
            Text(
                text = summary.ifBlank { "Generate a summary to distill this journal entry." },
                style = MaterialTheme.typography.bodyLarge,
                color = if (summary.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MoodSelector(
    moods: List<String>,
    selectedMood: String?,
    onMoodChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Mood", style = MaterialTheme.typography.titleLarge)
        FlowingMoods(moods = moods, selectedMood = selectedMood, onMoodChange = onMoodChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowingMoods(
    moods: List<String>,
    selectedMood: String?,
    onMoodChange: (String) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        moods.forEach { mood ->
            val isSelected = mood.equals(selectedMood, ignoreCase = true)
            AssistChip(
                onClick = { onMoodChange(mood) },
                label = { Text(mood.replaceFirstChar { it.titlecase() }) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun TranscriptEditor(
    transcript: String,
    onTranscriptChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Transcript", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = transcript,
            onValueChange = onTranscriptChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            placeholder = { Text("Add reflections or edits here…") }
        )
    }
}

@Composable
private fun AudioPlayerSection(
    isPlaying: Boolean,
    durationMs: Int,
    positionMs: Int,
    onToggle: () -> Unit,
    onSeek: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Playback", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onToggle) {
                    Icon(imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = positionMs.toFloat(),
                        onValueChange = { onSeek(it.toInt()) },
                        valueRange = 0f..max(1f, durationMs.toFloat())
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTimestamp(positionMs), style = MaterialTheme.typography.labelMedium)
                        Text(formatTimestamp(durationMs), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    onShare: () -> Unit,
    onExportTxt: () -> Unit,
    onExportPdf: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Actions", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onShare, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Outlined.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
            Button(onClick = onExportTxt, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export TXT")
            }
            Button(onClick = onExportPdf, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export PDF")
            }
        }
    }
}

@Composable
private fun SaveRow(
    canSave: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Button(
        onClick = onSave,
        enabled = canSave && !isSaving,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = if (isSaving) "Saving…" else "Save changes")
    }
}

@Composable
private fun DeleteRow(onDelete: () -> Unit, isDeleting: Boolean) {
    var confirm by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = { confirm = true },
            enabled = !isDeleting
        ) {
            Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isDeleting) "Deleting…" else "Delete entry", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete entry") },
            text = { Text("This will remove the entry and its audio permanently. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatTimestamp(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun EntryDetailPreview() {
    val previewState = EntryDetailUiState(
        isLoading = false,
        createdAtMillis = System.currentTimeMillis(),
        summary = "You reflected on how breathing exercises helped you reset between intense meetings.",
        transcript = "Today I practiced mindful breathing between meetings. It helped me stay calm and focused even though the day was intense.",
        mood = "calm",
        hasAudio = true,
        playbackDurationMs = 180000,
        playbackPositionMs = 30000,
        isPlaying = false
    )
    com.mindvault.ai.ui.theme.MindVaultTheme {
        EntryDetailScreen(
            state = previewState,
            onTranscriptChange = {},
            onMoodChange = {},
            onSummarize = {},
            onRedact = {},
            onSave = {},
            onShare = {},
            onExportTxt = {},
            onExportPdf = {},
            onDelete = {},
            onPlaybackToggle = {},
            onSeek = {}
        )
    }
}
