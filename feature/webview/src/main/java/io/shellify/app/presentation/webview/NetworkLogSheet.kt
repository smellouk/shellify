package io.shellify.app.presentation.webview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.presentation.theme.Dimens
import io.shellify.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkLogSheet(
    sessionLog: List<NetworkRequestLog>,
    isGeckoEngine: Boolean,
    onDismiss: () -> Unit,
    onClearSession: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(start = Dimens.spaceLg, end = Dimens.spaceXs),
        ) {
            Text(
                stringResource(R.string.network_log_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (onClearSession != null && sessionLog.isNotEmpty()) {
                IconButton(onClick = onClearSession) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.network_log_clear_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
        )
        if (sessionLog.isEmpty()) {
            NetworkLogEmptyState()
        } else {
            NetworkLogContent(sessionLog = sessionLog)
        }
        if (isGeckoEngine) {
            Text(
                text = stringResource(R.string.network_log_gecko_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Dimens.spaceLg),
            )
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun NetworkLogEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceXl),
    ) {
        Text(
            text = stringResource(R.string.network_log_empty_title),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.network_log_empty_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.spaceSm),
        )
    }
}

@Composable
private fun NetworkLogContent(sessionLog: List<NetworkRequestLog>) {
    val grouped = sessionLog.groupBy { it.hostname }

    LazyColumn {
        items(grouped.entries.toList(), key = { it.key }) { (hostname, entries) ->
            val hasBlocked = entries.any { it.isBlocked }
            var isExpanded by remember(hostname) { mutableStateOf(false) }

            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = stringResource(R.string.network_log_blocked_cd),
                        tint = if (hasBlocked) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    )
                },
                headlineContent = {
                    Text(
                        text = hostname,
                        color = if (hasBlocked) MaterialTheme.colorScheme.error
                        else LocalContentColor.current,
                    )
                },
                trailingContent = {
                    Text(
                        text = stringResource(R.string.network_log_request_count, entries.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier = Modifier.clickable { isExpanded = !isExpanded },
            )
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    entries.forEach { entry ->
                        Text(
                            text = entry.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceXxs),
                        )
                    }
                }
            }
        }
    }
}
