package com.chordquiz.app.data.repository

import com.chordquiz.app.data.db.GroupManager
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Instrument
import kotlinx.coroutines.flow.Flow

interface GroupsRepository {
    fun getGroupsFlow(instrumentId: String): Flow<List<GroupEntity>>

    suspend fun getGroupById(id: Long): GroupEntity?

    suspend fun insertGroup(group: GroupEntity): Long

    suspend fun updateGroup(id: Long, name: String): Unit

    suspend fun deleteGroup(id: Long): Unit

    suspend fun findGroupByName(instrumentId: String, name: String): GroupEntity?

    fun getChordIdsForGroup(groupId: Long): Flow<List<ChordDefinition>>

    suspend fun deleteChordsFromGroup(group: GroupEntity): Int
}
