package io.shellify.app.presentation.shortcuts

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.shortcut.PwaShortcutManager
import io.shellify.app.core.shortcut.ShortcutIconBuilder
import io.shellify.app.core.shortcut.SvgIconRenderer
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val getWebApps = mockk<GetWebAppsUseCase>()
    private val saveWebApp = mockk<SaveWebAppUseCase>()
    private val analyzer = mockk<PwaAnalyzer>(relaxed = true)
    private val faviconFetcher = mockk<FaviconFetcher>(relaxed = true)
    private val simpleIconsManager = mockk<SimpleIconsManager>(relaxed = true)

    private val testApp = WebApp(id = 1L, name = "Gmail", url = "https://gmail.com", isolationId = "iso-gmail")
    private val testItem = ShortcutItem(app = testApp, shortcutId = "pwa_iso-gmail", label = "Gmail")

    private lateinit var viewModel: ShortcutsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(PwaShortcutManager, SvgIconRenderer, ShortcutIconBuilder)
        every { getWebApps() } returns flowOf(listOf(testApp))
        every { PwaShortcutManager.getPinnedShortcuts(any()) } returns emptyList()
        every { PwaShortcutManager.removeShortcut(any(), any()) } returns Unit
        every { PwaShortcutManager.createShortcut(any(), any()) } returns true
        every { PwaShortcutManager.rename(any(), any(), any()) } returns true
        every { PwaShortcutManager.changeIcon(any(), any(), any(), any()) } returns true
        every { ShortcutIconBuilder.build(any(), any()) } returns mockk(relaxed = true)
        coEvery { SvgIconRenderer.render(any(), any(), any(), any(), any()) } returns null
        every { simpleIconsManager.state } returns MutableStateFlow(SimpleIconsState.NotImported)
        coEvery { saveWebApp(any()) } returns 1L
        viewModel = ShortcutsViewModel(context, getWebApps, saveWebApp, analyzer, faviconFetcher, simpleIconsManager)
    }

    @After
    fun tearDown() {
        unmockkObject(PwaShortcutManager, SvgIconRenderer, ShortcutIconBuilder)
        Dispatchers.resetMain()
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    @Test
    fun `startRename sets renameTarget and renameText`() {
        viewModel.startRename(testItem)
        val state = viewModel.uiState.value
        assertEquals(testItem, state.renameTarget)
        assertEquals("Gmail", state.renameText)
    }

    @Test
    fun `setRenameText updates renameText`() {
        viewModel.startRename(testItem)
        viewModel.setRenameText("My Gmail")
        assertEquals("My Gmail", viewModel.uiState.value.renameText)
    }

    @Test
    fun `dismissRename clears rename state`() {
        viewModel.startRename(testItem)
        viewModel.dismissRename()
        assertNull(viewModel.uiState.value.renameTarget)
        assertEquals("", viewModel.uiState.value.renameText)
    }

    // ── Icon sheet ────────────────────────────────────────────────────────────

    @Test
    fun `showIconSheet sets iconSheetTarget`() {
        viewModel.showIconSheet(testItem)
        assertEquals(testItem, viewModel.uiState.value.iconSheetTarget)
        assertEquals(IconRefreshState.Idle, viewModel.uiState.value.iconRefreshState)
    }

    @Test
    fun `dismissIconSheet clears iconSheetTarget`() {
        viewModel.showIconSheet(testItem)
        viewModel.dismissIconSheet()
        assertNull(viewModel.uiState.value.iconSheetTarget)
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    @Test
    fun `showRemove sets removeTarget`() {
        viewModel.showRemove(testItem)
        assertEquals(testItem, viewModel.uiState.value.removeTarget)
    }

    @Test
    fun `dismissRemove clears removeTarget`() {
        viewModel.showRemove(testItem)
        viewModel.dismissRemove()
        assertNull(viewModel.uiState.value.removeTarget)
    }

    @Test
    fun `confirmRemove clears removeTarget`() = runTest {
        viewModel.showRemove(testItem)
        viewModel.confirmRemove()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.removeTarget)
    }

    // ── Add sheet ─────────────────────────────────────────────────────────────

    @Test
    fun `showAddSheet sets showAddSheet true`() {
        viewModel.showAddSheet()
        assertTrue(viewModel.uiState.value.showAddSheet)
    }

    @Test
    fun `dismissAddSheet sets showAddSheet false`() {
        viewModel.showAddSheet()
        viewModel.dismissAddSheet()
        assertFalse(viewModel.uiState.value.showAddSheet)
    }

    // ── Icon picker ───────────────────────────────────────────────────────────

    @Test
    fun `setIconPickerQuery updates query`() {
        viewModel.setIconPickerQuery("slack")
        assertEquals("slack", viewModel.uiState.value.iconPickerQuery)
    }

    @Test
    fun `closeIconPackPicker clears picker state`() {
        viewModel.closeIconPackPicker()
        assertFalse(viewModel.uiState.value.showIconPackPicker)
        assertTrue(viewModel.uiState.value.packIcons.isEmpty())
    }

    // ── isIconPackAvailable ───────────────────────────────────────────────────

    @Test
    fun `isIconPackAvailable is false when SimpleIconsManager state is NotImported`() {
        every { simpleIconsManager.state } returns MutableStateFlow(SimpleIconsState.NotImported)
        val vm = ShortcutsViewModel(context, getWebApps, saveWebApp, analyzer, faviconFetcher, simpleIconsManager)
        assertFalse(vm.uiState.value.isIconPackAvailable)
    }

    @Test
    fun `isIconPackAvailable is true when SimpleIconsManager state is Imported`() {
        every { simpleIconsManager.state } returns MutableStateFlow(SimpleIconsState.Imported(iconCount = 3000))
        val vm = ShortcutsViewModel(context, getWebApps, saveWebApp, analyzer, faviconFetcher, simpleIconsManager)
        assertTrue(vm.uiState.value.isIconPackAvailable)
    }

    // ── deleteAllShortcuts ────────────────────────────────────────────────────
    // These tests do NOT use runTest because deleteAllShortcuts() calls load() on
    // Dispatchers.IO — an IO coroutine that outlives advanceUntilIdle(). runTest
    // accumulates uncaught exceptions from background threads and would surface them
    // in the NEXT runTest block as UncaughtExceptionsBeforeTest. Regular blocking
    // tests are immune to this, and CountDownLatch on saveWebApp provides the needed
    // synchronisation signal.

    private val appA = WebApp(id = 2L, name = "App A", url = "https://a.com", isolationId = "iso-a", hasLauncherShortcut = true)
    private val appB = WebApp(id = 3L, name = "App B", url = "https://b.com", isolationId = "iso-b", hasLauncherShortcut = true)

    @Test
    fun `deleteAllShortcuts saves every app with hasLauncherShortcut false`() {
        val latch = CountDownLatch(2)
        coEvery { saveWebApp(any()) } answers { latch.countDown(); 1L }
        every { getWebApps() } returns flowOf(listOf(appA, appB))
        viewModel.deleteAllShortcuts()
        @Suppress("MagicNumber") latch.await(2000, TimeUnit.MILLISECONDS)
        coVerify(exactly = 1) { saveWebApp(appA.copy(hasLauncherShortcut = false)) }
        coVerify(exactly = 1) { saveWebApp(appB.copy(hasLauncherShortcut = false)) }
    }

    @Test
    fun `deleteAllShortcuts removes launcher shortcut for each app`() {
        val latch = CountDownLatch(2)
        coEvery { saveWebApp(any()) } answers { latch.countDown(); 1L }
        every { getWebApps() } returns flowOf(listOf(appA, appB))
        viewModel.deleteAllShortcuts()
        @Suppress("MagicNumber") latch.await(2000, TimeUnit.MILLISECONDS)
        verify(exactly = 1) { PwaShortcutManager.removeShortcut(any(), appA) }
        verify(exactly = 1) { PwaShortcutManager.removeShortcut(any(), appB) }
    }

    @Test
    fun `deleteAllShortcuts with empty app list does not call saveWebApp`() {
        val latch = CountDownLatch(1)
        // Signal on getPinnedShortcuts (the reload after the empty forEach) to know when done
        every { PwaShortcutManager.getPinnedShortcuts(any()) } answers { latch.countDown(); emptyList() }
        every { getWebApps() } returns flowOf(emptyList())
        viewModel.deleteAllShortcuts()
        @Suppress("MagicNumber") latch.await(2000, TimeUnit.MILLISECONDS)
        coVerify(exactly = 0) { saveWebApp(any()) }
        verify(exactly = 0) { PwaShortcutManager.removeShortcut(any(), any()) }
    }

}
