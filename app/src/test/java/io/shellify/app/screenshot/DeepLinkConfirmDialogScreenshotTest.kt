package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.presentation.navigation.DeepLinkConfirmDialog
import io.shellify.app.presentation.theme.ShellifyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeepLinkConfirmDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun withUrl() {
        composeTestRule.setContent {
            ShellifyTheme {
                DeepLinkConfirmDialog(
                    url = "https://github.com/shellify/app",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
