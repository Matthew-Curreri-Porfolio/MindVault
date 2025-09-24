package com.mindvault.ai.ui.entries

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EntriesScreen(
    state: EntriesUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onMoodFilter: (String?) -> Unit,
    onHasAudioToggle: () -> Unit,
    onDateRangeChange: (Long?, Long?) -> Unit,
    onResetFilters: () -> Unit,
    onOpenEntry: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchChange,
            onClear = onClearSearch
        )

        FilterRow(
            state = state,
            onMoodFilter = onMoodFilter,
            onHasAudioToggle = onHasAudioToggle,
            onDateRangeChange = onDateRangeChange,
            onResetFilters = onResetFilters
        )

        when {
            state.isLoading -> LoadingSection(modifier = Modifier.weight(1f, fill = true))
            state.isEmpty -> EmptySection(modifier = Modifier.weight(1f, fill = true))
            else -> EntryList(
                entries = state.entries,
                onOpenEntry = onOpenEntry,
                modifier = Modifier.weight(1f, fill = true)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Clear search")
                }
            }
        },
        placeholder = { Text("Search transcripts & summaries") },
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    state: EntriesUiState,
    onMoodFilter: (String?) -> Unit,
    onHasAudioToggle: () -> Unit,
    onDateRangeChange: (Long?, Long?) -> Unit,
    onResetFilters: () -> Unit
) {
    val filter = state.filter
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val rangeLabel = remember(filter.fromMillis, filter.toMillis) {
        val start = filter.fromMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        val end = filter.toMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        when {
            start != null && end != null -> "${dateFormatter.format(start)} – ${dateFormatter.format(end)}"
            start != null -> ">= ${dateFormatter.format(start)}"
            end != null -> "<= ${dateFormatter.format(end)}"
            else -> "Date range"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Filters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            AnimatedVisibility(visible = filter.isActive, enter = fadeIn(), exit = fadeOut()) {
                TextButton(onClick = onResetFilters) { Text("Reset") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filter.onlyWithAudio,
                onClick = onHasAudioToggle,
                label = { Text("Has audio") },
                leadingIcon = if (filter.onlyWithAudio) {
                    { Icon(imageVector = Icons.Outlined.LibraryMusic, contentDescription = null) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            FilterChip(
                selected = filter.fromMillis != null || filter.toMillis != null,
                onClick = { showDatePicker = true },
                label = { Text(rangeLabel) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null)
                }
            )
        }

        FlowingMoodRow(
            available = state.availableMoods,
            selectedMood = filter.mood,
            onSelect = { mood -> onMoodFilter(if (filter.mood == mood) null else mood) }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.filter.fromMillis,
            initialSelectedEndDateMillis = state.filter.toMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    onDateRangeChange(pickerState.selectedStartDateMillis, pickerState.selectedEndDateMillis)
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pickerState.setSelection(null, null)
                    onDateRangeChange(null, null)
                    showDatePicker = false
                }) { Text("Clear") }
            }
        ) {
            DateRangePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowingMoodRow(
    available: List<String>,
    selectedMood: String?,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        available.forEach { mood ->
            val isSelected = mood.equals(selectedMood, ignoreCase = true)
            AssistChip(
                onClick = { onSelect(mood) },
                label = { Text(mood.replaceFirstChar { it.titlecase() }) },
                leadingIcon = if (isSelected) {
                    { Icon(imageVector = Icons.Outlined.FilterAlt, contentDescription = null) }
                } else null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun LoadingSection(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptySection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text(text = "Nothing yet", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Start a voice journal from Home and entries will appear here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun EntryList(entries: List<EntryListItem>, onOpenEntry: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            EntryCard(entry = entry, onClick = { onOpenEntry(entry.id) })
        }
    }
}

@Composable
private fun EntryCard(entry: EntryListItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.mood.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(entry.mood.replaceFirstChar { it.titlecase() }) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Text(
                text = entry.snippet,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateLabel = remember(entry.createdAtMillis) {
                    Instant.ofEpochMilli(entry.createdAtMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                }
                Text(text = dateLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.hasAudio) {
                    Icon(imageVector = Icons.Outlined.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntriesScreenPreview() {
    val sampleEntries = List(3) { index ->
        EntryListItem(
            id = "entry-$index",
            title = "Morning reflection #${index + 1}",
            snippet = "You talked about practicing gratitude and mindful breathing before starting the day to stay focused.",
            mood = if (index % 2 == 0) "calm" else "happy",
            createdAtMillis = System.currentTimeMillis() - index * 86_400_000L,
            hasAudio = index % 2 == 0,
            summary = "",
            transcript = ""
        )
    }
    val previewState = EntriesUiState(
        searchQuery = "",
        filter = EntriesFilter(onlyWithAudio = false),
        entries = sampleEntries,
        isLoading = false,
        isEmpty = false
    )
    com.mindvault.ai.ui.theme.MindVaultTheme {
        EntriesScreen(
            state = previewState,
            onSearchChange = {},
            onClearSearch = {},
            onMoodFilter = {},
            onHasAudioToggle = {},
            onDateRangeChange = { _, _ -> },
            onResetFilters = {},
            onOpenEntry = {}
        )
    }
}
