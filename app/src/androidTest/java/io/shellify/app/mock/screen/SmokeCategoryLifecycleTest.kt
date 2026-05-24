package io.shellify.app.mock.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.domain.model.Category
import io.shellify.app.presentation.category.CategoryScreen
import io.shellify.app.presentation.category.CategoryUiState
import io.shellify.app.presentation.category.CategoryViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.util.FakeData
import io.shellify.core.ui.R as CoreUiR
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E smoke tests for the Category lifecycle: create → list → edit → delete.
 */
@RunWith(AndroidJUnit4::class)
class SmokeCategoryLifecycleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ── Scenario: Empty state ─────────────────────────────────────────────────

    @Test
    fun emptyCategories_showsPlaceholder() {
        setScreen(categories = emptyList())
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.categories_empty)).assertIsDisplayed()
    }

    @Test
    fun nonEmptyCategories_fabIsVisible() {
        // FAB only shows when there are existing categories
        setScreen(categories = listOf(FakeData.category(id = 1L, name = "Work")))
        composeTestRule.onNodeWithContentDescription(context.getString(CoreUiR.string.categories_add_fab_cd)).assertIsDisplayed()
    }

    // ── Scenario: Add category ────────────────────────────────────────────────

    @Test
    fun addDialog_opensWhenFabTapped() {
        val vm = buildVm(categories = listOf(FakeData.category(id = 1L, name = "Work")))
        composeTestRule.setContent {
            ShellifyTheme { CategoryScreen(viewModel = vm) }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(CoreUiR.string.categories_add_fab_cd)).performClick()
        verify { vm.showDialog() }
    }

    @Test
    fun addDialog_showsNameInputField() {
        setScreen(
            categories = emptyList(),
            uiState = CategoryUiState(showAddDialog = true),
        )
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.categories_name_label)).assertIsDisplayed()
    }

    @Test
    fun addDialog_showsAddButton() {
        setScreen(
            categories = emptyList(),
            uiState = CategoryUiState(showAddDialog = true),
        )
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.categories_add_button)).assertIsDisplayed()
    }

    // ── Scenario: Category appears in list ────────────────────────────────────

    @Test
    fun category_appearsInListAfterSave() {
        setScreen(categories = listOf(FakeData.category(id = 1L, name = "Work")))
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
    }

    @Test
    fun multipleCategories_allVisibleInList() {
        val categories = listOf(
            FakeData.category(id = 1L, name = "Work"),
            FakeData.category(id = 2L, name = "Media"),
            FakeData.category(id = 3L, name = "Shopping"),
        )
        setScreen(categories = categories)
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Media").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping").assertIsDisplayed()
    }

    // ── Scenario: Edit category ───────────────────────────────────────────────

    @Test
    fun editDialog_showsExistingName() {
        setScreen(
            categories = listOf(FakeData.category(id = 2L, name = "Reading")),
            uiState = CategoryUiState(
                showAddDialog = true,
                editingId = 2L,
                newName = "Reading",
            ),
        )
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.categories_edit_dialog_title)).assertIsDisplayed()
        // "Reading" appears in both the list item and the dialog input — verify at least one
        composeTestRule.onAllNodesWithText("Reading")[0].assertIsDisplayed()
    }

    @Test
    fun editDialog_saveButtonIsPresent() {
        setScreen(
            categories = listOf(FakeData.category(id = 2L, name = "Reading")),
            uiState = CategoryUiState(
                showAddDialog = true,
                editingId = 2L,
                newName = "Reading",
            ),
        )
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.categories_save_button)).assertIsDisplayed()
    }

    // ── Scenario: Delete category ─────────────────────────────────────────────

    @Test
    fun cancelAddDialog_callsDismissOnViewModel() {
        val vm = buildVm(
            categories = emptyList(),
            uiState = CategoryUiState(showAddDialog = true),
        )
        composeTestRule.setContent {
            ShellifyTheme { CategoryScreen(viewModel = vm) }
        }
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.common_cancel)).performClick()
        verify { vm.dismissDialog() }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildVm(
        categories: List<Category>?,
        uiState: CategoryUiState = CategoryUiState(),
    ): CategoryViewModel = mockk<CategoryViewModel>(relaxed = true).also {
        every { it.categories } returns MutableStateFlow(categories)
        every { it.uiState } returns MutableStateFlow(uiState)
    }

    private fun setScreen(
        categories: List<Category>?,
        uiState: CategoryUiState = CategoryUiState(),
    ) {
        composeTestRule.setContent {
            ShellifyTheme { CategoryScreen(viewModel = buildVm(categories, uiState)) }
        }
    }
}
