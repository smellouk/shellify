package io.shellify.app.presentation.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WebViewViewModel(
    private val initialApp: WebApp,
    private val isolationManager: IsolationManager,
    private val saveWebApp: SaveWebAppUseCase,
    private val passwordManager: PasswordManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewUiState(app = initialApp))
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    private val _commands = MutableSharedFlow<WebViewCommand>(replay = 1, extraBufferCapacity = 16)
    val commands: SharedFlow<WebViewCommand> = _commands.asSharedFlow()

    private var loadFailed = false
    private val visitedUrls = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            val authState = resolveAuthState()
            _uiState.update { it.copy(authState = authState) }
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
        WebViewActivity.pageFinishedCallback?.invoke()
    }

    fun onError(code: Int, desc: String) {
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
            ) as T
    }
}
