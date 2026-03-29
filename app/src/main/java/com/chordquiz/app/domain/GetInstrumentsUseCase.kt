package com.chordquiz.app.domain

import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.repository.InstrumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInstrumentsUseCase @Inject constructor(
    private val repository: InstrumentRepository
) {
    operator fun invoke(): Flow<List<Instrument>> = repository.getAllInstruments()
}
