package com.mindvault.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindvault.ai.data.db.JournalEntry

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntry)

    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    suspend fun all(): List<JournalEntry>
}
