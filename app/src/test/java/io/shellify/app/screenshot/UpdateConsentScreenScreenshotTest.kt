package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.presentation.onboarding.UpdateConsentScreen
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
class UpdateConsentScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unchecked() {
        composeTestRule.setContent {
            ShellifyTheme { UpdateConsentScreen(onAccepted = {}) }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun checked() {
        composeTestRule.setContent {
            ShellifyTheme { UpdateConsentScreen(onAccepted = {}) }
        }
        composeTestRule.onNodeWithTag("update_consent_checkbox").performClick()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
