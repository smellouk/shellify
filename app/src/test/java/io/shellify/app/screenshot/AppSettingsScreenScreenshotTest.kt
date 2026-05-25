package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.settings.AppSettingsScreen
import io.shellify.app.presentation.settings.AppSettingsUiState
import io.shellify.app.presentation.settings.AppSettingsViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.presentation.settings.AppSettingsCommand
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
class AppSettingsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(state: AppSettingsUiState): AppSettingsViewModel {
        val gecko = mockk<GeckoEngineManager>(relaxed = true).also {
            every { it.installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
        }
        return mockk<AppSettingsViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
            every { it.geckoEngineManager } returns gecko
            every { it.commands } returns MutableSharedFlow<AppSettingsCommand>()
        }
    }

    private fun app(name: String, url: String) = WebApp(
        id = 1L, name = name, url = url,
        isolationId = UUID.randomUUID().toString(),
        isFullscreen = false, adBlockEnabled = true, translateEnabled = false,
    )

    @Test
    fun loadingState() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(AppSettingsUiState(app = null, isLoading = true)),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun populatedApp() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(AppSettingsUiState(app = app("Notion", "https://notion.so"), isLoading = false)),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun deleteConfirmationDialog() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(app = app("Asana", "https://asana.com"), isLoading = false, showDeleteDialog = true)
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun controlCenterToggle_enabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app("Linear", "https://linear.app").copy(showControlCenter = true),
                            isLoading = false,
                        )
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun controlCenterToggle_disabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app("Linear", "https://linear.app").copy(showControlCenter = false),
                            isLoading = false,
                        )
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun swipeToRefreshToggle_enabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app("Notion", "https://notion.so").copy(swipeToRefreshEnabled = true),
                            isLoading = false,
                        )
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun swipeToRefreshToggle_disabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app("Notion", "https://notion.so").copy(swipeToRefreshEnabled = false),
                            isLoading = false,
                        )
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun torSection_useTorEnabled_geckoView() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app("ProtonMail", "https://mail.proton.me").copy(
                                useTor = true,
                                engineType = EngineType.GECKOVIEW,
                            ),
                            isLoading = false,
                        )
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun torSection_useTorDisabled_systemWebView() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app("Reddit", "https://reddit.com").copy(
                                useTor = false,
                                engineType = EngineType.SYSTEM_WEBVIEW,
                            ),
                            isLoading = false,
                        )
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun torSection_preserveIdentity_enabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app("ProtonMail", "https://mail.proton.me").copy(
                                useTor = true,
                                preserveTorIdentity = true,
                                engineType = EngineType.GECKOVIEW,
                            ),
                            isLoading = false,
                        )
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun privacySection_alwaysIncognito_enabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                AppSettingsScreen(
                    viewModel = buildVm(
                        AppSettingsUiState(
                            app = app("Twitter", "https://twitter.com").copy(alwaysIncognito = true),
                            isLoading = false,
                        )
                    ),
                    onBack = {}, onDeleted = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
