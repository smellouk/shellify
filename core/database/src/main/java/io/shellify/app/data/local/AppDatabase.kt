package io.shellify.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.shellify.app.core.crypto.CryptoManager
import io.shellify.app.data.local.converter.IconSourceConverter
import io.shellify.app.data.local.dao.CategoryDao
import io.shellify.app.data.local.dao.NetworkRequestLogDao
import io.shellify.app.data.local.dao.NotificationDao
import io.shellify.app.data.local.dao.WebAppDao
import io.shellify.app.data.local.entity.CategoryEntity
import io.shellify.app.data.local.entity.NetworkRequestLogEntity
import io.shellify.app.data.local.entity.NotificationEntity
import io.shellify.app.data.local.entity.WebAppEntity
import io.shellify.app.data.local.migration.MIGRATION_1_2
import io.shellify.app.data.local.migration.MIGRATION_2_3
import io.shellify.app.data.local.migration.MIGRATION_3_4
import io.shellify.app.data.local.migration.MIGRATION_4_5
import io.shellify.app.data.local.migration.MIGRATION_5_6
import io.shellify.app.data.local.migration.MIGRATION_6_7
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [WebAppEntity::class, CategoryEntity::class, NotificationEntity::class, NetworkRequestLogEntity::class],
    version = 7,
    exportSchema = true,
)
@TypeConverters(IconSourceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun webAppDao(): WebAppDao
    abstract fun categoryDao(): CategoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun networkRequestLogDao(): NetworkRequestLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context, crypto: CryptoManager): AppDatabase {
            System.loadLibrary("sqlcipher")
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context, crypto).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context, crypto: CryptoManager): AppDatabase {
            // Passphrase is a random 32-byte secret encrypted at rest in SharedPreferences.
            // Zeroed immediately after the factory copies it — before Room.build() runs.
            val passphrase = crypto.databasePassphrase()
            val factory = try {
                SupportOpenHelperFactory(passphrase)
            } finally {
                passphrase.fill(0)
            }
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "shellify.db",
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
        }
    }
}
