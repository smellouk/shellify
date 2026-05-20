package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.presentation.add.AddScreen
import io.shellify.app.presentation.add.AddUiState
import io.shellify.app.presentation.add.AddViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AddScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(state: AddUiState): AddViewModel {
        val gecko = mockk<GeckoEngineManager>(relaxed = true).also {
            every { it.installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
        }
        return mockk<AddViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
            every { it.geckoEngineManager } returns gecko
        }
    }

    @Test
    fun createNew_emptyForm() {
        composeTestRule.setContent {
            ShellifyTheme {
                AddScreen(viewModel = buildVm(AddUiState(name = "", url = "")), onSaved = {}, onBack = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun editExisting_prefilledForm() {
        composeTestRule.setContent {
            ShellifyTheme {
                AddScreen(
                    viewModel = buildVm(AddUiState(name = "GitHub", url = "https://github.com", adBlockEnabled = true)),
                    onSaved = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun analyzing_loadingIndicator() {
        composeTestRule.setContent {
            ShellifyTheme {
                AddScreen(
                    viewModel = buildVm(AddUiState(url = "https://notion.so", isAnalyzing = true)),
                    onSaved = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun urlValidationError() {
        composeTestRule.setContent {
            ShellifyTheme {
                AddScreen(
                    viewModel = buildVm(AddUiState(url = "not-a-url", urlError = "Please enter a valid URL")),
                    onSaved = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun urlError_httpScheme() {
        composeTestRule.setContent {
            ShellifyTheme {
                AddScreen(
                    viewModel = buildVm(AddUiState(url = "http://example.com", urlError = "Use HTTPS instead of HTTP")),
                    onSaved = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun urlError_blankUrl() {
        composeTestRule.setContent {
            ShellifyTheme {
                AddScreen(
                    viewModel = buildVm(AddUiState(url = "", urlError = "Please enter a URL")),
                    onSaved = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun nameError_blank() {
        composeTestRule.setContent {
            ShellifyTheme {
                AddScreen(
                    viewModel = buildVm(AddUiState(url = "https://example.com", nameError = "Please enter a name")),
                    onSaved = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
