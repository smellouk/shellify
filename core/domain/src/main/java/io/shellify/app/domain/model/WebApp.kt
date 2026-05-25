package io.shellify.app.domain.model

import java.util.UUID

data class WebApp(
    val id: Long = 0,
    val name: String,
    val url: String,
    val iconSource: IconSource? = null,
    val themeColor: String? = null,
    val backgroundColor: String? = null,
    val description: String? = null,
    val categoryId: Long? = null,
    /** Stable UUID used as the WebView profile name and isolation key. Never changes after creation. */
    val isolationId: String = UUID.randomUUID().toString(),
    // Fullscreen
    val isFullscreen: Boolean = false,
    val fullscreenShowStatusBar: Boolean = false,
    val fullscreenShowNavBar: Boolean = false,
    val fullscreenShowTopToolbar: Boolean = false,
    // Ad blocking
    val adBlockEnabled: Boolean = true,
    val adBlockAllowUserToggle: Boolean = false,
    val adBlockCustomRules: List<String> = emptyList(),
    // Translation
    val translateEnabled: Boolean = false,
    val translateTarget: TranslateLanguage = TranslateLanguage.ENGLISH,
    val autoTranslateOnLoad: Boolean = true,
    // Browser
    val uaMode: UserAgentMode = UserAgentMode.CHROME_MOBILE,
    val engineType: EngineType = EngineType.SYSTEM_WEBVIEW,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Security
    val lockType: LockType = LockType.NONE,
    val wipeOnFailedAttempts: Boolean = false,
    // Launcher shortcut
    val hasLauncherShortcut: Boolean = false,
    // Control center
    val showControlCenter: Boolean = true,
    // Notifications
    val notificationPermission: NotificationPermission = NotificationPermission.NOT_ASKED,
    val dndStartHour: Int = -1,
    val dndEndHour: Int = -1,
    val backgroundNotificationsEnabled: Boolean = false,
    val swipeToRefreshEnabled: Boolean = true,
    // Privacy
    val stealthMode: Boolean = false,
    val cookieAutoWipe: Boolean = false,
    val alwaysIncognito: Boolean = false,
    val trackerBlockingEnabled: Boolean = false,
    // Tor
    val useTor: Boolean = false,
    val preserveTorIdentity: Boolean = false,
) {
    /** On-disk path: Path → path, SvgIcon → renderedPath, else null. */
    val iconPath: String?
        get() = when (val s = iconSource) {
            is IconSource.Path -> s.path
            is IconSource.SvgIcon -> s.renderedPath
            null -> null
        }
}

enum class LockType { NONE, PASSWORD, SYSTEM }

enum class UserAgentMode(val label: String, val uaString: String?) {
    DEFAULT("System default", null),
    CHROME_MOBILE(
        "Chrome (mobile)",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    ),
    CHROME_DESKTOP(
        "Chrome (desktop)",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    ),
    SAFARI_MOBILE(
        "Safari (mobile)",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
    ),
}

enum class TranslateLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    CHINESE("zh", "Chinese (Simplified)"),
    SPANISH("es", "Spanish"),
    FRENCH("fr", "French"),
    GERMAN("de", "German"),
    JAPANESE("ja", "Japanese"),
    KOREAN("ko", "Korean"),
    PORTUGUESE("pt", "Portuguese"),
    RUSSIAN("ru", "Russian"),
    ARABIC("ar", "Arabic"),
    HINDI("hi", "Hindi"),
    ITALIAN("it", "Italian"),
    DUTCH("nl", "Dutch"),
    TURKISH("tr", "Turkish"),
    POLISH("pl", "Polish"),
    VIETNAMESE("vi", "Vietnamese"),
    THAI("th", "Thai"),
    INDONESIAN("id", "Indonesian"),
}
