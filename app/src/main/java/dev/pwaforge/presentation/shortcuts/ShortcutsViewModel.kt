package dev.pwaforge.presentation.shortcuts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.domain.repository.WebAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShortcutItem(
    val app: WebApp,
    val shortcutId: String,
    val label: String,
)

data class ShortcutsUiState(
    val items: List<ShortcutItem> = emptyList(),
    val isLoading: Boolean = true,
    val renameTarget: ShortcutItem? = null,
    val renameText: String = "",
    val removeTarget: ShortcutItem? = null,
)

class ShortcutsViewModel(
    private val context: Context,
    private val repo: WebAppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShortcutsUiState())
    val uiState: StateFlow<ShortcutsUiState> = _state

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val appByIsolationId = repo.getAll().first().associateBy { it.isolationId }
            val items = PwaShortcutManager.getPinnedShortcuts(context).mapNotNull { shortcut ->
                val isolationId = shortcut.id.removePrefix("pwa_")
                val app = appByIsolationId[isolationId] ?: return@mapNotNull null
                ShortcutItem(
                    app = app,
                    shortcutId = shortcut.id,
                    label = shortcut.longLabel?.toString()
                        ?: shortcut.shortLabel?.toString()
                        ?: app.name,
                )
            }
            _state.update { it.copy(items = items, isLoading = false) }
        }
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    fun startRename(item: ShortcutItem) = _state.update { it.copy(renameTarget = item, renameText = item.label) }
    fun setRenameText(text: String) = _state.update { it.copy(renameText = text) }
    fun dismissRename() = _state.update { it.copy(renameTarget = null, renameText = "") }

    fun confirmRename() {
        val item = _state.value.renameTarget ?: return
        val newLabel = _state.value.renameText.trim().ifBlank { return }
        viewModelScope.launch(Dispatchers.IO) {
            PwaShortcutManager.rename(context, item.app, newLabel)
            _state.update { s ->
                s.copy(
                    renameTarget = null,
                    renameText = "",
                    items = s.items.map { if (it.shortcutId == item.shortcutId) it.copy(label = newLabel) else it },
                )
            }
        }
    }

    // ── Icon ──────────────────────────────────────────────────────────────────

    fun refreshIcon(item: ShortcutItem) = viewModelScope.launch(Dispatchers.IO) {
        PwaShortcutManager.refreshIcon(context, item.app, item.label)
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    fun showRemove(item: ShortcutItem) = _state.update { it.copy(removeTarget = item) }
    fun dismissRemove() = _state.update { it.copy(removeTarget = null) }

    fun confirmRemove() {
        val item = _state.value.removeTarget ?: return
        PwaShortcutManager.removeShortcut(context, item.app)
        _state.update { s ->
            s.copy(
                removeTarget = null,
                items = s.items.filter { it.shortcutId != item.shortcutId },
            )
        }
    }
}
