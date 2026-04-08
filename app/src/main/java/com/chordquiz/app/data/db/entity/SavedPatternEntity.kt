package com.chordquiz.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_patterns")
data class SavedPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val noteType: String,
    val slots: String,
    val bpm: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toName(): String = if (name.startsWith("Custom:")) name.substring(7) else name
    fun slotList(): List<String> = slots.split(",")
}
