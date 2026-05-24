package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.domain.model.NetworkRequestLog
import io.shellify.app.presentation.settings.networklog.NetworkLogHistoryContent
import io.shellify.app.presentation.settings.networklog.NetworkLogHistoryUiState
import io.shellify.app.presentation.settings.networklog.SessionGroup
import io.shellify.app.presentation.theme.ShellifyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NetworkLogHistoryScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState() {
        composeTestRule.setContent {
            ShellifyTheme {
                NetworkLogHistoryContent(
                    state = NetworkLogHistoryUiState(sessions = emptyList(), isLoading = false),
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun nonEmptyState() {
        val sessions = listOf(
            SessionGroup(
                sessionId = "session-1",
                entries = listOf(
                    NetworkRequestLog(
                        id = 1L, appId = 1L, sessionId = "session-1",
                        hostname = "tracker.example.com",
                        url = "https://tracker.example.com/pixel.gif",
                        isBlocked = true,
                        timestamp = 1748099200000L,
                    ),
                    NetworkRequestLog(
                        id = 2L, appId = 1L, sessionId = "session-1",
                        hostname = "api.example.com",
                        url = "https://api.example.com/v1/data",
                        isBlocked = false,
                        timestamp = 1748099210000L,
                    ),
                    NetworkRequestLog(
                        id = 3L, appId = 1L, sessionId = "session-1",
                        hostname = "api.example.com",
                        url = "https://api.example.com/v1/user",
                        isBlocked = false,
                        timestamp = 1748099220000L,
                    ),
                ),
                startedAt = 1748099200000L,
            ),
        )
        composeTestRule.setContent {
            ShellifyTheme {
                NetworkLogHistoryContent(
                    state = NetworkLogHistoryUiState(sessions = sessions, isLoading = false),
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
