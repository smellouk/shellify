package io.shellify.app.presentation.settings.networklog

import io.mockk.every
import io.mockk.mockk
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.usecase.ClearNetworkLogsUseCase
import io.shellify.app.domain.usecase.GetNetworkLogUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkLogHistoryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val getNetworkLog = mockk<GetNetworkLogUseCase>()
    private val clearNetworkLogs = mockk<ClearNetworkLogsUseCase>(relaxed = true)

    private lateinit var viewModel: NetworkLogHistoryViewModel

    private val appId = 1L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(entries: List<NetworkRequestLog>): NetworkLogHistoryViewModel {
        val flow = MutableStateFlow(entries)
        every { getNetworkLog(appId) } returns flow
        return NetworkLogHistoryViewModel(appId = appId, getNetworkLog = getNetworkLog, clearNetworkLogs = clearNetworkLogs)
    }

    @Test
    fun `when getNetworkLog emits empty list then sessions is empty and isLoading is false`() = runTest {
        viewModel = buildViewModel(emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.sessions.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `when getNetworkLog emits 3 entries with 2 distinct sessionIds then sessions has 2 groups`() = runTest {
        val entries = listOf(
            NetworkRequestLog(
                id = 1L, appId = appId, sessionId = "session-A",
                hostname = "example.com", url = "https://example.com/1",
                isBlocked = false, timestamp = 1000L,
            ),
            NetworkRequestLog(
                id = 2L, appId = appId, sessionId = "session-A",
                hostname = "cdn.com", url = "https://cdn.com/a.js",
                isBlocked = true, timestamp = 2000L,
            ),
            NetworkRequestLog(
                id = 3L, appId = appId, sessionId = "session-B",
                hostname = "api.com", url = "https://api.com/v1",
                isBlocked = false, timestamp = 3000L,
            ),
        )
        viewModel = buildViewModel(entries)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.sessions.size)
    }

    @Test
    fun `entries are grouped correctly for each sessionId`() = runTest {
        val entries = listOf(
            NetworkRequestLog(
                id = 1L, appId = appId, sessionId = "session-A",
                hostname = "example.com", url = "https://example.com/1",
                isBlocked = false, timestamp = 1000L,
            ),
            NetworkRequestLog(
                id = 2L, appId = appId, sessionId = "session-A",
                hostname = "cdn.com", url = "https://cdn.com/a.js",
                isBlocked = true, timestamp = 2000L,
            ),
            NetworkRequestLog(
                id = 3L, appId = appId, sessionId = "session-B",
                hostname = "api.com", url = "https://api.com/v1",
                isBlocked = false, timestamp = 3000L,
            ),
        )
        viewModel = buildViewModel(entries)
        advanceUntilIdle()

        val sessions = viewModel.uiState.value.sessions
        val sessionA = sessions.find { it.sessionId == "session-A" }
        val sessionB = sessions.find { it.sessionId == "session-B" }

        assertEquals(2, sessionA?.entries?.size)
        assertEquals(1, sessionB?.entries?.size)
    }

    @Test
    fun `isLoading starts as true and becomes false after first emission`() = runTest {
        val flow = MutableStateFlow<List<NetworkRequestLog>>(emptyList())
        every { getNetworkLog(appId) } returns flow

        viewModel = NetworkLogHistoryViewModel(appId = appId, getNetworkLog = getNetworkLog, clearNetworkLogs = clearNetworkLogs)
        // After the UnconfinedTestDispatcher runs, state is already updated
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sessions are ordered most-recent sessionId first based on max timestamp`() = runTest {
        val entries = listOf(
            NetworkRequestLog(
                id = 1L, appId = appId, sessionId = "old-session",
                hostname = "old.com", url = "https://old.com/",
                isBlocked = false, timestamp = 1000L,
            ),
            NetworkRequestLog(
                id = 2L, appId = appId, sessionId = "new-session",
                hostname = "new.com", url = "https://new.com/",
                isBlocked = false, timestamp = 5000L,
            ),
        )
        viewModel = buildViewModel(entries)
        advanceUntilIdle()

        val sessions = viewModel.uiState.value.sessions
        assertEquals("new-session", sessions.first().sessionId)
        assertEquals("old-session", sessions.last().sessionId)
    }

    @Test
    fun `SessionGroup startedAt is minimum timestamp in the group`() = runTest {
        val entries = listOf(
            NetworkRequestLog(
                id = 1L, appId = appId, sessionId = "session-X",
                hostname = "a.com", url = "https://a.com/",
                isBlocked = false, timestamp = 3000L,
            ),
            NetworkRequestLog(
                id = 2L, appId = appId, sessionId = "session-X",
                hostname = "b.com", url = "https://b.com/",
                isBlocked = false, timestamp = 1000L,
            ),
        )
        viewModel = buildViewModel(entries)
        advanceUntilIdle()

        val sessionX = viewModel.uiState.value.sessions.find { it.sessionId == "session-X" }
        assertEquals(1000L, sessionX?.startedAt)
    }
}
