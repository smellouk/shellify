package io.shellify.app.domain.repository

import io.shellify.app.domain.model.NetworkRequestLog
import kotlinx.coroutines.flow.Flow

interface NetworkRequestLogRepository {
    fun getByApp(appId: Long): Flow<List<NetworkRequestLog>>
    fun getBySession(appId: Long, sessionId: String): Flow<List<NetworkRequestLog>>
    suspend fun save(log: NetworkRequestLog): Long
    suspend fun deleteOlderThan(cutoff: Long)
    suspend fun deleteByApp(appId: Long)
}
