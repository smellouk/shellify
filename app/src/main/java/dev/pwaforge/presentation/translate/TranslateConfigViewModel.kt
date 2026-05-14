package dev.pwaforge.presentation.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pwaforge.domain.model.TranslateLanguage
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.domain.repository.WebAppRepository
import dev.pwaforge.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslateConfigUiState(
    val app: WebApp? = null,
    val isLoading: Boolean = true,
)

class TranslateConfigViewModel(
    private val appId: Long,
    private val repo: WebAppRepository,
    private val saveWebApp: SaveWebAppUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TranslateConfigUiState())
    val uiState: StateFlow<TranslateConfigUiState> = _state

    init {
        viewModelScope.launch {
            _state.update { it.copy(app = repo.getById(appId), isLoading = false) }
        }
    }

    private fun update(transform: (WebApp) -> WebApp) {
        val app = _state.value.app ?: return
        val updated = transform(app)
        _state.update { it.copy(app = updated) }
        viewModelScope.launch { saveWebApp(updated) }
    }

    fun setLanguage(lang: TranslateLanguage) = update { it.copy(translateTarget = lang) }
    fun setInstanceUrl(v: String) = update { it.copy(libreTranslateUrl = v) }
    fun setAutoTranslate(v: Boolean) = update { it.copy(autoTranslateOnLoad = v) }
}
