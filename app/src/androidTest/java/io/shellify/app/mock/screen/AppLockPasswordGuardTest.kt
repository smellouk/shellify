package io.shellify.app.mock.screen

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.ExportNetworkLogsUseCase
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.GetNetworkLogUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.GetWebAppByNameUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import io.shellify.app.presentation.add.AddViewModel
import io.shellify.app.presentation.settings.AppSettingsViewModel
import io.shellify.app.core.isolation.IsolationManager
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockPasswordGuardTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var passwordManager: PasswordManager

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        passwordManager = PasswordManager(context)
        passwordManager.clearPassword()
    }

    @After
    fun tearDown() = runTest {
        passwordManager.clearPassword()
        Dispatchers.resetMain()
    }

    // ── AppSettingsViewModel ──────────────────────────────────────────────────

    @Test
    fun appSettings_hasPassword_isFalse_whenNoGlobalPassword() = runTest {
        val vm = buildAppSettingsVm(WebApp(id = 1L, name = "A", url = "https://a.test"))
        assertFalse(vm.uiState.value.hasPassword)
    }

    @Test
    fun appSettings_hasPassword_isTrue_whenGlobalPasswordSet() = runTest {
        passwordManager.setPassword("hunter2")
        val vm = buildAppSettingsVm(WebApp(id = 1L, name = "A", url = "https://a.test"))
        withContext(Dispatchers.Default) { withTimeout(2_000) { vm.uiState.first { it.hasPassword } } }
        assertTrue(vm.uiState.value.hasPassword)
    }

    @Test
    fun appSettings_passwordLock_resetsToNone_whenGlobalPasswordRemoved() = runTest {
        passwordManager.setPassword("hunter2")
        val vm = buildAppSettingsVm(
            WebApp(id = 1L, name = "A", url = "https://a.test", lockType = LockType.PASSWORD)
        )
        withContext(Dispatchers.Default) { withTimeout(2_000) { vm.uiState.first { it.hasPassword } } }

        passwordManager.clearPassword()

        withContext(Dispatchers.Default) { withTimeout(2_000) { vm.uiState.first { !it.hasPassword } } }
        assertEquals(LockType.NONE, vm.uiState.value.app?.lockType)
    }

    @Test
    fun appSettings_systemLock_resetsToNone_whenGlobalPasswordRemoved() = runTest {
        passwordManager.setPassword("hunter2")
        val vm = buildAppSettingsVm(
            WebApp(id = 1L, name = "A", url = "https://a.test", lockType = LockType.SYSTEM)
        )
        withContext(Dispatchers.Default) { withTimeout(2_000) { vm.uiState.first { it.hasPassword } } }

        passwordManager.clearPassword()

        withContext(Dispatchers.Default) { withTimeout(2_000) { vm.uiState.first { !it.hasPassword } } }
        assertEquals(LockType.NONE, vm.uiState.value.app?.lockType)
    }

    // ── AddViewModel ──────────────────────────────────────────────────────────

    @Test
    fun add_hasPassword_isFalse_whenNoGlobalPassword() = runTest {
        val vm = buildAddVm()
        assertFalse(vm.uiState.value.hasPassword)
    }

    @Test
    fun add_hasPassword_isTrue_whenGlobalPasswordSet() = runTest {
        passwordManager.setPassword("hunter2")
        val vm = buildAddVm()
        withContext(Dispatchers.Default) { withTimeout(2_000) { vm.uiState.first { it.hasPassword } } }
        assertTrue(vm.uiState.value.hasPassword)
    }

    @Test
    fun add_lockType_resetsToNone_whenGlobalPasswordRemoved() = runTest {
        passwordManager.setPassword("hunter2")
        val vm = buildAddVm()
        withContext(Dispatchers.Default) { withTimeout(2_000) { vm.uiState.first { it.hasPassword } } }
        vm.setLockType(LockType.SYSTEM)
        assertEquals(LockType.SYSTEM, vm.uiState.value.lockType)

        passwordManager.clearPassword()

        withContext(Dispatchers.Default) { withTimeout(2_000) { vm.uiState.first { !it.hasPassword } } }
        assertEquals(LockType.NONE, vm.uiState.value.lockType)
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun buildAppSettingsVm(app: WebApp): AppSettingsViewModel {
        val getWebAppById = mockk<GetWebAppByIdUseCase>()
        coEvery { getWebAppById(app.id) } returns app
        val saveWebApp = mockk<SaveWebAppUseCase>()
        coEvery { saveWebApp(any<WebApp>()) } returns app.id
        return AppSettingsViewModel(
            appId = app.id,
            getWebAppById = getWebAppById,
            saveWebApp = saveWebApp,
            deleteWebApp = mockk(relaxed = true),
            isolationManager = mockk<IsolationManager>(relaxed = true),
            context = context,
            analyzer = mockk<PwaAnalyzer>(relaxed = true),
            faviconFetcher = mockk<FaviconFetcher>(relaxed = true),
            simpleIconsManager = mockk<SimpleIconsManager> {
                every { state } returns MutableStateFlow(SimpleIconsState.NotImported)
            },
            passwordManager = passwordManager,
            geckoEngineManager = mockk<GeckoEngineManager> {
                every { installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
            },
            exportNetworkLog = mockk<ExportNetworkLogsUseCase>(relaxed = true),
            getNetworkLog = mockk<GetNetworkLogUseCase>(relaxed = true),
        )
    }

    private fun buildAddVm(): AddViewModel {
        val saveWebApp = mockk<SaveWebAppUseCase>()
        coEvery { saveWebApp(any<WebApp>()) } returns 0L
        val getCategories = mockk<GetCategoriesUseCase>()
        every { getCategories() } returns flowOf(emptyList())
        return AddViewModel(
            appId = 0L,
            getWebAppById = mockk(relaxed = true),
            getWebAppByName = mockk<GetWebAppByNameUseCase>(relaxed = true),
            saveWebApp = saveWebApp,
            getCategories = getCategories,
            analyzer = mockk<PwaAnalyzer>(relaxed = true),
            faviconFetcher = mockk<FaviconFetcher>(relaxed = true),
            geckoEngineManager = mockk<GeckoEngineManager> {
                every { installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
            },
            themeManager = mockk<ThemeManager> {
                every { defaultEngineType } returns flowOf(EngineType.SYSTEM_WEBVIEW)
                every { globalNotificationsEnabled } returns MutableStateFlow(false)
            },
            simpleIconsManager = mockk<SimpleIconsManager> {
                every { state } returns MutableStateFlow(SimpleIconsState.NotImported)
            },
            context = context,
            passwordManager = passwordManager,
        )
    }
}
