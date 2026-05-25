package io.shellify.app.core.engine

sealed class TorState {
    data object Stopped : TorState()
    data object Connecting : TorState()
    data object Ready : TorState()
    data class Error(val message: String) : TorState()
}
