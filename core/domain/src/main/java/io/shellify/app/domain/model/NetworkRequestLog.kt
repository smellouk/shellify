package io.shellify.app.domain.model

data class NetworkRequestLog(
    val id: Long = 0,
    val appId: Long,
    val sessionId: String,
    val hostname: String,
    val url: String,
    val isBlocked: Boolean,
    val timestamp: Long,
)
