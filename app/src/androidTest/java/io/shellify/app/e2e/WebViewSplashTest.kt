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
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Verifies the per-PWA splash overlay lifecycle in WebViewActivity:
 * - Overlay is shown when loading starts
 * - Overlay is cleared on page finish, engine error, and SSL error
 */
@RunWith(AndroidJUnit4::class)
class WebViewSplashTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var capturedCallback: BrowserEngineCallback? = null
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
                    capturedCallback = callback
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
    }

    @After
    fun tearDown() {
        WebViewActivity.engineFactory = null
        capturedCallback = null
    }

    @Test
    fun splashOverlay_isShown_whenLoadingStarts() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use { scenario ->
            engineReady.await(5, TimeUnit.SECONDS)
            scenario.onActivity { activity ->
                assertNotNull(
                    "splashOverlay should be non-null while the page is loading",
                    activity.splashOverlay,
                )
            }
        }
    }

    @Test
    fun splashOverlay_isCleared_onPageFinished() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use { scenario ->
            engineReady.await(5, TimeUnit.SECONDS)
            scenario.onActivity {
                capturedCallback?.onPageFinished("https://shellify.app")
            }
            scenario.onActivity { activity ->
                assertNull(
                    "splashOverlay should be null after page finishes",
                    activity.splashOverlay,
                )
            }
        }
    }

    @Test
    fun splashOverlay_isCleared_onEngineError() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use { scenario ->
            engineReady.await(5, TimeUnit.SECONDS)
            scenario.onActivity {
                capturedCallback?.onError(-1, "net::ERR_NAME_NOT_RESOLVED")
            }
            scenario.onActivity { activity ->
                assertNull(
                    "splashOverlay should be null after a load error",
                    activity.splashOverlay,
                )
            }
        }
    }

    @Test
    fun splashOverlay_isCleared_onSslError() {
        ActivityScenario.launch<WebViewActivity>(previewIntent()).use { scenario ->
            engineReady.await(5, TimeUnit.SECONDS)
            scenario.onActivity {
                capturedCallback?.onSslError("SSL certificate error")
            }
            scenario.onActivity { activity ->
                assertNull(
                    "splashOverlay should be null after an SSL error",
                    activity.splashOverlay,
                )
            }
        }
    }

    private fun previewIntent() = Intent(context, WebViewActivity::class.java)
        .putExtra(WebViewActivity.EXTRA_PREVIEW_URL, "https://shellify.app")
        .putExtra(WebViewActivity.EXTRA_PREVIEW_NAME, "Shellify")
}
