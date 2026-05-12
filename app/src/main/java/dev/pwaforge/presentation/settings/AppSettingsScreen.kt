package dev.pwaforge.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pwaforge.R
import dev.pwaforge.presentation.theme.Dimens
import dev.pwaforge.core.shortcut.PwaShortcutManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onOpenTranslate: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val app = state.app
    val lockPasswordLabel = stringResource(R.string.settings_lock_password)
    val lockSystemLabel = stringResource(R.string.settings_lock_system)

    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    val screenBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = { Text(app?.name ?: stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) }
                },
            )
        },
    ) { padding ->
        if (app == null) return@Scaffold

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            SectionLabel(stringResource(R.string.settings_display))
            SettingsCard {
                ToggleListItem(stringResource(R.string.settings_fullscreen), app.isFullscreen, viewModel::toggleFullscreen,
                    icon = { Icon(Icons.Default.Fullscreen, null) })
            }

            SectionLabel(stringResource(R.string.settings_privacy))
            SettingsCard {
                ToggleListItem(stringResource(R.string.settings_adblock), app.adBlockEnabled, viewModel::toggleAdBlock,
                    icon = { Icon(Icons.Default.Shield, null) })
                HorizontalDivider()
                ToggleListItem(stringResource(R.string.settings_translate), app.translateEnabled, viewModel::toggleTranslate,
                    icon = { Icon(Icons.Default.GTranslate, null) })
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_translate_lang)) },
                    trailingContent = {
                        TextButton(onClick = onOpenTranslate) {
                            Icon(Icons.Default.GTranslate, null)
                            Text(app.translateTarget.displayName)
                        }
                    },
                )
            }

            SectionLabel(stringResource(R.string.settings_shortcut))
            SettingsCard {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_create_shortcut)) },
                    supportingContent = { Text(stringResource(R.string.settings_create_shortcut_desc)) },
                    trailingContent = {
                        IconButton(onClick = { PwaShortcutManager.createShortcut(context, app) }) {
                            Icon(Icons.AutoMirrored.Filled.Shortcut, stringResource(R.string.settings_create_shortcut_cd))
                        }
                    },
                )
            }

            SectionLabel(stringResource(R.string.settings_security))
            SettingsCard {
                ListItem(
                    leadingContent = {
                        Icon(
                            if (app.lockType != dev.pwaforge.domain.model.LockType.NONE) Icons.Default.Lock else Icons.Default.LockOpen,
                            null,
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.settings_applock)) },
                    trailingContent = {
                        Switch(
                            checked = app.lockType != dev.pwaforge.domain.model.LockType.NONE,
                            onCheckedChange = { on ->
                                viewModel.setLockType(
                                    if (on) dev.pwaforge.domain.model.LockType.PASSWORD
                                    else dev.pwaforge.domain.model.LockType.NONE
                                )
                            },
                        )
                    },
                )
                if (app.lockType != dev.pwaforge.domain.model.LockType.NONE) {
                    HorizontalDivider()
                    listOf(
                        dev.pwaforge.domain.model.LockType.PASSWORD to lockPasswordLabel,
                        dev.pwaforge.domain.model.LockType.SYSTEM   to lockSystemLabel,
                    ).forEach { (type, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.setLockType(type) }
                                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                        ) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                androidx.compose.material3.RadioButton(
                                    selected = app.lockType == type,
                                    onClick = { viewModel.setLockType(type) },
                                    modifier = Modifier.size(Dimens.sizeMd),
                                )
                            }
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(Dimens.spaceLg))
            SectionLabel(stringResource(R.string.settings_danger_zone))

            Button(
                onClick = viewModel::showDeleteDialog,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Delete, null)
                Text(stringResource(R.string.settings_delete_app))
            }
        }

        if (state.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteDialog,
                title = { Text(stringResource(R.string.settings_delete_confirm, app.name)) },
                text = { Text(stringResource(R.string.settings_delete_confirm_body)) },
                confirmButton = {
                    TextButton(onClick = viewModel::deleteApp) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteDialog) { Text(stringResource(R.string.common_cancel)) }
                },
            )
        }

    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Dimens.spaceXxs))

@Composable
private fun SettingsCard(content: @Composable () -> Unit) =
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cornerXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { content() }

@Composable
private fun ToggleListItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    icon: @Composable (() -> Unit)? = null,
) = ListItem(
    headlineContent = { Text(label) },
    leadingContent = icon,
    trailingContent = { Switch(checked = checked, onCheckedChange = { onToggle() }) },
)
