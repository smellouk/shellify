package io.shellify.app.data.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.data.local.AppDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented migration test for MIGRATION_5_6.
 *
 * Verifies that upgrading an existing v5 database to v6 adds all 6 privacy/Tor columns
 * to `web_apps` with DEFAULT 0, and that existing rows survive the upgrade unchanged.
 */
@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        SupportOpenHelperFactory(TEST_PASSPHRASE.copyOf()),
    )

    @Before
    fun loadSqlCipherLibrary() {
        System.loadLibrary("sqlcipher")
    }

    @Test
    fun migrate5To6_addsPrivacyAndTorColumns() {
        // Step 1: create a v5 database with one existing WebApp row.
        helper.createDatabase(TEST_DB, 5).use { db ->
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
                    1, 'PrivacyApp', 'https://example.com', 'iso-priv-001',
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

        // Step 2: run MIGRATION_5_6 and validate the schema matches 6.json.
        helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6).use { db ->

            // Step 3: verify all 6 new columns exist and default to 0 for the migrated row.
            db.query(
                "SELECT stealth_mode, cookie_auto_wipe, always_incognito, " +
                    "tracker_blocking_enabled, use_tor, preserve_tor_identity " +
                    "FROM web_apps WHERE id = 1"
            ).use { cursor ->
                assertTrue("web_apps row not found after migration 5→6", cursor.moveToFirst())
                assertEquals("stealth_mode must default to 0", 0, cursor.getInt(0))
                assertEquals("cookie_auto_wipe must default to 0", 0, cursor.getInt(1))
                assertEquals("always_incognito must default to 0", 0, cursor.getInt(2))
                assertEquals("tracker_blocking_enabled must default to 0", 0, cursor.getInt(3))
                assertEquals("use_tor must default to 0", 0, cursor.getInt(4))
                assertEquals("preserve_tor_identity must default to 0", 0, cursor.getInt(5))
            }

            // Step 4: insert a new row with the 6 privacy/Tor columns set to 1 and verify round-trip.
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
                    background_notifications_enabled, swipe_to_refresh_enabled,
                    stealth_mode, cookie_auto_wipe, always_incognito,
                    tracker_blocking_enabled, use_tor, preserve_tor_identity
                ) VALUES (
                    2, 'TorApp', 'https://tor.example.com', 'iso-tor-001',
                    1, 0, '',
                    0, 'en', 'AUTO',
                    1, 0,
                    'https://libretranslate.com', '',
                    'GECKOVIEW', 0, 0,
                    'NONE', 'GECKOVIEW', 0,
                    0, 0, 0,
                    0, 0, 1,
                    'NOT_ASKED', -1, -1,
                    0, 1,
                    1, 1, 1,
                    1, 1, 1
                )"""
            )

            // Step 5: query back and verify the 6 columns are stored as 1.
            db.query(
                "SELECT stealth_mode, cookie_auto_wipe, always_incognito, " +
                    "tracker_blocking_enabled, use_tor, preserve_tor_identity " +
                    "FROM web_apps WHERE id = 2"
            ).use { cursor ->
                assertTrue("new TorApp row not found after insert", cursor.moveToFirst())
                assertEquals("stealth_mode should be 1", 1, cursor.getInt(0))
                assertEquals("cookie_auto_wipe should be 1", 1, cursor.getInt(1))
                assertEquals("always_incognito should be 1", 1, cursor.getInt(2))
                assertEquals("tracker_blocking_enabled should be 1", 1, cursor.getInt(3))
                assertEquals("use_tor should be 1", 1, cursor.getInt(4))
                assertEquals("preserve_tor_identity should be 1", 1, cursor.getInt(5))
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-5-6-test.db"

        // Fixed passphrase for instrumented tests only — never used in production.
        private val TEST_PASSPHRASE = "shellify-test-passphrase".toByteArray()
    }
}
