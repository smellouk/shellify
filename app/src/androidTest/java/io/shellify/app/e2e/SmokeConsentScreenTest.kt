package io.shellify.app.e2e

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shellify.app.presentation.onboarding.ConsentScreen
import io.shellify.app.presentation.theme.ShellifyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeConsentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launch(onAccepted: () -> Unit = {}) {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    ConsentScreen(onAccepted = onAccepted)
                }
            }
        }
    }

    @Test
    fun consentScreen_renders_withoutCrash() {
        launch()
        composeTestRule.onNodeWithText("Before you start").assertIsDisplayed()
    }

    @Test
    fun consentScreen_agreeButton_disabledByDefault() {
        launch()
        composeTestRule.onNodeWithTag("consent_agree_button").assertIsNotEnabled()
    }

    @Test
    fun consentScreen_declineButton_isVisible() {
        launch()
        composeTestRule.onNodeWithTag("consent_decline_button").assertIsDisplayed()
    }

    @Test
    fun consentScreen_checkingCheckbox_enablesAgreeButton() {
        launch()
        composeTestRule.onNodeWithTag("consent_checkbox").performClick()
        composeTestRule.onNodeWithTag("consent_agree_button").assertIsEnabled()
    }

    @Test
    fun consentScreen_uncheckingCheckbox_disablesAgreeButton() {
        launch()
        composeTestRule.onNodeWithTag("consent_checkbox").performClick()
        composeTestRule.onNodeWithTag("consent_agree_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("consent_checkbox").performClick()
        composeTestRule.onNodeWithTag("consent_agree_button").assertIsNotEnabled()
    }

    @Test
    fun consentScreen_agreeButton_invokesCallback() {
        var accepted = false
        launch(onAccepted = { accepted = true })
        composeTestRule.onNodeWithTag("consent_checkbox").performClick()
        composeTestRule.onNodeWithTag("consent_agree_button").performClick()
        assertTrue(accepted)
    }

    @Test
    fun consentScreen_allSections_arePresent() {
        launch()
        listOf(
            "What Shellify does",
            "What Shellify does not do",
            "Acceptable use",
            "Your responsibility",
            "Limitation of liability",
            "Privacy",
        ).forEach { section ->
            composeTestRule.onNodeWithText(section).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun consentScreen_privacyPolicyLink_isVisible() {
        launch()
        composeTestRule
            .onNodeWithText("Read our full Privacy Policy")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun consentScreen_tosLink_isVisible() {
        launch()
        composeTestRule
            .onNodeWithText("Read our Terms of Service")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
