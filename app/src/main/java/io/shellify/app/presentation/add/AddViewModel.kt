package io.shellify.app.presentation.add

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import io.shellify.app.core.shortcut.SvgIconRenderer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.iconpack.SimpleIconEntry
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsReader
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.domain.model.EngineType
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.domain.model.PwaManifest
import io.shellify.app.domain.model.TranslateLanguage
import io.shellify.app.domain.model.IconSource
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.repository.WebAppRepository
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class AddUiState(
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isFetchingIcon: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val url: String = "",
    val iconPath: String? = null,
    val iconSource: IconSource? = null,
    val themeColor: String? = null,
    val categoryId: Long? = null,
    // Isolation ID — stable for the lifetime of this edit session
    val isolationId: String = UUID.randomUUID().toString(),
    // Analysis report waiting for user approval
    val pendingManifest: PwaManifest? = null,
    val pendingIconPath: String? = null,    // icon fetched during analysis, not yet applied
    // Fullscreen
    val isFullscreen: Boolean = false,
    val fullscreenShowStatusBar: Boolean = false,
    val fullscreenShowNavBar: Boolean = false,
    val fullscreenShowTopToolbar: Boolean = false,
    // Ad blocking
    val adBlockEnabled: Boolean = true,
    val adBlockAllowUserToggle: Boolean = false,
    val adBlockCustomRules: List<String> = emptyList(),
    val adBlockCustomRuleInput: String = "",
    // Translation
    val translateEnabled: Boolean = false,
    val translateTarget: TranslateLanguage = TranslateLanguage.ENGLISH,
    val autoTranslateOnLoad: Boolean = true,
    // Browser
    val uaMode: UserAgentMode = UserAgentMode.CHROME_MOBILE,
    val engineType: EngineType = EngineType.SYSTEM_WEBVIEW,
    // Security
    val lockType: LockType = LockType.NONE,
    val wipeOnFailedAttempts: Boolean = false,
    val analyzeError: String? = null,
    val urlError: String? = null,
    val nameError: String? = null,
    val duplicateError: String? = null,
    val saved: Boolean = false,
    // Non-null after save-and-run — signals screen to launch WebViewActivity
    val launchAppId: Long? = null,
    // Icon pack picker
    val showIconPackPicker: Boolean = false,
    val iconPackAvailable: Boolean = false,
    val packIcons: List<SimpleIconEntry> = emptyList(),
    val iconPickerQuery: String = "",
    val isSelectingPackIcon: Boolean = false,
)

