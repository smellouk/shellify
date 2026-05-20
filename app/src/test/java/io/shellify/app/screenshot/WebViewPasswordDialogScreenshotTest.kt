package io.shellify.app.screenshot

import android.graphics.Color as AndroidColor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.presentation.webview.WebViewPasswordDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WebViewPasswordDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noError_withPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme(accentColor = AndroidColor.parseColor("#1DB954")) {
                WebViewPasswordDialog(
                    appName = "Spotify",
                    errorMessage = null,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun noError_withoutPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme {
                WebViewPasswordDialog(
                    appName = "Spotify",
                    errorMessage = null,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun withError_withPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme(accentColor = AndroidColor.parseColor("#1DB954")) {
                WebViewPasswordDialog(
                    appName = "Spotify",
                    errorMessage = "Wrong password",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun withError_withoutPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme {
                WebViewPasswordDialog(
                    appName = "Spotify",
                    errorMessage = "Wrong password",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
