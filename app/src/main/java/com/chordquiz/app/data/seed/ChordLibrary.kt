package com.chordquiz.app.data.seed

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Instrument

object ChordLibrary {
    fun chordsForInstrument(instrument: Instrument): List<ChordDefinition> =
        when (instrument.id) {
            Instrument.GUITAR.id   -> GuitarChords.ALL
            Instrument.UKULELE.id  -> UkuleleChords.ALL
            Instrument.BASS.id     -> BassChords.ALL
            Instrument.BANJO.id    -> BanjoChords.ALL
            else -> emptyList()
        }

    val ALL: List<ChordDefinition> =
        GuitarChords.ALL + UkuleleChords.ALL + BassChords.ALL + BanjoChords.ALL
}
