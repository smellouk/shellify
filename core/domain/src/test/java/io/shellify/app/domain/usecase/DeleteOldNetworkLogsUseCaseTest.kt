package io.shellify.app.domain.usecase

import io.mockk.coVerify
import io.mockk.mockk
import io.shellify.app.domain.repository.NetworkRequestLogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteOldNetworkLogsUseCaseTest {

    private val repository = mockk<NetworkRequestLogRepository>(relaxed = true)
    private val useCase = DeleteOldNetworkLogsUseCase(repository)

    @Test
    fun `invoke calls deleteOlderThan with cutoff 30 days before now`() = runTest {
        val now = 1_000_000_000_000L
        val expectedCutoff = now - 30L * 24 * 60 * 60 * 1000

        useCase(now = now)

        coVerify(exactly = 1) { repository.deleteOlderThan(expectedCutoff) }
    }

    @Test
    fun `invoke with now 0 calls deleteOlderThan with negative cutoff`() = runTest {
        val now = 0L
        val expectedCutoff = -30L * 24 * 60 * 60 * 1000

        useCase(now = now)

        coVerify(exactly = 1) { repository.deleteOlderThan(expectedCutoff) }
    }
}
