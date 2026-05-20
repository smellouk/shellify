package io.shellify.app.presentation.webview

sealed class WebLoadError {
    object NoInternet : WebLoadError()
    object CannotReach : WebLoadError()
    object SslError : WebLoadError()
    object Timeout : WebLoadError()
    data class Generic(val description: String) : WebLoadError()

    companion object {
        fun from(errorCode: Int, description: String): WebLoadError {
            val d = description.lowercase()
            return when {
                "err_internet_disconnected" in d || "err_network_changed" in d ||
                    "err_network_io_suspended" in d -> NoInternet
                "err_name_not_resolved" in d || "err_address_unreachable" in d ||
                    "err_connection_refused" in d || "err_connection_reset" in d -> CannotReach
                "err_timed_out" in d || "err_connection_timed_out" in d -> Timeout
                "err_ssl" in d || errorCode == -11 -> SslError
                else -> Generic(description)
            }
        }
    }
}
