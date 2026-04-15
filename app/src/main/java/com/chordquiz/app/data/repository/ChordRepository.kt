package com.chordquiz.app.data.repository

import com.chordquiz.app.data.db.dao.ChordDao
import com.chordquiz.app.data.model.ChordDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChordRepository @Inject constructor(
    private val dao: ChordDao
) {

    fun getChordsForInstrument(instrumentId: String): Flow<List<ChordDefinition>> =
        dao.getChordsForInstrument(instrumentId).map { list -> list.map { it.toDomain() } }
}
