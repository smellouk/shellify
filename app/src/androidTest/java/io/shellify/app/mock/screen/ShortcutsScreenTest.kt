package io.shellify.app.mock.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shellify.app.presentation.shortcuts.ShortcutItem
import io.shellify.app.presentation.shortcuts.ShortcutsScreen
import io.shellify.app.presentation.shortcuts.ShortcutsUiState
import io.shellify.app.presentation.shortcuts.ShortcutsViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.util.FakeData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ShortcutsScreen.
 *
 * ShortcutsViewModel is mocked so we can inject specific [ShortcutsUiState]
 * values without needing a device's launcher to have real pinned shortcuts.
 */
@RunWith(AndroidJUnit4::class)
class ShortcutsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(uiState: ShortcutsUiState): ShortcutsViewModel {
        val vm = mockk<ShortcutsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(uiState)
        return vm
    }

    private fun setShortcutsScreen(uiState: ShortcutsUiState) {
        composeTestRule.setContent {
            ShellifyTheme {
                ShortcutsScreen(viewModel = buildViewModel(uiState))
            }
        }
    }

    // ─── Top bar ──────────────────────────────────────────────────────────────

    @Test
    fun topBar_showsShortcutsTitle() {
        setShortcutsScreen(ShortcutsUiState(isLoading = false))
        composeTestRule
            .onNodeWithText("Shortcuts")
            .assertIsDisplayed()
    }

    // ─── Empty state ──────────────────────────────────────────────────────────

    @Test
    fun emptyState_showsNoShortcutsYetTitle() {
        setShortcutsScreen(ShortcutsUiState(items = emptyList(), isLoading = false))
        composeTestRule
            .onNodeWithText("No shortcuts yet")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_showsEmptyStateDescription() {
        setShortcutsScreen(ShortcutsUiState(items = emptyList(), isLoading = false))
        composeTestRule
            .onNodeWithText("Shortcuts you add from an app's settings appear here", substring = true)
            .assertIsDisplayed()
    }

    // ─── List with items ──────────────────────────────────────────────────────

    @Test
    fun shortcutList_displaysItemLabel() {
        val app = FakeData.webApp(id = 1L, name = "GitHub", url = "https://github.com")
        val item = ShortcutItem(app = app, shortcutId = "pwa_test", label = "GitHub")
        setShortcutsScreen(
            ShortcutsUiState(items = listOf(item), isLoading = false)
        )
        composeTestRule
            .onNodeWithText("GitHub")
            .assertIsDisplayed()
    }

    @Test
    fun shortcutList_displaysMultipleItemLabels() {
        val items = listOf(
            ShortcutItem(
                app = FakeData.webApp(id = 1L, name = "Gmail"),
                shortcutId = "pwa_1",
                label = "Gmail",
            ),
            ShortcutItem(
                app = FakeData.webApp(id = 2L, name = "Notion"),
                shortcutId = "pwa_2",
                label = "Notion",
            ),
        )
        setShortcutsScreen(ShortcutsUiState(items = items, isLoading = false))
        composeTestRule.onNodeWithText("Gmail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notion").assertIsDisplayed()
    }

    // ─── Rename dialog ────────────────────────────────────────────────────────

    @Test
    fun renameDialog_isShownWhenRenameTargetIsSet() {
        val app = FakeData.webApp(id = 1L, name = "Slack")
        val item = ShortcutItem(app = app, shortcutId = "pwa_slack", label = "Slack")
        setShortcutsScreen(
            ShortcutsUiState(
                items = listOf(item),
                isLoading = false,
                renameTarget = item,
                renameText = "Slack",
            )
        )
        composeTestRule
            .onNodeWithText("Rename shortcut")
            .assertIsDisplayed()
    }

    // ─── Remove dialog ────────────────────────────────────────────────────────

    @Test
    fun removeDialog_isShownWhenRemoveTargetIsSet() {
        val app = FakeData.webApp(id = 1L, name = "Trello")
        val item = ShortcutItem(app = app, shortcutId = "pwa_trello", label = "Trello")
        setShortcutsScreen(
            ShortcutsUiState(
                items = listOf(item),
                isLoading = false,
                removeTarget = item,
            )
        )
        composeTestRule
            .onNodeWithText("Remove shortcut?")
            .assertIsDisplayed()
    }

    // ─── Change icon sheet ────────────────────────────────────────────────────

    @Test
    fun changeIconSheet_isDisplayedWhenIconSheetTargetIsSet() {
        val app = FakeData.webApp(id = 1L, name = "GitHub", url = "https://github.com")
        val item = ShortcutItem(app = app, shortcutId = "pwa_github", label = "GitHub")
        setShortcutsScreen(
            ShortcutsUiState(
                items = listOf(item),
                isLoading = false,
                iconSheetTarget = item,
                isIconPackAvailable = true,
            )
        )
        composeTestRule
            .onNodeWithText("Change icon")
            .assertIsDisplayed()
    }

    @Test
    fun changeIconSheet_fromIconPackButton_isDisplayed() {
        val app = FakeData.webApp(id = 1L, name = "GitHub", url = "https://github.com")
        val item = ShortcutItem(app = app, shortcutId = "pwa_github", label = "GitHub")
        setShortcutsScreen(
            ShortcutsUiState(
                items = listOf(item),
                isLoading = false,
                iconSheetTarget = item,
                isIconPackAvailable = true,
            )
        )
        composeTestRule
            .onNodeWithText("From icon pack")
            .assertIsDisplayed()
    }

    @Test
    fun changeIconSheet_fromIconPackButton_isNotClickableWhenPackNotAvailable() {
        val app = FakeData.webApp(id = 1L, name = "GitHub", url = "https://github.com")
        val item = ShortcutItem(app = app, shortcutId = "pwa_github", label = "GitHub")
        setShortcutsScreen(
            ShortcutsUiState(
                items = listOf(item),
                isLoading = false,
                iconSheetTarget = item,
                isIconPackAvailable = false,
            )
        )
        composeTestRule
            .onNodeWithText("From icon pack")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("From icon pack")
            .assertIsNotEnabled()
    }

    @Test
    fun changeIconSheet_fromIconPackButton_isClickableWhenPackAvailable() {
        val app = FakeData.webApp(id = 1L, name = "GitHub", url = "https://github.com")
        val item = ShortcutItem(app = app, shortcutId = "pwa_github", label = "GitHub")
        setShortcutsScreen(
            ShortcutsUiState(
                items = listOf(item),
                isLoading = false,
                iconSheetTarget = item,
                isIconPackAvailable = true,
            )
        )
        composeTestRule
            .onNodeWithText("From icon pack")
            .assertIsEnabled()
    }
}
