package dev.pwaforge.presentation.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import dev.pwaforge.R
import dev.pwaforge.core.iconpack.SimpleIconEntry
import dev.pwaforge.domain.model.EngineType
import dev.pwaforge.core.engine.GeckoInstallState
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.domain.model.PwaManifest
import dev.pwaforge.domain.model.TranslateEngine
import dev.pwaforge.domain.model.TranslateLanguage
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.presentation.home.AppIcon
import dev.pwaforge.presentation.theme.Dimens
import dev.pwaforge.presentation.webview.WebViewActivity
import kotlinx.coroutines.launch

// ── Preset colors for the color picker ──────────────────────────────────────
private val PRESET_COLORS = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
    "#795548", "#9E9E9E", "#607D8B", "#000000",
    "#FFFFFF", "#1976D2", "#388E3C", "#D32F2F",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddScreen(
    viewModel: AddViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val geckoInstallState by viewModel.geckoEngineManager.installState.collectAsState(initial = viewModel.geckoEngineManager.installState.value)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    LaunchedEffect(state.launchAppId) {
        val id = state.launchAppId ?: return@LaunchedEffect
        context.startActivity(WebViewActivity.launchIntent(context, id))
        viewModel.onLaunched()
    }

    // Image picker for custom icon
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val path = context.contentResolver.openInputStream(uri)?.use { input ->
                val dir = java.io.File(context.filesDir, "icons").also { it.mkdirs() }
                val file = java.io.File(dir, "${state.isolationId}.png")
                file.outputStream().use { input.copyTo(it) }
                file.absolutePath
            }
            if (path != null) viewModel.setIconPath(path)
        }
    }

    var showColorPicker by remember { mutableStateOf(false) }

    val screenBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)

    val createAppTitle = stringResource(R.string.add_create_app)
    val editAppTitle = stringResource(R.string.edit_title)
    val themeColorLabel = stringResource(R.string.add_theme_color)
    val themeColorNotSet = stringResource(R.string.add_theme_color_not_set)
    val lockPasswordLabel = stringResource(R.string.add_lock_password)
    val lockSystemLabel = stringResource(R.string.add_lock_system)

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.name.isEmpty() && state.url.isEmpty()) createAppTitle else editAppTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                actions = {
                    val canRun = state.url.isNotBlank() && state.name.isNotBlank() && !state.isSaving && state.duplicateError == null
                    IconButton(
                        onClick = { viewModel.run() },
                        enabled = canRun,
                    ) {
                        if (state.isSaving && state.launchAppId == null) {
                            CircularProgressIndicator(modifier = Modifier.size(Dimens.sizeMd), strokeWidth = Dimens.strokeMd)
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.add_run_app_cd),
                                tint = if (canRun) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.save { savedApp ->
                                PwaShortcutManager.createShortcut(context, savedApp)
                                viewModel.markShortcutCreated(savedApp)
                            }
                        },
                        enabled = state.name.isNotBlank() && state.url.isNotBlank() && !state.isSaving && state.duplicateError == null,
                    ) {
                        if (state.isSaving && state.launchAppId != null) {
                            CircularProgressIndicator(modifier = Modifier.size(Dimens.sizeXs), strokeWidth = Dimens.strokeMd)
                        } else {
                            Text(stringResource(R.string.common_save), fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            // ── Basic Info card ───────────────────────────────────────────────
            SectionCard {
                Text(stringResource(R.string.add_basic_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.spaceSm))

                // 1. URL + inline Analyze
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text(stringResource(R.string.add_url_label)) },
                    leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(Dimens.sizeMd)) },
                    placeholder = { Text(stringResource(R.string.add_url_hint)) },
                    isError = state.urlError != null,
                    supportingText = when {
                        state.urlError != null -> ({ Text(state.urlError!!) })
                        state.analyzeError != null -> ({ Text(state.analyzeError!!, color = MaterialTheme.colorScheme.error) })
                        else -> null
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = viewModel::analyze,
                            enabled = state.url.isNotBlank() && !state.isAnalyzing,
                        ) {
                            if (state.isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(Dimens.sizeMd), strokeWidth = Dimens.strokeMd)
                            } else {
                                Icon(
                                    Icons.Default.TravelExplore,
                                    contentDescription = stringResource(R.string.add_analyze_site_cd),
                                    tint = if (state.url.isNotBlank()) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.cornerLg),
                )

                Spacer(Modifier.height(Dimens.spaceSm))

                // 2. App Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text(stringResource(R.string.add_name_label)) },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(Dimens.sizeMd)) },
                    isError = state.nameError != null || state.duplicateError != null,
                    supportingText = {
                        when {
                            state.nameError != null -> Text(state.nameError!!)
                            state.duplicateError != null -> Text(state.duplicateError!!)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.cornerLg),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )

                Spacer(Modifier.height(Dimens.spaceLg))

                // 3. Icon preview · fetch · gallery  ──  color swatch · label (single row)
                val previewIconPath = state.iconPath ?: state.pendingIconPath
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Icon preview
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
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(java.io.File(previewIconPath))
                                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .build(),
                                contentDescription = state.name,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(Dimens.cornerIcon)),
                            )
                        } else if (state.name.isNotBlank()) {
                            AppIcon(
                                app = WebApp(name = state.name, url = state.url,
                                    iconSource = null, themeColor = state.themeColor),
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Default.PhoneAndroid, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(Dimens.size2xl))
                        }
                    }

                    Spacer(Modifier.width(Dimens.spaceSm))

                    // Fetch from URL
                    FilledIconButton(
                        onClick = viewModel::fetchIcon,
                        enabled = state.url.isNotBlank() && !state.isFetchingIcon,
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

                    // Pick from gallery
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

                        // Pick from icon pack
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

                    // Theme color swatch + label — tappable
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
                                    if (state.themeColor != null)
                                        runCatching { Color(android.graphics.Color.parseColor(state.themeColor)) }
                                            .getOrDefault(MaterialTheme.colorScheme.primaryContainer)
                                    else MaterialTheme.colorScheme.primaryContainer
                                ),
                        )
                        Column {
                            Text(themeColorLabel, style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium)
                            Text(
                                state.themeColor ?: themeColorNotSet,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── Feature cards ────────────────────────────────────────────────

            FeatureCard(Icons.Default.Shield, stringResource(R.string.add_feature_adblock), state.adBlockEnabled, viewModel::setAdBlock) {
                Text(stringResource(R.string.add_feature_adblock_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.spaceMd))
                SubToggleRow(stringResource(R.string.add_adblock_user_toggle),
                    stringResource(R.string.add_adblock_user_toggle_desc),
                    state.adBlockAllowUserToggle, viewModel::setAdBlockAllowUserToggle)
                Spacer(Modifier.height(Dimens.spaceMd))
                Text(stringResource(R.string.add_adblock_custom_rules_label), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(Dimens.spaceSm))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                    OutlinedTextField(
                        value = state.adBlockCustomRuleInput,
                        onValueChange = viewModel::setAdBlockCustomRuleInput,
                        placeholder = { Text(stringResource(R.string.add_adblock_rule_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Dimens.cornerMd),
                    )
                    FilledIconButton(onClick = viewModel::addAdBlockCustomRule, shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_adblock_add_rule_cd))
                    }
                }
                if (state.adBlockCustomRules.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.spaceSm))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
                        state.adBlockCustomRules.forEach { rule ->
                            InputChip(selected = false, onClick = {},
                                label = { Text(rule, style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.removeAdBlockCustomRule(rule) },
                                        modifier = Modifier.size(Dimens.sizeSm)) {
                                        Icon(Icons.Default.Close, stringResource(R.string.common_remove), modifier = Modifier.size(Dimens.space14))
                                    }
                                })
                        }
                    }
                }
            }

            FeatureCard(Icons.Default.GTranslate, stringResource(R.string.add_feature_translate), state.translateEnabled, viewModel::setTranslate) {
                Text(stringResource(R.string.add_feature_translate_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.spaceLg))
                LangDropdown(stringResource(R.string.add_translate_target_lang), state.translateTarget,
                    TranslateLanguage.entries, { it.displayName }, viewModel::setTranslateTarget)
                Spacer(Modifier.height(Dimens.spaceMd))
                EngineDropdown(state.translateEngine, viewModel::setTranslateEngine)
                Spacer(Modifier.height(Dimens.spaceMd))
                SubToggleRow(stringResource(R.string.add_translate_auto_load),
                    stringResource(R.string.add_translate_auto_load_desc),
                    state.autoTranslateOnLoad, viewModel::setAutoTranslateOnLoad)
            }

            FeatureCard(Icons.Default.Fullscreen, stringResource(R.string.add_feature_fullscreen), state.isFullscreen, viewModel::setFullscreen) {
                SubToggleRow(stringResource(R.string.add_fullscreen_status_bar),
                    stringResource(R.string.add_fullscreen_status_bar_desc),
                    state.fullscreenShowStatusBar, viewModel::setFullscreenShowStatusBar)
                SubToggleRow(stringResource(R.string.add_fullscreen_nav_bar),
                    stringResource(R.string.add_fullscreen_nav_bar_desc),
                    state.fullscreenShowNavBar, viewModel::setFullscreenShowNavBar)
                SubToggleRow(stringResource(R.string.add_fullscreen_top_toolbar),
                    stringResource(R.string.add_fullscreen_top_toolbar_desc),
                    state.fullscreenShowTopToolbar, viewModel::setFullscreenShowTopToolbar)
            }

            FeatureCard(Icons.Default.Lock, stringResource(R.string.add_feature_applock),
                enabled = state.lockType != dev.pwaforge.domain.model.LockType.NONE,
                onToggle = { on ->
                    viewModel.setLockType(if (on) dev.pwaforge.domain.model.LockType.PASSWORD else dev.pwaforge.domain.model.LockType.NONE)
                },
            ) {
                listOf(
                    dev.pwaforge.domain.model.LockType.PASSWORD to lockPasswordLabel,
                    dev.pwaforge.domain.model.LockType.SYSTEM to lockSystemLabel,
                ).forEach { (type, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.setLockType(type) }.padding(vertical = Dimens.spaceXxs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = state.lockType == type,
                            onClick = { viewModel.setLockType(type) },
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Browser Engine card ──────────────────────────────────────────
            BrowserEngineCard(
                selected = state.engineType,
                geckoInstallState = geckoInstallState,
                onSelect = viewModel::setEngineType,
                onGoToSettings = { /* user is notified inline */ },
            )

            Spacer(Modifier.height(Dimens.spaceXl))
        }

        // ── Dialogs ───────────────────────────────────────────────────────────

        if (state.pendingManifest != null) {
            PwaAnalysisDialog(
                manifest = state.pendingManifest!!,
                iconPath = state.pendingIconPath,
                onApplyAll = { viewModel.applyManifest() },
                onDismiss = { viewModel.dismissManifest() },
            )
        }

        if (showColorPicker) {
            ColorPickerDialog(
                current = state.themeColor,
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
                    viewModel.selectPackIcon(entry, state.isolationId, bgColorArgb)
                },
                onDismiss = viewModel::closeIconPackPicker,
            )
        }
    }
}

