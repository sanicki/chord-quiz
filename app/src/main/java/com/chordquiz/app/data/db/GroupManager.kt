package com.chordquiz.app.data.db

import com.chordquiz.app.data.db.dao.GroupDao
import com.chordquiz.app.data.db.entity.GroupEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GroupManager @Inject constructor(
    private val dao: GroupDao
) {
    fun getGroupsFlow(instrumentId: String): Flow<List<GroupEntity>> =
        dao.getAllGroups(instrumentId)

    suspend fun getGroupById(id: Long): GroupEntity? = dao.getGroupById(id)

    suspend fun insertGroup(group: GroupEntity): Long = dao.insert(group)

    suspend fun updateGroup(id: Long, name: String) = dao.updateGroup(id, name)

    suspend fun deleteGroup(id: Long) = dao.deleteGroup(id)

    suspend fun findGroupByName(instrumentId: String, name: String): GroupEntity? =
        dao.findGroupByName(instrumentId, name)
}
