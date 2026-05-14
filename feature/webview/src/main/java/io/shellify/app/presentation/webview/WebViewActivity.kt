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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.shellify.app.presentation.theme.Dimens
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

class WebViewActivity : FragmentActivity() {

    companion object {
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_PREVIEW_URL = "preview_url"
        const val EXTRA_PREVIEW_NAME = "preview_name"

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
    private val currentAppFlow = MutableStateFlow<WebApp?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val visitedUrls = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WebViewServiceProvider
        isolationManager = app.isolationManager

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1L)
        val previewUrl = intent.getStringExtra(EXTRA_PREVIEW_URL)

        val pwaApp: WebApp = when {
            previewUrl != null -> WebApp(
                name = intent.getStringExtra(EXTRA_PREVIEW_NAME) ?: previewUrl,
                url = previewUrl,
            )
            appId != -1L -> runBlocking(Dispatchers.IO) { app.getWebAppById(appId) }
                ?: run { finish(); return }
            else -> { finish(); return }
        }
        currentAppFlow.value = pwaApp

        // Apply FLAG_SECURE before setContentView so the window is never exposed unprotected.
        val screenshotProtection = runBlocking { app.passwordManager.screenshotProtection.first() }
        if (screenshotProtection) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        // Track live changes (e.g., user toggles the setting while the activity is visible).
        scope.launch {
            app.passwordManager.screenshotProtection.collect { on ->
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        engine = when {
            pwaApp.engineType == EngineType.GECKOVIEW && app.geckoEngineManager.isInstalled() ->
                GeckoViewEngine(this, app.geckoEngineManager)

            else -> SystemWebViewEngine(app.adBlocker)
        }

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
        // Load persisted count so the wipe limit survives process death between attempts.
        val persistedAttempts = runBlocking { app.passwordManager.getFailedAttempts(pwaApp.id) }
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val hash by app.passwordManager.passwordHash.collectAsState(initial = null)
                var input by remember { mutableStateOf("") }
                var visible by remember { mutableStateOf(false) }
                var failedAttempts by remember { mutableStateOf(persistedAttempts) }
                val maxAttempts = 3
                val remaining = maxAttempts - failedAttempts
                val wipe by app.passwordManager.wipeOnFailedAttempts.collectAsState(initial = false)

                val err2 = stringResource(R.string.webview_password_error_2_attempts)
                val err1 = stringResource(R.string.webview_password_error_1_attempt)
                val errBasic = stringResource(R.string.webview_password_error)
                val error = when {
                    failedAttempts == 0 -> null
                    wipe && remaining == 2 -> err2
                    wipe && remaining == 1 -> err1
                    !wipe -> errBasic
                    else -> null
                }

                ShellifyTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    controlStatusBar = false
                ) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        icon = { Icon(Icons.Default.Lock, null) },
                        title = { Text(pwaApp.name) },
                        text = {
                            Column(
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                    Dimens.spaceXxs
                                )
                            ) {
                                Text(stringResource(R.string.webview_password_prompt))
                                Spacer(Modifier.height(Dimens.spaceSm))
                                OutlinedTextField(
                                    value = input,
                                    onValueChange = { input = it },
                                    label = { Text(stringResource(R.string.common_password)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        IconButton(onClick = { visible = !visible }) {
                                            Icon(
                                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                null
                                            )
                                        }
                                    },
                                    isError = error != null,
                                    supportingText = { if (error != null) Text(error) },
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
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
                                    input = ""
                                    failedAttempts++
                                    scope.launch(Dispatchers.IO) {
                                        app.passwordManager.recordFailedAttempt(pwaApp.id)
                                    }
                                    if (wipe && failedAttempts >= maxAttempts) {
                                        wipeAndUnlock(app, pwaApp)
                                    }
                                }
                            }) { Text(stringResource(R.string.webview_unlock_button)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) { Text(stringResource(R.string.common_cancel)) }
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
            if (engine is SystemWebViewEngine) {
                isolationManager.restoreSession(pwaApp.isolationId)
            }
            engine.loadUrl(pwaApp.url)
            addControlsOverlay()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun addControlsOverlay() {
        val app = application as WebViewServiceProvider
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val pwaAppState by currentAppFlow.collectAsState()
                val pwaApp = pwaAppState ?: return@setContent
                val passwordHash by app.passwordManager.passwordHash.collectAsState(initial = null)
                val hasGlobalPassword = passwordHash != null

                ShellifyTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    controlStatusBar = false
                ) {
                    var showSheet by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(Dimens.spaceLg),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        SmallFloatingActionButton(
                            onClick = { showSheet = true },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = stringResource(R.string.webview_controls_fab_cd)
                            )
                        }
                    }

                    if (showSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showSheet = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                stringResource(R.string.webview_sheet_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(
                                    horizontal = Dimens.spaceLg,
                                    vertical = Dimens.spaceXxs
                                ),
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    horizontal = Dimens.spaceLg,
                                    vertical = Dimens.spaceSm
                                )
                            )

                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                leadingContent = { Icon(Icons.Default.Shield, null) },
                                headlineContent = { Text(stringResource(R.string.webview_control_adblock)) },
                                trailingContent = {
                                    Switch(
                                        checked = pwaApp.adBlockEnabled,
                                        onCheckedChange = { on ->
                                            val updated = pwaApp.copy(adBlockEnabled = on)
                                            currentAppFlow.value = updated
                                            scope.launch(Dispatchers.IO) {
                                                app.saveWebApp(
                                                    updated
                                                )
                                            }
                                            engine.getCurrentUrl()?.let { engine.loadUrl(it) }
                                        },
                                    )
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                leadingContent = { Icon(Icons.Default.GTranslate, null) },
                                headlineContent = { Text(stringResource(R.string.webview_control_translate)) },
                                trailingContent = {
                                    Switch(
                                        checked = pwaApp.translateEnabled,
                                        onCheckedChange = { on ->
                                            val updated = pwaApp.copy(translateEnabled = on)
                                            currentAppFlow.value = updated
                                            scope.launch(Dispatchers.IO) {
                                                app.saveWebApp(
                                                    updated
                                                )
                                            }
                                            if (on) {
                                                engine.evaluateJavascript(
                                                    "window.__shellifyTranslateLoaded = false;",
                                                    null
                                                )
                                                val script = TranslateBridge.buildScript(
                                                    targetLang = updated.translateTarget.code,
                                                    autoTranslate = true,
                                                )
                                                engine.evaluateJavascript(script, null)
                                            } else {
                                                engine.getCurrentUrl()?.let { engine.loadUrl(it) }
                                            }
                                        },
                                    )
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                leadingContent = { Icon(Icons.Default.Fullscreen, null) },
                                headlineContent = { Text(stringResource(R.string.webview_control_fullscreen)) },
                                trailingContent = {
                                    Switch(
                                        checked = pwaApp.isFullscreen,
                                        onCheckedChange = { on ->
                                            val updated = pwaApp.copy(isFullscreen = on)
                                            currentAppFlow.value = updated
                                            scope.launch(Dispatchers.IO) {
                                                app.saveWebApp(
                                                    updated
                                                )
                                            }
                                            applyWindowMode(updated)
                                        },
                                    )
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                leadingContent = {
                                    Icon(
                                        if (pwaApp.lockType != LockType.NONE) Icons.Default.Lock else Icons.Default.LockOpen,
                                        null,
                                        tint = if (hasGlobalPassword) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        stringResource(R.string.webview_control_applock),
                                        color = if (hasGlobalPassword) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    )
                                },
                                supportingContent = if (!hasGlobalPassword) ({
                                    Text(
                                        stringResource(R.string.webview_applock_disabled_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    )
                                }) else null,
                                trailingContent = {
                                    Switch(
                                        checked = pwaApp.lockType != LockType.NONE,
                                        onCheckedChange = { on ->
                                            val updated = pwaApp.copy(
                                                lockType = if (on) LockType.PASSWORD else LockType.NONE,
                                            )
                                            currentAppFlow.value = updated
                                            scope.launch(Dispatchers.IO) {
                                                app.saveWebApp(
                                                    updated
                                                )
                                            }
                                        },
                                        enabled = hasGlobalPassword,
                                    )
                                },
                            )

                            Spacer(Modifier.navigationBarsPadding())
                        }
                    }
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
            override fun onPageStarted(url: String?) {
                url?.let { visitedUrls += it }
            }

            override fun onPageFinished(url: String?) {
                url?.let { visitedUrls += it }
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
            override fun onError(errorCode: Int, description: String) {}
            override fun onSslError(error: String) {}

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
        setTaskDescription(ActivityManager.TaskDescription(app.name, icon))
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (engine.canGoBack()) engine.goBack()
        else super.onBackPressed()
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
