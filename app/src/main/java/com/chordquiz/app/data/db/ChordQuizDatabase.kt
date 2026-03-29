package com.chordquiz.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chordquiz.app.data.db.dao.ChordDao
import com.chordquiz.app.data.db.dao.InstrumentDao
import com.chordquiz.app.data.db.entity.ChordDefinitionEntity
import com.chordquiz.app.data.db.entity.InstrumentEntity

@Database(
    entities = [InstrumentEntity::class, ChordDefinitionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChordQuizDatabase : RoomDatabase() {
    abstract fun instrumentDao(): InstrumentDao
    abstract fun chordDao(): ChordDao
}
