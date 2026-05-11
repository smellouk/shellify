package dev.pwaforge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pwaforge.core.crypto.CryptoManager
import dev.pwaforge.data.local.dao.CategoryDao
import dev.pwaforge.data.local.dao.WebAppDao
import dev.pwaforge.data.local.entity.CategoryEntity
import dev.pwaforge.data.local.entity.WebAppEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [WebAppEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = true,
)
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
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { passphrase.fill(0) }
        }
    }
}
