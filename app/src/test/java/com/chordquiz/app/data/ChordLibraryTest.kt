package com.chordquiz.app.data

import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.seed.ChordLibrary
import com.chordquiz.app.data.seed.GuitarChords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordLibraryTest {

    @Test
    fun `guitar has 60 chords`() {
        val chords = ChordLibrary.chordsForInstrument(Instrument.GUITAR)
        assertEquals(60, chords.size)
    }

    @Test
    fun `ukulele has 60 chords`() {
        val chords = ChordLibrary.chordsForInstrument(Instrument.UKULELE)
        assertEquals(60, chords.size)
    }

    @Test
    fun `bass has 60 chords`() {
        val chords = ChordLibrary.chordsForInstrument(Instrument.BASS)
        assertEquals(60, chords.size)
    }

    @Test
    fun `banjo has 60 chords`() {
        val chords = ChordLibrary.chordsForInstrument(Instrument.BANJO)
        assertEquals(60, chords.size)
    }

    @Test
    fun `guitar has all 5 chord types`() {
        val types = GuitarChords.ALL.map { it.chordType }.toSet()
        assertEquals(ChordType.entries.toSet(), types)
    }

    @Test
    fun `all guitar chords have at least one fingering`() {
        GuitarChords.ALL.forEach { chord ->
            assertTrue("${chord.chordName} has no fingerings", chord.fingerings.isNotEmpty())
        }
    }

    @Test
    fun `all guitar chords have correct note count`() {
        GuitarChords.ALL.forEach { chord ->
            val expectedCount = chord.chordType.intervals.size
            assertEquals(
                "${chord.chordName} note count mismatch",
                expectedCount,
                chord.noteComponents.size
            )
        }
    }

    @Test
    fun `serialization round trip preserves fingering`() {
        val original = GuitarChords.A_MINOR.fingerings.first()
        val serialized = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.serializeFingerings(listOf(original))
        val deserialized = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(serialized)
        assertEquals(1, deserialized.size)
        val restored = deserialized.first()
        assertEquals(original.baseFret, restored.baseFret)
        assertEquals(original.positions.size, restored.positions.size)
        original.positions.forEachIndexed { i, pos ->
            assertEquals(pos.fret, restored.positions[i].fret)
        }
    }
}
