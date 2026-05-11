package dev.pwaforge.presentation.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.domain.model.TranslateEngine
import dev.pwaforge.domain.model.TranslateLanguage
import dev.pwaforge.domain.model.UserAgentMode
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.presentation.home.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    viewModel: AddViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    val screenBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBg),
                title = {
                    Text(
                        if (viewModel.uiState.value.let { it.name.isEmpty() && it.url.isEmpty() })
                            "Create App" else "Edit App",
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
                    TextButton(
                        onClick = {
                            viewModel.save { savedApp ->
                                PwaShortcutManager.createShortcut(context, savedApp)
                            }
                        },
                        enabled = state.name.isNotBlank() && state.url.isNotBlank() && !state.isSaving,
                    ) {
                        if (state.isSaving) {
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
            // Basic Info card
            SectionCard {
                Text(
                    "Basic Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))

                // App icon row
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Icon preview box
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.iconPath != null || state.name.isNotBlank()) {
                                AppIcon(
                                    app = WebApp(
                                        name = state.name, url = state.url,
                                        iconPath = state.iconPath, themeColor = state.themeColor,
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    Icons.Default.PhoneAndroid, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "App Icon",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "App Icon",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "Click Analyze to fetch from site",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        AssistChip(
                            onClick = { /* future: icon picker */ },
                            label = { Text("Icon Library") },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            border = null,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // App name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text("App Name") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.setName("") }) {
                            Icon(Icons.Default.Casino, null, modifier = Modifier.size(20.dp))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(Modifier.height(12.dp))

                // Website URL
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text("Website URL") },
                    leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp)) },
                    placeholder = { Text("https://example.com") },
                    isError = state.urlError != null,
                    supportingText = { if (state.urlError != null) Text(state.urlError!!) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Analyze button
            Button(
                onClick = viewModel::analyze,
                enabled = state.url.isNotBlank() && !state.isAnalyzing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                ),
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyzing…")
                } else {
                    Icon(Icons.Default.TravelExplore, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze Site")
                }
            }

            if (state.analyzeError != null) {
                Text(
                    state.analyzeError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            // ── Feature cards ──────────────────────────────────────────────

            // Ad Blocking
            FeatureCard(
                icon = Icons.Default.Shield,
                title = "Ad Blocking",
                enabled = state.adBlockEnabled,
                onToggle = viewModel::setAdBlock,
            ) {
                Text(
                    "When enabled, ads in web pages will be automatically blocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SubToggleRow(
                    title = "Allow User Toggle",
                    description = "When enabled, user can toggle ad blocking via floating button at runtime",
                    checked = state.adBlockAllowUserToggle,
                    onCheckedChange = viewModel::setAdBlockAllowUserToggle,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Custom Block Rules (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.adBlockCustomRuleInput,
                        onValueChange = viewModel::setAdBlockCustomRuleInput,
                        placeholder = { Text("e.g.: ads.example.com") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    FilledIconButton(
                        onClick = viewModel::addAdBlockCustomRule,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(Icons.Default.Add, "Add rule")
                    }
                }
                if (state.adBlockCustomRules.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    state.adBlockCustomRules.forEach { rule ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(rule, style = MaterialTheme.typography.bodySmall) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.removeAdBlockCustomRule(rule) },
                                    modifier = Modifier.size(18.dp),
                                ) {
                                    Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(14.dp))
                                }
                            },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            }

            // Auto Translate
            FeatureCard(
                icon = Icons.Default.GTranslate,
                title = "Auto Translate",
                enabled = state.translateEnabled,
                onToggle = viewModel::setTranslate,
            ) {
                Text(
                    "Auto translate to specified language after page loads with multi-engine fallback (Google / MyMemory / LibreTranslate / Lingva)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                LanguageDropdown(
                    label = "Translation Target Language",
                    selected = state.translateTarget,
                    options = TranslateLanguage.entries,
                    displayName = { it.displayName },
                    onSelect = viewModel::setTranslateTarget,
                )
                Spacer(Modifier.height(12.dp))
                EngineDropdown(
                    selected = state.translateEngine,
                    onSelect = viewModel::setTranslateEngine,
                )
                Spacer(Modifier.height(12.dp))
                SubToggleRow(
                    title = "Show Translate Button",
                    description = "Show a draggable translate FAB at bottom right",
                    checked = state.showTranslateButton,
                    onCheckedChange = viewModel::setShowTranslateButton,
                )
                SubToggleRow(
                    title = "Auto Translate on Load",
                    description = "Automatically translate after page loads without manual click",
                    checked = state.autoTranslateOnLoad,
                    onCheckedChange = viewModel::setAutoTranslateOnLoad,
                )
            }

            // Fullscreen Mode
            FeatureCard(
                icon = Icons.Default.Fullscreen,
                title = "Fullscreen Mode",
                enabled = state.isFullscreen,
                onToggle = viewModel::setFullscreen,
            ) {
                SubToggleRow(
                    title = "Show Status Bar",
                    description = "Show status bar in fullscreen mode, can fix navigation bar issues",
                    checked = state.fullscreenShowStatusBar,
                    onCheckedChange = viewModel::setFullscreenShowStatusBar,
                )
                SubToggleRow(
                    title = "Show Navigation Bar",
                    description = "Keep bottom navigation bar visible in fullscreen mode (Back, Home, Recents)",
                    checked = state.fullscreenShowNavBar,
                    onCheckedChange = viewModel::setFullscreenShowNavBar,
                )
                SubToggleRow(
                    title = "Show Top Toolbar",
                    description = "Keep browser toolbar visible in fullscreen mode (title, URL, back/forward/refresh)",
                    checked = state.fullscreenShowTopToolbar,
                    onCheckedChange = viewModel::setFullscreenShowTopToolbar,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Reusable components ──────────────────────────────────────────────────────

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        content = expandedContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LanguageDropdown(
    label: String,
    selected: T,
    options: List<T>,
    displayName: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayName(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayName(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineDropdown(
    selected: TranslateEngine,
    onSelect: (TranslateEngine) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Translation Engine") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TranslateEngine.entries.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine.displayName) },
                    onClick = { onSelect(engine); expanded = false },
                )
            }
        }
    }
}
