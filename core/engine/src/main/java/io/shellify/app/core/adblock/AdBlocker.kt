package io.shellify.app.core.adblock

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

/**
 * Intercepts WebView requests and returns an empty response for blocked URLs.
 * Supports two independent rule sets: ad-block (always evaluated) and tracker blocking
 * (gated by the per-app trackerBlockingEnabled flag).
 * Inspired by AppForge's AdBlocker.
 */
class AdBlocker(val cache: AdBlockFilterCache = AdBlockFilterCache()) {

    private val emptyResponse = WebResourceResponse("text/plain", "utf-8", "".byteInputStream())

    fun shouldBlock(request: WebResourceRequest, trackerBlockingEnabled: Boolean = false): WebResourceResponse? {
        val url = request.url.toString()
        // Never block the main frame — only sub-resources (scripts, images, iframes, etc.)
        if (request.isForMainFrame) return null
        val blocked = cache.shouldBlock(url) ||
            (trackerBlockingEnabled && cache.shouldBlockTracker(url))
        return if (blocked) emptyResponse else null
    }

    fun addCustomRules(rules: List<String>) = cache.addCustomRules(rules)
}
