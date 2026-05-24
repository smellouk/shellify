package io.shellify.app.mock.screen

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
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.presentation.onboarding.ConsentScreen
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.core.ui.R as CoreUiR
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeConsentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

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
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_title)).assertIsDisplayed()
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
            context.getString(CoreUiR.string.consent_section_what_title),
            context.getString(CoreUiR.string.consent_section_not_title),
            context.getString(CoreUiR.string.consent_section_acceptable_title),
            context.getString(CoreUiR.string.consent_section_responsibility_title),
            context.getString(CoreUiR.string.consent_section_legal_title),
            context.getString(CoreUiR.string.consent_section_privacy_title),
        ).forEach { section ->
            composeTestRule.onNodeWithText(section).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun consentScreen_brandIconDisclosure_isPresent() {
        launch()
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.consent_what_5), substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun consentScreen_privacyPolicyLink_isVisible() {
        launch()
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.consent_read_privacy))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun consentScreen_tosLink_isVisible() {
        launch()
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.consent_read_tos))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
