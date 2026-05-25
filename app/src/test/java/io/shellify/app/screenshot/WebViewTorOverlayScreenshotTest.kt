package io.shellify.app.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.presentation.theme.Dimens
import io.shellify.app.presentation.theme.ShellifyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for the full-screen Tor bootstrapping overlay rendered in
 * [io.shellify.app.presentation.webview.WebViewActivity.addTorConnectingOverlay].
 *
 * The overlay composable tree is inlined inside a [ComposeView] inside the Activity and cannot be
 * extracted without touching production code, so we replicate the identical tree here.
 * Any structural change to the overlay must be reflected here as well.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WebViewTorOverlayScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Renders the overlay content in isolation.
     *
     * @param isConnecting true when Tor is in [TorState.Connecting] — shows spinner branch.
     * @param errorMessage non-null when Tor is in [TorState.Error] — shows warning branch.
     * @param titleText the headline string to display (mirrors the string-resource selection
     *   in the Activity: [R.string.webview_tor_connecting] or [R.string.webview_tor_loading]).
     */
    private fun renderOverlay(
        isError: Boolean,
        errorMessage: String = "",
        isConnecting: Boolean = false,
        titleText: String = "",
    ) {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
                    ) {
                        if (isError) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.size5xl),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "Tor failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = Dimens.spaceXxl),
                            )
                            Button(onClick = {}) {
                                Text("Retry")
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.VpnLock,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.size5xl),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun torConnectingOverlay_connectingState() {
        renderOverlay(
            isError = false,
            isConnecting = true,
            titleText = "Connecting to Tor network…",
        )
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun torConnectingOverlay_errorState() {
        renderOverlay(
            isError = true,
            errorMessage = "SELinux denied",
        )
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
