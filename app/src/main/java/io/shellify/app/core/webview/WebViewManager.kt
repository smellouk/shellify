package io.shellify.app.core.webview

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.model.WebApp

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
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            userAgentString = resolveUserAgent(this, app.uaMode)
        }
        // Cookies must be explicitly enabled; third-party cookies needed for OAuth / SSO flows
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    private fun resolveUserAgent(settings: WebSettings, mode: UserAgentMode): String? =
        when (mode) {
            UserAgentMode.DEFAULT -> null // keep system default
            else -> mode.uaString
        }
}
