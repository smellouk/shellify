package dev.pwaforge.presentation.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.pwaforge.PWAForgeApplication
import dev.pwaforge.core.adblock.AdBlocker
import dev.pwaforge.core.isolation.IsolationManager
import dev.pwaforge.core.translate.TranslateBridge
import dev.pwaforge.core.webview.WebViewManager
import dev.pwaforge.domain.model.WebApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WebViewActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_ID = "app_id"
    }

    private lateinit var webView: WebView
    private lateinit var isolationManager: IsolationManager
    private lateinit var adBlocker: AdBlocker
    private var currentApp: WebApp? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PWAForgeApplication
        isolationManager = app.isolationManager
        adBlocker = app.adBlocker

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1L)
        if (appId == -1L) { finish(); return }

        // Build layout programmatically — no XML needed
        val container = FrameLayout(this)
        container.setBackgroundColor(Color.BLACK)
        webView = WebView(this)
        container.addView(webView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        setContentView(container)

        scope.launch {
            val pwaApp = withContext(Dispatchers.IO) { app.webAppRepository.getById(appId) }
                ?: run { finish(); return@launch }
            currentApp = pwaApp
            setupWebView(pwaApp)
            applyWindowMode(pwaApp.isFullscreen)
            // Apply isolation BEFORE loading the URL
            isolationManager.applyTo(webView, pwaApp.isolationId, pwaApp.url)
            webView.loadUrl(pwaApp.url)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(app: WebApp) {
        WebViewManager.configure(webView, app)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (app.adBlockEnabled) adBlocker.shouldBlock(request)?.let { return it }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                // Open non-http schemes in external apps
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (app.translateEnabled) {
                    val script = TranslateBridge.buildScript(
                        targetLang = app.translateTarget.code,
                        showButton = app.showTranslateButton,
                        autoTranslate = app.autoTranslateOnLoad,
                    )
                    view.evaluateJavascript(script, null)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            private var customView: View? = null

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView = view
                webView.visibility = View.GONE
                (webView.parent as? FrameLayout)?.addView(view)
                applyWindowMode(fullscreen = true)
            }

            override fun onHideCustomView() {
                (customView?.parent as? FrameLayout)?.removeView(customView)
                customView = null
                webView.visibility = View.VISIBLE
                applyWindowMode(fullscreen = app.isFullscreen)
            }
        }
    }

    private fun applyWindowMode(fullscreen: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !fullscreen)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        currentApp?.let { app ->
            isolationManager.onSessionEnd(app.isolationId, webView.url ?: app.url)
        }
        webView.destroy()
        scope.cancel()
        super.onDestroy()
    }
}
