package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.shortcuts.ShortcutItem
import io.shellify.app.presentation.shortcuts.ShortcutsScreen
import io.shellify.app.presentation.shortcuts.ShortcutsUiState
import io.shellify.app.presentation.shortcuts.ShortcutsViewModel
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
class ShortcutsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(state: ShortcutsUiState): ShortcutsViewModel =
        mockk<ShortcutsViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
        }

    private fun item(id: Long, name: String, url: String) = ShortcutItem(
        app = WebApp(id = id, name = name, url = url, isolationId = UUID.randomUUID().toString(),
            isFullscreen = false, adBlockEnabled = true, translateEnabled = false),
        shortcutId = "pwa_$id", label = name,
    )

    @Test
    fun emptyState() {
        composeTestRule.setContent {
            ShellifyTheme { ShortcutsScreen(viewModel = buildVm(ShortcutsUiState(items = emptyList(), isLoading = false))) }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun withShortcuts() {
        composeTestRule.setContent {
            ShellifyTheme {
                ShortcutsScreen(
                    viewModel = buildVm(
                        ShortcutsUiState(
                            items = listOf(item(1L, "Gmail", "https://mail.google.com"), item(2L, "Notion", "https://notion.so"), item(3L, "Slack", "https://slack.com")),
                            isLoading = false,
                        )
                    )
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun renameDialog() {
        val target = item(1L, "Slack", "https://slack.com")
        composeTestRule.setContent {
            ShellifyTheme {
                ShortcutsScreen(
                    viewModel = buildVm(ShortcutsUiState(items = listOf(target), isLoading = false, renameTarget = target, renameText = "Slack"))
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun removeDialog() {
        val target = item(1L, "Trello", "https://trello.com")
        composeTestRule.setContent {
            ShellifyTheme {
                ShortcutsScreen(
                    viewModel = buildVm(ShortcutsUiState(items = listOf(target), isLoading = false, removeTarget = target))
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
