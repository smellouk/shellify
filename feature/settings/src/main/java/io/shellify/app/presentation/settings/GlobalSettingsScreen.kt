package io.shellify.app.presentation.settings

import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.shellify.app.core.backup.BackupSchedule
import io.shellify.app.presentation.theme.ACCENT_COLORS
import io.shellify.app.presentation.components.ConfirmDialog
import io.shellify.app.presentation.components.SurfaceCard
import io.shellify.app.presentation.theme.Dimens
import io.shellify.app.presentation.theme.GeckoWarning
import io.shellify.app.presentation.theme.VerifiedGreen
import io.shellify.app.domain.model.EngineType
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.domain.model.UserAgentMode
import android.app.Activity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import io.shellify.core.ui.R
import io.shellify.app.core.theme.LocalThemeRevealState
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    viewModel: GlobalSettingsViewModel,
    onChangelog: () -> Unit = {},
    onLicenses: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val geckoInstallState by viewModel.geckoEngineManager.installState.collectAsState()
    val simpleIconsState by viewModel.simpleIconsManager.state.collectAsState()
    val geckoLatestVersion by viewModel.geckoEngineManager.latestVersion.collectAsState()
    var showUaDialog by remember { mutableStateOf(false) }

    // Trigger version check whenever the engine is installed
    androidx.compose.runtime.LaunchedEffect(geckoInstallState) {
        if (geckoInstallState is GeckoInstallState.Installed) viewModel.checkForGeckoUpdate()
    }
    var showScheduleDialog by remember { mutableStateOf(false) }
    val backupPwdTitle = stringResource(R.string.global_settings_backup_password_dialog_title)
    val backupPwdDesc = stringResource(R.string.global_settings_backup_password_dialog_desc)
    val importPwdTitle = stringResource(R.string.global_settings_import_password_dialog_title)
    val importPwdDesc = stringResource(R.string.global_settings_import_password_dialog_desc)
    val strSave = stringResource(R.string.common_save)
    val strRestore = stringResource(R.string.common_restore)
    val context = LocalContext.current

    // DataStore flows are reloaded in-place by BackupManager.reloadFromFile() after restore,
    // so no activity restart is needed — just clear the flag.
    androidx.compose.runtime.LaunchedEffect(state.restoreComplete) {
        if (state.restoreComplete) {
            viewModel.clearRestoreComplete()
            viewModel.repinShortcutsAfterRestore()
        }
    }
    val version = remember {
        @Suppress("DEPRECATION")
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
        }
            .getOrDefault("—")
    }

    // SAF launchers
    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { viewModel.setBackupDirectory(it) }
        }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.showImportDialog(it) }
    }
    val iconPackFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importSimpleIcons(it) }
    }

    val screenBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.global_settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
    ) { padding ->
        AnimatedVisibility(
            visible = state.isLoaded,
            enter = fadeIn(animationSpec = tween(160)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Dimens.spaceLg,
                        end = Dimens.spaceLg,
                        top = Dimens.spaceXxs,
                        bottom = Dimens.spaceLg
                    ),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {

                // ── Appearance ────────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.global_settings_section_appearance))
                SurfaceCard {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = Dimens.spaceLg,
                            vertical = Dimens.spaceMd
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space10),
                    ) {
                        Text(
                            stringResource(R.string.global_settings_theme_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val revealState = LocalThemeRevealState.current
                        val view = LocalView.current
                        val activity = context as? Activity
                        var buttonCenters by remember { mutableStateOf(mapOf<ThemeMode, Offset>()) }

                        val themeModes = listOf(
                            Triple(
                                ThemeMode.SYSTEM,
                                Icons.Default.BrightnessAuto,
                                stringResource(R.string.global_settings_theme_system)
                            ),
                            Triple(
                                ThemeMode.LIGHT,
                                Icons.Default.LightMode,
                                stringResource(R.string.global_settings_theme_light)
                            ),
                            Triple(
                                ThemeMode.DARK,
                                Icons.Default.DarkMode,
                                stringResource(R.string.global_settings_theme_dark)
                            ),
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themeModes.forEachIndexed { index, (mode, icon, label) ->
                                SegmentedButton(
                                    selected = state.themeMode == mode,
                                    onClick = {
                                        val center = buttonCenters[mode] ?: Offset.Zero
                                        if (revealState != null) {
                                            revealState.triggerReveal(
                                                center = center,
                                                switchToDark = mode == ThemeMode.DARK,
                                                view = view,
                                                window = activity?.window,
                                            ) { viewModel.setThemeMode(mode) }
                                        } else {
                                            viewModel.setThemeMode(mode)
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index,
                                        themeModes.size
                                    ),
                                    icon = {
                                        SegmentedButtonDefaults.Icon(active = state.themeMode == mode) {
                                            Icon(
                                                icon,
                                                null,
                                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                            )
                                        }
                                    },
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        val bounds = coords.boundsInRoot()
                                        buttonCenters = buttonCenters + (mode to Offset(
                                            bounds.left + bounds.width / 2,
                                            bounds.top + bounds.height / 2,
                                        ))
                                    },
                                ) { Text(label) }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                        ListItem(
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.4f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    stringResource(R.string.global_settings_dynamic_colors),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            supportingContent = { Text(stringResource(R.string.global_settings_dynamic_colors_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = state.dynamicColor,
                                    onCheckedChange = viewModel::setDynamicColor
                                )
                            },
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                    AccentColorRow(
                        current = state.accentColor,
                        onSelect = viewModel::setAccentColor,
                    )
                }

                // ── Icon Pack ─────────────────────────────────────────────────────
                IconPackCard(
                    state = simpleIconsState,
                    onDownload = viewModel::downloadSimpleIcons,
                    onImport = {
                        iconPackFilePicker.launch(
                            arrayOf(
                                "application/json",
                                "text/plain",
                                "*/*"
                            )
                        )
                    },
                    onRemove = viewModel::removeSimpleIcons,
                )

                // ── Browser ───────────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.global_settings_section_browser))
                SurfaceCard {
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.global_settings_default_ua),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = { Text(state.defaultUaMode.label) },
                        trailingContent = {
                            TextButton(onClick = {
                                showUaDialog = true
                            }) { Text(stringResource(R.string.common_change)) }
                        },
                    )
                }
                SurfaceCard {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = Dimens.spaceLg,
                            vertical = Dimens.spaceMd
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)
                    ) {
                        Text(
                            stringResource(R.string.global_settings_default_engine_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Dimens.spaceXxs))

                        // ── System WebView option ─────────────────────────────────
                        EngineOptionRow(
                            selected = state.defaultEngineType == EngineType.SYSTEM_WEBVIEW,
                            onClick = { viewModel.setDefaultEngineType(EngineType.SYSTEM_WEBVIEW) },
                            title = stringResource(R.string.global_settings_engine_webview_title),
                            hint = stringResource(R.string.global_settings_engine_webview_desc),
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spaceXxs))

                        // ── GeckoView option ──────────────────────────────────────
                        val geckoInstalled = geckoInstallState is GeckoInstallState.Installed
                        EngineOptionRow(
                            selected = state.defaultEngineType == EngineType.GECKOVIEW,
                            enabled = geckoInstalled,
                            onClick = {
                                if (geckoInstalled) viewModel.setDefaultEngineType(EngineType.GECKOVIEW)
                            },
                            title = stringResource(R.string.global_settings_engine_gecko_title),
                            hint = stringResource(R.string.global_settings_engine_gecko_desc),
                        )

                        // Download / status row — always visible
                        Spacer(Modifier.height(Dimens.spaceSm))
                        when (val gs = geckoInstallState) {
                            is GeckoInstallState.NotInstalled ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)
                                ) {
                                    Icon(
                                        Icons.Default.Language, null,
                                        modifier = Modifier.size(Dimens.sizeXs),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.global_settings_gecko_not_installed),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            stringResource(R.string.global_settings_gecko_download_size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = viewModel::installGeckoEngine) {
                                        Icon(
                                            Icons.Default.FileDownload,
                                            contentDescription = stringResource(R.string.global_settings_download_gecko_cd),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                            is GeckoInstallState.Downloading ->
                                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(Dimens.sizeXs),
                                            strokeWidth = Dimens.strokeMd
                                        )
                                        Text(
                                            "${gs.message}  ${(gs.progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = viewModel::cancelGeckoInstall) {
                                            Text(
                                                stringResource(R.string.common_cancel)
                                            )
                                        }
                                    }
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { gs.progress },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                            is GeckoInstallState.Installing ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(Dimens.sizeXs),
                                        strokeWidth = Dimens.strokeMd
                                    )
                                    Text(
                                        stringResource(R.string.global_settings_gecko_installing),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                            is GeckoInstallState.Installed -> {
                                val installedVer =
                                    viewModel.geckoEngineManager.getInstalledVersion() ?: "—"
                                val sizeMb = viewModel.geckoEngineManager.getInstalledSizeMb()
                                val hasUpdate = viewModel.geckoEngineManager.updateAvailable
                                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle, null,
                                            modifier = Modifier.size(Dimens.sizeXs),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                stringResource(
                                                    R.string.global_settings_gecko_version,
                                                    installedVer,
                                                    sizeMb
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (hasUpdate) Text(
                                                stringResource(
                                                    R.string.global_settings_gecko_update_available,
                                                    geckoLatestVersion ?: ""
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                                    ) {
                                        if (gs.verified) {
                                            val sha =
                                                viewModel.geckoEngineManager.getInstalledSha256()
                                            Icon(
                                                Icons.Default.VerifiedUser, null,
                                                modifier = Modifier.size(Dimens.sizeXs),
                                                tint = VerifiedGreen
                                            )
                                            Column {
                                                Text(
                                                    stringResource(R.string.global_settings_gecko_sha_verified),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = VerifiedGreen
                                                )
                                                if (sha != null) Text(
                                                    sha,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        } else {
                                            Icon(
                                                Icons.Default.Warning, null,
                                                modifier = Modifier.size(Dimens.sizeXs),
                                                tint = GeckoWarning
                                            )
                                            Text(
                                                stringResource(R.string.global_settings_gecko_sha_unverified),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GeckoWarning
                                            )
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (hasUpdate) Button(onClick = viewModel::updateGeckoEngine) {
                                            Text(
                                                stringResource(R.string.common_update)
                                            )
                                        }
                                        TextButton(
                                            onClick = viewModel::uninstallGeckoEngine,
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) { Text(stringResource(R.string.common_remove)) }
                                    }
                                }
                            }

                            is GeckoInstallState.Error ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)
                                ) {
                                    Icon(
                                        Icons.Default.Language, null,
                                        modifier = Modifier.size(Dimens.sizeXs),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        stringResource(
                                            R.string.global_settings_gecko_download_failed,
                                            gs.message
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = viewModel::installGeckoEngine) {
                                        Text(
                                            stringResource(R.string.common_retry)
                                        )
                                    }
                                }
                        }
                    }
                }

                // ── Security ──────────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.global_settings_section_security))
                SurfaceCard {
                    if (state.hasPassword) {
                        ListItem(
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.4f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    stringResource(R.string.global_settings_app_password_headline),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            supportingContent = { Text(stringResource(R.string.global_settings_password_set)) },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = viewModel::showChangePasswordDialog) {
                                        Text(
                                            stringResource(R.string.common_change)
                                        )
                                    }
                                    TextButton(
                                        onClick = viewModel::showRemovePasswordDialog,
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    ) { Text(stringResource(R.string.common_remove)) }
                                }
                            },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                        ListItem(
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.4f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.DeleteForever,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    stringResource(R.string.global_settings_wipe_on_failed),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            supportingContent = { Text(stringResource(R.string.global_settings_wipe_on_failed_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = state.wipeOnFailedAttempts,
                                    onCheckedChange = viewModel::setWipeOnFailedAttempts,
                                )
                            },
                        )
                    } else {
                        ListItem(
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.4f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.LockOpen,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    stringResource(R.string.global_settings_app_password_headline),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            supportingContent = { Text(stringResource(R.string.global_settings_password_not_set)) },
                            trailingContent = {
                                TextButton(onClick = viewModel::showSetPasswordDialog) {
                                    Text(
                                        stringResource(R.string.common_set)
                                    )
                                }
                            },
                        )
                    }
                }
                SurfaceCard {
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.NoPhotography,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.global_settings_screenshot_protection),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = { Text(stringResource(R.string.global_settings_screenshot_protection_desc)) },
                        trailingContent = {
                            Switch(
                                checked = state.screenshotProtection,
                                onCheckedChange = viewModel::setScreenshotProtection,
                            )
                        },
                    )
                }

                // ── Backup ────────────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.global_settings_section_backup))
                SurfaceCard {
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Backup,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.global_settings_encrypted_backup),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = {
                            Text(
                                if (state.backupEnabled) stringResource(R.string.global_settings_backup_enabled) else stringResource(
                                    R.string.global_settings_backup_disabled
                                )
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    filePicker.launch(
                                        arrayOf(
                                            "application/octet-stream",
                                            "*/*"
                                        )
                                    )
                                }) {
                                    Icon(
                                        Icons.Default.Restore,
                                        contentDescription = stringResource(R.string.global_settings_import_backup),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = state.backupEnabled,
                                    onCheckedChange = viewModel::setBackupEnabled,
                                )
                            }
                        },
                    )
                    AnimatedVisibility(
                        visible = state.backupEnabled,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))

                            // Password
                            ListItem(
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.4f
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Key,
                                            null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        stringResource(R.string.global_settings_backup_password_headline),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        if (state.backupHasPassword) stringResource(R.string.global_settings_backup_password_set) else stringResource(
                                            R.string.global_settings_backup_password_required
                                        )
                                    )
                                },
                                trailingContent = {
                                    TextButton(onClick = viewModel::showBackupPasswordDialog) {
                                        Text(
                                            if (state.backupHasPassword) stringResource(R.string.common_change) else stringResource(
                                                R.string.common_set
                                            )
                                        )
                                    }
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))

                            // Directory
                            ListItem(
                                modifier = Modifier.alpha(if (state.backupHasPassword) 1f else 0.38f),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.4f
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        stringResource(R.string.global_settings_backup_folder_headline),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        state.backupDirectoryUri?.let { uriToDisplayName(it) }
                                            ?: stringResource(R.string.global_settings_backup_folder_not_selected)
                                    )
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = { folderPicker.launch(null) },
                                        enabled = state.backupHasPassword,
                                    ) {
                                        Icon(
                                            Icons.Default.FolderOpen,
                                            contentDescription = stringResource(R.string.global_settings_select_folder_cd)
                                        )
                                    }
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))

                            // Schedule
                            val canBackup =
                                state.backupHasPassword && state.backupDirectoryUri != null
                            val scheduleLabel = when (state.backupSchedule) {
                                BackupSchedule.NONE -> stringResource(R.string.global_settings_schedule_disabled)
                                BackupSchedule.WEEKLY -> stringResource(R.string.global_settings_schedule_weekly)
                                BackupSchedule.MONTHLY -> stringResource(R.string.global_settings_schedule_monthly)
                            }
                            ListItem(
                                modifier = Modifier.alpha(if (canBackup) 1f else 0.38f),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.4f
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        stringResource(R.string.global_settings_backup_schedule_headline),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                supportingContent = {
                                    Text(scheduleLabel)
                                },
                                trailingContent = {
                                    TextButton(
                                        onClick = { showScheduleDialog = true },
                                        enabled = canBackup,
                                    ) { Text(stringResource(R.string.common_change)) }
                                },
                            )
                            if (state.backupLastTime > 0L) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                                ListItem(
                                    modifier = Modifier.alpha(if (canBackup) 1f else 0.38f),
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.4f
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.History,
                                                null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    headlineContent = {
                                        Text(
                                            stringResource(R.string.global_settings_last_backup_headline),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            DateFormat.getMediumDateFormat(context)
                                                .format(Date(state.backupLastTime)) +
                                                    " " + DateFormat.getTimeFormat(context)
                                                .format(Date(state.backupLastTime))
                                        )
                                    },
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))

                            // Actions
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = Dimens.spaceLg,
                                    vertical = Dimens.spaceMd
                                ),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                            ) {
                                Button(
                                    onClick = viewModel::backupNow,
                                    enabled = canBackup && !state.backupRunning,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (state.backupRunning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(Dimens.sizeSm),
                                            strokeWidth = Dimens.strokeMd,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(Dimens.spaceSm))
                                    }
                                    Icon(
                                        Icons.Default.Backup,
                                        null,
                                        modifier = Modifier.size(Dimens.sizeSm)
                                    )
                                    Spacer(Modifier.width(Dimens.spaceSm))
                                    Text(stringResource(R.string.global_settings_backup_now))
                                }
                            }
                        }
                    }
                }

                // ── Data ──────────────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.global_settings_section_data))
                SurfaceCard {
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Storage,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.global_settings_clear_all_data_headline),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = { Text(stringResource(R.string.global_settings_clear_all_data_desc)) },
                        trailingContent = {
                            IconButton(onClick = viewModel::showClearAllDialog) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = stringResource(R.string.global_settings_clear_all_cd),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Apps,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.global_settings_delete_all_apps_headline),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = { Text(stringResource(R.string.global_settings_delete_all_apps_desc)) },
                        trailingContent = {
                            IconButton(onClick = viewModel::showDeleteAllAppsDialog) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.global_settings_delete_all_categories_headline),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = { Text(stringResource(R.string.global_settings_delete_all_categories_desc)) },
                        trailingContent = {
                            IconButton(onClick = viewModel::showDeleteAllCategoriesDialog) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Shortcut,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.global_settings_delete_all_shortcuts_headline),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = { Text(stringResource(R.string.global_settings_delete_all_shortcuts_desc)) },
                        trailingContent = {
                            IconButton(onClick = viewModel::showDeleteAllShortcutsDialog) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                    )
                }

                // ── About ─────────────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.global_settings_section_about))
                SurfaceCard {
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.global_settings_version_headline),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Text(
                                version, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                    ListItem(
                        modifier = Modifier.clickable { onChangelog() },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.NewReleases,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.changelog_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                    ListItem(
                        modifier = Modifier.clickable { onLicenses() },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Article,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.licenses_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                    )
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showUaDialog) {
        ModalBottomSheet(onDismissRequest = { showUaDialog = false }) {
            Text(
                stringResource(R.string.global_settings_ua_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    start = Dimens.spaceLg,
                    end = Dimens.spaceLg,
                    bottom = Dimens.spaceSm
                ),
            )
            Column(modifier = Modifier.padding(horizontal = Dimens.spaceMd)) {
                @Suppress("DEPRECATION")
                UserAgentMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.cornerLg))
                            .clickable { viewModel.setDefaultUaMode(mode); showUaDialog = false }
                            .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.defaultUaMode == mode,
                            onClick = { viewModel.setDefaultUaMode(mode); showUaDialog = false })
                        Spacer(Modifier.width(Dimens.spaceSm))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(Modifier.height(Dimens.spaceXl))
        }
    }

    if (showScheduleDialog) {
        ModalBottomSheet(onDismissRequest = { showScheduleDialog = false }) {
            Text(
                stringResource(R.string.global_settings_schedule_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    start = Dimens.spaceLg,
                    end = Dimens.spaceLg,
                    bottom = Dimens.spaceSm
                ),
            )
            Column(modifier = Modifier.padding(horizontal = Dimens.spaceMd)) {
                BackupSchedule.entries.forEach { schedule ->
                    val label = when (schedule) {
                        BackupSchedule.NONE -> stringResource(R.string.global_settings_schedule_disabled)
                        BackupSchedule.WEEKLY -> stringResource(R.string.global_settings_schedule_weekly)
                        BackupSchedule.MONTHLY -> stringResource(R.string.global_settings_schedule_monthly)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.cornerLg))
                            .clickable {
                                viewModel.setBackupSchedule(schedule); showScheduleDialog = false
                            }
                            .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.backupSchedule == schedule,
                            onClick = {
                                viewModel.setBackupSchedule(schedule); showScheduleDialog = false
                            })
                        Spacer(Modifier.width(Dimens.spaceSm))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(Modifier.height(Dimens.spaceXl))
        }
    }

    if (state.showRemovePasswordWarning) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRemovePasswordWarning,
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.global_settings_remove_password_title)) },
            text = {
                Text(stringResource(R.string.global_settings_remove_password_warning))
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmRemovePasswordWarning,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.common_continue)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRemovePasswordWarning) {
                    Text(
                        stringResource(
                            R.string.common_cancel
                        )
                    )
                }
            },
        )
    }

    if (state.showPasswordDialog) {
        PasswordDialog(
            mode = state.passwordDialogMode,
            onDismiss = viewModel::dismissPasswordDialog,
            onSet = { password -> viewModel.setPassword(password) },
            onChange = { current, new, onError -> viewModel.changePassword(current, new, onError) },
            onRemove = { current, onError -> viewModel.removePassword(current, onError) },
        )
    }

    if (state.showBackupPasswordDialog) {
        SinglePasswordDialog(
            title = backupPwdTitle,
            description = backupPwdDesc,
            confirmLabel = strSave,
            onDismiss = viewModel::dismissBackupPasswordDialog,
            onConfirm = viewModel::setBackupPassword,
        )
    }

    if (state.showRestoreConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.global_settings_restore_confirm_title),
            body = stringResource(R.string.global_settings_restore_confirm_body),
            confirmLabel = stringResource(R.string.common_restore),
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::dismissRestoreConfirm,
            icon = Icons.Default.Restore,
            isDestructive = true,
        )
    }

    if (state.showImportPasswordDialog) {
        SinglePasswordDialog(
            title = importPwdTitle,
            description = importPwdDesc,
            confirmLabel = strRestore,
            onDismiss = viewModel::dismissImportDialog,
            onConfirm = viewModel::importBackup,
        )
    }

    if (state.showDeleteAllAppsDialog) {
        ConfirmDialog(
            title = stringResource(R.string.global_settings_delete_all_apps_confirm_title),
            body = stringResource(R.string.global_settings_delete_all_apps_confirm_body),
            confirmLabel = stringResource(R.string.common_delete_all),
            onConfirm = viewModel::deleteAllApps,
            onDismiss = viewModel::dismissDeleteAllAppsDialog,
            icon = Icons.Default.DeleteSweep,
            isDestructive = true,
        )
    }

    if (state.showDeleteAllCategoriesDialog) {
        ConfirmDialog(
            title = stringResource(R.string.global_settings_delete_all_categories_confirm_title),
            body = stringResource(R.string.global_settings_delete_all_categories_confirm_body),
            confirmLabel = stringResource(R.string.common_delete_all),
            onConfirm = viewModel::deleteAllCategories,
            onDismiss = viewModel::dismissDeleteAllCategoriesDialog,
            icon = Icons.Default.DeleteSweep,
            isDestructive = true,
        )
    }

    if (state.showDeleteAllShortcutsDialog) {
        ConfirmDialog(
            title = stringResource(R.string.global_settings_delete_all_shortcuts_confirm_title),
            body = stringResource(R.string.global_settings_delete_all_shortcuts_confirm_body),
            confirmLabel = stringResource(R.string.common_delete_all),
            onConfirm = viewModel::deleteAllShortcuts,
            onDismiss = viewModel::dismissDeleteAllShortcutsDialog,
            icon = Icons.Default.DeleteSweep,
            isDestructive = true,
        )
    }

    if (state.showClearAllDialog) {
        ConfirmDialog(
            title = stringResource(R.string.global_settings_clear_all_confirm_title),
            body = stringResource(R.string.global_settings_clear_all_confirm_body),
            confirmLabel = stringResource(R.string.common_clear_all),
            onConfirm = viewModel::clearAll,
            onDismiss = viewModel::dismissClearAllDialog,
            icon = Icons.Default.DeleteSweep,
            isDestructive = true,
        )
    }

    state.backupResultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearBackupMessage,
            icon = { Icon(Icons.Default.Backup, null) },
            title = { Text(stringResource(R.string.global_settings_backup_result_title)) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::clearBackupMessage) {
                    Text(
                        stringResource(R.string.common_ok)
                    )
                }
            },
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun SinglePasswordDialog(
    title: String,
    description: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val errTooShort = stringResource(R.string.global_settings_password_too_short)
    val errDontMatch = stringResource(R.string.global_settings_passwords_dont_match)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Card(
                    shape = RoundedCornerShape(Dimens.cornerXl),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        Dimens.borderDefault,
                        MaterialTheme.colorScheme.outlineVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.spaceMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; error = null },
                            label = { Text(stringResource(R.string.common_password)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { show = !show }) {
                                    Icon(
                                        if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null
                                    )
                                }
                            },
                        )
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { confirm = it; error = null },
                            label = { Text(stringResource(R.string.common_confirm_password)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = error != null,
                            supportingText = { if (error != null) Text(error!!) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    password.length < 4 -> error = errTooShort
                    password != confirm -> error = errDontMatch
                    else -> onConfirm(password)
                }
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun PasswordDialog(
    mode: PasswordDialogMode,
    onDismiss: () -> Unit,
    onSet: (String) -> Unit,
    onChange: (String, String, () -> Unit) -> Unit,
    onRemove: (String, () -> Unit) -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf<String?>(null) }
    var newError by remember { mutableStateOf<String?>(null) }

    val titleStr = stringResource(
        when (mode) {
            PasswordDialogMode.SET -> R.string.global_settings_set_password_title
            PasswordDialogMode.CHANGE -> R.string.global_settings_change_password_title
            PasswordDialogMode.REMOVE -> R.string.global_settings_remove_password_dialog_title
        }
    )
    val errCurrentRequired = stringResource(R.string.global_settings_enter_current_password)
    val errTooShort = stringResource(R.string.global_settings_password_too_short)
    val errDontMatch = stringResource(R.string.global_settings_passwords_dont_match)
    val errWrongPassword = stringResource(R.string.common_wrong_password)

    fun validate(): Boolean {
        currentError = null; newError = null
        if (mode == PasswordDialogMode.REMOVE) {
            if (currentPassword.isBlank()) {
                currentError = errCurrentRequired; return false
            }
            return true
        }
        if (mode == PasswordDialogMode.CHANGE && currentPassword.isBlank()) {
            currentError = errCurrentRequired; return false
        }
        if (newPassword.length < 4) {
            newError = errTooShort; return false
        }
        if (newPassword != confirmPassword) {
            newError = errDontMatch; return false
        }
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, null) },
        title = { Text(titleStr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                if (mode == PasswordDialogMode.CHANGE || mode == PasswordDialogMode.REMOVE) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it; currentError = null },
                        label = { Text(stringResource(R.string.common_current_password)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showCurrent = !showCurrent }) {
                                Icon(
                                    if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null
                                )
                            }
                        },
                        isError = currentError != null,
                        supportingText = { if (currentError != null) Text(currentError!!) },
                    )
                }
                if (mode == PasswordDialogMode.SET || mode == PasswordDialogMode.CHANGE) {
                    Card(
                        shape = RoundedCornerShape(Dimens.cornerXl),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            Dimens.borderDefault,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.spaceMd),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                        ) {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it; newError = null },
                                label = { Text(stringResource(R.string.common_new_password)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showNew = !showNew }) {
                                        Icon(
                                            if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            null
                                        )
                                    }
                                },
                                isError = newError != null,
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; newError = null },
                                label = { Text(stringResource(R.string.common_confirm_password)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = newError != null,
                                supportingText = { if (newError != null) Text(newError!!) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!validate()) return@TextButton
                    when (mode) {
                        PasswordDialogMode.SET -> onSet(newPassword)
                        PasswordDialogMode.CHANGE -> onChange(
                            currentPassword,
                            newPassword
                        ) { currentError = errWrongPassword }

                        PasswordDialogMode.REMOVE -> onRemove(currentPassword) {
                            currentError = errWrongPassword
                        }
                    }
                },
                colors = if (mode == PasswordDialogMode.REMOVE)
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.textButtonColors(),
            ) {
                Text(
                    if (mode == PasswordDialogMode.REMOVE) stringResource(R.string.common_remove) else stringResource(
                        R.string.common_save
                    )
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun uriToDisplayName(uriString: String): String = runCatching {
    val uri = android.net.Uri.parse(uriString)
    // SAF tree URI last path segment: "primary:Download/Shellify" or "XXXX-XXXX:Backups"
    val docId = uri.lastPathSegment ?: return@runCatching uriString
    val colon = docId.indexOf(':')
    if (colon < 0) return@runCatching docId
    val volume = docId.substring(0, colon)
    val path = docId.substring(colon + 1)
    if (volume.equals("primary", ignoreCase = true)) "/storage/emulated/0/$path"
    else "/storage/$volume/$path"
}.getOrDefault(uriString)

@Composable
private fun EngineOptionRow(
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    title: String,
    hint: String,
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Dimens.spaceXxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides 0.dp
            ) {
                RadioButton(
                    selected = selected,
                    onClick = if (enabled) onClick else null,
                    enabled = enabled,
                    modifier = Modifier.size(Dimens.sizeMd),
                )
            }
            Spacer(Modifier.width(Dimens.spaceMd))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold
                else androidx.compose.ui.text.font.FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
        }
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            modifier = Modifier.padding(start = Dimens.size4xl),
        )
    }
}


