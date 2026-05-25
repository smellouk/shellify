package io.shellify.app.presentation.add

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.shellify.app.core.shortcut.SvgIconRenderer
import io.shellify.core.ui.R
import io.shellify.feature.add.R as AddR
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.iconpack.SimpleIconEntry
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsReader
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.domain.model.EngineType
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.domain.model.PwaManifest
import io.shellify.app.domain.model.TranslateLanguage
import io.shellify.app.domain.model.IconSource
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.GetWebAppByNameUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
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
    // Notifications
    val notificationsEnabled: Boolean = true,
    val globalNotificationsEnabled: Boolean = false,
    // Security
    val hasPassword: Boolean = false,
    val lockType: LockType = LockType.NONE,
    val wipeOnFailedAttempts: Boolean = false,
    val analyzeError: String? = null,
    val urlError: String? = null,
    val nameError: String? = null,
    val duplicateError: String? = null,
    val saved: Boolean = false,
    // Non-null after save-and-run — signals screen to launch WebViewActivity
    val previewUrl: String? = null,
    // Icon pack picker
    val showIconPackPicker: Boolean = false,
    val iconPackAvailable: Boolean = false,
    val packIcons: List<SimpleIconEntry> = emptyList(),
    val iconPickerQuery: String = "",
    val isSelectingPackIcon: Boolean = false,
)

