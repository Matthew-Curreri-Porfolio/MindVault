package com.mindvault.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onToggleSync: (Boolean) -> Unit,
    onToggleAnalytics: (Boolean) -> Unit,
    onToggleIndexing: (Boolean) -> Unit,
    onThreadsChanged: (Int) -> Unit,
    onClearCache: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Text(text = "Settings", style = MaterialTheme.typography.displayLarge) }

        item {
            SettingsCard(title = "Account & Sync") {
                Text(text = state.userEmail ?: "Not signed in", style = MaterialTheme.typography.titleLarge)
                Text(text = state.syncStatusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ListItem(
                    headlineContent = { Text("Enable encrypted sync") },
                    supportingContent = { Text("Sync entries securely across devices") },
                    trailingContent = {
                        Switch(checked = state.isSyncEnabled, onCheckedChange = onToggleSync)
                    }
                )
                TextButton(onClick = { /* TODO: navigate to login */ }, enabled = state.userEmail == null) {
                    Text(text = "Login")
                }
                TextButton(onClick = { /* TODO: logout */ }, enabled = state.userEmail != null) {
                    Text(text = "Logout")
                }
            }
        }

        item {
            SettingsCard(title = "Privacy") {
                ListItem(
                    headlineContent = { Text("Local indexing") },
                    supportingContent = { Text("Allow offline search to tag and surface entries quickly.") },
                    trailingContent = {
                        Switch(checked = state.localIndexingEnabled, onCheckedChange = onToggleIndexing)
                    }
                )
                androidx.compose.material3.HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Analytics") },
                    supportingContent = { Text("Share anonymous usage metrics to help improve MindVault.") },
                    trailingContent = {
                        Switch(checked = state.analyticsEnabled, onCheckedChange = onToggleAnalytics)
                    }
                )
            }
        }

        item {
            SettingsCard(title = "Models") {
                Text(text = "Threads", style = MaterialTheme.typography.titleMedium)
                val threadsRange = 1..Runtime.getRuntime().availableProcessors().coerceAtMost(8)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "${state.modelThreads} threads")
                    androidx.compose.material3.Slider(
                        value = state.modelThreads.toFloat(),
                        onValueChange = { value -> onThreadsChanged(value.roundToInt()) },
                        valueRange = threadsRange.first.toFloat()..threadsRange.last.toFloat(),
                        steps = threadsRange.count() - 2
                    )
                }
                androidx.compose.material3.HorizontalDivider()
                Text(text = "Whisper model", style = MaterialTheme.typography.titleMedium)
                Text(text = state.whisperModelPath.ifBlank { "Not installed" }, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Qwen model", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Text(text = state.llamaModelPath.ifBlank { "Not installed" }, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            SettingsCard(title = "Storage") {
                Text(text = "Cache size: ${state.cacheSizeMb} MB", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onClearCache, enabled = !state.isClearingCache) {
                    if (state.isClearingCache) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    Text(text = if (state.isClearingCache) "Clearing…" else "Clear cache")
                }
            }
        }

        item {
            SettingsCard(title = "About") {
                Text(text = "MindVault ${state.appVersion}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Private voice journaling with on-device AI.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            content()
        })
    }
}
