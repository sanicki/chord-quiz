package com.chordquiz.app.data.repository

import com.chordquiz.app.data.model.ChordDefinition
import kotlinx.coroutines.flow.Flow

interface ChordRepository {
    fun getChordsForInstrument(instrumentId: String): Flow<List<ChordDefinition>>
}