// ── Simple Icons Picker Dialog ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SimpleIconPickerSheet(
    icons: List<SimpleIconEntry>,
    query: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (SimpleIconEntry, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val svgLoader = remember {
        ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()
    }
    val filtered = remember(icons, query) {
        if (query.isBlank()) icons else icons.filter { it.title.contains(query, ignoreCase = true) }
    }
    val iconBgColor = MaterialTheme.colorScheme.primary

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.add_icon_pack_picker_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = Dimens.spaceLg, end = Dimens.spaceLg, bottom = Dimens.spaceSm),
        )
        Column(
            modifier = Modifier.padding(horizontal = Dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.add_icon_pack_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(Dimens.sizeMd)) },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.cornerMd),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.cornerXl),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(Dimens.borderDefault, MaterialTheme.colorScheme.outlineVariant),
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(Dimens.heroHeightSm),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(Dimens.sizeEmptyIcon),
                        modifier = Modifier.fillMaxWidth().padding(Dimens.spaceSm),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                    ) {
                        items(filtered, key = { it.slug }) { entry ->
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Dimens.cornerMd))
                                    .clickable { onSelect(entry, primaryArgb) }
                                    .padding(Dimens.spaceXxs),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.sizeIconHero)
                                        .clip(RoundedCornerShape(Dimens.cornerMd))
                                        .background(iconBgColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AsyncImage(
                                        model = "https://cdn.jsdelivr.net/npm/simple-icons/icons/${entry.slug}.svg",
                                        contentDescription = entry.title,
                                        imageLoader = svgLoader,
                                        modifier = Modifier.size(Dimens.size5xl),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                            Color.White,
                                            androidx.compose.ui.graphics.BlendMode.SrcIn,
                                        ),
                                    )
                                }
                                Text(
                                    entry.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Dimens.spaceXl))
    }
}

