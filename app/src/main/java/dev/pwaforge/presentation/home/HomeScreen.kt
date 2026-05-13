package dev.pwaforge.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

import dev.pwaforge.R
import dev.pwaforge.domain.model.EngineType
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
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
    var hideDetails by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }
    var searchFocused by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val showDetailsCd = stringResource(R.string.home_show_details_cd)
    val hideDetailsCd = stringResource(R.string.home_hide_details_cd)
    val addFabCd = stringResource(R.string.home_add_fab_cd)
    val languageChangeCd = stringResource(R.string.language_change_cd)

    val screenBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Handyman,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.sizeLg),
                        )
                        Spacer(Modifier.width(Dimens.spaceXs))
                        Text(
                            text = stringResource(R.string.home_title),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLanguagePicker = true }) {
                        Icon(Icons.Default.Language, contentDescription = languageChangeCd)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            if (state.apps.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onAddApp,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(Dimens.spaceXs),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_fab_cd))
                }
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

        val focusManager = LocalFocusManager.current

        Column(modifier = Modifier.padding(padding)) {

            // Search bar + view controls
            if (state.hasAnyApps) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimens.spaceLg, end = Dimens.spaceMd, top = Dimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearch,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = Dimens.textSizeBody,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.sizeApp)
                        .onFocusChanged { searchFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimens.cornerFull)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(Modifier.width(Dimens.spaceMd))
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimens.sizeXs),
                            )
                            Spacer(Modifier.width(Dimens.spaceSm))
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        stringResource(R.string.home_search_hint),
                                        fontSize = Dimens.textSizeBody,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                            if (searchFocused || state.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.setSearch("")
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.size(Dimens.sizeApp),
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(Dimens.sizeXs))
                                }
                            } else {
                                Spacer(Modifier.width(Dimens.spaceMd))
                            }
                        }
                    },
                )
                if (state.apps.isNotEmpty() && !searchFocused) {
                    Spacer(Modifier.width(4.dp))
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        IconButton(
                            onClick = { hideDetails = !hideDetails },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                if (hideDetails) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (hideDetails) showDetailsCd else hideDetailsCd,
                            )
                        }
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }

            // Category filter chips
            if (state.categories.isNotEmpty() && state.hasAnyApps) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimens.spaceLg),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    modifier = Modifier.padding(top = Dimens.spaceSm, bottom = 0.dp),
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
                    onAddApp = onAddApp,
                    onQuickAdd = viewModel::quickAdd,
                    quickAddLoadingUrl = state.quickAddLoadingUrl,
                    quickAddDoneUrl = state.quickAddDoneUrl,
                )
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Dimens.sizeGridCell),
                    contentPadding = PaddingValues(start = Dimens.spaceLg, end = Dimens.spaceLg, top = Dimens.spaceSm, bottom = Dimens.spaceLg),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    gridItems(state.apps, key = { it.id }) { app ->
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
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = Dimens.spaceLg, end = Dimens.spaceLg, top = Dimens.spaceSm, bottom = Dimens.spaceLg),
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

@OptIn(ExperimentalMaterial3Api::class)
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.language_dialog_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = Dimens.spaceLg, end = Dimens.spaceLg, bottom = Dimens.spaceSm),
        )
        Column(
            modifier = Modifier.padding(horizontal = Dimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
        ) {
            options.forEach { lang ->
                val isSelected = currentLanguage == lang.code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cornerLg))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
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
                    RadioButton(selected = isSelected, onClick = { onSelect(lang.code) })
                }
            }
        }
        Spacer(Modifier.height(Dimens.spaceXl))
    }
}

