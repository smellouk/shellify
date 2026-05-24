package io.shellify.app.mock.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.linkdispatcher.DispatchSheet
import io.shellify.app.presentation.linkdispatcher.LinkDispatcherSheet
import io.shellify.app.presentation.linkdispatcher.LinkDispatcherUiState
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.util.FakeData
import io.shellify.core.ui.R as CoreUiR
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for the link-dispatcher share flow (Phase 1).
 *
 * Covers the three dispatch outcomes (single-match, multi-match, no-match) as
 * reflected in the sheet UI, and the end-to-end app selection journey.
 */
@RunWith(AndroidJUnit4::class)
class SmokeLinkDispatcherTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setSheet(
        uiState: LinkDispatcherUiState,
        onAppSelected: (WebApp, String) -> Unit = { _, _ -> },
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ShellifyTheme {
                LinkDispatcherSheet(
                    uiState = uiState,
                    onAppSelected = onAppSelected,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    // ── Scenario: no match → sheet stays hidden (dispatcher fires AddAsNew directly) ───

    @Test
    fun noMatch_sheetIsNotShown() {
        setSheet(LinkDispatcherUiState(sheet = DispatchSheet.None))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.link_dispatcher_chooser_title))
            .assertDoesNotExist()
    }

    // ── Scenario: single match → sheet stays hidden (dispatcher fires LaunchApp directly) ─

    @Test
    fun singleMatch_sheetIsNotShown() {
        // ViewModel never sets Chooser for a single match; sheet stays None.
        setSheet(LinkDispatcherUiState(sheet = DispatchSheet.None))
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.link_dispatcher_chooser_title))
            .assertDoesNotExist()
    }

    // ── Scenario: multiple matches → chooser shown → user picks one ──────────

    @Test
    fun multipleMatches_chooserShowsAllApps() {
        val apps = listOf(
            FakeData.webApp(name = "Notion", url = "https://notion.so"),
            FakeData.webApp(name = "Notion Personal", url = "https://notion.so/personal"),
        )
        setSheet(LinkDispatcherUiState(sheet = DispatchSheet.Chooser(apps, "https://notion.so/page")))

        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.link_dispatcher_chooser_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Notion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notion Personal").assertIsDisplayed()
    }

    @Test
    fun multipleMatches_selectingApp_firesCallbackWithDispatchedUrl() {
        val app1 = FakeData.webApp(name = "Notion", url = "https://notion.so")
        val app2 = FakeData.webApp(name = "Notion Personal", url = "https://notion.so/personal")
        val dispatchUrl = "https://notion.so/some/page"
        var selectedApp: WebApp? = null
        var selectedUrl: String? = null

        setSheet(
            uiState = LinkDispatcherUiState(sheet = DispatchSheet.Chooser(listOf(app1, app2), dispatchUrl)),
            onAppSelected = { a, u -> selectedApp = a; selectedUrl = u },
        )

        composeTestRule.onNodeWithText("Notion Personal").performClick()

        assertEquals(app2, selectedApp)
        assertEquals(dispatchUrl, selectedUrl)
    }

    @Test
    fun multipleMatches_selectingFirstApp_firesCallbackWithCorrectApp() {
        val app1 = FakeData.webApp(name = "GitHub", url = "https://github.com")
        val app2 = FakeData.webApp(name = "GitHub Mirror", url = "https://github.com/mirror")
        var selectedApp: WebApp? = null

        setSheet(
            uiState = LinkDispatcherUiState(
                sheet = DispatchSheet.Chooser(listOf(app1, app2), "https://github.com/repo"),
            ),
            onAppSelected = { a, _ -> selectedApp = a },
        )

        composeTestRule.onNodeWithText("GitHub").performClick()

        assertEquals(app1, selectedApp)
    }

    @Test
    fun chooser_doesNotFireOnDismiss_whenAppSelected() {
        val app = FakeData.webApp(name = "GitHub", url = "https://github.com")
        var dismissed = false

        setSheet(
            uiState = LinkDispatcherUiState(
                sheet = DispatchSheet.Chooser(listOf(app), "https://github.com"),
            ),
            onAppSelected = { _, _ -> },
            onDismiss = { dismissed = true },
        )

        composeTestRule.onNodeWithText("GitHub").performClick()

        assertEquals(false, dismissed)
    }
}
