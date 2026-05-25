package io.shellify.app.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.shortcut.PwaShortcutManager
import io.shellify.app.domain.model.Category
import io.shellify.app.domain.model.NotificationChannelId
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.model.IconSource
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val quickAddLoadingUrls: Set<String> = emptySet(),
    val quickAddDoneUrls: Set<String> = emptySet(),
)

private data class QuickAddState(val loadingUrls: Set<String> = emptySet(), val doneUrls: Set<String> = emptySet())

class HomeViewModel(
    private val getWebApps: GetWebAppsUseCase,
    private val deleteAllAppsUseCase: DeleteAllAppsUseCase,
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
            .filter {
                if (query.isBlank()) true else it.name.contains(
                    query,
                    ignoreCase = true
                ) || it.url.contains(query, ignoreCase = true)
            }
        HomeUiState(
            apps = filtered,
            hasAnyApps = apps.isNotEmpty(),
            categories = categories,
            selectedCategoryId = selectedCategory,
            searchQuery = query,
            isLoading = false,
            quickAddLoadingUrls = quickAdd.loadingUrls,
            quickAddDoneUrls = quickAdd.doneUrls,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectCategory(id: Long?) = _extra.update { it.copy(first = id) }
    fun setSearch(query: String) = _extra.update { it.copy(second = query) }

    fun quickAdd(name: String, rawUrl: String) {
        if (rawUrl in _quickAdd.value.loadingUrls || rawUrl in _quickAdd.value.doneUrls) return
        viewModelScope.launch {
            val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
            _quickAdd.update { it.copy(loadingUrls = it.loadingUrls + rawUrl) }
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
                    iconSource = IconSource.fromLegacyPath(
                        fetchedIconPath
                    ),
                    themeColor = manifest?.themeColor,
                    backgroundColor = manifest?.backgroundColor,
                    description = manifest?.description,
                    isolationId = isolationId,
                ),
            )
            _quickAdd.update { it.copy(loadingUrls = it.loadingUrls - rawUrl, doneUrls = it.doneUrls + rawUrl) }
            delay(1200)
            _quickAdd.update { it.copy(doneUrls = it.doneUrls - rawUrl) }
        }
    }

    fun delete(app: WebApp) = viewModelScope.launch {
        PwaShortcutManager.removeShortcut(context, app)
        deleteWebApp(app)
        androidx.core.app.NotificationManagerCompat.from(context)
            .deleteNotificationChannel(NotificationChannelId.forApp(app.isolationId))
    }

    fun assignCategory(app: WebApp, categoryId: Long?) = viewModelScope.launch {
        saveWebApp(app.copy(categoryId = categoryId))
    }

    fun clearData(app: WebApp) = viewModelScope.launch {
        isolationManager.clearData(app.isolationId)
    }

    fun deleteAllApps() = viewModelScope.launch {
        val apps = getWebApps().first()
        apps.forEach { app ->
            isolationManager.clearData(app.isolationId)
            PwaShortcutManager.removeShortcut(context, app)
        }
        deleteAllAppsUseCase()
    }
}
