package io.shellify.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.shellify.app.presentation.theme.Dimens
import io.shellify.core.ui.R

@Composable
fun WhatsNewScreen(onDismissed: () -> Unit) {
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
            // Hero
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.spaceXxl),
            )

            Text(
                stringResource(R.string.whats_new_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.whats_new_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Dimens.spaceXxs))

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    FeatureRow(
                        icon = Icons.Rounded.MenuBook,
                        title = stringResource(R.string.whats_new_reading_mode_title),
                        body = stringResource(R.string.whats_new_reading_mode_body),
                    )
                    FeatureRow(
                        icon = Icons.Rounded.VerifiedUser,
                        title = stringResource(R.string.whats_new_reader_security_title),
                        body = stringResource(R.string.whats_new_reader_security_body),
                    )
                    FeatureRow(
                        icon = Icons.Rounded.VpnLock,
                        title = stringResource(R.string.whats_new_tor_reliability_title),
                        body = stringResource(R.string.whats_new_tor_reliability_body),
                    )
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = onDismissed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
        ) {
            Text(stringResource(R.string.whats_new_got_it))
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.spaceLg + Dimens.spaceXxs),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
