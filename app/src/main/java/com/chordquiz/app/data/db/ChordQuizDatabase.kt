package com.chordquiz.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chordquiz.app.data.db.dao.ChordDao
import com.chordquiz.app.data.db.dao.GroupDao
import com.chordquiz.app.data.db.dao.InstrumentDao
import com.chordquiz.app.data.db.entity.ChordDefinitionEntity
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.db.entity.InstrumentEntity

@Database(
    entities = [InstrumentEntity::class, ChordDefinitionEntity::class, GroupEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ChordQuizDatabase : RoomDatabase() {
    abstract fun instrumentDao(): InstrumentDao
    abstract fun chordDao(): ChordDao
    abstract fun groupDao(): GroupDao
}
