package io.shellify.app.data.local.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MIGRATION_4_5 verifying the SQL statements and migration version numbers.
 *
 * Full end-to-end Room migration testing (inserting rows, querying after migration) is covered
 * by DatabaseMigrationTest in androidTest, which uses MigrationTestHelper with SQLCipher.
 *
 * These tests validate the migration object's contract — version bounds and SQL correctness —
 * without requiring an Android runtime or instrumentation.
 */
class Migration4To5Test {

    @Test
    fun `migration starts at version 4`() {
        assertEquals(4, MIGRATION_4_5.startVersion)
    }

    @Test
    fun `migration ends at version 5`() {
        assertEquals(5, MIGRATION_4_5.endVersion)
    }

    @Test
    fun `migration creates network_request_logs table`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_4_5.migrate(stubDb)

        val createStatement = recordedStatements.firstOrNull { it.contains("network_request_logs", ignoreCase = true) }
        assertTrue("Migration must create the network_request_logs table", createStatement != null)
        assertTrue(
            "SQL must be a CREATE TABLE IF NOT EXISTS statement",
            createStatement!!.trim().startsWith("CREATE TABLE IF NOT EXISTS", ignoreCase = true),
        )
    }

    @Test
    fun `migration creates table with all required columns`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_4_5.migrate(stubDb)

        val createStatement = recordedStatements.first { it.contains("network_request_logs", ignoreCase = true) }
        assertTrue("Table must have id column", createStatement.contains("id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", ignoreCase = true))
        assertTrue("Table must have app_id column", createStatement.contains("app_id INTEGER NOT NULL", ignoreCase = true))
        assertTrue("Table must have session_id column", createStatement.contains("session_id TEXT NOT NULL", ignoreCase = true))
        assertTrue("Table must have hostname column", createStatement.contains("hostname TEXT NOT NULL", ignoreCase = true))
        assertTrue("Table must have url column", createStatement.contains("url TEXT NOT NULL", ignoreCase = true))
        assertTrue("Table must have is_blocked column", createStatement.contains("is_blocked INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue("Table must have timestamp column", createStatement.contains("timestamp INTEGER NOT NULL", ignoreCase = true))
    }

    @Test
    fun `migration creates FK referencing web_apps with CASCADE delete`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_4_5.migrate(stubDb)

        val createStatement = recordedStatements.first { it.contains("network_request_logs", ignoreCase = true) }
        assertTrue(
            "FK must reference web_apps(id)",
            createStatement.contains("REFERENCES web_apps(id)", ignoreCase = true),
        )
        assertTrue(
            "FK must have ON DELETE CASCADE",
            createStatement.contains("ON DELETE CASCADE", ignoreCase = true),
        )
    }

    @Test
    fun `migration creates three indices`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_4_5.migrate(stubDb)

        val indexStatements = recordedStatements.filter { it.startsWith("CREATE INDEX", ignoreCase = true) }
        assertEquals("Migration must create exactly 3 indices", 3, indexStatements.size)
    }

    @Test
    fun `migration creates index on app_id`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_4_5.migrate(stubDb)

        val hasAppIdIndex = recordedStatements.any {
            it.contains("network_request_logs_app_id", ignoreCase = true) &&
                it.contains("network_request_logs(app_id)", ignoreCase = true)
        }
        assertTrue("Migration must create index on app_id", hasAppIdIndex)
    }

    @Test
    fun `migration creates index on session_id`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_4_5.migrate(stubDb)

        val hasSessionIdIndex = recordedStatements.any {
            it.contains("network_request_logs_session_id", ignoreCase = true) &&
                it.contains("network_request_logs(session_id)", ignoreCase = true)
        }
        assertTrue("Migration must create index on session_id", hasSessionIdIndex)
    }

    @Test
    fun `migration creates index on timestamp`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_4_5.migrate(stubDb)

        val hasTimestampIndex = recordedStatements.any {
            it.contains("network_request_logs_timestamp", ignoreCase = true) &&
                it.contains("network_request_logs(timestamp)", ignoreCase = true)
        }
        assertTrue("Migration must create index on timestamp", hasTimestampIndex)
    }

    @Test
    fun `migration executes exactly 4 SQL statements`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_4_5.migrate(stubDb)

        // 1 CREATE TABLE + 3 CREATE INDEX
        assertEquals("Migration must execute exactly 4 SQL statements", 4, recordedStatements.size)
    }
}
