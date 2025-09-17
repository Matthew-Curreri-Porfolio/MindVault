package com.mindvault.ai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class JournalEntry(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val transcript: String,
    val summary: String,
    val mood: String,
    val audioPath: String
)
