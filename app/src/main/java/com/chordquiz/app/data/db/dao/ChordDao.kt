package com.chordquiz.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chordquiz.app.data.db.entity.ChordDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChordDao {
    @Query("SELECT * FROM chords WHERE instrumentId = :instrumentId ORDER BY rootNoteSemitone, chordTypeName")
    fun getChordsForInstrument(instrumentId: String): Flow<List<ChordDefinitionEntity>>

    @Query("SELECT * FROM chords WHERE instrumentId = :instrumentId AND rootNoteSemitone = :semitone AND chordTypeName = :typeName")
    suspend fun getChord(instrumentId: String, semitone: Int, typeName: String): ChordDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(chords: List<ChordDefinitionEntity>)

    @Query("SELECT COUNT(*) FROM chords WHERE instrumentId = :instrumentId")
    suspend fun countForInstrument(instrumentId: String): Int

    @Query("SELECT * FROM chords WHERE instrumentId = :instrumentId AND id = :chordId")
    suspend fun getChordById(instrumentId: String, chordId: String): ChordDefinitionEntity?

    @Query("DELETE FROM chords WHERE id IN (:chordIds)")
    suspend fun deleteChordsById(chordIds: List<String>): Int
}
