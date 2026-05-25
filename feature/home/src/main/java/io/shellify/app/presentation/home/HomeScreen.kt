package io.shellify.app.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoNotDisturb
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

import io.shellify.core.ui.R
import io.shellify.app.domain.model.EngineType
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import io.shellify.app.domain.model.Category
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.components.AppIcon
import io.shellify.app.presentation.components.ConfirmDialog
import io.shellify.app.presentation.components.EmptyStateIllustration
import io.shellify.app.presentation.theme.Dimens
import io.shellify.app.presentation.theme.GeckoWarning
import io.shellify.app.presentation.theme.TagAdBlock
import io.shellify.app.presentation.theme.TagDnd
import io.shellify.app.presentation.theme.TagFullscreen
import io.shellify.app.presentation.theme.TagLockPassword
import io.shellify.app.presentation.theme.TagLockSystem
import io.shellify.app.presentation.theme.TagTor
import io.shellify.app.presentation.theme.TagTranslate
import io.shellify.app.presentation.share.AppShareSheet
import io.shellify.app.presentation.webview.WebViewActivity

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
    var showDeleteAllAppsDialog by remember { mutableStateOf(false) }

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
                        Box(
                            modifier = Modifier
                                .size(Dimens.sizeLg)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_app_logo_fg),
                                contentDescription = null,
                                modifier = Modifier.requiredSize(Dimens.size3xl),
                            )
                        }
                        Spacer(Modifier.width(Dimens.spaceXs))
                        Text(
                            text = stringResource(R.string.home_title),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    if (state.hasAnyApps) {
                        IconButton(onClick = { showDeleteAllAppsDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.home_delete_all_apps_cd),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
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
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.home_add_fab_cd)
                    )
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
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(Dimens.cornerFull)
                                ),
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
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.sizeXs)
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(Dimens.spaceMd))
                            }
                        }
                    },
                )
                if (state.apps.isNotEmpty() && !searchFocused) {
                    Spacer(Modifier.width(Dimens.spaceXxs))
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        IconButton(
                            onClick = { hideDetails = !hideDetails },
                            modifier = Modifier.size(Dimens.size4xl),
                        ) {
                            Icon(
                                if (hideDetails) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (hideDetails) showDetailsCd else hideDetailsCd,
                            )
                        }
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier.size(Dimens.size4xl),
                        ) {
                            Icon(
                                if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
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

            if (state.isLoading) {
                LoadingApps(modifier = Modifier.fillMaxSize())
            } else if (state.apps.isEmpty()) {
                val emptyReason = state.categories
                    .find { it.id == state.selectedCategoryId }
                    ?.let { HomeEmptyState.FilteredCategory(it.name) }
                    ?: HomeEmptyState.NoApps
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    reason = emptyReason,
                    onAddApp = onAddApp,
                    onQuickAdd = viewModel::quickAdd,
                    quickAddLoadingUrls = state.quickAddLoadingUrls,
                    quickAddDoneUrls = state.quickAddDoneUrls,
                )
            } else if (isGridView) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(Dimens.sizeGridCell),
                    contentPadding = PaddingValues(
                        start = Dimens.spaceLg,
                        end = Dimens.spaceLg,
                        top = Dimens.spaceSm,
                        bottom = Dimens.spaceLg
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                    verticalItemSpacing = Dimens.spaceMd,
                ) {
                    staggeredItems(state.apps, key = { it.id }) { app ->
                        AppCard(
                            app = app,
                            geckoInstalled = geckoInstalled,
                            categories = state.categories,
                            hideDetails = hideDetails,
                            onClick = {
                                context.startActivity(
                                    WebViewActivity.launchIntent(
                                        context,
                                        app.id
                                    )
                                )
                            },
                            onSettings = { onOpenSettings(app.id) },
                            onAssignCategory = { categoryId ->
                                viewModel.assignCategory(
                                    app,
                                    categoryId
                                )
                            },
                            onClearData = { viewModel.clearData(app) },
                            onDelete = { viewModel.delete(app) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = Dimens.spaceLg,
                        end = Dimens.spaceLg,
                        top = Dimens.spaceSm,
                        bottom = Dimens.spaceLg
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    items(state.apps, key = { it.id }) { app ->
                        AppCard(
                            app = app,
                            geckoInstalled = geckoInstalled,
                            categories = state.categories,
                            hideDetails = hideDetails,
                            onClick = {
                                context.startActivity(
                                    WebViewActivity.launchIntent(
                                        context,
                                        app.id
                                    )
                                )
                            },
                            onSettings = { onOpenSettings(app.id) },
                            onAssignCategory = { categoryId ->
                                viewModel.assignCategory(
                                    app,
                                    categoryId
                                )
                            },
                            onClearData = { viewModel.clearData(app) },
                            onDelete = { viewModel.delete(app) },
                        )
                    }
                }
            }
        }

    }

    if (showDeleteAllAppsDialog) {
        ConfirmDialog(
            title = stringResource(R.string.home_delete_all_apps_confirm_title),
            body = stringResource(R.string.home_delete_all_apps_confirm_body),
            confirmLabel = stringResource(R.string.common_delete_all),
            onConfirm = { showDeleteAllAppsDialog = false; viewModel.deleteAllApps() },
            onDismiss = { showDeleteAllAppsDialog = false },
            icon = Icons.Default.DeleteSweep,
            isDestructive = true,
        )
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
            modifier = Modifier.padding(
                start = Dimens.spaceLg,
                end = Dimens.spaceLg,
                bottom = Dimens.spaceSm
            ),
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
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                alpha = 0.7f
                            )
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
private fun LoadingApps(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading_dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dot_phase",
    )
    val dots = ".".repeat(phase.toInt().coerceIn(0, 3))
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.home_loading) + dots,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    reason: HomeEmptyState = HomeEmptyState.NoApps,
    onAddApp: () -> Unit = {},
    onQuickAdd: (name: String, url: String) -> Unit = { _, _ -> },
    quickAddLoadingUrls: Set<String> = emptySet(),
    quickAddDoneUrls: Set<String> = emptySet(),
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

    val p95 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    val p40 = MaterialTheme.colorScheme.primary
    val surfDim = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier.padding(horizontal = Dimens.size4xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Spacer(Modifier.height(Dimens.spaceXl + Dimens.spaceMd))

        EmptyStateIllustration(centerIcon = Icons.Default.GridView)

        Spacer(Modifier.height(Dimens.space14))
        Text(
            stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = Dimens.letterSpacingTight,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.spaceXxxs))
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
            Text(
                stringResource(R.string.home_empty_subtitle_action),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(Dimens.space22))

        // Quick suggestions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = Dimens.widthSuggestionsMax)
                .fillMaxWidth(),
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
                    Triple("Reddit", "reddit.com", Icons.Default.Forum),
                    Triple("Discord", "discord.com", Icons.Default.Headset),
                    Triple("GitHub", "github.com", Icons.Default.Code),
                ).forEach { (name, host, icon) ->
                    val isLoading = host in quickAddLoadingUrls
                    val isDone = host in quickAddDoneUrls
                    val isIdle = !isLoading && !isDone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.sizeApp)
                            .background(surface, RoundedCornerShape(Dimens.corner14))
                            .border(
                                Dimens.borderDefault,
                                surfDim,
                                RoundedCornerShape(Dimens.corner14)
                            )
                            .clickable(
                                enabled = !isLoading && !isDone,
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
                        val noFontPadding =
                            TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                name,
                                fontSize = Dimens.textSizeBody,
                                lineHeight = Dimens.textSizeBody,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = noFontPadding
                            )
                            Text(
                                host,
                                fontSize = Dimens.textSizeCaption,
                                lineHeight = Dimens.textSizeCaption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = noFontPadding
                            )
                        }
                        when {
                            isLoading -> CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.sizeXs),
                                strokeWidth = Dimens.strokeMd,
                                color = p40,
                            )

                            isDone -> Icon(
                                Icons.Default.Check,
                                null,
                                modifier = Modifier.size(Dimens.sizeXs),
                                tint = p40
                            )

                            isIdle -> Icon(
                                Icons.Default.Add,
                                null,
                                modifier = Modifier.size(Dimens.sizeXs),
                                tint = p40
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppCard(
    app: WebApp,
    geckoInstalled: Boolean,
    categories: List<Category>,
    hideDetails: Boolean = false,
    onClick: () -> Unit,
    onSettings: () -> Unit,
    onAssignCategory: (Long?) -> Unit,
    onClearData: () -> Unit,
    onDelete: () -> Unit,
) {
    val engineMissing = app.engineType == EngineType.GECKOVIEW && !geckoInstalled
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.cornerXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            Dimens.borderDefault,
            if (engineMissing) GeckoWarning.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
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
                        Icon(
                            Icons.Default.VisibilityOff, null,
                            modifier = Modifier.size(Dimens.sizeLg),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        if (hideDetails) "••••••••••••" else app.url.removePrefix("https://")
                            .removePrefix("http://"),
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
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(Dimens.sizeXl)
                    ) {
                        Icon(
                            Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_menu_more_cd),
                            modifier = Modifier.size(Dimens.sizeXs)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        AppCardContextMenu(
                            onAssignCategory = { showMenu = false; showCategoryPicker = true },
                            onShare = { showMenu = false; showShareSheet = true },
                            onClearData = { showMenu = false; showClearDataDialog = true },
                            onDelete = { showMenu = false; showDeleteDialog = true },
                            onSettings = { showMenu = false; onSettings() },
                        )
                    }
                }
            }
        }
        if (engineMissing) {
            HorizontalDivider(color = GeckoWarning.copy(alpha = 0.25f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GeckoWarning.copy(alpha = 0.08f))
                    .clickable { onSettings() }
                    .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.sizeSm),
                    tint = GeckoWarning,
                )
                Text(
                    stringResource(R.string.home_gecko_required),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = GeckoWarning,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.sizeSm),
                    tint = GeckoWarning.copy(alpha = 0.7f),
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
        ConfirmDialog(
            title = stringResource(R.string.home_clear_data_title),
            body = stringResource(R.string.home_clear_data_body, app.name),
            confirmLabel = stringResource(R.string.home_clear_data_button),
            onConfirm = { showClearDataDialog = false; onClearData() },
            onDismiss = { showClearDataDialog = false },
            icon = Icons.Default.Delete,
            isDestructive = true,
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_confirm, app.name),
            body = stringResource(R.string.settings_delete_confirm_body),
            confirmLabel = stringResource(R.string.home_menu_delete),
            onConfirm = { showDeleteDialog = false; onDelete() },
            onDismiss = { showDeleteDialog = false },
            icon = Icons.Default.DeleteForever,
            isDestructive = true,
        )
    }

    if (showShareSheet) {
        AppShareSheet(
            appName = app.name,
            appUrl = app.url,
            onDismiss = { showShareSheet = false },
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
            modifier = Modifier.padding(
                start = Dimens.spaceLg,
                end = Dimens.spaceLg,
                bottom = Dimens.spaceSm
            ),
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
                Text(
                    stringResource(R.string.common_none),
                    style = MaterialTheme.typography.bodyLarge
                )
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
                    RadioButton(
                        selected = currentCategoryId == cat.id,
                        onClick = { onSelect(cat.id) })
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
    data class Tag(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val color: Color
    )

    val tags = buildList {
        if (app.isFullscreen) add(Tag(Icons.Default.Fullscreen, "Fullscreen", TagFullscreen))
        if (app.adBlockEnabled) add(Tag(Icons.Default.Shield, "Ad block", TagAdBlock))
        if (app.useTor) add(Tag(Icons.Default.VpnLock, "Tor", TagTor))
        if (app.translateEnabled) add(Tag(Icons.Default.GTranslate, "Translate", TagTranslate))
        if (app.dndStartHour != -1) add(Tag(Icons.Default.DoNotDisturb, "DND", TagDnd))
        when (app.lockType) {
            LockType.PASSWORD -> add(Tag(Icons.Default.Lock, "Password", TagLockPassword))
            LockType.SYSTEM -> add(Tag(Icons.Default.Fingerprint, "Lock", TagLockSystem))
            LockType.NONE -> Unit
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
fun AppCardContextMenu(
    onAssignCategory: () -> Unit,
    onShare: () -> Unit,
    onClearData: () -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
) {
    Column {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.home_menu_assign_category)) },
            leadingIcon = { Icon(Icons.Default.Category, null) },
            onClick = onAssignCategory,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.share_button)) },
            leadingIcon = { Icon(Icons.Default.Share, null) },
            onClick = onShare,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.home_menu_clear_data)) },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            onClick = onClearData,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.home_menu_delete)) },
            leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
            onClick = onDelete,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.home_menu_settings)) },
            leadingIcon = { Icon(Icons.Default.Settings, null) },
            onClick = onSettings,
        )
    }
}
