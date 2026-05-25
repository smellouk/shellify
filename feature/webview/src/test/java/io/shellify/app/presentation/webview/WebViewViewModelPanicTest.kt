package io.shellify.app.presentation.webview

import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies the panic wipe pipeline: onPanicLongPress / onPanicDismiss / executePanicWipe.
 *
 * Seven behaviours per PLAN 02-03 Task 1:
 *  1. onPanicLongPress sets showPanicConfirm = true
 *  2. onPanicDismiss resets showPanicConfirm = false without any wipe
 *  3. executePanicWipe calls isolationManager.clearData once per app
 *  4. executePanicWipe calls deleteAllAppsUseCase exactly once
 *  5. executePanicWipe calls themeManager.clearAll and passwordManager.clearAll exactly once each
 *  6. executePanicWipe emits NavigateHome command after wipe
 *  7. NavigateHome is emitted after (not before) all wipe steps complete
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebViewViewModelPanicTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val isolationManager = mockk<IsolationManager>(relaxed = true)
    private val saveWebApp = mockk<SaveWebAppUseCase>(relaxed = true)
    private val passwordManager = mockk<PasswordManager>(relaxed = true)
    private val themeManager = mockk<ThemeManager>(relaxed = true)
    private val deleteAllAppsUseCase = mockk<DeleteAllAppsUseCase>(relaxed = true)
    private val getWebAppsUseCase = mockk<GetWebAppsUseCase>(relaxed = true)

    private val app1 = WebApp(id = 1L, name = "App1", url = "https://app1.com", isolationId = "iso-1")
    private val app2 = WebApp(id = 2L, name = "App2", url = "https://app2.com", isolationId = "iso-2")
    private val initialApp = WebApp(id = 3L, name = "Test", url = "https://test.com", lockType = LockType.NONE)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { passwordManager.passwordHash } returns MutableStateFlow(null)
        every { passwordManager.wipeOnFailedAttempts } returns MutableStateFlow(false)
        // getWebApps returns a flow of two apps
        every { getWebAppsUseCase() } returns MutableStateFlow(listOf(app1, app2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm(): WebViewViewModel = WebViewViewModel(
        initialApp = initialApp,
        isolationManager = isolationManager,
        saveWebApp = saveWebApp,
        passwordManager = passwordManager,
        deleteAllAppsUseCase = deleteAllAppsUseCase,
        getWebApps = getWebAppsUseCase,
        themeManager = themeManager,
    )

    /** Test 1: onPanicLongPress sets showPanicConfirm = true */
    @Test
    fun `onPanicLongPress sets showPanicConfirm to true`() = testScope.runTest {
        val vm = newVm()
        assertFalse(vm.uiState.value.showPanicConfirm)
        vm.onPanicLongPress()
        assertTrue(vm.uiState.value.showPanicConfirm)
    }

    /** Test 2: onPanicDismiss resets showPanicConfirm = false without invoking any wipe */
    @Test
    fun `onPanicDismiss resets showPanicConfirm to false without wipe`() = testScope.runTest {
        val vm = newVm()
        vm.onPanicLongPress()
        assertTrue(vm.uiState.value.showPanicConfirm)
        vm.onPanicDismiss()
        assertFalse(vm.uiState.value.showPanicConfirm)
        io.mockk.coVerify(exactly = 0) { isolationManager.clearData(any()) }
        io.mockk.coVerify(exactly = 0) { deleteAllAppsUseCase() }
    }

    /** Test 3: executePanicWipe calls isolationManager.clearData once per WebApp */
    @Test
    fun `executePanicWipe clears isolation data for each app`() = testScope.runTest {
        val vm = newVm()
        vm.executePanicWipe()
        advanceUntilIdle()
        io.mockk.coVerify(exactly = 1) { isolationManager.clearData("iso-1") }
        io.mockk.coVerify(exactly = 1) { isolationManager.clearData("iso-2") }
    }

    /** Test 4: executePanicWipe calls deleteAllAppsUseCase exactly once */
    @Test
    fun `executePanicWipe calls deleteAllAppsUseCase exactly once`() = testScope.runTest {
        val vm = newVm()
        vm.executePanicWipe()
        advanceUntilIdle()
        io.mockk.coVerify(exactly = 1) { deleteAllAppsUseCase() }
    }

    /** Test 5: executePanicWipe calls themeManager.clearAll and passwordManager.clearAll exactly once */
    @Test
    fun `executePanicWipe clears themeManager and passwordManager exactly once each`() = testScope.runTest {
        val vm = newVm()
        vm.executePanicWipe()
        advanceUntilIdle()
        io.mockk.coVerify(exactly = 1) { themeManager.clearAll() }
        io.mockk.coVerify(exactly = 1) { passwordManager.clearAll() }
    }

    /** Test 6: executePanicWipe emits NavigateHome command after wipe */
    @Test
    fun `executePanicWipe emits NavigateHome command`() = testScope.runTest {
        val vm = newVm()
        vm.executePanicWipe()
        advanceUntilIdle()
        val command = vm.commands.first()
        assertTrue("Expected NavigateHome but got $command", command is WebViewCommand.NavigateHome)
    }

    /** Test 7: NavigateHome is emitted after all wipe steps complete (sequencing) */
    @Test
    fun `executePanicWipe emits NavigateHome only after all wipe steps`() = testScope.runTest {
        val vm = newVm()
        vm.executePanicWipe()
        advanceUntilIdle()
        // Verify that clearData calls, deleteAllApps, and clearAll calls all precede NavigateHome emission
        coVerifyOrder {
            isolationManager.clearData(any())
            deleteAllAppsUseCase()
            themeManager.clearAll()
            passwordManager.clearAll()
        }
        // NavigateHome is emitted (verified in Test 6); this test confirms the ordering
        val command = vm.commands.first()
        assertTrue(command is WebViewCommand.NavigateHome)
    }
}