@Composable
internal fun AccentColorRow(current: Int?, onSelect: (Int?) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd)) {
        Text(
            stringResource(R.string.global_settings_accent_color),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.spaceSm))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            // "None" — uses system/dynamic primary
            Box(
                modifier = Modifier
                    .size(Dimens.sizeCard)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (current == null) Dimens.borderSelected else Dimens.borderDefault,
                        color = if (current == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = CircleShape,
                    )
                    .clickable { onSelect(null) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Palette, null,
                    modifier = Modifier.size(Dimens.sizeMd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ACCENT_COLORS.forEach { colorInt ->
                val color = Color(colorInt)
                val isSelected = current == colorInt
                Box(
                    modifier = Modifier
                        .size(Dimens.sizeCard)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) Dimens.borderSelected else Dimens.borderDefault,
                            color = if (isSelected) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape,
                        )
                        .clickable { onSelect(colorInt) },
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check, null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(Dimens.sizeMd),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackCard(
    state: SimpleIconsState,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onRemove: () -> Unit,
) {
    val iconColor = when (state) {
        is SimpleIconsState.Imported -> MaterialTheme.colorScheme.primary
        is SimpleIconsState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val iconVector = when (state) {
        is SimpleIconsState.Imported -> Icons.Default.CheckCircle
        is SimpleIconsState.Error -> Icons.Default.Warning
        else -> Icons.Default.AutoAwesome
    }

    SurfaceCard {
        when (state) {
            is SimpleIconsState.Imported -> {
                ListItem(
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                iconVector,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = iconColor
                            )
                        }
                    },
                    headlineContent = {
                        Text(
                            stringResource(R.string.global_settings_simple_icons_title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.global_settings_simple_icons_imported,
                                state.iconCount
                            )
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = onRemove,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text(stringResource(R.string.global_settings_simple_icons_remove)) }
                    },
                )
            }

            is SimpleIconsState.Processing -> {
                ListItem(
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = Dimens.strokeMd
                            )
                        }
                    },
                    headlineContent = {
                        Text(
                            stringResource(R.string.global_settings_icon_pack_headline),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.global_settings_simple_icons_processing))
                    },
                )
            }

            else -> {
                ListItem(
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                iconVector,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = iconColor
                            )
                        }
                    },
                    headlineContent = {
                        Text(
                            stringResource(R.string.global_settings_icon_pack_headline),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    supportingContent = {
                        Text(
                            when (state) {
                                is SimpleIconsState.Error -> state.message
                                else -> stringResource(R.string.global_settings_icon_pack_not_imported)
                            }
                        )
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                Column(
                    modifier = Modifier.padding(
                        horizontal = Dimens.spaceLg,
                        vertical = Dimens.spaceMd
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    when (state) {
                        is SimpleIconsState.Downloading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.sizeXs),
                                    strokeWidth = Dimens.strokeMd,
                                )
                                Text(
                                    stringResource(
                                        R.string.global_settings_simple_icons_downloading,
                                        (state.progress * 100).toInt()
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        else -> {
                            if (state is SimpleIconsState.NotImported) {
                                Text(
                                    stringResource(R.string.global_settings_simple_icons_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    stringResource(R.string.global_settings_simple_icons_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(Dimens.spaceXxs))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                                Button(onClick = onDownload) {
                                    Icon(
                                        Icons.Default.FileDownload,
                                        null,
                                        modifier = Modifier.size(Dimens.sizeSm)
                                    )
                                    Spacer(Modifier.width(Dimens.spaceXxs))
                                    Text(
                                        if (state is SimpleIconsState.Error)
                                            stringResource(R.string.global_settings_simple_icons_retry)
                                        else
                                            stringResource(R.string.global_settings_simple_icons_download)
                                    )
                                }
                                OutlinedButton(onClick = onImport) {
                                    Icon(
                                        Icons.Default.Restore,
                                        null,
                                        modifier = Modifier.size(Dimens.sizeSm)
                                    )
                                    Spacer(Modifier.width(Dimens.spaceXxs))
                                    Text(stringResource(R.string.global_settings_simple_icons_import))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = Dimens.letterSpacingCaps,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            top = Dimens.space10,
            bottom = 0.dp,
            start = Dimens.spaceXxs,
            end = Dimens.spaceXxs
        ),
    )