// ── PWA Analysis Report Dialog ────────────────────────────────────────────────

@Composable
private fun PwaAnalysisDialog(
    manifest: PwaManifest,
    iconPath: String?,
    onApplyAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Dimens.size4xl)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White,
                        modifier = Modifier.size(Dimens.sizeMd))
                }
                Spacer(Modifier.width(Dimens.spaceMd))
                Text(stringResource(R.string.add_pwa_config_detected),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(Dimens.size4xl)) {
                    Icon(Icons.Default.Close, stringResource(R.string.add_dismiss_cd), modifier = Modifier.size(Dimens.sizeSm))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                val source = if (manifest.display != null || manifest.icons.isNotEmpty()) "manifest.json" else "meta tags"
                Text(stringResource(R.string.add_manifest_source, source), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.spaceSm))
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg))
                Spacer(Modifier.height(Dimens.spaceSm))

                manifest.name?.let {
                    ManifestRow(stringResource(R.string.add_manifest_name_label), it)
                }
                manifest.bestIconUrl("")?.let { url ->
                    ManifestRow(stringResource(R.string.add_manifest_icon_label), url, maxLines = 2)
                }
                manifest.themeColor?.let { hex ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = Dimens.spaceXxs)) {
                        Text(stringResource(R.string.add_manifest_color_label), style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(Dimens.spaceXs))
                        val parsed = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrNull()
                        if (parsed != null) {
                            Box(modifier = Modifier.size(Dimens.sizeSm).clip(RoundedCornerShape(Dimens.cornerXxs))
                                .background(parsed)
                                .border(Dimens.borderHair, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(Dimens.cornerXxs)))
                            Spacer(Modifier.width(Dimens.spaceXs))
                        }
                        Text(hex, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                manifest.display?.let { ManifestRow(stringResource(R.string.add_manifest_display_label), it) }
                manifest.startUrl?.let { ManifestRow(stringResource(R.string.add_manifest_start_url_label), it, maxLines = 2) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyAll(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.cornerLg),
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(Dimens.sizeSm))
                Spacer(Modifier.width(Dimens.spaceSm))
                Text(stringResource(R.string.add_apply_all), fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun ManifestRow(label: String, value: String, maxLines: Int = 1) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = Dimens.spaceXxs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(Dimens.spaceXs))
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

// ── Color Picker Dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPickerDialog(
    current: String?,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(current ?: PRESET_COLORS.first()) }
    var hexInput by remember { mutableStateOf(current?.removePrefix("#") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_pick_color_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg)) {
                // Preview swatch
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                    Box(modifier = Modifier.size(Dimens.sizeApp).clip(RoundedCornerShape(Dimens.cornerLg))
                        .background(runCatching { Color(android.graphics.Color.parseColor(selected)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary))
                        .border(Dimens.borderDefault, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(Dimens.cornerLg)))
                    Text(selected, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Preset grid
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    PRESET_COLORS.forEach { hex ->
                        val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(Color.Gray)
                        Box(
                            modifier = Modifier
                                .size(Dimens.size5xl)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (selected.equals(hex, ignoreCase = true)) Dimens.borderSelected else Dimens.borderHair,
                                    if (selected.equals(hex, ignoreCase = true))
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    CircleShape,
                                )
                                .clickable { selected = hex; hexInput = hex.removePrefix("#") },
                        )
                    }
                }

                // Custom hex input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isLetterOrDigit() }.take(6).uppercase()
                        hexInput = filtered
                        if (filtered.length == 6) selected = "#$filtered"
                    },
                    label = { Text(stringResource(R.string.add_hex_color)) },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.cornerMd),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selected); onDismiss() }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

