package io.shellify.app.screenshot

import android.graphics.Color as AndroidColor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.presentation.webview.WebLoadError
import io.shellify.app.presentation.webview.WebViewErrorScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WebViewPwaThemingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorScreen_withPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme(accentColor = AndroidColor.parseColor("#1DB954")) {
                WebViewErrorScreen(error = WebLoadError.NoInternet, onRetry = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun errorScreen_withoutPwaTheme() {
        composeTestRule.setContent {
            ShellifyTheme {
                WebViewErrorScreen(error = WebLoadError.NoInternet, onRetry = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
