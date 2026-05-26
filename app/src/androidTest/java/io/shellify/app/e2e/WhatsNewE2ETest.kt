package io.shellify.app.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.presentation.onboarding.WhatsNewScreen
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.core.ui.R as CoreUiR
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-content instrumented tests for [WhatsNewScreen].
 *
 * Covered scenarios:
 *  - Title is displayed
 *  - All 5 feature rows are reachable and displayed
 *  - "Got it" button calls onDismissed
 */
@RunWith(AndroidJUnit4::class)
class WhatsNewE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setScreen(onDismissed: () -> Unit = {}) {
        composeRule.setContent {
            ShellifyTheme {
                WhatsNewScreen(onDismissed = onDismissed)
            }
        }
    }

    @Test
    fun title_isDisplayed() {
        setScreen()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.whats_new_title))
            .assertIsDisplayed()
    }

    @Test
    fun subtitle_isDisplayed() {
        setScreen()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.whats_new_subtitle))
            .assertIsDisplayed()
    }

    @Test
    fun privacyRow_isDisplayed() {
        setScreen()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.whats_new_privacy_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun torRow_isDisplayed() {
        setScreen()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.whats_new_tor_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun panicRow_isDisplayed() {
        setScreen()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.whats_new_panic_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun httpsRow_isDisplayed() {
        setScreen()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.whats_new_https_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun refreshRow_isDisplayed() {
        setScreen()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.whats_new_refresh_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun gotItButton_callsOnDismissed() {
        var dismissed = false
        setScreen(onDismissed = { dismissed = true })
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.whats_new_got_it))
            .performClick()
        assertTrue(dismissed)
    }
}
