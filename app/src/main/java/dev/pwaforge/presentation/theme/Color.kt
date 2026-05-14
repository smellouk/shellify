package dev.pwaforge.presentation.theme

import androidx.compose.ui.graphics.Color

// Material3 baseline palette (used by Theme.kt fallback color schemes)
val Purple80     = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80       = Color(0xFFEFB8C8)

val Purple40     = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40       = Color(0xFF7D5260)

// Feature-tag badge colors
val TagFullscreen   = Color(0xFFFB8C00) // orange  – fullscreen mode indicator
val TagAdBlock      = Color(0xFF43A047) // green   – ad-block indicator
val TagTranslate    = Color(0xFF1E88E5) // blue    – translate indicator
val TagLockPassword = Color(0xFF7C4DFF) // purple  – password-lock indicator
val TagLockSystem   = Color(0xFF3F51B5) // indigo  – system-lock indicator
val GeckoWarning    = Color(0xFFFF9800) // orange  – GeckoView not installed
val VerifiedGreen   = Color(0xFF4CAF50) // green   – SHA-256 integrity verified

// User-selectable accent palette (shared between onboarding and global settings)
val ACCENT_COLORS = listOf(
    0xFF33691E.toInt(), // Green
    0xFF3F4FBF.toInt(), // Indigo
    0xFF006B5F.toInt(), // Teal
    0xFFB5365E.toInt(), // Rose
    0xFF7A5300.toInt(), // Amber
)

// Category suggestion palette
val CategoryMediaFg   = Color(0xFF7A5300)
val CategoryMediaBg   = Color(0xFFFFE7BD)
val CategoryReadingFg = Color(0xFFB5365E)
val CategoryReadingBg = Color(0xFFFFD9E2)
val CategoryToolsFg   = Color(0xFF006B5F)
val CategoryToolsBg   = Color(0xFFDAF8F2)
val SuggestionVideoBg = Color(0xFFD1F5E5)  // teal-light bg for Video suggestion tile
val SuggestionChatBg  = Color(0xFFDCE7FF)  // indigo-light bg for Chat suggestion tile
val SuggestionChatFg  = Color(0xFF3A5BBF)  // indigo fg for Chat suggestion tile
