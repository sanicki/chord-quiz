package com.chordquiz.app.domain

import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.data.model.StringPosition
import javax.inject.Inject

data class NoteDrawEvalResult(
    val isCorrect: Boolean,
    val missedPositions: List<StringPosition> = emptyList()
)

class EvaluateNoteDrawAnswerUseCase @Inject constructor() {

    operator fun invoke(
        instrument: Instrument,
        fingering: Fingering,
        question: QuizQuestion.NoteQuestion
    ): NoteDrawEvalResult {
        val targetSemitone = question.note.semitone
        val targetOctave = question.octave

        // Derive note from each placed dot
        val placedNotes = fingering.positions
            .filter { it.fret >= 0 }
            .mapNotNull { pos ->
                val openNote = instrument.openStringNotes.getOrNull(pos.stringIndex) ?: return@mapNotNull null
                val openOctave = instrument.openStringOctaves.getOrNull(pos.stringIndex) ?: return@mapNotNull null
                val totalSemitones = openNote.semitone + pos.fret
                val semitone = totalSemitones % 12
                val octaveOffset = totalSemitones / 12
                val octave = openOctave + octaveOffset
                Triple(semitone, octave, pos.stringIndex)
            }

        return when (question.noteMode) {
            NoteMode.FIND_NOTE -> {
                // Any dot that produces the target pitch class is correct
                val isCorrect = placedNotes.any { (semitone, _, _) -> semitone == targetSemitone }
                NoteDrawEvalResult(isCorrect = isCorrect)
            }

            NoteMode.FIND_NOTE_CORRECT_OCTAVE -> {
                // Any dot that produces the exact (note, octave) is correct
                val isCorrect = placedNotes.any { (semitone, octave, _) ->
                    semitone == targetSemitone && octave == targetOctave
                }
                NoteDrawEvalResult(isCorrect = isCorrect)
            }

            NoteMode.FIND_ALL_NOTES -> {
                // All fretboard positions that produce the pitch class must be marked
                val allPositions = computeAllPositions(instrument, targetSemitone, null)
                val markedStrings = placedNotes
                    .filter { (semitone, _, _) -> semitone == targetSemitone }
                    .map { (_, _, strIdx) -> strIdx }
                    .toSet()
                val missedPositions = allPositions.filter { it.stringIndex !in markedStrings }
                NoteDrawEvalResult(isCorrect = missedPositions.isEmpty(), missedPositions = missedPositions)
            }

            NoteMode.FIND_ALL_NOTES_CORRECT_OCTAVE -> {
                // All fretboard positions that produce the exact (semitone, octave) must be marked
                val allPositions = computeAllPositions(instrument, targetSemitone, targetOctave)
                val markedPairs = placedNotes
                    .filter { (semitone, octave, _) -> semitone == targetSemitone && octave == targetOctave }
                    .map { (_, _, strIdx) -> strIdx }
                    .toSet()
                val missedPositions = allPositions.filter { it.stringIndex !in markedPairs }
                NoteDrawEvalResult(isCorrect = missedPositions.isEmpty(), missedPositions = missedPositions)
            }
        }
    }

    private fun computeAllPositions(
        instrument: Instrument,
        targetSemitone: Int,
        targetOctave: Int?
    ): List<StringPosition> {
        val result = mutableListOf<StringPosition>()
        for (stringIdx in instrument.openStringNotes.indices) {
            val openNote = instrument.openStringNotes[stringIdx]
            val openOctave = instrument.openStringOctaves[stringIdx]
            for (fret in 0..instrument.totalFrets) {
                val totalSemitones = openNote.semitone + fret
                val semitone = totalSemitones % 12
                val octave = openOctave + totalSemitones / 12
                val matches = if (targetOctave != null) {
                    semitone == targetSemitone && octave == targetOctave
                } else {
                    semitone == targetSemitone
                }
                if (matches) {
                    result.add(StringPosition(stringIndex = stringIdx, fret = fret))
                    break // only the first fret per string counts
                }
            }
        }
        return result
    }
}
