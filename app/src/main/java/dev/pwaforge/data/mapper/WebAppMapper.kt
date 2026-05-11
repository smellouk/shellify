package dev.pwaforge.data.mapper

import dev.pwaforge.data.local.entity.WebAppEntity
import dev.pwaforge.domain.model.TranslateEngine
import dev.pwaforge.domain.model.TranslateLanguage
import dev.pwaforge.domain.model.UserAgentMode
import dev.pwaforge.domain.model.WebApp

fun WebAppEntity.toDomain(): WebApp = WebApp(
    id = id,
    name = name,
    url = url,
    iconPath = iconPath,
    themeColor = themeColor,
    backgroundColor = backgroundColor,
    description = description,
    categoryId = categoryId,
    isolationId = isolationId,
    isFullscreen = isFullscreen,
    fullscreenShowStatusBar = fullscreenShowStatusBar,
    fullscreenShowNavBar = fullscreenShowNavBar,
    fullscreenShowTopToolbar = fullscreenShowTopToolbar,
    adBlockEnabled = adBlockEnabled,
    adBlockAllowUserToggle = adBlockAllowUserToggle,
    adBlockCustomRules = adBlockCustomRules.split("\n").filter { it.isNotBlank() },
    translateEnabled = translateEnabled,
    translateTarget = TranslateLanguage.entries.find { it.code == translateTarget } ?: TranslateLanguage.ENGLISH,
    translateEngine = runCatching { TranslateEngine.valueOf(translateEngine) }.getOrDefault(TranslateEngine.AUTO),
    showTranslateButton = showTranslateButton,
    autoTranslateOnLoad = autoTranslateOnLoad,
    uaMode = runCatching { UserAgentMode.valueOf(uaMode) }.getOrDefault(UserAgentMode.CHROME_MOBILE),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun WebApp.toEntity(): WebAppEntity = WebAppEntity(
    id = id,
    name = name,
    url = url,
    iconPath = iconPath,
    themeColor = themeColor,
    backgroundColor = backgroundColor,
    description = description,
    categoryId = categoryId,
    isolationId = isolationId,
    isFullscreen = isFullscreen,
    fullscreenShowStatusBar = fullscreenShowStatusBar,
    fullscreenShowNavBar = fullscreenShowNavBar,
    fullscreenShowTopToolbar = fullscreenShowTopToolbar,
    adBlockEnabled = adBlockEnabled,
    adBlockAllowUserToggle = adBlockAllowUserToggle,
    adBlockCustomRules = adBlockCustomRules.joinToString("\n"),
    translateEnabled = translateEnabled,
    translateTarget = translateTarget.code,
    translateEngine = translateEngine.name,
    showTranslateButton = showTranslateButton,
    autoTranslateOnLoad = autoTranslateOnLoad,
    uaMode = uaMode.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
