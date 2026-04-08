package com.chordquiz.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chordquiz.app.data.db.entity.SavedPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPatternDao {
    @Query("SELECT * FROM saved_patterns ORDER BY createdAt DESC")
    fun getAllPatterns(): Flow<List<SavedPatternEntity>>

    @Insert
    suspend fun insert(pattern: SavedPatternEntity): Long

    @Query("DELETE FROM saved_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM saved_patterns WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): SavedPatternEntity?
}
