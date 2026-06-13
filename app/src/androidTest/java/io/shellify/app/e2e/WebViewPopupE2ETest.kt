package io.shellify.app.e2e

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.core.engine.BrowserEngine
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.core.engine.GeckoViewEngine
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.webview.WebViewActivity
import io.shellify.app.presentation.webview.WebViewServiceProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * E2E coverage for OAuth / "Sign in with Google" popups (window.open()).
 *
 * - [systemWebView_windowOpen_showsPopupOverlay_andBackDismisses] drives the REAL System WebView
 *   engine: a page calls window.open(), the engine fires onCreateWindow, and the activity must host
 *   the popup as an overlay. Back-press must dismiss it. This is the engine reported broken.
 * - [popupHostingContract_showsAndRemovesOverlay] drives the activity's popup-hosting wiring with a
 *   fake engine. This path (onShowPopup / onClosePopup / closeTopPopup / back-press) is shared by
 *   BOTH engines, so it deterministically covers the GeckoView hosting contract too.
 * - [geckoView_windowOpen_showsPopupOverlay] drives the REAL GeckoView engine, but only when the
 *   GeckoView runtime is installed. Per the project convention (see GeckoEngineManagerTest), the CI
 *   emulator never ships Gecko native libs, so this skips cleanly there and runs for real locally.
 */
