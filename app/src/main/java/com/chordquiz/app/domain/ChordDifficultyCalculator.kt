package com.chordquiz.app.domain

import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.model.ChordDefinition

object ChordDifficultyCalculator {

    // Virtual group IDs (negative so they never collide with DB-generated IDs)
    const val EASY_GROUP_ID = -1L
    const val MODERATE_GROUP_ID = -2L
    const val DIFFICULT_GROUP_ID = -3L

    private const val EASY_MAX = 6
    private const val MODERATE_MAX = 9

    fun score(chord: ChordDefinition): Int {
        val fingering = chord.fingerings.firstOrNull() ?: return 0

        val frettedPositions = fingering.positions.filter { it.fret > 0 }

        // Count fingers used (barre counts as one finger regardless of strings covered)
        val fingersUsed = if (fingering.barre != null) {
            // 1 for the barre + any additional pressed strings not part of the barre
            val barreStrings = (fingering.barre.fromString..fingering.barre.toString).toSet()
            val extraFingers = frettedPositions.count {
                it.stringIndex !in barreStrings || it.fret != fingering.barre.fret
            }
            1 + extraFingers
        } else {
            frettedPositions.size
        }

        // Fret span across pressed strings
        val frets = frettedPositions.map { it.fret }
        val fretSpan = if (frets.size >= 2) frets.max() - frets.min() else 0

        // Barre width penalty
        val barreWidth = when {
            fingering.barre == null -> 0
            (fingering.barre.toString - fingering.barre.fromString + 1) >= 4 -> 3
            (fingering.barre.toString - fingering.barre.fromString + 1) >= 2 -> 2
            else -> 1
        }

        // Open strings (lower difficulty)
        val openStrings = fingering.positions.count { it.fret == 0 }

        // Movable chord bonus (no open strings = harder to orient)
        val movableBonus = if (openStrings == 0 && frettedPositions.isNotEmpty()) 1 else 0

        return fingersUsed + fretSpan + barreWidth - openStrings + movableBonus
    }

    fun difficultyLabel(chord: ChordDefinition): String = when {
        score(chord) <= EASY_MAX -> "Easy"
        score(chord) <= MODERATE_MAX -> "Moderate"
        else -> "Difficult"
    }

    fun buildDifficultyGroups(instrumentId: String, chords: List<ChordDefinition>): List<GroupEntity> {
        val easy = chords.filter { score(it) <= EASY_MAX }
        val moderate = chords.filter { score(it) in (EASY_MAX + 1)..MODERATE_MAX }
        val difficult = chords.filter { score(it) > MODERATE_MAX }

        return buildList {
            if (easy.isNotEmpty()) add(virtualGroup(EASY_GROUP_ID, instrumentId, "Easy", easy))
            if (moderate.isNotEmpty()) add(virtualGroup(MODERATE_GROUP_ID, instrumentId, "Moderate", moderate))
            if (difficult.isNotEmpty()) add(virtualGroup(DIFFICULT_GROUP_ID, instrumentId, "Difficult", difficult))
        }
    }

    private fun virtualGroup(id: Long, instrumentId: String, name: String, chords: List<ChordDefinition>) =
        GroupEntity(
            id = id,
            instrumentId = instrumentId,
            name = name,
            chordIds = chords.joinToString(",") { it.id }
        )
}
