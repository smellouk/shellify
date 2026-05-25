package io.shellify.app.core.engine

// Sealed class so the runtime cache equality is determined by data class structural equality.
// ProxyConfig.None is a singleton (object); Socks5 and Http are value types (data class).
sealed class ProxyConfig {
    data object None : ProxyConfig()
    data class Socks5(val host: String, val port: Int) : ProxyConfig()
    data class Http(val host: String, val port: Int) : ProxyConfig()
}
