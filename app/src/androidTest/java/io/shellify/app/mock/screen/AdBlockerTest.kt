package io.shellify.app.mock.screen

import android.net.Uri
import android.webkit.WebResourceRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.adblock.AdBlocker
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdBlockerTest {

    private lateinit var blocker: AdBlocker

    @Before
    fun setUp() { blocker = AdBlocker() }

    @Test
    fun shouldBlock_mainFrameOnAdDomain_returnsNull() {
        assertNull(blocker.shouldBlock(request("https://doubleclick.net/", mainFrame = true)))
    }

    @Test
    fun shouldBlock_subResourceOnAdDomain_returnsEmptyResponse() {
        assertNotNull(blocker.shouldBlock(request("https://doubleclick.net/tracker.js", mainFrame = false)))
    }

    @Test
    fun shouldBlock_subResourceOnLegitDomain_returnsNull() {
        assertNull(blocker.shouldBlock(request("https://youtube.com/api/fetch", mainFrame = false)))
    }

    @Test
    fun shouldBlock_subResourceWithAdPath_returnsEmptyResponse() {
        assertNotNull(blocker.shouldBlock(request("https://cdn.example.com/ads/banner.png", mainFrame = false)))
    }

    @Test
    fun shouldBlock_mainFrameOnAdPath_returnsNull() {
        assertNull(blocker.shouldBlock(request("https://ads.example.com/landing", mainFrame = true)))
    }

    @Test
    fun shouldBlock_googleTagManager_subResource_isBlocked() {
        assertNotNull(blocker.shouldBlock(request("https://googletagmanager.com/gtm.js", mainFrame = false)))
    }

    @Test
    fun addCustomRules_blocksCustomDomainOnSubResource() {
        blocker.addCustomRules(listOf("||custom-spy.example.com^"))
        assertNotNull(blocker.shouldBlock(request("https://custom-spy.example.com/pixel.gif", mainFrame = false)))
    }

    private fun request(url: String, mainFrame: Boolean): WebResourceRequest = mockk<WebResourceRequest>().also {
        every { it.url } returns Uri.parse(url)
        every { it.isForMainFrame } returns mainFrame
    }
}
