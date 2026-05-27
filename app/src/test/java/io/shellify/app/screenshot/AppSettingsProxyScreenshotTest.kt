package io.shellify.app.screenshot

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.ProxyType
import io.shellify.app.domain.model.WebApp
import io.shellify.app.presentation.theme.ShellifyTheme
import io.shellify.app.presentation.webview.CustomProxyState
import io.shellify.app.presentation.webview.WebViewControlCenterSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi screenshot regression tests for the custom proxy UI (Phase 18, PRX-13).
 *
 * Two goldens for CustomProxySection in AppSettings (via WebApp state) and
 * two goldens for the WebViewControlCenter proxy card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSettingsProxyScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val geckoApp = WebApp(
        name = "ProxyApp",
        url = "https://example.com",
        engineType = EngineType.GECKOVIEW,
        customProxyType = ProxyType.NONE,
    )

    private val proxyActiveSocks5App = WebApp(
        name = "ProxyApp",
        url = "https://example.com",
        engineType = EngineType.GECKOVIEW,
        customProxyType = ProxyType.SOCKS5,
        customProxyHost = "proxy.example.com",
        customProxyPort = 1080,
        customProxyUsername = "alice",
        customProxyPassword = "secret",
    )

    /**
     * Golden: sheet_proxyCardActive — proxy card in Active state in control center header.
     */
    @Test
    fun sheet_proxyCardActive() {
        composeTestRule.setContent {
            ShellifyTheme {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant) {
                    WebViewControlCenterSheet(
                        pwaApp = proxyActiveSocks5App,
                        hasGlobalPassword = false,
                        customProxyState = CustomProxyState.Active,
                        onDisableProxy = {},
                        onRetryProxy = {},
                        onAdBlockChanged = {},
                        onTranslateChanged = {},
                        onFullscreenChanged = {},
                        onLockChanged = {},
                        onClearData = {},
                        onNetworkLogClick = {},
                        isReadingModeActive = false,
                        onReadingModeToggled = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }

    /**
     * Golden: sheet_proxyCardUnreachable — proxy card in Unreachable state, retry icon visible.
     */
    @Test
    fun sheet_proxyCardUnreachable() {
        composeTestRule.setContent {
            ShellifyTheme {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant) {
                    WebViewControlCenterSheet(
                        pwaApp = proxyActiveSocks5App,
                        hasGlobalPassword = false,
                        customProxyState = CustomProxyState.Unreachable,
                        onDisableProxy = {},
                        onRetryProxy = {},
                        onAdBlockChanged = {},
                        onTranslateChanged = {},
                        onFullscreenChanged = {},
                        onLockChanged = {},
                        onClearData = {},
                        onNetworkLogClick = {},
                        isReadingModeActive = false,
                        onReadingModeToggled = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotOptions)
    }
}
