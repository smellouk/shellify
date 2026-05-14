package dev.pwaforge.data.mapper

import dev.pwaforge.data.local.entity.WebAppEntity
import dev.pwaforge.domain.model.EngineType
import dev.pwaforge.domain.model.IconSource
import dev.pwaforge.domain.model.LockType
import dev.pwaforge.domain.model.TranslateLanguage
import dev.pwaforge.domain.model.UserAgentMode
import dev.pwaforge.domain.model.WebApp

fun WebAppEntity.toDomain(): WebApp = WebApp(
    id = id,
    name = name,
    url = url,
    iconSource = iconSource ?: IconSource.fromLegacyPath(iconPath),
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
    libreTranslateUrl = libreTranslateUrl,
    autoTranslateOnLoad = autoTranslateOnLoad,
    uaMode = runCatching { UserAgentMode.valueOf(uaMode) }.getOrDefault(UserAgentMode.CHROME_MOBILE),
    engineType = runCatching { EngineType.valueOf(engineType) }.getOrDefault(EngineType.SYSTEM_WEBVIEW),
    createdAt = createdAt,
    updatedAt = updatedAt,
    lockType = runCatching { LockType.valueOf(lockType) }.getOrDefault(LockType.NONE),
    wipeOnFailedAttempts = wipeOnFailedAttempts,
    hasLauncherShortcut = hasLauncherShortcut,
)

fun WebApp.toEntity(): WebAppEntity = WebAppEntity(
    id = id,
    name = name,
    url = url,
    iconPath = iconPath,  // kept for legacy column; iconSource is the source of truth
    iconSource = iconSource,
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
    libreTranslateUrl = libreTranslateUrl,
    autoTranslateOnLoad = autoTranslateOnLoad,
    uaMode = uaMode.name,
    engineType = engineType.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lockType = lockType.name,
    wipeOnFailedAttempts = wipeOnFailedAttempts,
    hasLauncherShortcut = hasLauncherShortcut,
)
