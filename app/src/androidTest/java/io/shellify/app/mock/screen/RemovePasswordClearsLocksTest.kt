package io.shellify.app.mock.screen

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.backup.BackupManager
import io.shellify.app.core.backup.BackupSchedule
import io.shellify.app.core.backup.BackupSettings
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.DeleteAllCategoriesUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import io.shellify.app.presentation.settings.GlobalSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemovePasswordClearsLocksTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var passwordManager: PasswordManager

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        passwordManager = PasswordManager(context)
        passwordManager.clearPassword()
        passwordManager.setPassword(CORRECT_PASSWORD)
    }

    @After
    fun tearDown() = runTest {
        passwordManager.clearPassword()
        Dispatchers.resetMain()
    }

    @Test
    fun removePassword_clearsPasswordLockedApps() = runTest {
        val saved = mutableListOf<WebApp>()
        val apps = listOf(app(1L, LockType.PASSWORD), app(2L, LockType.NONE))
        val vm = buildVm(apps, captureInto = saved)
        waitForInit(vm)

        vm.removePassword(CORRECT_PASSWORD, onWrongPassword = { error("should not be called") })
        waitForPasswordCleared(vm)

        assertEquals(1, saved.size)
        assertTrue(saved.all { it.lockType == LockType.NONE })
    }

    @Test
    fun removePassword_clearsSystemLockedApps() = runTest {
        val saved = mutableListOf<WebApp>()
        val apps = listOf(app(1L, LockType.SYSTEM), app(2L, LockType.NONE))
        val vm = buildVm(apps, captureInto = saved)
        waitForInit(vm)

        vm.removePassword(CORRECT_PASSWORD, onWrongPassword = { error("should not be called") })
        waitForPasswordCleared(vm)

        assertEquals(1, saved.size)
        assertEquals(LockType.NONE, saved.first().lockType)
    }

    @Test
    fun removePassword_clearsMixedLockTypes() = runTest {
        val saved = mutableListOf<WebApp>()
        val apps = listOf(
            app(1L, LockType.PASSWORD),
            app(2L, LockType.SYSTEM),
            app(3L, LockType.NONE),
        )
        val vm = buildVm(apps, captureInto = saved)
        waitForInit(vm)

        vm.removePassword(CORRECT_PASSWORD, onWrongPassword = { error("should not be called") })
        waitForPasswordCleared(vm)

        assertEquals(2, saved.size)
        assertTrue(saved.all { it.lockType == LockType.NONE })
    }

    @Test
    fun removePassword_doesNotSave_noneLockedApps() = runTest {
        val saved = mutableListOf<WebApp>()
        val apps = listOf(app(1L, LockType.NONE), app(2L, LockType.NONE))
        val vm = buildVm(apps, captureInto = saved)
        waitForInit(vm)

        vm.removePassword(CORRECT_PASSWORD, onWrongPassword = { error("should not be called") })
        waitForPasswordCleared(vm)

        assertTrue(saved.isEmpty())
    }

    @Test
    fun removePassword_wrongPassword_doesNotClearAnyLock() = runTest {
        val saved = mutableListOf<WebApp>()
        val apps = listOf(app(1L, LockType.PASSWORD), app(2L, LockType.SYSTEM))
        val vm = buildVm(apps, captureInto = saved)
        waitForInit(vm)

        val callbackSignal = MutableStateFlow(false)
        vm.removePassword("wrong-password") { callbackSignal.value = true }
        withContext(Dispatchers.Default) { withTimeout(3_000) { callbackSignal.first { it } } }

        assertTrue(callbackSignal.value)
        assertTrue(saved.isEmpty())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun waitForInit(vm: GlobalSettingsViewModel) =
        withContext(Dispatchers.Default) { withTimeout(3_000) { vm.uiState.first { it.hasPassword } } }

    private suspend fun waitForPasswordCleared(vm: GlobalSettingsViewModel) =
        withContext(Dispatchers.Default) { withTimeout(3_000) { vm.uiState.first { !it.hasPassword } } }

    private fun app(id: Long, lockType: LockType) =
        WebApp(id = id, name = "App$id", url = "https://app$id.test", lockType = lockType)

    private fun buildVm(apps: List<WebApp>, captureInto: MutableList<WebApp>): GlobalSettingsViewModel {
        val getWebApps = mockk<GetWebAppsUseCase>()
        every { getWebApps() } returns flowOf(apps)
        val saveWebApp = mockk<SaveWebAppUseCase>()
        coEvery { saveWebApp(any<WebApp>()) } answers {
            captureInto.add(firstArg())
            firstArg<WebApp>().id
        }
        val themeManager = mockk<ThemeManager>(relaxed = true) {
            every { themeMode } returns flowOf(ThemeMode.SYSTEM)
            every { dynamicColor } returns flowOf(true)
            every { accentColor } returns flowOf(null)
            every { defaultUaMode } returns flowOf(UserAgentMode.CHROME_MOBILE)
            every { defaultEngineType } returns flowOf(EngineType.SYSTEM_WEBVIEW)
            every { geckoSafeBrowsing } returns flowOf(false)
            every { globalNotificationsEnabled } returns MutableStateFlow(false)
        }
        val backupSettings = mockk<BackupSettings>(relaxed = true) {
            every { enabled } returns flowOf(false)
            every { hasPassword } returns flowOf(false)
            every { directoryUri } returns flowOf(null)
            every { schedule } returns flowOf(BackupSchedule.NONE)
            every { lastBackupTime } returns flowOf(0L)
        }
        return GlobalSettingsViewModel(
            themeManager = themeManager,
            isolationManager = mockk<IsolationManager>(relaxed = true),
            getWebApps = getWebApps,
            saveWebApp = saveWebApp,
            deleteAllAppsUseCase = mockk<DeleteAllAppsUseCase>(relaxed = true),
            deleteAllCategoriesUseCase = mockk<DeleteAllCategoriesUseCase>(relaxed = true),
            passwordManager = passwordManager,
            backupSettings = backupSettings,
            backupManager = mockk<BackupManager>(relaxed = true),
            context = context,
            geckoEngineManager = mockk<GeckoEngineManager>(relaxed = true) {
                every { installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
            },
            simpleIconsManager = mockk<SimpleIconsManager> {
                every { state } returns MutableStateFlow(SimpleIconsState.NotImported)
            },
        )
    }

    companion object {
        private const val CORRECT_PASSWORD = "correct-horse-battery"
    }
}