private sealed class HomeEmptyState {
    data object NoApps : HomeEmptyState()
    data class FilteredCategory(val name: String) : HomeEmptyState()
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    reason: HomeEmptyState = HomeEmptyState.NoApps,
    onAddApp: () -> Unit = {},
    onQuickAdd: (name: String, url: String) -> Unit = { _, _ -> },
    quickAddLoadingUrl: String? = null,
    quickAddDoneUrl: String? = null,
) {
    val filteredTitle = if (reason is HomeEmptyState.FilteredCategory)
        stringResource(R.string.home_empty_filtered_category, reason.name)
        else ""
    val filteredSubtitle = stringResource(R.string.home_empty_filtered_subtitle)

    if (reason is HomeEmptyState.FilteredCategory) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                modifier = Modifier.padding(horizontal = Dimens.spaceXxl),
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.sizeEmptyIconLg),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    filteredTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    filteredSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    // NoApps empty state — pixel-accurate from design handoff
    val p97 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    val p95 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    val p90 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f)
    val p40 = MaterialTheme.colorScheme.primary
    val surfDim = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier.padding(horizontal = Dimens.size4xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Spacer(Modifier.height(Dimens.spaceXl + Dimens.spaceMd))

        // 160×160 illustration — 3 filled rings + single dashed orbit
        Box(
            modifier = Modifier.size(Dimens.illustrationSize),
            contentAlignment = Alignment.Center,
        ) {
            // Outermost tinted ring
            Box(modifier = Modifier.size(Dimens.illustrationSize).background(p97, CircleShape))
            // Middle ring
            Box(modifier = Modifier.size(Dimens.illustrationSizeMid).background(p95, CircleShape))
            // Inner ring
            Box(modifier = Modifier.size(Dimens.illustrationSizeInner).background(p90, CircleShape))
            // Single dashed orbit at r=70
            Canvas(modifier = Modifier.size(Dimens.illustrationSize)) {
                drawCircle(
                    color = p40.copy(alpha = 0.35f),
                    radius = Dimens.illustrationRadius.toPx(),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 7.dp.toPx()), 0f,
                        ),
                    ),
                )
            }
            // Center tile: 64dp / radius 20dp / icon 30dp
            Box(
                modifier = Modifier
                    .size(Dimens.sizeIllustrationTile)
                    .background(p40, RoundedCornerShape(Dimens.corner20)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.GridView, null, modifier = Modifier.size(Dimens.sizeIconLarge), tint = Color.White)
            }
            // Ghost tiles — positions match design handoff absolute coords
            Box(
                modifier = Modifier.size(Dimens.size2xl).offset(x = (-49).dp, y = (-57).dp)
                    .background(surface, RoundedCornerShape(Dimens.cornerSm))
                    .border(Dimens.borderDefault, surfDim, RoundedCornerShape(Dimens.cornerSm)),
            )
            Box(
                modifier = Modifier.size(Dimens.sizeLg).offset(x = 57.dp, y = (-45).dp)
                    .background(surface, RoundedCornerShape(Dimens.cornerSm))
                    .border(Dimens.borderDefault, surfDim, RoundedCornerShape(Dimens.cornerSm)),
            )
            Box(
                modifier = Modifier.size(Dimens.sizeLg).offset(x = (-61).dp, y = 51.dp)
                    .background(surface, RoundedCornerShape(Dimens.cornerSm))
                    .border(Dimens.borderDefault, surfDim, RoundedCornerShape(Dimens.cornerSm)),
            )
            Box(
                modifier = Modifier.size(Dimens.size2xl).offset(x = 41.dp, y = 59.dp)
                    .background(surface, RoundedCornerShape(Dimens.cornerSm))
                    .border(Dimens.borderDefault, surfDim, RoundedCornerShape(Dimens.cornerSm)),
            )
        }

        Spacer(Modifier.height(Dimens.space14))
        Text(
            stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = Dimens.letterSpacingTight,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.home_empty_subtitle),
            fontSize = Dimens.textSizeBody,
            lineHeight = Dimens.lineHeightBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.space18))
        Button(
            onClick = onAddApp,
            shape = RoundedCornerShape(Dimens.corner24),
            modifier = Modifier.heightIn(min = Dimens.sizeApp),
            contentPadding = PaddingValues(horizontal = Dimens.sizeLg),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(Dimens.sizeSm))
            Spacer(Modifier.width(Dimens.spaceSm))
            Text(stringResource(R.string.home_empty_subtitle_action), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(Dimens.space22))

        // Quick suggestions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.home_quick_suggestions),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = Dimens.letterSpacingCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(Dimens.spaceSm))
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                listOf(
                    Triple("YouTube", "youtube.com", Icons.Default.PlayArrow),
                    Triple("WhatsApp", "web.whatsapp.com", Icons.Default.Chat),
                    Triple("Spotify", "open.spotify.com", Icons.Default.MusicNote),
                ).forEach { (name, host, icon) ->
                    val isLoading = quickAddLoadingUrl == host
                    val isDone = quickAddDoneUrl == host
                    val isIdle = !isLoading && !isDone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.sizeApp)
                            .background(surface, RoundedCornerShape(Dimens.corner14))
                            .border(Dimens.borderDefault, surfDim, RoundedCornerShape(Dimens.corner14))
                            .clickable(
                                enabled = quickAddLoadingUrl == null && !isDone,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onQuickAdd(name, host) },
                            )
                            .padding(horizontal = Dimens.space10),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space10),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.sizeIconLarge)
                                .background(p95, RoundedCornerShape(Dimens.cornerSm)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, null, modifier = Modifier.size(Dimens.sizeXs), tint = p40)
                        }
                        val noFontPadding = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = Dimens.textSizeBody, lineHeight = Dimens.textSizeBody, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = noFontPadding)
                            Text(host, fontSize = Dimens.textSizeCaption, lineHeight = Dimens.textSizeCaption, color = MaterialTheme.colorScheme.onSurfaceVariant, style = noFontPadding)
                        }
                        when {
                            isLoading -> CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.sizeXs),
                                strokeWidth = 2.dp,
                                color = p40,
                            )
                            isDone -> Icon(Icons.Default.Check, null, modifier = Modifier.size(Dimens.sizeXs), tint = p40)
                            isIdle -> Icon(Icons.Default.Add, null, modifier = Modifier.size(Dimens.sizeXs), tint = p40)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
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
        border = BorderStroke(Dimens.borderDefault, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hideDetails) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.sizeCard)
                            .clip(RoundedCornerShape(Dimens.cornerLg))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.VisibilityOff, null,
                            modifier = Modifier.size(Dimens.sizeLg),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    AppIcon(app = app, modifier = Modifier.size(Dimens.sizeCard))
                }
                Spacer(Modifier.width(Dimens.spaceMd))
                Column(modifier = Modifier.weight(1f)) {
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
                }
            }
            Spacer(Modifier.height(Dimens.spaceXxs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FeatureTags(app)
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
        }
        if (engineMissing) {
            Row(
                modifier = Modifier.padding(top = Dimens.spaceXxs),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerDialog(
    categories: List<Category>,
    currentCategoryId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.home_assign_category_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = Dimens.spaceLg, end = Dimens.spaceLg, bottom = Dimens.spaceSm),
        )
        Column(modifier = Modifier.padding(horizontal = Dimens.spaceMd)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.cornerLg))
                    .clickable { onSelect(null) }
                    .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = currentCategoryId == null, onClick = { onSelect(null) })
                Spacer(Modifier.width(Dimens.spaceSm))
                Text(stringResource(R.string.common_none), style = MaterialTheme.typography.bodyLarge)
            }
            categories.forEach { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cornerLg))
                        .clickable { onSelect(cat.id) }
                        .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = currentCategoryId == cat.id, onClick = { onSelect(cat.id) })
                    Spacer(Modifier.width(Dimens.spaceSm))
                    Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(Dimens.spaceXl))
    }
}

@Composable
private fun FeatureTags(app: WebApp) {
    data class Tag(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val color: Color)
    val tags = buildList {
        if (app.isFullscreen) add(Tag(Icons.Default.Fullscreen, "Fullscreen", TagFullscreen))
        if (app.adBlockEnabled) add(Tag(Icons.Default.Shield, "Ad block", TagAdBlock))
        if (app.translateEnabled) add(Tag(Icons.Default.GTranslate, "Translate", TagTranslate))
        when (app.lockType) {
            LockType.PASSWORD -> add(Tag(Icons.Default.Lock, "Password", TagLockPassword))
            LockType.SYSTEM   -> add(Tag(Icons.Default.Fingerprint, "Lock", TagLockSystem))
            LockType.NONE     -> Unit
        }
    }
    if (tags.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .background(
                        tag.color.copy(alpha = 0.15f),
                        RoundedCornerShape(Dimens.cornerFull),
                    )
                    .padding(Dimens.spaceXxs),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    tag.icon,
                    contentDescription = tag.label,
                    modifier = Modifier.size(Dimens.sizeXs),
                    tint = tag.color,
                )
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
