package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.presentation.onboarding.OnboardingScreen
import io.shellify.app.presentation.onboarding.OnboardingUiState
import io.shellify.app.presentation.onboarding.OnboardingViewModel
import io.shellify.app.presentation.onboarding.QuickPicksStatus
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
class OnboardingScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(state: OnboardingUiState): OnboardingViewModel =
        mockk<OnboardingViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
        }

    private fun state(page: Int, block: OnboardingUiState.() -> OnboardingUiState = { this }) =
        OnboardingUiState(page = page).block()

    @Test
    fun page0_welcome() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(state(0)),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun page1_what() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(state(1)),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun page2_appearance() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(state(2) { copy(themeMode = ThemeMode.DARK) }),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun page3_security() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(state(3)),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun page3_securityPasswordSet() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(state(3) { copy(passwordSet = true) }),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun page4_backup() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(state(4)),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun page5_quickPicks() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(
                        state(5) { copy(pickedAppIds = listOf("proton", "bitwarden")) }
                    ),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun page5_quickPicksAdding() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(
                        state(5) {
                            copy(
                                pickedAppIds = listOf("proton", "bitwarden", "element"),
                                quickPicksStatus = QuickPicksStatus.Adding(done = 1, total = 3),
                            )
                        }
                    ),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun page6_done() {
        composeTestRule.setContent {
            ShellifyTheme {
                OnboardingScreen(
                    viewModel = buildVm(state(6)),
                    onFinished = {},
                    onLanguageChange = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
