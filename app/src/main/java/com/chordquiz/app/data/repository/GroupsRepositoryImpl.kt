package com.chordquiz.app.data.repository

import com.chordquiz.app.data.db.dao.ChordDao
import com.chordquiz.app.data.db.dao.GroupDao
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.domain.ChordDifficultyCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GroupsRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val chordDao: ChordDao
) : GroupsRepository {

    override fun getGroupsFlow(instrumentId: String): Flow<List<GroupEntity>> =
        groupDao.getAllGroups(instrumentId)

    override suspend fun getGroupById(id: Long): GroupEntity? =
        groupDao.getGroupById(id)

    override suspend fun insertGroup(group: GroupEntity): Long =
        groupDao.insert(group)

    override suspend fun updateGroup(id: Long, name: String): Unit =
        groupDao.updateGroup(id, name)

    override suspend fun deleteGroup(id: Long) {
        val group = groupDao.getGroupById(id)
        group?.let { groupDao.deleteGroup(it) }
    }

    override suspend fun findGroupByName(instrumentId: String, name: String): GroupEntity? =
        groupDao.findGroupByName(instrumentId, name)

    override fun getChordIdsForGroup(groupId: Long): Flow<List<ChordDefinition>> =
        flow {
            val group = groupDao.getGroupById(groupId)
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

    override fun computeDifficultyGroups(instrumentId: String, chords: List<ChordDefinition>): List<GroupEntity> =
        ChordDifficultyCalculator.buildDifficultyGroups(instrumentId, chords)
}
