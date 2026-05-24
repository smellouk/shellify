package io.shellify.app.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import io.shellify.app.presentation.settings.AppSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the per-app control center toggle via AppSettingsViewModel:
 * - Default state has showControlCenter = true
 * - toggleControlCenter() flips the value
 * - SaveWebAppUseCase is called with the updated value
 */
@RunWith(AndroidJUnit4::class)
class ControlCenterToggleTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var saveWebApp: SaveWebAppUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        saveWebApp = mockk()
        coEvery { saveWebApp(any<WebApp>()) } returns 1L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showControlCenter_isTrue_byDefault() = runTest {
        val vm = buildVm(WebApp(id = 1L, name = "A", url = "https://a.test"))
        assertTrue(vm.uiState.value.app!!.showControlCenter)
    }

    @Test
    fun toggleControlCenter_setsFalse_whenCurrentlyTrue() = runTest {
        val vm = buildVm(WebApp(id = 1L, name = "A", url = "https://a.test", showControlCenter = true))
        vm.toggleControlCenter()
        assertFalse(vm.uiState.value.app!!.showControlCenter)
    }

    @Test
    fun toggleControlCenter_setsTrue_whenCurrentlyFalse() = runTest {
        val vm = buildVm(WebApp(id = 1L, name = "A", url = "https://a.test", showControlCenter = false))
        vm.toggleControlCenter()
        assertTrue(vm.uiState.value.app!!.showControlCenter)
    }

    @Test
    fun toggleControlCenter_callsSaveWebApp_withUpdatedValue() = runTest {
        val vm = buildVm(WebApp(id = 1L, name = "A", url = "https://a.test", showControlCenter = true))
        vm.toggleControlCenter()
        coVerify { saveWebApp(match { !it.showControlCenter }) }
    }

    @Test
    fun toggleControlCenter_togglesTwice_restoresOriginalValue() = runTest {
        val vm = buildVm(WebApp(id = 1L, name = "A", url = "https://a.test", showControlCenter = true))
        vm.toggleControlCenter()
        vm.toggleControlCenter()
        assertTrue(vm.uiState.value.app!!.showControlCenter)
    }

    private fun buildVm(app: WebApp): AppSettingsViewModel {
        val getWebAppById = mockk<GetWebAppByIdUseCase>()
        coEvery { getWebAppById(app.id) } returns app
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
            passwordManager = PasswordManager(context),
            geckoEngineManager = mockk<GeckoEngineManager> {
                every { installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
            },
        )
    }
}
