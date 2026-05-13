package dev.pwaforge.presentation.shortcuts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pwaforge.core.iconpack.SimpleIconEntry
import dev.pwaforge.core.iconpack.SimpleIconsReader
import dev.pwaforge.core.pwa.FaviconFetcher
import dev.pwaforge.core.pwa.PwaAnalyzer
import dev.pwaforge.core.shortcut.PwaShortcutManager
import dev.pwaforge.core.shortcut.ShortcutIconBuilder
import dev.pwaforge.core.shortcut.SvgIconRenderer
import dev.pwaforge.domain.model.IconSource
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.domain.repository.WebAppRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class IconRefreshState { Idle, Loading, Success, Error }

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
    val addableApps: List<WebApp> = emptyList(),
    val showAddSheet: Boolean = false,
    val iconSheetTarget: ShortcutItem? = null,
    val iconRefreshState: IconRefreshState = IconRefreshState.Idle,
    val showIconPackPicker: Boolean = false,
    val packIcons: List<SimpleIconEntry> = emptyList(),
    val iconPickerQuery: String = "",
    val isLoadingIconPack: Boolean = false,
)

class ShortcutsViewModel(
    private val context: Context,
    private val repo: WebAppRepository,
    private val analyzer: PwaAnalyzer,
    private val faviconFetcher: FaviconFetcher,
) : ViewModel() {

    private val _state = MutableStateFlow(ShortcutsUiState())
    val uiState: StateFlow<ShortcutsUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val appByIsolationId = repo.getAll().first().associateBy { it.isolationId }
            val pinned = PwaShortcutManager.getPinnedShortcuts(context).filter { it.isEnabled }
            val pinnedIds = pinned.map { it.id }.toSet()
            val items = pinned.mapNotNull { shortcut ->
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
            val addableApps = appByIsolationId.values
                .filter { "pwa_${it.isolationId}" !in pinnedIds }
                .sortedBy { it.name }
            _state.update { it.copy(items = items, isLoading = false, addableApps = addableApps) }
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

    fun showIconSheet(item: ShortcutItem) = _state.update { it.copy(iconSheetTarget = item, iconRefreshState = IconRefreshState.Idle) }
    fun dismissIconSheet() = _state.update { it.copy(iconSheetTarget = null, iconRefreshState = IconRefreshState.Idle) }

    fun refreshIconFromSource() {
        val item = _state.value.iconSheetTarget ?: return
        _state.update { it.copy(iconRefreshState = IconRefreshState.Loading) }
        viewModelScope.launch(Dispatchers.IO) {
            val success = runCatching {
                val manifest = analyzer.analyze(item.app.url)
                val iconUrl = manifest.bestIconUrl(item.app.url)
                val path = faviconFetcher.fetch(iconUrl, item.app.url, item.app.isolationId)
                    ?: return@runCatching false
                val updated = item.app.copy(iconSource = IconSource.Path(path))
                repo.save(updated)
                val bitmap = ShortcutIconBuilder.build(context, updated)
                PwaShortcutManager.changeIcon(context, updated, item.label, bitmap)
                true
            }.getOrDefault(false)
            _state.update { it.copy(iconRefreshState = if (success) IconRefreshState.Success else IconRefreshState.Error) }
            delay(1_500)
            dismissIconSheet()
            if (success) load()
        }
    }

    fun openIconPackPicker() {
        val item = _state.value.iconSheetTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingIconPack = true, showIconPackPicker = true, iconSheetTarget = item) }
            val icons = SimpleIconsReader(context).readAll()
            _state.update { it.copy(packIcons = icons, isLoadingIconPack = false, iconPickerQuery = "") }
        }
    }

    fun closeIconPackPicker() = _state.update { it.copy(showIconPackPicker = false, packIcons = emptyList(), iconSheetTarget = null) }

    fun setIconPickerQuery(q: String) = _state.update { it.copy(iconPickerQuery = q) }

    fun applyPackIcon(entry: SimpleIconEntry, bgColorArgb: Int) {
        val item = _state.value.iconSheetTarget ?: return
        _state.update { it.copy(showIconPackPicker = false, iconSheetTarget = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val iconSource = SvgIconRenderer.render(
                context = context,
                slug = entry.slug,
                bgColorArgb = bgColorArgb,
                isolationId = item.app.isolationId,
                existingIconPath = item.app.iconPath,
            ) ?: return@launch
            val updated = item.app.copy(iconSource = iconSource)
            repo.save(updated)
            val bitmap = ShortcutIconBuilder.build(context, updated)
            PwaShortcutManager.changeIcon(context, updated, item.label, bitmap)
            load()
        }
    }

    fun applyPickedIcon(item: ShortcutItem, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return@launch
        val scaled = ShortcutIconBuilder.scaleCentered(bitmap)
        val path = saveBitmap(item.app, scaled) ?: return@launch
        val updated = item.app.copy(iconSource = IconSource.Path(path))
        repo.save(updated)
        PwaShortcutManager.changeIcon(context, updated, item.label, scaled)
        load()
    }

    private fun saveBitmap(app: WebApp, bitmap: Bitmap): String? = runCatching {
        val dir = File(context.filesDir, "icons").also { it.mkdirs() }
        app.iconPath?.let { File(it).delete() }
        val file = File(dir, "${app.isolationId}_${System.currentTimeMillis()}.png")
        file.outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        file.absolutePath
    }.getOrNull()

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
                addableApps = (s.addableApps + item.app).sortedBy { it.name },
            )
        }
    }

    // ── Add ───────────────────────────────────────────────────────────────────

    fun showAddSheet() = _state.update { it.copy(showAddSheet = true) }
    fun dismissAddSheet() = _state.update { it.copy(showAddSheet = false) }

    fun createShortcut(app: WebApp) {
        PwaShortcutManager.createShortcut(context, app)
        _state.update { it.copy(showAddSheet = false) }
        load()
    }
}
