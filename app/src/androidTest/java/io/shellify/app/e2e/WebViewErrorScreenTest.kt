package io.shellify.app.e2e

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.core.ui.R as CoreUiR
import io.shellify.app.core.engine.BrowserEngine
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.webview.WebViewActivity
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WebViewErrorScreenTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var capturedCallback: BrowserEngineCallback? = null
    private var reloadCalled = false
    private val engineReady = CountDownLatch(1)

    @Before
    fun setUp() {
        reloadCalled = false
        WebViewActivity.engineFactory = {
            object : BrowserEngine {
                override val engineType = EngineType.SYSTEM_WEBVIEW
                override fun createView(context: Context, app: WebApp, callback: BrowserEngineCallback): View {
                    capturedCallback = callback
                    engineReady.countDown()
                    return View(context)
                }
                override fun loadUrl(url: String) {}
                override fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {}
                override fun canGoBack() = false
                override fun goBack() {}
                override fun reload() { reloadCalled = true }
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
        capturedCallback = null
    }

    @Test
    fun noInternetError_showsNoInternetScreen() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            it.onActivity { capturedCallback?.onError(-2, "net::ERR_INTERNET_DISCONNECTED") }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(context.getString(CoreUiR.string.webview_error_no_internet_title)).assertIsDisplayed()
        }
    }

    @Test
    fun sslError_showsSslErrorScreen() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            it.onActivity { capturedCallback?.onSslError("SSL certificate error") }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(context.getString(CoreUiR.string.webview_error_ssl_title)).assertIsDisplayed()
        }
    }

    @Test
    fun pageStarted_doesNotClearErrorScreen() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            it.onActivity { capturedCallback?.onError(-2, "net::ERR_INTERNET_DISCONNECTED") }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(context.getString(CoreUiR.string.webview_error_no_internet_title)).assertIsDisplayed()

            it.onActivity { capturedCallback?.onPageStarted("https://shellify.app") }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(context.getString(CoreUiR.string.webview_error_no_internet_title)).assertIsDisplayed()
        }
    }

    @Test
    fun pageFinished_afterSuccessfulLoad_clearsErrorScreen() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            it.onActivity { capturedCallback?.onError(-2, "net::ERR_INTERNET_DISCONNECTED") }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(context.getString(CoreUiR.string.webview_error_no_internet_title)).assertIsDisplayed()

            it.onActivity {
                capturedCallback?.onPageStarted("https://shellify.app")
                capturedCallback?.onPageFinished("https://shellify.app")
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(context.getString(CoreUiR.string.webview_error_no_internet_title)).assertDoesNotExist()
        }
    }

    @Test
    fun pageFinished_afterErrorInSameCycle_keepsErrorScreen() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            it.onActivity {
                capturedCallback?.onPageStarted("https://shellify.app")
                capturedCallback?.onError(-2, "net::ERR_INTERNET_DISCONNECTED")
                capturedCallback?.onPageFinished("https://shellify.app")
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(context.getString(CoreUiR.string.webview_error_no_internet_title)).assertIsDisplayed()
        }
    }

    @Test
    fun retryButton_callsEngineReload() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use {
            engineReady.await(5, TimeUnit.SECONDS)
            it.onActivity { capturedCallback?.onError(-2, "net::ERR_INTERNET_DISCONNECTED") }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(context.getString(CoreUiR.string.webview_error_retry)).performClick()
            composeTestRule.waitForIdle()
            assertTrue("reload() should have been called after tapping Try again", reloadCalled)
        }
    }

    private fun previewIntent() = Intent(context, WebViewActivity::class.java)
        .putExtra(WebViewActivity.EXTRA_PREVIEW_URL, "https://shellify.app")
        .putExtra(WebViewActivity.EXTRA_PREVIEW_NAME, "Shellify")
}
