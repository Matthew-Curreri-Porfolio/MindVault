package com.mindvault.ai

import android.content.Context
import kotlinx.coroutines.*
import java.io.File

class JournalAI(private val ctxHandle: Long) {
    companion object {
        fun init(appContext: Context, modelFilename: String = "Qwen3-4B-Q4_K_M.gguf", nCtx: Int = 1024, nThreads: Int = 4): JournalAI? {
            val modelFile = File(appContext.filesDir, "models/")
            if (!modelFile.exists()) return null
            val handle = LlamaBridge.initModel(modelFile.absolutePath, nCtx, nThreads)
            if (handle == 0L) return null
            return JournalAI(handle)
        }
    }

    suspend fun summarize(text: String): String = withContext(Dispatchers.Default) {
        val prompt = "Summarize the following journal entry in 2 sentences:\n\nSummary:"
        LlamaBridge.generate(ctxHandle, prompt, 64, 0.0f, 1.0f).trim()
    }

    suspend fun mood(text: String): String = withContext(Dispatchers.Default) {
        val prompt = "Choose ONE mood word from [calm,happy,anxious,sad,angry,stressed,grateful,tired] for this journal entry. Output ONLY the word. Entry: \nMood:"
        LlamaBridge.generate(ctxHandle, prompt, 8, 0.0f, 1.0f).trim()
    }

    fun close() {
        LlamaBridge.free(ctxHandle)
    }
}
