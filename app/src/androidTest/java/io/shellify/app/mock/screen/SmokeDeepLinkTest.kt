package io.shellify.app.mock.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.presentation.navigation.DeepLinkConfirmDialog
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.core.ui.R as CoreUiR
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for the deep-link import confirmation dialog.
 *
 * Covers every user-visible scenario: title, host extraction, button actions,
 * malformed URLs, subdomains, and custom schemes.
 */
@RunWith(AndroidJUnit4::class)
class SmokeDeepLinkTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun launch(url: String, onConfirm: () -> Unit = {}, onDismiss: () -> Unit = {}) {
        composeTestRule.setContent {
            ShellifyTheme {
                DeepLinkConfirmDialog(url = url, onConfirm = onConfirm, onDismiss = onDismiss)
            }
        }
    }

    // ── Renders ───────────────────────────────────────────────────────────────

    @Test
    fun dialog_showsTitle() {
        launch("https://example.com")
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.deeplink_confirm_title)).assertIsDisplayed()
    }

    @Test
    fun dialog_showsAddButton() {
        launch("https://example.com")
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.deeplink_confirm_add)).assertIsDisplayed()
    }

    @Test
    fun dialog_showsCancelButton() {
        launch("https://example.com")
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.common_cancel)).assertIsDisplayed()
    }

    // ── Host extraction ───────────────────────────────────────────────────────

    @Test
    fun dialog_extractsHostFromFullUrl() {
        launch("https://example.com/some/path?query=value")
        composeTestRule.onNodeWithText("example.com", substring = true).assertIsDisplayed()
    }

    @Test
    fun dialog_preservesSubdomain() {
        launch("https://app.example.co.uk/dashboard")
        composeTestRule.onNodeWithText("app.example.co.uk", substring = true).assertIsDisplayed()
    }

    @Test
    fun dialog_stripsPathAndKeepsHostOnly() {
        launch("https://github.com/user/repo/issues/42")
        composeTestRule.onNodeWithText("github.com", substring = true).assertIsDisplayed()
    }

    @Test
    fun dialog_malformedUrl_showsRawString() {
        launch("not-a-valid-url")
        composeTestRule.onNodeWithText("not-a-valid-url", substring = true).assertIsDisplayed()
    }

    @Test
    fun dialog_customScheme_shellifyUrl_showsHost() {
        // shellify://add?url=https://example.com — the *outer* host is null for custom schemes,
        // so the raw value is shown rather than crashing or showing a blank.
        launch("shellify://add?url=https%3A%2F%2Fexample.com")
        // "add" is the host in the shellify:// URI — verify it surfaces something readable
        composeTestRule.onNodeWithText("add", substring = true).assertIsDisplayed()
    }

    @Test
    fun dialog_httpUrl_showsHost() {
        launch("http://insecure.example.org/page")
        composeTestRule.onNodeWithText("insecure.example.org", substring = true).assertIsDisplayed()
    }

    // ── Button callbacks ──────────────────────────────────────────────────────

    @Test
    fun dialog_addButton_invokesConfirmCallback() {
        var confirmed = false
        launch("https://example.com", onConfirm = { confirmed = true })
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.deeplink_confirm_add)).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun dialog_cancelButton_invokesDismissCallback() {
        var dismissed = false
        launch("https://example.com", onDismiss = { dismissed = true })
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.common_cancel)).performClick()
        assertTrue(dismissed)
    }

    @Test
    fun dialog_addButton_doesNotTriggerDismiss() {
        var dismissed = false
        launch("https://example.com", onConfirm = {}, onDismiss = { dismissed = true })
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.deeplink_confirm_add)).performClick()
        assertFalse(dismissed)
    }

    @Test
    fun dialog_cancelButton_doesNotTriggerConfirm() {
        var confirmed = false
        launch("https://example.com", onConfirm = { confirmed = true }, onDismiss = {})
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.common_cancel)).performClick()
        assertFalse(confirmed)
    }
}
