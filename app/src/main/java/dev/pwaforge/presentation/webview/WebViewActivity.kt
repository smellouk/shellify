package dev.pwaforge.presentation.webview

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
import dev.pwaforge.presentation.theme.Dimens
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import dev.pwaforge.PWAForgeApplication
import dev.pwaforge.core.engine.BrowserEngine
import dev.pwaforge.core.engine.BrowserEngineCallback
import dev.pwaforge.domain.model.EngineType
import dev.pwaforge.core.engine.GeckoViewEngine
import dev.pwaforge.core.engine.SystemWebViewEngine
import dev.pwaforge.core.isolation.IsolationManager
import dev.pwaforge.core.security.showSystemLockPrompt
import dev.pwaforge.core.security.verifyPassword
import kotlinx.coroutines.flow.first
import dev.pwaforge.core.theme.ThemeMode
import dev.pwaforge.core.translate.TranslateBridge
import dev.pwaforge.domain.model.LockType
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.R
import dev.pwaforge.presentation.theme.PWAForgeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WebViewActivity : FragmentActivity() {

    companion object {
        const val EXTRA_APP_ID = "app_id"

        fun launchIntent(context: android.content.Context, appId: Long): Intent =
            Intent(context, WebViewActivity::class.java)
                .putExtra(EXTRA_APP_ID, appId)
                .setData(android.net.Uri.parse("pwaforge://app/$appId"))
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

        val app = application as PWAForgeApplication
        isolationManager = app.isolationManager

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1L)
        if (appId == -1L) { finish(); return }

        val pwaApp = runBlocking(Dispatchers.IO) { app.webAppRepository.getById(appId) }
            ?: run { finish(); return }
        currentAppFlow.value = pwaApp

        engine = when (pwaApp.engineType) {
            EngineType.GECKOVIEW -> GeckoViewEngine(this, app.geckoEngineManager)
            EngineType.SYSTEM_WEBVIEW -> SystemWebViewEngine(app.adBlocker)
        }

        container = FrameLayout(this)
        container.setBackgroundColor(Color.BLACK)

        val engineView = engine.createView(this, pwaApp, buildCallback(pwaApp, container))

        if (engine is SystemWebViewEngine) {
            val wv = (engine as SystemWebViewEngine).getWebView()
            if (wv != null) isolationManager.attachProfile(wv, pwaApp.isolationId)
        }

        container.addView(
            engineView,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )

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
        applyWindowMode(pwaApp)
        if (!pwaApp.isFullscreen) applyStatusBarColor(pwaApp.themeColor)
        applyTaskDescription(pwaApp)

        authenticate(app, pwaApp)
    }

    private fun authenticate(app: PWAForgeApplication, pwaApp: WebApp) {
        when (pwaApp.lockType) {
            LockType.NONE -> startLoading(pwaApp)
            LockType.PASSWORD -> {
                val hash = runBlocking { app.passwordManager.passwordHash.first() }
                if (hash != null) showPasswordDialog(app, pwaApp) else startLoading(pwaApp)
            }
            LockType.SYSTEM -> showSystemLockWithWipe(app, pwaApp)
        }
    }

    private fun showPasswordDialog(app: PWAForgeApplication, pwaApp: WebApp) {
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val hash by app.passwordManager.passwordHash.collectAsState(initial = null)
                var input by remember { mutableStateOf("") }
                var visible by remember { mutableStateOf(false) }
                var failedAttempts by remember { mutableStateOf(0) }
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

                PWAForgeTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        icon = { Icon(Icons.Default.Lock, null) },
                        title = { Text(pwaApp.name) },
                        text = {
                            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.spaceXxs)) {
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
                                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
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
                                    container.removeView(this@apply)
                                    startLoading(pwaApp)
                                } else {
                                    input = ""
                                    failedAttempts++
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

    private fun showSystemLockWithWipe(app: PWAForgeApplication, pwaApp: WebApp) {
        val maxAttempts = 3
        var failedAttempts = 0

        fun prompt() {
            showSystemLockPrompt(
                activity = this,
                title = getString(R.string.webview_lock_prompt_title, pwaApp.name),
                onSuccess = { startLoading(pwaApp) },
                onFailed = {
                    failedAttempts++
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

    private fun wipeAndUnlock(app: PWAForgeApplication, pwaApp: WebApp) {
        scope.launch(Dispatchers.IO) {
            app.isolationManager.clearData(pwaApp.isolationId)
            app.webAppRepository.save(pwaApp.copy(lockType = LockType.NONE))
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
        val app = application as PWAForgeApplication
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val pwaAppState by currentAppFlow.collectAsState()
                val pwaApp = pwaAppState ?: return@setContent
                val passwordHash by app.passwordManager.passwordHash.collectAsState(initial = null)
                val hasGlobalPassword = passwordHash != null

                PWAForgeTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                    var showSheet by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier.fillMaxSize().padding(Dimens.spaceLg),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        SmallFloatingActionButton(
                            onClick = { showSheet = true },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.webview_controls_fab_cd))
                        }
                    }

                    if (showSheet) {
                        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                            Text(
                                pwaApp.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceXxs),
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spaceSm))

                            ListItem(
                                leadingContent = { Icon(Icons.Default.Shield, null) },
                                headlineContent = { Text(stringResource(R.string.webview_control_adblock)) },
                                trailingContent = {
                                    Switch(
                                        checked = pwaApp.adBlockEnabled,
                                        onCheckedChange = { on ->
                                            val updated = pwaApp.copy(adBlockEnabled = on)
                                            currentAppFlow.value = updated
                                            scope.launch(Dispatchers.IO) { app.webAppRepository.save(updated) }
                                            engine.getCurrentUrl()?.let { engine.loadUrl(it) }
                                        },
                                    )
                                },
                            )
                            HorizontalDivider()
                            ListItem(
                                leadingContent = { Icon(Icons.Default.GTranslate, null) },
                                headlineContent = { Text(stringResource(R.string.webview_control_translate)) },
                                trailingContent = {
                                    Switch(
                                        checked = pwaApp.translateEnabled,
                                        onCheckedChange = { on ->
                                            val updated = pwaApp.copy(translateEnabled = on)
                                            currentAppFlow.value = updated
                                            scope.launch(Dispatchers.IO) { app.webAppRepository.save(updated) }
                                            if (on) {
                                                val script = TranslateBridge.buildScript(
                                                    targetLang = updated.translateTarget.code,
                                                    showButton = updated.showTranslateButton,
                                                    autoTranslate = updated.autoTranslateOnLoad,
                                                )
                                                engine.evaluateJavascript(script, null)
                                            } else {
                                                engine.getCurrentUrl()?.let { engine.loadUrl(it) }
                                            }
                                        },
                                    )
                                },
                            )
                            HorizontalDivider()
                            ListItem(
                                leadingContent = { Icon(Icons.Default.Fullscreen, null) },
                                headlineContent = { Text(stringResource(R.string.webview_control_fullscreen)) },
                                trailingContent = {
                                    Switch(
                                        checked = pwaApp.isFullscreen,
                                        onCheckedChange = { on ->
                                            val updated = pwaApp.copy(isFullscreen = on)
                                            currentAppFlow.value = updated
                                            scope.launch(Dispatchers.IO) { app.webAppRepository.save(updated) }
                                            applyWindowMode(updated)
                                        },
                                    )
                                },
                            )
                            HorizontalDivider()
                            ListItem(
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
                                            scope.launch(Dispatchers.IO) { app.webAppRepository.save(updated) }
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

    private fun buildCallback(app: WebApp, container: FrameLayout): BrowserEngineCallback =
        object : BrowserEngineCallback {
            override fun onPageStarted(url: String?) {
                url?.let { visitedUrls += it }
            }

            override fun onPageFinished(url: String?) {
                url?.let { visitedUrls += it }
                if (app.translateEnabled) {
                    val script = TranslateBridge.buildScript(
                        targetLang = app.translateTarget.code,
                        showButton = app.showTranslateButton,
                        autoTranslate = app.autoTranslateOnLoad,
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
                applyWindowMode(app.copy(isFullscreen = true))
            }

            override fun onHideCustomView() {
                (customView?.parent as? FrameLayout)?.removeView(customView)
                customView = null
                engine.getView()?.visibility = View.VISIBLE
                applyWindowMode(app)
            }

            override fun onDownloadStart(
                url: String, userAgent: String, contentDisposition: String,
                mimeType: String, contentLength: Long,
            ) {}
        }

    private fun applyWindowMode(app: WebApp) {
        val fullscreen = app.isFullscreen
        WindowCompat.setDecorFitsSystemWindows(window, !fullscreen)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val showStatus = app.fullscreenShowStatusBar
            val showNav = app.fullscreenShowNavBar
            when {
                showStatus && showNav -> controller.show(WindowInsetsCompat.Type.systemBars())
                showStatus -> { controller.hide(WindowInsetsCompat.Type.navigationBars()); controller.show(WindowInsetsCompat.Type.statusBars()) }
                showNav -> { controller.hide(WindowInsetsCompat.Type.statusBars()); controller.show(WindowInsetsCompat.Type.navigationBars()) }
                else -> controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun applyStatusBarColor(themeColor: String?) {
        val color = themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: return
        val isLight = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255 > 0.5
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight

        if (Build.VERSION.SDK_INT >= 35) {
            // Android 15+: window.statusBarColor is a no-op under forced edge-to-edge.
            // Draw a scrim view at the top of the container sized to the status bar inset.
            val scrim = statusBarScrim ?: View(this).also { v ->
                statusBarScrim = v
                container.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP))
            }
            scrim.setBackgroundColor(color)
            ViewCompat.setOnApplyWindowInsetsListener(container) { _, insets ->
                val h = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                scrim.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, h, Gravity.TOP)
                insets
            }
            ViewCompat.requestApplyInsets(container)
        } else {
            @Suppress("DEPRECATION")
            window.statusBarColor = color
        }
    }

    @Suppress("DEPRECATION")
    private fun applyTaskDescription(app: WebApp) {
        val icon: Bitmap? = app.iconPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
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
