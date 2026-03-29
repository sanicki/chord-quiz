package com.chordquiz.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chordquiz.app.data.db.entity.InstrumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrumentDao {
    @Query("SELECT * FROM instruments ORDER BY displayName")
    fun getAllInstruments(): Flow<List<InstrumentEntity>>

    @Query("SELECT * FROM instruments WHERE id = :id")
    suspend fun getInstrumentById(id: String): InstrumentEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(instruments: List<InstrumentEntity>)

    @Query("SELECT COUNT(*) FROM instruments")
    suspend fun count(): Int
}
