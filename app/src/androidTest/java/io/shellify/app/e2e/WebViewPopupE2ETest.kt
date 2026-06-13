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

    /**
     * Regression for the OAuth flow: the popup must actually LOAD and run its script. The popup
     * opens a data: URL that calls window.close() — which only fires if the data: page loaded (i.e.
     * was not mis-routed to an external intent by the scheme guard) and window.close() is honoured.
     */
    @Test
    fun systemWebView_popupSelfClose_dismissesOverlay() {
        ActivityScenario.launch<WebViewActivity>(selfClosingPopupIntent()).use { scenario ->
            awaitPopupShown(scenario)
            scenario.onActivity { assertEquals(1, it.popupOverlayCount()) }
            assertTrue(
                "Popup must load its data: page and self-dismiss via window.close()",
                awaitPopupCount(scenario, target = 0, timeoutSeconds = 10),
            )
        }
    }

    /**
     * Regression for the OAuth opener channel: a popup must be able to postMessage back to its
     * window.opener. The opener opens popup #1, which messages the opener; on receipt the opener
     * opens popup #2. Reaching 2 overlays proves the message was delivered (count only grows, so
     * the assertion is race-free).
     */
    @Test
    fun systemWebView_popupOpenerPostMessage_roundTrips() {
        ActivityScenario.launch<WebViewActivity>(openerRoundTripIntent()).use { scenario ->
            awaitPopupShown(scenario)
            assertTrue(
                "Opener must receive the popup's postMessage and open a second window",
                awaitPopupCount(scenario, target = 2, timeoutSeconds = 10),
            )
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

    /** Polls [WebViewActivity.popupOverlayCount] until it reaches [target] or the timeout elapses. */
    private fun awaitPopupCount(
        scenario: ActivityScenario<WebViewActivity>,
        target: Int,
        timeoutSeconds: Long,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        while (System.nanoTime() < deadline) {
            var count = -1
            scenario.onActivity { count = it.popupOverlayCount() }
            if (count == target) return true
            Thread.sleep(100)
        }
        return false
    }

    /** Preview intent whose page immediately calls window.open() on load (no network needed). */
    private fun windowOpenIntent(): Intent {
        val html = "<html><body><script>window.open('about:blank','_blank','width=200,height=200')</script></body></html>"
        val dataUrl = "data:text/html;base64," + Base64.encodeToString(html.toByteArray(), Base64.NO_WRAP)
        return previewIntent(dataUrl)
    }

    /**
     * Preview intent whose page opens an about:blank popup and writes a script that calls
     * window.close() shortly after — exercises the full open→load→close path. Note we cannot
     * window.open() a data: URL: Chromium blocks navigating a popup's top frame to data:, so the
     * page would never load. about:blank + document.write() is the offline-safe equivalent.
     */
    private fun selfClosingPopupIntent(): Intent {
        val opener = "<html><body><script>" +
            "var w=window.open('about:blank','_blank','width=200,height=200');" +
            "if(w){w.document.write('<scr'+'ipt>setTimeout(function(){window.close()},300)<\\/scr'+'ipt>');w.document.close();}" +
            "</script></body></html>"
        val openerUrl = "data:text/html;base64," + Base64.encodeToString(opener.toByteArray(), Base64.NO_WRAP)
        return previewIntent(openerUrl)
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

    /**
     * Opener page that opens popup #1 (which postMessages the opener) and, on receiving that
     * message, opens popup #2 — so a second overlay only appears if window.opener works.
     */
    private fun openerRoundTripIntent(): Intent {
        val opener = "<html><body><script>" +
            "window.addEventListener('message',function(e){" +
            "if(e.data&&e.data.shellifyOAuth){window.open('about:blank','_blank','width=100,height=100');}});" +
            "var w=window.open('about:blank','_blank','width=200,height=200');" +
            "if(w){w.document.write('<scr'+'ipt>setTimeout(function(){try{window.opener.postMessage({shellifyOAuth:1},\"*\")}catch(e){}},300)<\\/scr'+'ipt>');w.document.close();}" +
            "</script></body></html>"
        val openerUrl = "data:text/html;base64," + Base64.encodeToString(opener.toByteArray(), Base64.NO_WRAP)
        return previewIntent(openerUrl)
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
