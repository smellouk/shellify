package dev.pwaforge.presentation.translate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pwaforge.R
import dev.pwaforge.presentation.theme.Dimens
import dev.pwaforge.domain.model.TranslateLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateConfigScreen(
    viewModel: TranslateConfigViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val app = state.app
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.translate_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) }
                },
            )
        },
    ) { padding ->
        if (app == null) return@Scaffold
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Dimens.spaceLg),
        ) {
            // Language selector
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = app.translateTarget.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.translate_target_language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).padding(bottom = Dimens.spaceLg),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    TranslateLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.displayName) },
                            onClick = { viewModel.setLanguage(lang); expanded = false },
                        )
                    }
                }
            }

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.translate_show_button)) },
                supportingContent = { Text(stringResource(R.string.translate_show_button_desc)) },
                trailingContent = {
                    Switch(checked = app.showTranslateButton, onCheckedChange = viewModel::setShowButton)
                },
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.translate_auto)) },
                supportingContent = { Text(stringResource(R.string.translate_auto_desc)) },
                trailingContent = {
                    Switch(checked = app.autoTranslateOnLoad, onCheckedChange = viewModel::setAutoTranslate)
                },
            )
        }
    }
}
