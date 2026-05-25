package io.shellify.app.e2e

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.core.engine.BrowserEngine
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.webview.WebViewActivity
import io.shellify.app.util.FakeData
import io.shellify.core.ui.R as CoreUiR
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * WebViewActivity instrumented tests for Phase 2: Privacy & Tor features.
 *
 * Tests require [WebViewActivity.engineFactory] to stub the engine so no real
 * WebView or GeckoView is created on the emulator.
 *
 * Covered UAT scenarios:
 *  T-06 Panic option is accessible via the Control Center FAB
 *  T-07 Panic option shows confirm dialog
 *  T-07 Panic confirm dialog has the correct action buttons
 *  T-04 Incognito indicator shown in Control Center when alwaysIncognito = true
 */
@RunWith(AndroidJUnit4::class)
class PrivacyWebViewActivityE2ETest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    // Recreated before each test so the latch is always at count = 1.
    private var engineReady = CountDownLatch(1)

    /** Stub engine injected into WebViewActivity to avoid real WebView/GeckoView overhead. */
    private val stubEngine = object : BrowserEngine {
        override val engineType = EngineType.SYSTEM_WEBVIEW
        override fun createView(context: Context, app: WebApp, callback: BrowserEngineCallback): View {
            engineReady.countDown()
            callback.onPageFinished(app.url)
            return View(context)
        }
        override fun loadUrl(url: String) {}
        override fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {}
        override fun canGoBack() = false
        override fun goBack() {}
        override fun reload() {}
        override fun stopLoading() {}
        override fun getCurrentUrl(): String? = null
        override fun getView(): View? = null
        override fun destroy() {}
        override fun clearCache(includeDiskFiles: Boolean) {}
    }

    @Before
    fun setUp() {
        engineReady = CountDownLatch(1)
        WebViewActivity.engineFactory = { stubEngine }
    }

    @After
    fun tearDown() {
        WebViewActivity.engineFactory = null
        WebViewActivity.webAppOverride = null
    }

    // ─── Panic option via Control Center ──────────────────────────────────────

    @Test
    fun panicOption_accessibleViaControlCenter() {
        WebViewActivity.webAppOverride = anyApp()
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            composeRule.waitForIdle()
            composeRule
                .onNodeWithContentDescription(context.getString(CoreUiR.string.webview_controls_fab_cd))
                .performClick()
            composeRule.waitForIdle()
            // Panic row is inside a ModalBottomSheet that may be partially expanded.
            // ModalBottomSheet does not expose standard scroll semantics, so performScrollTo()
            // cannot reliably bring the row into the viewport. assertExists() confirms the row
            // is in the composition; the next two tests verify it is clickable.
            composeRule
                .onNodeWithText(context.getString(CoreUiR.string.webview_control_panic))
                .assertExists()
        }
    }

    @Test
    fun panicOption_click_showsConfirmDialog() {
        WebViewActivity.webAppOverride = anyApp()
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            composeRule.waitForIdle()
            composeRule
                .onNodeWithContentDescription(context.getString(CoreUiR.string.webview_controls_fab_cd))
                .performClick()
            composeRule.waitForIdle()
            composeRule
                .onNodeWithText(context.getString(CoreUiR.string.webview_control_panic))
                .performClick()
            composeRule.waitForIdle()
            composeRule
                .onNodeWithText(context.getString(CoreUiR.string.webview_panic_confirm_title))
                .assertIsDisplayed()
        }
    }

    @Test
    fun panicConfirmDialog_hasExpectedButtons() {
        WebViewActivity.webAppOverride = anyApp()
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            composeRule.waitForIdle()
            composeRule
                .onNodeWithContentDescription(context.getString(CoreUiR.string.webview_controls_fab_cd))
                .performClick()
            composeRule.waitForIdle()
            composeRule
                .onNodeWithText(context.getString(CoreUiR.string.webview_control_panic))
                .performClick()
            composeRule.waitForIdle()
            composeRule
                .onNodeWithText(context.getString(CoreUiR.string.webview_panic_confirm_button))
                .assertIsDisplayed()
            composeRule
                .onNodeWithText(context.getString(CoreUiR.string.webview_panic_confirm_dismiss))
                .assertIsDisplayed()
        }
    }

    // ─── Incognito indicator via Control Center ────────────────────────────────

    @Test
    fun incognitoIndicator_shownInSheet_whenAlwaysIncognito() {
        WebViewActivity.webAppOverride = FakeData.webApp(
            name = "Test",
            url = "https://test.shellify.app",
            alwaysIncognito = true,
        )
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            composeRule.waitForIdle()
            composeRule
                .onNodeWithContentDescription(context.getString(CoreUiR.string.webview_controls_fab_cd))
                .performClick()
            composeRule.waitForIdle()
            composeRule
                .onNodeWithText(context.getString(CoreUiR.string.webview_incognito_mode))
                .assertIsDisplayed()
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun anyApp() = FakeData.webApp(
        name = "Test",
        url = "https://test.shellify.app",
    )

    private fun previewIntent(): Intent =
        Intent(context, WebViewActivity::class.java)
            .putExtra(WebViewActivity.EXTRA_PREVIEW_URL, "https://test.shellify.app")
            .putExtra(WebViewActivity.EXTRA_PREVIEW_NAME, "Test")
}
