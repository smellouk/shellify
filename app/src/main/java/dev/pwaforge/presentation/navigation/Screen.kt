package dev.pwaforge.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Add : Screen("add/{appId}") {
        fun createRoute(appId: Long = 0L) = "add/$appId"
    }
    object Settings : Screen("settings/{appId}") {
        fun createRoute(appId: Long) = "settings/$appId"
    }
    object Categories : Screen("categories")
    object TranslateConfig : Screen("translate/{appId}") {
        fun createRoute(appId: Long) = "translate/$appId"
    }
    object GlobalSettings : Screen("global_settings")
    object Shortcuts : Screen("shortcuts")
}
