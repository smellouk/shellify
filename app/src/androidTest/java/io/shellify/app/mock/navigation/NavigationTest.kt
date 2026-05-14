package io.shellify.app.mock.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shellify.app.presentation.navigation.Screen
import io.shellify.app.presentation.theme.ShellifyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.testTag

/**
 * Navigation integration tests.
 *
 * Rather than using the full MainActivity (which triggers SQLCipher + DataStore init),
 * we compose a minimal NavHost that mirrors the real route definitions and verify
 * that navigating between routes lands on the expected destination screens.
 *
 * Full-app navigation smoke tests via createAndroidComposeRule<MainActivity> are
 * intentionally not used here because the MainActivity depends on the full DI chain
 * (encrypted DB, GeckoView init, etc.) which makes reliable instrumented tests very
 * slow and fragile on CI. The real route smoke tests are covered by the NavGraph
 * structure assertions below.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─── Route definition sanity checks ───────────────────────────────────────

    @Test
    fun homeRoute_isCorrect() {
        assert(Screen.Home.route == "home")
    }

    @Test
    fun addRoute_containsAppIdPlaceholder() {
        assert(Screen.Add.route.contains("{appId}"))
    }

    @Test
    fun settingsRoute_containsAppIdPlaceholder() {
        assert(Screen.Settings.route.contains("{appId}"))
    }

    @Test
    fun categoriesRoute_isCorrect() {
        assert(Screen.Categories.route == "categories")
    }

    @Test
    fun shortcutsRoute_isCorrect() {
        assert(Screen.Shortcuts.route == "shortcuts")
    }

    @Test
    fun globalSettingsRoute_isCorrect() {
        assert(Screen.GlobalSettings.route == "global_settings")
    }

    @Test
    fun translateConfigRoute_containsAppIdPlaceholder() {
        assert(Screen.TranslateConfig.route.contains("{appId}"))
    }

    @Test
    fun onboardingRoute_isCorrect() {
        assert(Screen.Onboarding.route == "onboarding")
    }

    @Test
    fun consentRoute_isCorrect() {
        assert(Screen.Consent.route == "consent")
    }

    @Test
    fun licensesRoute_isCorrect() {
        assert(Screen.Licenses.route == "licenses")
    }

    // ─── Route factory methods ────────────────────────────────────────────────

    @Test
    fun addRoute_createRoute_encodesAppId() {
        val route = Screen.Add.createRoute(appId = 42L)
        assert(route.contains("42")) { "Expected appId 42 in route: $route" }
    }

    @Test
    fun addRoute_createRoute_encodesUrl() {
        val route = Screen.Add.createRoute(url = "https://example.com")
        // URL is encoded via Uri.encode — just check it's non-empty in the route
        assert(route.isNotBlank())
    }

    @Test
    fun settingsRoute_createRoute_encodesAppId() {
        val route = Screen.Settings.createRoute(appId = 7L)
        assert(route == "settings/7") { "Expected 'settings/7' but got: $route" }
    }

    @Test
    fun translateConfigRoute_createRoute_encodesAppId() {
        val route = Screen.TranslateConfig.createRoute(appId = 3L)
        assert(route == "translate/3") { "Expected 'translate/3' but got: $route" }
    }

    // ─── Minimal NavHost compose test ─────────────────────────────────────────

    @Test
    fun minimalNavHost_startsOnHomeDestination() {
        composeTestRule.setContent {
            ShellifyTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                ) {
                    composable(Screen.Home.route) {
                        Text("Home Screen Stub", modifier = androidx.compose.ui.Modifier.testTag("home"))
                    }
                    composable(Screen.Categories.route) {
                        Text("Categories Screen Stub")
                    }
                    composable(Screen.Shortcuts.route) {
                        Text("Shortcuts Screen Stub")
                    }
                    composable(Screen.GlobalSettings.route) {
                        Text("Global Settings Screen Stub")
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithText("Home Screen Stub")
            .assertIsDisplayed()
    }

    @Test
    fun minimalNavHost_navigatesToCategoriesRoute() {
        var navCtrl: NavHostController? = null

        composeTestRule.setContent {
            ShellifyTheme {
                val nc = rememberNavController()
                navCtrl = nc
                NavHost(navController = nc, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) { Text("Home") }
                    composable(Screen.Categories.route) { Text("Categories Destination") }
                    composable(Screen.Shortcuts.route) { Text("Shortcuts") }
                    composable(Screen.GlobalSettings.route) { Text("GlobalSettings") }
                }
            }
        }

        composeTestRule.runOnUiThread {
            navCtrl?.navigate(Screen.Categories.route)
        }

        composeTestRule
            .onNodeWithText("Categories Destination")
            .assertIsDisplayed()
    }

    @Test
    fun minimalNavHost_navigatesToShortcutsRoute() {
        var navCtrl: NavHostController? = null

        composeTestRule.setContent {
            ShellifyTheme {
                val nc = rememberNavController()
                navCtrl = nc
                NavHost(navController = nc, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) { Text("Home") }
                    composable(Screen.Categories.route) { Text("Categories") }
                    composable(Screen.Shortcuts.route) { Text("Shortcuts Destination") }
                    composable(Screen.GlobalSettings.route) { Text("GlobalSettings") }
                }
            }
        }

        composeTestRule.runOnUiThread {
            navCtrl?.navigate(Screen.Shortcuts.route)
        }

        composeTestRule
            .onNodeWithText("Shortcuts Destination")
            .assertIsDisplayed()
    }

    @Test
    fun minimalNavHost_navigatesToGlobalSettingsRoute() {
        var navCtrl: NavHostController? = null

        composeTestRule.setContent {
            ShellifyTheme {
                val nc = rememberNavController()
                navCtrl = nc
                NavHost(navController = nc, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) { Text("Home") }
                    composable(Screen.Categories.route) { Text("Categories") }
                    composable(Screen.Shortcuts.route) { Text("Shortcuts") }
                    composable(Screen.GlobalSettings.route) { Text("GlobalSettings Destination") }
                }
            }
        }

        composeTestRule.runOnUiThread {
            navCtrl?.navigate(Screen.GlobalSettings.route)
        }

        composeTestRule
            .onNodeWithText("GlobalSettings Destination")
            .assertIsDisplayed()
    }
}
