package io.shellify.app.mock.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.core.theme.ThemeManager
import io.shellify.core.ui.R as CoreUiR
import io.shellify.app.presentation.navigation.Screen
import io.shellify.app.presentation.navigation.resolveStartDestination
import io.shellify.app.presentation.onboarding.UpdateConsentScreen
import io.shellify.app.presentation.theme.ShellifyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeUpdateConsentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun launch(onAccepted: () -> Unit = {}) {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    UpdateConsentScreen(onAccepted = onAccepted)
                }
            }
        }
    }

    // ── Render guards ─────────────────────────────────────────────────────────

    @Test
    fun updateConsentScreen_rendersTitle_withoutCrash() {
        launch()
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_update_title)).assertIsDisplayed()
    }

    @Test
    fun updateConsentScreen_agreeButton_disabledByDefault() {
        launch()
        composeTestRule.onNodeWithTag("update_consent_agree_button").assertIsNotEnabled()
    }

    @Test
    fun updateConsentScreen_declineButton_isVisible() {
        launch()
        composeTestRule.onNodeWithTag("update_consent_decline_button").assertIsDisplayed()
    }

    // ── Checkbox interaction ──────────────────────────────────────────────────

    @Test
    fun checkingCheckbox_enablesAgreeButton() {
        launch()
        composeTestRule.onNodeWithTag("update_consent_checkbox").performClick()
        composeTestRule.onNodeWithTag("update_consent_agree_button").assertIsEnabled()
    }

    @Test
    fun uncheckingCheckbox_disablesAgreeButton() {
        launch()
        composeTestRule.onNodeWithTag("update_consent_checkbox").performClick()
        composeTestRule.onNodeWithTag("update_consent_agree_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("update_consent_checkbox").performClick()
        composeTestRule.onNodeWithTag("update_consent_agree_button").assertIsNotEnabled()
    }

    @Test
    fun agreeButton_invokesCallback() {
        var accepted = false
        launch(onAccepted = { accepted = true })
        composeTestRule.onNodeWithTag("update_consent_checkbox").performClick()
        composeTestRule.onNodeWithTag("update_consent_agree_button").performClick()
        assertTrue(accepted)
    }

    // ── Content checks ────────────────────────────────────────────────────────

    @Test
    fun changesSection_isPresent() {
        launch()
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_update_changes_title)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun privacyPolicyLink_isVisible() {
        launch()
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.consent_read_privacy))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun tosLink_isVisible() {
        launch()
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.consent_read_tos))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ── Routing: outdated consent routes to UpdateConsentScreen ───────────────

    @Test
    fun outdatedConsent_routesToUpdateConsentScreen() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    val navController = rememberNavController()
                    val startDestination = resolveStartDestination(
                        consentVersion = 1,
                        onboardingDone = true,
                        whatsNewVersion = 0,
                    )
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Consent.route) { Text("Consent") }
                        composable(Screen.UpdateConsent.route) {
                            UpdateConsentScreen(onAccepted = {})
                        }
                        composable(Screen.Onboarding.route) { Text("Onboarding") }
                        composable(Screen.Home.route) { Text("Home") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_update_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Home").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Consent").assertIsNotDisplayed()
    }

    @Test
    fun currentConsent_doesNotRouteToUpdateConsentScreen() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    val navController = rememberNavController()
                    val startDestination = resolveStartDestination(
                        consentVersion = ThemeManager.CURRENT_CONSENT_VERSION,
                        onboardingDone = true,
                        whatsNewVersion = ThemeManager.CURRENT_WHATS_NEW_VERSION,
                    )
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Consent.route) { Text("Consent") }
                        composable(Screen.UpdateConsent.route) {
                            UpdateConsentScreen(onAccepted = {})
                        }
                        composable(Screen.Onboarding.route) { Text("Onboarding") }
                        composable(Screen.Home.route) { Text("Home") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_update_title)).assertIsNotDisplayed()
    }
}
