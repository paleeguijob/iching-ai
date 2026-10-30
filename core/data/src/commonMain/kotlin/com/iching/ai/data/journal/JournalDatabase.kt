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
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
)

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE id = :id")
    suspend fun get(id: Long): ReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReadingEntity): Long

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [ReadingEntity::class], version = 1)
@ConstructedBy(JournalDatabaseConstructor::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
}

// Room generates the actual implementations for each platform.
@Suppress("KotlinNoActualForExpect")
expect object JournalDatabaseConstructor : RoomDatabaseConstructor<JournalDatabase> {
    override fun initialize(): JournalDatabase
}

internal fun ReadingEntity.toDomain() = Reading(
    id = id,
    question = question,
    createdAtMillis = createdAtMillis,
    lines = lineValues.map { LineType.fromValue(it.digitToInt()) },
    primaryId = primaryId,
    relatingId = relatingId,
    interpretation = interpretation,
    source = InterpretationSource.valueOf(source),
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
)

class RoomJournalRepository(private val dao: ReadingDao) : JournalRepository {
    override fun observeAll(): Flow<List<Reading>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun get(id: Long): Reading? = dao.get(id)?.toDomain()
    override suspend fun save(reading: Reading): Long = dao.upsert(reading.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
}
