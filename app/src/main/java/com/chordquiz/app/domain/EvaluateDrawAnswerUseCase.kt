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
     * as the target chord (octave-agnostic), AND does not mute any string
     * that the reference fingering requires to be played.
     */
    operator fun invoke(
        instrument: Instrument,
        userFingering: Fingering,
        target: ChordDefinition,
        referenceFingering: Fingering? = null
    ): Boolean {
        // Check note set equality
        val userNotes = computeNotes(instrument, userFingering)
        val targetNotes = target.noteComponents.toSet()
        if (userNotes.isEmpty() ||
            !userNotes.containsAll(targetNotes) ||
            !targetNotes.containsAll(userNotes)
        ) return false

        // Additionally verify the user hasn't muted a string that the reference
        // fingering requires to be played. This catches cases where muting a
        // duplicate-note string still yields a matching note set.
        if (referenceFingering != null) {
            val requiredStrings = referenceFingering.positions
                .filter { it.fret >= 0 }
                .map { it.stringIndex }
                .toSet()
            val userMutedStrings = userFingering.positions
                .filter { it.fret == -1 }
                .map { it.stringIndex }
                .toSet()
            if (requiredStrings.intersect(userMutedStrings).isNotEmpty()) return false
        }

        return true
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
