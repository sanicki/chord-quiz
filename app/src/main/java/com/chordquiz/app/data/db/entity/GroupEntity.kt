package com.chordquiz.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chordquiz.app.data.model.ChordDefinitionEntity

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val instrumentId: String,
    val chordIds: String, // comma-separated chord IDs
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun chordIdsList(): List<String> = chordIds.split(",").filter { it.isNotBlank() }

    fun isCustom(): Boolean = name.startsWith("Custom:")

    fun toName(): String = if (name.startsWith("Custom:")) name.substring(7) else name

    companion object {
        fun fromName(name: String) = "Custom:$name"
    }
}
