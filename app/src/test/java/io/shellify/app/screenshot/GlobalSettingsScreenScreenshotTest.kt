package io.shellify.app.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.presentation.settings.GlobalSettingsScreen
import io.shellify.app.presentation.settings.GlobalSettingsUiState
import io.shellify.app.presentation.settings.GlobalSettingsViewModel
import io.shellify.app.domain.model.EngineType
import io.shellify.app.presentation.settings.PasswordDialogMode
import io.shellify.app.presentation.theme.ShellifyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlobalSettingsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildVm(state: GlobalSettingsUiState): GlobalSettingsViewModel {
        val gecko = mockk<GeckoEngineManager>(relaxed = true).also {
            every { it.installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
            every { it.latestVersion } returns MutableStateFlow(null)
        }
        val simpleIcons = mockk<SimpleIconsManager>(relaxed = true).also {
            every { it.state } returns MutableStateFlow(SimpleIconsState.NotImported)
        }
        return mockk<GlobalSettingsViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
            every { it.geckoEngineManager } returns gecko
            every { it.simpleIconsManager } returns simpleIcons
        }
    }

    @Test
    fun loadingState() {
        composeTestRule.setContent {
            ShellifyTheme {
                GlobalSettingsScreen(
                    viewModel = buildVm(GlobalSettingsUiState(isLoaded = false)),
                    onLicenses = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun defaultSettings() {
        composeTestRule.setContent {
            ShellifyTheme {
                GlobalSettingsScreen(
                    viewModel = buildVm(GlobalSettingsUiState(isLoaded = true)),
                    onLicenses = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun withPasswordEnabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                GlobalSettingsScreen(
                    viewModel = buildVm(
                        GlobalSettingsUiState(
                            isLoaded = true,
                            hasPassword = true,
                            wipeOnFailedAttempts = false,
                            screenshotProtection = true,
                        )
                    ),
                    onLicenses = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun withBackupEnabled() {
        composeTestRule.setContent {
            ShellifyTheme {
                GlobalSettingsScreen(
                    viewModel = buildVm(
                        GlobalSettingsUiState(
                            isLoaded = true,
                            backupEnabled = true,
                            backupHasPassword = true,
                            backupDirectoryUri = "content://com.android.externalstorage/tree/primary%3AShellify",
                        )
                    ),
                    onLicenses = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun clearDataDialog() {
        composeTestRule.setContent {
            ShellifyTheme {
                GlobalSettingsScreen(
                    viewModel = buildVm(
                        GlobalSettingsUiState(isLoaded = true, showClearAllDialog = true)
                    ),
                    onLicenses = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun geckoViewSelectedSafeBrowsingOff() {
        composeTestRule.setContent {
            ShellifyTheme {
                GlobalSettingsScreen(
                    viewModel = buildVm(
                        GlobalSettingsUiState(
                            isLoaded = true,
                            defaultEngineType = EngineType.GECKOVIEW,
                            geckoSafeBrowsing = false,
                        )
                    ),
                    onLicenses = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun geckoViewSelectedSafeBrowsingOn() {
        composeTestRule.setContent {
            ShellifyTheme {
                GlobalSettingsScreen(
                    viewModel = buildVm(
                        GlobalSettingsUiState(
                            isLoaded = true,
                            defaultEngineType = EngineType.GECKOVIEW,
                            geckoSafeBrowsing = true,
                        )
                    ),
                    onLicenses = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    @Test
    fun setPasswordDialog() {
        composeTestRule.setContent {
            ShellifyTheme {
                GlobalSettingsScreen(
                    viewModel = buildVm(
                        GlobalSettingsUiState(
                            isLoaded = true,
                            showPasswordDialog = true,
                            passwordDialogMode = PasswordDialogMode.SET,
                        )
                    ),
                    onLicenses = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
