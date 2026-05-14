package io.shellify.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.shellify.app.presentation.theme.Dimens
import io.shellify.core.ui.R

private data class ChangelogRelease(
    val version: String,
    val date: String,
    val sections: List<ChangelogSection>,
)

private data class ChangelogSection(
    val title: String,
    val items: List<String>,
)

private val changelog = listOf(
    ChangelogRelease(
        version = "1.0.0",
        date = "2025",
        sections = listOf(
            ChangelogSection(
                title = "Features",
                items = listOf(
                    "Add and manage web apps as native-like PWAs",
                    "Organize apps with custom categories",
                    "Create and manage home screen shortcuts",
                    "WebView and GeckoView (Firefox) browser engine support",
                    "Per-app ad blocking",
                    "In-app translation via system translator",
                    "Fullscreen mode per app",
                ),
            ),
            ChangelogSection(
                title = "Appearance",
                items = listOf(
                    "Light, dark, and system-follow themes",
                    "Material You dynamic color on Android 12+",
                    "Five accent color options",
                    "Custom icon packs via SimpleIcons",
                ),
            ),
            ChangelogSection(
                title = "Security & Privacy",
                items = listOf(
                    "App lock with password or biometric authentication",
                    "Per-app data isolation",
                    "Screenshot protection",
                ),
            ),
            ChangelogSection(
                title = "Backup",
                items = listOf(
                    "Export and import full app data backups",
                    "Encrypted backup support",
                    "Scheduled automatic backups",
                ),
            ),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.changelog_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceXxs),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            items(changelog) { release ->
                ReleaseCard(release)
            }
            item { Spacer(Modifier.height(Dimens.spaceLg)) }
        }
    }
}

@Composable
private fun ReleaseCard(release: ChangelogRelease) {
    Surface(
        shape = RoundedCornerShape(Dimens.cornerXl),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spaceLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "v${release.version}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    release.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            release.sections.forEach { section ->
                Spacer(Modifier.height(Dimens.spaceMd))
                Text(
                    section.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.spaceXxs))
                section.items.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Default.Circle,
                            null,
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(5.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(Dimens.spaceXs))
                        Text(
                            item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
