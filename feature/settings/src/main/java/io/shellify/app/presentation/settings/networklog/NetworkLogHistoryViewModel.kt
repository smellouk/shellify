package io.shellify.app.presentation.settings.networklog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.usecase.ClearNetworkLogsUseCase
import io.shellify.app.domain.usecase.GetNetworkLogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NetworkLogHistoryUiState(
    val sessions: List<SessionGroup> = emptyList(),
    val isLoading: Boolean = true,
    val showClearDialog: Boolean = false,
)

data class SessionGroup(
    val sessionId: String,
    val entries: List<NetworkRequestLog>,
    val startedAt: Long,
)

class NetworkLogHistoryViewModel(
    private val appId: Long,
    private val getNetworkLog: GetNetworkLogUseCase,
    private val clearNetworkLogs: ClearNetworkLogsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkLogHistoryUiState())
    val uiState: StateFlow<NetworkLogHistoryUiState> = _state

    init {
        viewModelScope.launch {
            getNetworkLog(appId).collect { list ->
                val grouped = list
                    .groupBy { it.sessionId }
                    .entries
                    .map { (sid, entries) ->
                        SessionGroup(
                            sessionId = sid,
                            entries = entries,
                            startedAt = entries.minOf { it.timestamp },
                        )
                    }
                    .sortedByDescending { it.startedAt }
                _state.update { it.copy(sessions = grouped, isLoading = false) }
            }
        }
    }

    fun requestClear() = _state.update { it.copy(showClearDialog = true) }
    fun dismissClear() = _state.update { it.copy(showClearDialog = false) }
    fun confirmClear() {
        _state.update { it.copy(showClearDialog = false) }
        viewModelScope.launch { clearNetworkLogs(appId) }
    }

    class Factory(
        private val appId: Long,
        private val getNetworkLog: GetNetworkLogUseCase,
        private val clearNetworkLogs: ClearNetworkLogsUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NetworkLogHistoryViewModel(appId, getNetworkLog, clearNetworkLogs) as T
    }
}
