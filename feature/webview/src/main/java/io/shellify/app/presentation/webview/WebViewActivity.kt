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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import io.shellify.app.core.engine.BrowserEngine
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.core.engine.GeckoViewEngine
import io.shellify.app.core.engine.SystemWebViewEngine
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.isLegacyHash
import io.shellify.app.core.security.showSystemLockPrompt
import io.shellify.app.core.security.verifyPassword
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.core.translate.TranslateBridge
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.theme.Dimens
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.core.ui.R
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
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
    private lateinit var viewModel: WebViewViewModel
    private var statusBarScrim: View? = null

    @VisibleForTesting
    var splashOverlay: View? = null

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

        lifecycleScope.launch {
            app.passwordManager.screenshotProtection.collect { on ->
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        viewModel = ViewModelProvider(this, WebViewViewModel.Factory(pwaApp, app))[WebViewViewModel::class.java]

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

        val engineView = engine.createView(this, pwaApp, buildCallback())

        if (engine is SystemWebViewEngine) {
            val wv = (engine as SystemWebViewEngine).getWebView()
            if (wv != null) isolationManager.attachProfile(wv, pwaApp.isolationId)
        }

        container.addView(
            engineView,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )

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

        observeState(app, pwaApp)
        collectCommands()
    }

    private fun observeState(app: WebViewServiceProvider, pwaApp: WebApp) {
        lifecycleScope.launch {
            var previousAuthState: AuthState? = null
            viewModel.uiState.collect { state ->
                val authState = state.authState
                if (authState != previousAuthState) {
                    previousAuthState = authState
                    handleAuthState(app, pwaApp, authState)
                }
            }
        }
        addErrorOverlay(app)
        addControlsOverlay(app)
    }

    private fun handleAuthState(app: WebViewServiceProvider, pwaApp: WebApp, authState: AuthState) {
        when (authState) {
            AuthState.Loading -> Unit
            AuthState.Authenticated -> startLoading(pwaApp)
            is AuthState.PasswordRequired -> showPasswordDialog(app, pwaApp, authState)
            AuthState.SystemLockRequired -> showSystemLockWithWipe(app, pwaApp)
        }
    }

    private fun collectCommands() {
        lifecycleScope.launch {
            viewModel.commands.collect { command ->
                when (command) {
                    is WebViewCommand.LoadUrl -> engine.loadUrl(command.url)
                    WebViewCommand.Reload -> engine.reload()
                    WebViewCommand.Finish -> finish()
                }
            }
        }
    }

    private fun showPasswordDialog(app: WebViewServiceProvider, pwaApp: WebApp, initial: AuthState.PasswordRequired) {
        val pwaAccentColor = pwaApp.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val authState by viewModel.uiState.collectAsState()
                val state = authState.authState
                if (state !is AuthState.PasswordRequired) return@setContent

                val maxAttempts = 3
                val remaining = maxAttempts - state.failedAttempts
                val err2 = stringResource(R.string.webview_password_error_2_attempts)
                val err1 = stringResource(R.string.webview_password_error_1_attempt)
                val errBasic = stringResource(R.string.webview_password_error)
                val errorMessage = when {
                    state.failedAttempts == 0 -> null
                    state.wipeEnabled && remaining == 2 -> err2
                    state.wipeEnabled && remaining == 1 -> err1
                    !state.wipeEnabled -> errBasic
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
                            if (verifyPassword(input, state.hash)) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    if (isLegacyHash(state.hash)) app.passwordManager.setPassword(input)
                                }
                                container.removeView(this@apply)
                                viewModel.onPasswordVerified()
                            } else {
                                viewModel.recordFailedAttempt()
                                val newState = viewModel.uiState.value.authState
                                if (newState is AuthState.PasswordRequired &&
                                    newState.wipeEnabled && newState.failedAttempts >= maxAttempts
                                ) {
                                    viewModel.onWipeAndUnlock()
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

        fun prompt() {
            showSystemLockPrompt(
                activity = this,
                title = getString(R.string.webview_lock_prompt_title, pwaApp.name),
                onSuccess = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        app.passwordManager.clearFailedAttempts(pwaApp.id)
                    }
                    viewModel.onPasswordVerified()
                },
                onFailed = {
                    viewModel.recordFailedAttempt()
                    val state = viewModel.uiState.value.authState
                    if (state is AuthState.PasswordRequired &&
                        state.wipeEnabled && state.failedAttempts >= maxAttempts
                    ) {
                        viewModel.onWipeAndUnlock()
                    } else {
                        prompt()
                    }
                },
            )
        }
        prompt()
    }

    private fun startLoading(pwaApp: WebApp) {
        lifecycleScope.launch {
            showSplash(pwaApp)
            if (engine is SystemWebViewEngine) {
                isolationManager.restoreSession(pwaApp.isolationId)
            }
            engine.loadUrl(pwaApp.url)
        }
    }

    private fun addErrorOverlay(app: WebViewServiceProvider) {
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val state by viewModel.uiState.collectAsState()
                val accentColor = state.app?.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                val error = state.error ?: return@setContent
                ShellifyTheme(themeMode = themeMode, dynamicColor = dynamicColor, accentColor = accentColor, controlStatusBar = false) {
                    WebViewErrorScreen(
                        error = error,
                        isRetrying = state.isRetrying,
                        onRetry = { viewModel.onRetry() },
                    )
                }
            }
        }
        container.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
    }

    private fun addControlsOverlay(app: WebViewServiceProvider) {
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val state by viewModel.uiState.collectAsState()
                val pwaApp = state.app ?: return@setContent
                if (!pwaApp.showControlCenter || !state.isPageLoaded) return@setContent
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
                        onAdBlockChanged = { viewModel.onAdBlockChanged(it) },
                        onTranslateChanged = { viewModel.onTranslateChanged(it) },
                        onFullscreenChanged = { on ->
                            viewModel.onFullscreenChanged(on)
                            applyWindowMode(viewModel.uiState.value.app ?: pwaApp)
                        },
                        onLockChanged = { viewModel.onLockChanged(it) },
                        onClearData = { viewModel.onClearData() },
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

    private fun buildCallback(): BrowserEngineCallback =
        object : BrowserEngineCallback {
            override fun onPageStarted(url: String?) {
                viewModel.onPageStarted(url)
            }

            override fun onPageFinished(url: String?) {
                viewModel.onPageFinished(url)
                engine.getView()?.visibility = if (viewModel.uiState.value.error == null) View.VISIBLE else View.INVISIBLE
                hideSplash()
                val app = viewModel.uiState.value.app ?: return
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
                viewModel.onError(errorCode, description)
                engine.getView()?.visibility = View.INVISIBLE
                hideSplash()
            }

            override fun onSslError(error: String) {
                viewModel.onSslError(error)
                engine.getView()?.visibility = View.INVISIBLE
                hideSplash()
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
                val app = viewModel.uiState.value.app ?: return
                applyWindowMode(app.copy(isFullscreen = true))
            }

            override fun onHideCustomView() {
                (customView?.parent as? FrameLayout)?.removeView(customView)
                customView = null
                engine.getView()?.visibility = View.VISIBLE
                viewModel.uiState.value.app?.let { applyWindowMode(it) }
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
        @Suppress("MagicNumber")
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
                    controller.hide(WindowInsetsCompat.Type.navigationBars())
                    controller.show(WindowInsetsCompat.Type.statusBars())
                }
                showNav -> {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.show(WindowInsetsCompat.Type.navigationBars())
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
        @Suppress("MagicNumber")
        val isLight =
            (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255 > 0.5
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isStatusBarContrastEnforced = false
        window.statusBarColor = color
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
        val opaqueColor = Color.argb(255, Color.red(rawColor), Color.green(rawColor), Color.blue(rawColor))
        setTaskDescription(ActivityManager.TaskDescription(app.name, icon, opaqueColor))
    }

    override fun onResume() {
        super.onResume()
        viewModel.uiState.value.app?.let { applyTaskDescription(it) }
    }

    override fun onDestroy() {
        viewModel.onSessionEnd()
        engine.destroy()
        super.onDestroy()
    }
}
