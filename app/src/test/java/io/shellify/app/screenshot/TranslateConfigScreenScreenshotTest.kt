package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.domain.model.TranslateLanguage
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.presentation.translate.TranslateConfigScreen
import io.shellify.app.presentation.translate.TranslateConfigUiState
import io.shellify.app.presentation.translate.TranslateConfigViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TranslateConfigScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(state: TranslateConfigUiState): TranslateConfigViewModel =
        mockk<TranslateConfigViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
        }

    private fun app(
        translateTarget: TranslateLanguage = TranslateLanguage.FRENCH,
        autoTranslateOnLoad: Boolean = false,
    ) = WebApp(
        id = 1L,
        name = "Gmail",
        url = "https://mail.google.com",
        isolationId = UUID.randomUUID().toString(),
        isFullscreen = false,
        adBlockEnabled = true,
        translateEnabled = true,
        translateTarget = translateTarget,
        autoTranslateOnLoad = autoTranslateOnLoad,
    )

    @Test
    fun loadingState() {
        composeTestRule.setContent {
            ShellifyTheme {
                TranslateConfigScreen(
                    viewModel = buildVm(TranslateConfigUiState(app = null, isLoading = true)),
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun withApp() {
        composeTestRule.setContent {
            ShellifyTheme {
                TranslateConfigScreen(
                    viewModel = buildVm(
                        TranslateConfigUiState(app = app(TranslateLanguage.FRENCH), isLoading = false)
                    ),
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun withAutoTranslateOn() {
        composeTestRule.setContent {
            ShellifyTheme {
                TranslateConfigScreen(
                    viewModel = buildVm(
                        TranslateConfigUiState(
                            app = app(TranslateLanguage.GERMAN, autoTranslateOnLoad = true),
                            isLoading = false,
                        )
                    ),
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
