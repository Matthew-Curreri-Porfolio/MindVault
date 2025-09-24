package com.mindvault.ai.ui.detail

import android.app.Application
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindvault.ai.LlamaBridge
import com.mindvault.ai.data.db.JournalEntry
import com.mindvault.ai.data.repo.EntryRepository
import com.mindvault.ai.ml.ModelInstaller
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntryDetailViewModel(
    application: Application,
    private val entryId: String
) : AndroidViewModel(application) {
    private val repository = EntryRepository(application)

    private val _uiState = MutableStateFlow(EntryDetailUiState(entryId = entryId))
    val uiState: StateFlow<EntryDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EntryDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EntryDetailEvent> = _events

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val modelInitGuard = AtomicBoolean(false)

    private var currentEntry: JournalEntry? = null
    private var llamaHandle: Long = 0L
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        observeEntry()
    }

    private fun observeEntry() {
        viewModelScope.launch(ioDispatcher) {
            repository.observe(entryId).collectLatest { entry ->
                if (entry == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Entry not found"
                        )
                    }
                } else {
                    currentEntry = entry
                    val audioPath = entry.audioPath.takeIf { it.isNotBlank() }
                    _uiState.update { state ->
                        val keepContent = state.isDirty
                        state.copy(
                            isLoading = false,
                            entryId = entry.id,
                            createdAtMillis = entry.createdAt,
                            summary = if (keepContent) state.summary else entry.summary,
                            transcript = if (keepContent) state.transcript else entry.transcript,
                            mood = if (keepContent) state.mood else entry.mood,
                            audioPath = audioPath,
                            hasAudio = audioPath != null
                        )
                    }
                    if (audioPath != null) {
                        preparePlayer(audioPath)
                    } else {
                        releasePlayer()
                    }
                }
            }
        }
    }

    fun onTranscriptChange(value: String) {
        _uiState.update { it.copy(transcript = value, isDirty = true) }
    }

    fun onMoodChange(mood: String) {
        _uiState.update { it.copy(mood = mood, isDirty = true) }
    }

    fun onSummarize() {
        val transcript = uiState.value.transcript
        if (transcript.isBlank()) return
        viewModelScope.launch {
            ensureModels()
            _uiState.update { it.copy(isSummarizing = true) }
            val result = runCatching {
                withContext(ioDispatcher) {
                    val summaryPrompt = "Improve this summary with 2-3 sentences highlighting key insights:\n$transcript\nSummary:"
                    LlamaBridge.generate(
                        llamaHandle,
                        summaryPrompt,
                        120,
                        0.2f,
                        0.9f
                    ).trim()
                }
            }
            result.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        summary = if (summary.isBlank()) it.summary else summary,
                        isSummarizing = false,
                        isDirty = true,
                        statusMessage = "Summary updated"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSummarizing = false,
                        errorMessage = throwable.localizedMessage ?: "Unable to summarize"
                    )
                }
            }
        }
    }

    fun onRedact() {
        val transcript = uiState.value.transcript
        if (transcript.isBlank()) return
        viewModelScope.launch {
            val redacted = withContext(ioDispatcher) { redactText(transcript) }
            _uiState.update {
                it.copy(
                    transcript = redacted,
                    isDirty = true,
                    statusMessage = "PII redacted"
                )
            }
        }
    }

    fun onSave() {
        val entry = currentEntry ?: return
        val state = uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(ioDispatcher) {
            val updated = entry.copy(
                summary = state.summary,
                transcript = state.transcript,
                mood = state.mood ?: entry.mood
            )
            runCatching {
                repository.upsert(updated)
            }.onSuccess {
                currentEntry = updated
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isDirty = false,
                        statusMessage = "Changes saved"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.localizedMessage ?: "Unable to save"
                    )
                }
            }
        }
    }

    fun onDelete() {
        val entry = currentEntry ?: return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch(ioDispatcher) {
            runCatching { repository.delete(entry.id) }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.tryEmit(EntryDetailEvent.Deleted)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = throwable.localizedMessage ?: "Unable to delete entry"
                        )
                    }
                }
        }
    }

    fun onShareRequested() {
        val state = uiState.value
        val content = buildString {
            appendLine("Summary:")
            appendLine(state.summary.ifBlank { "(none)" })
            appendLine()
            appendLine("Transcript:")
            append(state.transcript.ifBlank { "(empty)" })
        }
        _events.tryEmit(EntryDetailEvent.Share(content.trim()))
    }

    fun onExport(format: EntryExportFormat) {
        val entry = currentEntry ?: return
        viewModelScope.launch(ioDispatcher) {
            val context = getApplication<Application>()
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val filenameBase = "entry_${entry.createdAt}"
            runCatching {
                val file = when (format) {
                    EntryExportFormat.TXT -> File(exportsDir, "$filenameBase.txt").apply {
                        writeText(buildExportBody(entry), Charsets.UTF_8)
                    }
                    EntryExportFormat.PDF -> File(exportsDir, "$filenameBase.pdf").also { exportPdf(entry, it) }
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mimeType = when (format) {
                    EntryExportFormat.TXT -> "text/plain"
                    EntryExportFormat.PDF -> "application/pdf"
                }
                EntryDetailEvent.Export(uri, mimeType)
            }.onSuccess { event ->
                _events.emit(event)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(errorMessage = throwable.localizedMessage ?: "Unable to export entry")
                }
            }
        }
    }

    fun onPlaybackToggle() {
        val path = uiState.value.audioPath ?: run {
            _uiState.update { it.copy(errorMessage = "No audio available") }
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            val player = mediaPlayer ?: preparePlayer(path)
            if (player == null) {
                _uiState.update { it.copy(errorMessage = "Unable to play audio") }
                return@launch
            }
            if (player.isPlaying) {
                player.pause()
                stopProgressUpdates()
                _uiState.update { it.copy(isPlaying = false) }
            } else {
                player.start()
                startProgressUpdates()
                _uiState.update { it.copy(isPlaying = true, playbackDurationMs = player.duration) }
            }
        }
    }

    fun onSeek(positionMs: Int) {
        mediaPlayer?.let { player ->
            if (uiState.value.hasAudio) {
                player.seekTo(positionMs)
                _uiState.update { it.copy(playbackPositionMs = positionMs) }
            }
        }
    }

    fun onStatusConsumed() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun onErrorConsumed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
        if (llamaHandle != 0L) {
            runCatching { LlamaBridge.free(llamaHandle) }
            llamaHandle = 0L
        }
    }

    private suspend fun ensureModels() {
        if (llamaHandle != 0L) return
        if (!modelInitGuard.compareAndSet(false, true)) return
        val result = runCatching {
            withContext(ioDispatcher) {
                val context = getApplication<Application>()
                val modelFile = ModelInstaller.ensureModel(context, QWEN_MODEL)
                val threads = Runtime.getRuntime().availableProcessors().coerceAtMost(8)
                LlamaBridge.initModel(modelFile.absolutePath, 1024, threads)
            }
        }
        result.onSuccess { handle ->
            llamaHandle = handle
            if (handle == 0L) {
                _uiState.update { it.copy(errorMessage = "Model initialization failed") }
            }
        }.onFailure { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.localizedMessage ?: "Unable to initialize model") }
        }
    }

    private suspend fun preparePlayer(path: String): MediaPlayer? {
        return withContext(Dispatchers.Main) {
            try {
                val player = mediaPlayer ?: MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(path)
                    prepare()
                    setOnCompletionListener {
                        stopProgressUpdates()
                        _uiState.update { it.copy(isPlaying = false, playbackPositionMs = 0) }
                    }
                    mediaPlayer = this
                }
                _uiState.update { it.copy(playbackDurationMs = player.duration, playbackPositionMs = player.currentPosition) }
                player
            } catch (throwable: Throwable) {
                releasePlayer()
                _uiState.update { it.copy(errorMessage = throwable.localizedMessage ?: "Unable to prepare audio") }
                null
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch(Dispatchers.Main) {
            while (mediaPlayer?.isPlaying == true) {
                val position = mediaPlayer?.currentPosition ?: 0
                _uiState.update { it.copy(playbackPositionMs = position) }
                delay(250)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releasePlayer() {
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        _uiState.update { it.copy(isPlaying = false, playbackPositionMs = 0, playbackDurationMs = 0) }
    }

    private fun buildExportBody(entry: JournalEntry): String {
        return buildString {
            appendLine("MindVault Entry")
            appendLine("Date: ${formatDate(entry.createdAt)}")
            appendLine("Mood: ${entry.mood.ifBlank { "unspecified" }}")
            appendLine()
            appendLine("Summary")
            appendLine(entry.summary.ifBlank { "(none)" })
            appendLine()
            appendLine("Transcript")
            append(entry.transcript.ifBlank { "(empty)" })
        }
    }

    private fun exportPdf(entry: JournalEntry, file: File) {
        val document = PdfDocument()
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
        }
        val leftMargin = 40f
        val topMargin = 60f
        val pageWidth = 595
        val pageHeight = 842
        val maxWidth = pageWidth - (leftMargin * 2)

        var pageIndex = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
        var canvas = page.canvas
        var cursorY = topMargin

        val lines = buildExportBody(entry).lines()
        lines.forEach { line ->
            val chunks = wrapText(line, paint, maxWidth)
            chunks.forEach { chunk ->
                if (cursorY >= pageHeight - topMargin) {
                    document.finishPage(page)
                    pageIndex += 1
                    page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
                    canvas = page.canvas
                    cursorY = topMargin
                }
                canvas.drawText(chunk, leftMargin, cursorY, paint)
                cursorY += paint.textSize + 6f
            }
        }
        document.finishPage(page)
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else current.toString() + " " + word
            if (paint.measureText(candidate) <= maxWidth) {
                if (current.isNotEmpty()) current.append(' ')
                current.append(word)
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                if (paint.measureText(word) <= maxWidth) {
                    current = StringBuilder(word)
                } else {
                    lines.addAll(hardWrap(word, paint, maxWidth))
                    current = StringBuilder()
                }
            }
        }
        if (current.isNotEmpty()) {
            lines += current.toString()
        }
        return if (lines.isEmpty()) listOf(text) else lines
    }

    private fun hardWrap(word: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            while (end > start && paint.measureText(word, start, end) > maxWidth) {
                end--
            }
            if (end == start) {
                end = min(word.length, start + 1)
            }
            result += word.substring(start, end)
            start = end
        }
        return result
    }

    private fun formatDate(millis: Long): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        return java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }

    private fun redactText(input: String): String {
        var output = input
        // Mask email addresses
        output = EMAIL_REGEX.replace(output) { matchResult ->
            val email = matchResult.value
            val atIndex = email.indexOf('@')
            if (atIndex <= 1) "***@***" else email.first() + "***@***"
        }
        // Mask phone-like numbers
        output = PHONE_REGEX.replace(output) {
            "***"
        }
        // Mask sequences of digits >= 4 (account numbers, etc.)
        output = DIGIT_REGEX.replace(output) {
            "[redacted]"
        }
        return output
    }

    companion object {
        private const val QWEN_MODEL = "Qwen3-4B-Q4_K_M.gguf"
        private val EMAIL_REGEX = Regex("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+")
        private val PHONE_REGEX = Regex("(?:(?:\\+?\\d{1,3})?[-.\\s]?)?(?:\\(\\d{3}\\)|\\d{3})[-.\\s]?\\d{3}[-.\\s]?\\d{4}")
        private val DIGIT_REGEX = Regex("\\d{4,}")

        fun provideFactory(application: Application, entryId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(EntryDetailViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return EntryDetailViewModel(application, entryId) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
