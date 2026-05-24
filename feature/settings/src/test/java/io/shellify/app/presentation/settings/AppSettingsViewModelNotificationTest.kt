package io.shellify.app.presentation.settings

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.ExportNetworkLogsUseCase
import io.shellify.app.domain.usecase.GetNetworkLogUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
class AppSettingsViewModelNotificationTest {

    private val dispatcher = StandardTestDispatcher()

    private val getWebAppById = mockk<GetWebAppByIdUseCase>()
    private val saveWebApp = mockk<SaveWebAppUseCase>()
    private val deleteWebApp = mockk<DeleteWebAppUseCase>()
    private val isolationManager = mockk<IsolationManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val analyzer = mockk<PwaAnalyzer>(relaxed = true)
    private val faviconFetcher = mockk<FaviconFetcher>(relaxed = true)
    private val simpleIconsManager = mockk<SimpleIconsManager>(relaxed = true)
    private val passwordManager = mockk<PasswordManager>(relaxed = true)
    private val geckoEngineManager = mockk<GeckoEngineManager>(relaxed = true)
    private val exportNetworkLog = mockk<ExportNetworkLogsUseCase>(relaxed = true)
    private val getNetworkLog = mockk<GetNetworkLogUseCase>(relaxed = true)

    private val testApp = WebApp(
        id = 42L,
        name = "TestApp",
        url = "https://test.com",
        isolationId = "iso-42",
        notificationPermission = NotificationPermission.NOT_ASKED,
        dndStartHour = -1,
        dndEndHour = -1,
        backgroundNotificationsEnabled = false,
    )

