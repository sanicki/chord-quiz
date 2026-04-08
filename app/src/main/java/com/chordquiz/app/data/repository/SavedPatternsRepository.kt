package com.chordquiz.app.data.repository

import com.chordquiz.app.data.db.entity.SavedPatternEntity
import kotlinx.coroutines.flow.Flow

interface SavedPatternsRepository {
    fun getPatternsFlow(): Flow<List<SavedPatternEntity>>
    suspend fun insertPattern(pattern: SavedPatternEntity): Long
    suspend fun deletePattern(id: Long)
    suspend fun findByName(name: String): SavedPatternEntity?
}
