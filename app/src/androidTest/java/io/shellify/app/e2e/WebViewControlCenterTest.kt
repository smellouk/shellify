package io.shellify.app.e2e

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

/**
 * Verifies the control center FAB visibility in WebViewActivity:
 * - FAB is displayed when showControlCenter = true
 * - FAB is absent when showControlCenter = false
 */
@RunWith(AndroidJUnit4::class)
class WebViewControlCenterTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val engineReady = CountDownLatch(1)

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
        }
    }

    @After
    fun tearDown() {
        WebViewActivity.engineFactory = null
        WebViewActivity.webAppOverride = null
    }

    @Test
    fun controlCenterFab_isDisplayed_whenShowControlCenterTrue() {
        WebViewActivity.webAppOverride = testApp(showControlCenter = true)
        ActivityScenario.launch<WebViewActivity>(launchIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription(context.getString(R.string.webview_controls_fab_cd)).assertIsDisplayed()
        }
    }

    @Test
    fun controlCenterFab_doesNotExist_whenShowControlCenterFalse() {
        WebViewActivity.webAppOverride = testApp(showControlCenter = false)
        ActivityScenario.launch<WebViewActivity>(launchIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription(context.getString(R.string.webview_controls_fab_cd), useUnmergedTree = true)
                .assertDoesNotExist()
        }
    }

    private fun testApp(showControlCenter: Boolean) = WebApp(
        name = "Test",
        url = "https://test.shellify.app",
        isolationId = UUID.randomUUID().toString(),
        showControlCenter = showControlCenter,
    )

    private fun launchIntent() = Intent(context, WebViewActivity::class.java)
        .putExtra(WebViewActivity.EXTRA_PREVIEW_URL, "https://test.shellify.app")
        .putExtra(WebViewActivity.EXTRA_PREVIEW_NAME, "Test")
}
