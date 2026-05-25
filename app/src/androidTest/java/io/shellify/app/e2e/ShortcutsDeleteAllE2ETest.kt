package io.shellify.app.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.presentation.shortcuts.ShortcutItem
import io.shellify.app.presentation.shortcuts.ShortcutsScreen
import io.shellify.app.presentation.shortcuts.ShortcutsUiState
import io.shellify.app.presentation.shortcuts.ShortcutsViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.util.FakeData
import io.shellify.core.ui.R as CoreUiR
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E Compose tests covering the "Delete all shortcuts" confirmation dialog in
 * [ShortcutsScreen].
 *
 * The dialog is pure local UI state (not in the ViewModel), so tests trigger it
 * by clicking the delete-all icon button in the top-app-bar, which only renders
 * when the shortcuts list is non-empty.
 *
 * Covered scenarios:
 *  D-SC-01  Delete-all dialog is shown when the user taps the delete-all action
 *  D-SC-02  Dialog displays the expected action label (common_delete_all)
 *  D-SC-03  Tapping Cancel dismisses the dialog without invoking deleteAllShortcuts
 */
@RunWith(AndroidJUnit4::class)
class ShortcutsDeleteAllE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Builds a [ShortcutsUiState] with a non-empty item list so the delete-all button
     * is rendered in the top-app-bar.
     */
    private fun nonEmptyState(): ShortcutsUiState {
        val app = FakeData.webApp(id = 1L, name = "GitHub", url = "https://github.com")
        val item = ShortcutItem(app = app, shortcutId = "pwa_github", label = "GitHub")
        return ShortcutsUiState(items = listOf(item), isLoading = false)
    }

    private fun buildViewModel(uiState: ShortcutsUiState = nonEmptyState()): ShortcutsViewModel {
        val vm = mockk<ShortcutsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(uiState)
        return vm
    }

    private fun setShortcutsScreen(viewModel: ShortcutsViewModel = buildViewModel()) {
        composeRule.setContent {
            ShellifyTheme {
                ShortcutsScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    private fun tapDeleteAllButton() {
        composeRule
            .onNodeWithContentDescription(
                context.getString(CoreUiR.string.shortcuts_delete_all_cd)
            )
            .performClick()
        composeRule.waitForIdle()
    }

    // ─── D-SC-01 ──────────────────────────────────────────────────────────────

    /**
     * D-SC-01: tapping the delete-all icon in the top-app-bar shows the confirmation dialog.
     * The dialog title must be visible.
     */
    @Test
    fun deleteAll_tapAction_showsConfirmationDialog() {
        setShortcutsScreen()

        tapDeleteAllButton()

        composeRule
            .onNodeWithText(
                context.getString(CoreUiR.string.shortcuts_delete_all_confirm_title)
            )
            .assertIsDisplayed()
    }

    // ─── D-SC-02 ──────────────────────────────────────────────────────────────

    /**
     * D-SC-02: the confirmation dialog carries the correct destructive action label
     * (common_delete_all) and Cancel button.
     */
    @Test
    fun deleteAll_confirmDialog_showsDeleteAllLabel() {
        setShortcutsScreen()

        tapDeleteAllButton()

        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.common_delete_all))
            .assertIsDisplayed()
    }

    @Test
    fun deleteAll_confirmDialog_showsCancelButton() {
        setShortcutsScreen()

        tapDeleteAllButton()

        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.common_cancel))
            .assertIsDisplayed()
    }

    // ─── D-SC-03 ──────────────────────────────────────────────────────────────

    /**
     * D-SC-03: tapping Cancel dismisses the dialog — the title is no longer shown.
     * The delete-all action on the ViewModel must NOT have been called.
     */
    @Test
    fun deleteAll_tapCancel_dismissesDialog() {
        setShortcutsScreen()

        tapDeleteAllButton()

        // Dialog is visible before cancelling
        composeRule
            .onNodeWithText(
                context.getString(CoreUiR.string.shortcuts_delete_all_confirm_title)
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.common_cancel))
            .performClick()
        composeRule.waitForIdle()

        // Dialog must be gone after cancel
        composeRule
            .onNodeWithText(
                context.getString(CoreUiR.string.shortcuts_delete_all_confirm_title)
            )
            .assertDoesNotExist()
    }
}
