package io.shellify.app.presentation.webview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import io.shellify.app.presentation.components.ConfirmDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onAdBlockChanged: (Boolean) -> Unit,
    onTranslateChanged: (Boolean) -> Unit,
    onFullscreenChanged: (Boolean) -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onClearData: () -> Unit,
    onNetworkLogClick: () -> Unit,
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
                onAdBlockChanged = onAdBlockChanged,
                onTranslateChanged = onTranslateChanged,
                onFullscreenChanged = onFullscreenChanged,
                onLockChanged = onLockChanged,
                onClearData = { showSheet = false; onClearData() },
                onNetworkLogClick = { showSheet = false; onNetworkLogClick() },
            )
        }
    }
}

@Composable
fun WebViewControlCenterSheet(
    pwaApp: WebApp,
    hasGlobalPassword: Boolean,
    onAdBlockChanged: (Boolean) -> Unit,
    onTranslateChanged: (Boolean) -> Unit,
    onFullscreenChanged: (Boolean) -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onClearData: () -> Unit,
    onNetworkLogClick: () -> Unit,
) {
    var showClearDataDialog by remember { mutableStateOf(false) }

    Text(
        stringResource(R.string.webview_sheet_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceXxs),
    )
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
        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
        headlineContent = {
            Text(stringResource(R.string.webview_control_clear_data), color = MaterialTheme.colorScheme.error)
        },
        modifier = Modifier.clickable { showClearDataDialog = true },
    )
    Spacer(Modifier.navigationBarsPadding())

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
}
