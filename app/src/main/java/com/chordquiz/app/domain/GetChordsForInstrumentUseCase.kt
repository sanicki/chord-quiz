package com.chordquiz.app.domain

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.data.repository.ChordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetChordsForInstrumentUseCase @Inject constructor(
    private val repository: ChordRepository
) {
    operator fun invoke(
        instrumentId: String,
        typeFilter: ChordType? = null
    ): Flow<List<ChordDefinition>> =
        repository.getChordsForInstrument(instrumentId).map { chords ->
            if (typeFilter != null) chords.filter { it.chordType == typeFilter } else chords
        }
}
