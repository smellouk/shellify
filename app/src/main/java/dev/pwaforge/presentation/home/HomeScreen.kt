package dev.pwaforge.presentation.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.presentation.webview.WebViewActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddApp: () -> Unit,
    onEditApp: (Long) -> Unit,
    onOpenApp: (WebApp) -> Unit,
    onOpenSettings: (Long) -> Unit,
    onOpenCategories: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::setSearch,
                            placeholder = { Text("Search apps…") },
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
                        Text("PWAForge")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) viewModel.setSearch("") }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenCategories) {
                        Icon(Icons.Default.Category, contentDescription = "Categories")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddApp) {
                Icon(Icons.Default.Add, contentDescription = "Add PWA")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Category filter chips
            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All") },
                        )
                    }
                    items(state.categories) { cat ->
                        FilterChip(
                            selected = state.selectedCategoryId == cat.id,
                            onClick = { viewModel.selectCategory(cat.id) },
                            label = { Text(cat.name) },
                        )
                    }
                }
            }

            if (state.apps.isEmpty() && !state.isLoading) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.apps, key = { it.id }) { app ->
                        AppCard(
                            app = app,
                            onClick = {
                                context.startActivity(
                                    Intent(context, WebViewActivity::class.java)
                                        .putExtra(WebViewActivity.EXTRA_APP_ID, app.id)
                                )
                            },
                            onEdit = { onEditApp(app.id) },
                            onSettings = { onOpenSettings(app.id) },
                            onDelete = { viewModel.delete(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            // Phone + sparkle icon stack
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 28.dp, y = (-16).dp)
                        .align(Alignment.TopEnd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            Text(
                "No apps yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                "Tap the button below to create your first app",
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
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app = app, modifier = Modifier.size(48.dp))
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu",
                            modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { showMenu = false; onSettings() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { showMenu = false; onDelete() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(app.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                app.url.removePrefix("https://").removePrefix("http://"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
        )
    } else {
        val color = runCatching { Color(android.graphics.Color.parseColor(app.themeColor)) }
            .getOrDefault(MaterialTheme.colorScheme.primary)
        Box(
            modifier = modifier.clip(RoundedCornerShape(12.dp)).background(color),
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