@RunWith(AndroidJUnit4::class)
class WebViewPopupE2ETest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val provider: WebViewServiceProvider
        get() = context.applicationContext as WebViewServiceProvider

    @After
    fun tearDown() {
        WebViewActivity.engineFactory = null
        WebViewActivity.webAppOverride = null
    }

    // ── Real System WebView ────────────────────────────────────────────────────

    @Test
    fun systemWebView_windowOpen_showsPopupOverlay_andBackDismisses() {
        // engineFactory left null → activity builds a real SystemWebViewEngine. Preview apps default
        // to EngineType.SYSTEM_WEBVIEW.
        ActivityScenario.launch<WebViewActivity>(windowOpenIntent()).use { scenario ->
            awaitPopupShown(scenario)
            scenario.onActivity { assertEquals(1, it.popupOverlayCount()) }

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            scenario.onActivity {
                assertEquals("Back press must dismiss the popup overlay", 0, it.popupOverlayCount())
            }
        }
    }

    // ── Shared hosting contract (both engines) via a fake engine ────────────────

    @Test
    fun popupHostingContract_showsAndRemovesOverlay() {
        val engine = FakePopupEngine()
        WebViewActivity.engineFactory = { engine }

        ActivityScenario.launch<WebViewActivity>(previewIntent("about:blank")).use { scenario ->
            assertTrue("Engine must be created", engine.ready.await(5, TimeUnit.SECONDS))

            // Engine surfaces a popup → activity hosts it as an overlay.
            scenario.onActivity { engine.firePopup(it) }
            scenario.onActivity { assertEquals(1, it.popupOverlayCount()) }

            // Back-press routes through engine.closeTopPopup() → onClosePopup() → overlay removed.
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            scenario.onActivity { assertEquals(0, it.popupOverlayCount()) }
        }
    }

    // ── Real GeckoView (runs only where the Gecko runtime is installed) ─────────

    @Test
    fun geckoView_windowOpen_showsPopupOverlay() {
        assumeTrue(
            "GeckoView runtime not installed — skipping real Gecko popup test",
            provider.geckoEngineManager.isInstalled(),
        )
        provider.injectAndLoadGeckoView()
        WebViewActivity.engineFactory = {
            GeckoViewEngine(context.applicationContext, provider.geckoEngineManager)
        }

        // Gecko's popup blocker only allows window.open() from a user gesture (a real "Sign in with
        // Google" tap), so click a full-screen button rather than firing window.open() on load.
        ActivityScenario.launch<WebViewActivity>(buttonPopupIntent()).use { scenario ->
            tapUntilPopupShown(scenario)
            scenario.onActivity { assertEquals(1, it.popupOverlayCount()) }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Registers a popup-shown latch and waits for it. Also handles the race where the popup is
     * already visible by the time onActivity runs (the page may open the window before we attach).
     */
    private fun awaitPopupShown(scenario: ActivityScenario<WebViewActivity>, timeoutSeconds: Long = 15) {
        val shown = CountDownLatch(1)
        scenario.onActivity {
            if (it.popupOverlayCount() > 0) shown.countDown() else it.popupShownCallback = { shown.countDown() }
        }
        assertTrue(
            "Expected a popup overlay within ${timeoutSeconds}s after window.open()",
            shown.await(timeoutSeconds, TimeUnit.SECONDS),
        )
    }

    /**
     * Taps the screen centre (a real user gesture via injected MotionEvents) until a popup overlay
     * appears, retrying because the page may not be laid out when the first tap lands.
     */
    private fun tapUntilPopupShown(scenario: ActivityScenario<WebViewActivity>, attempts: Int = 10) {
        val shown = CountDownLatch(1)
        scenario.onActivity { it.popupShownCallback = { shown.countDown() } }
        repeat(attempts) {
            if (shown.count == 0L) return
            tapCenter(scenario)
            if (shown.await(2, TimeUnit.SECONDS)) return
        }
        assertTrue("Expected a popup overlay after tapping the sign-in button", shown.count == 0L)
    }

    private fun tapCenter(scenario: ActivityScenario<WebViewActivity>) {
        var x = 0f
        var y = 0f
        scenario.onActivity {
            val decor = it.window.decorView
            x = decor.width / 2f
            y = decor.height / 2f
        }
        if (x <= 0f || y <= 0f) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        instrumentation.sendPointerSync(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0))
        instrumentation.sendPointerSync(MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0))
    }

    /** Preview intent whose page immediately calls window.open() on load (no network needed). */
    private fun windowOpenIntent(): Intent {
        val html = "<html><body><script>window.open('about:blank','_blank','width=200,height=200')</script></body></html>"
        val dataUrl = "data:text/html;base64," + Base64.encodeToString(html.toByteArray(), Base64.NO_WRAP)
        return previewIntent(dataUrl)
    }

    /** Preview intent with a full-screen button that calls window.open() on click (a user gesture). */
    private fun buttonPopupIntent(): Intent {
        val html = "<html><body style=\"margin:0\">" +
            "<button style=\"width:100vw;height:100vh\" " +
            "onclick=\"window.open('about:blank','_blank','width=200,height=200')\">Sign in</button>" +
            "</body></html>"
        val dataUrl = "data:text/html;base64," + Base64.encodeToString(html.toByteArray(), Base64.NO_WRAP)
        return previewIntent(dataUrl)
    }

    private fun previewIntent(url: String): Intent =
        Intent(context, WebViewActivity::class.java)
            .putExtra(WebViewActivity.EXTRA_PREVIEW_URL, url)
            .putExtra(WebViewActivity.EXTRA_PREVIEW_NAME, "Popup Test")

    /**
     * Minimal engine that exercises the activity's popup-hosting contract without a real web engine.
     * [firePopup] mimics onCreateWindow / onNewSession; [closeTopPopup] mimics onCloseWindow /
     * onCloseRequest, both of which every real engine routes through the same callbacks.
     */
    private class FakePopupEngine : BrowserEngine {
        val ready = CountDownLatch(1)
        private var callback: BrowserEngineCallback? = null
        private val popups = ArrayDeque<View>()

        override val engineType = EngineType.SYSTEM_WEBVIEW

        override fun createView(context: Context, app: WebApp, callback: BrowserEngineCallback): View {
            this.callback = callback
            ready.countDown()
            return View(context)
        }

        fun firePopup(context: Context) {
            val popup = View(context)
            popups.addLast(popup)
            callback?.onShowPopup(popup)
        }

        override fun closeTopPopup(): Boolean {
            val popup = popups.removeLastOrNull() ?: return false
            callback?.onClosePopup(popup)
            return true
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
