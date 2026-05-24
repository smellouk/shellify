package io.shellify.app.domain.usecase

import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.repository.NetworkRequestLogRepository
import kotlinx.coroutines.flow.Flow

class GetNetworkLogUseCase(private val repo: NetworkRequestLogRepository) {
    operator fun invoke(appId: Long): Flow<List<NetworkRequestLog>> = repo.getByApp(appId)
    fun getBySession(appId: Long, sessionId: String): Flow<List<NetworkRequestLog>> = repo.getBySession(appId, sessionId)
}
