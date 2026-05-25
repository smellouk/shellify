package io.shellify.app.presentation.home

import android.app.NotificationManager
import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.domain.model.Category
import io.shellify.app.domain.model.PwaManifest
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val getWebApps = mockk<GetWebAppsUseCase>()
    private val deleteAllAppsUseCase = mockk<DeleteAllAppsUseCase>(relaxed = true)
    private val deleteWebApp = mockk<DeleteWebAppUseCase>()
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val saveWebApp = mockk<SaveWebAppUseCase>()
    private val isolationManager = mockk<IsolationManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val pwaAnalyzer = mockk<PwaAnalyzer>(relaxed = true)
    private val faviconFetcher = mockk<FaviconFetcher>(relaxed = true)

    private val app1 = WebApp(id = 1L, name = "Gmail", url = "https://gmail.com", isolationId = "iso-1")
    private val app2 = WebApp(id = 2L, name = "Twitter", url = "https://twitter.com", isolationId = "iso-2", categoryId = 10L)
    private val cat1 = Category(id = 10L, name = "Social")

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { getWebApps() } returns flowOf(listOf(app1, app2))
        every { getCategories() } returns flowOf(listOf(cat1))
        coEvery { saveWebApp(any()) } returns 0L
        coEvery { deleteWebApp(any()) } returns Unit
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockk<NotificationManager>(relaxed = true)
        viewModel = HomeViewModel(
            getWebApps, deleteAllAppsUseCase, deleteWebApp, getCategories, saveWebApp,
            isolationManager, context, pwaAnalyzer, faviconFetcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads apps and categories`() = runTest {
        // Subscribe to activate WhileSubscribed sharing
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.apps.size)
        assertEquals(1, state.categories.size)
        assertTrue(state.hasAnyApps)
        assertFalse(state.isLoading)
    }

    @Test
    fun `selectCategory filters apps by categoryId`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.selectCategory(10L)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(1, state.apps.size)
        assertEquals("Twitter", state.apps[0].name)
        assertEquals(10L, state.selectedCategoryId)
    }

    @Test
    fun `selectCategory null shows all apps`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.selectCategory(10L)
        viewModel.selectCategory(null)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.apps.size)
        assertNull(state.selectedCategoryId)
    }

    @Test
    fun `setSearch filters apps by name`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSearch("gmail")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(1, state.apps.size)
        assertEquals("Gmail", state.apps[0].name)
        assertEquals("gmail", state.searchQuery)
    }

    @Test
    fun `setSearch filters apps by url`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSearch("twitter.com")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(1, state.apps.size)
        assertEquals("Twitter", state.apps[0].name)
    }

    @Test
    fun `assignCategory saves app with new categoryId`() = runTest {
        viewModel.assignCategory(app1, 10L)
        advanceUntilIdle()
        coVerify(exactly = 1) { saveWebApp(match { it.id == 1L && it.categoryId == 10L }) }
    }

    @Test
    fun `clearData calls isolationManager with correct isolationId`() = runTest {
        viewModel.clearData(app1)
        advanceUntilIdle()
        coVerify(exactly = 1) { isolationManager.clearData("iso-1") }
    }

    @Test
    fun `delete calls deleteWebApp with correct app`() = runTest {
        viewModel.delete(app1)
        advanceUntilIdle()
        coVerify(exactly = 1) { deleteWebApp(app1) }
    }

    @Test
    fun `quickAdd saves a new web app`() = runTest {
        val manifest = mockk<PwaManifest>(relaxed = true)
        every { manifest.name } returns "Gmail"
        every { manifest.bestIconUrl(any()) } returns null
        coEvery { pwaAnalyzer.analyze(any()) } returns manifest
        coEvery { faviconFetcher.fetch(any(), any(), any()) } returns null
        coEvery { saveWebApp(any()) } returns 99L

        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.quickAdd("Gmail", "gmail.com")
        advanceUntilIdle()

        coVerify(atLeast = 1) { saveWebApp(any()) }
    }
}
