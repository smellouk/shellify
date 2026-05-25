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
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.ExportNetworkLogsUseCase
import io.shellify.app.domain.usecase.GetNetworkLogUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelPrivacyTest {

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
        alwaysIncognito = false,
        trackerBlockingEnabled = false,
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

    // Behavior 1: toggleAlwaysIncognito flips alwaysIncognito and persists
    @Test
    fun `toggleAlwaysIncognito flips alwaysIncognito and saves`() = runTest {
        assertFalse(viewModel.uiState.value.app?.alwaysIncognito ?: true)
        viewModel.toggleAlwaysIncognito()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.app?.alwaysIncognito ?: false)
        coVerify(exactly = 1) { saveWebApp(match { it.alwaysIncognito }) }
    }

    // Behavior 2: toggleTrackerBlocking flips trackerBlockingEnabled and persists
    @Test
    fun `toggleTrackerBlocking flips trackerBlockingEnabled and saves`() = runTest {
        assertFalse(viewModel.uiState.value.app?.trackerBlockingEnabled ?: true)
        viewModel.toggleTrackerBlocking()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.app?.trackerBlockingEnabled ?: false)
        coVerify(exactly = 1) { saveWebApp(match { it.trackerBlockingEnabled }) }
    }
}
