package dev.pwaforge.core.webview

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import dev.pwaforge.domain.model.UserAgentMode
import dev.pwaforge.domain.model.WebApp

/**
 * Configures a WebView instance from a WebApp's settings.
 * Inspired by AppForge's WebViewManager.
 */
object WebViewManager {

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView, app: WebApp) {
        WebView.setWebContentsDebuggingEnabled(false)
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            // Explicitly deny cross-origin file access
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            userAgentString = resolveUserAgent(this, app.uaMode)
        }
    }

    private fun resolveUserAgent(settings: WebSettings, mode: UserAgentMode): String? =
        when (mode) {
            UserAgentMode.DEFAULT -> null // keep system default
            else -> mode.uaString
        }
}
