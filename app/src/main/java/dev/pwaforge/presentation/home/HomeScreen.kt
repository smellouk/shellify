package dev.pwaforge.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.pwaforge.R
import dev.pwaforge.domain.model.EngineType
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import dev.pwaforge.domain.model.Category
import dev.pwaforge.domain.model.LockType
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.presentation.theme.Dimens
import dev.pwaforge.presentation.theme.GeckoWarning
import dev.pwaforge.presentation.theme.TagAdBlock
import dev.pwaforge.presentation.theme.TagFullscreen
import dev.pwaforge.presentation.theme.TagLockPassword
import dev.pwaforge.presentation.theme.TagLockSystem
import dev.pwaforge.presentation.theme.TagTranslate
import dev.pwaforge.presentation.webview.WebViewActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    geckoInstalled: Boolean,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onAddApp: () -> Unit,
    onEditApp: (Long) -> Unit,
    onOpenApp: (WebApp) -> Unit,
    onOpenSettings: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSearch by remember { mutableStateOf(false) }
    var hideDetails by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val showDetailsCd = stringResource(R.string.home_show_details_cd)
    val hideDetailsCd = stringResource(R.string.home_hide_details_cd)
    val searchCd = stringResource(R.string.home_search_cd)
    val addFabCd = stringResource(R.string.home_add_fab_cd)
    val languageChangeCd = stringResource(R.string.language_change_cd)

    val screenBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::setSearch,
                            placeholder = { Text(stringResource(R.string.home_search_hint)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(stringResource(R.string.home_title))
                    }
                },
                actions = {
                    IconButton(onClick = { hideDetails = !hideDetails }) {
                        Icon(if (hideDetails) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (hideDetails) showDetailsCd else hideDetailsCd)
                    }
                    IconButton(onClick = { showLanguagePicker = true }) {
                        Icon(Icons.Default.Language, contentDescription = languageChangeCd)
                    }
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) viewModel.setSearch("") }) {
                        Icon(Icons.Default.Search, contentDescription = searchCd)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddApp,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = addFabCd)
            }
        },
    ) { padding ->
        if (showLanguagePicker) {
            LanguagePickerDialog(
                currentLanguage = currentLanguage,
                onDismiss = { showLanguagePicker = false },
                onSelect = { code ->
                    showLanguagePicker = false
                    onLanguageChange(code)
                },
            )
        }

        Column(modifier = Modifier.padding(padding)) {

            // Category filter chips
            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimens.spaceLg),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    modifier = Modifier.padding(vertical = Dimens.spaceSm),
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text(stringResource(R.string.home_all_categories)) },
                        )
                    }
                    items(state.categories) { cat ->
                        val catColor = runCatching {
                            Color(android.graphics.Color.parseColor(cat.color))
                        }.getOrDefault(MaterialTheme.colorScheme.primary)
                        FilterChip(
                            selected = state.selectedCategoryId == cat.id,
                            onClick = { viewModel.selectCategory(cat.id) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(Dimens.space10)
                                            .background(catColor, CircleShape),
                                    )
                                    Text(cat.name)
                                }
                            },
                        )
                    }
                }
            }

            if (state.apps.isEmpty() && !state.isLoading) {
                val emptyReason = state.categories
                    .find { it.id == state.selectedCategoryId }
                    ?.let { HomeEmptyState.FilteredCategory(it.name) }
                    ?: HomeEmptyState.NoApps
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    reason = emptyReason,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Dimens.sizeGridCell),
                    contentPadding = PaddingValues(Dimens.spaceLg),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    items(state.apps, key = { it.id }) { app ->
                        AppCard(
                            app = app,
                            geckoInstalled = geckoInstalled,
                            categories = state.categories,
                            hideDetails = hideDetails,
                            onClick = { context.startActivity(WebViewActivity.launchIntent(context, app.id)) },
                            onEdit = { onEditApp(app.id) },
                            onSettings = { onOpenSettings(app.id) },
                            onDelete = { viewModel.delete(app) },
                            onClearData = { viewModel.clearData(app) },
                            onAssignCategory = { categoryId -> viewModel.assignCategory(app, categoryId) },
                        )
                    }
                }
            }
        }

    }

}

