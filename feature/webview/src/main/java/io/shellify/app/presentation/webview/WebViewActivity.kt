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
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.alpha
import io.shellify.app.core.engine.TorState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.shellify.app.core.engine.BrowserEngine
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.core.engine.GeckoViewEngine
import io.shellify.app.core.engine.SystemWebViewEngine
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.isLegacyHash
import io.shellify.app.core.security.showSystemLockPrompt
import io.shellify.app.core.security.verifyPassword
import io.shellify.app.core.theme.ThemeMode
import androidx.core.app.NotificationManagerCompat
import io.shellify.app.core.webbridge.NotificationBridge
import io.shellify.app.core.webbridge.ReadingModeBridge
import io.shellify.app.core.webbridge.ShellifyBridge
import io.shellify.app.core.webbridge.TranslateBridge
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.theme.Dimens
import io.shellify.app.presentation.theme.IncognitoPurple
import io.shellify.app.presentation.theme.IncognitoPurpleHex
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.presentation.theme.incognitoModeBadge
import io.shellify.app.presentation.webview.WebViewViewModel.PermissionDialogState
import io.shellify.core.ui.R
import androidx.core.view.ViewCompat
import android.util.Log
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WebViewActivity : FragmentActivity() {

    companion object {
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_PREVIEW_URL = "preview_url"
        const val EXTRA_PREVIEW_NAME = "preview_name"

        /** Passes the LockType.name string to override the preview path lock enforcement. */
        const val EXTRA_LOCK_TYPE = "lock_type"

        // Opacity for the VisibilityOff icon and subtitle text in the incognito splash badge.
        private const val INCOGNITO_CONTENT_ALPHA = 0.8f

        @VisibleForTesting
        var engineFactory: (() -> BrowserEngine)? = null

        /** Overrides the [WebApp] used in [onCreate]. Set in tests to inject a custom app. */
        @VisibleForTesting
        var webAppOverride: WebApp? = null

        fun launchIntent(context: android.content.Context, appId: Long): Intent =
            Intent(context, WebViewActivity::class.java)
                .putExtra(EXTRA_APP_ID, appId)
                .setData(android.net.Uri.parse("shellify://app/$appId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

        fun previewIntent(
            context: android.content.Context,
            url: String,
            name: String,
            lockType: LockType? = null,
        ): Intent =
            Intent(context, WebViewActivity::class.java)
                .putExtra(EXTRA_PREVIEW_URL, url)
                .putExtra(EXTRA_PREVIEW_NAME, name)
                .apply { lockType?.let { putExtra(EXTRA_LOCK_TYPE, it.name) } }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

        private const val SPLASH_MIN_MS = 500L
    }

    private lateinit var engine: BrowserEngine
    private lateinit var progressBar: ProgressBar
    private lateinit var container: FrameLayout
    @VisibleForTesting
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var isolationManager: IsolationManager

    // Overlay views for OAuth / window.open() popups, newest last. Back press dismisses the topmost.
    private val popupOverlays = ArrayDeque<View>()
    private lateinit var viewModel: WebViewViewModel
    private var statusBarScrim: View? = null

    // Tor release metadata stored at activity level so onStop can call torManager.releaseApp even
    // when the ViewModel has not finished async initialization (WR-06). Set in initWithApp.
    private var torAppId: Long = -1L
    private var torEnabled: Boolean = false
    private var torPreserveIdentity: Boolean = false

    // Effective theme color for the current session — set once in initWithApp.
    // Equals IncognitoPurpleHex when alwaysIncognito is on; otherwise the app's themeColor.
    // Stored at activity scope so onResume can re-apply it without re-reading the DB.
    private var effectiveThemeColorHex: String? = null

    // One logger per Activity session: captures all onRequestIntercepted events into a live flow.
    private var networkRequestLogger: NetworkRequestLogger? = null

    // Controls NetworkLogSheet visibility from outside the Compose tree.
    private val showNetworkLog = MutableStateFlow(false)

    // Readability.js asset is loaded once at first reading-mode activation and reused for all
    // subsequent toggles — avoids repeated asset I/O on every tap (RESEARCH Pitfall 2).
    private val readabilityJs: String by lazy { assets.open("readability.min.js").bufferedReader().readText() }

    // Registered in onCreate — must be registered before the Activity reaches STARTED.
    private val postNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            lifecycleScope.launch {
                android.widget.Toast.makeText(
                    this@WebViewActivity,
                    getString(R.string.settings_notifications_permission_os_denied),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    @VisibleForTesting
    var splashOverlay: View? = null
    private var splashShownAt: Long = 0L

    @VisibleForTesting
    fun navigateTo(url: String) = engine.loadUrl(url)

    /** Number of currently displayed popup overlays (OAuth / window.open). */
    @VisibleForTesting
    fun popupOverlayCount(): Int = popupOverlays.size

    /** Invoked after a popup overlay is attached, so tests can await the asynchronous window.open. */
    @VisibleForTesting
    var popupShownCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WebViewServiceProvider
        isolationManager = app.isolationManager

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1L)
        val previewUrl = intent.getStringExtra(EXTRA_PREVIEW_URL)

        val immediateApp: WebApp? = webAppOverride ?: when {
            previewUrl != null -> WebApp(
                name = intent.getStringExtra(EXTRA_PREVIEW_NAME) ?: previewUrl,
                url = previewUrl,
            )
            else -> null
        }

        if (immediateApp != null) {
            initWithApp(app, immediateApp, appId, previewUrl)
        } else if (appId != -1L) {
            lifecycleScope.launch {
                val webApp = withContext(Dispatchers.IO) { app.getWebAppById(appId) }
                    ?: run { finish(); return@launch }
                initWithApp(app, webApp, appId, previewUrl)
            }
        } else {
            finish()
        }
    }

    private fun initWithApp(app: WebViewServiceProvider, initialPwaApp: WebApp, appId: Long, previewUrl: String?) {
        // On the preview path, allow the dispatcher to override the lock type (per D-02/INTG-07).
        // This is only applied when launching by URL; for appId launches the DB-stored lock is used.
        var pwaApp = if (previewUrl != null) {
            val lockTypeExtra = intent.getStringExtra(EXTRA_LOCK_TYPE)
            if (lockTypeExtra != null) initialPwaApp.copy(
                lockType = runCatching { LockType.valueOf(lockTypeExtra) }.getOrDefault(LockType.NONE)
            ) else initialPwaApp
        } else initialPwaApp

        // alwaysIncognito: if the app has the flag set, assign an ephemeral isolationId before
        // the WebView is constructed. This ensures onDestroy clears the ephemeral profile (per D-04 / T-02-07).
        if (pwaApp.alwaysIncognito) {
            pwaApp = pwaApp.copy(isolationId = java.util.UUID.randomUUID().toString())
        }

        // Override the visual theme with incognito purple when alwaysIncognito is on so every
        // UI surface (status bar, container, splash, progress bar, swipe indicator) signals
        // the privacy mode to the user without any extra state flag.
        val effectiveThemeColor: String? = if (pwaApp.alwaysIncognito) IncognitoPurpleHex else pwaApp.themeColor
        effectiveThemeColorHex = effectiveThemeColor

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.passwordManager.screenshotProtection.collect { on ->
                    if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        viewModel = ViewModelProvider(this, WebViewViewModel.Factory(pwaApp, app))[WebViewViewModel::class.java]
        // Store Tor release metadata at activity scope so onStop can call torManager.releaseApp
        // even if the ViewModel finishes initialization after onStop fires (WR-06).
        if (pwaApp.useTor && appId != -1L) {
            torAppId = appId
            torEnabled = true
            torPreserveIdentity = pwaApp.preserveTorIdentity
        }
        // Logger is only created for real app sessions (appId != -1L).
        // Preview and incognito sessions have no persistent app identity, so network
        // logging is skipped entirely — the null check in the engine callback is the guard.
        networkRequestLogger = if (appId != -1L) {
            NetworkRequestLogger(
                appId = appId,
                logNetworkRequest = app.logNetworkRequest,
                clearNetworkLogs = app.clearNetworkLogs,
            )
        } else null

        engine = engineFactory?.invoke() ?: when {
            pwaApp.useTor -> {
                // Tor requires GeckoView proxy routing. If GeckoView is not installed,
                // refuse to open and surface an error instead of silently leaking traffic.
                if (!app.geckoEngineManager.isInstalled()) {
                    viewModel.onError(-1, getString(R.string.webview_tor_requires_geckoview))
                    return
                }
                GeckoViewEngine(this, app.geckoEngineManager)
            }
            pwaApp.engineType == EngineType.GECKOVIEW && app.geckoEngineManager.isInstalled() ->
                GeckoViewEngine(this, app.geckoEngineManager)
            else -> SystemWebViewEngine(app.adBlocker)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    popupOverlays.isNotEmpty() -> dismissTopPopup()
                    engine.canGoBack() -> engine.goBack()
                    else -> finish()
                }
            }
        })

        container = FrameLayout(this)
        container.setBackgroundColor(
            effectiveThemeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                ?: Color.BLACK
        )

        // Initialise swipeRefreshLayout before engine.createView() so that the BrowserEngineCallback
        // (which sets isRefreshing = false in onPageFinished/onError/onSslError) can access it
        // safely even when a test engine factory fires the callback synchronously during createView.
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            val tintColor = effectiveThemeColor
                ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                ?: getColor(android.R.color.holo_blue_bright)
            setColorSchemeColors(tintColor)
            isEnabled = pwaApp.swipeToRefreshEnabled
            setOnChildScrollUpCallback { _, _ ->
                when (val e = engine) {
                    is GeckoViewEngine -> e.canScrollUp()
                    else -> e.getView()?.canScrollVertically(-1) ?: false
                }
            }
            setOnRefreshListener { engine.reload() }
        }

        val engineView = engine.createView(this, pwaApp, buildCallback())

        if (engine is SystemWebViewEngine) {
            val wv = (engine as SystemWebViewEngine).getWebView()
            if (wv != null) {
                isolationManager.attachProfile(wv, pwaApp.isolationId)
                wv.addJavascriptInterface(ShellifyBridge(
                    notificationCallback = { t: String, b: String, i: String ->
                        viewModel.onNotificationReceived(t, b, i, null)
                    },
                    permissionProvider = {
                        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) "BLOCKED_BY_OS"
                        else viewModel.uiState.value.app?.notificationPermission?.name ?: "NOT_ASKED"
                    },
                    permissionRequestCallback = {
                        Handler(Looper.getMainLooper()).post {
                            viewModel.onNotificationPermissionRequested { granted ->
                                resolveWebViewPermission(granted)
                            }
                        }
                    },
                ), "ShellifyBridge")
            }
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
            val tint = effectiveThemeColor
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

        swipeRefreshLayout.addView(container)
        // SwipeRefreshLayout is the root view after edge-to-edge is enabled. Register the insets
        // listener here — not on container — so insets are reliably dispatched to the actual root.
        // displayCutout handles punch-hole cameras; topInset = max(statusBar, cutout) pushes
        // the spinner below the camera and status bar without double-counting.
        ViewCompat.setOnApplyWindowInsetsListener(swipeRefreshLayout) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val topInset = maxOf(statusBars.top, displayCutout.top)
            @Suppress("MagicNumber")
            val spinnerEndPx = topInset + (64 * resources.displayMetrics.density).toInt()
            swipeRefreshLayout.setProgressViewOffset(false, topInset, spinnerEndPx)
            container.setPadding(0, topInset, 0, navBars.bottom)
            insets
        }
        setContentView(swipeRefreshLayout)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    swipeRefreshLayout.isEnabled = uiState.app?.swipeToRefreshEnabled ?: false
                }
            }
        }

        applyWindowMode(pwaApp)
        applyStatusBarColor(effectiveThemeColor)
        applyTaskDescription(pwaApp, effectiveThemeColor)

        observeState(app, pwaApp)
        collectCommands()

        // Track that this Activity is actively showing the app — used by BackgroundNotificationService
        // to avoid creating a competing GeckoSession for the same appId (T-06-30).
        if (appId != -1L) (application as? WebViewServiceProvider)?.registerActiveApp(appId)
    }

    private fun addPermissionDialogOverlay(app: WebViewServiceProvider) {
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val state by viewModel.uiState.collectAsState()
                val dialogState by viewModel.permissionDialog.collectAsState()
                val accentColor = state.app?.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                val shown = dialogState as? PermissionDialogState.Shown ?: return@setContent
                ShellifyTheme(themeMode = themeMode, dynamicColor = dynamicColor, accentColor = accentColor, controlStatusBar = false) {
                    // Guard against Material3 firing onDismissRequest after a button click, which
                    // would call onPermissionDialogResult a second time and overwrite the persisted
                    // grant with DENIED. Each dialog showing gets a fresh false via remember.
                    var dialogHandled by remember { mutableStateOf(false) }
                    AlertDialog(
                        onDismissRequest = {
                            if (!dialogHandled) {
                                dialogHandled = true
                                viewModel.onPermissionDialogResult(false)
                                // pendingPermissionResult callback resolves the JS Promise (if any).
                            }
                        },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        title = { Text(shown.appName) },
                        text = { Text(stringResource(R.string.notification_permission_dialog_body, shown.appName)) },
                        confirmButton = {
                            TextButton(onClick = {
                                if (!dialogHandled) {
                                    dialogHandled = true
                                    viewModel.onPermissionDialogResult(true)
                                    // pendingPermissionResult callback resolves the JS Promise only
                                    // when this dialog was triggered by JS requestPermission(); for
                                    // the notification-arrival path the callback re-dispatches instead.
                                    requestPostNotificationsPermissionIfNeeded()
                                    // SystemWebView needs a reload to re-inject NotificationBridge.
                                    // GeckoView must NOT reload: the reload resets Notification.permission
                                    // to 'default', breaking timers already scheduled on the page.
                                    if (engine.engineType == EngineType.SYSTEM_WEBVIEW) engine.reload()
                                }
                            }) { Text(stringResource(R.string.notification_permission_allow)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                if (!dialogHandled) {
                                    dialogHandled = true
                                    viewModel.onPermissionDialogResult(false)
                                }
                            }) { Text(stringResource(R.string.notification_permission_not_now)) }
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

    private fun resolveWebViewPermission(granted: Boolean) {
        val app = viewModel.uiState.value.app ?: return
        if (app.engineType != EngineType.SYSTEM_WEBVIEW) return
        engine.evaluateJavascript("window.__shellifyResolvePermission($granted)", null)
        if (granted) {
            engine.evaluateJavascript("window.__shellifyNotificationLoaded = false;", null)
            engine.evaluateJavascript(NotificationBridge.buildScript(), null)
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                postNotificationsLauncher.launch(permission)
            }
        }
    }

    private fun observeState(app: WebViewServiceProvider, pwaApp: WebApp) {
        lifecycleScope.launch {
            var previousAuthState: AuthState? = null
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val authState = state.authState
                    if (authState != previousAuthState) {
                        previousAuthState = authState
                        handleAuthState(app, pwaApp, authState)
                    }
                }
            }
        }
        addErrorOverlay(app)
        addControlsOverlay(app)
        addNetworkLogOverlay(app)
        addPermissionDialogOverlay(app)
        addTorConnectingOverlay(app, pwaApp)
    }

    /**
     * Adds a full-screen centered overlay for Tor bootstrapping and error states.
     * Replaces the previous top-corner chip with a more prominent, app-like experience:
     *
     *  • [TorState.Connecting]: full-screen surface with centered [VpnLock] icon,
     *    "Connecting to Tor network…" text, and a [CircularProgressIndicator].
     *    The overlay fades away automatically once Tor reaches [TorState.Ready].
     *  • [TorState.Error]: replaces the spinner with a [Warning] icon, error message,
     *    and a Retry button that requests a new Tor circuit via [WebViewViewModel.onNewTorIdentity].
     *
     * Skipped entirely for non-Tor apps.
     */
    @Suppress("MagicNumber")
    private fun addTorConnectingOverlay(app: WebViewServiceProvider, pwaApp: WebApp) {
        if (!pwaApp.useTor) return
        val overlay = ComposeView(this).apply {
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val accentColor = pwaApp.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                val state by viewModel.uiState.collectAsState()
                val isConnecting = state.torState is TorState.Connecting
                val isError = state.torState is TorState.Error
                // Keep the overlay visible until the first onPageStop arrives (isPageLoaded).
                // Without this guard there is a window between TorState.Ready and the first
                // successful page-stop where GeckoView errors (e.g. a transient SOCKS
                // connection race) would flash through on screen.
                val isWaitingForPage = !isError && !state.isPageLoaded

                ShellifyTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    accentColor = accentColor,
                    controlStatusBar = false,
                ) {
                    AnimatedVisibility(
                        visible = isConnecting || isError || isWaitingForPage,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(400)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
                            ) {
                                // Branch on isError first so TorState.Ready + !isPageLoaded
                                // still shows the spinner rather than falling through to the
                                // error branch (which is the old bug: isConnecting=false → else).
                                if (isError) {
                                    val errorMessage = (state.torState as TorState.Error).message
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.size5xl),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = stringResource(R.string.webview_tor_error_chip),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = Dimens.spaceXxl),
                                    )
                                    androidx.compose.material3.Button(
                                        onClick = { viewModel.onNewTorIdentity() },
                                    ) {
                                        Text(stringResource(R.string.webview_tor_retry))
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.VpnLock,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.size5xl),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = stringResource(
                                            if (isConnecting) R.string.webview_tor_connecting
                                            else R.string.webview_tor_loading,
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.commands.collect { command ->
                    when (command) {
                        is WebViewCommand.LoadUrl -> engine.loadUrl(command.url)
                        WebViewCommand.Reload -> engine.reload()
                        WebViewCommand.Finish -> finish()
                        WebViewCommand.PageFinished -> pageFinishedCallback?.invoke()
                        // Panic wipe complete: finish this Activity and return to HomeScreen.
                        // The back stack already has HomeScreen; finishing here surfaces it
                        // with an empty app list (per D-02 wipe sequence).
                        WebViewCommand.NavigateHome -> finish()
                        // NewTorIdentityRequested: torState transitions Ready->Connecting->Ready
                        // automatically via TorManager BroadcastReceiver. The bootstrap chip
                        // will reappear on its own — no explicit UI action needed here.
                        WebViewCommand.NewTorIdentityRequested -> Unit
                        WebViewCommand.LoadReadingMode -> {
                            runCatching {
                                val noContent = getString(R.string.webview_reader_no_content)
                                engine.evaluateJavascript(ReadingModeBridge.buildScript(readabilityJs, noContent), null)
                            }.onFailure { e -> Log.e("WebViewActivity", "Failed to inject reading mode script", e) }
                        }
                    }
                }
            }
        }
    }

    @VisibleForTesting
    var pageFinishedCallback: (() -> Unit)? = null

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
            // Route through onAppReady so Tor apps wait for TorState.Ready before loading
            // any URL (T-02-23 traffic-leak prevention gate). Non-Tor apps emit LoadUrl immediately.
            viewModel.onAppReady(pwaApp)
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
                        torState = state.torState,
                        onAdBlockChanged = { viewModel.onAdBlockChanged(it) },
                        onTranslateChanged = { viewModel.onTranslateChanged(it) },
                        onFullscreenChanged = { on ->
                            viewModel.onFullscreenChanged(on)
                            applyWindowMode(viewModel.uiState.value.app ?: pwaApp)
                        },
                        onLockChanged = { viewModel.onLockChanged(it) },
                        onClearData = { viewModel.onClearData() },
                        onNetworkLogClick = { showNetworkLog.value = true },
                        onNewTorIdentity = { viewModel.onNewTorIdentity() },
                        onPanic = { viewModel.executePanicWipe() },
                        isReadingModeActive = state.isReadingModeActive,
                        onReadingModeToggled = { viewModel.toggleReadingMode() },
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

    private fun addNetworkLogOverlay(app: WebViewServiceProvider) {
        val logOverlay = ComposeView(this).apply {
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
                val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
                val state by viewModel.uiState.collectAsState()
                val show by showNetworkLog.collectAsState()
                if (!show) return@setContent
                val accentColor = state.app?.themeColor?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                ShellifyTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    accentColor = accentColor,
                    controlStatusBar = false,
                ) {
                    NetworkLogSheet(
                        sessionLog = networkRequestLogger?.sessionLog?.collectAsState()?.value ?: emptyList(),
                        isGeckoEngine = engine is GeckoViewEngine,
                        onDismiss = { showNetworkLog.value = false },
                        onClearSession = { networkRequestLogger?.clearSession() },
                    )
                }
            }
        }
        container.addView(
            logOverlay,
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
                // Inject the appropriate notification script for System WebView on each page load.
                // Load-guards prevent double-patching; re-injection on navigation is intentional.
                if (app.engineType == EngineType.SYSTEM_WEBVIEW) {
                    when (app.notificationPermission) {
                        NotificationPermission.GRANTED -> {
                            engine.evaluateJavascript("window.__shellifyNotificationLoaded = false;", null)
                            engine.evaluateJavascript(NotificationBridge.buildScript(), null)
                        }
                        NotificationPermission.NOT_ASKED -> {
                            engine.evaluateJavascript("window.__shellifyPermRequestLoaded = false;", null)
                            engine.evaluateJavascript(NotificationBridge.buildPermissionRequestScript(), null)
                        }
                        NotificationPermission.DENIED -> Unit
                    }
                }
                swipeRefreshLayout.isRefreshing = false
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
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onSslError(error: String) {
                viewModel.onSslError(error)
                engine.getView()?.visibility = View.INVISIBLE
                hideSplash()
                swipeRefreshLayout.isRefreshing = false
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

            override fun onShowPopup(view: View) {
                // System WebView popups are fresh WebViews without the per-app profile — attach the
                // same isolation profile so OAuth cookies land in (and are read from) the app's store.
                // GeckoView popups inherit the parent contextId, so they need no extra wiring.
                val isolationId = viewModel.uiState.value.app?.isolationId
                if (view is WebView && isolationId != null) isolationManager.attachProfile(view, isolationId)
                popupOverlays.addLast(view)
                container.addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                popupShownCallback?.invoke()
            }

            override fun onClosePopup(view: View) {
                popupOverlays.remove(view)
                (view.parent as? ViewGroup)?.removeView(view)
            }

            override fun onDownloadStart(
                url: String, userAgent: String, contentDisposition: String,
                mimeType: String, contentLength: Long,
            ) {
            }

            override fun onNotificationReceived(title: String, body: String?, iconUrl: String?, tag: String?) {
                viewModel.onNotificationReceived(title, body, iconUrl, tag)
            }

            override fun onNotificationPermissionRequested(onResult: (Boolean) -> Unit) {
                viewModel.onNotificationPermissionRequested(onResult)
            }

            override fun onRequestIntercepted(url: String, blocked: Boolean) {
                // logger is null for preview/incognito sessions — safe no-op.
                networkRequestLogger?.onRequestIntercepted(url, blocked)
            }
        }

    // Routes back-press dismissal through the engine so the underlying popup session/WebView is
    // torn down (not just hidden); the engine fires onClosePopup to detach the overlay.
    private fun dismissTopPopup() {
        engine.closeTopPopup()
    }

    private fun showSplash(pwaApp: WebApp) {
        splashShownAt = SystemClock.elapsedRealtime()
        val app = application as WebViewServiceProvider

        // When alwaysIncognito is on, the splash always uses the deep-purple signal color with
        // white text regardless of the app's configured themeColor.
        val bgColor: androidx.compose.ui.graphics.Color
        val contentColor: androidx.compose.ui.graphics.Color
        if (pwaApp.alwaysIncognito) {
            bgColor = IncognitoPurple
            contentColor = androidx.compose.ui.graphics.Color.White
        } else {
            val rawColor = pwaApp.themeColor
                ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                ?: Color.BLACK
            bgColor = androidx.compose.ui.graphics.Color(rawColor)
            @Suppress("MagicNumber")
            val isLight =
                (0.299 * Color.red(rawColor) + 0.587 * Color.green(rawColor) + 0.114 * Color.blue(rawColor)) / 255 > 0.5
            contentColor = if (isLight) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
        }

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
                            if (pwaApp.alwaysIncognito) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        tint = contentColor.copy(alpha = INCOGNITO_CONTENT_ALPHA),
                                        modifier = Modifier.size(Dimens.sizeSm),
                                    )
                                    Text(
                                        text = stringResource(R.string.webview_incognito_mode),
                                        style = MaterialTheme.typography.incognitoModeBadge,
                                        color = contentColor.copy(alpha = INCOGNITO_CONTENT_ALPHA),
                                    )
                                }
                            }
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
        val remaining = (SPLASH_MIN_MS - (SystemClock.elapsedRealtime() - splashShownAt)).coerceAtLeast(0L)
        lifecycleScope.launch {
            if (remaining > 0L) delay(remaining)
            splash.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction { container.removeView(splash) }
                .start()
        }
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
    private fun applyTaskDescription(app: WebApp, themeColor: String? = app.themeColor) {
        val icon: Bitmap? =
            app.iconPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        val rawColor = themeColor
            ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            ?: Color.WHITE
        val opaqueColor = Color.argb(255, Color.red(rawColor), Color.green(rawColor), Color.blue(rawColor))
        setTaskDescription(ActivityManager.TaskDescription(app.name, icon, opaqueColor))
    }

    override fun onStart() {
        super.onStart()
        val appId = intent.getLongExtra(EXTRA_APP_ID, -1L)
        if (appId != -1L) (application as? WebViewServiceProvider)?.registerActiveApp(appId)
        if (::engine.isInitialized && engine is GeckoViewEngine) {
            // Reclaim the runtime-scoped WebNotificationDelegate that the background service may
            // have overwritten so notifications route through the ViewModel (in-app handling).
            (engine as GeckoViewEngine).reattachNotificationDelegate()
            // The foreground Activity now handles notifications — stop the background service.
            val stopIntent = android.content.Intent(this, BackgroundNotificationService::class.java)
                .apply { action = BackgroundNotificationService.ACTION_STOP }
            startService(stopIntent)
        }
    }

    override fun onStop() {
        super.onStop()
        val appId = intent.getLongExtra(EXTRA_APP_ID, -1L)
        if (appId == -1L) return
        (application as? WebViewServiceProvider)?.unregisterActiveApp(appId)
        // For GeckoView apps with notification permission, start a foreground service so the
        // background GeckoSession runs without Android's JS throttling. The service's initSession()
        // dedup guard prevents creating multiple sessions if onStartCommand fires repeatedly.
        if (::engine.isInitialized && engine is GeckoViewEngine &&
            ::viewModel.isInitialized &&
            viewModel.uiState.value.app?.notificationPermission == NotificationPermission.GRANTED
        ) {
            val svcIntent = android.content.Intent(this, BackgroundNotificationService::class.java)
                .apply { putExtra(BackgroundNotificationService.EXTRA_APP_ID, appId) }
            startForegroundService(svcIntent)
        }
        // Cookie auto-wipe: when enabled, clear the isolation profile on stop so the next
        // session starts with a clean cookie jar. This is distinct from incognito onDestroy
        // wipe — it preserves the persistent isolationId (per D-05 / T-02-06).
        if (::viewModel.isInitialized) {
            viewModel.onSessionStop()
        } else if (torEnabled && torAppId != -1L) {
            // ViewModel not yet initialized (fast onStop before async DB lookup completes).
            // Release the Tor app registration directly so the grace-period shutdown can proceed
            // and stale app IDs do not block daemon shutdown forever (WR-06).
            (application as? WebViewServiceProvider)?.torManager?.releaseApp(torAppId, torPreserveIdentity)
        }
    }

    override fun onResume() {
        super.onResume()
        // viewModel is initialized asynchronously for appId launches — guard until ready.
        if (::viewModel.isInitialized) {
            viewModel.uiState.value.app?.let { applyTaskDescription(it, effectiveThemeColorHex) }
        }
    }

    override fun onDestroy() {
        // viewModel/engine may not be initialized if the Activity was destroyed before the
        // async DB lookup (appId path) completed.
        if (::viewModel.isInitialized) {
            viewModel.onSessionEnd()
        }
        // Cancel the logger scope before engine destroy — prevents orphaned IO coroutines.
        networkRequestLogger?.cancel()
        // unregisterActiveApp was already called in onStop(); engine.destroy() closes the session.
        if (::engine.isInitialized) engine.destroy()
        super.onDestroy()
    }

}
