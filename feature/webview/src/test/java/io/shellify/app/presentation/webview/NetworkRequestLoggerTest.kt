package io.shellify.app.presentation.webview

import io.mockk.coVerify
import io.mockk.mockk
import io.shellify.app.domain.usecase.ClearNetworkLogsUseCase
import io.shellify.app.domain.usecase.LogNetworkRequestUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkRequestLoggerTest {

    private val appId = 1L
    private val logNetworkRequest = mockk<LogNetworkRequestUseCase>(relaxed = true)
    private val clearNetworkLogs = mockk<ClearNetworkLogsUseCase>(relaxed = true)

    private fun buildLogger() = NetworkRequestLogger(
        appId = appId,
        logNetworkRequest = logNetworkRequest,
        clearNetworkLogs = clearNetworkLogs,
    )

    @Test
    fun `onRequestIntercepted adds entry with correct hostname and url for non-blocked request`() = runTest {
        val logger = buildLogger()
        logger.onRequestIntercepted("https://cdn.example.com/a.js", blocked = false)
        advanceUntilIdle()

        val log = logger.sessionLog.value
        assertEquals(1, log.size)
        assertEquals("cdn.example.com", log[0].hostname)
        assertEquals("https://cdn.example.com/a.js", log[0].url)
        assertFalse(log[0].isBlocked)
        assertEquals(appId, log[0].appId)
    }

    @Test
    fun `onRequestIntercepted adds entry with isBlocked true for blocked request`() = runTest {
        val logger = buildLogger()
        logger.onRequestIntercepted("https://tracker.com/t.png", blocked = true)
        advanceUntilIdle()

        val log = logger.sessionLog.value
        assertEquals(1, log.size)
        assertTrue(log[0].isBlocked)
        assertEquals("tracker.com", log[0].hostname)
    }

    @Test
    fun `onRequestIntercepted extracts hostname without port`() = runTest {
        val logger = buildLogger()
        logger.onRequestIntercepted("https://api.example.com:8080/path", blocked = false)
        advanceUntilIdle()

        val log = logger.sessionLog.value
        assertEquals("api.example.com", log[0].hostname)
    }

    @Test
    fun `sessionLog accumulates multiple entries`() = runTest {
        val logger = buildLogger()
        logger.onRequestIntercepted("https://cdn.example.com/a.js", blocked = false)
        logger.onRequestIntercepted("https://tracker.com/t.png", blocked = true)
        advanceUntilIdle()

        assertEquals(2, logger.sessionLog.value.size)
    }

    @Test
    fun `sessionId is a non-blank UUID string`() {
        val logger = buildLogger()
        assertTrue(logger.sessionId.isNotBlank())
        // Validate basic UUID format: 8-4-4-4-12 hex chars
        assertTrue(logger.sessionId.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `clearSession empties the in-memory log`() = runTest {
        val logger = buildLogger()
        logger.onRequestIntercepted("https://example.com/", blocked = false)
        advanceUntilIdle()

        logger.clearSession()

        assertEquals(0, logger.sessionLog.value.size)
    }

    @Test
    fun `clearSession also deletes DB rows via clearNetworkLogs`() = runTest {
        val logger = buildLogger()
        logger.onRequestIntercepted("https://example.com/", blocked = false)
        advanceUntilIdle()

        logger.clearSession()
        advanceUntilIdle()

        coVerify(exactly = 1) { clearNetworkLogs(appId) }
    }
}
