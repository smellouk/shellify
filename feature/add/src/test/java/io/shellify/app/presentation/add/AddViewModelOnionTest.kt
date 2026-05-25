package io.shellify.app.presentation.add

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.GetWebAppByNameUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Verifies .onion URL exemptions in AddViewModel (Plan 02-05 Task 3).
 *
 * Four behaviours:
 *  A1: http://host.onion is accepted — does NOT set urlError = add_url_error_http
 *  A2: http://example.com (non-onion) continues to set urlError (regression guard)
 *  A3: bare host.onion (no scheme) is normalized to http://... (NOT https://)
 *  A4: https://host.onion (explicit https) is accepted without error
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddViewModelOnionTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val getWebAppById = mockk<GetWebAppByIdUseCase>()
    private val getWebAppByName = mockk<GetWebAppByNameUseCase>()
    private val saveWebApp = mockk<SaveWebAppUseCase>(relaxed = true)
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val analyzer = mockk<PwaAnalyzer>(relaxed = true)
    private val faviconFetcher = mockk<FaviconFetcher>(relaxed = true)
    private val geckoEngineManager = mockk<GeckoEngineManager>(relaxed = true)
    private val themeManager = mockk<ThemeManager>(relaxed = true)
    private val simpleIconsManager = mockk<SimpleIconsManager>(relaxed = true)
    private val passwordManager = mockk<PasswordManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { getCategories() } returns flowOf(emptyList())
        every { simpleIconsManager.state } returns MutableStateFlow(SimpleIconsState.NotImported)
        every { themeManager.defaultEngineType } returns MutableStateFlow(EngineType.SYSTEM_WEBVIEW)
        every { themeManager.globalNotificationsEnabled } returns MutableStateFlow(true)
        every { passwordManager.passwordHash } returns MutableStateFlow(null)
        // No duplicate name found by default — lets save() proceed past the name uniqueness check.
        coEvery { getWebAppByName(any()) } returns null
        // save() calls getWebAppById after persisting to load the saved entity.
        // Return null so the ViewModel falls back to buildApp().
        coEvery { getWebAppById(any()) } returns null
        // getString() returns "" for any resource ID by default (mockk relaxed)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = AddViewModel(
        appId = 0L,
        getWebAppById = getWebAppById,
        getWebAppByName = getWebAppByName,
        saveWebApp = saveWebApp,
        getCategories = getCategories,
        analyzer = analyzer,
        faviconFetcher = faviconFetcher,
        geckoEngineManager = geckoEngineManager,
        themeManager = themeManager,
        simpleIconsManager = simpleIconsManager,
        passwordManager = passwordManager,
        context = context,
    )

    /**
     * A1: http://host.onion URL should be accepted as-is.
     * The http-not-allowed error must NOT be raised (onion hosts use Tor circuit encryption).
     * Set a name so save() can proceed through URL validation to reach name checks.
     */
    @Test
    fun `http onion url is accepted without urlError`() = runTest {
        val vm = newVm()
        vm.setName("DarkSite")
        vm.setUrl("http://abcdef1234567890.onion")
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        // urlError must be null — onion HTTP is explicitly exempt from https-only rule
        assertNull("http .onion must not set urlError", vm.uiState.value.urlError)
    }

    /**
     * A2: http://example.com (non-onion host) must still set urlError — regression guard.
     */
    @Test
    fun `http non-onion url still sets urlError`() = runTest {
        val vm = newVm()
        vm.setUrl("http://example.com")
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertNotNull("http non-onion must set urlError", vm.uiState.value.urlError)
    }

    /**
     * A3: Bare *.onion (no scheme) should be normalized to http:// — NOT https://.
     * Onion services rarely have valid HTTPS certificates; defaulting to http keeps them working.
     */
    @Test
    fun `bare onion hostname is classified as onion url`() {
        val vm = newVm()
        assertTrue(vm.isOnionUrl("abcdef1234567890.onion"))
        assertTrue(vm.isOnionUrl("abcdef1234567890.onion/path"))
        assertTrue(vm.isOnionUrl("http://abcdef1234567890.onion"))
        assertTrue(vm.isOnionUrl("https://abcdef1234567890.onion"))
        // Non-onion must return false
        assertFalse(vm.isOnionUrl("https://example.com"))
        assertFalse(vm.isOnionUrl("http://example.com"))
    }

    /**
     * A4: https://host.onion (explicit https) should be accepted without error.
     */
    @Test
    fun `https onion url is accepted without urlError`() = runTest {
        val vm = newVm()
        vm.setName("SecureOnion")
        vm.setUrl("https://abcdef1234567890.onion")
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertNull("https .onion must not set urlError", vm.uiState.value.urlError)
    }
}
