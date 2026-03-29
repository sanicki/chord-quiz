package com.chordquiz.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.model.StringPosition
import com.chordquiz.app.data.model.BarreSegment

@Entity(tableName = "chords")
data class ChordDefinitionEntity(
    @PrimaryKey val id: String,
    val instrumentId: String,
    val chordName: String,
    val rootNoteSemitone: Int,
    val chordTypeName: String,
    /** JSON-encoded list of fingerings */
    val fingeringsJson: String,
    /** Comma-separated semitones */
    val noteComponentsSemitones: String
) {
    fun toDomain(): ChordDefinition = ChordDefinition(
        id = id,
        instrumentId = instrumentId,
        chordName = chordName,
        rootNote = Note.fromSemitone(rootNoteSemitone),
        chordType = ChordType.valueOf(chordTypeName),
        fingerings = deserializeFingerings(fingeringsJson),
        noteComponents = noteComponentsSemitones.split(",")
            .map { Note.fromSemitone(it.trim().toInt()) }
    )

    companion object {
        fun fromDomain(c: ChordDefinition) = ChordDefinitionEntity(
            id = c.id,
            instrumentId = c.instrumentId,
            chordName = c.chordName,
            rootNoteSemitone = c.rootNote.semitone,
            chordTypeName = c.chordType.name,
            fingeringsJson = serializeFingerings(c.fingerings),
            noteComponentsSemitones = c.noteComponents.joinToString(",") { it.semitone.toString() }
        )

        /**
         * Serialization format per fingering (semicolon-separated fingerings):
         * baseFret|barre(fret,from,to or null)|pos0fret,pos1fret,...
         */
        fun serializeFingerings(fingerings: List<Fingering>): String =
            fingerings.joinToString(";") { f ->
                val barre = f.barre?.let { "${it.fret},${it.fromString},${it.toString}" } ?: "null"
                val positions = f.positions.sortedBy { it.stringIndex }
                    .joinToString(",") { it.fret.toString() }
                "${f.baseFret}|$barre|$positions"
            }

        fun deserializeFingerings(json: String): List<Fingering> =
            json.split(";").map { part ->
                val sections = part.split("|")
                val baseFret = sections[0].toInt()
                val barre = if (sections[1] == "null") null else {
                    val b = sections[1].split(",")
                    BarreSegment(b[0].toInt(), b[1].toInt(), b[2].toInt())
                }
                val frets = sections[2].split(",").map { it.toInt() }
                val positions = frets.mapIndexed { idx, fret -> StringPosition(idx, fret) }
                Fingering(positions = positions, barre = barre, baseFret = baseFret)
            }
    }
}
