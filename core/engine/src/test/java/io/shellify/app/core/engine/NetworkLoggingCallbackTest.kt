package io.shellify.app.core.engine

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Unit tests for the engine capture layer of Phase 22 (network request logging).
 *
 * GeckoSession and WebView require native libraries unavailable in the JVM unit-test
 * environment. We therefore test the extracted top-level dispatch functions directly,
 * following the same pattern used in GeckoViewEngineNotificationTest for notifications.
 *
 * Covered behaviours:
 *  1. SystemWebViewEngine — sub-resource request fires onRequestIntercepted (blocked)
 *  2. SystemWebViewEngine — sub-resource request fires onRequestIntercepted (allowed)
 *  3. SystemWebViewEngine — main-frame request does NOT fire onRequestIntercepted
 *  4. GeckoViewEngine ContentBlocking — onContentBlocked with isBlocking=true
 *  5. GeckoViewEngine ContentBlocking — onContentBlocked with isBlocking=false (tracked, not blocked)
 *  6. GeckoViewEngine ContentBlocking — onContentLoaded fires with blocked=false
 */
class NetworkLoggingCallbackTest {

    // ──────────────────────────────────────────────────────────────────────────────
    // SystemWebViewEngine
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun `dispatchInterceptedRequest for sub-resource blocked fires onRequestIntercepted blocked=true`() {
        val cb = mockk<BrowserEngineCallback>(relaxed = true)

        dispatchInterceptedRequest(
            url = "https://tracker.example.com/pixel.gif",
            isForMainFrame = false,
            blocked = true,
            cb = cb,
        )

        verify(exactly = 1) {
            cb.onRequestIntercepted("https://tracker.example.com/pixel.gif", blocked = true)
        }
    }

    @Test
    fun `dispatchInterceptedRequest for sub-resource allowed fires onRequestIntercepted blocked=false`() {
        val cb = mockk<BrowserEngineCallback>(relaxed = true)

        dispatchInterceptedRequest(
            url = "https://cdn.example.com/style.css",
            isForMainFrame = false,
            blocked = false,
            cb = cb,
        )

        verify(exactly = 1) {
            cb.onRequestIntercepted("https://cdn.example.com/style.css", blocked = false)
        }
    }

    @Test
    fun `dispatchInterceptedRequest for main-frame does NOT fire onRequestIntercepted`() {
        val cb = mockk<BrowserEngineCallback>(relaxed = true)

        dispatchInterceptedRequest(
            url = "https://example.com/",
            isForMainFrame = true,
            blocked = false,
            cb = cb,
        )

        verify(exactly = 0) { cb.onRequestIntercepted(any(), any()) }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // GeckoViewEngine — ContentBlocking.Delegate
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun `dispatchContentBlocked with isBlocking=true fires onRequestIntercepted blocked=true`() {
        val cb = mockk<BrowserEngineCallback>(relaxed = true)

        dispatchContentBlocked(
            uri = "https://tracker.example.com/track.js",
            isBlocking = true,
            cb = cb,
        )

        verify(exactly = 1) {
            cb.onRequestIntercepted("https://tracker.example.com/track.js", blocked = true)
        }
    }

    @Test
    fun `dispatchContentBlocked with isBlocking=false fires onRequestIntercepted blocked=false`() {
        val cb = mockk<BrowserEngineCallback>(relaxed = true)

        // isBlocking=false means resource was tracked/observed but NOT actually blocked.
        // Misclassifying this as blocked=true would be incorrect per RESEARCH.md Pattern 3.
        dispatchContentBlocked(
            uri = "https://analytics.example.com/beacon",
            isBlocking = false,
            cb = cb,
        )

        verify(exactly = 1) {
            cb.onRequestIntercepted("https://analytics.example.com/beacon", blocked = false)
        }
    }

    @Test
    fun `dispatchContentLoaded fires onRequestIntercepted blocked=false`() {
        val cb = mockk<BrowserEngineCallback>(relaxed = true)

        dispatchContentLoaded(
            uri = "https://cdn.example.com/image.png",
            cb = cb,
        )

        verify(exactly = 1) {
            cb.onRequestIntercepted("https://cdn.example.com/image.png", blocked = false)
        }
    }
}
