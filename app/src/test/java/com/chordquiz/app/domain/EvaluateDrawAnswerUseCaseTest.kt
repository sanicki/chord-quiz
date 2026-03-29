package com.chordquiz.app.domain

import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.StringPosition
import com.chordquiz.app.data.seed.GuitarChords
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluateDrawAnswerUseCaseTest {

    private lateinit var useCase: EvaluateDrawAnswerUseCase
    private val guitar = Instrument.GUITAR

    @Before
    fun setUp() {
        useCase = EvaluateDrawAnswerUseCase()
    }

    @Test
    fun `correct Am fingering is accepted`() {
        // Am: x02210 → strings E=muted, A=0, D=2, G=2, B=1, e=0
        val fingering = Fingering(
            positions = listOf(
                StringPosition(0, -1), // E muted
                StringPosition(1, 0),  // A open
                StringPosition(2, 2),  // D fret 2
                StringPosition(3, 2),  // G fret 2
                StringPosition(4, 1),  // B fret 1
                StringPosition(5, 0)   // e open
            )
        )
        val result = useCase(guitar, fingering, GuitarChords.A_MINOR)
        assertTrue(result)
    }

    @Test
    fun `wrong fingering for Am is rejected`() {
        // All open strings → E major notes, not Am
        val fingering = Fingering(
            positions = (0..5).map { StringPosition(it, 0) }
        )
        val result = useCase(guitar, fingering, GuitarChords.A_MINOR)
        assertFalse(result)
    }

    @Test
    fun `E major open chord is accepted`() {
        // E: 022100
        val fingering = Fingering(
            positions = listOf(
                StringPosition(0, 0),  // E open
                StringPosition(1, 2),  // A fret 2
                StringPosition(2, 2),  // D fret 2
                StringPosition(3, 1),  // G fret 1
                StringPosition(4, 0),  // B open
                StringPosition(5, 0)   // e open
            )
        )
        val result = useCase(guitar, fingering, GuitarChords.E_MAJOR)
        assertTrue(result)
    }

    @Test
    fun `all muted strings fails`() {
        val fingering = Fingering(
            positions = (0..5).map { StringPosition(it, -1) }
        )
        val result = useCase(guitar, fingering, GuitarChords.A_MINOR)
        assertFalse(result)
    }
}
