package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.domain.model.Category
import io.shellify.app.presentation.category.CategoryScreen
import io.shellify.app.presentation.category.CategoryUiState
import io.shellify.app.presentation.category.CategoryViewModel
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
class CategoryScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(categories: List<Category>?, uiState: CategoryUiState = CategoryUiState()): CategoryViewModel =
        mockk<CategoryViewModel>(relaxed = true).also {
            every { it.categories } returns MutableStateFlow(categories)
            every { it.uiState } returns MutableStateFlow(uiState)
        }

    @Test
    fun emptyState() {
        composeTestRule.setContent {
            ShellifyTheme { CategoryScreen(viewModel = buildVm(emptyList())) }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun withCategories() {
        composeTestRule.setContent {
            ShellifyTheme {
                CategoryScreen(
                    viewModel = buildVm(
                        listOf(
                            Category(id = 1L, name = "Work", sortIndex = 0, icon = "work", color = "#4285F4"),
                            Category(id = 2L, name = "Media", sortIndex = 1, icon = "movie", color = "#EA4335"),
                            Category(id = 3L, name = "Social", sortIndex = 2, icon = "people", color = "#34A853"),
                        )
                    )
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun addCategoryDialog() {
        composeTestRule.setContent {
            ShellifyTheme { CategoryScreen(viewModel = buildVm(emptyList(), CategoryUiState(showAddDialog = true))) }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun editCategoryDialog() {
        val cat = Category(id = 3L, name = "Reading", sortIndex = 0, icon = "book", color = "#6D28D9")
        composeTestRule.setContent {
            ShellifyTheme {
                CategoryScreen(
                    viewModel = buildVm(listOf(cat), CategoryUiState(showAddDialog = true, editingId = 3L, newName = "Reading"))
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
