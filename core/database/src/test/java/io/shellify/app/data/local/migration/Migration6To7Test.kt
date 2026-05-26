package io.shellify.app.data.local.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MIGRATION_6_7 verifying the SQL statements and migration version numbers.
 *
 * Full end-to-end Room migration testing (inserting rows, querying after migration) is covered by
 * DatabaseMigrationTest in androidTest, which uses MigrationTestHelper with SQLCipher.
 *
 * These tests validate the migration object's contract — version bounds and SQL correctness —
 * without requiring an Android runtime or instrumentation.
 */
class Migration6To7Test {

    @Test
    fun `migration starts at version 6`() {
        assertEquals(6, MIGRATION_6_7.startVersion)
    }

    @Test
    fun `migration ends at version 7`() {
        assertEquals(7, MIGRATION_6_7.endVersion)
    }

    @Test
    fun `migration executes exactly five ALTER TABLE statements`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_6_7.migrate(stubDb)

        assertEquals(
            "Migration must execute exactly five SQL statements (one per proxy column)",
            5,
            recordedStatements.size,
        )
    }

    @Test
    fun `migration adds custom_proxy_type column as TEXT NOT NULL DEFAULT NONE`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_6_7.migrate(stubDb)

        val sql = recordedStatements.first { it.contains("custom_proxy_type", ignoreCase = true) }
        assertTrue(
            "SQL must target web_apps table",
            sql.contains("web_apps", ignoreCase = true),
        )
        assertTrue(
            "SQL must add custom_proxy_type column",
            sql.contains("custom_proxy_type", ignoreCase = true),
        )
        assertTrue(
            "Column must be TEXT NOT NULL",
            sql.contains("TEXT NOT NULL", ignoreCase = true),
        )
        assertTrue(
            "Column must default to 'NONE'",
            sql.contains("DEFAULT 'NONE'", ignoreCase = true),
        )
    }

    @Test
    fun `migration adds custom_proxy_host column as nullable TEXT`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_6_7.migrate(stubDb)

        val sql = recordedStatements.first { it.contains("custom_proxy_host", ignoreCase = true) }
        assertTrue(
            "SQL must add custom_proxy_host column",
            sql.contains("custom_proxy_host", ignoreCase = true),
        )
        assertTrue(
            "Column must be TEXT (nullable — no NOT NULL)",
            sql.contains("TEXT", ignoreCase = true) && !sql.contains("NOT NULL", ignoreCase = true),
        )
    }

    @Test
    fun `migration adds custom_proxy_port column as INTEGER NOT NULL DEFAULT 0`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_6_7.migrate(stubDb)

        val sql = recordedStatements.first { it.contains("custom_proxy_port", ignoreCase = true) }
        assertTrue(
            "SQL must add custom_proxy_port column",
            sql.contains("custom_proxy_port", ignoreCase = true),
        )
        assertTrue(
            "Column must be INTEGER NOT NULL",
            sql.contains("INTEGER NOT NULL", ignoreCase = true),
        )
        assertTrue(
            "Column must default to 0",
            sql.contains("DEFAULT 0", ignoreCase = true),
        )
    }

    @Test
    fun `migration adds custom_proxy_username column as nullable TEXT`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_6_7.migrate(stubDb)

        val sql = recordedStatements.first { it.contains("custom_proxy_username", ignoreCase = true) }
        assertTrue(
            "SQL must add custom_proxy_username column",
            sql.contains("custom_proxy_username", ignoreCase = true),
        )
        assertTrue(
            "Column must be TEXT (nullable — no NOT NULL)",
            sql.contains("TEXT", ignoreCase = true) && !sql.contains("NOT NULL", ignoreCase = true),
        )
    }

    @Test
    fun `migration adds custom_proxy_password column as nullable TEXT`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_6_7.migrate(stubDb)

        val sql = recordedStatements.first { it.contains("custom_proxy_password", ignoreCase = true) }
        assertTrue(
            "SQL must add custom_proxy_password column",
            sql.contains("custom_proxy_password", ignoreCase = true),
        )
        assertTrue(
            "Column must be TEXT (nullable — no NOT NULL)",
            sql.contains("TEXT", ignoreCase = true) && !sql.contains("NOT NULL", ignoreCase = true),
        )
    }

    @Test
    fun `all migration statements are ALTER TABLE ADD COLUMN statements`() {
        val recordedStatements = mutableListOf<String>()
        val stubDb = StubSupportSQLiteDatabase(recordedStatements)

        MIGRATION_6_7.migrate(stubDb)

        recordedStatements.forEach { sql ->
            assertTrue(
                "Each statement must be ALTER TABLE: $sql",
                sql.trim().startsWith("ALTER TABLE", ignoreCase = true),
            )
            assertTrue(
                "Each statement must use ADD COLUMN: $sql",
                sql.contains("ADD COLUMN", ignoreCase = true),
            )
        }
    }
}
