package io.shellify.app.presentation.webview

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnKeyOff
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.shellify.app.core.engine.TorState
import io.shellify.app.presentation.components.ConfirmDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.theme.Dimens
import io.shellify.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewControlCenter(
    pwaApp: WebApp,
    hasGlobalPassword: Boolean,
    torState: TorState = TorState.Stopped,
    onAdBlockChanged: (Boolean) -> Unit,
    onTranslateChanged: (Boolean) -> Unit,
    onFullscreenChanged: (Boolean) -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onClearData: () -> Unit,
    onNetworkLogClick: () -> Unit,
    onNewTorIdentity: () -> Unit = {},
    onPanic: () -> Unit = {},
    isReadingModeActive: Boolean = false,
    onReadingModeToggled: () -> Unit = {},
    customProxyState: CustomProxyState = CustomProxyState.None,
    onDisableProxy: () -> Unit = {},
    onRetryProxy: () -> Unit = {},
) {
    var showSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(Dimens.spaceLg),
        contentAlignment = Alignment.BottomEnd,
    ) {
        SmallFloatingActionButton(
            onClick = { showSheet = true },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.webview_controls_fab_cd))
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            WebViewControlCenterSheet(
                pwaApp = pwaApp,
                hasGlobalPassword = hasGlobalPassword,
                torState = torState,
                onAdBlockChanged = onAdBlockChanged,
                onTranslateChanged = onTranslateChanged,
                onFullscreenChanged = onFullscreenChanged,
                onLockChanged = onLockChanged,
                onClearData = { showSheet = false; onClearData() },
                onNetworkLogClick = { showSheet = false; onNetworkLogClick() },
                onNewTorIdentity = { showSheet = false; onNewTorIdentity() },
                onPanic = { showSheet = false; onPanic() },
                isReadingModeActive = isReadingModeActive,
                onReadingModeToggled = { onReadingModeToggled(); showSheet = false },
                customProxyState = customProxyState,
                onDisableProxy = { showSheet = false; onDisableProxy() },
                onRetryProxy = { showSheet = false; onRetryProxy() },
            )
        }
    }
}

