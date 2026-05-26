package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.presentation.onboarding.WhatsNewScreen
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
class WhatsNewScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun whatsNewScreen_defaultState() {
        composeRule.setContent {
            ShellifyTheme {
                WhatsNewScreen(onDismissed = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
