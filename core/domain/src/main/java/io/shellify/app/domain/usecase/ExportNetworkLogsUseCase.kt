package io.shellify.app.domain.usecase

import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.domain.repository.NetworkRequestLogRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportNetworkLogsUseCase(private val repo: NetworkRequestLogRepository) {

    suspend operator fun invoke(appId: Long): String {
        val logs = repo.getByApp(appId).first()
        return formatLogs(logs)
    }

    private fun formatLogs(logs: List<NetworkRequestLog>): String {
        val timestampFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sessionHeaderFmt = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("Shellify Network Log")
        sb.appendLine("Exported: ${timestampFmt.format(Date())}")
        if (logs.isEmpty()) {
            sb.appendLine("(no records)")
            return sb.toString()
        }
        val bySession = logs.groupBy { it.sessionId }
        bySession.entries
            .sortedByDescending { (_, entries) -> entries.minOfOrNull { it.timestamp } ?: 0L }
            .forEach { (_, entries) ->
                val sessionStart = entries.minOfOrNull { it.timestamp } ?: 0L
                sb.appendLine()
                sb.appendLine("--- ${sessionHeaderFmt.format(Date(sessionStart))} ---")
                entries.sortedBy { it.timestamp }.forEach { log ->
                    val status = if (log.isBlocked) "BLOCKED" else "ALLOWED"
                    sb.appendLine("${timestampFmt.format(Date(log.timestamp))}\t$status\t${log.url}")
                }
            }
        return sb.toString()
    }
}
