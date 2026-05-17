package io.shellify.app.presentation.onboarding

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.shellify.app.presentation.theme.Dimens
import io.shellify.core.ui.R

@Composable
fun ConsentScreen(onAccepted: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var checked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            Text(
                stringResource(R.string.consent_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.consent_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Dimens.spaceXxs))

            ConsentSection(
                title = stringResource(R.string.consent_section_what_title),
                items = listOf(
                    stringResource(R.string.consent_what_1),
                    stringResource(R.string.consent_what_2),
                    stringResource(R.string.consent_what_3),
                    stringResource(R.string.consent_what_4),
                ),
            )

            ConsentSection(
                title = stringResource(R.string.consent_section_not_title),
                items = listOf(
                    stringResource(R.string.consent_not_1),
                    stringResource(R.string.consent_not_2),
                    stringResource(R.string.consent_not_3),
                    stringResource(R.string.consent_not_4),
                    stringResource(R.string.consent_not_5),
                ),
            )

            ConsentSection(
                title = stringResource(R.string.consent_section_acceptable_title),
                intro = stringResource(R.string.consent_acceptable_intro),
                items = listOf(
                    stringResource(R.string.consent_acceptable_1),
                    stringResource(R.string.consent_acceptable_2),
                    stringResource(R.string.consent_acceptable_3),
                    stringResource(R.string.consent_acceptable_4),
                    stringResource(R.string.consent_acceptable_5),
                    stringResource(R.string.consent_acceptable_6),
                    stringResource(R.string.consent_acceptable_7),
                ),
                footer = stringResource(R.string.consent_acceptable_reserve),
            )

            ConsentSection(
                title = stringResource(R.string.consent_section_responsibility_title),
                items = listOf(
                    stringResource(R.string.consent_responsibility_1),
                    stringResource(R.string.consent_responsibility_2),
                    stringResource(R.string.consent_responsibility_3),
                    stringResource(R.string.consent_responsibility_4),
                    stringResource(R.string.consent_responsibility_5),
                ),
            )

            ConsentSection(
                title = stringResource(R.string.consent_section_legal_title),
                intro = stringResource(R.string.consent_legal_intro),
                items = listOf(
                    stringResource(R.string.consent_legal_1),
                    stringResource(R.string.consent_legal_2),
                    stringResource(R.string.consent_legal_3),
                    stringResource(R.string.consent_legal_4),
                ),
            )

            ConsentSection(
                title = stringResource(R.string.consent_section_privacy_title),
                intro = stringResource(R.string.consent_privacy_desc),
                items = emptyList(),
            )

            Column(modifier = Modifier.padding(top = Dimens.spaceXxs)) {
                TextButton(onClick = { uriHandler.openUri("https://shellify.app/privacy.html") }) {
                    Text(stringResource(R.string.consent_read_privacy))
                }
                TextButton(onClick = { uriHandler.openUri("https://shellify.app/terms.html") }) {
                    Text(stringResource(R.string.consent_read_tos))
                }
                TextButton(onClick = { uriHandler.openUri("mailto:contact@shellify.app") }) {
                    Text(stringResource(R.string.consent_contact))
                }
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier = Modifier.testTag("consent_checkbox"),
                )
                Text(
                    stringResource(R.string.consent_checkbox_label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = onAccepted,
                enabled = checked,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("consent_agree_button"),
            ) {
                Text(stringResource(R.string.consent_agree_button))
            }

            TextButton(
                onClick = { (context as? Activity)?.finish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("consent_decline_button"),
            ) {
                Text(
                    stringResource(R.string.consent_decline_button),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConsentSection(
    title: String,
    items: List<String>,
    intro: String? = null,
    footer: String? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spaceMd)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (intro != null) {
                Spacer(Modifier.height(Dimens.spaceXxs))
                Text(
                    intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.spaceXxs))
                items.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                    ) {
                        Text(
                            "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (footer != null) {
                Spacer(Modifier.height(Dimens.spaceXxs))
                Text(
                    footer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
