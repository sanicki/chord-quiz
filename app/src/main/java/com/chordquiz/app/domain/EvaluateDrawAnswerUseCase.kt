package com.chordquiz.app.domain

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.Note
import javax.inject.Inject

class EvaluateDrawAnswerUseCase @Inject constructor() {

    /**
     * Returns true if the user's fingering matches the target.
     *
     * When a [referenceFingering] is provided (the canonical voicing shown on screen):
     *  - Muted strings must match exactly.
     *  - Non-muted strings must produce the same set of notes as the reference.
     *
     * Without a reference (fallback) we compare note sets against the chord's
     * [ChordDefinition.noteComponents].
     */
    operator fun invoke(
        instrument: Instrument,
        userFingering: Fingering,
        target: ChordDefinition,
        referenceFingering: Fingering? = null
    ): Boolean {
        if (referenceFingering != null) {
            // Muted strings must match exactly
            val refMuted  = referenceFingering.positions.filter { it.fret == -1 }.map { it.stringIndex }.toSet()
            val userMuted = userFingering.positions.filter { it.fret == -1 }.map { it.stringIndex }.toSet()
            if (refMuted != userMuted) return false

            // Notes produced by the non-muted strings must match
            val refNotes  = computeNotes(instrument, referenceFingering)
            val userNotes = computeNotes(instrument, userFingering)
            return userNotes.isNotEmpty() && userNotes == refNotes
        }

        // Fallback: compare against chord components
        val userNotes   = computeNotes(instrument, userFingering)
        val targetNotes = target.noteComponents.toSet()
        return userNotes.isNotEmpty() &&
            userNotes.containsAll(targetNotes) &&
            targetNotes.containsAll(userNotes)
    }

    private fun computeNotes(instrument: Instrument, fingering: Fingering): Set<Note> =
        fingering.positions
            .filter { it.fret >= 0 }
            .map { pos -> instrument.openStringNotes[pos.stringIndex].plus(pos.fret) }
            .toSet()
}