@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    data class LangOption(val code: String, val nativeName: String, val englishName: String)
    val options = listOf(
        LangOption("en", "English", "English"),
        LangOption("fr", "Français", "French"),
        LangOption("ar", "العربية", "Arabic"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Language, null) },
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                options.forEach { lang ->
                    val isSelected = currentLanguage == lang.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.cornerLg))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            )
                            .clickable { onSelect(lang.code) }
                            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                lang.nativeName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                lang.englishName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(lang.code) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private sealed class HomeEmptyState {
    data object NoApps : HomeEmptyState()
    data class FilteredCategory(val name: String) : HomeEmptyState()
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, reason: HomeEmptyState = HomeEmptyState.NoApps) {
    val noAppsTitle = stringResource(R.string.home_empty_title)
    val noAppsSubtitle = stringResource(R.string.home_empty_subtitle_action)
    val filteredTitle = if (reason is HomeEmptyState.FilteredCategory)
        stringResource(R.string.home_empty_filtered_category, reason.name)
        else ""
    val filteredSubtitle = stringResource(R.string.home_empty_filtered_subtitle)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            modifier = Modifier.padding(horizontal = Dimens.spaceXxl),
        ) {
            Box(
                modifier = Modifier.size(Dimens.sizeEmptyBox),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (reason) {
                        is HomeEmptyState.NoApps -> Icons.Default.PhoneAndroid
                        is HomeEmptyState.FilteredCategory -> Icons.Default.Category
                    },
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.sizeEmptyIconLg),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Dimens.sizeCard)
                        .offset(x = 28.dp, y = (-16).dp)
                        .align(Alignment.TopEnd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            Text(
                when (reason) {
                    is HomeEmptyState.NoApps -> noAppsTitle
                    is HomeEmptyState.FilteredCategory -> filteredTitle
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                when (reason) {
                    is HomeEmptyState.NoApps -> noAppsSubtitle
                    is HomeEmptyState.FilteredCategory -> filteredSubtitle
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AppCard(
    app: WebApp,
    geckoInstalled: Boolean,
    categories: List<Category>,
    hideDetails: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onDelete: () -> Unit,
    onClearData: () -> Unit,
    onAssignCategory: (Long?) -> Unit,
) {
    val engineMissing = app.engineType == EngineType.GECKOVIEW && !geckoInstalled
    var showMenu by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.cornerXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hideDetails) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.sizeApp)
                            .clip(RoundedCornerShape(Dimens.cornerLg))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.VisibilityOff, null,
                            modifier = Modifier.size(Dimens.sizeLg),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    AppIcon(app = app, modifier = Modifier.size(Dimens.sizeApp))
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(Dimens.sizeXl)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu",
                            modifier = Modifier.size(Dimens.sizeXs))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_menu_edit)) },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_menu_assign_category)) },
                            leadingIcon = { Icon(Icons.Default.Category, null) },
                            onClick = { showMenu = false; showCategoryPicker = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_menu_settings)) },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { showMenu = false; onSettings() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_menu_clear_data)) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                            onClick = { showMenu = false; showClearDataDialog = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_menu_delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { showMenu = false; onDelete() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(Dimens.spaceSm))
            Text(
                if (hideDetails) "•".repeat(app.name.length.coerceIn(4, 12)) else app.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (hideDetails) "••••••••••••" else app.url.removePrefix("https://").removePrefix("http://"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FeatureTags(app)
            if (engineMissing) {
                Spacer(Modifier.height(Dimens.spaceXs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.sizeXxs),
                        tint = GeckoWarning,
                    )
                    Text(
                        stringResource(R.string.home_gecko_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = GeckoWarning,
                    )
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = categories,
            currentCategoryId = app.categoryId,
            onDismiss = { showCategoryPicker = false },
            onSelect = { categoryId ->
                showCategoryPicker = false
                onAssignCategory(categoryId)
            },
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, null) },
            title = { Text(stringResource(R.string.home_clear_data_title)) },
            text = { Text(stringResource(R.string.home_clear_data_body, app.name)) },
            confirmButton = {
                TextButton(
                    onClick = { showClearDataDialog = false; onClearData() },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.home_clear_data_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<Category>,
    currentCategoryId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit,
) {
    var selected by remember { mutableStateOf(currentCategoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_assign_category_title)) },
        text = {
            Column {
                // "None" option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = null }
                        .padding(vertical = Dimens.spaceXxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == null, onClick = { selected = null })
                    Spacer(Modifier.width(Dimens.spaceSm))
                    Text(stringResource(R.string.common_none), style = MaterialTheme.typography.bodyLarge)
                }
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = cat.id }
                            .padding(vertical = Dimens.spaceXxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == cat.id, onClick = { selected = cat.id })
                        Spacer(Modifier.width(Dimens.spaceSm))
                        Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) { Text(stringResource(R.string.common_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun FeatureTags(app: WebApp) {
    data class Tag(val icon: androidx.compose.ui.graphics.vector.ImageVector, val desc: String, val color: Color)
    val tags = buildList {
        if (app.isFullscreen) add(Tag(Icons.Default.Fullscreen, "Fullscreen", TagFullscreen))
        if (app.adBlockEnabled) add(Tag(Icons.Default.Shield, "Ad block", TagAdBlock))
        if (app.translateEnabled) add(Tag(Icons.Default.GTranslate, "Translate", TagTranslate))
        when (app.lockType) {
            LockType.PASSWORD -> add(Tag(Icons.Default.Lock, "Password lock", TagLockPassword))
            LockType.SYSTEM   -> add(Tag(Icons.Default.Fingerprint, "System lock", TagLockSystem))
            LockType.NONE     -> Unit
        }
    }
    if (tags.isEmpty()) return
    Spacer(Modifier.height(Dimens.spaceXxs))
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .size(Dimens.sizeLg)
                    .background(tag.color.copy(alpha = 0.15f), RoundedCornerShape(Dimens.cornerXs)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tag.icon, contentDescription = tag.desc, modifier = Modifier.size(Dimens.sizeTagIcon), tint = tag.color)
            }
        }
    }
}

@Composable
fun AppIcon(app: WebApp, modifier: Modifier = Modifier) {
    if (app.iconPath != null) {
        AsyncImage(
            model = app.iconPath,
            contentDescription = app.name,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(Dimens.cornerLg)),
        )
    } else {
        val color = runCatching { Color(android.graphics.Color.parseColor(app.themeColor)) }
            .getOrDefault(MaterialTheme.colorScheme.primary)
        Box(
            modifier = modifier.clip(RoundedCornerShape(Dimens.cornerLg)).background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "P",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}
