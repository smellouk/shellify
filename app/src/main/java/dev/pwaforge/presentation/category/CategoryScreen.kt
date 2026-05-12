package dev.pwaforge.presentation.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pwaforge.R
import dev.pwaforge.presentation.theme.Dimens

val CATEGORY_ICON_KEYS = listOf(
    "folder", "folder_open", "phone_android", "laptop",
    "sports_esports", "music_note", "movie", "menu_book",
    "calendar_today", "work", "shopping_bag", "favorite",
    "star", "local_fire_department", "lightbulb", "auto_awesome",
    "home", "directions_car", "flight", "sailing",
    "language", "palette", "dark_mode", "wb_sunny",
)

val CATEGORY_COLORS = listOf(
    "#6D28D9", "#1E1B4B", "#0D9488", "#065F46",
    "#7C3AED", "#5B21B6", "#EA580C", "#CA8A04",
    "#16A34A", "#65A30D", "#E11D48", "#DC2626",
    "#2563EB", "#4338CA", "#DB2777", "#9D174D",
)

fun categoryIconVector(name: String): ImageVector = when (name) {
    "folder" -> Icons.Default.Folder
    "folder_open" -> Icons.Default.FolderOpen
    "phone_android" -> Icons.Default.PhoneAndroid
    "laptop" -> Icons.Default.Laptop
    "sports_esports" -> Icons.Default.SportsEsports
    "music_note" -> Icons.Default.MusicNote
    "movie" -> Icons.Default.Movie
    "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
    "calendar_today" -> Icons.Default.CalendarToday
    "work" -> Icons.Default.Work
    "shopping_bag" -> Icons.Default.ShoppingBag
    "favorite" -> Icons.Default.Favorite
    "star" -> Icons.Default.Star
    "local_fire_department" -> Icons.Default.LocalFireDepartment
    "lightbulb" -> Icons.Default.Lightbulb
    "auto_awesome" -> Icons.Default.AutoAwesome
    "home" -> Icons.Default.Home
    "directions_car" -> Icons.Default.DirectionsCar
    "flight" -> Icons.Default.Flight
    "sailing" -> Icons.Default.Sailing
    "language" -> Icons.Default.Language
    "palette" -> Icons.Default.Palette
    "dark_mode" -> Icons.Default.DarkMode
    "wb_sunny" -> Icons.Default.WbSunny
    else -> Icons.Default.Folder
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
) {
    val categories by viewModel.categories.collectAsState()
    val state by viewModel.uiState.collectAsState()

    val screenBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.categories_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showDialog,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.categories_add_fab_cd))
            }
        },
    ) { padding ->
        if (categories == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (categories!!.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
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
                        stringResource(R.string.categories_empty),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.categories_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(Dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            items(categories!!, key = { it.id }) { cat ->
                val catColor = runCatching { Color(android.graphics.Color.parseColor(cat.color)) }
                    .getOrDefault(MaterialTheme.colorScheme.primary)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.cornerXl),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    ListItem(
                        colors = androidx.compose.material3.ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(Dimens.size5xl)
                                    .clip(CircleShape)
                                    .background(catColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = categoryIconVector(cat.icon),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(Dimens.sizeMd),
                                )
                            }
                        },
                        headlineContent = { Text(cat.name) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.delete(cat) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.categories_delete_cd))
                            }
                        },
                    )
                }
            }
        }
        } // end else
    }

    if (state.showAddDialog) {
        AddCategoryDialog(
            state = state,
            onNameChange = viewModel::setNewName,
            onIconSelect = viewModel::setSelectedIcon,
            onColorSelect = viewModel::setSelectedColor,
            onConfirm = viewModel::addCategory,
            onDismiss = viewModel::dismissDialog,
        )
    }
}

@Composable
private fun AddCategoryDialog(
    state: CategoryUiState,
    onNameChange: (String) -> Unit,
    onIconSelect: (String) -> Unit,
    onColorSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories_new_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            ) {
                OutlinedTextField(
                    value = state.newName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.categories_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.categories_icon_label), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
                    CATEGORY_ICON_KEYS.chunked(6).forEach { rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
                        ) {
                            rowKeys.forEach { key ->
                                val isSelected = key == state.selectedIcon
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.sizeCard)
                                        .clip(RoundedCornerShape(Dimens.cornerSm))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isSelected) Dimens.borderSelected else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(Dimens.cornerSm),
                                        )
                                        .clickable { onIconSelect(key) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = categoryIconVector(key),
                                        contentDescription = key,
                                        modifier = Modifier.size(Dimens.sizeLg),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(0.dp))
                Text(stringResource(R.string.categories_color_label), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                    CATEGORY_COLORS.chunked(8).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                            rowColors.forEach { hex ->
                                val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                    .getOrDefault(MaterialTheme.colorScheme.primary)
                                val isSelected = hex == state.selectedColor
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.size3xl)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSelected) Modifier.border(Dimens.borderSelected, Color.White, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { onColorSelect(hex) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.newName.isNotBlank()) {
                Text(stringResource(R.string.categories_add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
