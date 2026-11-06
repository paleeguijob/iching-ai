package com.iching.ai.data

import com.iching.ai.data.journal.DeletedReadingEntity
import com.iching.ai.data.journal.ReadingDao
import com.iching.ai.data.journal.ReadingEntity
import com.iching.ai.data.journal.RoomJournalRepository
import com.iching.ai.data.journal.UnsyncedOutcomeEntity
import com.iching.ai.data.journal.toEntity
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeDao : ReadingDao {
    val rows = mutableListOf<ReadingEntity>()
    val deleted = mutableSetOf<String>()
    override fun observeAll(): Flow<List<ReadingEntity>> = flowOf(rows)
    override suspend fun get(id: Long) = rows.firstOrNull { it.id == id }
    override suspend fun serverIds() = rows.mapNotNull { it.serverId }
    override suspend fun upsert(entity: ReadingEntity): Long {
        rows += entity
        return rows.size.toLong()
    }
    override suspend fun delete(id: Long) {
        rows.removeAll { it.id == id }
    }
    override suspend fun clear() = rows.clear()
    override suspend fun markDeleted(entity: DeletedReadingEntity) {
        deleted += entity.serverId
    }
    override suspend fun deletedServerIds() = deleted.toList()
    override suspend fun deletionSynced(serverId: String) {
        deleted -= serverId
    }
    override suspend fun clearDeleted() = deleted.clear()
    val unsynced = mutableSetOf<String>()
    override suspend fun markOutcomeUnsynced(entity: UnsyncedOutcomeEntity) {
        unsynced += entity.serverId
    }
    override suspend fun unsyncedOutcomes() = rows.filter { it.serverId in unsynced }
    override suspend fun outcomeSynced(serverId: String) {
        unsynced -= serverId
    }
    override suspend fun clearUnsyncedOutcomes() = unsynced.clear()
}

private fun serverReading(id: String) = Reading(
    question = "q", createdAtMillis = 1, lines = List(6) { LineType.YOUNG_YANG }, primaryId = 1, relatingId = null,
    interpretation = "text", source = InterpretationSource.AI, serverId = id,
)

class RoomJournalRepositoryTest {

    @Test
    fun mergeSkipsReadingsWaitingToBeDeleted() = runTest {
        val dao = FakeDao()
        val repo = RoomJournalRepository(dao)
        repo.markDeleted("gone")

        val added = repo.mergeFromServer(listOf(serverReading("gone"), serverReading("new")))

        assertEquals(1, added)
        assertEquals(listOf("new"), dao.rows.map { it.serverId })
    }

    @Test
    fun unsyncedOutcomesReturnTheMarkedReadings() = runTest {
        val dao = FakeDao()
        val repo = RoomJournalRepository(dao)
        repo.save(serverReading("a"))
        repo.save(serverReading("b"))

        repo.markOutcomeUnsynced("a")
        assertEquals(listOf("a"), repo.unsyncedOutcomes().map { it.serverId })

        repo.outcomeSynced("a")
        assertEquals(emptyList(), repo.unsyncedOutcomes())
    }

    @Test
    fun deletingAReadingForgetsItsUnsyncedOutcome() = runTest {
        val dao = FakeDao()
        val repo = RoomJournalRepository(dao)
        dao.rows += serverReading("a").toEntity().copy(id = 7)
        repo.markOutcomeUnsynced("a")

        repo.delete(7)

        assertEquals(emptySet(), dao.unsynced)
    }

    @Test
    fun clearForgetsUnsyncedOutcomes() = runTest {
        val dao = FakeDao()
        val repo = RoomJournalRepository(dao)
        repo.markOutcomeUnsynced("a")

        repo.clear()

        assertEquals(emptySet(), dao.unsynced)
    }

    @Test
    fun clearForgetsPendingDeletions() = runTest {
        val dao = FakeDao()
        val repo = RoomJournalRepository(dao)
        repo.markDeleted("gone")

        repo.clear()

        assertEquals(emptyList(), repo.pendingDeletions())
    }
}
