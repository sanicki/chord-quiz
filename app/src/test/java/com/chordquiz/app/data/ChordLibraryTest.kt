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

    @Test
    fun `empty string input returns empty list`() {
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings("")
        assertEquals(0, result.size)
    }

    @Test
    fun `blank string input returns empty list`() {
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings("   ")
        assertEquals(0, result.size)
    }

    @Test
    fun `incomplete sections missing pipes skips malformed fingering`() {
        val malformed = "1|null"  // missing third section
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(malformed)
        assertEquals(0, result.size)
    }

    @Test
    fun `non-integer baseFret skips malformed fingering`() {
        val malformed = "abc|null|1,0,2"
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(malformed)
        assertEquals(0, result.size)
    }

    @Test
    fun `malformed barre segment skips fingering`() {
        val malformed = "1|1,1|0,1,2"  // barre has only 2 components instead of 3
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(malformed)
        assertEquals(0, result.size)
    }

    @Test
    fun `non-integer in barre segment skips fingering`() {
        val malformed = "1|x,1,5|0,1,2"
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(malformed)
        assertEquals(0, result.size)
    }

    @Test
    fun `non-integer in positions skips fingering`() {
        val malformed = "1|null|0,x,2"
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(malformed)
        assertEquals(0, result.size)
    }

    @Test
    fun `mixed valid and invalid fingerings skips invalid only`() {
        val valid = "1|null|0,1,2"
        val invalid = "abc|null|0,1,2"
        val mixed = "$valid;$invalid;$valid"
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(mixed)
        assertEquals(2, result.size)
        result.forEach { fingering ->
            assertEquals(1, fingering.baseFret)
            assertEquals(null, fingering.barre)
        }
    }

    @Test
    fun `valid fingering with null barre deserializes correctly`() {
        val valid = "1|null|0,1,2,3,2,0"
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(valid)
        assertEquals(1, result.size)
        val fingering = result.first()
        assertEquals(1, fingering.baseFret)
        assertEquals(null, fingering.barre)
        assertEquals(6, fingering.positions.size)
    }

    @Test
    fun `valid fingering with barre deserializes correctly`() {
        val valid = "1|1,1,5|0,1,2,3,2,0"
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(valid)
        assertEquals(1, result.size)
        val fingering = result.first()
        assertEquals(1, fingering.baseFret)
        assertEquals(1, fingering.barre?.fret)
        assertEquals(1, fingering.barre?.fromString)
        assertEquals(5, fingering.barre?.toString)
        assertEquals(6, fingering.positions.size)
    }

    @Test
    fun `single section only skips malformed fingering`() {
        val malformed = "1"
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(malformed)
        assertEquals(0, result.size)
    }

    @Test
    fun `extra pipes are handled gracefully`() {
        val malformed = "1|null|0,1,2|extra"
        val result = com.chordquiz.app.data.db.entity.ChordDefinitionEntity.deserializeFingerings(malformed)
        // Should succeed because split("|") will create 4 sections, but we only check the first 3
        assertEquals(1, result.size)
    }
}
