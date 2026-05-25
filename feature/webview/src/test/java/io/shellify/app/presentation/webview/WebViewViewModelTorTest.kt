package io.shellify.app.presentation.webview

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.shellify.app.core.engine.TorManager
import io.shellify.app.core.engine.TorState
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Verifies Tor integration in WebViewViewModel per Plan 02-05 Task 1.
 *
 * Six behaviours:
 *  1. When useTor == false, onAppReady emits LoadUrl immediately (no torState observation)
 *  2. When useTor == true, onAppReady does NOT emit LoadUrl until torState reaches TorState.Ready
 *  3. When useTor == true and preserveTorIdentity == true, registerPreserveIdentityApp is called
 *  4. onSessionStop calls torManager.releaseApp exactly once
 *  5. onNewTorIdentity invokes torManager.newIdentity once and emits NewTorIdentityRequested
 *  6. uiState.torState forwards the latest TorState from torManager.torState
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebViewViewModelTorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val torStateFlow = MutableStateFlow<TorState>(TorState.Stopped)
    private val torManager = mockk<TorManager>(relaxed = true) {
        every { torState } returns torStateFlow
    }

    private val isolationManager = mockk<IsolationManager>(relaxed = true)
    private val saveWebApp = mockk<SaveWebAppUseCase>(relaxed = true)
    private val passwordManager = mockk<PasswordManager>(relaxed = true) {
        every { passwordHash } returns MutableStateFlow(null)
    }
    private val deleteAllApps = mockk<DeleteAllAppsUseCase>(relaxed = true)
    private val getWebApps = mockk<GetWebAppsUseCase>(relaxed = true)

    private val nonTorApp = WebApp(
        id = 1L,
        name = "Test App",
        url = "https://example.com",
        lockType = LockType.NONE,
        useTor = false,
        preserveTorIdentity = false,
    )

    private val torApp = WebApp(
        id = 2L,
        name = "Tor App",
        url = "https://example.onion",
        lockType = LockType.NONE,
        useTor = true,
        preserveTorIdentity = false,
    )

    private val torPreserveApp = WebApp(
        id = 3L,
        name = "Tor Preserve App",
        url = "https://example2.onion",
        lockType = LockType.NONE,
        useTor = true,
        preserveTorIdentity = true,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(app: WebApp): WebViewViewModel =
        WebViewViewModel(
            initialApp = app,
            isolationManager = isolationManager,
            saveWebApp = saveWebApp,
            passwordManager = passwordManager,
            deleteAllAppsUseCase = deleteAllApps,
            getWebApps = getWebApps,
            torManager = torManager,
        )

    /**
     * Test 1: When useTor == false, onAppReady emits LoadUrl immediately without waiting.
     */
    @Test
    fun `when useTor false, onAppReady emits LoadUrl immediately`() = testScope.runTest {
        val vm = createViewModel(nonTorApp)
        advanceUntilIdle()

        vm.onAppReady(nonTorApp)
        advanceUntilIdle()

        val cmd = vm.commands.first()
        assertEquals(WebViewCommand.LoadUrl(nonTorApp.url), cmd)
    }

    /**
     * Test 2: When useTor == true, onAppReady does NOT emit LoadUrl until torState == Ready.
     */
    @Test
    fun `when useTor true, onAppReady gates LoadUrl on TorState Ready`() = testScope.runTest {
        torStateFlow.value = TorState.Connecting
        val vm = createViewModel(torApp)
        advanceUntilIdle()

        vm.onAppReady(torApp)
        advanceUntilIdle()

        // Should NOT have emitted LoadUrl yet while Connecting
        val commands = mutableListOf<WebViewCommand>()
        val job = launch { vm.commands.collect { commands += it } }

        // No LoadUrl should be emitted while Connecting
        advanceUntilIdle()
        assertFalse("No LoadUrl while Connecting", commands.any { it is WebViewCommand.LoadUrl })

        // Emit Ready — now LoadUrl should be dispatched
        torStateFlow.value = TorState.Ready
        advanceUntilIdle()

        val loadUrlCommands = commands.filterIsInstance<WebViewCommand.LoadUrl>()
        assertEquals("Exactly one LoadUrl emitted on Ready", 1, loadUrlCommands.size)
        assertEquals(torApp.url, loadUrlCommands[0].url)

        job.cancel()
    }

    /**
     * Test 3: When useTor == true and preserveTorIdentity == true, registerPreserveIdentityApp is called.
     */
    @Test
    fun `when useTor and preserveTorIdentity true, registerPreserveIdentityApp is called once`() = testScope.runTest {
        torStateFlow.value = TorState.Ready
        val vm = createViewModel(torPreserveApp)
        advanceUntilIdle()

        vm.onAppReady(torPreserveApp)
        advanceUntilIdle()

        verify(exactly = 1) { torManager.registerPreserveIdentityApp(torPreserveApp.id) }
    }

    /**
     * Test 4: onSessionStop calls torManager.releaseApp exactly once.
     */
    @Test
    fun `onSessionStop calls torManager releaseApp exactly once`() = testScope.runTest {
        val vm = createViewModel(torApp)
        advanceUntilIdle()

        vm.onSessionStop(torApp)
        advanceUntilIdle()

        verify(exactly = 1) { torManager.releaseApp(torApp.id, torApp.preserveTorIdentity) }
    }

    /**
     * Test 5: onNewTorIdentity invokes torManager.newIdentity once and emits NewTorIdentityRequested.
     */
    @Test
    fun `onNewTorIdentity calls torManager newIdentity and emits NewTorIdentityRequested`() = testScope.runTest {
        val vm = createViewModel(torApp)
        advanceUntilIdle()

        vm.onNewTorIdentity()
        advanceUntilIdle()

        verify(exactly = 1) { torManager.newIdentity() }
        val cmd = vm.commands.first()
        assertEquals(WebViewCommand.NewTorIdentityRequested, cmd)
    }

    /**
     * Test 6: uiState.torState forwards the latest TorState from torManager.torState.
     */
    @Test
    fun `uiState torState reflects torManager torState flow`() = testScope.runTest {
        torStateFlow.value = TorState.Stopped
        val vm = createViewModel(torApp)
        advanceUntilIdle()

        assertEquals(TorState.Stopped, vm.uiState.value.torState)

        torStateFlow.value = TorState.Connecting
        advanceUntilIdle()
        assertEquals(TorState.Connecting, vm.uiState.value.torState)

        torStateFlow.value = TorState.Ready
        advanceUntilIdle()
        assertEquals(TorState.Ready, vm.uiState.value.torState)
    }
}
