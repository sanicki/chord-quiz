package com.chordquiz.app.data.repository

import com.chordquiz.app.data.model.Instrument
import kotlinx.coroutines.flow.Flow

interface InstrumentRepository {
    fun getAllInstruments(): Flow<List<Instrument>>
    suspend fun getInstrumentById(id: String): Instrument?
}
