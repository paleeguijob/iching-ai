package com.iching.ai.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.iching.ai.data.journal.MIGRATION_4_5
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * There is no SQLite engine on the host JVM, so this records the SQL the migration runs.
 * Adding one nullable column is what keeps every existing reading and gives it yearOf = NULL.
 */
private class RecordingConnection : SQLiteConnection {
    val executed = mutableListOf<String>()

    override fun prepare(sql: String): SQLiteStatement {
        executed += sql
        return object : SQLiteStatement {
            override fun bindBlob(index: Int, value: ByteArray) = Unit
            override fun bindDouble(index: Int, value: Double) = Unit
            override fun bindLong(index: Int, value: Long) = Unit
            override fun bindText(index: Int, value: String) = Unit
            override fun bindNull(index: Int) = Unit
            override fun getBlob(index: Int) = ByteArray(0)
            override fun getDouble(index: Int) = 0.0
            override fun getLong(index: Int) = 0L
            override fun getText(index: Int) = ""
            override fun isNull(index: Int) = true
            override fun getColumnCount() = 0
            override fun getColumnName(index: Int) = ""
            override fun getColumnType(index: Int) = 0
            override fun step() = false
            override fun reset() = Unit
            override fun clearBindings() = Unit
            override fun close() = Unit
        }
    }

    override fun close() = Unit
}

class JournalMigrationTest {

    @Test
    fun migrationKeepsReadingsAndAddsYearOf() {
        val connection = RecordingConnection()
        MIGRATION_4_5.migrate(connection)

        assertEquals(4, MIGRATION_4_5.startVersion)
        assertEquals(5, MIGRATION_4_5.endVersion)
        // Only an added nullable column: no table is dropped or rebuilt, so no reading is lost.
        assertEquals(listOf("ALTER TABLE readings ADD COLUMN yearOf INTEGER"), connection.executed)
    }
}
