package io.shellify.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.shellify.app.domain.model.IconSource

@Entity(
    tableName = "web_apps",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index("categoryId"), Index("updatedAt")],
)
data class WebAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val iconPath: String? = null,
    @ColumnInfo(name = "icon_source") val iconSource: IconSource? = null,
    val themeColor: String? = null,
    val backgroundColor: String? = null,
    val description: String? = null,
    val categoryId: Long? = null,
    val isolationId: String,
    // Fullscreen
    val isFullscreen: Boolean = false,
    val fullscreenShowStatusBar: Boolean = false,
    val fullscreenShowNavBar: Boolean = false,
    val fullscreenShowTopToolbar: Boolean = false,
    // Ad blocking
    val adBlockEnabled: Boolean = true,
    val adBlockAllowUserToggle: Boolean = false,
    val adBlockCustomRules: String = "",        // newline-separated
    // Translation
    val translateEnabled: Boolean = false,
    val translateTarget: String = "en",
    val translateEngine: String = "AUTO",
    val showTranslateButton: Boolean = true,
    val autoTranslateOnLoad: Boolean = false,
    val libreTranslateUrl: String = "https://libretranslate.com",
    val libreTranslateApiKey: String = "",
    // Browser
    val uaMode: String = "CHROME_MOBILE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val passwordHash: String? = null,   // unused — kept so DB column exists from migration 3→4
    val lockType: String = "NONE",
    val engineType: String = "SYSTEM_WEBVIEW",
    val wipeOnFailedAttempts: Boolean = false,
    @ColumnInfo(name = "has_launcher_shortcut") val hasLauncherShortcut: Boolean = false,
    @ColumnInfo(name = "show_control_center") val showControlCenter: Boolean = true,
    // Notifications
    @ColumnInfo(name = "notification_permission") val notificationPermission: String = "NOT_ASKED",
    @ColumnInfo(name = "dnd_start_hour") val dndStartHour: Int = -1,
    @ColumnInfo(name = "dnd_end_hour") val dndEndHour: Int = -1,
    @ColumnInfo(name = "background_notifications_enabled") val backgroundNotificationsEnabled: Boolean = false,
    @ColumnInfo(name = "swipe_to_refresh_enabled") val swipeToRefreshEnabled: Boolean = true,
    // Privacy
    @ColumnInfo(name = "stealth_mode") val stealthMode: Boolean = false,
    @ColumnInfo(name = "cookie_auto_wipe") val cookieAutoWipe: Boolean = false,
    @ColumnInfo(name = "always_incognito") val alwaysIncognito: Boolean = false,
    @ColumnInfo(name = "tracker_blocking_enabled") val trackerBlockingEnabled: Boolean = false,
    // Tor
    @ColumnInfo(name = "use_tor") val useTor: Boolean = false,
    @ColumnInfo(name = "preserve_tor_identity") val preserveTorIdentity: Boolean = false,
    // Custom proxy
    @ColumnInfo(name = "custom_proxy_type") val customProxyType: String = "NONE",
    @ColumnInfo(name = "custom_proxy_host") val customProxyHost: String? = null,
    @ColumnInfo(name = "custom_proxy_port") val customProxyPort: Int = 0,
    @ColumnInfo(name = "custom_proxy_username") val customProxyUsername: String? = null,
    @ColumnInfo(name = "custom_proxy_password") val customProxyPassword: String? = null,
)
