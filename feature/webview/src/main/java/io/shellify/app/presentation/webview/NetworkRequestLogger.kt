package io.shellify.app.presentation.webview

import android.net.Uri
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.usecase.ClearNetworkLogsUseCase
import io.shellify.app.domain.usecase.LogNetworkRequestUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class NetworkRequestLogger(
    private val appId: Long,
    private val logNetworkRequest: LogNetworkRequestUseCase,
    private val clearNetworkLogs: ClearNetworkLogsUseCase,
) {

    val sessionId: String = UUID.randomUUID().toString()

    private val _sessionLog = MutableStateFlow<List<NetworkRequestLog>>(emptyList())
    val sessionLog: StateFlow<List<NetworkRequestLog>> = _sessionLog.asStateFlow()

    // IO scope for fire-and-forget DB writes — supervised so individual write failures
    // do not cancel subsequent writes (T-22-04-02 DoS mitigation).
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun onRequestIntercepted(url: String, blocked: Boolean) {
        val hostname = Uri.parse(url).host ?: url
        val entry = NetworkRequestLog(
            appId = appId,
            sessionId = sessionId,
            hostname = hostname,
            url = url,
            isBlocked = blocked,
            timestamp = System.currentTimeMillis(),
        )
        _sessionLog.update { it + entry }
        scope.launch { logNetworkRequest(entry) }
    }

    fun clearSession() {
        // Wipe both the live in-memory view and the persisted DB rows so that the
        // control center and the history screen stay consistent after a clear.
        _sessionLog.update { emptyList() }
        scope.launch { clearNetworkLogs(appId) }
    }

    fun cancel() {
        scope.cancel()
    }
}
