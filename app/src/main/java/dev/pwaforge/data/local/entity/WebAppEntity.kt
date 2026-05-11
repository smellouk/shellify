package dev.pwaforge.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    // Browser
    val uaMode: String = "CHROME_MOBILE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
