package io.shellify.app.presentation.webview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.shellify.app.presentation.theme.Dimens
import io.shellify.core.ui.R

@Composable
fun WebViewErrorScreen(
    error: WebLoadError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isRetrying: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "retry")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    val icon: ImageVector
    val titleRes: Int
    val bodyRes: Int
    when (error) {
        WebLoadError.NoInternet -> {
            icon = Icons.Default.WifiOff
            titleRes = R.string.webview_error_no_internet_title
            bodyRes = R.string.webview_error_no_internet_body
        }
        WebLoadError.CannotReach -> {
            icon = Icons.Default.CloudOff
            titleRes = R.string.webview_error_unreachable_title
            bodyRes = R.string.webview_error_unreachable_body
        }
        WebLoadError.SslError -> {
            icon = Icons.Default.LockOpen
            titleRes = R.string.webview_error_ssl_title
            bodyRes = R.string.webview_error_ssl_body
        }
        WebLoadError.Timeout -> {
            icon = Icons.Default.Warning
            titleRes = R.string.webview_error_timeout_title
            bodyRes = R.string.webview_error_timeout_body
        }
        is WebLoadError.Generic -> {
            icon = Icons.Default.Warning
            titleRes = R.string.webview_error_generic_title
            bodyRes = R.string.webview_error_generic_body
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimens.spaceXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.spaceXl))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.spaceMd))
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.spaceXxl))
        Button(onClick = onRetry) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .size(Dimens.sizeMd)
                    .rotate(if (isRetrying) rotation else 0f),
            )
            Spacer(Modifier.width(Dimens.spaceSm))
            Text(stringResource(R.string.webview_error_retry))
        }
    }
}
