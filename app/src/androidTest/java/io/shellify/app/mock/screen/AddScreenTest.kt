package io.shellify.app.mock.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.presentation.add.AddScreen
import io.shellify.app.presentation.add.AddUiState
import io.shellify.app.presentation.add.AddViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.core.ui.R as CoreUiR
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

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

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
            .onNodeWithText(context.getString(CoreUiR.string.add_create_app))
            .assertIsDisplayed()
    }

    @Test
    fun topBar_showsCreateAppTitle_whenFormIsPreFilled() {
        setAddScreen(AddUiState(name = "My App", url = "https://example.com"))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_create_app))
            .assertIsDisplayed()
    }

    // ─── URL field ────────────────────────────────────────────────────────────

    @Test
    fun urlField_isDisplayed() {
        setAddScreen(AddUiState())
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_url_label))
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
            .onNodeWithText(context.getString(CoreUiR.string.add_name_label))
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
            .onNodeWithContentDescription(context.getString(CoreUiR.string.add_analyze_site_cd))
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

    @Test
    fun urlError_blankUrl_showsResourceString() {
        setAddScreen(AddUiState(urlError = context.getString(CoreUiR.string.add_url_error_blank)))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_url_error_blank))
            .assertIsDisplayed()
    }

    @Test
    fun urlError_httpScheme_showsResourceString() {
        setAddScreen(AddUiState(url = "http://example.com", urlError = context.getString(CoreUiR.string.add_url_error_http)))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_url_error_http))
            .assertIsDisplayed()
    }

    @Test
    fun urlError_invalidUrl_showsResourceString() {
        setAddScreen(AddUiState(url = "not-a-url", urlError = context.getString(CoreUiR.string.error_invalid_url)))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.error_invalid_url))
            .assertIsDisplayed()
    }

    @Test
    fun nameError_blank_showsResourceString() {
        setAddScreen(AddUiState(url = "https://example.com", nameError = context.getString(CoreUiR.string.add_name_error_blank)))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_name_error_blank))
            .assertIsDisplayed()
    }

    // ─── Settings section ─────────────────────────────────────────────────────

    @Test
    fun settingsSection_showsAdBlockLabel() {
        setAddScreen(AddUiState(name = "App", url = "https://example.com"))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_feature_adblock))
            .assertIsDisplayed()
    }

    @Test
    fun settingsSection_showsFullscreenLabel() {
        setAddScreen(AddUiState(name = "App", url = "https://example.com"))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_feature_fullscreen))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun settingsSection_showsAutoTranslateLabel() {
        setAddScreen(AddUiState(name = "App", url = "https://example.com"))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_feature_translate))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
