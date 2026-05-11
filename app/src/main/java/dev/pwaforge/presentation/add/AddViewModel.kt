package dev.pwaforge.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pwaforge.core.pwa.FaviconFetcher
import dev.pwaforge.core.pwa.PwaAnalyzer
import dev.pwaforge.domain.model.TranslateEngine
import dev.pwaforge.domain.model.TranslateLanguage
import dev.pwaforge.domain.model.UserAgentMode
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.domain.repository.WebAppRepository
import dev.pwaforge.domain.usecase.GetCategoriesUseCase
import dev.pwaforge.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddUiState(
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val url: String = "",
    val iconPath: String? = null,
    val themeColor: String? = null,
    val categoryId: Long? = null,
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
    val translateEngine: TranslateEngine = TranslateEngine.AUTO,
    val showTranslateButton: Boolean = true,
    val autoTranslateOnLoad: Boolean = false,
    // Browser
    val uaMode: UserAgentMode = UserAgentMode.CHROME_MOBILE,
    val analyzeError: String? = null,
    val urlError: String? = null,
    val saved: Boolean = false,
)

class AddViewModel(
    private val appId: Long,
    private val repo: WebAppRepository,
    private val saveWebApp: SaveWebAppUseCase,
    getCategories: GetCategoriesUseCase,
    private val analyzer: PwaAnalyzer,
    private val faviconFetcher: FaviconFetcher,
) : ViewModel() {

    private val _state = MutableStateFlow(AddUiState(isLoading = appId != 0L))
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
                        themeColor = app.themeColor,
                        categoryId = app.categoryId,
                        isFullscreen = app.isFullscreen,
                        fullscreenShowStatusBar = app.fullscreenShowStatusBar,
                        fullscreenShowNavBar = app.fullscreenShowNavBar,
                        fullscreenShowTopToolbar = app.fullscreenShowTopToolbar,
                        adBlockEnabled = app.adBlockEnabled,
                        adBlockAllowUserToggle = app.adBlockAllowUserToggle,
                        adBlockCustomRules = app.adBlockCustomRules,
                        translateEnabled = app.translateEnabled,
                        translateTarget = app.translateTarget,
                        translateEngine = app.translateEngine,
                        showTranslateButton = app.showTranslateButton,
                        autoTranslateOnLoad = app.autoTranslateOnLoad,
                        uaMode = app.uaMode,
                    )
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setUrl(v: String) = _state.update { it.copy(url = v, urlError = null) }
    fun setFullscreen(v: Boolean) = _state.update { it.copy(isFullscreen = v) }
    fun setFullscreenShowStatusBar(v: Boolean) = _state.update { it.copy(fullscreenShowStatusBar = v) }
    fun setFullscreenShowNavBar(v: Boolean) = _state.update { it.copy(fullscreenShowNavBar = v) }
    fun setFullscreenShowTopToolbar(v: Boolean) = _state.update { it.copy(fullscreenShowTopToolbar = v) }
    fun setAdBlock(v: Boolean) = _state.update { it.copy(adBlockEnabled = v) }
    fun setAdBlockAllowUserToggle(v: Boolean) = _state.update { it.copy(adBlockAllowUserToggle = v) }
    fun setAdBlockCustomRuleInput(v: String) = _state.update { it.copy(adBlockCustomRuleInput = v) }
    fun addAdBlockCustomRule() {
        val rule = _state.value.adBlockCustomRuleInput.trim()
        if (rule.isBlank() || rule in _state.value.adBlockCustomRules) return
        _state.update { it.copy(adBlockCustomRules = it.adBlockCustomRules + rule, adBlockCustomRuleInput = "") }
    }
    fun removeAdBlockCustomRule(rule: String) = _state.update { it.copy(adBlockCustomRules = it.adBlockCustomRules - rule) }
    fun setTranslate(v: Boolean) = _state.update { it.copy(translateEnabled = v) }
    fun setTranslateTarget(v: TranslateLanguage) = _state.update { it.copy(translateTarget = v) }
    fun setTranslateEngine(v: TranslateEngine) = _state.update { it.copy(translateEngine = v) }
    fun setShowTranslateButton(v: Boolean) = _state.update { it.copy(showTranslateButton = v) }
    fun setAutoTranslateOnLoad(v: Boolean) = _state.update { it.copy(autoTranslateOnLoad = v) }
    fun setUaMode(v: UserAgentMode) = _state.update { it.copy(uaMode = v) }

    fun analyze() {
        val rawUrl = _state.value.url.trim().let { if (!it.startsWith("http")) "https://$it" else it }
        if (rawUrl.isBlank()) { _state.update { it.copy(urlError = "Please enter a URL") }; return }
        _state.update { it.copy(isAnalyzing = true, analyzeError = null, url = rawUrl) }
        viewModelScope.launch {
            runCatching {
                val manifest = analyzer.analyze(rawUrl)
                val isolationId = originalApp?.isolationId ?: WebApp(name = "", url = "").isolationId
                val iconPath = faviconFetcher.fetch(manifest.bestIconUrl(rawUrl), rawUrl, isolationId)
                _state.update { s ->
                    s.copy(
                        isAnalyzing = false,
                        name = s.name.ifBlank { manifest.shortName ?: manifest.name ?: "" },
                        themeColor = manifest.themeColor,
                        iconPath = iconPath ?: s.iconPath,
                    )
                }
            }.onFailure {
                _state.update { s ->
                    s.copy(isAnalyzing = false, analyzeError = "Could not read site info. You can still save manually.")
                }
            }
        }
    }

    fun save(onCreateShortcut: ((WebApp) -> Unit)? = null) {
        val s = _state.value
        val url = s.url.trim().let { if (!it.startsWith("http")) "https://$it" else it }
        if (url.isBlank()) { _state.update { it.copy(urlError = "Please enter a URL") }; return }
        if (s.name.isBlank()) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val app = (originalApp ?: WebApp(name = "", url = "")).copy(
                id = appId,
                name = s.name.trim(),
                url = url,
                iconPath = s.iconPath,
                themeColor = s.themeColor,
                categoryId = s.categoryId,
                isFullscreen = s.isFullscreen,
                fullscreenShowStatusBar = s.fullscreenShowStatusBar,
                fullscreenShowNavBar = s.fullscreenShowNavBar,
                fullscreenShowTopToolbar = s.fullscreenShowTopToolbar,
                adBlockEnabled = s.adBlockEnabled,
                adBlockAllowUserToggle = s.adBlockAllowUserToggle,
                adBlockCustomRules = s.adBlockCustomRules,
                translateEnabled = s.translateEnabled,
                translateTarget = s.translateTarget,
                translateEngine = s.translateEngine,
                showTranslateButton = s.showTranslateButton,
                autoTranslateOnLoad = s.autoTranslateOnLoad,
                uaMode = s.uaMode,
            )
            val savedId = saveWebApp(app)
            val savedApp = repo.getById(savedId) ?: app.copy(id = savedId)
            onCreateShortcut?.invoke(savedApp)
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
