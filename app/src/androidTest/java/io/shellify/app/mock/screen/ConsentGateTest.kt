package io.shellify.app.mock.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.presentation.navigation.Screen
import io.shellify.app.presentation.navigation.resolveStartDestination
import io.shellify.app.presentation.onboarding.ConsentScreen
import io.shellify.app.presentation.onboarding.UpdateConsentScreen
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.core.ui.R as CoreUiR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard: the consent screen must always be the first screen on a fresh install
 * and must never crash on launch. Tests here fail if the routing logic or the composable
 * itself is broken.
 */
@RunWith(AndroidJUnit4::class)
class ConsentGateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ── Routing contract ──────────────────────────────────────────────────────

    @Test
    fun freshInstall_startsOnConsentScreen() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    val navController = rememberNavController()
                    val startDestination = resolveStartDestination(
                        consentVersion = 0,
                        onboardingDone = false,
                        whatsNewVersion = 0,
                    )
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Consent.route) { ConsentScreen(onAccepted = {}) }
                        composable(Screen.UpdateConsent.route) { UpdateConsentScreen(onAccepted = {}) }
                        composable(Screen.Onboarding.route) { Text("Onboarding") }
                        composable(Screen.Home.route) { Text("Home") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Onboarding").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Home").assertIsNotDisplayed()
    }

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
                        composable(Screen.Consent.route) { ConsentScreen(onAccepted = {}) }
                        composable(Screen.UpdateConsent.route) { UpdateConsentScreen(onAccepted = {}) }
                        composable(Screen.Onboarding.route) { Text("Onboarding") }
                        composable(Screen.Home.route) { Text("Home") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_update_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_title)).assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Home").assertIsNotDisplayed()
    }

    @Test
    fun currentConsent_onboardingPending_routesToOnboarding() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    val navController = rememberNavController()
                    val startDestination = resolveStartDestination(
                        consentVersion = ThemeManager.CURRENT_CONSENT_VERSION,
                        onboardingDone = false,
                        whatsNewVersion = 0,
                    )
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Consent.route) { ConsentScreen(onAccepted = {}) }
                        composable(Screen.UpdateConsent.route) { UpdateConsentScreen(onAccepted = {}) }
                        composable(Screen.Onboarding.route) { Text("Onboarding Screen") }
                        composable(Screen.Home.route) { Text("Home") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Onboarding Screen").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_title)).assertIsNotDisplayed()
    }

    // ── Render guard ──────────────────────────────────────────────────────────

    @Test
    fun consentScreen_rendersTitle_onFirstLaunch() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    ConsentScreen(onAccepted = {})
                }
            }
        }

        composeTestRule.onNodeWithText(context.getString(CoreUiR.string.consent_title)).assertIsDisplayed()
    }

    @Test
    fun consentScreen_footerIsVisible_onFirstLaunch() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    ConsentScreen(onAccepted = {})
                }
            }
        }

        composeTestRule.onNodeWithTag("consent_agree_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("consent_decline_button").assertIsDisplayed()
    }
}
