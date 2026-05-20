package io.shellify.app.screenshot

import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.presentation.webview.WebViewControlCenter
import io.shellify.app.presentation.webview.WebViewControlCenterSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WebViewControlCenterScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val pwaAppWithTheme = WebApp(
        name = "Spotify",
        url = "https://open.spotify.com",
        themeColor = "#1DB954",
        adBlockEnabled = true,
    )

    private val pwaAppNoTheme = WebApp(
        name = "GitHub",
        url = "https://github.com",
    )

    @Test
    fun fab_withPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme(accentColor = AndroidColor.parseColor("#1DB954")) {
                WebViewControlCenter(
                    pwaApp = pwaAppWithTheme,
                    hasGlobalPassword = true,
                    onAdBlockChanged = {},
                    onTranslateChanged = {},
                    onFullscreenChanged = {},
                    onLockChanged = {},
                    onClearData = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun fab_withoutPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme {
                WebViewControlCenter(
                    pwaApp = pwaAppNoTheme,
                    hasGlobalPassword = true,
                    onAdBlockChanged = {},
                    onTranslateChanged = {},
                    onFullscreenChanged = {},
                    onLockChanged = {},
                    onClearData = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun sheet_withPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme(accentColor = AndroidColor.parseColor("#1DB954")) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    WebViewControlCenterSheet(
                        pwaApp = pwaAppWithTheme,
                        hasGlobalPassword = true,
                        onAdBlockChanged = {},
                        onTranslateChanged = {},
                        onFullscreenChanged = {},
                        onLockChanged = {},
                    onClearData = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun sheet_withoutPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    WebViewControlCenterSheet(
                        pwaApp = pwaAppNoTheme,
                        hasGlobalPassword = true,
                        onAdBlockChanged = {},
                        onTranslateChanged = {},
                        onFullscreenChanged = {},
                        onLockChanged = {},
                    onClearData = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
