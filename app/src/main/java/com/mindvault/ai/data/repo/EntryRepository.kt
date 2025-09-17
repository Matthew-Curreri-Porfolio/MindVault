package com.mindvault.ai.data.repo

import android.content.Context
import com.mindvault.ai.data.db.AppDatabase
import com.mindvault.ai.data.db.JournalEntry
import java.util.*

class EntryRepository(ctx: Context) {
    private val dao = AppDatabase.get(ctx).entryDao()

    suspend fun save(transcript: String, summary: String, mood: String, audioPath: String) {
        val entry = JournalEntry(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            transcript = transcript,
            summary = summary,
            mood = mood,
            audioPath = audioPath
        )
        dao.upsert(entry)
    }

    suspend fun list() = dao.all()
}
