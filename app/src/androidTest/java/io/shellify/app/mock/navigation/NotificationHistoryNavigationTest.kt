package io.shellify.app.mock.navigation

import androidx.compose.foundation.layout.padding
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
 * Verifies that navigating to and from Screen.NotificationHistory works correctly
 * using a minimal stub NavHost (no full DI chain required).
 */
@RunWith(AndroidJUnit4::class)
class NotificationHistoryNavigationTest {

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
                    androidx.compose.material3.Scaffold(
                        topBar = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    navController.navigate(
                                        Screen.NotificationHistory.createRoute(appId)
                                    )
                                },
                                modifier = Modifier.testTag("open_history"),
                            ) {
                                Text(context.getString(CoreUiR.string.settings_notifications_history))
                            }
                        },
                    ) { padding ->
                        Text(
                            text = "Settings Screen",
                            modifier = Modifier.padding(padding).testTag("screen_settings"),
                        )
                    }
                }
                composable(
                    route = Screen.NotificationHistory.route,
                    arguments = listOf(navArgument("appId") { type = NavType.LongType }),
                ) {
                    Text(
                        text = context.getString(CoreUiR.string.settings_notifications_history_title),
                        modifier = Modifier.testTag("screen_history"),
                    )
                }
            }
        }
    }

    @Test
    fun notificationHistoryRoute_isRegistered() {
        composeTestRule.setContent { TestApp() }
        composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_history").performClick()
        composeTestRule.onNodeWithTag("screen_history").assertIsDisplayed()
    }

    @Test
    fun notificationHistoryScreen_showsTitle() {
        composeTestRule.setContent { TestApp() }
        composeTestRule.onNodeWithTag("open_history").performClick()
        composeTestRule.onNodeWithText(
            context.getString(CoreUiR.string.settings_notifications_history_title)
        ).assertIsDisplayed()
    }
}
