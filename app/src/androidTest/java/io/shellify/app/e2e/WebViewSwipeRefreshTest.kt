package io.shellify.app.e2e

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.core.engine.BrowserEngine
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.webview.WebViewActivity
import io.shellify.core.ui.R as CoreUiR
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Verifies that SwipeRefreshLayout.isEnabled is false when WebApp.swipeToRefreshEnabled = false.
 * STR-06: the gesture is fully disabled at the layout level (no spinner shown) when the toggle is off.
 */
@RunWith(AndroidJUnit4::class)
class WebViewSwipeRefreshTest {

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
        WebViewActivity.webAppOverride = WebApp(
            name = context.getString(CoreUiR.string.test_app_name),
            url = context.getString(CoreUiR.string.test_url_example),
            swipeToRefreshEnabled = false,
        )
    }

    @After
    fun tearDown() {
        WebViewActivity.engineFactory = null
        WebViewActivity.webAppOverride = null
    }

    @Test
    fun swipeRefreshLayout_isDisabled_whenSwipeToRefreshEnabled_isFalse() {
        ActivityScenario.launch<WebViewActivity>(launchIntent()).use { scenario ->
            engineReady.await(5, TimeUnit.SECONDS)
            scenario.onActivity { activity ->
                assertFalse(
                    "swipeRefreshLayout.isEnabled must be false when swipeToRefreshEnabled = false",
                    activity.swipeRefreshLayout.isEnabled,
                )
            }
        }
    }

    @Test
    fun swipeRefreshLayout_isEnabled_whenSwipeToRefreshEnabled_isTrue() {
        WebViewActivity.webAppOverride = WebApp(
            name = context.getString(CoreUiR.string.test_app_name),
            url = context.getString(CoreUiR.string.test_url_example),
            swipeToRefreshEnabled = true,
        )
        ActivityScenario.launch<WebViewActivity>(launchIntent()).use { scenario ->
            engineReady.await(5, TimeUnit.SECONDS)
            scenario.onActivity { activity ->
                assertTrue(
                    "swipeRefreshLayout.isEnabled must be true when swipeToRefreshEnabled = true (default)",
                    activity.swipeRefreshLayout.isEnabled,
                )
            }
        }
    }

    private fun launchIntent() = Intent(context, WebViewActivity::class.java)
        .putExtra(WebViewActivity.EXTRA_PREVIEW_URL, context.getString(CoreUiR.string.test_url_example))
        .putExtra(WebViewActivity.EXTRA_PREVIEW_NAME, context.getString(CoreUiR.string.test_app_name))
}
