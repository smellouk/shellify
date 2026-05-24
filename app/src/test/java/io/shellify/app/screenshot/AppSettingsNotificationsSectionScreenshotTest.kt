package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.settings.AppSettingsScreen
import io.shellify.app.presentation.settings.AppSettingsUiState
import io.shellify.app.presentation.settings.AppSettingsViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import androidx.test.core.app.ApplicationProvider
import io.shellify.core.ui.R as CoreUiR
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSettingsNotificationsSectionScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(state: AppSettingsUiState): AppSettingsViewModel {
        val gecko = mockk<GeckoEngineManager>(relaxed = true).also {
            every { it.installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
        }
        return mockk<AppSettingsViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
            every { it.geckoEngineManager } returns gecko
            every { it.commands } returns MutableSharedFlow()
        }
    }

    private fun app(
        notificationPermission: NotificationPermission = NotificationPermission.NOT_ASKED,
        engineType: EngineType = EngineType.SYSTEM_WEBVIEW,
        dndStartHour: Int = -1,
        dndEndHour: Int = -1,
        backgroundNotificationsEnabled: Boolean = false,
    ) = WebApp(
        id = 1L,
        name = "TestApp",
        url = "https://test.com",
        isolationId = UUID.randomUUID().toString(),
        notificationPermission = notificationPermission,
        engineType = engineType,
        dndStartHour = dndStartHour,
        dndEndHour = dndEndHour,
        backgroundNotificationsEnabled = backgroundNotificationsEnabled,
    )

    @Test
    fun notifications_grantedWithGeckoview() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app(
                                notificationPermission = NotificationPermission.GRANTED,
                                engineType = EngineType.GECKOVIEW,
                                dndStartHour = 22,
                                dndEndHour = 8,
                                backgroundNotificationsEnabled = true,
                            ),
                            isLoading = false,
                        )
                    ),
                    onBack = {},
                    onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun notifications_grantedWithSystemWebView() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app(
                                notificationPermission = NotificationPermission.GRANTED,
                                engineType = EngineType.SYSTEM_WEBVIEW,
                                dndStartHour = 22,
                                dndEndHour = 8,
                            ),
                            isLoading = false,
                        )
                    ),
                    onBack = {},
                    onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun notifications_denied() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app(notificationPermission = NotificationPermission.DENIED),
                            isLoading = false,
                        )
                    ),
                    onBack = {},
                    onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun notifications_globallyDisabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app(notificationPermission = NotificationPermission.GRANTED),
                            isLoading = false,
                            globalNotificationsEnabled = false,
                        )
                    ),
                    onBack = {},
                    onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun notifications_notAsked() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app(notificationPermission = NotificationPermission.NOT_ASKED),
                            isLoading = false,
                        )
                    ),
                    onBack = {},
                    onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun networkLog_downloadIconInRow() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app(notificationPermission = NotificationPermission.NOT_ASKED),
                            isLoading = false,
                        )
                    ),
                    onBack = {},
                    onDeleted = {},
                )
            }
        }
        // Scroll until the download icon is on screen, then capture.
        val exportCd = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(CoreUiR.string.network_log_export_cd)
        composeTestRule
            .onNodeWithContentDescription(exportCd)
            .performScrollTo()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
