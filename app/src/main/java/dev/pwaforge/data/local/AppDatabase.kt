package dev.pwaforge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pwaforge.core.crypto.CryptoManager
import dev.pwaforge.data.local.converter.IconSourceConverter
import dev.pwaforge.data.local.dao.CategoryDao
import dev.pwaforge.data.local.dao.WebAppDao
import dev.pwaforge.data.local.entity.CategoryEntity
import dev.pwaforge.data.local.entity.WebAppEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [WebAppEntity::class, CategoryEntity::class],
    version = 10,
    exportSchema = true,
)
@TypeConverters(IconSourceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun webAppDao(): WebAppDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE web_apps ADD COLUMN fullscreenShowStatusBar INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE web_apps ADD COLUMN fullscreenShowNavBar INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE web_apps ADD COLUMN fullscreenShowTopToolbar INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE web_apps ADD COLUMN adBlockAllowUserToggle INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE web_apps ADD COLUMN adBlockCustomRules TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE web_apps ADD COLUMN translateEngine TEXT NOT NULL DEFAULT 'AUTO'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT NOT NULL DEFAULT 'folder'")
                db.execSQL("ALTER TABLE categories ADD COLUMN color TEXT NOT NULL DEFAULT '#6D28D9'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE web_apps ADD COLUMN passwordHash TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE web_apps ADD COLUMN lockType TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE web_apps ADD COLUMN engineType TEXT NOT NULL DEFAULT 'SYSTEM_WEBVIEW'")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE web_apps ADD COLUMN wipeOnFailedAttempts INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE web_apps ADD COLUMN icon_source TEXT DEFAULT NULL")
                db.execSQL(
                    "UPDATE web_apps SET icon_source = json_object('type','path','path',icon_path) WHERE icon_path IS NOT NULL"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE web_apps ADD COLUMN has_launcher_shortcut INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE web_apps ADD COLUMN libreTranslateUrl TEXT NOT NULL DEFAULT 'https://libretranslate.com'")
            }
        }

        fun getInstance(context: Context, crypto: CryptoManager): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context, crypto).also { instance = it }
            }

        private fun buildDatabase(context: Context, crypto: CryptoManager): AppDatabase {
            // Passphrase is a random 32-byte secret encrypted at rest in SharedPreferences.
            // Decrypted here via Android Keystore, passed directly to SupportFactory, then zeroed.
            val passphrase = crypto.databasePassphrase()
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "pwaforge.db",
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .build()
                .also { passphrase.fill(0) }
        }
    }
}
