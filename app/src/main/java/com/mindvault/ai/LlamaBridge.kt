package com.mindvault.ai

object LlamaBridge {
    init { System.loadLibrary("journal_ai") }
    external fun initModel(modelPath: String, nCtx: Int, nThreads: Int): Long
    external fun generate(ctx: Long, prompt: String, maxTokens: Int, temp: Float, topP: Float): String
    external fun free(ctx: Long)
}
