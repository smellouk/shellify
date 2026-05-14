package io.shellify.app.e2e

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
import io.shellify.app.presentation.navigation.Screen
import io.shellify.app.presentation.navigation.resolveStartDestination
import io.shellify.app.presentation.onboarding.ConsentScreen
import io.shellify.app.presentation.theme.ShellifyTheme
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

    // ── Routing contract ──────────────────────────────────────────────────────

    @Test
    fun freshInstall_startsOnConsentScreen() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    val navController = rememberNavController()
                    val startDestination = resolveStartDestination(
                        consentGiven = false,
                        onboardingDone = false,
                    )
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Consent.route) { ConsentScreen(onAccepted = {}) }
                        composable(Screen.Onboarding.route) { Text("Onboarding") }
                        composable(Screen.Home.route) { Text("Home") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Before you start").assertIsDisplayed()
        composeTestRule.onNodeWithText("Onboarding").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Home").assertIsNotDisplayed()
    }

    @Test
    fun consentAlreadyGiven_skipsConsentScreen() {
        composeTestRule.setContent {
            ShellifyTheme {
                Box(modifier = Modifier.size(400.dp, 900.dp)) {
                    val navController = rememberNavController()
                    val startDestination = resolveStartDestination(
                        consentGiven = true,
                        onboardingDone = false,
                    )
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Consent.route) { ConsentScreen(onAccepted = {}) }
                        composable(Screen.Onboarding.route) { Text("Onboarding Screen") }
                        composable(Screen.Home.route) { Text("Home") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Onboarding Screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Before you start").assertIsNotDisplayed()
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

        composeTestRule.onNodeWithText("Before you start").assertIsDisplayed()
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
