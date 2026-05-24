package io.shellify.app.mock.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.R
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.core.ui.R as CoreUiR
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.presentation.settings.AppSettingsScreen
import io.shellify.app.presentation.settings.AppSettingsUiState
import io.shellify.app.presentation.settings.AppSettingsViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.util.FakeData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun buildViewModel(uiState: AppSettingsUiState): AppSettingsViewModel {
        val geckoManager = mockk<GeckoEngineManager>(relaxed = true)
        every { geckoManager.installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)

        val vm = mockk<AppSettingsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(uiState)
        every { vm.geckoEngineManager } returns geckoManager
        every { vm.commands } returns MutableSharedFlow()
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
            .onNodeWithText(context.getString(CoreUiR.string.settings_title))
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
            .onNodeWithText(context.getString(CoreUiR.string.settings_app_info))
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsUrlLabel() {
        val app = FakeData.webApp(id = 1L, name = "Slack", url = "https://slack.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.add_url_label))
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsControlCenterSection() {
        val app = FakeData.webApp(id = 1L, name = "Figma", url = "https://figma.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.settings_control_center_title))
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsAdBlockToggle() {
        val app = FakeData.webApp(id = 1L, name = "Reddit", url = "https://reddit.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.settings_adblock))
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsFullscreenToggle() {
        val app = FakeData.webApp(id = 1L, name = "YouTube", url = "https://youtube.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.settings_fullscreen))
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsDangerZoneSection() {
        val app = FakeData.webApp(id = 1L, name = "Twitter", url = "https://x.com")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.settings_danger_zone))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun populatedState_showsDeleteAppOption() {
        val app = FakeData.webApp(id = 1L, name = "Mastodon", url = "https://mastodon.social")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.settings_delete_app), substring = true)
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
            .onNodeWithText(context.getString(CoreUiR.string.settings_delete_confirm, "Asana"))
            .assertIsDisplayed()
    }

    @Test
    fun swipeToRefreshToggle_isDisplayed() {
        val app = FakeData.webApp(id = 1L, name = "Notion", url = "https://notion.so")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.settings_swipe_to_refresh))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ─── Network log row ──────────────────────────────────────────────────────

    @Test
    fun networkLogRow_isDisplayed() {
        val app = FakeData.webApp(id = 1L, name = "Notion", url = "https://notion.so")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.settings_network_log))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun networkLogRow_downloadIconIsDisplayed() {
        val app = FakeData.webApp(id = 1L, name = "Notion", url = "https://notion.so")
        setAppSettingsScreen(AppSettingsUiState(app = app, isLoading = false))
        composeTestRule
            .onNodeWithContentDescription(context.getString(CoreUiR.string.network_log_export_cd))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun networkLogRow_downloadIconClick_invokesExportOnViewModel() {
        val app = FakeData.webApp(id = 1L, name = "Notion", url = "https://notion.so")
        // hasNetworkLogs = true is required — the download button is disabled otherwise.
        val vm = buildViewModel(AppSettingsUiState(app = app, isLoading = false, hasNetworkLogs = true))
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(viewModel = vm, onBack = {}, onDeleted = {})
            }
        }
        composeTestRule
            .onNodeWithContentDescription(context.getString(CoreUiR.string.network_log_export_cd))
            .performScrollTo()
            .performClick()
        verify { vm.onExportNetworkLogsClick() }
    }
}
