package io.shellify.app.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.repository.NetworkRequestLogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LogNetworkRequestUseCaseTest {

    private val repository = mockk<NetworkRequestLogRepository>()
    private val useCase = LogNetworkRequestUseCase(repository)

    @Test
    fun `invoke delegates to repository save`() = runTest {
        val log = NetworkRequestLog(
            appId = 1L,
            sessionId = "session1",
            hostname = "example.com",
            url = "https://example.com/page",
            isBlocked = false,
            timestamp = 1000L,
        )
        coEvery { repository.save(log) } returns 42L

        useCase(log)

        coVerify(exactly = 1) { repository.save(log) }
    }

    @Test
    fun `invoke returns id from repository`() = runTest {
        val log = NetworkRequestLog(
            appId = 2L,
            sessionId = "session2",
            hostname = "blocked.com",
            url = "https://blocked.com/ad",
            isBlocked = true,
            timestamp = 2000L,
        )
        coEvery { repository.save(log) } returns 99L

        val result = useCase(log)

        assertEquals(99L, result)
    }
}
