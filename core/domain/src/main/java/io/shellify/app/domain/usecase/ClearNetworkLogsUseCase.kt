package io.shellify.app.domain.usecase

import io.shellify.app.domain.repository.NetworkRequestLogRepository

class ClearNetworkLogsUseCase(private val repo: NetworkRequestLogRepository) {
    suspend operator fun invoke(appId: Long) = repo.deleteByApp(appId)
}
