package io.shellify.app.presentation.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.shellify.app.core.engine.TorManager
import io.shellify.app.core.engine.TorState
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WebViewViewModel(
    private val initialApp: WebApp,
    private val isolationManager: IsolationManager,
    private val saveWebApp: SaveWebAppUseCase,
    private val passwordManager: PasswordManager,
    private val notificationDispatcher: PwaNotificationDispatcher? = null,
    private val deleteAllAppsUseCase: DeleteAllAppsUseCase? = null,
    private val getWebApps: GetWebAppsUseCase? = null,
    private val themeManager: ThemeManager? = null,
    private val torManager: TorManager? = null,
) : ViewModel() {

    sealed interface PermissionDialogState {
        data object Hidden : PermissionDialogState
        data class Shown(val appName: String) : PermissionDialogState
    }

    private val _uiState = MutableStateFlow(WebViewUiState(app = initialApp))
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    private val _commands = MutableSharedFlow<WebViewCommand>(replay = 1, extraBufferCapacity = 16)
    val commands: SharedFlow<WebViewCommand> = _commands.asSharedFlow()

    private val _permissionDialog = MutableStateFlow<PermissionDialogState>(PermissionDialogState.Hidden)
    val permissionDialog: StateFlow<PermissionDialogState> = _permissionDialog.asStateFlow()

    // Holds the pending callback from the engine until the user answers the dialog.
    private var pendingPermissionResult: ((Boolean) -> Unit)? = null

    private var loadFailed = false
    private val visitedUrls = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            val authState = resolveAuthState()
            _uiState.update { it.copy(authState = authState) }
        }
        // For Tor apps, prime the UI state to Connecting before the first TorState emission
        // arrives from TorManager. Without this, the initial TorState.Stopped means the
        // Tor connecting overlay is not visible during the startup gap, so GeckoView crash-
        // recovery errors (e.g. proxy unavailable after a :tor process restart) flash through.
        if (initialApp.useTor) {
            _uiState.update { it.copy(torState = TorState.Connecting) }
        }
        // Collect TorState from TorManager and forward to UiState so the Activity/Compose layer
        // can reactively update the bootstrap chip and toolbar without direct TorManager access.
        torManager?.let { tm ->
            viewModelScope.launch {
                tm.torState.collect { newState ->
                    _uiState.update { it.copy(torState = newState) }
                }
            }
        }
    }

    private suspend fun resolveAuthState(): AuthState {
        return when (initialApp.lockType) {
            LockType.NONE -> AuthState.Authenticated
            LockType.PASSWORD -> {
                val hash = passwordManager.passwordHash.first()
                if (hash != null) {
                    val failedAttempts = passwordManager.getFailedAttempts(initialApp.id)
                    val wipeEnabled = passwordManager.wipeOnFailedAttempts.first()
                    AuthState.PasswordRequired(
                        hash = hash,
                        failedAttempts = failedAttempts,
                        wipeEnabled = wipeEnabled,
                    )
                } else {
                    AuthState.Authenticated
                }
            }
            LockType.SYSTEM -> AuthState.SystemLockRequired
        }
    }

    fun onPageStarted(url: String?) {
        if (url?.startsWith("about:") == true) return
        loadFailed = false
        url?.let { visitedUrls += it }
    }

    fun onPageFinished(url: String?) {
        _uiState.update { it.copy(isRetrying = false, isPageLoaded = true) }
        if (!loadFailed) {
            _uiState.update { it.copy(error = null) }
        }
        url?.let { visitedUrls += it }
        viewModelScope.launch { _commands.emit(WebViewCommand.PageFinished) }
    }

    fun onError(code: Int, desc: String) {
        // Suppress errors that arrive while the Tor circuit is not yet ready. They indicate
        // SOCKS-proxy unavailability (expected during bootstrapping), not real page failures.
        // The Tor connecting overlay covers the screen during this phase, so surfacing an
        // error screen here would be confusing and is immediately overwritten anyway.
        if (initialApp.useTor && _uiState.value.torState !is TorState.Ready) return
        loadFailed = true
        _uiState.update { it.copy(error = WebLoadError.from(code, desc), isPageLoaded = true) }
    }

    fun onSslError(error: String) {
        loadFailed = true
        _uiState.update { it.copy(error = WebLoadError.SslError, isPageLoaded = true) }
    }

    fun onRetry() {
        _uiState.update { it.copy(isRetrying = true) }
        viewModelScope.launch { _commands.emit(WebViewCommand.Reload) }
    }

    fun onAdBlockChanged(on: Boolean) {
        val updated = currentApp().copy(adBlockEnabled = on)
        _uiState.update { it.copy(app = updated) }
        viewModelScope.launch(Dispatchers.IO) { saveWebApp(updated) }
        val currentUrl = updated.url
        viewModelScope.launch { _commands.emit(WebViewCommand.LoadUrl(currentUrl)) }
    }

    fun onTranslateChanged(on: Boolean) {
        val updated = currentApp().copy(translateEnabled = on)
        _uiState.update { it.copy(app = updated) }
        viewModelScope.launch(Dispatchers.IO) { saveWebApp(updated) }
    }

    fun onFullscreenChanged(on: Boolean) {
        val updated = currentApp().copy(isFullscreen = on)
        _uiState.update { it.copy(app = updated) }
        viewModelScope.launch(Dispatchers.IO) { saveWebApp(updated) }
    }

    fun onLockChanged(on: Boolean) {
        val updated = currentApp().copy(lockType = if (on) LockType.PASSWORD else LockType.NONE)
        _uiState.update { it.copy(app = updated) }
        viewModelScope.launch(Dispatchers.IO) { saveWebApp(updated) }
    }

    fun onClearData() {
        val app = currentApp()
        viewModelScope.launch {
            isolationManager.clearData(app.isolationId)
            _commands.emit(WebViewCommand.LoadUrl(app.url))
        }
    }

    fun onSessionEnd() {
        val app = _uiState.value.app ?: return
        isolationManager.onSessionEnd(app.isolationId, visitedUrls)
    }

    /**
     * Called from Activity.onStop(). Notifies TorManager that this app's session has ended
     * so it can schedule daemon shutdown if no other Tor apps remain.
     */
    fun onSessionStop() {
        val app = _uiState.value.app ?: return
        if (app.useTor) {
            torManager?.releaseApp(app.id, app.preserveTorIdentity)
        }
    }

    /**
     * Overload that accepts an explicit [app] — used by tests to verify releaseApp is called
     * with the correct app identity without depending on the ViewModel's internal state.
     */
    fun onSessionStop(app: WebApp) {
        if (app.useTor) {
            torManager?.releaseApp(app.id, app.preserveTorIdentity)
        }
    }

    /**
     * Called when authentication is resolved and the app is ready to load.
     *
     * For non-Tor apps, emits LoadUrl immediately.
     * For Tor apps (per T-02-23), gates the LoadUrl on TorState.Ready to prevent
     * pre-circuit traffic leaks. Also starts TorManager and registers preserve-identity
     * tracking when applicable (per D-07).
     */
    fun onAppReady(app: WebApp) {
        if (!app.useTor) {
            viewModelScope.launch { _commands.emit(WebViewCommand.LoadUrl(app.url)) }
            return
        }
        // Tor path: start the daemon and gate page load on TorState.Ready.
        torManager?.let { tm ->
            tm.ensureStarted(app.id, preserveIdentity = app.preserveTorIdentity)
            if (app.preserveTorIdentity) {
                tm.registerPreserveIdentityApp(app.id)
            }
            viewModelScope.launch {
                // Wait for the Tor circuit to be established before loading any URL.
                // This is the critical gate that prevents DNS and traffic leaks (T-02-23).
                // Also handles TorState.Error so the coroutine is not suspended indefinitely
                // when TorService fails to start (CR-02).
                val state = tm.torState.filter { it is TorState.Ready || it is TorState.Error }.first()
                if (state is TorState.Error) {
                    _uiState.update { it.copy(error = WebLoadError.from(-1, state.message)) }
                    return@launch
                }
                _commands.emit(WebViewCommand.LoadUrl(app.url))
            }
        } ?: run {
            // Fallback if TorManager not wired (should not happen in production)
            viewModelScope.launch { _commands.emit(WebViewCommand.LoadUrl(app.url)) }
        }
    }

    /**
     * Rotates the Tor circuit by sending a NEWNYM signal through TorManager.
     * The bootstrap chip will briefly reappear as the new circuit establishes.
     */
    fun onNewTorIdentity() {
        torManager?.newIdentity()
        viewModelScope.launch { _commands.emit(WebViewCommand.NewTorIdentityRequested) }
    }

    /** Shows the panic-wipe confirmation dialog (triggered by 2s long-press on panic icon). */
    fun onPanicLongPress() {
        _uiState.update { it.copy(showPanicConfirm = true) }
    }

    /** Dismisses the panic confirmation dialog without any wipe action. */
    fun onPanicDismiss() {
        _uiState.update { it.copy(showPanicConfirm = false) }
    }

    /**
     * Executes the atomic 6-step panic wipe sequence after user confirmation:
     *  1. Clear every IsolationManager profile
     *  2. Delete all apps from the DB via DeleteAllAppsUseCase
     *  3. Clear ThemeManager DataStore
     *  4. Clear PasswordManager DataStore
     *  5. Reset showPanicConfirm state
     *  6. Emit NavigateHome command
     *
     * All steps run sequentially inside a single coroutine so partial failure is minimised.
     * Per T-02-12: IsolationManager clearData calls are individually atomic; DeleteAllAppsUseCase
     * runs inside a Room transaction; DataStore edits are atomic.
     */
    fun executePanicWipe() = viewModelScope.launch {
        // Fail loudly if critical use cases are not wired. Their nullable type allows test
        // injection to omit them, but in production Factory.create() always provides both.
        // A null here means a misconfigured build where the wipe would silently skip clearing
        // isolation data — the most sensitive step (WR-05).
        checkNotNull(getWebApps) { "executePanicWipe: getWebApps use case not wired" }
        checkNotNull(deleteAllAppsUseCase) { "executePanicWipe: deleteAllAppsUseCase not wired" }
        val apps = getWebApps.invoke().first()
        apps.forEach { app -> isolationManager.clearData(app.isolationId) }
        deleteAllAppsUseCase.invoke()
        themeManager?.clearAll()
        passwordManager.clearAll()
        // Stop the Tor daemon so its foreground service notification disappears with the data (WR-01).
        torManager?.forceStop()
        _uiState.update { it.copy(showPanicConfirm = false) }
        _commands.emit(WebViewCommand.NavigateHome)
    }

    fun onPasswordVerified() {
        viewModelScope.launch(Dispatchers.IO) {
            passwordManager.clearFailedAttempts(initialApp.id)
        }
        _uiState.update { it.copy(authState = AuthState.Authenticated) }
    }

    fun recordFailedAttempt() {
        val current = _uiState.value.authState
        if (current !is AuthState.PasswordRequired) return
        val next = current.copy(failedAttempts = current.failedAttempts + 1)
        _uiState.update { it.copy(authState = next) }
        viewModelScope.launch(Dispatchers.IO) {
            passwordManager.recordFailedAttempt(initialApp.id)
        }
    }

    fun onWipeAndUnlock() {
        viewModelScope.launch(Dispatchers.IO) {
            isolationManager.clearData(initialApp.isolationId)
            saveWebApp(initialApp.copy(lockType = LockType.NONE))
            _commands.emit(WebViewCommand.Finish)
        }
    }

    /**
     * Called when the engine fires a notification permission request.
     *
     * Three-branch behavior:
     * - GRANTED → invoke onResult(true) immediately
     * - DENIED  → invoke onResult(false) immediately
     * - NOT_ASKED → store callback, show dialog so the user can decide
     */
    fun onNotificationPermissionRequested(onResult: (Boolean) -> Unit) {
        val app = currentApp()
        when (app.notificationPermission) {
            NotificationPermission.GRANTED -> onResult(true)
            NotificationPermission.DENIED -> onResult(false)
            NotificationPermission.NOT_ASKED -> {
                pendingPermissionResult = onResult
                _permissionDialog.value = PermissionDialogState.Shown(app.name)
            }
        }
    }

    /**
     * Called by the Activity when the user taps Allow or Not Now in the permission dialog.
     * No-ops if the dialog is not currently shown — guards against double-invocation when
     * Material3 fires onDismissRequest after a button click.
     */
    fun onPermissionDialogResult(granted: Boolean) {
        if (_permissionDialog.value !is PermissionDialogState.Shown) return
        val cb = pendingPermissionResult
        pendingPermissionResult = null
        val newPermission = if (granted) NotificationPermission.GRANTED else NotificationPermission.DENIED
        val updated = currentApp().copy(notificationPermission = newPermission)
        _uiState.update { it.copy(app = updated) }
        viewModelScope.launch(Dispatchers.IO) { saveWebApp(updated) }
        cb?.invoke(granted)
        _permissionDialog.value = PermissionDialogState.Hidden
    }

    /**
     * Called when the engine or JS bridge delivers a notification.
     * Delegates to PwaNotificationDispatcher for all gating and posting logic.
     */
    fun onNotificationReceived(title: String, body: String?, iconUrl: String?, tag: String?) {
        val app = currentApp()
        val disp = notificationDispatcher ?: return
        viewModelScope.launch {
            val result = disp.dispatch(app, title, body, iconUrl, tag)
            if (result == PwaNotificationDispatcher.DispatchResult.Dropped.NotAsked) {
                onNotificationPermissionRequested { granted ->
                    if (granted) {
                        viewModelScope.launch { disp.dispatch(currentApp(), title, body, iconUrl, tag) }
                    }
                }
            }
        }
    }

    private fun currentApp(): WebApp = _uiState.value.app ?: initialApp

    class Factory(
        private val app: WebApp,
        private val services: WebViewServiceProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WebViewViewModel(
                initialApp = app,
                isolationManager = services.isolationManager,
                saveWebApp = services.saveWebApp,
                passwordManager = services.passwordManager,
                notificationDispatcher = services.notificationDispatcher,
                deleteAllAppsUseCase = services.deleteAllApps,
                getWebApps = services.getWebApps,
                themeManager = services.themeManager,
                torManager = services.torManager,
            ) as T
    }
}
