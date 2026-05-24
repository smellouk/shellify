package io.shellify.app.domain.usecase

import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.repository.NetworkRequestLogRepository

class LogNetworkRequestUseCase(private val repo: NetworkRequestLogRepository) {
    suspend operator fun invoke(log: NetworkRequestLog): Long = repo.save(log)
}
