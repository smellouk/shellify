package io.shellify.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.shellify.app.core.crypto.CryptoManager
import io.shellify.app.data.local.converter.IconSourceConverter
import io.shellify.app.data.local.dao.CategoryDao
import io.shellify.app.data.local.dao.WebAppDao
import io.shellify.app.data.local.entity.CategoryEntity
import io.shellify.app.data.local.entity.WebAppEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [WebAppEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(IconSourceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun webAppDao(): WebAppDao
    abstract fun categoryDao(): CategoryDao

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
            // Decrypted here via Android Keystore, passed directly to SupportOpenHelperFactory, then zeroed.
            val passphrase = crypto.databasePassphrase()
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "shellify.db",
            )
                .openHelperFactory(factory)
                .build()
                .also { passphrase.fill(0) }
        }
    }
}
