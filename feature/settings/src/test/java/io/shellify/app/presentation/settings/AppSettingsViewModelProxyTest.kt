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
import io.shellify.app.domain.model.ProxyType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.ExportNetworkLogsUseCase
import io.shellify.app.domain.usecase.GetNetworkLogUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies custom proxy methods in AppSettingsViewModel (Phase 18 Plan 03 Task 1).
 *
 * Covers PRX-13 (proxy enables, Tor cleared), PRX-14 (Tor enables, proxy cleared),
 * PRX-15 (NONE does not touch Tor), host trimming, port set, username/password ifBlank.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelProxyTest {

    private val dispatcher = UnconfinedTestDispatcher()
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
        id = 1L,
        name = "TestApp",
        url = "https://test.com",
        isolationId = "iso-abc",
        useTor = false,
        customProxyType = ProxyType.NONE,
    )

    private lateinit var viewModel: AppSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { simpleIconsManager.state } returns MutableStateFlow(SimpleIconsState.NotImported)
        coEvery { getWebAppById(1L) } returns testApp
        coEvery { saveWebApp(any()) } returns 1L
        coEvery { deleteWebApp(any()) } returns Unit
        every { passwordManager.passwordHash } returns MutableStateFlow(null)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockk<NotificationManager>(relaxed = true)
        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns mockk<AppOpsManager>(relaxed = true)
        every { getNetworkLog(any()) } returns MutableStateFlow(emptyList())
        viewModel = AppSettingsViewModel(
            appId = 1L,
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

    /** PRX-13: setCustomProxyType SOCKS5 when useTor=true disables Tor and emits ShowProxyEnabledTorDisabledToast. */
    @Test
    fun `setCustomProxyType SOCKS5 when useTor true disables tor and emits ShowProxyEnabledTorDisabledToast`() = runTest(dispatcher) {
        // Arrange: prime app with useTor=true
        coEvery { getWebAppById(1L) } returns testApp.copy(useTor = true)
        viewModel = AppSettingsViewModel(
            appId = 1L,
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

        val received = mutableListOf<AppSettingsCommand>()
        val job = launch(dispatcher) { viewModel.commands.take(1).toList(received) }

        viewModel.setCustomProxyType(ProxyType.SOCKS5)
        advanceUntilIdle()

        job.cancel()
        // Tor must be cleared
        assertFalse(viewModel.uiState.value.app?.useTor ?: true)
        // Proxy type must be set
        assertEquals(ProxyType.SOCKS5, viewModel.uiState.value.app?.customProxyType)
        // Toast command must be emitted
        assertEquals(1, received.size)
        assertEquals(AppSettingsCommand.ShowProxyEnabledTorDisabledToast, received[0])
        // Save called with updated state
        coVerify(atLeast = 1) { saveWebApp(match { it.customProxyType == ProxyType.SOCKS5 && !it.useTor }) }
    }

    /** PRX-14: toggleUseTor on when customProxyType=SOCKS5 clears proxy and emits ShowTorEnabledProxyDisabledToast. */
    @Test
    fun `toggleUseTor on when customProxyType SOCKS5 clears proxy and emits ShowTorEnabledProxyDisabledToast`() = runTest(dispatcher) {
        // Arrange: prime app with customProxyType=SOCKS5
        coEvery { getWebAppById(1L) } returns testApp.copy(customProxyType = ProxyType.SOCKS5, useTor = false)
        viewModel = AppSettingsViewModel(
            appId = 1L,
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

        val received = mutableListOf<AppSettingsCommand>()
        val job = launch(dispatcher) { viewModel.commands.take(1).toList(received) }

        viewModel.toggleUseTor()
        advanceUntilIdle()

        job.cancel()
        // Tor must be on
        assertTrue(viewModel.uiState.value.app?.useTor ?: false)
        // Proxy must be cleared
        assertEquals(ProxyType.NONE, viewModel.uiState.value.app?.customProxyType)
        // Toast command must be emitted
        assertEquals(1, received.size)
        assertEquals(AppSettingsCommand.ShowTorEnabledProxyDisabledToast, received[0])
        coVerify(atLeast = 1) { saveWebApp(match { it.useTor && it.customProxyType == ProxyType.NONE }) }
    }

    /** PRX-15: setCustomProxyType NONE does not affect useTor and emits no command. */
    @Test
    fun `setCustomProxyType NONE does not affect useTor and emits no command`() = runTest(dispatcher) {
        // Arrange: prime app with useTor=true
        coEvery { getWebAppById(1L) } returns testApp.copy(useTor = true, customProxyType = ProxyType.NONE)
        viewModel = AppSettingsViewModel(
            appId = 1L,
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

        viewModel.setCustomProxyType(ProxyType.NONE)
        advanceUntilIdle()

        // useTor must be unchanged
        assertTrue(viewModel.uiState.value.app?.useTor ?: false)
        // No command emitted (replayCache should not contain proxy toasts)
        val cached = viewModel.commands.replayCache
        assertTrue(cached.none { it is AppSettingsCommand.ShowProxyEnabledTorDisabledToast || it is AppSettingsCommand.ShowTorEnabledProxyDisabledToast })
    }

    /** setCustomProxyHost trims whitespace and converts blank to null. */
    @Test
    fun `setCustomProxyHost trims and converts blank to null`() = runTest {
        advanceUntilIdle()

        viewModel.setCustomProxyHost("  proxy.example.com  ")
        advanceUntilIdle()
        assertEquals("proxy.example.com", viewModel.uiState.value.app?.customProxyHost)

        viewModel.setCustomProxyHost("   ")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.app?.customProxyHost)
    }

    /** setCustomProxyPort writes the integer value. */
    @Test
    fun `setCustomProxyPort writes integer value`() = runTest {
        advanceUntilIdle()

        viewModel.setCustomProxyPort(8080)
        advanceUntilIdle()

        assertEquals(8080, viewModel.uiState.value.app?.customProxyPort)
        coVerify(atLeast = 1) { saveWebApp(match { it.customProxyPort == 8080 }) }
    }

    /** setCustomProxyUsername and setCustomProxyPassword convert blank to null. */
    @Test
    fun `setCustomProxyUsername and Password convert blank to null`() = runTest {
        advanceUntilIdle()

        viewModel.setCustomProxyUsername("alice")
        advanceUntilIdle()
        assertEquals("alice", viewModel.uiState.value.app?.customProxyUsername)

        viewModel.setCustomProxyUsername("")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.app?.customProxyUsername)

        viewModel.setCustomProxyPassword("secret")
        advanceUntilIdle()
        assertEquals("secret", viewModel.uiState.value.app?.customProxyPassword)

        viewModel.setCustomProxyPassword("")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.app?.customProxyPassword)
    }
}
