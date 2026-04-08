package com.chordquiz.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chordquiz.app.data.db.dao.ChordDao
import com.chordquiz.app.data.db.dao.GroupDao
import com.chordquiz.app.data.db.dao.InstrumentDao
import com.chordquiz.app.data.db.dao.SavedPatternDao
import com.chordquiz.app.data.db.entity.ChordDefinitionEntity
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.db.entity.InstrumentEntity
import com.chordquiz.app.data.db.entity.SavedPatternEntity

@Database(
    entities = [InstrumentEntity::class, ChordDefinitionEntity::class, GroupEntity::class, SavedPatternEntity::class],
    version = 6,
    exportSchema = false
)
abstract class ChordQuizDatabase : RoomDatabase() {
    abstract fun instrumentDao(): InstrumentDao
    abstract fun chordDao(): ChordDao
    abstract fun groupDao(): GroupDao
    abstract fun savedPatternDao(): SavedPatternDao
}
