package com.chordquiz.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.Note

@Entity(tableName = "instruments")
data class InstrumentEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val stringCount: Int,
    val openStringNotesSemitones: String,   // comma-separated semitone ints
    val openStringOctaves: String,           // comma-separated ints
    val displayedFretCount: Int,
    val totalFrets: Int
) {
    fun toDomain(): Instrument = Instrument(
        id = id,
        displayName = displayName,
        stringCount = stringCount,
        openStringNotes = openStringNotesSemitones.split(",")
            .map { Note.fromSemitone(it.trim().toInt()) },
        openStringOctaves = openStringOctaves.split(",")
            .map { it.trim().toInt() },
        displayedFretCount = displayedFretCount,
        totalFrets = totalFrets
    )

    companion object {
        fun fromDomain(i: Instrument) = InstrumentEntity(
            id = i.id,
            displayName = i.displayName,
            stringCount = i.stringCount,
            openStringNotesSemitones = i.openStringNotes.joinToString(",") { it.semitone.toString() },
            openStringOctaves = i.openStringOctaves.joinToString(","),
            displayedFretCount = i.displayedFretCount,
            totalFrets = i.totalFrets
        )
    }
}