// ── Reusable layout components ────────────────────────────────────────────────

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.cornerXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(Dimens.borderDefault, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(Dimens.spaceLg), content = content)
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.cornerXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(Dimens.borderDefault, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceLg, vertical = Dimens.space14),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(Dimens.sizeCard).clip(RoundedCornerShape(Dimens.cornerMd))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(Dimens.sizeLg))
                }
                Spacer(Modifier.width(Dimens.spaceMd))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            AnimatedVisibility(visible = enabled, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg), color = MaterialTheme.colorScheme.outlineVariant)
                    Column(modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.space14),
                        content = expandedContent)
                }
            }
        }
    }
}

@Composable
private fun SubToggleRow(title: String, description: String, checked: Boolean,
                          onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Dimens.spaceLg))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LangDropdown(label: String, selected: T, options: List<T>,
                              displayName: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = displayName(selected), onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(Dimens.cornerLg))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { DropdownMenuItem(text = { Text(displayName(it)) },
                onClick = { onSelect(it); expanded = false }) }
        }
    }
}

@Composable
private fun BrowserEngineCard(
    selected: EngineType,
    geckoInstallState: GeckoInstallState,
    onSelect: (EngineType) -> Unit,
    onGoToSettings: () -> Unit,
) {
    val geckoAvailable = geckoInstallState is GeckoInstallState.Installed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cornerXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(Dimens.borderDefault, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceLg, vertical = Dimens.space14),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(Dimens.sizeCard).clip(RoundedCornerShape(Dimens.cornerMd))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Language, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(Dimens.sizeLg))
                }
                Spacer(Modifier.width(Dimens.spaceMd))
                Text(stringResource(R.string.add_engine_section), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spaceLg), color = MaterialTheme.colorScheme.outlineVariant)
            Column(modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.space14),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
                EngineType.entries.forEach { engine ->
                    val isGecko = engine == EngineType.GECKOVIEW
                    val enabled = !isGecko || geckoAvailable
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = enabled) { onSelect(engine) }
                            .padding(vertical = Dimens.spaceXxs),
                        verticalAlignment = Alignment.Top,
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides 0.dp
                        ) {
                            androidx.compose.material3.RadioButton(
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
                                    is GeckoInstallState.Downloading -> stringResource(R.string.add_engine_gecko_downloading, (geckoInstallState.progress * 100).toInt())
                                    is GeckoInstallState.Installing -> stringResource(R.string.add_engine_gecko_installing)
                                    is GeckoInstallState.Error -> stringResource(R.string.add_engine_gecko_error)
                                    else -> stringResource(R.string.add_engine_gecko_not_installed, engine.estimatedSizeMb)
                                }
                                Text(label, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(engine.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineDropdown(selected: TranslateEngine, onSelect: (TranslateEngine) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = selected.displayName, onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.add_translate_engine_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(Dimens.cornerLg))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TranslateEngine.entries.forEach { engine ->
                DropdownMenuItem(text = { Text(engine.displayName) },
                    onClick = { onSelect(engine); expanded = false })
            }
        }
    }
}