@Composable
fun WebViewControlCenterSheet(
    pwaApp: WebApp,
    hasGlobalPassword: Boolean,
    torState: TorState = TorState.Stopped,
    onAdBlockChanged: (Boolean) -> Unit,
    onTranslateChanged: (Boolean) -> Unit,
    onFullscreenChanged: (Boolean) -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onClearData: () -> Unit,
    onNetworkLogClick: () -> Unit,
    onNewTorIdentity: () -> Unit = {},
    onPanic: () -> Unit = {},
    isReadingModeActive: Boolean = false,
    onReadingModeToggled: () -> Unit = {},
    customProxyState: CustomProxyState = CustomProxyState.None,
    onDisableProxy: () -> Unit = {},
    onRetryProxy: () -> Unit = {},
) {
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showPanicDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
    // Header: sheet title on the left; Tor state icons on the right (only for Tor apps).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceXxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.webview_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (pwaApp.useTor) {
            val context = LocalContext.current
            val (torIcon, torLabel, torTint) = torStateDisplay(torState)
            val circuitTint = if (torState is TorState.Ready) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
            val circuitLabel = if (torState is TorState.Ready) {
                stringResource(R.string.webview_tor_circuit_active)
            } else {
                stringResource(R.string.webview_tor_no_circuit)
            }
            val newIdentityLabel = stringResource(R.string.webview_control_new_tor_identity)
            // Card matches SurfaceCard: cornerXl, surface color, 1dp outlineVariant border.
            Card(
                shape = RoundedCornerShape(Dimens.cornerXl),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(Dimens.borderDefault, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = Dimens.spaceSm, vertical = Dimens.spaceXxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                ) {
                    // Onion — 20% larger, full emoji colors preserved (no tint).
                    Icon(
                        painter = painterResource(R.drawable.ic_tor_onion),
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.sizeSm * 1.2f),
                        tint = Color.Unspecified,
                    )
                    VerticalDivider(modifier = Modifier.width(Dimens.borderDefault))
                    // Status icons grouped together with tighter spacing.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                    ) {
                        Icon(
                            torIcon,
                            contentDescription = torLabel,
                            tint = torTint,
                            modifier = Modifier
                                .size(Dimens.sizeSm)
                                .clickable { Toast.makeText(context, torLabel, Toast.LENGTH_SHORT).show() },
                        )
                        Icon(
                            Icons.Default.Route,
                            contentDescription = circuitLabel,
                            tint = circuitTint,
                            modifier = Modifier
                                .size(Dimens.sizeSm)
                                .clickable { Toast.makeText(context, circuitLabel, Toast.LENGTH_SHORT).show() },
                        )
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = newIdentityLabel,
                            modifier = Modifier
                                .size(Dimens.sizeSm)
                                .clickable {
                                    Toast.makeText(context, newIdentityLabel, Toast.LENGTH_SHORT).show()
                                    onNewTorIdentity()
                                },
                        )
                    }
                }
            }
        }
        // Proxy card — shown when a custom proxy is active for this session (PRX-13).
        if (customProxyState != CustomProxyState.None) {
            val context = LocalContext.current
            val disableLabel = stringResource(R.string.webview_proxy_disable)
            val retryLabel = stringResource(R.string.webview_proxy_retry)
            Card(
                shape = RoundedCornerShape(Dimens.cornerXl),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(Dimens.borderDefault, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = Dimens.spaceSm, vertical = Dimens.spaceXxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                ) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.sizeSm),
                    )
                    VerticalDivider(modifier = Modifier.width(Dimens.borderDefault))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                    ) {
                        val statusIcon = if (customProxyState is CustomProxyState.Unreachable) {
                            Icons.Default.Warning
                        } else {
                            Icons.Default.CheckCircle
                        }
                        val statusTint = if (customProxyState is CustomProxyState.Unreachable) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        val statusLabel = stringResource(
                            if (customProxyState is CustomProxyState.Unreachable) {
                                R.string.webview_proxy_unreachable
                            } else {
                                R.string.webview_proxy_active
                            }
                        )
                        Icon(
                            statusIcon,
                            contentDescription = statusLabel,
                            tint = statusTint,
                            modifier = Modifier
                                .size(Dimens.sizeSm)
                                .clickable { Toast.makeText(context, statusLabel, Toast.LENGTH_SHORT).show() },
                        )
                        if (customProxyState is CustomProxyState.Unreachable) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = retryLabel,
                                modifier = Modifier
                                    .size(Dimens.sizeSm)
                                    .clickable { onRetryProxy() },
                            )
                        }
                        Icon(
                            Icons.Default.PublicOff,
                            contentDescription = disableLabel,
                            modifier = Modifier
                                .size(Dimens.sizeSm)
                                .clickable {
                                    Toast.makeText(context, disableLabel, Toast.LENGTH_SHORT).show()
                                    onDisableProxy()
                                },
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Default.Shield, null) },
        headlineContent = { Text(stringResource(R.string.webview_control_adblock)) },
        trailingContent = {
            Switch(checked = pwaApp.adBlockEnabled, onCheckedChange = onAdBlockChanged)
        },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Default.GTranslate, null) },
        headlineContent = { Text(stringResource(R.string.webview_control_translate)) },
        trailingContent = {
            Switch(checked = pwaApp.translateEnabled, onCheckedChange = onTranslateChanged)
        },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Default.Fullscreen, null) },
        headlineContent = { Text(stringResource(R.string.webview_control_fullscreen)) },
        trailingContent = {
            Switch(checked = pwaApp.isFullscreen, onCheckedChange = onFullscreenChanged)
        },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(
                if (pwaApp.lockType != LockType.NONE) Icons.Default.Lock else Icons.Default.LockOpen,
                null,
                tint = if (hasGlobalPassword) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        },
        headlineContent = {
            Text(
                stringResource(R.string.webview_control_applock),
                color = if (hasGlobalPassword) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        },
        supportingContent = if (!hasGlobalPassword) ({
            Text(
                stringResource(R.string.webview_applock_disabled_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }) else null,
        trailingContent = {
            Switch(
                checked = pwaApp.lockType != LockType.NONE,
                onCheckedChange = onLockChanged,
                enabled = hasGlobalPassword,
            )
        },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Default.Shield, null) },
        headlineContent = { Text(stringResource(R.string.webview_control_network_log)) },
        modifier = Modifier.clickable { onNetworkLogClick() },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null) },
        headlineContent = {
            Text(
                stringResource(
                    if (isReadingModeActive) R.string.webview_control_reading_mode_exit
                    else R.string.webview_control_reading_mode
                )
            )
        },
        modifier = Modifier.clickable { onReadingModeToggled() },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
        headlineContent = {
            Text(stringResource(R.string.webview_control_clear_data), color = MaterialTheme.colorScheme.error)
        },
        modifier = Modifier.clickable { showClearDataDialog = true },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
        },
        headlineContent = {
            Text(stringResource(R.string.webview_control_panic), color = MaterialTheme.colorScheme.error)
        },
        modifier = Modifier.clickable { showPanicDialog = true },
    )
    Spacer(Modifier.navigationBarsPadding())
    } // Column

    if (showClearDataDialog) {
        ConfirmDialog(
            title = stringResource(R.string.home_clear_data_title),
            body = stringResource(R.string.home_clear_data_body, pwaApp.name),
            confirmLabel = stringResource(R.string.home_clear_data_button),
            onConfirm = { showClearDataDialog = false; onClearData() },
            onDismiss = { showClearDataDialog = false },
            icon = Icons.Default.Delete,
            isDestructive = true,
        )
    }

    if (showPanicDialog) {
        ConfirmDialog(
            title = stringResource(R.string.webview_panic_confirm_title),
            body = stringResource(R.string.webview_panic_confirm_body),
            confirmLabel = stringResource(R.string.webview_panic_confirm_button),
            onConfirm = { showPanicDialog = false; onPanic() },
            onDismiss = { showPanicDialog = false },
            icon = Icons.Default.DeleteForever,
            isDestructive = true,
        )
    }
}

/**
 * Maps a [TorState] to the triple of (icon, label string resource id label, tint) used
 * in the control-center Tor status row.
 */
@Composable
private fun torStateDisplay(torState: TorState): Triple<ImageVector, String, Color> = when (torState) {
    is TorState.Ready -> Triple(
        Icons.Default.CheckCircle,
        stringResource(R.string.webview_tor_connected),
        MaterialTheme.colorScheme.primary,
    )
    is TorState.Connecting -> Triple(
        Icons.Default.VpnLock,
        stringResource(R.string.webview_tor_connecting_short),
        MaterialTheme.colorScheme.tertiary,
    )
    is TorState.Error -> Triple(
        Icons.Default.Warning,
        stringResource(R.string.webview_tor_connection_failed),
        MaterialTheme.colorScheme.error,
    )
    TorState.Stopped -> Triple(
        Icons.Default.VpnKeyOff,
        stringResource(R.string.webview_tor_stopped),
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
