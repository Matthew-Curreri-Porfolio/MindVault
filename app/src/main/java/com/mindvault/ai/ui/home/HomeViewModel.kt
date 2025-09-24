package com.mindvault.ai.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.ai.LlamaBridge
import com.mindvault.ai.ml.ModelInstaller
import com.mindvault.ai.WhisperBridge
import com.mindvault.ai.audio.AudioRecorder
import com.mindvault.ai.data.repo.EntryRepository
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayDeque
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EntryRepository(application)
    private val audioRecorder = AudioRecorder()
    private val isInitializing = AtomicBoolean(false)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var llamaHandle: Long = 0L
    private var whisperHandle: Long = 0L
    private var currentAudioFile: File? = null
    private var waveformJob: Job? = null
    private var timerJob: Job? = null

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    fun onInitializeModels() {
        if (uiState.value.modelsReady || uiState.value.isInitializingModels) return
        viewModelScope.launch {
            initializeModelsInternal()
        }
    }

    fun onRecordToggle() {
        if (uiState.value.isRecording) {
            stopRecordingAndTranscribe()
        } else {
            viewModelScope.launch {
                val ready = ensureModels()
                if (!ready) return@launch
                startRecording()
            }
        }
    }

    fun onTranscriptChange(value: String) {
        _uiState.update { it.copy(transcript = value) }
    }

    fun onSummarize() {
        if (!uiState.value.canSummarize) return
        viewModelScope.launch {
            val transcript = uiState.value.transcript
            summarize(transcript, auto = false)
        }
    }

    fun onMoodPick(mood: String) {
        _uiState.update { it.copy(mood = mood) }
    }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    repository.save(
                        transcript = state.transcript,
                        summary = state.summary,
                        mood = state.mood ?: "neutral",
                        audioPath = state.audioPath.orEmpty()
                    )
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(statusMessage = "Entry saved to vault")
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(errorMessage = throwable.localizedMessage ?: "Unable to save entry")
                }
            }
        }
    }

    fun onErrorConsumed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onStatusConsumed() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopRecorder()
        releaseModels()
    }

    private suspend fun ensureModels(): Boolean {
        if (uiState.value.modelsReady) return true
        initializeModelsInternal()
        return uiState.value.modelsReady
    }

    private suspend fun initializeModelsInternal() {
        if (!isInitializing.compareAndSet(false, true)) return
        _uiState.update { it.copy(isInitializingModels = true, errorMessage = null, statusMessage = null) }
        val result = runCatching {
            withContext(ioDispatcher) {
                val context = getApplication<Application>()
                val llamaModel = ModelInstaller.ensureModel(context, QWEN_MODEL)
                val whisperModel = ModelInstaller.ensureModel(context, WHISPER_MODEL)
                val cores = Runtime.getRuntime().availableProcessors()
                val llamaThreads = cores.coerceAtMost(8)
                val whisperThreads = cores.coerceAtMost(6)
                val llama = LlamaBridge.initModel(llamaModel.absolutePath, 1024, llamaThreads)
                val whisper = WhisperBridge.initModel(whisperModel.absolutePath, whisperThreads)
                Pair(llama, whisper)
            }
        }
        result.onSuccess { (llama, whisper) ->
            llamaHandle = llama
            whisperHandle = whisper
            _uiState.update { it.copy(modelsReady = llama != 0L && whisper != 0L) }
            if (llama == 0L || whisper == 0L) {
                _uiState.update { it.copy(errorMessage = "Model initialization failed") }
            }
        }.onFailure { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.localizedMessage ?: "Unable to initialize models") }
        }
        _uiState.update { it.copy(isInitializingModels = false) }
        isInitializing.set(false)
    }

    private suspend fun startRecording() {
        val audioDir = File(getApplication<Application>().filesDir, "audio").apply { mkdirs() }
        val output = File(audioDir, "session_${System.currentTimeMillis()}.wav")
        runCatching {
            withContext(Dispatchers.Default) {
                audioRecorder.start(output.absolutePath)
            }
        }.onSuccess {
            currentAudioFile = output
            _uiState.update {
                it.copy(
                    isRecording = true,
                    elapsedSeconds = 0,
                    waveLevels = emptyList(),
                    audioPath = output.absolutePath,
                    statusMessage = null,
                    errorMessage = null
                )
            }
            startWaveformSimulation()
            startTimer()
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(errorMessage = throwable.localizedMessage ?: "Unable to start recording")
            }
        }
    }

    private fun stopRecordingAndTranscribe() {
        stopRecorder()
        val audio = currentAudioFile
        if (audio == null) {
            _uiState.update { it.copy(isRecording = false) }
            return
        }
        _uiState.update { it.copy(isRecording = false, isTranscribing = true) }
        viewModelScope.launch {
            val transcript = runCatching {
                withContext(ioDispatcher) {
                    WhisperBridge.transcribe(
                        whisperHandle,
                        audio.absolutePath,
                        Runtime.getRuntime().availableProcessors().coerceAtMost(6)
                    )
                }
            }
            transcript.onSuccess { text ->
                if (text.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isTranscribing = false,
                            transcript = "",
                            errorMessage = "Transcription unavailable"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isTranscribing = false,
                            transcript = text,
                            statusMessage = null
                        )
                    }
                    summarize(text, auto = true)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isTranscribing = false,
                        errorMessage = throwable.localizedMessage ?: "Transcription failed"
                    )
                }
            }
        }
    }

    private fun stopRecorder() {
        runCatching { audioRecorder.stop() }
        waveformJob?.cancel()
        timerJob?.cancel()
        waveformJob = null
        timerJob = null
        _uiState.update { it.copy(waveLevels = emptyList()) }
    }

    private fun startWaveformSimulation() {
        waveformJob?.cancel()
        waveformJob = viewModelScope.launch(Dispatchers.Default) {
            val deque = ArrayDeque<Float>()
            val random = Random(System.currentTimeMillis())
            while (isActive) {
                delay(50)
                val level = random.nextFloat().coerceIn(0.05f, 1f)
                if (deque.size >= 40) deque.removeFirst()
                deque.addLast(level)
                val snapshot = deque.toList()
                _uiState.update { it.copy(waveLevels = snapshot) }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var seconds = 0
            while (isActive) {
                delay(1_000)
                seconds += 1
                _uiState.update { it.copy(elapsedSeconds = seconds) }
            }
        }
    }

    private suspend fun summarize(transcript: String, auto: Boolean) {
        if (transcript.isBlank()) return
        _uiState.update { it.copy(isSummarizing = true) }
        val summaryResult = runCatching {
            withContext(ioDispatcher) {
                val summaryPrompt = "Summarize in 2 sentences:\n$transcript\nSummary:"
                val summary = LlamaBridge.generate(
                    llamaHandle,
                    summaryPrompt,
                    96,
                    0.2f,
                    0.9f
                ).trim()
                val moodPrompt = "Choose ONE word from [calm,happy,anxious,sad,angry,stressed,grateful,tired]. Entry:\n$transcript\nMood:"
                val mood = LlamaBridge.generate(
                    llamaHandle,
                    moodPrompt,
                    6,
                    0.0f,
                    1.0f
                ).trim().lowercase()
                summary to mood
            }
        }
        summaryResult.onSuccess { (summary, moodRaw) ->
            val targetState = uiState.value
            val normalizedMood = targetState.availableMoods.firstOrNull { moodRaw.contains(it) } ?: moodRaw
            val safeSummary = if (summary.isBlank()) targetState.summary else summary
            val safeMood = normalizedMood.ifBlank { targetState.mood ?: "neutral" }
            _uiState.update {
                it.copy(
                    isSummarizing = false,
                    summary = safeSummary,
                    mood = safeMood,
                    statusMessage = if (auto) "Summary generated" else "Summary updated"
                )
            }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    isSummarizing = false,
                    errorMessage = throwable.localizedMessage ?: "Summarization failed"
                )
            }
        }
    }

    private fun releaseModels() {
        if (llamaHandle != 0L) {
            runCatching { LlamaBridge.free(llamaHandle) }
            llamaHandle = 0L
        }
        if (whisperHandle != 0L) {
            runCatching { WhisperBridge.free(whisperHandle) }
            whisperHandle = 0L
        }
    }

    companion object {
        private const val QWEN_MODEL = "Qwen3-4B-Q4_K_M.gguf"
        private const val WHISPER_MODEL = "ggml-base.en-q5_1.bin"
    }
}
