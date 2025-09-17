package com.mindvault.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mindvault.ai.audio.AudioRecorder
import com.mindvault.ai.data.repo.EntryRepository
import com.mindvault.ai.ml.ModelInstaller
import com.mindvault.ai.ml.SyncClient
import com.mindvault.ai.ml.TokenStore
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var audioRecorder: AudioRecorder? = null
    private var llamaHandle: Long = 0
    private var whisperHandle: Long = 0
    private lateinit var repo: EntryRepository

    private val reqPerms = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this, "Mic permission required", Toast.LENGTH_SHORT).show()
    }

    // EDIT THESE to your server + test account
    private val serverUrl = "http://10.0.2.2:8000"
    private val demoEmail = "you@example.com"
    private val demoPassword = "hunter2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = EntryRepository(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            reqPerms.launch(Manifest.permission.RECORD_AUDIO)
        }

        val btnInit = findViewById<Button>(R.id.btnInit)
        val btnSumm = findViewById<Button>(R.id.btnSummarize)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnBackup = findViewById<Button>(R.id.btnBackup)
        val btnRestore = findViewById<Button>(R.id.btnRestore)
        val edit = findViewById<EditText>(R.id.editText)
        val tv = findViewById<TextView>(R.id.tvResult)
        val progress = findViewById<ProgressBar>(R.id.progress)

        fun showProgress(on: Boolean, msg: String? = null) {
            progress.visibility = if (on) android.view.View.VISIBLE else android.view.View.GONE
            tv.text = msg ?: tv.text
        }

        btnInit.setOnClickListener {
            scope.launch {
                showProgress(true, "Copying models...")
                val qwen = ModelInstaller.ensureModel(applicationContext, "Qwen3-4B-Q4_K_M.gguf")
                val whisper = ModelInstaller.ensureModel(applicationContext, "ggml-base.en-q5_1.bin")
                showProgress(true, "Initializing models...")
                withContext(Dispatchers.Default) {
                    llamaHandle = LlamaBridge.initModel(qwen.absolutePath, 1024, Runtime.getRuntime().availableProcessors().coerceAtMost(8))
                    whisperHandle = WhisperBridge.initModel(whisper.absolutePath, Runtime.getRuntime().availableProcessors().coerceAtMost(6))
                }
                showProgress(false, if (llamaHandle != 0L && whisperHandle != 0L) "Models ready." else "Model init failed.")
            }
        }

        btnSumm.setOnClickListener {
            val outWav = File(filesDir, "audio/session_${System.currentTimeMillis()}.wav").absolutePath
            if (audioRecorder != null) {
                tv.text = "Stopping..."
                audioRecorder?.stop()
                audioRecorder = null
            } else {
                tv.text = "Recording (tap again to stop)..."
                audioRecorder = AudioRecorder().also { it.start(outWav) }
                return@setOnClickListener
            }

            scope.launch {
                showProgress(true, "Transcribing...")
                val transcript = withContext(Dispatchers.Default) {
                    WhisperBridge.transcribe(whisperHandle, outWav, Runtime.getRuntime().availableProcessors().coerceAtMost(6))
                }.ifBlank { "(transcription unavailable in demo JNI stub)" }
                edit.setText(transcript)

                showProgress(true, "Summarizing...")
                val summary = withContext(Dispatchers.Default) {
                    val prompt = "Summarize in 2 sentences:\n$transcript\nSummary:"
                    LlamaBridge.generate(llamaHandle, prompt, 64, 0.0f, 1.0f).trim()
                }

                val mood = withContext(Dispatchers.Default) {
                    val prompt = "Choose ONE word from [calm,happy,anxious,sad,angry,stressed,grateful,tired]. Entry:\n$transcript\nMood:"
                    LlamaBridge.generate(llamaHandle, prompt, 6, 0.0f, 1.0f).trim()
                }

                tv.text = "Summary:\n$summary\n\nMood: $mood"
                withContext(Dispatchers.Default) { repo.save(transcript, summary, mood, outWav) }
                showProgress(false, "Saved locally.")
            }
        }

        btnLogin.setOnClickListener {
            scope.launch {
                showProgress(true, "Logging in...")
                val token = SyncClient.login(serverUrl, demoEmail, demoPassword)
                if (token != null) {
                    TokenStore.save(this@MainActivity, demoEmail, token)
                    showProgress(false, "Logged in & token saved.")
                } else {
                    showProgress(false, "Login failed.")
                }
            }
        }

        btnBackup.setOnClickListener {
            scope.launch {
                val text = edit.text.toString().ifBlank { "(no transcript)" }
                showProgress(true, "Encrypting & syncing...")
                // quick summarization if summary not present in tv
                val summary = "Backup summary"
                val mood = "neutral"
                val ok = SyncClient.syncEncryptedEntry(serverUrl, text, summary, mood)
                showProgress(false, if (ok) "Backup complete." else "Backup failed.")
            }
        }

        btnRestore.setOnClickListener {
            scope.launch {
                showProgress(true, "Restoring entries...")
                val restored = SyncClient.restoreEntries(serverUrl, sinceMs = 0L)
                var count = 0
                withContext(Dispatchers.Default) {
                    for (r in restored) {
                        repo.save(r.transcript, r.summary, r.mood, "")
                        count++
                    }
                }
                showProgress(false, "Restored $count entries.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (llamaHandle != 0L) LlamaBridge.free(llamaHandle)
        if (whisperHandle != 0L) WhisperBridge.free(whisperHandle)
        audioRecorder?.stop()
    }
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.menu_entries) {
            startActivity(android.content.Intent(this, com.mindvault.ai.ui.EntryListActivity::class.java))
            return true
        }
        if (item.itemId == R.id.menu_settings) {
            startActivity(android.content.Intent(this, com.mindvault.ai.ui.SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}


