package io.shellify.app.core.adblock

import android.net.Uri
import android.webkit.WebResourceRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AdBlockerTest {

    private lateinit var adBlocker: AdBlocker

    @Before
    fun setUp() {
        adBlocker = AdBlocker()
    }

    private fun makeRequest(url: String, isMainFrame: Boolean = false): WebResourceRequest {
        val uri = mockk<Uri>()
        every { uri.toString() } returns url
        return mockk<WebResourceRequest>().also {
            every { it.isForMainFrame } returns isMainFrame
            every { it.url } returns uri
        }
    }

    @Test
    fun `main frame requests are never blocked`() {
        val request = makeRequest("https://doubleclick.net/ad.js", isMainFrame = true)
        assertNull(adBlocker.shouldBlock(request))
    }

    @Test
    fun `sub-resource to known ad domain is blocked`() {
        val request = makeRequest("https://doubleclick.net/ad.js", isMainFrame = false)
        assertNotNull(adBlocker.shouldBlock(request))
    }

    @Test
    fun `sub-resource to allowed domain is not blocked`() {
        val request = makeRequest("https://example.com/script.js", isMainFrame = false)
        assertNull(adBlocker.shouldBlock(request))
    }

    @Test
    fun `sub-resource matching path pattern is blocked`() {
        val request = makeRequest("https://cdn.example.com/ads/banner.js", isMainFrame = false)
        assertNotNull(adBlocker.shouldBlock(request))
    }

    @Test
    fun `addCustomRules propagates to the cache`() {
        adBlocker.addCustomRules(listOf("||my-custom-ad.com^"))
        val request = makeRequest("https://my-custom-ad.com/tracker.gif", isMainFrame = false)
        assertNotNull(adBlocker.shouldBlock(request))
    }

    @Test
    fun `main frame with ad-like url is still not blocked`() {
        val request = makeRequest("https://ads.example.com/page", isMainFrame = true)
        assertNull(adBlocker.shouldBlock(request))
    }

    // ── trackerBlockingEnabled orchestration ──────────────────────────────────

    // TB-1: tracker URL is NOT blocked when flag is false (opt-out by default)
    @Test
    fun `tracker URL is not blocked when trackerBlockingEnabled is false`() {
        adBlocker.cache.loadTrackerRules(sequenceOf("google-analytics.com"))
        val request = makeRequest("https://google-analytics.com/collect", isMainFrame = false)
        assertNull(adBlocker.shouldBlock(request, trackerBlockingEnabled = false))
    }

    // TB-2: tracker URL IS blocked when flag is true
    @Test
    fun `tracker URL is blocked when trackerBlockingEnabled is true`() {
        adBlocker.cache.loadTrackerRules(sequenceOf("google-analytics.com"))
        val request = makeRequest("https://google-analytics.com/collect", isMainFrame = false)
        assertNotNull(adBlocker.shouldBlock(request, trackerBlockingEnabled = true))
    }

    // TB-3: main-frame requests are never blocked even when trackerBlockingEnabled is true
    @Test
    fun `main frame tracker URL is never blocked even when trackerBlockingEnabled is true`() {
        adBlocker.cache.loadTrackerRules(sequenceOf("spy-tracker.io"))
        val request = makeRequest("https://spy-tracker.io/track", isMainFrame = true)
        assertNull(adBlocker.shouldBlock(request, trackerBlockingEnabled = true))
    }

    // TB-4: ad-block (non-tracker) URL is blocked regardless of trackerBlockingEnabled
    @Test
    fun `ad-block URL is blocked even when trackerBlockingEnabled is false`() {
        // doubleclick.net is in the built-in ad-block host set, not the tracker set
        val request = makeRequest("https://doubleclick.net/ad.gif", isMainFrame = false)
        assertNotNull(adBlocker.shouldBlock(request, trackerBlockingEnabled = false))
    }

    // TB-5: URL not in any list returns null (not blocked)
    @Test
    fun `URL absent from all rule sets returns null`() {
        val request = makeRequest("https://example.com/api/data.json", isMainFrame = false)
        assertNull(adBlocker.shouldBlock(request, trackerBlockingEnabled = true))
    }
}
