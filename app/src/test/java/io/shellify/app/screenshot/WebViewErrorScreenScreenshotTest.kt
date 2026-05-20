package io.shellify.app.screenshot

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
class WebViewErrorScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun render(error: WebLoadError) {
        composeTestRule.setContent {
            ShellifyTheme { WebViewErrorScreen(error = error, onRetry = {}) }
        }
    }

    @Test
    fun noInternet() {
        render(WebLoadError.NoInternet)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun cannotReach() {
        render(WebLoadError.CannotReach)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun sslError() {
        render(WebLoadError.SslError)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun timeout() {
        render(WebLoadError.Timeout)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun generic() {
        render(WebLoadError.Generic("net::ERR_FAILED"))
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun retrying() {
        composeTestRule.setContent {
            ShellifyTheme {
                WebViewErrorScreen(
                    error = WebLoadError.NoInternet,
                    isRetrying = true,
                    onRetry = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
