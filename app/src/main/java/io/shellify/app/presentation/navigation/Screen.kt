package io.shellify.app.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Add : Screen("add/{appId}?url={url}&name={name}") {
        fun createRoute(appId: Long = 0L, url: String = "", name: String = "") =
            "add/$appId?url=${android.net.Uri.encode(url)}&name=${android.net.Uri.encode(name)}"
    }

    object Settings : Screen("settings/{appId}") {
        fun createRoute(appId: Long) = "settings/$appId"
    }

    object Categories : Screen("categories")
    object TranslateConfig : Screen("translate/{appId}") {
        fun createRoute(appId: Long) = "translate/$appId"
    }

    object Consent : Screen("consent")
    object UpdateConsent : Screen("update_consent")
    object WhatsNew : Screen("whats_new")
    object GlobalSettings : Screen("global_settings")
    object Licenses : Screen("licenses")
    object Shortcuts : Screen("shortcuts")
    object Onboarding : Screen("onboarding")
    object NotificationHistory : Screen("notification_history/{appId}") {
        fun createRoute(appId: Long) = "notification_history/$appId"
    }

    object NetworkLog : Screen("network_log/{appId}") {
        fun createRoute(appId: Long) = "network_log/$appId"
    }
}
