package com.chordquiz.app.data.repository

import com.chordquiz.app.data.db.entity.ChordDefinitionEntity
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.db.entity.InstrumentEntity
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Instrument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GroupsRepositoryImpl @Inject constructor(
    private val groupManager: GroupManager,
    private val chordDao: ChordRepository
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
        groupManager.getGroupsFlow("") // Get all groups across all instruments
            .map { groups ->
                groups.find { it.id == groupId }?.let { group ->
                    // Use the group's instrumentId, not chordDao.instrumentId
                    group.chordIdsList().map { chordId ->
                        chordDao.getChordById(group.instrumentId, chordId)
                            ?.toDomain() // Convert to domain model
                    } ?: emptyList()
                } ?: emptyList()
            }

    override suspend fun deleteChordsFromGroup(group: GroupEntity): Int {
        val chordIdsToRemove = group.chordIdsList()
        if (chordIdsToRemove.isEmpty()) return 0
        val deletedCount = chordDao.deleteChordsById(group.chordIdsList())
        return deletedCount
    }
}
