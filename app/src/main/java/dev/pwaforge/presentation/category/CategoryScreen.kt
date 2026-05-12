package dev.pwaforge.presentation.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val screenBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
            )
        },
    ) { padding ->
        if (categories == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (categories!!.isEmpty()) {
            val p97 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            val p95 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            val p90 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f)
            val p40 = MaterialTheme.colorScheme.primary
            val surfDim = MaterialTheme.colorScheme.outlineVariant
            val surf = MaterialTheme.colorScheme.surface

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))

                // 160×160 illustration — 3 filled rings + single dashed orbit
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size(160.dp).background(p97, CircleShape))
                    Box(modifier = Modifier.size(116.dp).background(p95, CircleShape))
                    Box(modifier = Modifier.size(72.dp).background(p90, CircleShape))
                    Canvas(modifier = Modifier.size(160.dp)) {
                        drawCircle(
                            color = p40.copy(alpha = 0.35f),
                            radius = 70.dp.toPx(),
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(3.dp.toPx(), 7.dp.toPx()), 0f,
                                ),
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(p40, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Layers, null, modifier = Modifier.size(30.dp), tint = Color.White)
                    }
                    // Ghost tiles — same positions as apps screen
                    Box(modifier = Modifier.size(26.dp).offset(x = (-49).dp, y = (-57).dp)
                        .background(surf, RoundedCornerShape(8.dp)).border(1.dp, surfDim, RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(22.dp).offset(x = 57.dp, y = (-45).dp)
                        .background(surf, RoundedCornerShape(7.dp)).border(1.dp, surfDim, RoundedCornerShape(7.dp)))
                    Box(modifier = Modifier.size(22.dp).offset(x = (-61).dp, y = 51.dp)
                        .background(surf, RoundedCornerShape(7.dp)).border(1.dp, surfDim, RoundedCornerShape(7.dp)))
                    Box(modifier = Modifier.size(26.dp).offset(x = 41.dp, y = 59.dp)
                        .background(surf, RoundedCornerShape(8.dp)).border(1.dp, surfDim, RoundedCornerShape(8.dp)))
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Sort apps into spaces",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Group your tiles by purpose — Work, Reading, Media — then jump between them from the drawer.",
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = viewModel::showDialog,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 22.dp),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("New category", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(20.dp))

                // Suggestion chips — 2×2 wrap, dashed border, maxWidth 280dp
                val chipData = listOf(
                    Triple("Media",   Icons.Default.Bolt,     Color(0xFF7A5300) to Color(0xFFFFE7BD)),
                    Triple("Work",    Icons.Default.Apps,     p40               to p90),
                    Triple("Reading", Icons.Default.Home,     Color(0xFFB5365E) to Color(0xFFFFD9E2)),
                    Triple("Tools",   Icons.Default.GridView, Color(0xFF006B5F) to Color(0xFFDAF8F2)),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.widthIn(max = 280.dp),
                ) {
                    chipData.chunked(2).forEach { rowChips ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowChips.forEach { (label, icon, colors) ->
                                val (fg, bg) = colors
                                Row(
                                    modifier = Modifier
                                        .background(bg, RoundedCornerShape(100.dp))
                                        .drawBehind {
                                            drawRoundRect(
                                                color = surfDim,
                                                cornerRadius = CornerRadius(100.dp.toPx()),
                                                style = Stroke(
                                                    width = 1.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(
                                                        floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f,
                                                    ),
                                                ),
                                            )
                                        }
                                        .clickable { viewModel.showDialog() }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(icon, null, modifier = Modifier.size(13.dp), tint = fg)
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = fg)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Dimens.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            ) {
                items(categories!!, key = { it.id }) { cat ->
                    val catColor = runCatching { Color(android.graphics.Color.parseColor(cat.color)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(catColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = categoryIconVector(cat.icon),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.delete(cat) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.categories_delete_cd),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                cat.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(20.dp))
                            .clickable { viewModel.showDialog() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                "New category",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
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
