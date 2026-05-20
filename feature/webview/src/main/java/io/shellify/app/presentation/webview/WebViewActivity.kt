package io.shellify.app.presentation.webview

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
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import io.shellify.app.presentation.theme.Dimens
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import io.shellify.app.presentation.webview.WebViewServiceProvider
import io.shellify.app.core.engine.BrowserEngine
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.domain.model.EngineType
import io.shellify.app.core.engine.GeckoViewEngine
import io.shellify.app.core.engine.SystemWebViewEngine
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.isLegacyHash
import io.shellify.app.core.security.showSystemLockPrompt
import io.shellify.app.core.security.verifyPassword
import kotlinx.coroutines.flow.first
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.core.translate.TranslateBridge
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.WebApp
import io.shellify.core.ui.R
import io.shellify.app.presentation.theme.ShellifyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class WebViewActivity : FragmentActivity() {

    companion object {
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_PREVIEW_URL = "preview_url"
        const val EXTRA_PREVIEW_NAME = "preview_name"

        @VisibleForTesting
        var engineFactory: (() -> BrowserEngine)? = null

        @VisibleForTesting
        var pageFinishedCallback: (() -> Unit)? = null

        /** Overrides the [WebApp] used in [onCreate]. Set in tests to inject a custom app. */
        @VisibleForTesting
        var webAppOverride: WebApp? = null

        fun launchIntent(context: android.content.Context, appId: Long): Intent =
            Intent(context, WebViewActivity::class.java)
                .putExtra(EXTRA_APP_ID, appId)
                .setData(android.net.Uri.parse("shellify://app/$appId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

        fun previewIntent(context: android.content.Context, url: String, name: String): Intent =
            Intent(context, WebViewActivity::class.java)
                .putExtra(EXTRA_PREVIEW_URL, url)
                .putExtra(EXTRA_PREVIEW_NAME, name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    private lateinit var engine: BrowserEngine
    private lateinit var progressBar: ProgressBar
    private lateinit var container: FrameLayout
    private lateinit var isolationManager: IsolationManager
    private var statusBarScrim: View? = null

    @VisibleForTesting
    var splashOverlay: View? = null
    private val currentAppFlow = MutableStateFlow<WebApp?>(null)
    private val errorFlow = MutableStateFlow<WebLoadError?>(null)
    private val isRetryingFlow = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val visitedUrls = mutableSetOf<String>()

    @VisibleForTesting
    fun navigateTo(url: String) = engine.loadUrl(url)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WebViewServiceProvider
        isolationManager = app.isolationManager

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1L)
        val previewUrl = intent.getStringExtra(EXTRA_PREVIEW_URL)

        val pwaApp: WebApp = webAppOverride ?: when {
            previewUrl != null -> WebApp(
                name = intent.getStringExtra(EXTRA_PREVIEW_NAME) ?: previewUrl,
                url = previewUrl,
            )
            appId != -1L -> runBlocking(Dispatchers.IO) { app.getWebAppById(appId) }
                ?: run { finish(); return }
            else -> { finish(); return }
        }
        currentAppFlow.value = pwaApp

        // Collect screenshot-protection flag reactively. DataStore emits the current value
        // immediately on first collection, so FLAG_SECURE is applied before any frame is drawn.
        // runBlocking on the main thread was removed — it deadlocked in test environments where
        // the DataStore flow never emitted while the main thread was blocked.
        scope.launch {
            app.passwordManager.screenshotProtection.collect { on ->
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        engine = engineFactory?.invoke() ?: when {
            pwaApp.engineType == EngineType.GECKOVIEW && app.geckoEngineManager.isInstalled() ->
                GeckoViewEngine(this, app.geckoEngineManager)

            else -> SystemWebViewEngine(app.adBlocker)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (engine.canGoBack()) engine.goBack() else finish()
            }
        })

        container = FrameLayout(this)
        container.setBackgroundColor(
            pwaApp.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                ?: Color.BLACK
        )

        val engineView =
            engine.createView(this, pwaApp, buildCallback({ currentAppFlow.value }, container))

        if (engine is SystemWebViewEngine) {
            val wv = (engine as SystemWebViewEngine).getWebView()
            if (wv != null) isolationManager.attachProfile(wv, pwaApp.isolationId)
        }

        container.addView(
            engineView,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )

        // Apply status-bar and nav-bar insets to the container so WebView content never renders
        // behind system bars. Works on all API levels including Android 15 where
        // setDecorFitsSystemWindows(true) is ignored. Insets are 0 when bars are hidden.
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, statusBars.top, 0, navBars.bottom)
            insets
        }

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
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                barHeightPx,
                Gravity.TOP
            ),
        )

        setContentView(container)
        applyWindowMode(pwaApp)
        applyStatusBarColor(pwaApp.themeColor)
        applyTaskDescription(pwaApp)

        authenticate(app, pwaApp)
    }

    private fun authenticate(app: WebViewServiceProvider, pwaApp: WebApp) {
        when (pwaApp.lockType) {
            LockType.NONE -> startLoading(pwaApp)
            LockType.PASSWORD -> {
                val hash = runBlocking { app.passwordManager.passwordHash.first() }
                if (hash != null) showPasswordDialog(app, pwaApp) else startLoading(pwaApp)
            }

            LockType.SYSTEM -> showSystemLockWithWipe(app, pwaApp)
        }
    }

    private fun showPasswordDialog(app: WebViewServiceProvider, pwaApp: WebApp) {
        val pwaAccentColor = pwaApp.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
        // Load persisted count so the wipe limit survives process death between attempts.
        val persistedAttempts = runBlocking { app.passwordManager.getFailedAttempts(pwaApp.id) }
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val hash by app.passwordManager.passwordHash.collectAsState(initial = null)
                val wipe by app.passwordManager.wipeOnFailedAttempts.collectAsState(initial = false)
                var failedAttempts by remember { mutableStateOf(persistedAttempts) }
                val maxAttempts = 3
                val remaining = maxAttempts - failedAttempts

                val err2 = stringResource(R.string.webview_password_error_2_attempts)
                val err1 = stringResource(R.string.webview_password_error_1_attempt)
                val errBasic = stringResource(R.string.webview_password_error)
                val errorMessage = when {
                    failedAttempts == 0 -> null
                    wipe && remaining == 2 -> err2
                    wipe && remaining == 1 -> err1
                    !wipe -> errBasic
                    else -> null
                }

                ShellifyTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    accentColor = pwaAccentColor,
                    controlStatusBar = false,
                ) {
                    WebViewPasswordDialog(
                        appName = pwaApp.name,
                        errorMessage = errorMessage,
                        onConfirm = { input ->
                            val h = hash
                            if (h != null && verifyPassword(input, h)) {
                                scope.launch(Dispatchers.IO) {
                                    app.passwordManager.clearFailedAttempts(pwaApp.id)
                                    // Transparently upgrade a legacy SHA-256 hash to PBKDF2.
                                    if (isLegacyHash(h)) app.passwordManager.setPassword(input)
                                }
                                container.removeView(this@apply)
                                startLoading(pwaApp)
                            } else {
                                failedAttempts++
                                scope.launch(Dispatchers.IO) {
                                    app.passwordManager.recordFailedAttempt(pwaApp.id)
                                }
                                if (wipe && failedAttempts >= maxAttempts) {
                                    wipeAndUnlock(app, pwaApp)
                                }
                            }
                        },
                        onDismiss = { finish() },
                    )
                }
            }
        }
        container.addView(
            overlay,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
    }

    private fun showSystemLockWithWipe(app: WebViewServiceProvider, pwaApp: WebApp) {
        val maxAttempts = 3
        // Load persisted count so the wipe limit survives process death between attempts.
        var failedAttempts = runBlocking { app.passwordManager.getFailedAttempts(pwaApp.id) }

        fun prompt() {
            showSystemLockPrompt(
                activity = this,
                title = getString(R.string.webview_lock_prompt_title, pwaApp.name),
                onSuccess = {
                    scope.launch(Dispatchers.IO) { app.passwordManager.clearFailedAttempts(pwaApp.id) }
                    startLoading(pwaApp)
                },
                onFailed = {
                    failedAttempts++
                    scope.launch(Dispatchers.IO) { app.passwordManager.recordFailedAttempt(pwaApp.id) }
                    val wipe = runBlocking { app.passwordManager.wipeOnFailedAttempts.first() }
                    if (wipe && failedAttempts >= maxAttempts) {
                        wipeAndUnlock(app, pwaApp)
                    } else {
                        prompt()
                    }
                },
            )
        }
        prompt()
    }

    private fun wipeAndUnlock(app: WebViewServiceProvider, pwaApp: WebApp) {
        scope.launch(Dispatchers.IO) {
            app.isolationManager.clearData(pwaApp.isolationId)
            app.saveWebApp(pwaApp.copy(lockType = LockType.NONE))
            finish()
        }
    }

    private fun startLoading(pwaApp: WebApp) {
        scope.launch {
            showSplash(pwaApp)
            if (engine is SystemWebViewEngine) {
                isolationManager.restoreSession(pwaApp.isolationId)
            }
            engine.loadUrl(pwaApp.url)
            addErrorOverlay()
            addControlsOverlay()
        }
    }

    private fun addErrorOverlay() {
        val app = application as WebViewServiceProvider
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val currentApp by currentAppFlow.collectAsState()
                val accentColor = currentApp?.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                val error by errorFlow.collectAsState()
                val isRetrying by isRetryingFlow.collectAsState()
                if (error == null) return@setContent
                ShellifyTheme(themeMode = themeMode, dynamicColor = dynamicColor, accentColor = accentColor, controlStatusBar = false) {
                    WebViewErrorScreen(
                        error = error!!,
                        isRetrying = isRetrying,
                        onRetry = {
                            isRetryingFlow.value = true
                            engine.reload()
                        },
                    )
                }
            }
        }
        container.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
    }

    private fun addControlsOverlay() {
        val app = application as WebViewServiceProvider
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val pwaAppState by currentAppFlow.collectAsState()
                val pwaApp = pwaAppState ?: return@setContent
                if (!pwaApp.showControlCenter) return@setContent
                val passwordHash by app.passwordManager.passwordHash.collectAsState(initial = null)

                ShellifyTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    accentColor = pwaApp.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() },
                    controlStatusBar = false,
                ) {
                    WebViewControlCenter(
                        pwaApp = pwaApp,
                        hasGlobalPassword = passwordHash != null,
                        onAdBlockChanged = { on ->
                            val updated = pwaApp.copy(adBlockEnabled = on)
                            currentAppFlow.value = updated
                            scope.launch(Dispatchers.IO) { app.saveWebApp(updated) }
                            engine.getCurrentUrl()?.let { engine.loadUrl(it) }
                        },
                        onTranslateChanged = { on ->
                            val updated = pwaApp.copy(translateEnabled = on)
                            currentAppFlow.value = updated
                            scope.launch(Dispatchers.IO) { app.saveWebApp(updated) }
                            if (on) {
                                engine.evaluateJavascript("window.__shellifyTranslateLoaded = false;", null)
                                val script = TranslateBridge.buildScript(
                                    targetLang = updated.translateTarget.code,
                                    autoTranslate = true,
                                )
                                engine.evaluateJavascript(script, null)
                            } else {
                                engine.getCurrentUrl()?.let { engine.loadUrl(it) }
                            }
                        },
                        onFullscreenChanged = { on ->
                            val updated = pwaApp.copy(isFullscreen = on)
                            currentAppFlow.value = updated
                            scope.launch(Dispatchers.IO) { app.saveWebApp(updated) }
                            applyWindowMode(updated)
                        },
                        onLockChanged = { on ->
                            val updated = pwaApp.copy(lockType = if (on) LockType.PASSWORD else LockType.NONE)
                            currentAppFlow.value = updated
                            scope.launch(Dispatchers.IO) { app.saveWebApp(updated) }
                        },
                        onClearData = {
                            scope.launch(Dispatchers.IO) { app.isolationManager.clearData(pwaApp.isolationId) }
                            engine.loadUrl(pwaApp.url)
                        },
                    )
                }
            }
        }
        container.addView(
            overlay,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
    }

    private fun buildCallback(
        currentApp: () -> WebApp?,
        container: FrameLayout
    ): BrowserEngineCallback =
        object : BrowserEngineCallback {
            private var loadFailed = false

            override fun onPageStarted(url: String?) {
                if (url?.startsWith("about:") == true) return
                loadFailed = false
                url?.let { visitedUrls += it }
            }

            override fun onPageFinished(url: String?) {
                isRetryingFlow.value = false
                if (!loadFailed) {
                    errorFlow.value = null
                    engine.getView()?.visibility = View.VISIBLE
                }
                url?.let { visitedUrls += it }
                pageFinishedCallback?.invoke()
                hideSplash()
                val app = currentApp() ?: return
                if (app.translateEnabled) {
                    engine.evaluateJavascript("window.__shellifyTranslateLoaded = false;", null)
                    val script = TranslateBridge.buildScript(
                        targetLang = app.translateTarget.code,
                        autoTranslate = true,
                    )
                    engine.evaluateJavascript(script, null)
                }
            }

            override fun onProgressChanged(progress: Int) {
                progressBar.progress = progress
                progressBar.visibility = if (progress < 100) View.VISIBLE else View.GONE
            }

            override fun onTitleChanged(title: String?) {}
            override fun onIconReceived(icon: Bitmap?) {}
            override fun onError(errorCode: Int, description: String) {
                loadFailed = true
                engine.getView()?.visibility = View.INVISIBLE
                hideSplash()
                errorFlow.value = WebLoadError.from(errorCode, description)
            }
            override fun onSslError(error: String) {
                loadFailed = true
                engine.getView()?.visibility = View.INVISIBLE
                hideSplash()
                errorFlow.value = WebLoadError.SslError
            }

            override fun onExternalLink(url: String) {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }

            private var customView: View? = null

            override fun onShowCustomView(view: View?, callback: Any?) {
                view ?: return
                customView = view
                engine.getView()?.visibility = View.GONE
                container.addView(view)
                applyWindowMode((currentApp() ?: return).copy(isFullscreen = true))
            }

            override fun onHideCustomView() {
                (customView?.parent as? FrameLayout)?.removeView(customView)
                customView = null
                engine.getView()?.visibility = View.VISIBLE
                currentApp()?.let { applyWindowMode(it) }
            }

            override fun onDownloadStart(
                url: String, userAgent: String, contentDisposition: String,
                mimeType: String, contentLength: Long,
            ) {
            }
        }

    private fun showSplash(pwaApp: WebApp) {
        val app = application as WebViewServiceProvider
        val rawColor = pwaApp.themeColor
            ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            ?: Color.BLACK
        val bgColor = androidx.compose.ui.graphics.Color(rawColor)
        val isLight = (0.299 * Color.red(rawColor) + 0.587 * Color.green(rawColor) + 0.114 * Color.blue(rawColor)) / 255 > 0.5
        val contentColor = if (isLight) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White

        val view = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val iconBitmap by produceState<Bitmap?>(null) {
                    value = withContext(Dispatchers.IO) {
                        pwaApp.iconPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
                    }
                }
                ShellifyTheme(themeMode = themeMode, dynamicColor = dynamicColor, controlStatusBar = false) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(bgColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXl),
                        ) {
                            iconBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(Dimens.sizeEmptyIconLg)
                                        .clip(RoundedCornerShape(Dimens.cornerLg)),
                                )
                            }
                            Text(
                                text = pwaApp.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
        }
        splashOverlay = view
        container.addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    private fun hideSplash() {
        val splash = splashOverlay ?: return
        splashOverlay = null
        splash.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction { container.removeView(splash) }
            .start()
    }

    private fun applyWindowMode(app: WebApp) {
        val fullscreen = app.isFullscreen
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // Always edge-to-edge; the container insets listener handles padding.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (fullscreen) {
            val showStatus = app.fullscreenShowStatusBar
            val showNav = app.fullscreenShowNavBar
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            when {
                showStatus && showNav -> controller.show(WindowInsetsCompat.Type.systemBars())
                showStatus -> {
                    controller.hide(WindowInsetsCompat.Type.navigationBars()); controller.show(
                        WindowInsetsCompat.Type.statusBars()
                    )
                }

                showNav -> {
                    controller.hide(WindowInsetsCompat.Type.statusBars()); controller.show(
                        WindowInsetsCompat.Type.navigationBars()
                    )
                }

                else -> controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyStatusBarColor(themeColor: String?) {
        val color = themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: return
        val isLight =
            (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255 > 0.5
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isStatusBarContrastEnforced =
            false
        window.statusBarColor = color
        // Remove any scrim left over from a previous approach.
        statusBarScrim?.let { container.removeView(it) }
        statusBarScrim = null
    }

    @Suppress("DEPRECATION")
    private fun applyTaskDescription(app: WebApp) {
        val icon: Bitmap? =
            app.iconPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        val rawColor = app.themeColor
            ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            ?: Color.WHITE
        // colorPrimary must be fully opaque — Android silently ignores the label when it is 0.
        val opaqueColor = Color.argb(255, Color.red(rawColor), Color.green(rawColor), Color.blue(rawColor))
        setTaskDescription(ActivityManager.TaskDescription(app.name, icon, opaqueColor))
    }

    override fun onResume() {
        super.onResume()
        currentAppFlow.value?.let { applyTaskDescription(it) }
    }

    override fun onDestroy() {
        currentAppFlow.value?.let { app ->
            engine.getCurrentUrl()?.let { visitedUrls += it }
            if (engine is SystemWebViewEngine) {
                isolationManager.onSessionEnd(app.isolationId, visitedUrls)
            }
        }
        engine.destroy()
        scope.cancel()
        super.onDestroy()
    }
}
