package com.mindvault.ai

object WhisperBridge {
    init { System.loadLibrary("journal_ai") }
    external fun initModel(modelPath: String, nThreads: Int): Long
    external fun transcribe(ctx: Long, audioPath: String, nThreads: Int): String
    external fun free(ctx: Long)
}
