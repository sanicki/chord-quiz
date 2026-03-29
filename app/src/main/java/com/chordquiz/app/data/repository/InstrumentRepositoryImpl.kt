package com.chordquiz.app.data.repository

import com.chordquiz.app.data.db.dao.InstrumentDao
import com.chordquiz.app.data.db.entity.InstrumentEntity
import com.chordquiz.app.data.model.Instrument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InstrumentRepositoryImpl @Inject constructor(
    private val dao: InstrumentDao
) : InstrumentRepository {

    override fun getAllInstruments(): Flow<List<Instrument>> =
        dao.getAllInstruments().map { list -> list.map { it.toDomain() } }

    override suspend fun getInstrumentById(id: String): Instrument? =
        dao.getInstrumentById(id)?.toDomain()
}
