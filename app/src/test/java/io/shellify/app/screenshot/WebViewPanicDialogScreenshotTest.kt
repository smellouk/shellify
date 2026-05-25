package io.shellify.app.screenshot

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.presentation.components.ConfirmDialog
import io.shellify.app.presentation.theme.ShellifyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot regression test for the panic wipe ConfirmDialog (PRIV-04).
 *
 * Verifies that the dialog renders with:
 * - Warning icon in error/destructive tint
 * - "Wipe all data?" title
 * - Body copy explaining the destructive nature
 * - "Wipe everything" confirm button
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WebViewPanicDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun panicConfirmDialog_shown() {
        composeTestRule.setContent {
            ShellifyTheme {
                ConfirmDialog(
                    icon = Icons.Default.Warning,
                    isDestructive = true,
                    title = "Wipe all data?",
                    body = "This will permanently delete all apps, sessions, and settings. This cannot be undone.",
                    confirmLabel = "Wipe everything",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
