package io.shellify.app.domain.usecase

import io.shellify.app.domain.repository.NetworkRequestLogRepository

class DeleteOldNetworkLogsUseCase(private val repo: NetworkRequestLogRepository) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()) {
        repo.deleteOlderThan(now - 30L * 24 * 60 * 60 * 1000)
    }
}
