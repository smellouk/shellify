package dev.pwaforge.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.pwaforge.R
import dev.pwaforge.core.theme.ThemeMode
import java.util.Locale

private val CairoFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

private fun arabicTypography() = Typography().run {
    copy(
        displayLarge    = displayLarge.copy(fontFamily    = CairoFamily),
        displayMedium   = displayMedium.copy(fontFamily   = CairoFamily),
        displaySmall    = displaySmall.copy(fontFamily    = CairoFamily),
        headlineLarge   = headlineLarge.copy(fontFamily   = CairoFamily),
        headlineMedium  = headlineMedium.copy(fontFamily  = CairoFamily),
        headlineSmall   = headlineSmall.copy(fontFamily   = CairoFamily),
        titleLarge      = titleLarge.copy(fontFamily      = CairoFamily),
        titleMedium     = titleMedium.copy(fontFamily     = CairoFamily),
        titleSmall      = titleSmall.copy(fontFamily      = CairoFamily),
        bodyLarge       = bodyLarge.copy(fontFamily       = CairoFamily),
        bodyMedium      = bodyMedium.copy(fontFamily      = CairoFamily),
        bodySmall       = bodySmall.copy(fontFamily       = CairoFamily),
        labelLarge      = labelLarge.copy(fontFamily      = CairoFamily),
        labelMedium     = labelMedium.copy(fontFamily     = CairoFamily),
        labelSmall      = labelSmall.copy(fontFamily      = CairoFamily),
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
)

@Composable
fun PWAForgeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    accentColor: Int? = null,
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
            primary              = accent,
            onPrimary            = onAccent,
            primaryContainer     = container,
            onPrimaryContainer   = accent,
            secondary            = accent,
            onSecondary          = onAccent,
            secondaryContainer   = container,
            onSecondaryContainer = accent,
        )
    } else colorScheme

    val typography = if (Locale.getDefault().language == "ar") arabicTypography() else Typography()

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = typography,
        content = content,
    )
}
