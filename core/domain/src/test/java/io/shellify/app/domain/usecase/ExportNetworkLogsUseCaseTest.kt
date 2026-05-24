package io.shellify.app.domain.usecase

import io.mockk.every
import io.mockk.mockk
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.repository.NetworkRequestLogRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportNetworkLogsUseCaseTest {

    private val repository = mockk<NetworkRequestLogRepository>()
    private val useCase = ExportNetworkLogsUseCase(repository)

    @Test
    fun `invoke returns header with Shellify label and exported date`() = runTest {
        every { repository.getByApp(1L) } returns flowOf(emptyList())

        val result = useCase(1L)

        assertTrue(result.contains("Shellify Network Log"))
        assertTrue(result.contains("Exported:"))
    }

    @Test
    fun `invoke with empty logs returns no-records notice`() = runTest {
        every { repository.getByApp(1L) } returns flowOf(emptyList())

        val result = useCase(1L)

        assertTrue(result.contains("(no records)"))
    }

    @Test
    fun `invoke with logs does not contain no-records notice`() = runTest {
        val logs = listOf(sampleLog(url = "https://example.com", isBlocked = false))
        every { repository.getByApp(1L) } returns flowOf(logs)

        val result = useCase(1L)

        assertFalse(result.contains("(no records)"))
    }

    @Test
    fun `invoke formats ALLOWED entry`() = runTest {
        val logs = listOf(sampleLog(url = "https://example.com", isBlocked = false))
        every { repository.getByApp(1L) } returns flowOf(logs)

        val result = useCase(1L)

        assertTrue(result.contains("ALLOWED"))
        assertTrue(result.contains("https://example.com"))
    }

    @Test
    fun `invoke formats BLOCKED entry`() = runTest {
        val logs = listOf(sampleLog(url = "https://ads.tracker.com/t.js", isBlocked = true))
        every { repository.getByApp(1L) } returns flowOf(logs)

        val result = useCase(1L)

        assertTrue(result.contains("BLOCKED"))
        assertTrue(result.contains("https://ads.tracker.com/t.js"))
    }

    @Test
    fun `invoke groups entries by session with divider`() = runTest {
        val logs = listOf(
            sampleLog(sessionId = "s1", url = "https://a.com", timestamp = 1_000_000L, isBlocked = false),
            sampleLog(sessionId = "s2", url = "https://b.com", timestamp = 2_000_000L, isBlocked = false),
        )
        every { repository.getByApp(1L) } returns flowOf(logs)

        val result = useCase(1L)

        val dividerCount = result.split("---").size - 1
        // Two session dividers: one open and one close per session header line
        assertTrue("Expected at least 2 session dividers, got $dividerCount", dividerCount >= 2)
    }

    @Test
    fun `invoke includes all URLs from multiple sessions`() = runTest {
        val logs = listOf(
            sampleLog(sessionId = "s1", url = "https://a.com", timestamp = 1_000_000L, isBlocked = false),
            sampleLog(sessionId = "s2", url = "https://b.com", timestamp = 2_000_000L, isBlocked = true),
        )
        every { repository.getByApp(1L) } returns flowOf(logs)

        val result = useCase(1L)

        assertTrue(result.contains("https://a.com"))
        assertTrue(result.contains("https://b.com"))
    }

    private fun sampleLog(
        sessionId: String = "s1",
        url: String = "https://example.com",
        isBlocked: Boolean = false,
        timestamp: Long = 1_000_000L,
    ) = NetworkRequestLog(
        appId = 1L,
        sessionId = sessionId,
        hostname = "example.com",
        url = url,
        isBlocked = isBlocked,
        timestamp = timestamp,
    )
}
