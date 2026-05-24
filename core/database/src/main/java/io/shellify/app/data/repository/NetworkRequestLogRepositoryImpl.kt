package io.shellify.app.data.repository

import io.shellify.app.data.local.dao.NetworkRequestLogDao
import io.shellify.app.data.mapper.toDomain
import io.shellify.app.data.mapper.toEntity
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.repository.NetworkRequestLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetworkRequestLogRepositoryImpl(private val dao: NetworkRequestLogDao) : NetworkRequestLogRepository {

    override fun getByApp(appId: Long): Flow<List<NetworkRequestLog>> =
        dao.getByApp(appId).map { list -> list.map { it.toDomain() } }

    override fun getBySession(appId: Long, sessionId: String): Flow<List<NetworkRequestLog>> =
        dao.getBySession(appId, sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun save(log: NetworkRequestLog): Long =
        dao.insert(log.toEntity())

    override suspend fun deleteOlderThan(cutoff: Long) = dao.deleteOlderThan(cutoff)

    override suspend fun deleteByApp(appId: Long) = dao.deleteByApp(appId)
}
