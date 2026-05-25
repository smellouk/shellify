package io.shellify.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE web_apps ADD COLUMN show_control_center INTEGER NOT NULL DEFAULT 1"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // New columns on web_apps for notification support
        db.execSQL("ALTER TABLE web_apps ADD COLUMN notification_permission TEXT NOT NULL DEFAULT 'NOT_ASKED'")
        db.execSQL("ALTER TABLE web_apps ADD COLUMN dnd_start_hour INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE web_apps ADD COLUMN dnd_end_hour INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE web_apps ADD COLUMN background_notifications_enabled INTEGER NOT NULL DEFAULT 0")
        // New notifications table with FK to web_apps(id) with CASCADE delete
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                app_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                body TEXT,
                icon_url TEXT,
                timestamp INTEGER NOT NULL,
                is_read INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (app_id) REFERENCES web_apps(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_app_id ON notifications(app_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_timestamp ON notifications(timestamp)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE web_apps ADD COLUMN swipe_to_refresh_enabled INTEGER NOT NULL DEFAULT 1"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS network_request_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                app_id INTEGER NOT NULL,
                session_id TEXT NOT NULL,
                hostname TEXT NOT NULL,
                url TEXT NOT NULL,
                is_blocked INTEGER NOT NULL DEFAULT 0,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (app_id) REFERENCES web_apps(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_network_request_logs_app_id ON network_request_logs(app_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_network_request_logs_session_id ON network_request_logs(session_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_network_request_logs_timestamp ON network_request_logs(timestamp)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE web_apps ADD COLUMN stealth_mode INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE web_apps ADD COLUMN cookie_auto_wipe INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE web_apps ADD COLUMN always_incognito INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE web_apps ADD COLUMN tracker_blocking_enabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE web_apps ADD COLUMN use_tor INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE web_apps ADD COLUMN preserve_tor_identity INTEGER NOT NULL DEFAULT 0")
    }
}
