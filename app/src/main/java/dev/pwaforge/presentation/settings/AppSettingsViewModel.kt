package dev.pwaforge.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pwaforge.core.iconpack.SimpleIconEntry
import dev.pwaforge.core.iconpack.SimpleIconsManager
import dev.pwaforge.core.iconpack.SimpleIconsReader
import dev.pwaforge.core.iconpack.SimpleIconsState
import dev.pwaforge.core.isolation.IsolationManager
import dev.pwaforge.core.pwa.FaviconFetcher
import dev.pwaforge.core.pwa.PwaAnalyzer
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.core.shortcut.SvgIconRenderer
import dev.pwaforge.domain.model.IconSource
import dev.pwaforge.domain.model.LockType
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.domain.repository.WebAppRepository
import dev.pwaforge.domain.usecase.DeleteWebAppUseCase
import dev.pwaforge.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
)

class AppSettingsViewModel(
    private val appId: Long,
    private val repo: WebAppRepository,
    private val saveWebApp: SaveWebAppUseCase,
    private val deleteWebApp: DeleteWebAppUseCase,
    private val isolationManager: IsolationManager,
    private val context: Context,
    private val analyzer: PwaAnalyzer,
    private val faviconFetcher: FaviconFetcher,
    private val simpleIconsManager: SimpleIconsManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AppSettingsUiState(
        iconPackAvailable = simpleIconsManager.state.value is SimpleIconsState.Imported,
    ))
    val uiState: StateFlow<AppSettingsUiState> = _state

    init {
        viewModelScope.launch {
            val app = repo.getById(appId)
            _state.update { it.copy(app = app, isLoading = false) }
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
            val bgColorArgb = runCatching { android.graphics.Color.parseColor(color) }.getOrNull() ?: return
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
    fun setTranslateTarget(lang: dev.pwaforge.domain.model.TranslateLanguage) = update { it.copy(translateTarget = lang) }
    fun setLockType(v: LockType) = update { it.copy(lockType = v) }
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
            _state.update { it.copy(deleted = true) }
        }
    }

    // ── Icon pack picker ──────────────────────────────────────────────────────

    fun openIconPackPicker() {
        viewModelScope.launch {
            val icons = SimpleIconsReader(context).readAll()
            _state.update { it.copy(showIconPackPicker = true, packIcons = icons, iconPickerQuery = "") }
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
