package io.shellify.app.mock.screen

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shellify.app.core.adblock.AdBlockFilterCache
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdBlockFilterCacheTest {

    private lateinit var cache: AdBlockFilterCache

    @Before
    fun setUp() { cache = AdBlockFilterCache() }

    @Test fun shouldBlock_doubleclick_returnsTrue() = assertTrue(cache.shouldBlock("https://doubleclick.net/ad"))
    @Test fun shouldBlock_googleAdServices_returnsTrue() = assertTrue(cache.shouldBlock("https://googleadservices.com/conv"))
    @Test fun shouldBlock_criteo_returnsTrue() = assertTrue(cache.shouldBlock("https://criteo.com/events"))
    @Test fun shouldBlock_taboola_returnsTrue() = assertTrue(cache.shouldBlock("https://taboola.com/recs"))
    @Test fun shouldBlock_wwwPrefix_isStripped() = assertTrue(cache.shouldBlock("https://www.doubleclick.net/t"))
    @Test fun shouldBlock_adsPath_returnsTrue() = assertTrue(cache.shouldBlock("https://example.com/ads/banner.png"))
    @Test fun shouldBlock_adPath_returnsTrue() = assertTrue(cache.shouldBlock("https://cdn.example.com/ad/pixel.gif"))
    @Test fun shouldBlock_pageadSubstring_returnsTrue() = assertTrue(cache.shouldBlock("https://example.com/pagead/show"))
    @Test fun shouldBlock_vastPath_returnsTrue() = assertTrue(cache.shouldBlock("https://video.example.com/vast/ad.xml"))
    @Test fun shouldBlock_legitimateGoogle_returnsFalse() = assertFalse(cache.shouldBlock("https://google.com/search?q=test"))
    @Test fun shouldBlock_youtube_returnsFalse() = assertFalse(cache.shouldBlock("https://youtube.com/watch?v=abc"))
    @Test fun shouldBlock_github_returnsFalse() = assertFalse(cache.shouldBlock("https://github.com/shellify"))
    @Test fun shouldBlock_emptyUrl_returnsFalse() = assertFalse(cache.shouldBlock(""))
    @Test fun shouldBlock_malformedUrl_returnsFalse() = assertFalse(cache.shouldBlock("not-a-url"))

    @Test
    fun addCustomRules_domainRule_blocksAddedDomain() {
        cache.addCustomRules(listOf("||custom-tracker.example.com^"))
        assertTrue(cache.shouldBlock("https://custom-tracker.example.com/pixel"))
    }

    @Test
    fun addCustomRules_pathRule_blocksMatchingPath() {
        cache.addCustomRules(listOf("/custom-ads/"))
        assertTrue(cache.shouldBlock("https://cdn.example.com/custom-ads/banner.jpg"))
    }

    @Test
    fun addCustomRules_doesNotBlockUnrelatedDomain() {
        cache.addCustomRules(listOf("||only-this.example.com^"))
        assertFalse(cache.shouldBlock("https://other.example.com/page"))
    }
}
