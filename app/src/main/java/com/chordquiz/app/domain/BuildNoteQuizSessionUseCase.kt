package com.chordquiz.app.domain

import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.data.model.QuizSession
import java.util.UUID
import javax.inject.Inject

/**
 * Represents a unique playable note position: note + octave + display spelling.
 */
private data class PoolEntry(val note: Note, val octave: Int, val displayName: String)

class BuildNoteQuizSessionUseCase @Inject constructor() {

    fun buildDrawSession(
        instrument: Instrument,
        noteMode: NoteMode,
        questionCount: Int,
        repeatMissed: Boolean
    ): QuizSession = buildSession(instrument, QuizMode.NOTE_DRAW, noteMode, questionCount, repeatMissed)

    fun buildPlaySession(
        instrument: Instrument,
        noteMode: NoteMode,
        questionCount: Int,
        repeatMissed: Boolean
    ): QuizSession = buildSession(instrument, QuizMode.NOTE_PLAY, noteMode, questionCount, repeatMissed)

    private fun buildSession(
        instrument: Instrument,
        mode: QuizMode,
        noteMode: NoteMode,
        questionCount: Int,
        repeatMissed: Boolean
    ): QuizSession {
        val pool = buildNotePool(instrument)

        val questions = buildQuestionList(pool, questionCount).map { entry ->
            QuizQuestion.NoteQuestion(
                note = entry.note,
                octave = entry.octave,
                displayName = entry.displayName,
                noteMode = noteMode
            )
        }

        return QuizSession(
            id = UUID.randomUUID().toString(),
            mode = mode,
            instrument = instrument,
            questions = questions,
            repeatMissed = repeatMissed
        )
    }

    /**
     * Compute all unique (note, octave, displayName) entries within the instrument's playable range.
     * Both enharmonic spellings are included for accidental pitches (e.g., C# and Db).
     */
    private fun buildNotePool(instrument: Instrument): List<PoolEntry> {
        val seen = mutableSetOf<Triple<Int, Int, String>>() // semitone, octave, displayName
        val pool = mutableListOf<PoolEntry>()

        for (stringIdx in instrument.openStringNotes.indices) {
            val openNote = instrument.openStringNotes[stringIdx]
            val openOctave = instrument.openStringOctaves[stringIdx]

            for (fret in 0..instrument.totalFrets) {
                val semitone = (openNote.semitone + fret) % 12
                val octaveIncrement = (openNote.semitone + fret) / 12
                val octave = openOctave + octaveIncrement
                val note = Note.fromSemitone(semitone)

                // Add sharp spelling
                val sharpName = note.displayName
                val sharpKey = Triple(semitone, octave, sharpName)
                if (seen.add(sharpKey)) {
                    pool.add(PoolEntry(note, octave, sharpName))
                }

                // Add flat spelling if it exists
                val flatName = note.flatName
                if (flatName != null) {
                    val flatKey = Triple(semitone, octave, flatName)
                    if (seen.add(flatKey)) {
                        pool.add(PoolEntry(note, octave, flatName))
                    }
                }
            }
        }

        return pool
    }
}
