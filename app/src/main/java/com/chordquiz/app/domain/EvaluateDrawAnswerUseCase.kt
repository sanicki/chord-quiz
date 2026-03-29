package com.chordquiz.app.domain

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.model.StringPosition
import javax.inject.Inject

class EvaluateDrawAnswerUseCase @Inject constructor() {

    /**
     * Returns true if the user's fingering produces the same set of notes
     * as the target chord (octave-agnostic).
     */
    operator fun invoke(
        instrument: Instrument,
        userFingering: Fingering,
        target: ChordDefinition
    ): Boolean {
        val userNotes = computeNotes(instrument, userFingering)
        val targetNotes = target.noteComponents.toSet()
        // User must produce all chord tones (no extra unrelated notes allowed)
        return userNotes.isNotEmpty() &&
            userNotes.containsAll(targetNotes) &&
            targetNotes.containsAll(userNotes)
    }

    private fun computeNotes(instrument: Instrument, fingering: Fingering): Set<Note> {
        return fingering.positions
            .filter { it.fret >= 0 }  // exclude muted strings
            .map { pos ->
                val openNote = instrument.openStringNotes[pos.stringIndex]
                openNote.plus(pos.fret)
            }.toSet()
    }
}
