package io.shellify.app.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.presentation.onboarding.ConsentScreen
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
class ConsentScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun initialState_agreeDisabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    ConsentScreen(onAccepted = {})
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
