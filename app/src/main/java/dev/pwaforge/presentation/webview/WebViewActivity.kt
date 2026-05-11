package dev.pwaforge.presentation.webview

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class WebViewActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_ID = "app_id"

        /** Creates an intent that opens this app in its own recents task. */
        fun launchIntent(context: android.content.Context, appId: Long): Intent =
            Intent(context, WebViewActivity::class.java)
                .putExtra(EXTRA_APP_ID, appId)
                // Unique data URI = unique document task per app
                .setData(android.net.Uri.parse("pwaforge://app/$appId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var isolationManager: IsolationManager
    private lateinit var adBlocker: AdBlocker
    private var currentApp: WebApp? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val visitedUrls = mutableSetOf<String>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PWAForgeApplication
        isolationManager = app.isolationManager
        adBlocker = app.adBlocker

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1L)
        if (appId == -1L) { finish(); return }

        // Load synchronously so we have the isolationId before creating the WebView.
        // WebView Profiles (API 33+) must be assigned before the view is attached to a window.
        val pwaApp = runBlocking(Dispatchers.IO) { app.webAppRepository.getById(appId) }
            ?: run { finish(); return }
        currentApp = pwaApp

        val container = FrameLayout(this)
        container.setBackgroundColor(Color.BLACK)
        webView = WebView(this)

        // Must happen BEFORE addView / setContentView (API 33+ requirement)
        isolationManager.attachProfile(webView, pwaApp.isolationId)

        container.addView(webView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        // Loading progress bar pinned to the top of the screen
        val barHeightPx = (3 * resources.displayMetrics.density).toInt().coerceAtLeast(2)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            isIndeterminate = false
            val tint = pwaApp.themeColor
                ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                ?: getColor(android.R.color.holo_blue_bright)
            progressTintList = ColorStateList.valueOf(tint)
            visibility = View.VISIBLE
        }
        container.addView(
            progressBar,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, barHeightPx, Gravity.TOP),
        )

        setContentView(container)

        setupWebView(pwaApp)
        applyWindowMode(pwaApp)
        if (!pwaApp.isFullscreen) applyStatusBarColor(pwaApp.themeColor)
        applyTaskDescription(pwaApp)

        scope.launch {
            // Restore cookies BEFORE loading — must be awaited (API < 33)
            isolationManager.restoreSession(pwaApp.isolationId)
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
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                // Track every domain visited so we can save all cookies on session end
                visitedUrls += url
            }

            override fun onPageFinished(view: WebView, url: String) {
                visitedUrls += url
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

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView = view
                webView.visibility = View.GONE
                (webView.parent as? FrameLayout)?.addView(view)
                applyWindowMode(app.copy(isFullscreen = true))
            }

            override fun onHideCustomView() {
                (customView?.parent as? FrameLayout)?.removeView(customView)
                customView = null
                webView.visibility = View.VISIBLE
                applyWindowMode(app)
            }
        }
    }

    private fun applyWindowMode(app: WebApp) {
        val fullscreen = app.isFullscreen
        WindowCompat.setDecorFitsSystemWindows(window, !fullscreen)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Selectively restore bars based on user preferences
            val showStatus = app.fullscreenShowStatusBar
            val showNav = app.fullscreenShowNavBar
            if (showStatus && showNav) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else if (showStatus) {
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else if (showNav) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun applyStatusBarColor(themeColor: String?) {
        val color = themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            ?: return
        window.statusBarColor = color
        val isLight = run {
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.5
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
    }

    @Suppress("DEPRECATION")
    private fun applyTaskDescription(app: WebApp) {
        val iconBitmap: Bitmap? = app.iconPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        setTaskDescription(ActivityManager.TaskDescription(app.name, iconBitmap))
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        currentApp?.let { app ->
            // Include the current URL in case onPageStarted wasn't called for it
            webView.url?.let { visitedUrls += it }
            isolationManager.onSessionEnd(app.isolationId, visitedUrls)
        }
        webView.destroy()
        scope.cancel()
        super.onDestroy()
    }
}
