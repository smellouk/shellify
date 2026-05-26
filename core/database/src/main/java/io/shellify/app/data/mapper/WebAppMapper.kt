package io.shellify.app.data.mapper

import io.shellify.app.data.local.entity.WebAppEntity
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.IconSource
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.domain.model.ProxyType
import io.shellify.app.domain.model.TranslateLanguage
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.model.WebApp

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
    translateTarget = TranslateLanguage.entries.find { it.code == translateTarget }
        ?: TranslateLanguage.ENGLISH,
    autoTranslateOnLoad = autoTranslateOnLoad,
    uaMode = runCatching { UserAgentMode.valueOf(uaMode) }.getOrDefault(UserAgentMode.CHROME_MOBILE),
    engineType = runCatching { EngineType.valueOf(engineType) }.getOrDefault(EngineType.SYSTEM_WEBVIEW),
    createdAt = createdAt,
    updatedAt = updatedAt,
    lockType = runCatching { LockType.valueOf(lockType) }.getOrDefault(LockType.NONE),
    wipeOnFailedAttempts = wipeOnFailedAttempts,
    hasLauncherShortcut = hasLauncherShortcut,
    showControlCenter = showControlCenter,
    notificationPermission = runCatching {
        NotificationPermission.valueOf(notificationPermission)
    }.getOrDefault(NotificationPermission.NOT_ASKED),
    dndStartHour = dndStartHour,
    dndEndHour = dndEndHour,
    backgroundNotificationsEnabled = backgroundNotificationsEnabled,
    swipeToRefreshEnabled = swipeToRefreshEnabled,
    stealthMode = stealthMode,
    cookieAutoWipe = cookieAutoWipe,
    alwaysIncognito = alwaysIncognito,
    trackerBlockingEnabled = trackerBlockingEnabled,
    useTor = useTor,
    preserveTorIdentity = preserveTorIdentity,
    customProxyType = runCatching { ProxyType.valueOf(customProxyType) }.getOrDefault(ProxyType.NONE),
    customProxyHost = customProxyHost,
    customProxyPort = customProxyPort,
    customProxyUsername = customProxyUsername,
    customProxyPassword = customProxyPassword,
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
    autoTranslateOnLoad = autoTranslateOnLoad,
    uaMode = uaMode.name,
    engineType = engineType.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lockType = lockType.name,
    wipeOnFailedAttempts = wipeOnFailedAttempts,
    hasLauncherShortcut = hasLauncherShortcut,
    showControlCenter = showControlCenter,
    notificationPermission = notificationPermission.name,
    dndStartHour = dndStartHour,
    dndEndHour = dndEndHour,
    backgroundNotificationsEnabled = backgroundNotificationsEnabled,
    swipeToRefreshEnabled = swipeToRefreshEnabled,
    stealthMode = stealthMode,
    cookieAutoWipe = cookieAutoWipe,
    alwaysIncognito = alwaysIncognito,
    trackerBlockingEnabled = trackerBlockingEnabled,
    useTor = useTor,
    preserveTorIdentity = preserveTorIdentity,
    customProxyType = customProxyType.name,
    customProxyHost = customProxyHost,
    customProxyPort = customProxyPort,
    customProxyUsername = customProxyUsername,
    customProxyPassword = customProxyPassword,
)
