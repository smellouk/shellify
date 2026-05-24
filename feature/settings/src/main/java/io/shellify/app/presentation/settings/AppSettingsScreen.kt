package io.shellify.app.presentation.settings

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import io.shellify.app.presentation.components.ConfirmDialog
import io.shellify.app.presentation.components.SurfaceCard
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import io.shellify.core.ui.R
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.shortcut.PwaShortcutManager
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.presentation.theme.GeckoWarning
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.TranslateLanguage
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.settings.components.DndRangeRow
import io.shellify.app.presentation.settings.components.TimePickerHourDialog
import io.shellify.app.presentation.share.AppShareSheet
import io.shellify.app.presentation.components.SimpleIconPickerSheet
import io.shellify.app.presentation.components.AppIcon
import io.shellify.app.presentation.theme.Dimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onNavigateToHistory: (appId: Long) -> Unit = {},
    onNavigateToNetworkLog: (appId: Long) -> Unit = {},
    onStartBackgroundService: (appId: Long) -> Unit = {},
    onStopBackgroundService: (appId: Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val geckoInstallState by viewModel.geckoEngineManager.installState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshGlobalNotificationState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val scope = rememberCoroutineScope()
    val app = state.app
    val lockPasswordLabel = stringResource(R.string.settings_lock_password)
    val lockSystemLabel = stringResource(R.string.settings_lock_system)
    val themeColorLabel = stringResource(R.string.add_theme_color)
    val themeColorNotSet = stringResource(R.string.add_theme_color_not_set)

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showDndStartPicker by remember { mutableStateOf(false) }
    var showDndEndPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val path = context.contentResolver.openInputStream(uri)?.use { input ->
                val dir = java.io.File(context.filesDir, "icons").also { it.mkdirs() }
                val file = java.io.File(dir, "${app?.isolationId}.png")
                file.outputStream().use { input.copyTo(it) }
                file.absolutePath
            }
            if (path != null) viewModel.setIconPath(path)
        }
    }

    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    LaunchedEffect(Unit) {
        viewModel.commands.collect { command ->
            when (command) {
                is AppSettingsCommand.NavigateToNotificationHistory ->
                    onNavigateToHistory(command.appId)
                is AppSettingsCommand.NavigateToNetworkLog ->
                    onNavigateToNetworkLog(command.appId)
                is AppSettingsCommand.StartBackgroundService ->
                    onStartBackgroundService(command.appId)
                is AppSettingsCommand.StopBackgroundService ->
                    onStopBackgroundService(command.appId)
                is AppSettingsCommand.ShareNetworkLog -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, command.content)
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.network_log_export_subject))
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
        topBar = {
            TopAppBar(
                title = { Text(app?.name ?: stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_button)
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (app == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            // ── App Info ──────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_app_info))
            SurfaceCard {
                Column(
                    modifier = Modifier.padding(Dimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    OutlinedTextField(
                        value = app.url,
                        onValueChange = viewModel::setUrl,
                        label = { Text(stringResource(R.string.add_url_label)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Link,
                                null,
                                modifier = Modifier.size(Dimens.sizeMd)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.cornerLg),
                    )
                    OutlinedTextField(
                        value = app.name,
                        onValueChange = viewModel::setName,
                        label = { Text(stringResource(R.string.add_name_label)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                null,
                                modifier = Modifier.size(Dimens.sizeMd)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.cornerLg),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )

                    // Icon row: preview + fetch + gallery + icon pack + theme color
                    val previewIconPath = app.iconPath
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.sizeIconPreview)
                                .clip(RoundedCornerShape(Dimens.cornerIcon))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(
                                    Dimens.borderDefault,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(Dimens.cornerIcon)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (previewIconPath != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(java.io.File(previewIconPath))
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = app.name,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(Dimens.cornerIcon)),
                                )
                            } else {
                                AppIcon(app = app, modifier = Modifier.fillMaxSize())
                            }
                        }

                        Spacer(Modifier.width(Dimens.spaceSm))

                        FilledIconButton(
                            onClick = viewModel::fetchIcon,
                            enabled = app.url.isNotBlank() && !state.isFetchingIcon,
                            modifier = Modifier.size(Dimens.size4xl),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            if (state.isFetchingIcon) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.sizeTagIcon),
                                    strokeWidth = Dimens.strokeSm
                                )
                            } else {
                                Icon(
                                    Icons.Default.Language,
                                    stringResource(R.string.add_fetch_icon_cd),
                                    modifier = Modifier.size(Dimens.sizeXs)
                                )
                            }
                        }

                        Spacer(Modifier.width(Dimens.spaceXxs))

                        FilledIconButton(
                            onClick = {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.size(Dimens.size4xl),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Icon(
                                Icons.Default.Image,
                                stringResource(R.string.add_choose_image_cd),
                                modifier = Modifier.size(Dimens.sizeXs)
                            )
                        }

                        if (state.iconPackAvailable) {
                            Spacer(Modifier.width(Dimens.spaceXxs))
                            FilledIconButton(
                                onClick = viewModel::openIconPackPicker,
                                modifier = Modifier.size(Dimens.size4xl),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    null,
                                    modifier = Modifier.size(Dimens.sizeXs)
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.cornerLg))
                                .border(
                                    Dimens.borderDefault,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(Dimens.cornerLg)
                                )
                                .clickable { showColorPicker = true }
                                .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Dimens.size2xl)
                                    .clip(RoundedCornerShape(Dimens.cornerXs))
                                    .background(
                                        if (app.themeColor != null)
                                            runCatching {
                                                Color(
                                                    android.graphics.Color.parseColor(
                                                        app.themeColor
                                                    )
                                                )
                                            }
                                                .getOrDefault(MaterialTheme.colorScheme.primaryContainer)
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                            )
                            Column {
                                Text(
                                    themeColorLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    app.themeColor ?: themeColorNotSet,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── Control center ───────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_control_center_title))
            SurfaceCard {
                ToggleListItem(
                    label = stringResource(R.string.settings_control_center),
                    checked = app.showControlCenter,
                    onToggle = viewModel::toggleControlCenter,
                    icon = { Icon(Icons.Default.Tune, null) },
                )
                CardDivider()
                ToggleListItem(
                    label = stringResource(R.string.settings_fullscreen),
                    checked = app.isFullscreen,
                    onToggle = viewModel::toggleFullscreen,
                    icon = { Icon(Icons.Default.Fullscreen, null) },
                )
                CardDivider()
                ToggleListItem(
                    label = stringResource(R.string.settings_adblock),
                    checked = app.adBlockEnabled,
                    onToggle = viewModel::toggleAdBlock,
                    icon = { Icon(Icons.Default.Shield, null) },
                )
                CardDivider()
                ToggleListItem(
                    label = stringResource(R.string.settings_translate),
                    checked = app.translateEnabled,
                    onToggle = viewModel::toggleTranslate,
                    icon = { Icon(Icons.Default.GTranslate, null) },
                )
                CardDivider()
                ToggleListItem(
                    label = stringResource(R.string.settings_swipe_to_refresh),
                    checked = app.swipeToRefreshEnabled,
                    onToggle = viewModel::toggleSwipeToRefresh,
                    icon = { Icon(Icons.Default.Refresh, null) },
                )
                CardDivider()
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.settings_translate_lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (app.translateEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ),
                        )
                    },
                    trailingContent = {
                        androidx.compose.foundation.layout.Box {
                            TextButton(
                                onClick = { if (app.translateEnabled) showLangMenu = true },
                                enabled = app.translateEnabled,
                            ) {
                                Icon(
                                    Icons.Default.GTranslate,
                                    null,
                                    modifier = Modifier.size(Dimens.sizeSm)
                                )
                                Text(
                                    " ${app.translateTarget.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (app.translateEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.38f
                                    ),
                                )
                            }
                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false }) {
                                TranslateLanguage.entries.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.displayName) },
                                        onClick = {
                                            viewModel.setTranslateTarget(lang); showLangMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
            SurfaceCard {
                ToggleListItem(
                    label = stringResource(R.string.settings_notifications_permission),
                    checked = app.notificationPermission == NotificationPermission.GRANTED && state.globalNotificationsEnabled,
                    onToggle = {
                        if (state.globalNotificationsEnabled && app.notificationPermission != NotificationPermission.NOT_ASKED) {
                            viewModel.toggleNotificationPermission()
                        }
                    },
                    icon = {
                        Icon(
                            if (app.notificationPermission == NotificationPermission.GRANTED && state.globalNotificationsEnabled) {
                                Icons.Default.Notifications
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = null,
                        )
                    },
                )
                if (!state.globalNotificationsEnabled) {
                    CardDivider()
                    ListItem(
                        leadingContent = {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.settings_notifications_global_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                } else if (app.notificationPermission == NotificationPermission.NOT_ASKED) {
                    CardDivider()
                    ListItem(
                        leadingContent = {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.settings_notifications_not_asked_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                } else if (app.notificationPermission == NotificationPermission.GRANTED) {
                    CardDivider()
                    DndRangeRow(
                        startHour = app.dndStartHour,
                        endHour = app.dndEndHour,
                        onClick = { showDndStartPicker = true },
                        onClear = viewModel::clearDndSchedule,
                    )
                    if (app.engineType == EngineType.GECKOVIEW) {
                        CardDivider()
                        ToggleListItem(
                            label = stringResource(R.string.settings_notifications_background),
                            checked = app.backgroundNotificationsEnabled,
                            onToggle = viewModel::toggleBackgroundNotifications,
                        )
                    }
                }
                CardDivider()
                @Suppress("MagicNumber")
                ListItem(
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                    headlineContent = {
                        Text(
                            stringResource(R.string.settings_notifications_history),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    },
                    modifier = Modifier.clickable { viewModel.onNotificationHistoryClick() },
                )
                CardDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Default.Dns, contentDescription = null) },
                    headlineContent = {
                        Text(
                            stringResource(R.string.settings_network_log),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.onExportNetworkLogsClick() },
                                enabled = state.hasNetworkLogs,
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = stringResource(R.string.network_log_export_cd),
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    },
                    modifier = Modifier.clickable { viewModel.onNetworkLogClick() },
                )
            }

            // ── Browser Engine ────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.add_engine_section))
            AppEngineCard(
                selected = app.engineType,
                geckoInstallState = geckoInstallState,
                onSelect = viewModel::setEngineType,
            )

            // ── Shortcut ──────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_shortcut))
            SurfaceCard {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.settings_create_shortcut),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.settings_create_shortcut_desc)) },
                    trailingContent = {
                        IconButton(onClick = {
                            PwaShortcutManager.createShortcut(context, app)
                            viewModel.markShortcutCreated(app)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Shortcut,
                                stringResource(R.string.settings_create_shortcut_cd)
                            )
                        }
                    },
                )
            }

            // ── Security ──────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_security))
            SurfaceCard {
                ListItem(
                    leadingContent = {
                        Icon(
                            if (app.lockType != LockType.NONE) Icons.Default.Lock else Icons.Default.LockOpen,
                            null
                        )
                    },
                    headlineContent = {
                        Text(
                            stringResource(R.string.settings_applock),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = app.lockType != LockType.NONE,
                            enabled = state.hasPassword,
                            onCheckedChange = { on ->
                                if (on) {
                                    viewModel.setLockType(LockType.PASSWORD)
                                } else {
                                    if (app.lockType == LockType.PASSWORD) {
                                        viewModel.requestDisableLock()
                                    } else {
                                        viewModel.setLockType(LockType.NONE)
                                    }
                                }
                            },
                        )
                    },
                )
                if (app.lockType != LockType.NONE) {
                    CardDivider()
                    listOf(
                        LockType.PASSWORD to lockPasswordLabel,
                        LockType.SYSTEM to lockSystemLabel
                    ).forEach { (type, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setLockType(type) }
                                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                        ) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                RadioButton(
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

            // ── Danger zone ───────────────────────────────────────────────────
            Spacer(Modifier.height(Dimens.spaceMd))
            SectionLabel(stringResource(R.string.settings_danger_zone))
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
                Button(
                    onClick = { showClearDataDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Text(" ${stringResource(R.string.home_menu_clear_data)}")
                }
                Button(
                    onClick = viewModel::showDeleteDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, null)
                    Text(" ${stringResource(R.string.settings_delete_app)}")
                }
            }
        }

        if (showClearDataDialog) {
            ConfirmDialog(
                title = stringResource(R.string.home_clear_data_title),
                body = stringResource(R.string.home_clear_data_body, app?.name.orEmpty()),
                confirmLabel = stringResource(R.string.home_clear_data_button),
                onConfirm = { showClearDataDialog = false; viewModel.clearData() },
                onDismiss = { showClearDataDialog = false },
                icon = Icons.Default.DeleteSweep,
                isDestructive = true,
            )
        }

        if (state.showDeleteDialog) {
            ConfirmDialog(
                title = stringResource(R.string.settings_delete_confirm, app.name),
                body = stringResource(R.string.settings_delete_confirm_body),
                confirmLabel = stringResource(R.string.common_delete),
                onConfirm = viewModel::deleteApp,
                onDismiss = viewModel::dismissDeleteDialog,
                isDestructive = true,
            )
        }

        if (state.showDisableLockDialog) {
            var disableLockPassword by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = viewModel::dismissDisableLockDialog,
                icon = { Icon(Icons.Default.LockOpen, null) },
                title = { Text(stringResource(R.string.settings_disable_lock_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                        Text(stringResource(R.string.settings_disable_lock_body))
                        OutlinedTextField(
                            value = disableLockPassword,
                            onValueChange = { disableLockPassword = it },
                            label = { Text(stringResource(R.string.common_current_password)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = state.disableLockError,
                            supportingText = if (state.disableLockError) {
                                {
                                    Text(
                                        stringResource(R.string.common_wrong_password),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.cornerLg),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDisableLock(disableLockPassword) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.settings_disable_lock_title)) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDisableLockDialog) {
                        Text(
                            stringResource(
                                R.string.common_cancel
                            )
                        )
                    }
                },
            )
        }

        if (showColorPicker) {
            io.shellify.app.presentation.add.ColorPickerDialog(
                current = app?.themeColor,
                onColorSelected = { viewModel.setThemeColor(it) },
                onDismiss = { showColorPicker = false },
            )
        }

        if (state.showIconPackPicker) {
            SimpleIconPickerSheet(
                icons = state.packIcons,
                query = state.iconPickerQuery,
                isLoading = state.isSelectingPackIcon,
                onQueryChange = viewModel::setIconPickerQuery,
                onSelect = { entry, bgColorArgb ->
                    viewModel.selectPackIcon(entry, bgColorArgb)
                },
                onDismiss = viewModel::closeIconPackPicker,
            )
        }

        if (showShareSheet) {
            AppShareSheet(
                appName = app.name,
                appUrl = app.url,
                onDismiss = { showShareSheet = false },
            )
        }

        if (showDndStartPicker) {
            TimePickerHourDialog(
                titleRes = R.string.settings_notifications_dnd_start_title,
                initialHour = if (app.dndStartHour == -1) 22 else app.dndStartHour,
                onDismiss = { showDndStartPicker = false },
                onConfirm = { hour ->
                    viewModel.setDndStartHour(hour)
                    showDndStartPicker = false
                    showDndEndPicker = true
                },
            )
        }

        if (showDndEndPicker) {
            TimePickerHourDialog(
                titleRes = R.string.settings_notifications_dnd_end_title,
                initialHour = if (app.dndEndHour == -1) 8 else app.dndEndHour,
                onDismiss = { showDndEndPicker = false },
                onConfirm = { hour ->
                    viewModel.setDndEndHour(hour)
                    showDndEndPicker = false
                },
            )
        }
    }
}

@Composable
private fun AppEngineCard(
    selected: EngineType,
    geckoInstallState: GeckoInstallState,
    onSelect: (EngineType) -> Unit,
) {
    val geckoAvailable = geckoInstallState is GeckoInstallState.Installed
    val geckoMissing = selected == EngineType.GECKOVIEW && !geckoAvailable

    SurfaceCard {
        Column {
            if (geckoMissing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GeckoWarning.copy(alpha = 0.12f))
                        .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = GeckoWarning,
                        modifier = Modifier.size(Dimens.sizeMd),
                    )
                    Text(
                        stringResource(R.string.settings_engine_gecko_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = GeckoWarning,
                        modifier = Modifier.weight(1f),
                    )
                }
                CardDivider()
            }
            Column(
                modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.space14),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
            ) {
                EngineType.entries.forEach { engine ->
                    val isGecko = engine == EngineType.GECKOVIEW
                    val enabled = !isGecko || geckoAvailable
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onSelect(engine) }
                            .padding(vertical = Dimens.spaceXxs),
                        verticalAlignment = Alignment.Top,
                    ) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                            RadioButton(
                                selected = selected == engine,
                                onClick = if (enabled) ({ onSelect(engine) }) else null,
                                enabled = enabled,
                                modifier = Modifier.size(Dimens.sizeMd),
                            )
                        }
                        Spacer(Modifier.width(Dimens.spaceMd))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                engine.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                            if (isGecko && !geckoAvailable) {
                                val label = when (geckoInstallState) {
                                    is GeckoInstallState.Downloading -> stringResource(
                                        R.string.add_engine_gecko_downloading,
                                        (geckoInstallState.progress * 100).toInt()
                                    )

                                    is GeckoInstallState.Installing -> stringResource(R.string.add_engine_gecko_installing)
                                    is GeckoInstallState.Error -> stringResource(R.string.add_engine_gecko_error)
                                    else -> stringResource(
                                        R.string.add_engine_gecko_not_installed,
                                        engine.estimatedSizeMb
                                    )
                                }
                                Text(
                                    label, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    engine.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Dimens.spaceXxs),
    )

@Composable
private fun CardDivider() =
    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))

@Composable
private fun ToggleListItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    icon: @Composable (() -> Unit)? = null,
) = ListItem(
    headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
    leadingContent = icon,
    trailingContent = { Switch(checked = checked, onCheckedChange = { onToggle() }) },
)
