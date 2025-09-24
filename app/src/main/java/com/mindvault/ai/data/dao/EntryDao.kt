package com.mindvault.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindvault.ai.data.db.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntry)

    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    suspend fun all(): List<JournalEntry>

    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): JournalEntry?

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<JournalEntry?>

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
