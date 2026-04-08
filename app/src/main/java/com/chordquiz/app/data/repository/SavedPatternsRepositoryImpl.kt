package com.chordquiz.app.data.repository

import com.chordquiz.app.data.db.dao.SavedPatternDao
import com.chordquiz.app.data.db.entity.SavedPatternEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SavedPatternsRepositoryImpl @Inject constructor(
    private val dao: SavedPatternDao
) : SavedPatternsRepository {
    override fun getPatternsFlow(): Flow<List<SavedPatternEntity>> = dao.getAllPatterns()
    override suspend fun insertPattern(pattern: SavedPatternEntity): Long = dao.insert(pattern)
    override suspend fun deletePattern(id: Long) = dao.deleteById(id)
    override suspend fun findByName(name: String): SavedPatternEntity? = dao.findByName(name)
}
