package io.shellify.app.presentation.webview

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.SaveWebAppUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val isolationManager = mockk<IsolationManager>(relaxed = true)
    private val saveWebApp = mockk<SaveWebAppUseCase>(relaxed = true)
    private val passwordManager = mockk<PasswordManager>(relaxed = true)

    private val app = WebApp(id = 1L, name = "Test", url = "https://test.com", lockType = LockType.NONE)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { passwordManager.passwordHash } returns MutableStateFlow(null)
        every { passwordManager.wipeOnFailedAttempts } returns MutableStateFlow(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm(initialApp: WebApp = app) = WebViewViewModel(
        initialApp = initialApp,
        isolationManager = isolationManager,
        saveWebApp = saveWebApp,
        passwordManager = passwordManager,
    )

    // ── isPageLoaded ──────────────────────────────────────────────────────────

    @Test
    fun `initial state has isPageLoaded false`() {
        val vm = newVm()
        assertFalse(vm.uiState.value.isPageLoaded)
    }

    @Test
    fun `onPageFinished sets isPageLoaded true`() {
        val vm = newVm()
        vm.onPageFinished("https://test.com")
        assertTrue(vm.uiState.value.isPageLoaded)
    }

    @Test
    fun `onError sets isPageLoaded true`() {
        val vm = newVm()
        vm.onError(-2, "net::ERR_INTERNET_DISCONNECTED")
        assertTrue(vm.uiState.value.isPageLoaded)
    }

    @Test
    fun `onSslError sets isPageLoaded true`() {
        val vm = newVm()
        vm.onSslError("SSL handshake failed")
        assertTrue(vm.uiState.value.isPageLoaded)
    }

    @Test
    fun `isPageLoaded stays true after subsequent onPageStarted`() {
        val vm = newVm()
        vm.onPageFinished("https://test.com")
        vm.onPageStarted("https://test.com/page2")
        assertTrue(vm.uiState.value.isPageLoaded)
    }

    // ── onPageFinished side-effects ───────────────────────────────────────────

    @Test
    fun `onPageFinished clears error when load did not fail`() {
        val vm = newVm()
        vm.onPageFinished("https://test.com")
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `onPageFinished clears isRetrying`() {
        val vm = newVm()
        vm.onRetry()
        vm.onPageFinished("https://test.com")
        assertFalse(vm.uiState.value.isRetrying)
    }

    @Test
    fun `onPageFinished preserves error set by onError`() {
        val vm = newVm()
        vm.onError(-2, "net::ERR_INTERNET_DISCONNECTED")
        vm.onPageFinished(null)
        assertNotNull(vm.uiState.value.error)
    }

    // ── onRetry ───────────────────────────────────────────────────────────────

    @Test
    fun `onRetry sets isRetrying true and emits Reload command`() = runTest {
        val vm = newVm()
        vm.onRetry()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isRetrying)
        assertEquals(WebViewCommand.Reload, vm.commands.replayCache.last())
    }

    // ── settings toggles ──────────────────────────────────────────────────────

    @Test
    fun `onAdBlockChanged updates app and emits LoadUrl command`() = runTest {
        val vm = newVm()
        advanceUntilIdle()
        vm.onAdBlockChanged(false)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.app?.adBlockEnabled ?: true)
        val cmd = vm.commands.replayCache.last()
        assertTrue(cmd is WebViewCommand.LoadUrl)
    }

    @Test
    fun `onTranslateChanged updates translateEnabled`() {
        val vm = newVm()
        vm.onTranslateChanged(true)
        assertTrue(vm.uiState.value.app?.translateEnabled ?: false)
    }

    @Test
    fun `onFullscreenChanged updates isFullscreen`() {
        val vm = newVm()
        vm.onFullscreenChanged(true)
        assertTrue(vm.uiState.value.app?.isFullscreen ?: false)
    }

    @Test
    fun `onLockChanged true sets lockType PASSWORD`() {
        val vm = newVm()
        vm.onLockChanged(true)
        assertEquals(LockType.PASSWORD, vm.uiState.value.app?.lockType)
    }

    @Test
    fun `onLockChanged false sets lockType NONE`() {
        val vm = newVm()
        vm.onLockChanged(false)
        assertEquals(LockType.NONE, vm.uiState.value.app?.lockType)
    }

    // ── auth ──────────────────────────────────────────────────────────────────

    @Test
    fun `onPasswordVerified sets authState Authenticated`() = runTest {
        val vm = newVm()
        advanceUntilIdle()
        vm.onPasswordVerified()
        assertEquals(AuthState.Authenticated, vm.uiState.value.authState)
    }

    @Test
    fun `recordFailedAttempt increments failedAttempts`() = runTest {
        val lockedApp = app.copy(lockType = LockType.PASSWORD)
        every { passwordManager.passwordHash } returns MutableStateFlow("hash")
        coEvery { passwordManager.getFailedAttempts(any()) } returns 0
        val vm = newVm(lockedApp)
        advanceUntilIdle()
        vm.recordFailedAttempt()
        val state = vm.uiState.value.authState
        assertTrue(state is AuthState.PasswordRequired)
        assertEquals(1, (state as AuthState.PasswordRequired).failedAttempts)
    }
}
