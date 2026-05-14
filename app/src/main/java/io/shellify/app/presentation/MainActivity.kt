package io.shellify.app.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import io.shellify.app.core.locale.LocaleHelper
import io.shellify.app.core.deeplink.DeepLinkHandler
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import io.shellify.app.ShellifyApplication
import io.shellify.app.core.theme.CircularRevealOverlay
import io.shellify.app.core.theme.LocalThemeRevealState
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.core.theme.rememberThemeRevealState
import io.shellify.app.presentation.navigation.AppNavigation
import io.shellify.app.presentation.theme.ShellifyTheme

class MainActivity : AppCompatActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { handleDeepLinkUri(it) }
    }

    private fun handleDeepLinkUri(uri: Uri) {
        val parsed = DeepLinkHandler.parse(uri) ?: return
        val app = application as ShellifyApplication
        lifecycleScope.launch { app.pendingDeepLink.emit(parsed) }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ShellifyApplication
        intent?.data?.let { handleDeepLinkUri(it) }
        setContent {
            // Created outside ShellifyTheme so screenshots capture the current theme
            val themeRevealState = rememberThemeRevealState()

            val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
            val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
            val accentColor by app.themeManager.accentColor.collectAsState(null)
            val screenshotProtection by app.passwordManager.screenshotProtection.collectAsState(false)

            LaunchedEffect(screenshotProtection) {
                if (screenshotProtection) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            ShellifyTheme(themeMode = themeMode, dynamicColor = dynamicColor, accentColor = accentColor) {
                CompositionLocalProvider(LocalThemeRevealState provides themeRevealState) {
                    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        AppNavigation(
                            navController = navController,
                            app = app,
                        )
                        CircularRevealOverlay(revealState = themeRevealState)
                    }
                }
            }
        }
    }
}
