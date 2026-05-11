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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.domain.model.PwaManifest
import dev.pwaforge.domain.model.TranslateEngine
import dev.pwaforge.domain.model.TranslateLanguage
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.presentation.home.AppIcon
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

    val screenBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBg),
                title = {
                    Text(
                        if (state.name.isEmpty() && state.url.isEmpty()) "Create App" else "Edit App",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    val canRun = state.url.isNotBlank() && state.name.isNotBlank() && !state.isSaving
                    IconButton(
                        onClick = { viewModel.run() },
                        enabled = canRun,
                    ) {
                        if (state.isSaving && state.launchAppId == null) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Run app",
                                tint = if (canRun) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.save { savedApp ->
                                PwaShortcutManager.createShortcut(context, savedApp)
                            }
                        },
                        enabled = state.name.isNotBlank() && state.url.isNotBlank() && !state.isSaving,
                    ) {
                        if (state.isSaving && state.launchAppId != null) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save", fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Basic Info card ───────────────────────────────────────────────
            SectionCard {
                Text("Basic Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))

                // 1. URL + inline Analyze
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text("Website URL") },
                    leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp)) },
                    placeholder = { Text("https://example.com") },
                    isError = state.urlError != null || state.duplicateError != null,
                    supportingText = {
                        when {
                            state.urlError != null -> Text(state.urlError!!)
                            state.duplicateError != null -> Text(state.duplicateError!!)
                            state.analyzeError != null -> Text(
                                state.analyzeError!!,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = viewModel::analyze,
                            enabled = state.url.isNotBlank() && !state.isAnalyzing,
                        ) {
                            if (state.isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Default.TravelExplore,
                                    contentDescription = "Analyze site",
                                    tint = if (state.url.isNotBlank()) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(Modifier.height(12.dp))

                // 2. App Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text("App Name") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(20.dp)) },
                    isError = state.nameError != null,
                    supportingText = { if (state.nameError != null) Text(state.nameError!!) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )

                Spacer(Modifier.height(16.dp))

                // 3. Icon (left) + Theme Color (right)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Icon box + action buttons below
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val previewIconPath = state.iconPath ?: state.pendingIconPath
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                .clickable {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (previewIconPath != null || state.name.isNotBlank()) {
                                AppIcon(
                                    app = WebApp(
                                        name = state.name, url = state.url,
                                        iconPath = previewIconPath, themeColor = state.themeColor,
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    Icons.Default.PhoneAndroid, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Fetch from URL
                            FilledIconButton(
                                onClick = viewModel::fetchIcon,
                                enabled = state.url.isNotBlank() && !state.isFetchingIcon,
                                modifier = Modifier.size(28.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                if (state.isFetchingIcon) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                } else {
                                    Icon(Icons.Default.Language, "Fetch icon", modifier = Modifier.size(14.dp))
                                }
                            }
                            // Pick from gallery
                            FilledIconButton(
                                onClick = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.size(28.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                Icon(Icons.Default.AutoAwesome, "Choose image", modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Theme color (takes remaining width)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { showColorPicker = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.themeColor != null)
                                        runCatching { Color(android.graphics.Color.parseColor(state.themeColor)) }
                                            .getOrDefault(MaterialTheme.colorScheme.primaryContainer)
                                    else MaterialTheme.colorScheme.primaryContainer
                                ),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Theme Color", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(
                                state.themeColor ?: "Not set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── Feature cards ────────────────────────────────────────────────

            FeatureCard(Icons.Default.Shield, "Ad Blocking", state.adBlockEnabled, viewModel::setAdBlock) {
                Text("When enabled, ads in web pages will be automatically blocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                SubToggleRow("Allow User Toggle",
                    "User can toggle ad blocking via floating button at runtime",
                    state.adBlockAllowUserToggle, viewModel::setAdBlockAllowUserToggle)
                Spacer(Modifier.height(12.dp))
                Text("Custom Block Rules (optional)", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.adBlockCustomRuleInput,
                        onValueChange = viewModel::setAdBlockCustomRuleInput,
                        placeholder = { Text("e.g.: ads.example.com") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    FilledIconButton(onClick = viewModel::addAdBlockCustomRule, shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Add, "Add rule")
                    }
                }
                if (state.adBlockCustomRules.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.adBlockCustomRules.forEach { rule ->
                            InputChip(selected = false, onClick = {},
                                label = { Text(rule, style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.removeAdBlockCustomRule(rule) },
                                        modifier = Modifier.size(18.dp)) {
                                        Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(14.dp))
                                    }
                                })
                        }
                    }
                }
            }

            FeatureCard(Icons.Default.GTranslate, "Auto Translate", state.translateEnabled, viewModel::setTranslate) {
                Text("Auto translate to specified language after page loads with multi-engine fallback (Google / MyMemory / LibreTranslate / Lingva)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                LangDropdown("Translation Target Language", state.translateTarget,
                    TranslateLanguage.entries, { it.displayName }, viewModel::setTranslateTarget)
                Spacer(Modifier.height(12.dp))
                EngineDropdown(state.translateEngine, viewModel::setTranslateEngine)
                Spacer(Modifier.height(12.dp))
                SubToggleRow("Show Translate Button", "Show a draggable translate FAB at bottom right",
                    state.showTranslateButton, viewModel::setShowTranslateButton)
                SubToggleRow("Auto Translate on Load",
                    "Automatically translate after page loads without manual click",
                    state.autoTranslateOnLoad, viewModel::setAutoTranslateOnLoad)
            }

            FeatureCard(Icons.Default.Fullscreen, "Fullscreen Mode", state.isFullscreen, viewModel::setFullscreen) {
                SubToggleRow("Show Status Bar",
                    "Show status bar in fullscreen mode, can fix navigation bar issues",
                    state.fullscreenShowStatusBar, viewModel::setFullscreenShowStatusBar)
                SubToggleRow("Show Navigation Bar",
                    "Keep bottom navigation bar visible in fullscreen mode (Back, Home, Recents)",
                    state.fullscreenShowNavBar, viewModel::setFullscreenShowNavBar)
                SubToggleRow("Show Top Toolbar",
                    "Keep browser toolbar visible in fullscreen mode (title, URL, back/forward/refresh)",
                    state.fullscreenShowTopToolbar, viewModel::setFullscreenShowTopToolbar)
            }

            Spacer(Modifier.height(24.dp))
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("PWA Configuration Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                val source = if (manifest.display != null || manifest.icons.isNotEmpty()) "manifest.json" else "meta tags"
                Text("Source: $source", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                manifest.name?.let {
                    ManifestRow("App Name:", it)
                }
                manifest.bestIconUrl("")?.let { url ->
                    ManifestRow("App Icon:", url, maxLines = 2)
                }
                manifest.themeColor?.let { hex ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("Theme Color:", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(6.dp))
                        val parsed = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrNull()
                        if (parsed != null) {
                            Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                                .background(parsed)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(hex, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                manifest.display?.let { ManifestRow("Display Mode:", it) }
                manifest.startUrl?.let { ManifestRow("Start URL:", it, maxLines = 2) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyAll(); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apply All", fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun ManifestRow(label: String, value: String, maxLines: Int = 1) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

// ── Color Picker Dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    current: String?,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(current ?: PRESET_COLORS.first()) }
    var hexInput by remember { mutableStateOf(current?.removePrefix("#") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Theme Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Preview swatch
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(runCatching { Color(android.graphics.Color.parseColor(selected)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)))
                    Text(selected, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Preset grid
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PRESET_COLORS.forEach { hex ->
                        val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(Color.Gray)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (selected.equals(hex, ignoreCase = true)) 2.dp else 0.5.dp,
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
                    label = { Text("Hex color") },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selected); onDismiss() }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ── Reusable layout components ────────────────────────────────────────────────

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp), content = content)
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
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            AnimatedVisibility(visible = enabled, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        content = expandedContent)
                }
            }
        }
    }
}

@Composable
private fun SubToggleRow(title: String, description: String, checked: Boolean,
                          onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
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
            shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { DropdownMenuItem(text = { Text(displayName(it)) },
                onClick = { onSelect(it); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineDropdown(selected: TranslateEngine, onSelect: (TranslateEngine) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = selected.displayName, onValueChange = {}, readOnly = true,
            label = { Text("Translation Engine") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TranslateEngine.entries.forEach { engine ->
                DropdownMenuItem(text = { Text(engine.displayName) },
                    onClick = { onSelect(engine); expanded = false })
            }
        }
    }
}