class AddViewModel(
    private val appId: Long,
    private val getWebAppById: GetWebAppByIdUseCase,
    private val getWebAppByName: GetWebAppByNameUseCase,
    private val saveWebApp: SaveWebAppUseCase,
    getCategories: GetCategoriesUseCase,
    private val analyzer: PwaAnalyzer,
    private val faviconFetcher: FaviconFetcher,
    val geckoEngineManager: GeckoEngineManager,
    private val themeManager: ThemeManager,
    private val simpleIconsManager: SimpleIconsManager,
    private val context: Context,
    private val passwordManager: PasswordManager,
    private val prefilledUrl: String = "",
    private val prefilledName: String = "",
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddUiState(
            isLoading = appId != 0L,
            iconPackAvailable = simpleIconsManager.state.value is SimpleIconsState.Imported,
        )
    )
    val uiState: StateFlow<AddUiState> = _state

    private var originalApp: WebApp? = null

    init {
        if (appId != 0L) {
            viewModelScope.launch {
                val app = getWebAppById(appId) ?: return@launch
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
                        notificationsEnabled = app.notificationPermission != NotificationPermission.DENIED,
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
        viewModelScope.launch {
            passwordManager.passwordHash.collect { hash ->
                val hasPassword = hash != null
                _state.update { s ->
                    s.copy(
                        hasPassword = hasPassword,
                        lockType = if (!hasPassword) LockType.NONE else s.lockType,
                    )
                }
            }
        }
        viewModelScope.launch {
            themeManager.globalNotificationsEnabled.collect { enabled ->
                _state.update { it.copy(globalNotificationsEnabled = enabled) }
            }
        }
    }

    // Basic info
    fun setName(v: String) =
        _state.update { it.copy(name = v, nameError = null, duplicateError = null) }

    fun setUrl(v: String) = _state.update { it.copy(url = v, urlError = null) }
    fun setThemeColor(v: String?) {
        _state.update { it.copy(themeColor = v) }
        val src = _state.value.iconSource
        if (src is IconSource.SvgIcon && v != null) {
            val bgColorArgb =
                runCatching { android.graphics.Color.parseColor(v) }.getOrNull() ?: return
            reRenderSvgIcon(src.slug, bgColorArgb)
        }
    }

    fun setIconPath(v: String) =
        _state.update { it.copy(iconPath = v, iconSource = IconSource.Path(v)) }

    // Fullscreen
    fun setFullscreen(v: Boolean) = _state.update { it.copy(isFullscreen = v) }
    fun setFullscreenShowStatusBar(v: Boolean) =
        _state.update { it.copy(fullscreenShowStatusBar = v) }

    fun setFullscreenShowNavBar(v: Boolean) = _state.update { it.copy(fullscreenShowNavBar = v) }
    fun setFullscreenShowTopToolbar(v: Boolean) =
        _state.update { it.copy(fullscreenShowTopToolbar = v) }

    // Ad blocking
    fun setAdBlock(v: Boolean) = _state.update { it.copy(adBlockEnabled = v) }
    fun setAdBlockAllowUserToggle(v: Boolean) =
        _state.update { it.copy(adBlockAllowUserToggle = v) }

    fun setAdBlockCustomRuleInput(v: String) = _state.update { it.copy(adBlockCustomRuleInput = v) }
    fun addAdBlockCustomRule() {
        val rule = _state.value.adBlockCustomRuleInput.trim()
        if (rule.isBlank() || rule in _state.value.adBlockCustomRules) return
        _state.update {
            it.copy(
                adBlockCustomRules = it.adBlockCustomRules + rule,
                adBlockCustomRuleInput = ""
            )
        }
    }

    fun removeAdBlockCustomRule(rule: String) =
        _state.update { it.copy(adBlockCustomRules = it.adBlockCustomRules - rule) }

    // Translation
    fun setTranslate(v: Boolean) = _state.update { it.copy(translateEnabled = v) }
    fun setTranslateTarget(v: TranslateLanguage) = _state.update { it.copy(translateTarget = v) }
    fun setAutoTranslateOnLoad(v: Boolean) = _state.update { it.copy(autoTranslateOnLoad = v) }

    // Browser
    fun setUaMode(v: UserAgentMode) = _state.update { it.copy(uaMode = v) }
    fun setEngineType(v: EngineType) = _state.update { it.copy(engineType = v) }

    // Notifications
    fun setNotificationsEnabled(v: Boolean) = _state.update { it.copy(notificationsEnabled = v) }

    // Security
    fun setLockType(v: LockType) = _state.update { it.copy(lockType = v) }
    fun setWipeOnFailedAttempts(v: Boolean) = _state.update { it.copy(wipeOnFailedAttempts = v) }

    // ── Analysis ──────────────────────────────────────────────────────────────

    /** Fetches only the icon for the current URL without running full PWA analysis. */
    fun fetchIcon() {
        val url = normalizeAndValidateUrl(_state.value.url) ?: return
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
        val rawUrl = normalizeAndValidateUrl(_state.value.url) ?: return
        _state.update { it.copy(isAnalyzing = true, analyzeError = null, url = rawUrl) }
        viewModelScope.launch {
            val manifest = runCatching { analyzer.analyze(rawUrl) }.getOrNull()
            val iconUrl = manifest?.bestIconUrl(rawUrl)
            val iconPath = faviconFetcher.fetch(iconUrl, rawUrl, _state.value.isolationId)
            if (manifest != null) {
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        pendingManifest = manifest,
                        pendingIconPath = iconPath
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        analyzeError = context.getString(R.string.error_analyze_failed),
                        pendingIconPath = iconPath
                    )
                }
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

    fun dismissManifest() =
        _state.update { it.copy(pendingManifest = null, pendingIconPath = null) }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun normalizeAndValidateUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(urlError = context.getString(R.string.add_url_error_blank)) }
            return null
        }
        // .onion hosts are Tor hidden services: HTTPS is rarely available and the plain HTTP
        // traffic is encrypted end-to-end by the Tor circuit. Allow http://*.onion as-is
        // and normalize bare *.onion hostnames to http:// (never https://).
        if (isOnionUrl(trimmed)) {
            val normalized = if (!trimmed.startsWith("http")) "http://$trimmed" else trimmed
            if (!isValidUrl(normalized)) {
                _state.update { it.copy(urlError = context.getString(R.string.error_invalid_url)) }
                return null
            }
            return normalized
        }
        // Explicitly typed http:// must be upgraded to https before saving.
        if (trimmed.startsWith("http://")) {
            _state.update { it.copy(urlError = context.getString(R.string.add_url_error_http)) }
            return null
        }
        val normalized = if (!trimmed.startsWith("https://")) "https://$trimmed" else trimmed
        if (!isValidHttpsUrl(normalized)) {
            _state.update { it.copy(urlError = context.getString(R.string.error_invalid_url)) }
            return null
        }
        return normalized
    }

    /**
     * Returns true if [raw] is an .onion URL — with or without a scheme.
     * Matches: `http://abc.onion`, `https://abc.onion`, `abc.onion/path`, `abc.onion`.
     */
    internal fun isOnionUrl(raw: String): Boolean {
        val host = runCatching { java.net.URL(raw).host }.getOrNull() ?: raw.substringBefore('/')
        return host.trimEnd('/').endsWith(".onion", ignoreCase = true)
    }

    private fun isValidUrl(url: String): Boolean = try {
        val parsed = java.net.URL(url)
        parsed.host.isNotEmpty() && !parsed.host.contains(' ')
    } catch (_: Exception) { false }

    private fun isValidHttpsUrl(url: String): Boolean = try {
        val parsed = java.net.URL(url)
        parsed.protocol == "https" && parsed.host.isNotEmpty() && !parsed.host.contains(' ')
    } catch (_: Exception) { false }

    private fun validate(): String? {
        val url = normalizeAndValidateUrl(_state.value.url) ?: return null
        if (_state.value.name.isBlank()) {
            _state.update { it.copy(nameError = context.getString(R.string.add_name_error_blank)) }
            return null
        }
        return url
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save(onCreateShortcut: ((WebApp) -> Unit)? = null) {
        val url = validate() ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (isDuplicate()) return@launch
            val savedId = persistApp(url)
            val savedApp = getWebAppById(savedId) ?: buildApp(url).copy(id = savedId)
            if (appId == 0L) onCreateShortcut?.invoke(savedApp)
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }

    fun run() {
        val url = validate() ?: return
        _state.update { it.copy(previewUrl = url) }
    }

    private suspend fun isDuplicate(): Boolean {
        if (appId != 0L) return false
        val name = _state.value.name.trim()
        val existing = getWebAppByName(name) ?: return false
        _state.update {
            it.copy(
                isSaving = false,
                duplicateError = context.getString(AddR.string.add_error_duplicate, existing.name)
            )
        }
        return true
    }

    fun onPreviewed() = _state.update { it.copy(previewUrl = null) }

    fun markShortcutCreated(app: WebApp) = viewModelScope.launch {
        saveWebApp(app.copy(hasLauncherShortcut = true))
    }

    // ── Icon pack picker ──────────────────────────────────────────────────────

    fun openIconPackPicker() {
        viewModelScope.launch {
            val reader = SimpleIconsReader(context)
            val icons = reader.readAll()
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

    private suspend fun reRenderSvgIconInternal(
        slug: String,
        bgColorArgb: Int,
        isSelection: Boolean
    ) {
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
            notificationPermission = if (!s.notificationsEnabled) NotificationPermission.DENIED
            else originalApp?.notificationPermission ?: NotificationPermission.NOT_ASKED,
            lockType = s.lockType,
            wipeOnFailedAttempts = s.wipeOnFailedAttempts,
        )
    }
}
