package io.shellify.app.data.mapper

import io.shellify.app.data.local.entity.NetworkRequestLogEntity
import io.shellify.app.domain.model.NetworkRequestLog

fun NetworkRequestLogEntity.toDomain(): NetworkRequestLog = NetworkRequestLog(
    id = id,
    appId = appId,
    sessionId = sessionId,
    hostname = hostname,
    url = url,
    isBlocked = isBlocked,
    timestamp = timestamp,
)

fun NetworkRequestLog.toEntity(): NetworkRequestLogEntity = NetworkRequestLogEntity(
    id = id,
    appId = appId,
    sessionId = sessionId,
    hostname = hostname,
    url = url,
    isBlocked = isBlocked,
    timestamp = timestamp,
)