class AddViewModel(
    private val appId: Long,
    private val repo: WebAppRepository,
    private val saveWebApp: SaveWebAppUseCase,
    getCategories: GetCategoriesUseCase,
    private val analyzer: PwaAnalyzer,
    private val faviconFetcher: FaviconFetcher,
    val geckoEngineManager: GeckoEngineManager,
    private val themeManager: ThemeManager,
    private val simpleIconsManager: SimpleIconsManager,
    private val context: Context,
    private val prefilledUrl: String = "",
    private val prefilledName: String = "",
) : ViewModel() {

    private val _state = MutableStateFlow(AddUiState(
        isLoading = appId != 0L,
        iconPackAvailable = simpleIconsManager.state.value is SimpleIconsState.Imported,
    ))
    val uiState: StateFlow<AddUiState> = _state

    private var originalApp: WebApp? = null

    init {
        if (appId != 0L) {
            viewModelScope.launch {
                val app = repo.getById(appId) ?: return@launch
                originalApp = app
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = app.name,
                        url = app.url,
                        iconPath = app.iconPath,
                        iconSource = app.iconSource,
                        themeColor = app.themeColor,
                        categoryId = app.categoryId,
                        isolationId = app.isolationId,
                        isFullscreen = app.isFullscreen,
                        fullscreenShowStatusBar = app.fullscreenShowStatusBar,
                        fullscreenShowNavBar = app.fullscreenShowNavBar,
                        fullscreenShowTopToolbar = app.fullscreenShowTopToolbar,
                        adBlockEnabled = app.adBlockEnabled,
                        adBlockAllowUserToggle = app.adBlockAllowUserToggle,
                        adBlockCustomRules = app.adBlockCustomRules,
                        translateEnabled = app.translateEnabled,
                        translateTarget = app.translateTarget,
                        autoTranslateOnLoad = app.autoTranslateOnLoad,
                        uaMode = app.uaMode,
                        engineType = app.engineType,
                        lockType = app.lockType,
                        wipeOnFailedAttempts = app.wipeOnFailedAttempts,
                    )
                }
            }
        } else if (prefilledUrl.isNotBlank()) {
            _state.update { it.copy(url = prefilledUrl, name = prefilledName) }
            viewModelScope.launch {
                val defaultEngine = themeManager.defaultEngineType.first()
                _state.update { it.copy(engineType = defaultEngine) }
            }
        } else {
            viewModelScope.launch {
                val defaultEngine = themeManager.defaultEngineType.first()
                _state.update { it.copy(engineType = defaultEngine) }
            }
        }
    }

    // Basic info
    fun setName(v: String) = _state.update { it.copy(name = v, nameError = null, duplicateError = null) }
    fun setUrl(v: String) = _state.update { it.copy(url = v, urlError = null) }
    fun setThemeColor(v: String?) {
        _state.update { it.copy(themeColor = v) }
        val src = _state.value.iconSource
        if (src is IconSource.SvgIcon && v != null) {
            val bgColorArgb = runCatching { android.graphics.Color.parseColor(v) }.getOrNull() ?: return
            reRenderSvgIcon(src.slug, bgColorArgb)
        }
    }
    fun setIconPath(v: String) = _state.update { it.copy(iconPath = v, iconSource = IconSource.Path(v)) }

    // Fullscreen
    fun setFullscreen(v: Boolean) = _state.update { it.copy(isFullscreen = v) }
    fun setFullscreenShowStatusBar(v: Boolean) = _state.update { it.copy(fullscreenShowStatusBar = v) }
    fun setFullscreenShowNavBar(v: Boolean) = _state.update { it.copy(fullscreenShowNavBar = v) }
    fun setFullscreenShowTopToolbar(v: Boolean) = _state.update { it.copy(fullscreenShowTopToolbar = v) }

    // Ad blocking
    fun setAdBlock(v: Boolean) = _state.update { it.copy(adBlockEnabled = v) }
    fun setAdBlockAllowUserToggle(v: Boolean) = _state.update { it.copy(adBlockAllowUserToggle = v) }
    fun setAdBlockCustomRuleInput(v: String) = _state.update { it.copy(adBlockCustomRuleInput = v) }
    fun addAdBlockCustomRule() {
        val rule = _state.value.adBlockCustomRuleInput.trim()
        if (rule.isBlank() || rule in _state.value.adBlockCustomRules) return
        _state.update { it.copy(adBlockCustomRules = it.adBlockCustomRules + rule, adBlockCustomRuleInput = "") }
    }
    fun removeAdBlockCustomRule(rule: String) = _state.update { it.copy(adBlockCustomRules = it.adBlockCustomRules - rule) }

    // Translation
    fun setTranslate(v: Boolean) = _state.update { it.copy(translateEnabled = v) }
    fun setTranslateTarget(v: TranslateLanguage) = _state.update { it.copy(translateTarget = v) }
    fun setAutoTranslateOnLoad(v: Boolean) = _state.update { it.copy(autoTranslateOnLoad = v) }

    // Browser
    fun setUaMode(v: UserAgentMode) = _state.update { it.copy(uaMode = v) }
    fun setEngineType(v: EngineType) = _state.update { it.copy(engineType = v) }

    // Security
    fun setLockType(v: LockType) = _state.update { it.copy(lockType = v) }
    fun setWipeOnFailedAttempts(v: Boolean) = _state.update { it.copy(wipeOnFailedAttempts = v) }

    // ── Analysis ──────────────────────────────────────────────────────────────

    /** Fetches only the icon for the current URL without running full PWA analysis. */
    fun fetchIcon() {
        val url = _state.value.url.trim().let { if (!it.startsWith("http")) "https://$it" else it }
        if (url.isBlank()) { _state.update { it.copy(urlError = "Enter a URL first") }; return }
        _state.update { it.copy(isFetchingIcon = true) }
        viewModelScope.launch {
            val iconUrl = runCatching { analyzer.analyze(url).bestIconUrl(url) }.getOrNull()
            val path = faviconFetcher.fetch(iconUrl, url, _state.value.isolationId)
            val resolvedPath = path ?: _state.value.iconPath
            _state.update {
                it.copy(
                    isFetchingIcon = false,
                    iconPath = resolvedPath,
                    iconSource = resolvedPath?.let { p -> IconSource.Path(p) } ?: it.iconSource,
                )
            }
        }
    }

    fun analyze() {
        val rawUrl = _state.value.url.trim().let { if (!it.startsWith("http")) "https://$it" else it }
        if (rawUrl.isBlank()) { _state.update { it.copy(urlError = "Please enter a URL") }; return }
        _state.update { it.copy(isAnalyzing = true, analyzeError = null, url = rawUrl) }
        viewModelScope.launch {
            val manifest = runCatching { analyzer.analyze(rawUrl) }.getOrNull()
            val iconUrl = manifest?.bestIconUrl(rawUrl)
            val iconPath = faviconFetcher.fetch(iconUrl, rawUrl, _state.value.isolationId)
            if (manifest != null) {
                _state.update { it.copy(isAnalyzing = false, pendingManifest = manifest, pendingIconPath = iconPath) }
            } else {
                _state.update { it.copy(isAnalyzing = false, analyzeError = "Could not read site info. You can still save manually.", pendingIconPath = iconPath) }
            }
        }
    }

    /** Apply all detected fields from the analysis report to the form. */
    fun applyManifest() {
        val s = _state.value
        val m = s.pendingManifest ?: return
        val resolvedPath = s.pendingIconPath ?: _state.value.iconPath
        _state.update {
            it.copy(
                name = it.name.ifBlank { m.shortName ?: m.name ?: it.name },
                themeColor = m.themeColor ?: it.themeColor,
                iconPath = resolvedPath,
                iconSource = resolvedPath?.let { p -> IconSource.Path(p) } ?: it.iconSource,
                pendingManifest = null,
                pendingIconPath = null,
            )
        }
    }

    fun dismissManifest() = _state.update { it.copy(pendingManifest = null, pendingIconPath = null) }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validate(): String? {
        val s = _state.value
        val url = s.url.trim()
        if (url.isBlank()) {
            _state.update { it.copy(urlError = "Please enter a URL") }
            return null
        }
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = "Please enter a name") }
            return null
        }
        return if (url.startsWith("http")) url else "https://$url"
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save(onCreateShortcut: ((WebApp) -> Unit)? = null) {
        val url = validate() ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (isDuplicate()) return@launch
            val savedId = persistApp(url)
            val savedApp = repo.getById(savedId) ?: buildApp(url).copy(id = savedId)
            if (appId == 0L) onCreateShortcut?.invoke(savedApp)
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }

    fun run() {
        val url = validate() ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (isDuplicate()) return@launch
            val savedId = persistApp(url)
            _state.update { it.copy(isSaving = false, launchAppId = savedId) }
        }
    }

    private suspend fun isDuplicate(): Boolean {
        if (appId != 0L) return false
        val name = _state.value.name.trim()
        val existing = repo.getByName(name) ?: return false
        _state.update { it.copy(isSaving = false, duplicateError = "\"${existing.name}\" already exists") }
        return true
    }

    fun onLaunched() = _state.update { it.copy(launchAppId = null) }

    fun markShortcutCreated(app: WebApp) = viewModelScope.launch {
        repo.save(app.copy(hasLauncherShortcut = true))
    }

    // ── Icon pack picker ──────────────────────────────────────────────────────

    fun openIconPackPicker() {
        viewModelScope.launch {
            val reader = SimpleIconsReader(context)
            val icons = reader.readAll()
            _state.update { it.copy(showIconPackPicker = true, packIcons = icons, iconPickerQuery = "") }
        }
    }

    fun closeIconPackPicker() = _state.update { it.copy(showIconPackPicker = false) }

    fun setIconPickerQuery(q: String) = _state.update { it.copy(iconPickerQuery = q) }

    fun selectPackIcon(entry: SimpleIconEntry, isolationId: String, bgColorArgb: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isSelectingPackIcon = true) }
            val themeColor = _state.value.themeColor
            val effectiveBg = themeColor
                ?.let { runCatching { android.graphics.Color.parseColor(it) }.getOrNull() }
                ?: bgColorArgb
            reRenderSvgIconInternal(entry.slug, effectiveBg, isSelection = true)
            _state.update { it.copy(isSelectingPackIcon = false, showIconPackPicker = false) }
        }
    }

    private fun reRenderSvgIcon(slug: String, bgColorArgb: Int) {
        viewModelScope.launch { reRenderSvgIconInternal(slug, bgColorArgb, isSelection = false) }
    }

    private suspend fun reRenderSvgIconInternal(slug: String, bgColorArgb: Int, isSelection: Boolean) {
        val iconSource = SvgIconRenderer.render(
            context = context,
            slug = slug,
            bgColorArgb = bgColorArgb,
            isolationId = _state.value.isolationId,
            existingIconPath = _state.value.iconPath,
        ) ?: return
        _state.update { it.copy(iconPath = iconSource.renderedPath, iconSource = iconSource) }
    }

    private suspend fun persistApp(url: String): Long {
        val app = buildApp(url)
        return saveWebApp(app)
    }

    private fun buildApp(url: String): WebApp {
        val s = _state.value
        return (originalApp ?: WebApp(name = "", url = "", isolationId = s.isolationId)).copy(
            id = appId,
            name = s.name.trim(),
            url = url,
            iconSource = s.iconSource,
            themeColor = s.themeColor,
            categoryId = s.categoryId,
            isolationId = s.isolationId,
            isFullscreen = s.isFullscreen,
            fullscreenShowStatusBar = s.fullscreenShowStatusBar,
            fullscreenShowNavBar = s.fullscreenShowNavBar,
            fullscreenShowTopToolbar = s.fullscreenShowTopToolbar,
            adBlockEnabled = s.adBlockEnabled,
            adBlockAllowUserToggle = s.adBlockAllowUserToggle,
            adBlockCustomRules = s.adBlockCustomRules,
            translateEnabled = s.translateEnabled,
            translateTarget = s.translateTarget,
            autoTranslateOnLoad = s.autoTranslateOnLoad,
            uaMode = s.uaMode,
            engineType = s.engineType,
            lockType = s.lockType,
            wipeOnFailedAttempts = s.wipeOnFailedAttempts,
        )
    }
}
