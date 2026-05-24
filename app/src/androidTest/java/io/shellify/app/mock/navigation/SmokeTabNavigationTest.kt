package io.shellify.app.mock.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shellify.app.presentation.navigation.Screen
import io.shellify.app.presentation.theme.ShellifyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for bottom-tab navigation.
 *
 * Verifies that all 4 top-level destinations are reachable by tapping the bottom nav
 * and that navigating between them does not crash. Uses a minimal Scaffold + NavHost
 * with stub screen content to avoid the full DI chain.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTabNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tabs = listOf(
        Triple(Screen.Home.route, "Apps", "screen_home"),
        Triple(Screen.Categories.route, "Categories", "screen_categories"),
        Triple(Screen.Shortcuts.route, "Shortcuts", "screen_shortcuts"),
        Triple(Screen.GlobalSettings.route, "Settings", "screen_settings"),
    )

    @Composable
    private fun TestScaffold() {
        ShellifyTheme {
            val navController = rememberNavController()
            Scaffold(
                bottomBar = { TestBottomNav(navController) }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(padding),
                ) {
                    composable(Screen.Home.route) {
                        Text("Home Content", modifier = Modifier.testTag("screen_home"))
                    }
                    composable(Screen.Categories.route) {
                        Text("Categories Content", modifier = Modifier.testTag("screen_categories"))
                    }
                    composable(Screen.Shortcuts.route) {
                        Text("Shortcuts Content", modifier = Modifier.testTag("screen_shortcuts"))
                    }
                    composable(Screen.GlobalSettings.route) {
                        Text("Settings Content", modifier = Modifier.testTag("screen_settings"))
                    }
                }
            }
        }
    }

    @Composable
    private fun TestBottomNav(navController: NavHostController) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { (route, label, _) ->
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .testTag("tab_$label"),
                )
            }
        }
    }

    @Test
    fun appStartsOnHomeScreen() {
        composeTestRule.setContent { TestScaffold() }
        composeTestRule.onNodeWithText("Home Content").assertIsDisplayed()
    }

    @Test
    fun tapCategories_showsCategoriesScreen() {
        composeTestRule.setContent { TestScaffold() }
        composeTestRule.onNodeWithTag("tab_Categories").performClick()
        composeTestRule.onNodeWithText("Categories Content").assertIsDisplayed()
    }

    @Test
    fun tapShortcuts_showsShortcutsScreen() {
        composeTestRule.setContent { TestScaffold() }
        composeTestRule.onNodeWithTag("tab_Shortcuts").performClick()
        composeTestRule.onNodeWithText("Shortcuts Content").assertIsDisplayed()
    }

    @Test
    fun tapSettings_showsSettingsScreen() {
        composeTestRule.setContent { TestScaffold() }
        composeTestRule.onNodeWithTag("tab_Settings").performClick()
        composeTestRule.onNodeWithText("Settings Content").assertIsDisplayed()
    }

    @Test
    fun tapAllTabsSequentially_noDestinationCrashes() {
        composeTestRule.setContent { TestScaffold() }

        composeTestRule.onNodeWithTag("tab_Categories").performClick()
        composeTestRule.onNodeWithText("Categories Content").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_Shortcuts").performClick()
        composeTestRule.onNodeWithText("Shortcuts Content").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_Settings").performClick()
        composeTestRule.onNodeWithText("Settings Content").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_Apps").performClick()
        composeTestRule.onNodeWithText("Home Content").assertIsDisplayed()
    }

    @Test
    fun tapCategoriesThenBackToApps_restoresHome() {
        composeTestRule.setContent { TestScaffold() }

        composeTestRule.onNodeWithTag("tab_Categories").performClick()
        composeTestRule.onNodeWithText("Categories Content").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_Apps").performClick()
        composeTestRule.onNodeWithText("Home Content").assertIsDisplayed()
    }

    @Test
    fun allBottomNavTabsAreVisible() {
        composeTestRule.setContent { TestScaffold() }
        tabs.forEach { (_, label, _) ->
            composeTestRule.onNodeWithTag("tab_$label").assertIsDisplayed()
        }
    }
}
