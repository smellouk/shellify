package io.shellify.app.domain.usecase

import io.mockk.coVerify
import io.mockk.mockk
import io.shellify.app.domain.repository.NetworkRequestLogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClearNetworkLogsUseCaseTest {

    private val repository = mockk<NetworkRequestLogRepository>(relaxed = true)
    private val useCase = ClearNetworkLogsUseCase(repository)

    @Test
    fun `invoke delegates to repository deleteByApp with the given appId`() = runTest {
        useCase(99L)
        coVerify(exactly = 1) { repository.deleteByApp(99L) }
    }
}
