package io.shellify.app.domain.usecase

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.repository.NetworkRequestLogRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNetworkLogUseCaseTest {

    private val repository = mockk<NetworkRequestLogRepository>()
    private val useCase = GetNetworkLogUseCase(repository)

    @Test
    fun `invoke delegates to repository getByApp`() {
        every { repository.getByApp(42L) } returns flowOf(emptyList())

        useCase(42L)

        verify(exactly = 1) { repository.getByApp(42L) }
    }

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val logs = listOf(
            NetworkRequestLog(
                appId = 42L,
                sessionId = "s1",
                hostname = "example.com",
                url = "https://example.com",
                isBlocked = false,
                timestamp = 1000L,
            ),
        )
        every { repository.getByApp(42L) } returns flowOf(logs)

        val emitted = useCase(42L).toList()

        assertEquals(1, emitted.size)
        assertEquals(logs, emitted[0])
    }

    @Test
    fun `getBySession delegates to repository getBySession`() {
        every { repository.getBySession(42L, "s1") } returns flowOf(emptyList())

        useCase.getBySession(42L, "s1")

        verify(exactly = 1) { repository.getBySession(42L, "s1") }
    }
}
