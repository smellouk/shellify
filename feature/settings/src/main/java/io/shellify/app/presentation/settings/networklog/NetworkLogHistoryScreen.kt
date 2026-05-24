package io.shellify.app.presentation.settings.networklog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.shellify.app.presentation.components.EmptyStateIllustration
import io.shellify.app.presentation.theme.Dimens
import io.shellify.core.ui.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkLogHistoryScreen(
    viewModel: NetworkLogHistoryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    NetworkLogHistoryContent(
        state = state,
        onBack = onBack,
        onRequestClear = viewModel::requestClear,
        onConfirmClear = viewModel::confirmClear,
        onDismissClear = viewModel::dismissClear,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkLogHistoryContent(
    state: NetworkLogHistoryUiState,
    onBack: () -> Unit,
    onRequestClear: () -> Unit = {},
    onConfirmClear: () -> Unit = {},
    onDismissClear: () -> Unit = {},
) {
    if (state.showClearDialog) {
        AlertDialog(
            onDismissRequest = onDismissClear,
            title = { Text(stringResource(R.string.network_log_clear_title)) },
            text = { Text(stringResource(R.string.network_log_clear_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmClear) {
                    Text(stringResource(R.string.network_log_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClear) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_log_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    if (state.sessions.isNotEmpty()) {
                        IconButton(onClick = onRequestClear) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.network_log_clear_cd),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.sessions.isEmpty() -> {
                NetworkLogEmptyState(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                NetworkLogSessionList(
                    sessions = state.sessions,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun NetworkLogEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyStateIllustration(
            centerIcon = Icons.Default.Shield,
            modifier = Modifier.padding(top = Dimens.spaceXxl),
        )
        Text(
            text = stringResource(R.string.network_log_history_empty_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = Dimens.spaceLg),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.network_log_history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.spaceSm),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NetworkLogSessionList(
    sessions: List<SessionGroup>,
    modifier: Modifier = Modifier,
) {
    // Accordion: one expanded hostname per session — null means none open.
    val expandedBySession = remember { mutableStateMapOf<String, String?>() }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        sessions.forEach { group ->
            item(key = "header-${group.sessionId}") {
                Text(
                    text = stringResource(
                        R.string.network_log_session_header,
                        formatSessionDate(group.startedAt),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
                )
            }
            val domainGroups = group.entries.groupBy { it.hostname }
            items(
                items = domainGroups.entries.toList(),
                key = { (hostname, _) -> "${group.sessionId}-$hostname" },
            ) { (hostname, domainEntries) ->
                val hasBlocked = domainEntries.any { it.isBlocked }
                val expanded = expandedBySession[group.sessionId] == hostname
                val requestCount = domainEntries.size
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = stringResource(R.string.network_log_blocked_cd),
                            tint = if (hasBlocked) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    headlineContent = {
                        Text(
                            text = hostname,
                            color = if (hasBlocked) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    trailingContent = {
                        Text(
                            text = stringResource(R.string.network_log_request_count, requestCount),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    modifier = Modifier.clickable {
                        // Accordion: open this domain, close it if already open.
                        expandedBySession[group.sessionId] = if (expanded) null else hostname
                    },
                )
                AnimatedVisibility(visible = expanded) {
                    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    Column(modifier = Modifier.padding(bottom = Dimens.spaceSm)) {
                        domainEntries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = Dimens.spaceXl,
                                        end = Dimens.spaceLg,
                                        top = Dimens.spaceXxs,
                                        bottom = Dimens.spaceXxs,
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                            ) {
                                Text(
                                    text = timeFmt.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.isBlocked) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Text(
                                    text = entry.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (entry.isBlocked) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.network_log_history_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val sessionDateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

private fun formatSessionDate(timestamp: Long): String = sessionDateFormat.format(Date(timestamp))
