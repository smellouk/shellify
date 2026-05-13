package dev.pwaforge.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pwaforge.core.isolation.IsolationManager
import dev.pwaforge.core.pwa.FaviconFetcher
import dev.pwaforge.core.pwa.PwaAnalyzer
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.domain.model.Category
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.domain.usecase.DeleteWebAppUseCase
import dev.pwaforge.domain.usecase.GetCategoriesUseCase
import dev.pwaforge.domain.usecase.GetWebAppsUseCase
import dev.pwaforge.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val apps: List<WebApp> = emptyList(),
    val hasAnyApps: Boolean = false,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val quickAddLoadingUrl: String? = null,
    val quickAddDoneUrl: String? = null,
)

private data class QuickAddState(val loadingUrl: String? = null, val doneUrl: String? = null)

class HomeViewModel(
    getWebApps: GetWebAppsUseCase,
    private val deleteWebApp: DeleteWebAppUseCase,
    getCategories: GetCategoriesUseCase,
    private val saveWebApp: SaveWebAppUseCase,
    private val isolationManager: IsolationManager,
    private val context: Context,
    private val pwaAnalyzer: PwaAnalyzer,
    private val faviconFetcher: FaviconFetcher,
) : ViewModel() {

    private val _extra = MutableStateFlow(Pair<Long?, String>(null, ""))
    private val _quickAdd = MutableStateFlow(QuickAddState())

    val uiState: StateFlow<HomeUiState> = combine(
        getWebApps(),
        getCategories(),
        _extra,
        _quickAdd,
    ) { apps, categories, (selectedCategory, query), quickAdd ->
        val filtered = apps
            .filter { if (selectedCategory != null) it.categoryId == selectedCategory else true }
            .filter { if (query.isBlank()) true else it.name.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true) }
        HomeUiState(
            apps = filtered,
            hasAnyApps = apps.isNotEmpty(),
            categories = categories,
            selectedCategoryId = selectedCategory,
            searchQuery = query,
            isLoading = false,
            quickAddLoadingUrl = quickAdd.loadingUrl,
            quickAddDoneUrl = quickAdd.doneUrl,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectCategory(id: Long?) = _extra.update { it.copy(first = id) }
    fun setSearch(query: String) = _extra.update { it.copy(second = query) }

    fun quickAdd(name: String, rawUrl: String) {
        if (_quickAdd.value.loadingUrl != null) return
        viewModelScope.launch {
            val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
            _quickAdd.value = QuickAddState(loadingUrl = rawUrl)
            val isolationId = UUID.randomUUID().toString()
            val manifest = runCatching { pwaAnalyzer.analyze(fullUrl) }.getOrNull()
            val iconUrl = manifest?.bestIconUrl(fullUrl)
            val fetchedIconPath = faviconFetcher.fetch(iconUrl, fullUrl, isolationId)
            saveWebApp(
                WebApp(
                    name = manifest?.name?.takeIf { it.isNotBlank() }
                        ?: manifest?.shortName?.takeIf { it.isNotBlank() }
                        ?: name,
                    url = fullUrl,
                    iconSource = dev.pwaforge.domain.model.IconSource.fromLegacyPath(fetchedIconPath),
                    themeColor = manifest?.themeColor,
                    backgroundColor = manifest?.backgroundColor,
                    description = manifest?.description,
                    isolationId = isolationId,
                ),
            )
            _quickAdd.value = QuickAddState(doneUrl = rawUrl)
            delay(1200)
            _quickAdd.value = QuickAddState()
        }
    }

    fun delete(app: WebApp) = viewModelScope.launch {
        PwaShortcutManager.removeShortcut(context, app)
        deleteWebApp(app)
    }

    fun assignCategory(app: WebApp, categoryId: Long?) = viewModelScope.launch {
        saveWebApp(app.copy(categoryId = categoryId))
    }

    fun clearData(app: WebApp) = viewModelScope.launch {
        isolationManager.clearData(app.isolationId)
    }
}
