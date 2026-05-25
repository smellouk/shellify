package io.shellify.app.core.adblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdBlockFilterCacheTrackerTest {

    private lateinit var cache: AdBlockFilterCache

    @Before
    fun setUp() {
        cache = AdBlockFilterCache()
    }

    // Behavior 1: Known tracker domains loaded via loadTrackerRules are blocked by shouldBlockTracker
    @Test
    fun `shouldBlockTracker returns true for known tracker domain`() {
        cache.loadTrackerRules(sequenceOf("doubleclick.net", "google-analytics.com"))
        assertTrue(cache.shouldBlockTracker("https://www.doubleclick.net/foo"))
    }

    // Behavior 2: Non-tracker domains return false
    @Test
    fun `shouldBlockTracker returns false for domain not in tracker set`() {
        cache.loadTrackerRules(sequenceOf("doubleclick.net", "google-analytics.com"))
        assertFalse(cache.shouldBlockTracker("https://example.com/path"))
    }

    // Behavior 3: Comment lines and blank lines are skipped
    @Test
    fun `loadTrackerRules skips comment lines and blank lines`() {
        cache.loadTrackerRules(
            sequenceOf(
                "# EasyPrivacy tracker list",
                "doubleclick.net",
                "",
                "# Another comment",
                "google-analytics.com",
            )
        )
        assertTrue(cache.shouldBlockTracker("https://doubleclick.net/ad"))
        assertTrue(cache.shouldBlockTracker("https://google-analytics.com/collect"))
        // The comment line itself should not be in the set
        assertFalse(cache.shouldBlockTracker("https://# EasyPrivacy tracker list/"))
    }

    // Behavior 4: extractHost normalizes www prefix and lowercases (existing pattern match)
    @Test
    fun `shouldBlockTracker matches after www removal and lowercase normalization`() {
        cache.loadTrackerRules(sequenceOf("scorecardresearch.com"))
        // www prefix should be stripped from URL before lookup
        assertTrue(cache.shouldBlockTracker("https://www.scorecardresearch.com/beacon.js"))
        // Non-www variant also matches
        assertTrue(cache.shouldBlockTracker("https://scorecardresearch.com/beacon.js"))
    }

    // Behavior 5: shouldBlock (ad-block) continues to work correctly after tracker rules loaded (no regression)
    @Test
    fun `shouldBlock ad-block rules continue to work after loadTrackerRules called`() {
        cache.loadTrackerRules(sequenceOf("some-tracker.net"))
        // Built-in ad domain should still be blocked
        assertTrue(cache.shouldBlock("https://doubleclick.net/ad"))
        // Built-in pattern should still be blocked
        assertTrue(cache.shouldBlock("https://example.com/ads/banner.png"))
        // Legitimate domain should not be blocked by ad rules
        assertFalse(cache.shouldBlock("https://github.com/user/repo"))
    }
}
