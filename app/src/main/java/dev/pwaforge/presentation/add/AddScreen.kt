package dev.pwaforge.presentation.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.domain.model.UserAgentMode
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.uiState.value.let { it.name.isEmpty() && it.url.isEmpty() }) "Add PWA" else "Edit App") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // URL + Analyze
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text("Website URL") },
                    placeholder = { Text("https://example.com") },
                    isError = state.urlError != null,
                    supportingText = { if (state.urlError != null) Text(state.urlError!!) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = viewModel::analyze,
                    enabled = !state.isAnalyzing,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (state.isAnalyzing) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Icon(Icons.Default.Search, "Analyze")
                }
            }

            if (state.analyzeError != null) {
                Text(state.analyzeError!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // Icon preview
            if (state.iconPath != null || state.name.isNotBlank()) {
                AppIcon(
                    app = dev.pwaforge.domain.model.WebApp(
                        name = state.name, url = state.url,
                        iconPath = state.iconPath, themeColor = state.themeColor,
                    ),
                    modifier = Modifier.size(72.dp).align(Alignment.CenterHorizontally),
                )
            }

            // App name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("App name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Text("Settings", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            // Full screen
            SettingToggleRow("Full screen mode", state.isFullscreen, viewModel::setFullscreen)
            // Ad block
            SettingToggleRow("Block ads", state.adBlockEnabled, viewModel::setAdBlock)
            // Translate
            SettingToggleRow("Auto-translate", state.translateEnabled, viewModel::setTranslate)

            // UA mode
            UaModeSelector(selected = state.uaMode, onSelect = viewModel::setUaMode)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.save { savedApp ->
                        PwaShortcutManager.createShortcut(context, savedApp)
                    }
                },
                enabled = state.name.isNotBlank() && state.url.isNotBlank() && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Save")
            }
        }
    }
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UaModeSelector(selected: UserAgentMode, onSelect: (UserAgentMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Browser identity") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UserAgentMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = { onSelect(mode); expanded = false },
                )
            }
        }
    }
}
