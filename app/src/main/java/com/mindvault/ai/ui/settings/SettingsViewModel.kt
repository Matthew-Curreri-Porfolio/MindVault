package com.mindvault.ai.ui.settings

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SettingsUiState(appVersion = resolveVersionName()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // pretend to fetch persisted preferences
        viewModelScope.launch {
            delay(150)
            _uiState.update {
                it.copy(
                    userEmail = "you@example.com",
                    lastSyncTimestamp = System.currentTimeMillis() - 86_400_000L,
                    isSyncEnabled = true,
                    analyticsEnabled = false,
                    localIndexingEnabled = true,
                    whisperModelPath = "storage/emulated/0/MindVault/Models/whisper.bin",
                    llamaModelPath = "storage/emulated/0/MindVault/Models/qwen.gguf",
                    cacheSizeMb = 512
                )
            }
        }
    }

    fun onToggleSync(enabled: Boolean) {
        _uiState.update { it.copy(isSyncEnabled = enabled, statusMessage = if (enabled) "Sync enabled" else "Sync disabled") }
    }

    fun onToggleAnalytics(enabled: Boolean) {
        _uiState.update { it.copy(analyticsEnabled = enabled, statusMessage = if (enabled) "Analytics on" else "Analytics off") }
    }

    fun onToggleIndexing(enabled: Boolean) {
        _uiState.update { it.copy(localIndexingEnabled = enabled, statusMessage = if (enabled) "Indexing on" else "Indexing off") }
    }

    fun onThreadsChanged(count: Int) {
        _uiState.update { it.copy(modelThreads = count, statusMessage = "Threads set to $count") }
    }

    fun onClearCache() {
        if (_uiState.value.isClearingCache) return
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true) }
            delay(800)
            _uiState.update { it.copy(isClearingCache = false, cacheSizeMb = 0, statusMessage = "Cache cleared") }
        }
    }

    fun onStatusConsumed() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun resolveVersionName(): String {
        return try {
            val pm = getApplication<Application>().packageManager
            val pkg = getApplication<Application>().packageName
            val info = pm.getPackageInfo(pkg, 0)
            info.versionName ?: ""
        } catch (exception: PackageManager.NameNotFoundException) {
            ""
        }
    }
}
