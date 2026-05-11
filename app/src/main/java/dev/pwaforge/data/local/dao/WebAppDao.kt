package dev.pwaforge.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.pwaforge.data.local.entity.WebAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebAppDao {
    @Query("SELECT * FROM web_apps ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<WebAppEntity>>

    @Query("SELECT * FROM web_apps WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getByCategory(categoryId: Long): Flow<List<WebAppEntity>>

    @Query("SELECT * FROM web_apps WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WebAppEntity?

    @Query("SELECT * FROM web_apps WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): WebAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WebAppEntity): Long

    @Update
    suspend fun update(entity: WebAppEntity)

    @Delete
    suspend fun delete(entity: WebAppEntity)

    @Query("DELETE FROM web_apps WHERE id = :id")
    suspend fun deleteById(id: Long)
}
