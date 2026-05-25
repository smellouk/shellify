package io.shellify.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.shellify.core.ui.R
import io.shellify.app.core.theme.ThemeMode
import java.util.Locale

/**
 * Named typography style for the incognito-mode badge rendered below the app name in the
 * WebView splash screen. Bold + 20% larger than bodyMedium so the privacy signal is prominent.
 */
val Typography.incognitoModeBadge
    get() = bodyMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = bodyMedium.fontSize * 1.2f,
    )

private val CairoFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

private fun arabicTypography() = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = CairoFamily),
        displayMedium = displayMedium.copy(fontFamily = CairoFamily),
        displaySmall = displaySmall.copy(fontFamily = CairoFamily),
        headlineLarge = headlineLarge.copy(fontFamily = CairoFamily),
        headlineMedium = headlineMedium.copy(fontFamily = CairoFamily),
        headlineSmall = headlineSmall.copy(fontFamily = CairoFamily),
        titleLarge = titleLarge.copy(fontFamily = CairoFamily),
        titleMedium = titleMedium.copy(fontFamily = CairoFamily),
        titleSmall = titleSmall.copy(fontFamily = CairoFamily),
        bodyLarge = bodyLarge.copy(fontFamily = CairoFamily),
        bodyMedium = bodyMedium.copy(fontFamily = CairoFamily),
        bodySmall = bodySmall.copy(fontFamily = CairoFamily),
        labelLarge = labelLarge.copy(fontFamily = CairoFamily),
        labelMedium = labelMedium.copy(fontFamily = CairoFamily),
        labelSmall = labelSmall.copy(fontFamily = CairoFamily),
    )
}

private val DarkColors = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColors = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun ShellifyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    accentColor: Int? = null,
    controlStatusBar: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (useDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }

        useDark -> DarkColors
        else -> LightColors
    }

    val finalColorScheme = if (accentColor != null) {
        val accent = Color(accentColor)
        val lum = with(accent) { 0.299f * red + 0.587f * green + 0.114f * blue }
        val onAccent = if (lum > 0.5f) Color.Black else Color.White
        // container = accent at low opacity over the scheme's surface for a gentle tonal fill
        val container = accent.copy(alpha = 0.15f)
        colorScheme.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = container,
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = container,
            onSecondaryContainer = accent,
        )
    } else colorScheme

    val typography = if (Locale.getDefault().language == "ar") arabicTypography() else Typography()

    val view = LocalView.current
    if (!view.isInEditMode && controlStatusBar) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = finalColorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                    !useDark
            }
        }
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = typography,
        content = content,
    )
}
