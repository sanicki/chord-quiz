package com.chordquiz.app.data.repository

import com.chordquiz.app.data.db.GroupManager
import com.chordquiz.app.data.db.dao.ChordDao
import com.chordquiz.app.data.db.entity.ChordDefinitionEntity
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.model.ChordDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GroupsRepositoryImpl @Inject constructor(
    private val groupManager: GroupManager,
    private val chordDao: ChordDao
) : GroupsRepository {

    override fun getGroupsFlow(instrumentId: String): Flow<List<GroupEntity>> =
        groupManager.getGroupsFlow(instrumentId)

    override suspend fun getGroupById(id: Long): GroupEntity? =
        groupManager.getGroupById(id)

    override suspend fun insertGroup(group: GroupEntity): Long =
        groupManager.insertGroup(group)

    override suspend fun updateGroup(id: Long, name: String): Unit =
        groupManager.updateGroup(id, name)

    override suspend fun deleteGroup(id: Long): Unit =
        groupManager.deleteGroup(id)

    override suspend fun findGroupByName(instrumentId: String, name: String): GroupEntity? =
        groupManager.findGroupByName(instrumentId, name)

    override fun getChordIdsForGroup(groupId: Long): Flow<List<ChordDefinition>> =
        flow {
            val group = groupManager.getGroupById(groupId)
            if (group != null) {
                val chords = group.chordIdsList().mapNotNull { chordId ->
                    chordDao.getChordById(group.instrumentId, chordId)?.toDomain()
                }
                emit(chords)
            } else {
                emit(emptyList())
            }
        }

    override suspend fun deleteChordsFromGroup(group: GroupEntity): Int {
        val chordIdsToRemove = group.chordIdsList()
        if (chordIdsToRemove.isEmpty()) return 0
        return chordDao.deleteChordsById(chordIdsToRemove)
    }
}
