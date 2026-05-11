package dev.pwaforge.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.pwaforge.core.shortcut.PwaShortcutManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onOpenTranslate: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val app = state.app

    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app?.name ?: "App settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        if (app == null) return@Scaffold

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("Display")
            SettingsCard {
                ToggleListItem("Full screen", app.isFullscreen, viewModel::toggleFullscreen)
            }

            SectionLabel("Privacy")
            SettingsCard {
                ToggleListItem("Block ads", app.adBlockEnabled, viewModel::toggleAdBlock)
                HorizontalDivider()
                ToggleListItem("Auto-translate", app.translateEnabled, viewModel::toggleTranslate)
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Translation language") },
                    trailingContent = {
                        TextButton(onClick = onOpenTranslate) {
                            Icon(Icons.Default.Translate, null)
                            Text(app.translateTarget.displayName)
                        }
                    },
                )
            }

            SectionLabel("Shortcut")
            SettingsCard {
                ListItem(
                    headlineContent = { Text("Add to home screen") },
                    supportingContent = { Text("Creates a shortcut on your launcher") },
                    trailingContent = {
                        IconButton(onClick = { PwaShortcutManager.createShortcut(context, app) }) {
                            Icon(Icons.AutoMirrored.Filled.Shortcut, "Create shortcut")
                        }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Danger zone")

            Button(
                onClick = viewModel::showDeleteDialog,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Delete, null)
                Text("  Delete app")
            }
        }

        if (state.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteDialog,
                title = { Text("Delete \"${app.name}\"?") },
                text = { Text("This will remove the app and all its stored data.") },
                confirmButton = {
                    TextButton(onClick = viewModel::deleteApp) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteDialog) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp))

@Composable
private fun SettingsCard(content: @Composable () -> Unit) =
    Card(elevation = CardDefaults.cardElevation(2.dp)) { content() }

@Composable
private fun ToggleListItem(label: String, checked: Boolean, onToggle: () -> Unit) =
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = { onToggle() }) },
    )
