package com.chordquiz.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chordquiz.app.data.db.ChordQuizDatabase
import com.chordquiz.app.data.db.dao.ChordDao
import com.chordquiz.app.data.db.dao.GroupDao
import com.chordquiz.app.data.db.dao.InstrumentDao
import com.chordquiz.app.data.db.entity.ChordDefinitionEntity
import com.chordquiz.app.data.db.entity.InstrumentEntity
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.seed.ChordLibrary
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DELETE FROM chords")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChordQuizDatabase {
        var database: ChordQuizDatabase? = null

        val callback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) { seedIfEmpty() }
            override fun onOpen(db: SupportSQLiteDatabase) { seedIfEmpty() }

            private fun seedIfEmpty() {
                CoroutineScope(Dispatchers.IO).launch {
                    database?.let { db ->
                        if (db.instrumentDao().count() == 0) {
                            db.instrumentDao().insertAll(
                                Instrument.ALL.map { InstrumentEntity.fromDomain(it) }
                            )
                        }
                        if (db.chordDao().count() == 0) {
                            db.chordDao().insertAll(
                                ChordLibrary.ALL.map { ChordDefinitionEntity.fromDomain(it) }
                            )
                        }
                    }
                }
            }
        }

        return Room.databaseBuilder(
            context,
            ChordQuizDatabase::class.java,
            "chord_quiz.db"
        ).addMigrations(MIGRATION_3_4)
            .addCallback(callback)
            .build()
            .also { database = it }
    }

    @Provides
    fun provideInstrumentDao(db: ChordQuizDatabase): InstrumentDao = db.instrumentDao()

    @Provides
    fun provideChordDao(db: ChordQuizDatabase): ChordDao = db.chordDao()

    @Provides
    fun provideGroupDao(db: ChordQuizDatabase): GroupDao = db.groupDao()
}
