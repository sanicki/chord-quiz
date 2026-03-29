package com.chordquiz.app.audio

import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.seed.GuitarChords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ChordRecognizerTest {

    private lateinit var recognizer: ChordRecognizer

    @Before
    fun setUp() {
        recognizer = ChordRecognizer()
        recognizer.setCandidates(GuitarChords.ALL)
    }

    @Test
    fun `recognize Am from exact notes returns Am`() {
        // Am = A, C, E
        val amFreqs = listOf(
            NoteFrequencyTable.midiToHz(69),  // A4
            NoteFrequencyTable.midiToHz(60),  // C4
            NoteFrequencyTable.midiToHz(64)   // E4
        )
        val result = recognizer.recognize(amFreqs)
        assertNotNull(result)
        assertEquals(Note.A, result!!.chord.rootNote)
        assertEquals(ChordType.MINOR, result.chord.chordType)
    }

    @Test
    fun `recognize G major from exact notes`() {
        // G = G, B, D
        val gFreqs = listOf(
            NoteFrequencyTable.midiToHz(67),  // G4
            NoteFrequencyTable.midiToHz(71),  // B4
            NoteFrequencyTable.midiToHz(62)   // D4
        )
        val result = recognizer.recognize(gFreqs)
        assertNotNull(result)
        assertEquals(Note.G, result!!.chord.rootNote)
        assertEquals(ChordType.MAJOR, result.chord.chordType)
    }

    @Test
    fun `empty frequencies returns null`() {
        val result = recognizer.recognize(emptyList())
        assertNull(result)
    }

    @Test
    fun `no candidates returns null`() {
        recognizer.setCandidates(emptyList())
        val result = recognizer.recognize(listOf(440.0))
        assertNull(result)
    }

    @Test
    fun `note frequency table round trip`() {
        val midiNote = 69  // A4
        val hz = NoteFrequencyTable.midiToHz(midiNote)
        val back = NoteFrequencyTable.hzToMidi(hz)
        assertEquals(midiNote, back)
    }
}