    private lateinit var viewModel: AppSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { simpleIconsManager.state } returns MutableStateFlow(SimpleIconsState.NotImported)
        coEvery { getWebAppById(42L) } returns testApp
        coEvery { saveWebApp(any()) } returns 42L
        coEvery { deleteWebApp(any()) } returns Unit
        every { passwordManager.passwordHash } returns MutableStateFlow(null)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockk<NotificationManager>(relaxed = true)
        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns mockk<AppOpsManager>(relaxed = true)
        every { getNetworkLog(any()) } returns MutableStateFlow(emptyList())
        viewModel = AppSettingsViewModel(
            appId = 42L,
            getWebAppById = getWebAppById,
            saveWebApp = saveWebApp,
            deleteWebApp = deleteWebApp,
            isolationManager = isolationManager,
            context = context,
            analyzer = analyzer,
            faviconFetcher = faviconFetcher,
            simpleIconsManager = simpleIconsManager,
            passwordManager = passwordManager,
            geckoEngineManager = geckoEngineManager,
            exportNetworkLog = exportNetworkLog,
            getNetworkLog = getNetworkLog,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleNotificationPermission fromNotAsked isNoOp`() = runTest {
        advanceUntilIdle()
        assertEquals(NotificationPermission.NOT_ASKED, viewModel.uiState.value.app?.notificationPermission)

        viewModel.toggleNotificationPermission()
        advanceUntilIdle()

        assertEquals(NotificationPermission.NOT_ASKED, viewModel.uiState.value.app?.notificationPermission)
        coVerify(exactly = 0) { saveWebApp(any()) }
    }

    @Test
    fun `toggleNotificationPermission fromGranted setsNotAsked`() = runTest {
        val grantedApp = testApp.copy(notificationPermission = NotificationPermission.GRANTED)
        coEvery { getWebAppById(42L) } returns grantedApp
        viewModel = AppSettingsViewModel(
            appId = 42L,
            getWebAppById = getWebAppById,
            saveWebApp = saveWebApp,
            deleteWebApp = deleteWebApp,
            isolationManager = isolationManager,
            context = context,
            analyzer = analyzer,
            faviconFetcher = faviconFetcher,
            simpleIconsManager = simpleIconsManager,
            passwordManager = passwordManager,
            geckoEngineManager = geckoEngineManager,
            exportNetworkLog = exportNetworkLog,
            getNetworkLog = getNetworkLog,
        )
        advanceUntilIdle()

        viewModel.toggleNotificationPermission()
        advanceUntilIdle()

        assertEquals(NotificationPermission.NOT_ASKED, viewModel.uiState.value.app?.notificationPermission)
        coVerify(exactly = 1) { saveWebApp(match { it.notificationPermission == NotificationPermission.NOT_ASKED }) }
    }

    @Test
    fun `toggleNotificationPermission fromDenied setsNotAsked`() = runTest {
        val deniedApp = testApp.copy(notificationPermission = NotificationPermission.DENIED)
        coEvery { getWebAppById(42L) } returns deniedApp
        viewModel = AppSettingsViewModel(
            appId = 42L,
            getWebAppById = getWebAppById,
            saveWebApp = saveWebApp,
            deleteWebApp = deleteWebApp,
            isolationManager = isolationManager,
            context = context,
            analyzer = analyzer,
            faviconFetcher = faviconFetcher,
            simpleIconsManager = simpleIconsManager,
            passwordManager = passwordManager,
            geckoEngineManager = geckoEngineManager,
            exportNetworkLog = exportNetworkLog,
            getNetworkLog = getNetworkLog,
        )
        advanceUntilIdle()

        viewModel.toggleNotificationPermission()
        advanceUntilIdle()

        assertEquals(NotificationPermission.NOT_ASKED, viewModel.uiState.value.app?.notificationPermission)
        coVerify(exactly = 1) { saveWebApp(match { it.notificationPermission == NotificationPermission.NOT_ASKED }) }
    }

    @Test
    fun `setDndStartHour persistsHour`() = runTest {
        advanceUntilIdle()

        viewModel.setDndStartHour(7)
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.app?.dndStartHour)
        coVerify(exactly = 1) { saveWebApp(match { it.dndStartHour == 7 }) }
    }

    @Test
    fun `setDndEndHour minusOne clearsValue`() = runTest {
        advanceUntilIdle()

        viewModel.setDndEndHour(-1)
        advanceUntilIdle()

        assertEquals(-1, viewModel.uiState.value.app?.dndEndHour)
    }

    @Test
    fun `toggleBackgroundNotifications fromFalse emitsStartCommand`() = runTest {
        advanceUntilIdle()
        val commands = mutableListOf<AppSettingsCommand>()
        val job = launch { viewModel.commands.toList(commands) }

        viewModel.toggleBackgroundNotifications()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.app?.backgroundNotificationsEnabled ?: false)
        assertTrue(commands.any { it is AppSettingsCommand.StartBackgroundService && it.appId == 42L })

        job.cancel()
    }

    @Test
    fun `toggleBackgroundNotifications fromTrue emitsStopCommand`() = runTest {
        val enabledApp = testApp.copy(backgroundNotificationsEnabled = true)
        coEvery { getWebAppById(42L) } returns enabledApp
        viewModel = AppSettingsViewModel(
            appId = 42L,
            getWebAppById = getWebAppById,
            saveWebApp = saveWebApp,
            deleteWebApp = deleteWebApp,
            isolationManager = isolationManager,
            context = context,
            analyzer = analyzer,
            faviconFetcher = faviconFetcher,
            simpleIconsManager = simpleIconsManager,
            passwordManager = passwordManager,
            geckoEngineManager = geckoEngineManager,
            exportNetworkLog = exportNetworkLog,
            getNetworkLog = getNetworkLog,
        )
        advanceUntilIdle()
        val commands = mutableListOf<AppSettingsCommand>()
        val job = launch { viewModel.commands.toList(commands) }

        viewModel.toggleBackgroundNotifications()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.app?.backgroundNotificationsEnabled ?: true)
        assertTrue(commands.any { it is AppSettingsCommand.StopBackgroundService && it.appId == 42L })

        job.cancel()
    }

    @Test
    fun `onNotificationHistoryClick emitsNavigateCommand`() = runTest {
        advanceUntilIdle()
        val commands = mutableListOf<AppSettingsCommand>()
        val job = launch { viewModel.commands.toList(commands) }

        viewModel.onNotificationHistoryClick()
        advanceUntilIdle()

        assertTrue(commands.any { it is AppSettingsCommand.NavigateToNotificationHistory && it.appId == 42L })

        job.cancel()
    }

    @Test
    fun `clearDndSchedule resets both hours to minus one and saves`() = runTest {
        val appWithDnd = testApp.copy(dndStartHour = 22, dndEndHour = 8)
        coEvery { getWebAppById(42L) } returns appWithDnd
        viewModel = AppSettingsViewModel(
            appId = 42L,
            getWebAppById = getWebAppById,
            saveWebApp = saveWebApp,
            deleteWebApp = deleteWebApp,
            isolationManager = isolationManager,
            context = context,
            analyzer = analyzer,
            faviconFetcher = faviconFetcher,
            simpleIconsManager = simpleIconsManager,
            passwordManager = passwordManager,
            geckoEngineManager = geckoEngineManager,
            exportNetworkLog = exportNetworkLog,
            getNetworkLog = getNetworkLog,
        )
        advanceUntilIdle()
        assertEquals(22, viewModel.uiState.value.app?.dndStartHour)

        viewModel.clearDndSchedule()
        advanceUntilIdle()

        assertEquals(-1, viewModel.uiState.value.app?.dndStartHour)
        assertEquals(-1, viewModel.uiState.value.app?.dndEndHour)
        coVerify(exactly = 1) { saveWebApp(match { it.dndStartHour == -1 && it.dndEndHour == -1 }) }
    }

    // ── Export network logs ────────────────────────────────────────────────────

    @Test
    fun `onExportNetworkLogsClick invokes exportNetworkLog with the app id`() = runTest {
        coEvery { exportNetworkLog(42L) } returns "log"
        advanceUntilIdle()

        viewModel.onExportNetworkLogsClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { exportNetworkLog(42L) }
    }

    @Test
    fun `onExportNetworkLogsClick emits ShareNetworkLog command with returned content`() = runTest {
        coEvery { exportNetworkLog(42L) } returns "formatted log content"
        advanceUntilIdle()
        val commands = mutableListOf<AppSettingsCommand>()
        val job = launch { viewModel.commands.toList(commands) }

        viewModel.onExportNetworkLogsClick()
        advanceUntilIdle()

        assertTrue(
            commands.any {
                it is AppSettingsCommand.ShareNetworkLog && it.content == "formatted log content"
            }
        )
        job.cancel()
    }
}
