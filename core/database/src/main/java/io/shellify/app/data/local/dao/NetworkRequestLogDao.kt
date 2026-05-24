package io.shellify.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.shellify.app.data.local.entity.NetworkRequestLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkRequestLogDao {
    @Query("SELECT * FROM network_request_logs WHERE app_id = :appId ORDER BY timestamp DESC")
    fun getByApp(appId: Long): Flow<List<NetworkRequestLogEntity>>

    @Query("SELECT * FROM network_request_logs WHERE app_id = :appId AND session_id = :sessionId ORDER BY timestamp ASC")
    fun getBySession(appId: Long, sessionId: String): Flow<List<NetworkRequestLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NetworkRequestLogEntity): Long

    @Query("DELETE FROM network_request_logs WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM network_request_logs WHERE app_id = :appId")
    suspend fun deleteByApp(appId: Long)
}
