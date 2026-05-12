package dev.pwaforge.presentation

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import dev.pwaforge.core.locale.LocaleHelper
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import dev.pwaforge.PWAForgeApplication
import dev.pwaforge.core.theme.CircularRevealOverlay
import dev.pwaforge.core.theme.LocalThemeRevealState
import dev.pwaforge.core.theme.ThemeMode
import dev.pwaforge.core.theme.rememberThemeRevealState
import dev.pwaforge.presentation.navigation.AppNavigation
import dev.pwaforge.presentation.theme.PWAForgeTheme

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PWAForgeApplication
        setContent {
            // Created outside PWAForgeTheme so screenshots capture the current theme
            val themeRevealState = rememberThemeRevealState()

            val themeMode by app.themeManager.themeMode.collectAsState(ThemeMode.SYSTEM)
            val dynamicColor by app.themeManager.dynamicColor.collectAsState(true)
            val accentColor by app.themeManager.accentColor.collectAsState(null)
            val screenshotProtection by app.passwordManager.screenshotProtection.collectAsState(false)

            LaunchedEffect(screenshotProtection) {
                if (screenshotProtection) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            PWAForgeTheme(themeMode = themeMode, dynamicColor = dynamicColor, accentColor = accentColor) {
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
