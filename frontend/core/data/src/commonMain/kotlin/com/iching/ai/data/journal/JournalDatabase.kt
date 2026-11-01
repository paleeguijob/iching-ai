package com.iching.ai.data.journal

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.iching.ai.domain.model.FollowUpTurn
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val createdAtMillis: Long,
    /** Six line values bottom to top, e.g. "789867". */
    val lineValues: String,
    val primaryId: Int,
    val relatingId: Int?,
    val interpretation: String,
    val source: String,
    // ---- added in version 2 ----
    val serverId: String? = null,
    val category: String? = null,
    val outcome: String? = null,
    val outcomeNote: String? = null,
    /** JSON array of {q, a}. */
    val followUpsJson: String? = null,
)

/** A server reading the seeker deleted here that the server has not confirmed deleting yet. */
@Entity(tableName = "deleted_readings")
data class DeletedReadingEntity(@PrimaryKey val serverId: String)

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE id = :id")
    suspend fun get(id: Long): ReadingEntity?

    @Query("SELECT serverId FROM readings WHERE serverId IS NOT NULL")
    suspend fun serverIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReadingEntity): Long

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM readings")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markDeleted(entity: DeletedReadingEntity)

    @Query("SELECT serverId FROM deleted_readings")
    suspend fun deletedServerIds(): List<String>

    @Query("DELETE FROM deleted_readings WHERE serverId = :serverId")
    suspend fun deletionSynced(serverId: String)

    @Query("DELETE FROM deleted_readings")
    suspend fun clearDeleted()
}

@Database(entities = [ReadingEntity::class, DeletedReadingEntity::class], version = 3)
@ConstructedBy(JournalDatabaseConstructor::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
}

/** v1 → v2: server id, category, "did it come true?" outcome and follow-up turns. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE readings ADD COLUMN serverId TEXT")
        connection.execSQL("ALTER TABLE readings ADD COLUMN category TEXT")
        connection.execSQL("ALTER TABLE readings ADD COLUMN outcome TEXT")
        connection.execSQL("ALTER TABLE readings ADD COLUMN outcomeNote TEXT")
        connection.execSQL("ALTER TABLE readings ADD COLUMN followUpsJson TEXT")
    }
}

/** v2 → v3: server readings deleted on this device, so a journal sync does not bring them back. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `deleted_readings` (`serverId` TEXT NOT NULL, PRIMARY KEY(`serverId`))")
    }
}

// Room generates the actual implementations for each platform.
@Suppress("KotlinNoActualForExpect")
expect object JournalDatabaseConstructor : RoomDatabaseConstructor<JournalDatabase> {
    override fun initialize(): JournalDatabase
}

@Serializable
private data class TurnJson(val q: String, val a: String)

private val json = Json { ignoreUnknownKeys = true }
private val turnsSerializer = ListSerializer(TurnJson.serializer())

internal fun ReadingEntity.toDomain() = Reading(
    id = id,
    question = question,
    createdAtMillis = createdAtMillis,
    lines = lineValues.map { LineType.fromValue(it.digitToInt()) },
    primaryId = primaryId,
    relatingId = relatingId,
    interpretation = interpretation,
    source = InterpretationSource.valueOf(source),
    serverId = serverId,
    category = QuestionCategory.fromWire(category),
    outcome = outcome?.let { runCatching { Outcome.valueOf(it) }.getOrNull() },
    outcomeNote = outcomeNote,
    followUps = followUpsJson
        ?.let { runCatching { json.decodeFromString(turnsSerializer, it) }.getOrNull() }
        .orEmpty()
        .map { FollowUpTurn(it.q, it.a) },
)

internal fun Reading.toEntity() = ReadingEntity(
    id = id,
    question = question,
    createdAtMillis = createdAtMillis,
    lineValues = lines.joinToString("") { it.value.toString() },
    primaryId = primaryId,
    relatingId = relatingId,
    interpretation = interpretation,
    source = source.name,
    serverId = serverId,
    category = category?.wire,
    outcome = outcome?.name,
    outcomeNote = outcomeNote,
    followUpsJson = if (followUps.isEmpty()) null else json.encodeToString(turnsSerializer, followUps.map { TurnJson(it.question, it.answer) }),
)

class RoomJournalRepository(private val dao: ReadingDao) : JournalRepository {
    override fun observeAll(): Flow<List<Reading>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun get(id: Long): Reading? = dao.get(id)?.toDomain()
    override suspend fun save(reading: Reading): Long = dao.upsert(reading.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun markDeleted(serverId: String) = dao.markDeleted(DeletedReadingEntity(serverId))
    override suspend fun pendingDeletions(): List<String> = dao.deletedServerIds()
    override suspend fun deletionSynced(serverId: String) = dao.deletionSynced(serverId)

    override suspend fun clear() {
        dao.clear()
        dao.clearDeleted()
    }

    override suspend fun mergeFromServer(readings: List<Reading>): Int {
        val known = dao.serverIds().toSet() + dao.deletedServerIds()
        val fresh = readings.filter { it.serverId != null && it.serverId !in known }
        fresh.forEach { dao.upsert(it.copy(id = 0).toEntity()) }
        return fresh.size
    }
}
