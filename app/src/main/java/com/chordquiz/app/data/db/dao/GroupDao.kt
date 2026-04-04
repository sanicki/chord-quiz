package com.chordquiz.app.data.db.dao

import androidx.room.*
import com.chordquiz.app.data.db.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE instrumentId = :instrumentId ORDER BY name")
    fun getGroupsByInstrument(instrumentId: String): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Insert
    suspend fun insert(group: GroupEntity): Long

    @Query("UPDATE groups SET name = :name, updatedAt = updatedAt WHERE id = :id")
    suspend fun updateGroup(id: Long, name: String)

    @DeleteEntity
    suspend fun deleteGroup(id: Long)

    @Query("SELECT * FROM groups WHERE instrumentId = :instrumentId AND name = :name")
    suspend fun findGroupByName(instrumentId: String, name: String): GroupEntity?

    @Query("SELECT * FROM groups WHERE instrumentId = :instrumentId")
    fun getAllGroups(instrumentId: String): Flow<List<GroupEntity>>
}
