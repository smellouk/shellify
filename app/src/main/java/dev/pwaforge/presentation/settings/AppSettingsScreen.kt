package dev.pwaforge.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import dev.pwaforge.R
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.domain.model.LockType
import dev.pwaforge.domain.model.TranslateLanguage
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.presentation.add.SimpleIconPickerSheet
import dev.pwaforge.presentation.home.AppIcon
import dev.pwaforge.presentation.theme.Dimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = state.app
    val lockPasswordLabel = stringResource(R.string.settings_lock_password)
    val lockSystemLabel = stringResource(R.string.settings_lock_system)
    val themeColorLabel = stringResource(R.string.add_theme_color)
    val themeColorNotSet = stringResource(R.string.add_theme_color_not_set)

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
        topBar = {
            TopAppBar(
                title = { Text(app?.name ?: stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
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
            SettingsCard {
                Column(
                    modifier = Modifier.padding(Dimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    OutlinedTextField(
                        value = app.url,
                        onValueChange = viewModel::setUrl,
                        label = { Text(stringResource(R.string.add_url_label)) },
                        leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(Dimens.sizeMd)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.cornerLg),
                    )
                    OutlinedTextField(
                        value = app.name,
                        onValueChange = viewModel::setName,
                        label = { Text(stringResource(R.string.add_name_label)) },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(Dimens.sizeMd)) },
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
                                .border(Dimens.borderDefault, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(Dimens.cornerIcon)),
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
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(Dimens.cornerIcon)),
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
                                CircularProgressIndicator(modifier = Modifier.size(Dimens.sizeTagIcon), strokeWidth = Dimens.strokeSm)
                            } else {
                                Icon(Icons.Default.Language, stringResource(R.string.add_fetch_icon_cd), modifier = Modifier.size(Dimens.sizeXs))
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
                            Icon(Icons.Default.Image, stringResource(R.string.add_choose_image_cd), modifier = Modifier.size(Dimens.sizeXs))
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
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(Dimens.sizeXs))
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.cornerLg))
                                .border(Dimens.borderDefault, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(Dimens.cornerLg))
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
                                            runCatching { Color(android.graphics.Color.parseColor(app.themeColor)) }
                                                .getOrDefault(MaterialTheme.colorScheme.primaryContainer)
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                            )
                            Column {
                                Text(themeColorLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
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

            // ── Features ─────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_features))
            SettingsCard {
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
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_translate_lang), style = MaterialTheme.typography.bodyMedium) },
                    trailingContent = {
                        androidx.compose.foundation.layout.Box {
                            TextButton(onClick = { showLangMenu = true }) {
                                Icon(Icons.Default.GTranslate, null, modifier = Modifier.size(Dimens.sizeSm))
                                Text(
                                    " ${app.translateTarget.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                                TranslateLanguage.entries.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.displayName) },
                                        onClick = { viewModel.setTranslateTarget(lang); showLangMenu = false },
                                    )
                                }
                            }
                        }
                    },
                )
            }

            // ── Shortcut ──────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_shortcut))
            SettingsCard {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_create_shortcut), style = MaterialTheme.typography.bodyMedium) },
                    supportingContent = { Text(stringResource(R.string.settings_create_shortcut_desc)) },
                    trailingContent = {
                        IconButton(onClick = {
                            PwaShortcutManager.createShortcut(context, app)
                            viewModel.markShortcutCreated(app)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Shortcut, stringResource(R.string.settings_create_shortcut_cd))
                        }
                    },
                )
            }

            // ── Security ──────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_security))
            SettingsCard {
                ListItem(
                    leadingContent = {
                        Icon(if (app.lockType != LockType.NONE) Icons.Default.Lock else Icons.Default.LockOpen, null)
                    },
                    headlineContent = { Text(stringResource(R.string.settings_applock), style = MaterialTheme.typography.bodyMedium) },
                    trailingContent = {
                        Switch(
                            checked = app.lockType != LockType.NONE,
                            onCheckedChange = { on ->
                                viewModel.setLockType(if (on) LockType.PASSWORD else LockType.NONE)
                            },
                        )
                    },
                )
                if (app.lockType != LockType.NONE) {
                    CardDivider()
                    listOf(LockType.PASSWORD to lockPasswordLabel, LockType.SYSTEM to lockSystemLabel).forEach { (type, label) ->
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
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                icon = { Icon(Icons.Default.DeleteSweep, null) },
                title = { Text(stringResource(R.string.home_clear_data_title)) },
                text = { Text(stringResource(R.string.home_clear_data_body, app?.name.orEmpty())) },
                confirmButton = {
                    TextButton(
                        onClick = { showClearDataDialog = false; viewModel.clearData() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.home_clear_data_button)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) { Text(stringResource(R.string.common_cancel)) }
                },
            )
        }

        if (state.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteDialog,
                title = { Text(stringResource(R.string.settings_delete_confirm, app.name)) },
                text = { Text(stringResource(R.string.settings_delete_confirm_body)) },
                confirmButton = {
                    TextButton(onClick = viewModel::deleteApp) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteDialog) { Text(stringResource(R.string.common_cancel)) }
                },
            )
        }

        if (showColorPicker) {
            dev.pwaforge.presentation.add.ColorPickerDialog(
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
private fun SettingsCard(content: @Composable () -> Unit) =
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cornerXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(Dimens.borderDefault, MaterialTheme.colorScheme.outlineVariant),
    ) { content() }

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
