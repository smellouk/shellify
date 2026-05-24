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
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.TranslateLanguage
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelTest {

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

    private val testApp = WebApp(id = 1L, name = "TestApp", url = "https://test.com", isolationId = "iso-abc")

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

    @Test
    fun `initial state loads app and sets isLoading false`() {
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(testApp, viewModel.uiState.value.app)
    }

    @Test
    fun `setName updates app name and saves`() = runTest {
        viewModel.setName("NewName")
        advanceUntilIdle()
        assertEquals("NewName", viewModel.uiState.value.app?.name)
        coVerify(exactly = 1) { saveWebApp(match { it.name == "NewName" }) }
    }

    @Test
    fun `setUrl updates app url and saves`() = runTest {
        viewModel.setUrl("https://new.com")
        advanceUntilIdle()
        assertEquals("https://new.com", viewModel.uiState.value.app?.url)
        coVerify(exactly = 1) { saveWebApp(match { it.url == "https://new.com" }) }
    }

    @Test
    fun `toggleFullscreen flips isFullscreen and saves`() = runTest {
        assertFalse(viewModel.uiState.value.app?.isFullscreen ?: true)
        viewModel.toggleFullscreen()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.app?.isFullscreen ?: false)
        coVerify(exactly = 1) { saveWebApp(match { it.isFullscreen }) }
    }

    @Test
    fun `toggleAdBlock flips adBlockEnabled and saves`() = runTest {
        val initial = viewModel.uiState.value.app?.adBlockEnabled ?: true
        viewModel.toggleAdBlock()
        advanceUntilIdle()
        assertEquals(!initial, viewModel.uiState.value.app?.adBlockEnabled)
    }

    @Test
    fun `setLockType updates lockType and saves`() = runTest {
        viewModel.setLockType(LockType.PASSWORD)
        advanceUntilIdle()
        assertEquals(LockType.PASSWORD, viewModel.uiState.value.app?.lockType)
        coVerify(exactly = 1) { saveWebApp(match { it.lockType == LockType.PASSWORD }) }
    }

    @Test
    fun `showDeleteDialog sets showDeleteDialog true`() {
        viewModel.showDeleteDialog()
        assertTrue(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `dismissDeleteDialog sets showDeleteDialog false`() {
        viewModel.showDeleteDialog()
        viewModel.dismissDeleteDialog()
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `deleteApp sets deleted true`() = runTest {
        viewModel.deleteApp()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.deleted)
    }

    @Test
    fun `requestDisableLock shows dialog without error`() {
        viewModel.requestDisableLock()
        assertTrue(viewModel.uiState.value.showDisableLockDialog)
        assertFalse(viewModel.uiState.value.disableLockError)
    }

    @Test
    fun `dismissDisableLockDialog hides dialog`() {
        viewModel.requestDisableLock()
        viewModel.dismissDisableLockDialog()
        assertFalse(viewModel.uiState.value.showDisableLockDialog)
    }

    @Test
    fun `setTranslateTarget updates translateTarget and saves`() = runTest {
        viewModel.setTranslateTarget(TranslateLanguage.JAPANESE)
        advanceUntilIdle()
        assertEquals(TranslateLanguage.JAPANESE, viewModel.uiState.value.app?.translateTarget)
    }

    @Test
    fun `closeIconPackPicker sets showIconPackPicker false`() {
        viewModel.closeIconPackPicker()
        assertFalse(viewModel.uiState.value.showIconPackPicker)
    }

    @Test
    fun `setIconPickerQuery updates iconPickerQuery`() {
        viewModel.setIconPickerQuery("github")
        assertEquals("github", viewModel.uiState.value.iconPickerQuery)
    }

    @Test
    fun `toggleSwipeToRefresh flips swipeToRefreshEnabled from true to false`() = runTest {
        // swipeToRefreshEnabled defaults to true in WebApp
        assertTrue(viewModel.uiState.value.app?.swipeToRefreshEnabled ?: false)
        viewModel.toggleSwipeToRefresh()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.app?.swipeToRefreshEnabled ?: true)
        coVerify(exactly = 1) { saveWebApp(match { !it.swipeToRefreshEnabled }) }
    }

    @Test
    fun `toggleSwipeToRefresh is idempotent — toggling twice restores original value`() = runTest {
        val initial = viewModel.uiState.value.app?.swipeToRefreshEnabled ?: false
        viewModel.toggleSwipeToRefresh()
        advanceUntilIdle()
        viewModel.toggleSwipeToRefresh()
        advanceUntilIdle()
        assertEquals(initial, viewModel.uiState.value.app?.swipeToRefreshEnabled)
    }
}
