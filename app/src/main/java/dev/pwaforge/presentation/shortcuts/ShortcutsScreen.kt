package dev.pwaforge.presentation.shortcuts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pwaforge.R
import dev.pwaforge.presentation.theme.Dimens
import dev.pwaforge.presentation.home.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsScreen(viewModel: ShortcutsViewModel) {
    val state by viewModel.uiState.collectAsState()

    val screenBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.shortcuts_title)) })
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.items.isEmpty() -> EmptyState(Modifier.fillMaxSize().padding(padding))
            else -> {
                var iconPickItem by remember { mutableStateOf<ShortcutItem?>(null) }
                val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
                    val item = iconPickItem ?: return@rememberLauncherForActivityResult
                    iconPickItem = null
                    if (uri != null) viewModel.applyPickedIcon(item, uri)
                }

                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(Dimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    items(state.items, key = { it.shortcutId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.cornerXl),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            ShortcutRow(
                                item = item,
                                onRename = { viewModel.startRename(item) },
                                onRefreshIcon = { viewModel.refreshIcon(item) },
                                onChangeIcon = {
                                    iconPickItem = item
                                    pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                                },
                                onRemove = { viewModel.showRemove(item) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.renameTarget != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRename,
            title = { Text(stringResource(R.string.shortcuts_rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = state.renameText,
                    onValueChange = viewModel::setRenameText,
                    label = { Text(stringResource(R.string.shortcuts_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmRename,
                    enabled = state.renameText.isNotBlank(),
                ) { Text(stringResource(R.string.shortcuts_rename_button)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRename) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (state.removeTarget != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRemove,
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text(stringResource(R.string.shortcuts_remove_dialog_title)) },
            text = {
                Text(stringResource(R.string.shortcuts_remove_body, state.removeTarget!!.label))
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmRemove,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.shortcuts_disable_button)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRemove) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun ShortcutRow(
    item: ShortcutItem,
    onRename: () -> Unit,
    onRefreshIcon: () -> Unit,
    onChangeIcon: () -> Unit,
    onRemove: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        leadingContent = { AppIcon(app = item.app, modifier = Modifier.size(Dimens.sizeApp)) },
        headlineContent = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                item.app.url.removePrefix("https://").removePrefix("http://"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.shortcuts_options_cd))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.shortcuts_menu_rename)) },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { showMenu = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.shortcuts_menu_change_icon)) },
                        leadingIcon = { Icon(Icons.Default.Image, null) },
                        onClick = { showMenu = false; onChangeIcon() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.shortcuts_menu_refresh_icon)) },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        onClick = { showMenu = false; onRefreshIcon() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.shortcuts_menu_remove)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onRemove() },
                    )
                }
            }
        },
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            modifier = Modifier.padding(horizontal = Dimens.spaceXxl),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Shortcut,
                contentDescription = null,
                modifier = Modifier.size(Dimens.sizeEmptyIcon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Text(
                stringResource(R.string.shortcuts_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.shortcuts_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
