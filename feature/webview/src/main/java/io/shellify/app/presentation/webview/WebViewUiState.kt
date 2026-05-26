package io.shellify.app.presentation.webview

import io.shellify.app.core.engine.TorState
import io.shellify.app.domain.model.WebApp

data class WebViewUiState(
    val app: WebApp? = null,
    val error: WebLoadError? = null,
    val isRetrying: Boolean = false,
    val authState: AuthState = AuthState.Loading,
    val isPageLoaded: Boolean = false,
    val showPanicConfirm: Boolean = false,
    val torState: TorState = TorState.Stopped,
    val isReadingModeActive: Boolean = false,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object Authenticated : AuthState
    data class PasswordRequired(
        val hash: String,
        val failedAttempts: Int,
        val wipeEnabled: Boolean,
    ) : AuthState
    data object SystemLockRequired : AuthState
}

sealed interface WebViewCommand {
    data class LoadUrl(val url: String) : WebViewCommand
    data object Reload : WebViewCommand
    data object Finish : WebViewCommand
    data object PageFinished : WebViewCommand
    data object NavigateHome : WebViewCommand
    data object NewTorIdentityRequested : WebViewCommand
    data object LoadReadingMode : WebViewCommand
}
