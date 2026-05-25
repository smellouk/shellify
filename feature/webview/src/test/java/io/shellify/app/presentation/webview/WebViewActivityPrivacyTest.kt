package io.shellify.app.presentation.webview

import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewActivityPrivacyTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val isolationManager = mockk<IsolationManager>(relaxed = true)
    private val saveWebApp = mockk<SaveWebAppUseCase>(relaxed = true)
    private val passwordManager = mockk<PasswordManager>(relaxed = true)

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

    private fun buildVm(): WebViewViewModel {
        val app = WebApp(
            id = 1L,
            name = "Test",
            url = "https://test.com",
            lockType = LockType.NONE,
            isolationId = "iso-test",
        )
        return WebViewViewModel(
            initialApp = app,
            isolationManager = isolationManager,
            saveWebApp = saveWebApp,
            passwordManager = passwordManager,
        )
    }

    // onSessionStop must not clear isolation data — cookie auto-wipe was removed.
    @Test
    fun `onSessionStop does not clear isolation data`() = runTest {
        val vm = buildVm()
        vm.onSessionStop()
        advanceUntilIdle()
        coVerify(exactly = 0) { isolationManager.clearData(any()) }
    }
}
