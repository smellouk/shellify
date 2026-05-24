package io.shellify.app.mock.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.shellify.app.presentation.navigation.Screen
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.core.ui.R as CoreUiR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that navigating to and from Screen.NetworkLog works correctly
 * using a minimal stub NavHost (no full DI chain required).
 */
@RunWith(AndroidJUnit4::class)
class NetworkLogNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Composable
    private fun TestApp(appId: Long = 42L) {
        ShellifyTheme {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screen.Settings.createRoute(appId),
            ) {
                composable(
                    route = Screen.Settings.route,
                    arguments = listOf(navArgument("appId") { type = NavType.LongType }),
                ) {
                    Scaffold(
                        topBar = {
                            Button(
                                onClick = {
                                    navController.navigate(Screen.NetworkLog.createRoute(appId))
                                },
                                modifier = Modifier.testTag("open_network_log"),
                            ) {
                                Text(context.getString(CoreUiR.string.settings_network_log))
                            }
                        },
                    ) { padding ->
                        Text(
                            text = "Settings Screen",
                            modifier = Modifier
                                .padding(padding)
                                .testTag("screen_settings"),
                        )
                    }
                }
                composable(
                    route = Screen.NetworkLog.route,
                    arguments = listOf(navArgument("appId") { type = NavType.LongType }),
                ) {
                    Text(
                        text = context.getString(CoreUiR.string.network_log_history_title),
                        modifier = Modifier.testTag("screen_network_log"),
                    )
                }
            }
        }
    }

    @Test
    fun networkLogRoute_isRegistered() {
        composeTestRule.setContent { TestApp() }
        composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_network_log").performClick()
        composeTestRule.onNodeWithTag("screen_network_log").assertIsDisplayed()
    }

    @Test
    fun networkLogScreen_showsTitle() {
        composeTestRule.setContent { TestApp() }
        composeTestRule.onNodeWithTag("open_network_log").performClick()
        composeTestRule
            .onNodeWithText(context.getString(CoreUiR.string.network_log_history_title))
            .assertIsDisplayed()
    }
}
