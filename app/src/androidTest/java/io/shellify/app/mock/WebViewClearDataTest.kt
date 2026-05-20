package io.shellify.app.mock

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
import io.shellify.app.R
import io.shellify.app.core.engine.BrowserEngine
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.webview.WebViewActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies the full "Clear Data" flow in the WebView control center:
 * open the bottom sheet, tap the clear-data row, confirm the dialog,
 * and observe that the engine reloads the app's URL.
 */
@RunWith(AndroidJUnit4::class)
class WebViewClearDataTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val engineReady = CountDownLatch(1)
    private val loadUrlCallCount = AtomicInteger(0)

    @Before
    fun setUp() {
        WebViewActivity.engineFactory = {
            object : BrowserEngine {
                override val engineType = EngineType.SYSTEM_WEBVIEW

                override fun createView(
                    context: Context,
                    app: WebApp,
                    callback: BrowserEngineCallback,
                ): View {
                    engineReady.countDown()
                    return View(context)
                }

                override fun loadUrl(url: String) {
                    loadUrlCallCount.incrementAndGet()
                }

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
        }
    }

    @After
    fun tearDown() {
        WebViewActivity.engineFactory = null
        WebViewActivity.webAppOverride = null
    }

    @Test
    fun clearData_fromControlCenter_reloadsEngine() {
        WebViewActivity.webAppOverride = WebApp(
            name = "Test",
            url = "https://test.shellify.app",
            isolationId = UUID.randomUUID().toString(),
            showControlCenter = true,
        )
        ActivityScenario.launch<WebViewActivity>(launchIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            composeTestRule.waitForIdle()

            val countAfterInitialLoad = loadUrlCallCount.get()

            // Open the control center bottom sheet.
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.webview_controls_fab_cd))
                .performClick()
            composeTestRule.waitForIdle()

            // Tap the "Clear data" row.
            composeTestRule
                .onNodeWithText(context.getString(R.string.webview_control_clear_data), useUnmergedTree = true)
                .performClick()
            composeTestRule.waitForIdle()

            // Confirm the destructive dialog.
            composeTestRule
                .onNodeWithText(context.getString(R.string.home_clear_data_button))
                .performClick()
            composeTestRule.waitForIdle()

            // The engine must have been told to loadUrl again after clearing.
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                loadUrlCallCount.get() > countAfterInitialLoad
            }
        }
    }

    @Test
    fun clearData_confirmDialog_isDisplayed_afterTappingClearDataRow() {
        WebViewActivity.webAppOverride = WebApp(
            name = "TestApp",
            url = "https://test.shellify.app",
            isolationId = UUID.randomUUID().toString(),
            showControlCenter = true,
        )
        ActivityScenario.launch<WebViewActivity>(launchIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.webview_controls_fab_cd))
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText(context.getString(R.string.webview_control_clear_data), useUnmergedTree = true)
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText(context.getString(R.string.home_clear_data_title))
                .assertIsDisplayed()
        }
    }

    private fun launchIntent() = Intent(context, WebViewActivity::class.java)
        .putExtra(WebViewActivity.EXTRA_PREVIEW_URL, "https://test.shellify.app")
        .putExtra(WebViewActivity.EXTRA_PREVIEW_NAME, "Test")
}
