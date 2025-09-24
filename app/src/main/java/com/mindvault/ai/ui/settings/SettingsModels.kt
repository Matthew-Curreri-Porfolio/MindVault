package com.mindvault.ai.ui.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val userEmail: String? = null,
    val lastSyncTimestamp: Long? = null,
    val isSyncEnabled: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val localIndexingEnabled: Boolean = true,
    val modelThreads: Int = Runtime.getRuntime().availableProcessors().coerceAtMost(8),
    val whisperModelPath: String = "",
    val llamaModelPath: String = "",
    val cacheSizeMb: Int = 0,
    val appVersion: String = "",
    val isClearingCache: Boolean = false,
    val statusMessage: String? = null
) {
    val syncStatusText: String
        get() = when {
            lastSyncTimestamp == null -> "Never synced"
            else -> "Last synced " + android.text.format.DateUtils.getRelativeTimeSpanString(lastSyncTimestamp)
        }
}
