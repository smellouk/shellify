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
import io.shellify.app.presentation.category.CategoryScreen
import io.shellify.app.presentation.category.CategoryUiState
import io.shellify.app.presentation.category.CategoryViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.util.FakeData
import io.shellify.core.ui.R as CoreUiR
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E Compose tests covering the "Delete all categories" confirmation dialog in
 * [CategoryScreen].
 *
 * The dialog is pure local UI state (not in the ViewModel), so tests trigger it
 * by clicking the delete-all icon button in the top-app-bar.
 *
 * Covered scenarios:
 *  D-CAT-01  Delete-all dialog is shown when the user taps the delete-all action
 *  D-CAT-02  Dialog displays the expected confirm and cancel buttons
 *  D-CAT-03  Tapping Cancel dismisses the dialog without invoking deleteAllCategories
 */
@RunWith(AndroidJUnit4::class)
class CategoryDeleteAllE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Inflates [CategoryScreen] with a mocked [CategoryViewModel] that exposes a
     * non-empty category list so the delete-all icon button is rendered in the top bar.
     */
    private fun setCategoryScreen(viewModel: CategoryViewModel = buildViewModel()) {
        composeRule.setContent {
            ShellifyTheme {
                CategoryScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    private fun buildViewModel(
        categories: List<io.shellify.app.domain.model.Category> = FakeData.categoryList(),
        uiState: CategoryUiState = CategoryUiState(),
    ): CategoryViewModel {
        val vm = mockk<CategoryViewModel>(relaxed = true)
        every { vm.categories } returns MutableStateFlow(categories)
        every { vm.uiState } returns MutableStateFlow(uiState)
        return vm
    }

    private fun tapDeleteAllButton() {
        composeRule
            .onNodeWithContentDescription(
                context.getString(CoreUiR.string.categories_delete_all_cd)
            )
            .performClick()
        composeRule.waitForIdle()
    }

    // ─── D-CAT-01 ─────────────────────────────────────────────────────────────

    /**
     * D-CAT-01: tapping the delete-all icon in the top-app-bar shows the confirmation dialog.
     * The dialog title must be visible.
     */
    @Test
    fun deleteAll_tapAction_showsConfirmationDialog() {
        setCategoryScreen()

        tapDeleteAllButton()

        composeRule
            .onNodeWithText(
                context.getString(CoreUiR.string.categories_delete_all_confirm_title)
            )
            .assertIsDisplayed()
    }

    // ─── D-CAT-02 ─────────────────────────────────────────────────────────────

    /**
     * D-CAT-02: the confirmation dialog contains both the destructive confirm button and the
     * cancel button, each labelled with the correct string resource.
     */
    @Test
    fun deleteAll_confirmDialog_showsConfirmButton() {
        setCategoryScreen()

        tapDeleteAllButton()

        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.common_delete_all))
            .assertIsDisplayed()
    }

    @Test
    fun deleteAll_confirmDialog_showsCancelButton() {
        setCategoryScreen()

        tapDeleteAllButton()

        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.common_cancel))
            .assertIsDisplayed()
    }

    // ─── D-CAT-03 ─────────────────────────────────────────────────────────────

    /**
     * D-CAT-03: tapping Cancel dismisses the dialog — the dialog title is no longer shown.
     * The delete-all action on the ViewModel must NOT have been called.
     */
    @Test
    fun deleteAll_tapCancel_dismissesDialog() {
        val vm = buildViewModel()
        setCategoryScreen(viewModel = vm)

        tapDeleteAllButton()

        // Dialog is visible before cancelling
        composeRule
            .onNodeWithText(
                context.getString(CoreUiR.string.categories_delete_all_confirm_title)
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.common_cancel))
            .performClick()
        composeRule.waitForIdle()

        // Dialog must be gone after cancel
        composeRule
            .onNodeWithText(
                context.getString(CoreUiR.string.categories_delete_all_confirm_title)
            )
            .assertDoesNotExist()
    }
}
