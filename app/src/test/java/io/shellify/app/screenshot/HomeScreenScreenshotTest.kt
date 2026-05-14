package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.domain.model.Category
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.home.HomeScreen
import io.shellify.app.presentation.home.HomeUiState
import io.shellify.app.presentation.home.HomeViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
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
class HomeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(state: HomeUiState): HomeViewModel =
        mockk<HomeViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
        }

    private fun app(id: Long, name: String, url: String, categoryId: Long? = null) =
        WebApp(
            id = id,
            name = name,
            url = url,
            categoryId = categoryId,
            isolationId = UUID.randomUUID().toString(),
            isFullscreen = false,
            adBlockEnabled = true,
            translateEnabled = false,
        )

    @Test
    fun emptyState() {
        composeTestRule.setContent {
            ShellifyTheme {
                HomeScreen(
                    viewModel = buildVm(HomeUiState(apps = emptyList(), isLoading = false)),
                    geckoInstalled = true,
                    currentLanguage = "en",
                    onLanguageChange = {},
                    onAddApp = {},
                    onEditApp = {},
                    onOpenApp = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun withApps() {
        val apps = listOf(
            app(1L, "GitHub", "https://github.com"),
            app(2L, "Notion", "https://notion.so"),
            app(3L, "Slack", "https://slack.com"),
            app(4L, "Figma", "https://figma.com"),
            app(5L, "Linear", "https://linear.app"),
            app(6L, "Jira", "https://atlassian.net"),
        )
        composeTestRule.setContent {
            ShellifyTheme {
                HomeScreen(
                    viewModel = buildVm(HomeUiState(apps = apps, hasAnyApps = true, isLoading = false)),
                    geckoInstalled = true,
                    currentLanguage = "en",
                    onLanguageChange = {},
                    onAddApp = {},
                    onEditApp = {},
                    onOpenApp = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun withCategoryFilters() {
        val categories = listOf(
            Category(id = 1L, name = "Work", sortIndex = 0, icon = "work", color = "#4285F4"),
            Category(id = 2L, name = "Media", sortIndex = 1, icon = "movie", color = "#EA4335"),
        )
        val apps = listOf(
            app(1L, "Gmail", "https://mail.google.com", categoryId = 1L),
            app(2L, "Meet", "https://meet.google.com", categoryId = 1L),
            app(3L, "YouTube", "https://youtube.com", categoryId = 2L),
        )
        composeTestRule.setContent {
            ShellifyTheme {
                HomeScreen(
                    viewModel = buildVm(
                        HomeUiState(
                            apps = apps,
                            hasAnyApps = true,
                            categories = categories,
                            isLoading = false,
                        )
                    ),
                    geckoInstalled = true,
                    currentLanguage = "en",
                    onLanguageChange = {},
                    onAddApp = {},
                    onEditApp = {},
                    onOpenApp = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun filteredEmptyCategory() {
        val categories = listOf(
            Category(id = 1L, name = "Media", sortIndex = 0, icon = "movie", color = "#EA4335"),
        )
        composeTestRule.setContent {
            ShellifyTheme {
                HomeScreen(
                    viewModel = buildVm(
                        HomeUiState(
                            apps = emptyList(),
                            hasAnyApps = true,
                            categories = categories,
                            selectedCategoryId = 1L,
                            isLoading = false,
                        )
                    ),
                    geckoInstalled = true,
                    currentLanguage = "en",
                    onLanguageChange = {},
                    onAddApp = {},
                    onEditApp = {},
                    onOpenApp = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
