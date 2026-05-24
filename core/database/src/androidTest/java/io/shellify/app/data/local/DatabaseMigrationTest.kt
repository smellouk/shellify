package io.shellify.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.data.local.migration.MIGRATION_2_3
import io.shellify.app.data.local.migration.MIGRATION_3_4
import io.shellify.app.data.local.migration.MIGRATION_4_5
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that the Room schema at each database version matches the exported JSON baseline,
 * and that every Migration object correctly transforms the schema between versions.
 *
 * To add a migration test when bumping AppDatabase.version from N to N+1:
 *  1. Add a `Migration(N, N+1)` constant in a Migrations.kt file under this package.
 *  2. Register it in AppDatabase.buildDatabase via .addMigrations().
 *  3. Add a @Test here: open at version N, call runMigrationsAndValidate(N+1, migration), assert data.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule
    val migrationHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        SupportOpenHelperFactory(TEST_PASSPHRASE.copyOf()),
    )

    @Before
    fun loadSqlCipherLibrary() {
        System.loadLibrary("sqlcipher")
    }

    // ── Version 1 schema validation ───────────────────────────────────────────

    @Test
    fun version1_webAppsTableHasExpectedColumns() {
        migrationHelper.createDatabase(TEST_DB_NAME, 1).use { db ->
            // Querying all columns verifies Room generated the exact set declared in WebAppEntity.
            db.query(
                "SELECT id, name, url, iconPath, icon_source, themeColor, backgroundColor, " +
                    "description, categoryId, isolationId, isFullscreen, fullscreenShowStatusBar, " +
                    "fullscreenShowNavBar, fullscreenShowTopToolbar, adBlockEnabled, " +
                    "adBlockAllowUserToggle, adBlockCustomRules, translateEnabled, translateTarget, " +
                    "translateEngine, showTranslateButton, autoTranslateOnLoad, libreTranslateUrl, " +
                    "libreTranslateApiKey, uaMode, createdAt, updatedAt, passwordHash, lockType, " +
                    "engineType, wipeOnFailedAttempts, has_launcher_shortcut FROM web_apps LIMIT 0"
            ).close()
        }
    }

    @Test
    fun version1_categoriesTableHasExpectedColumns() {
        migrationHelper.createDatabase(TEST_DB_NAME, 1).use { db ->
            db.query("SELECT id, name, sortIndex, icon, color FROM categories LIMIT 0").close()
        }
    }

    @Test
    fun version1_canInsertAndQueryCategory() {
        migrationHelper.createDatabase(TEST_DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO categories (name, sortIndex, icon, color) VALUES ('Work', 0, 'folder', '#6D28D9')"
            )
            db.query("SELECT id FROM categories WHERE name = 'Work'").use { cursor ->
                assertTrue("Category row not found after insert", cursor.moveToFirst())
            }
        }
    }

    @Test
    fun version1_canInsertWebAppLinkedToCategory() {
        migrationHelper.createDatabase(TEST_DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, name, sortIndex, icon, color) VALUES (1, 'Dev', 0, 'folder', '#6D28D9')"
            )
            db.execSQL(
                """INSERT INTO web_apps (
                    name, url, isolationId, categoryId,
                    adBlockEnabled, adBlockAllowUserToggle, adBlockCustomRules,
                    translateEnabled, translateTarget, translateEngine,
                    showTranslateButton, autoTranslateOnLoad,
                    libreTranslateUrl, libreTranslateApiKey,
                    uaMode, createdAt, updatedAt,
                    lockType, engineType, wipeOnFailedAttempts,
                    isFullscreen, fullscreenShowStatusBar, fullscreenShowNavBar,
                    fullscreenShowTopToolbar, has_launcher_shortcut
                ) VALUES (
                    'GitHub', 'https://github.com', 'iso-abc-123', 1,
                    1, 0, '',
                    0, 'en', 'AUTO',
                    1, 0,
                    'https://libretranslate.com', '',
                    'CHROME_MOBILE', 0, 0,
                    'NONE', 'SYSTEM_WEBVIEW', 0,
                    0, 0, 0,
                    0, 0
                )"""
            )
            db.query("SELECT id, name FROM web_apps WHERE categoryId = 1").use { cursor ->
                assertTrue("WebApp row not found after insert", cursor.moveToFirst())
            }
        }
    }

    @Test
    fun version1_deletingCategoryNullsOutWebAppForeignKey() {
        migrationHelper.createDatabase(TEST_DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, name, sortIndex, icon, color) VALUES (1, 'Temp', 0, 'folder', '#000000')"
            )
            db.execSQL(
                """INSERT INTO web_apps (
                    name, url, isolationId, categoryId,
                    adBlockEnabled, adBlockAllowUserToggle, adBlockCustomRules,
                    translateEnabled, translateTarget, translateEngine,
                    showTranslateButton, autoTranslateOnLoad,
                    libreTranslateUrl, libreTranslateApiKey,
                    uaMode, createdAt, updatedAt,
                    lockType, engineType, wipeOnFailedAttempts,
                    isFullscreen, fullscreenShowStatusBar, fullscreenShowNavBar,
                    fullscreenShowTopToolbar, has_launcher_shortcut
                ) VALUES (
                    'App', 'https://example.com', 'iso-xyz', 1,
                    1, 0, '',
                    0, 'en', 'AUTO',
                    1, 0,
                    'https://libretranslate.com', '',
                    'CHROME_MOBILE', 0, 0,
                    'NONE', 'SYSTEM_WEBVIEW', 0,
                    0, 0, 0,
                    0, 0
                )"""
            )
            // MigrationTestHelper doesn't run PRAGMA foreign_keys = ON automatically the way
            // Room's own open helper does — enable it explicitly before the DELETE.
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM categories WHERE id = 1")
            db.query("SELECT categoryId FROM web_apps WHERE name = 'App'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                // ForeignKey(onDelete = SET_NULL) — categoryId must be NULL after parent deleted
                assertTrue("categoryId should be NULL after category deletion", cursor.isNull(0))
            }
        }
    }

    // ── Migration 2 → 3 ───────────────────────────────────────────────────────

    @Test
    fun migrate2To3_addsNewColumnsAndNotificationsTable() {
        // Step 1: create a v2 database with one WebApp row.
        migrationHelper.createDatabase(TEST_DB_NAME, 2).use { db ->
            db.execSQL(
                """INSERT INTO web_apps (
                    id, name, url, isolationId,
                    adBlockEnabled, adBlockAllowUserToggle, adBlockCustomRules,
                    translateEnabled, translateTarget, translateEngine,
                    showTranslateButton, autoTranslateOnLoad,
                    libreTranslateUrl, libreTranslateApiKey,
                    uaMode, createdAt, updatedAt,
                    lockType, engineType, wipeOnFailedAttempts,
                    isFullscreen, fullscreenShowStatusBar, fullscreenShowNavBar,
                    fullscreenShowTopToolbar, has_launcher_shortcut, show_control_center
                ) VALUES (
                    1, 'TestApp', 'https://test.com', 'iso-test-001',
                    1, 0, '',
                    0, 'en', 'AUTO',
                    1, 0,
                    'https://libretranslate.com', '',
                    'CHROME_MOBILE', 0, 0,
                    'NONE', 'SYSTEM_WEBVIEW', 0,
                    0, 0, 0,
                    0, 0, 1
                )"""
            )
        }

        // Step 2: run the migration and validate the schema matches 3.json.
        migrationHelper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, MIGRATION_2_3).use { db ->

            // Step 3: verify the four new columns exist on web_apps with correct defaults.
            db.query("SELECT notification_permission, dnd_start_hour, dnd_end_hour, background_notifications_enabled FROM web_apps WHERE id = 1").use { cursor ->
                assertTrue("web_apps row not found after migration", cursor.moveToFirst())
                assertEquals("NOT_ASKED", cursor.getString(0))
                assertEquals(-1, cursor.getInt(1))
                assertEquals(-1, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
            }

            // Step 4: verify the notifications table exists (PRAGMA table_info).
            db.query("PRAGMA table_info(notifications)").use { cursor ->
                assertTrue("notifications table should exist after migration 2→3", cursor.moveToFirst())
            }

            // Step 5: insert a notification referencing the migrated WebApp.
            db.execSQL(
                "INSERT INTO notifications (app_id, title, body, timestamp, is_read) VALUES (1, 'Hello', 'World', 1000000, 0)"
            )

            // Step 6: query it back and verify correctness.
            db.query("SELECT app_id, title, body, is_read FROM notifications WHERE app_id = 1").use { cursor ->
                assertTrue("Notification row not found after insert", cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(0))
                assertEquals("Hello", cursor.getString(1))
                assertEquals("World", cursor.getString(2))
                assertFalse("is_read should be 0 (false) by default", cursor.getInt(3) != 0)
            }
        }
    }

    // ── Migration 3 → 4 ───────────────────────────────────────────────────────

    @Test
    fun migrate3To4_addsSwipeToRefreshEnabledColumnWithDefaultTrue() {
        // Step 1: create a v3 database with one WebApp row (no swipe_to_refresh_enabled column).
        migrationHelper.createDatabase(TEST_DB_NAME, 3).use { db ->
            db.execSQL(
                """INSERT INTO web_apps (
                    id, name, url, isolationId,
                    adBlockEnabled, adBlockAllowUserToggle, adBlockCustomRules,
                    translateEnabled, translateTarget, translateEngine,
                    showTranslateButton, autoTranslateOnLoad,
                    libreTranslateUrl, libreTranslateApiKey,
                    uaMode, createdAt, updatedAt,
                    lockType, engineType, wipeOnFailedAttempts,
                    isFullscreen, fullscreenShowStatusBar, fullscreenShowNavBar,
                    fullscreenShowTopToolbar, has_launcher_shortcut, show_control_center,
                    notification_permission, dnd_start_hour, dnd_end_hour,
                    background_notifications_enabled
                ) VALUES (
                    1, 'RefreshApp', 'https://refresh.example.com', 'iso-refresh-001',
                    1, 0, '',
                    0, 'en', 'AUTO',
                    1, 0,
                    'https://libretranslate.com', '',
                    'CHROME_MOBILE', 0, 0,
                    'NONE', 'SYSTEM_WEBVIEW', 0,
                    0, 0, 0,
                    0, 0, 1,
                    'NOT_ASKED', -1, -1,
                    0
                )"""
            )
        }

        // Step 2: run the migration and validate the schema matches 4.json.
        migrationHelper.runMigrationsAndValidate(TEST_DB_NAME, 4, true, MIGRATION_3_4).use { db ->

            // Step 3: verify swipe_to_refresh_enabled defaults to 1 for the existing row.
            db.query(
                "SELECT swipe_to_refresh_enabled FROM web_apps WHERE id = 1"
            ).use { cursor ->
                assertTrue("web_apps row not found after migration 3→4", cursor.moveToFirst())
                assertEquals(
                    "swipe_to_refresh_enabled must be 1 (DEFAULT 1) after migration",
                    1,
                    cursor.getInt(0),
                )
            }
        }
    }

    // ── Migration 4 → 5 ───────────────────────────────────────────────────────

    @Test
    fun migrate4To5_createsNetworkRequestLogsTableWithIndices() {
        // Step 1: create a v4 database with one WebApp row.
        migrationHelper.createDatabase(TEST_DB_NAME, 4).use { db ->
            db.execSQL(
                """INSERT INTO web_apps (
                    id, name, url, isolationId,
                    adBlockEnabled, adBlockAllowUserToggle, adBlockCustomRules,
                    translateEnabled, translateTarget, translateEngine,
                    showTranslateButton, autoTranslateOnLoad,
                    libreTranslateUrl, libreTranslateApiKey,
                    uaMode, createdAt, updatedAt,
                    lockType, engineType, wipeOnFailedAttempts,
                    isFullscreen, fullscreenShowStatusBar, fullscreenShowNavBar,
                    fullscreenShowTopToolbar, has_launcher_shortcut, show_control_center,
                    notification_permission, dnd_start_hour, dnd_end_hour,
                    background_notifications_enabled, swipe_to_refresh_enabled
                ) VALUES (
                    1, 'NetApp', 'https://net.example.com', 'iso-net-001',
                    1, 0, '',
                    0, 'en', 'AUTO',
                    1, 0,
                    'https://libretranslate.com', '',
                    'CHROME_MOBILE', 0, 0,
                    'NONE', 'SYSTEM_WEBVIEW', 0,
                    0, 0, 0,
                    0, 0, 1,
                    'NOT_ASKED', -1, -1,
                    0, 1
                )"""
            )
        }

        // Step 2: run migration and validate schema matches 5.json.
        migrationHelper.runMigrationsAndValidate(TEST_DB_NAME, 5, true, MIGRATION_4_5).use { db ->

            // Step 3: verify the network_request_logs table exists and has expected columns.
            db.query(
                "SELECT id, app_id, session_id, hostname, url, is_blocked, timestamp " +
                    "FROM network_request_logs LIMIT 0"
            ).close()

            // Step 4: insert a row referencing the migrated WebApp.
            db.execSQL(
                "INSERT INTO network_request_logs (app_id, session_id, hostname, url, is_blocked, timestamp) " +
                    "VALUES (1, 'sess-abc', 'example.com', 'https://example.com/api', 0, 1000000)"
            )

            // Step 5: query it back.
            db.query("SELECT hostname, is_blocked FROM network_request_logs WHERE app_id = 1").use { cursor ->
                assertTrue("network_request_logs row not found after insert", cursor.moveToFirst())
                assertEquals("example.com", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }

            // Step 6: verify ON DELETE CASCADE — deleting the WebApp removes its logs.
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM web_apps WHERE id = 1")
            db.query("SELECT id FROM network_request_logs WHERE app_id = 1").use { cursor ->
                assertFalse("network_request_logs rows must be deleted when WebApp is deleted", cursor.moveToFirst())
            }
        }
    }

    companion object {
        private const val TEST_DB_NAME = "migration-test.db"

        // Fixed passphrase for instrumented tests only — never used in production.
        private val TEST_PASSPHRASE = "shellify-test-passphrase".toByteArray()
    }
}
