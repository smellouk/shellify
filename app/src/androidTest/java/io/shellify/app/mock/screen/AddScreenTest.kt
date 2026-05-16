package io.shellify.app.mock.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.presentation.add.AddScreen
import io.shellify.app.presentation.add.AddUiState
import io.shellify.app.presentation.add.AddViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for AddScreen.
 *
 * The AddViewModel has a dependency on GeckoEngineManager (exposed as a property),
 * so both are mocked. The state flow drives the UI state directly.
 */
@RunWith(AndroidJUnit4::class)
class AddScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(uiState: AddUiState): AddViewModel {
        val geckoManager = mockk<GeckoEngineManager>(relaxed = true)
        every { geckoManager.installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)

        val vm = mockk<AddViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(uiState)
        every { vm.geckoEngineManager } returns geckoManager
        return vm
    }

    private fun setAddScreen(uiState: AddUiState) {
        composeTestRule.setContent {
            ShellifyTheme {
                AddScreen(
                    viewModel = buildViewModel(uiState),
                    onSaved = {},
                    onBack = {},
                )
            }
        }
    }

    // ─── Top bar / title ──────────────────────────────────────────────────────

    @Test
    fun topBar_showsCreateAppTitle_whenNameAndUrlAreEmpty() {
        setAddScreen(AddUiState(name = "", url = ""))
        composeTestRule
            .onNodeWithText("Create App")
            .assertIsDisplayed()
    }

    @Test
    fun topBar_showsCreateAppTitle_whenFormIsPreFilled() {
        setAddScreen(AddUiState(name = "My App", url = "https://example.com"))
        composeTestRule
            .onNodeWithText("Create App")
            .assertIsDisplayed()
    }

    // ─── URL field ────────────────────────────────────────────────────────────

    @Test
    fun urlField_isDisplayed() {
        setAddScreen(AddUiState())
        composeTestRule
            .onNodeWithText("Website URL")
            .assertIsDisplayed()
    }

    @Test
    fun urlField_showsPrefilledUrl() {
        setAddScreen(AddUiState(url = "https://github.com"))
        composeTestRule
            .onNodeWithText("https://github.com")
            .assertIsDisplayed()
    }

    // ─── Name field ───────────────────────────────────────────────────────────

    @Test
    fun nameField_isDisplayed() {
        setAddScreen(AddUiState())
        composeTestRule
            .onNodeWithText("App name")
            .assertIsDisplayed()
    }

    @Test
    fun nameField_showsPrefilledName() {
        setAddScreen(AddUiState(name = "GitHub"))
        composeTestRule
            .onNodeWithText("GitHub")
            .assertIsDisplayed()
    }

    // ─── Analyze button ───────────────────────────────────────────────────────

    @Test
    fun analyzeButton_isDisplayedWhenNotAnalyzing() {
        setAddScreen(AddUiState(isAnalyzing = false, url = "https://example.com"))
        composeTestRule
            .onNodeWithContentDescription("Analyze site")
            .assertIsDisplayed()
    }

    @Test
    fun analyzeButton_showsAnalyzingIndicator_duringAnalysis() {
        setAddScreen(AddUiState(isAnalyzing = true, url = "https://example.com"))
        composeTestRule
            .onNodeWithContentDescription("Analyzing")
            .assertIsDisplayed()
    }

    // ─── URL validation error ──────────────────────────────────────────────────

    @Test
    fun urlError_isDisplayedWhenPresent() {
        setAddScreen(AddUiState(urlError = "Please enter a valid URL"))
        composeTestRule
            .onNodeWithText("Please enter a valid URL")
            .assertIsDisplayed()
    }

    // ─── Settings section ─────────────────────────────────────────────────────

    @Test
    fun settingsSection_showsAdBlockLabel() {
        setAddScreen(AddUiState(name = "App", url = "https://example.com"))
        composeTestRule
            .onNodeWithText("Ad Blocking")
            .assertIsDisplayed()
    }

    @Test
    fun settingsSection_showsFullscreenLabel() {
        setAddScreen(AddUiState(name = "App", url = "https://example.com"))
        composeTestRule
            .onNodeWithText("Fullscreen Mode")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun settingsSection_showsAutoTranslateLabel() {
        setAddScreen(AddUiState(name = "App", url = "https://example.com"))
        composeTestRule
            .onNodeWithText("Auto Translate")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
