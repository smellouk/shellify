package io.shellify.app.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.shellify.app.domain.model.Category
import io.shellify.app.domain.usecase.DeleteAllCategoriesUseCase
import io.shellify.app.domain.usecase.DeleteCategoryUseCase
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.SaveCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryUiState(
    val newName: String = "",
    val editingId: Long? = null,
    val showAddDialog: Boolean = false,
    val selectedIcon: String = "folder",
    val selectedColor: String = "#6D28D9",
)

class CategoryViewModel(
    getCategories: GetCategoriesUseCase,
    private val saveCategory: SaveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val deleteAllCategories: DeleteAllCategoriesUseCase,
) : ViewModel() {

    val categories: kotlinx.coroutines.flow.StateFlow<List<Category>?> = getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _state = MutableStateFlow(CategoryUiState())
    val uiState = _state

    fun showDialog() = _state.update { it.copy(showAddDialog = true, editingId = null) }

    fun showDialogWithPreset(name: String, iconKey: String, hexColor: String) = _state.update {
        it.copy(
            showAddDialog = true,
            editingId = null,
            newName = name,
            selectedIcon = iconKey,
            selectedColor = hexColor,
        )
    }

    fun showEditDialog(category: Category) = _state.update {
        it.copy(
            showAddDialog = true,
            editingId = category.id,
            newName = category.name,
            selectedIcon = category.icon,
            selectedColor = category.color,
        )
    }

    fun dismissDialog() = _state.update {
        it.copy(
            showAddDialog = false,
            editingId = null,
            newName = "",
            selectedIcon = "folder",
            selectedColor = "#6D28D9"
        )
    }

    fun setNewName(name: String) = _state.update { it.copy(newName = name) }
    fun setSelectedIcon(icon: String) = _state.update { it.copy(selectedIcon = icon) }
    fun setSelectedColor(color: String) = _state.update { it.copy(selectedColor = color) }

    fun addCategory() {
        val name = _state.value.newName.trim().takeIf { it.isNotBlank() } ?: return
        val icon = _state.value.selectedIcon
        val color = _state.value.selectedColor
        val id = _state.value.editingId ?: 0L
        viewModelScope.launch {
            saveCategory(Category(id = id, name = name, icon = icon, color = color))
            dismissDialog()
        }
    }

    fun delete(category: Category) = viewModelScope.launch { deleteCategory(category) }

    fun deleteAllCategories() = viewModelScope.launch { deleteAllCategories.invoke() }
}
