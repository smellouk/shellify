package io.shellify.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.iconpack.SimpleIconEntry
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsReader
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.security.verifyPassword
import io.shellify.app.core.shortcut.PwaShortcutManager
import io.shellify.app.core.shortcut.SvgIconRenderer
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.IconSource
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.NotificationChannelId
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.domain.model.TranslateLanguage
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppSettingsCommand {
    data class StartBackgroundService(val appId: Long) : AppSettingsCommand
    data class StopBackgroundService(val appId: Long) : AppSettingsCommand
    data class NavigateToNotificationHistory(val appId: Long) : AppSettingsCommand
}

data class AppSettingsUiState(
    val app: WebApp? = null,
    val isLoading: Boolean = true,
    val deleted: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isFetchingIcon: Boolean = false,
    val iconPackAvailable: Boolean = false,
    val showIconPackPicker: Boolean = false,
    val packIcons: List<SimpleIconEntry> = emptyList(),
    val iconPickerQuery: String = "",
    val isSelectingPackIcon: Boolean = false,
    val showDisableLockDialog: Boolean = false,
    val disableLockError: Boolean = false,
    val hasPassword: Boolean = false,
    val globalNotificationsEnabled: Boolean = false,
)

class AppSettingsViewModel(
    private val appId: Long,
    private val getWebAppById: GetWebAppByIdUseCase,
    private val saveWebApp: SaveWebAppUseCase,
    private val deleteWebApp: DeleteWebAppUseCase,
    private val isolationManager: IsolationManager,
    private val context: Context,
    private val analyzer: PwaAnalyzer,
    private val faviconFetcher: FaviconFetcher,
    private val simpleIconsManager: SimpleIconsManager,
    private val passwordManager: PasswordManager,
    val geckoEngineManager: GeckoEngineManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppSettingsUiState(
            iconPackAvailable = simpleIconsManager.state.value is SimpleIconsState.Imported,
        )
    )
    val uiState: StateFlow<AppSettingsUiState> = _state

    private val _commands = MutableSharedFlow<AppSettingsCommand>(extraBufferCapacity = 1)
    val commands: SharedFlow<AppSettingsCommand> = _commands

    init {
        viewModelScope.launch {
            val app = getWebAppById(appId)
            _state.update { it.copy(app = app, isLoading = false) }
        }
        viewModelScope.launch {
            passwordManager.passwordHash.collect { hash ->
                val hasPassword = hash != null
                _state.update { s ->
                    s.copy(
                        hasPassword = hasPassword,
                        app = if (!hasPassword) s.app?.copy(lockType = LockType.NONE) else s.app,
                    )
                }
            }
        }
        _state.update {
            it.copy(globalNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled())
        }
    }

    fun refreshGlobalNotificationState() {
        _state.update {
            it.copy(globalNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled())
        }
    }

    fun update(transform: (WebApp) -> WebApp) {
        val app = _state.value.app ?: return
        val updated = transform(app)
        _state.update { it.copy(app = updated) }
        viewModelScope.launch { saveWebApp(updated) }
    }

    fun setName(name: String) = update { it.copy(name = name) }
    fun setUrl(url: String) = update { it.copy(url = url) }
    fun setThemeColor(color: String?) {
        update { it.copy(themeColor = color) }
        val app = _state.value.app ?: return
        val src = app.iconSource
        if (src is IconSource.SvgIcon && color != null) {
            val bgColorArgb =
                runCatching { android.graphics.Color.parseColor(color) }.getOrNull() ?: return
            reRenderSvgIcon(src.slug, bgColorArgb)
        }
    }

    fun setIconPath(path: String) = update { it.copy(iconSource = IconSource.Path(path)) }

    fun fetchIcon() {
        val app = _state.value.app ?: return
        val url = app.url.trim().let { if (!it.startsWith("http")) "https://$it" else it }
        if (url.isBlank()) return
        _state.update { it.copy(isFetchingIcon = true) }
        viewModelScope.launch {
            val iconUrl = runCatching { analyzer.analyze(url).bestIconUrl(url) }.getOrNull()
            val path = faviconFetcher.fetch(iconUrl, url, app.isolationId)
            val newSource = path?.let { IconSource.Path(it) } ?: app.iconSource
            _state.update { it.copy(isFetchingIcon = false) }
            update { it.copy(iconSource = newSource) }
        }
    }

    fun toggleFullscreen() = update { it.copy(isFullscreen = !it.isFullscreen) }
    fun toggleAdBlock() = update { it.copy(adBlockEnabled = !it.adBlockEnabled) }
    fun toggleTranslate() = update { it.copy(translateEnabled = !it.translateEnabled) }
    fun toggleControlCenter() = update { it.copy(showControlCenter = !it.showControlCenter) }
    fun setTranslateTarget(lang: TranslateLanguage) =
        update { it.copy(translateTarget = lang) }

    fun setLockType(v: LockType) = update { it.copy(lockType = v) }

    fun requestDisableLock() =
        _state.update { it.copy(showDisableLockDialog = true, disableLockError = false) }

    fun dismissDisableLockDialog() =
        _state.update { it.copy(showDisableLockDialog = false, disableLockError = false) }

    fun confirmDisableLock(password: String) {
        viewModelScope.launch {
            val hash = passwordManager.passwordHash.first()
            if (hash != null && verifyPassword(password, hash)) {
                _state.update { it.copy(showDisableLockDialog = false, disableLockError = false) }
                update { it.copy(lockType = LockType.NONE) }
            } else {
                _state.update { it.copy(disableLockError = true) }
            }
        }
    }

    fun setEngineType(engine: EngineType) = update { it.copy(engineType = engine) }

    fun setWipeOnFailedAttempts(v: Boolean) = update { it.copy(wipeOnFailedAttempts = v) }
    fun markShortcutCreated(app: WebApp) = update { it.copy(hasLauncherShortcut = true) }

    fun clearData() {
        val app = _state.value.app ?: return
        viewModelScope.launch { isolationManager.clearData(app.isolationId) }
    }

    fun showDeleteDialog() = _state.update { it.copy(showDeleteDialog = true) }
    fun dismissDeleteDialog() = _state.update { it.copy(showDeleteDialog = false) }

    fun deleteApp() {
        val app = _state.value.app ?: return
        viewModelScope.launch {
            PwaShortcutManager.removeShortcut(context, app)
            isolationManager.clearData(app.isolationId)
            deleteWebApp(app)
            androidx.core.app.NotificationManagerCompat.from(context)
                .deleteNotificationChannel(NotificationChannelId.forApp(app.isolationId))
            _state.update { it.copy(deleted = true) }
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    fun toggleNotificationPermission() = update {
        it.copy(
            notificationPermission = if (it.notificationPermission == NotificationPermission.GRANTED) {
                NotificationPermission.DENIED
            } else {
                NotificationPermission.GRANTED
            }
        )
    }

    fun setDndStartHour(hour: Int) = update { it.copy(dndStartHour = hour) }

    fun setDndEndHour(hour: Int) = update { it.copy(dndEndHour = hour) }

    fun clearDndSchedule() = update { it.copy(dndStartHour = -1, dndEndHour = -1) }

    fun toggleBackgroundNotifications() {
        val current = _state.value.app ?: return
        val newValue = !current.backgroundNotificationsEnabled
        update { it.copy(backgroundNotificationsEnabled = newValue) }
        viewModelScope.launch {
            _commands.tryEmit(
                if (newValue) AppSettingsCommand.StartBackgroundService(current.id)
                else AppSettingsCommand.StopBackgroundService(current.id)
            )
        }
    }

    fun onNotificationHistoryClick() {
        val current = _state.value.app ?: return
        viewModelScope.launch {
            _commands.tryEmit(AppSettingsCommand.NavigateToNotificationHistory(current.id))
        }
    }

    // ── Icon pack picker ──────────────────────────────────────────────────────

    fun openIconPackPicker() {
        viewModelScope.launch {
            val icons = SimpleIconsReader(context).readAll()
            _state.update {
                it.copy(
                    showIconPackPicker = true,
                    packIcons = icons,
                    iconPickerQuery = ""
                )
            }
        }
    }

    fun closeIconPackPicker() = _state.update { it.copy(showIconPackPicker = false) }

    fun setIconPickerQuery(q: String) = _state.update { it.copy(iconPickerQuery = q) }

    fun selectPackIcon(entry: SimpleIconEntry, bgColorArgb: Int) {
        val app = _state.value.app ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSelectingPackIcon = true) }
            val effectiveBg = app.themeColor
                ?.let { runCatching { android.graphics.Color.parseColor(it) }.getOrNull() }
                ?: bgColorArgb
            val iconSource = SvgIconRenderer.render(
                context = context,
                slug = entry.slug,
                bgColorArgb = effectiveBg,
                isolationId = app.isolationId,
                existingIconPath = app.iconPath,
            )
            if (iconSource != null) {
                update { it.copy(iconSource = iconSource) }
            }
            _state.update { it.copy(isSelectingPackIcon = false, showIconPackPicker = false) }
        }
    }

    private fun reRenderSvgIcon(slug: String, bgColorArgb: Int) {
        val app = _state.value.app ?: return
        viewModelScope.launch {
            val iconSource = SvgIconRenderer.render(
                context = context,
                slug = slug,
                bgColorArgb = bgColorArgb,
                isolationId = app.isolationId,
                existingIconPath = app.iconPath,
            ) ?: return@launch
            update { it.copy(iconSource = iconSource) }
        }
    }
}
