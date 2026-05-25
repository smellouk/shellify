package io.shellify.app.core.adblock

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Compiled ad-block rules split into exact-host sets and substring patterns.
 * Also carries a parallel tracker host set loaded from the bundled EasyPrivacy asset.
 * Inspired by AppForge's AdBlockFilterCache.
 *
 * Collections are CopyOnWrite variants so concurrent reads from WebViewClient.shouldInterceptRequest
 * (background thread) and writes from loadBuiltInRules / loadTrackerRules / addCustomRules
 * (main thread or IO thread) do not race (WR-04).
 */
class AdBlockFilterCache {

    private val blockedHosts = CopyOnWriteArraySet<String>()
    private val blockedPatterns = CopyOnWriteArrayList<String>()

    // Parallel tracker rule set — loaded from assets/easyprivacy_domains.txt.
    // Evaluated independently from ad-block rules; gated by trackerBlockingEnabled flag in AdBlocker.
    private val blockedTrackerHosts = CopyOnWriteArraySet<String>()

    fun shouldBlockTracker(url: String): Boolean {
        val host = extractHost(url) ?: return false
        return host in blockedTrackerHosts
    }

    fun loadTrackerRules(lines: Sequence<String>) {
        lines
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { host -> blockedTrackerHosts += host.lowercase().removePrefix("www.") }
    }

    init {
        loadBuiltInRules()
    }

    fun shouldBlock(url: String): Boolean {
        val host = extractHost(url) ?: return false
        if (host in blockedHosts) return true
        return blockedPatterns.any { url.contains(it, ignoreCase = true) }
    }

    private fun extractHost(url: String): String? = runCatching {
        val start = url.indexOf("://").takeIf { it >= 0 }?.plus(3) ?: return@runCatching null
        val end = url.indexOf('/', start).takeIf { it > 0 } ?: url.length
        url.substring(start, end).lowercase().removePrefix("www.")
    }.getOrNull()

    private fun loadBuiltInRules() {
        // Common ad/tracker domains
        blockedHosts += setOf(
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "adnxs.com",
            "advertising.com",
            "ads.yahoo.com",
            "pagead2.googlesyndication.com",
            "adservice.google.com",
            "amazon-adsystem.com",
            "adsymptotic.com",
            "moatads.com",
            "criteo.com",
            "criteo.net",
            "outbrain.com",
            "taboola.com",
            "revcontent.com",
            "pubmatic.com",
            "openx.net",
            "rubiconproject.com",
            "appnexus.com",
            "smartadserver.com",
            "adform.net",
            "quantserve.com",
            "scorecardresearch.com",
            "omtrdc.net",
            "adobedtm.com",
            "demdex.net",
            "adsafeprotected.com",
            "googletagservices.com",
            "googletagmanager.com",
            "yandex.ru",
            "mc.yandex.ru",
            "hotjar.com",
            "mixpanel.com",
            "segment.com",
            "segment.io",
            "fullstory.com",
            "mouseflow.com",
        )

        // Substring patterns for ad paths
        blockedPatterns += listOf(
            "/ads/",
            "/ad/",
            "/adserver/",
            "/banners/",
            "/pop.js",
            "/popup.js",
            "pagead",
            "adsbygoogle",
            "prebid.js",
            "/vast/",
            "/vpaid/",
        )
    }

    fun addCustomRules(rules: List<String>) {
        rules.forEach { rule ->
            val cleaned = rule.trim().removePrefix("||").removeSuffix("^")
            if (cleaned.contains('/')) blockedPatterns += cleaned
            else blockedHosts += cleaned.lowercase().removePrefix("www.")
        }
    }
}
