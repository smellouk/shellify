package io.shellify.app.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.presentation.settings.AppSettingsScreen
import io.shellify.app.presentation.settings.AppSettingsUiState
import io.shellify.app.presentation.settings.AppSettingsViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.util.FakeData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for AppSettingsScreen.
 *
 * AppSettingsViewModel is mocked so we can control the loaded WebApp and
 * visible dialog states without touching real use-cases or DI.
 */
@RunWith(AndroidJUnit4::class)
class AppSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(uiState: AppSettingsUiState): AppSettingsViewModel {
        val geckoManager = mockk<GeckoEngineManager>(relaxed = true)
        every { geckoManager.installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)

        val vm = mockk<AppSettingsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(uiState)
        every { vm.geckoEngineManager } returns geckoManager
        return vm
    }

    private fun setAppSettingsScreen(uiState: AppSettingsUiState) {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildViewModel(uiState),
                    onBack = {},
                    onDeleted = {},
                )
            }
        }
    }

    // ─── Loading state ─────────────────────────────────────────────────────────

    @Test
    fun loadingState_showsSettingsTitleWhenAppIsNull() {
        setAppSettingsScreen(AppSettingsUiState(app = null, isLoading = true))
        composeTestRule
            .onNodeWithText("App settings")
            .assertIsDisplayed()
    }

    // ─── Populated app state ──────────────────────────────────────────────────

    @Test
    fun populatedState_showsAppNameInTopBar() {
        val app = FakeData.webApp(id = 1L, name = "Notion", url = "https://notion.so")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onAllNodesWithText("Notion")[0]
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsAppInfoSection() {
        val app = FakeData.webApp(id = 1L, name = "Jira", url = "https://atlassian.net")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText("App info")
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsUrlLabel() {
        val app = FakeData.webApp(id = 1L, name = "Slack", url = "https://slack.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText("Website URL")
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsFeaturesSection() {
        val app = FakeData.webApp(id = 1L, name = "Figma", url = "https://figma.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText("Features")
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsAdBlockToggle() {
        val app = FakeData.webApp(id = 1L, name = "Reddit", url = "https://reddit.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText("Block ads")
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsFullscreenToggle() {
        val app = FakeData.webApp(id = 1L, name = "YouTube", url = "https://youtube.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText("Full screen")
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsDangerZoneSection() {
        val app = FakeData.webApp(id = 1L, name = "Twitter", url = "https://x.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText("Danger zone")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsDeleteAppOption() {
        val app = FakeData.webApp(id = 1L, name = "Mastodon", url = "https://mastodon.social")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText("Delete app", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ─── Delete confirmation dialog ────────────────────────────────────────────

    @Test
    fun deleteDialog_isShownWhenShowDeleteDialogIsTrue() {
        val app = FakeData.webApp(id = 1L, name = "Asana", url = "https://asana.com")
        setAppSettingsScreen(
            AppSettingsUiState(app = app, isLoading = false, showDeleteDialog = true)
        )
        composeTestRule
            .onNodeWithText("Delete \"Asana\"?")
            .assertIsDisplayed()
    }
}
