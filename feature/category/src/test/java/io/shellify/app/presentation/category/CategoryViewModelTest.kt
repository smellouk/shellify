package io.shellify.app.presentation.category

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.shellify.app.domain.model.Category
import io.shellify.app.domain.usecase.DeleteAllCategoriesUseCase
import io.shellify.app.domain.usecase.DeleteCategoryUseCase
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.SaveCategoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val saveCategory = mockk<SaveCategoryUseCase>()
    private val deleteCategory = mockk<DeleteCategoryUseCase>()
    private val deleteAllCategories = mockk<DeleteAllCategoriesUseCase>(relaxed = true)
    private lateinit var viewModel: CategoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { getCategories() } returns flowOf(emptyList())
        viewModel = CategoryViewModel(getCategories, saveCategory, deleteCategory, deleteAllCategories)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has dialog hidden and default values`() {
        val state = viewModel.uiState.value
        assertFalse(state.showAddDialog)
        assertNull(state.editingId)
        assertEquals("", state.newName)
        assertEquals("folder", state.selectedIcon)
        assertEquals("#6D28D9", state.selectedColor)
    }

    @Test
    fun `showDialog sets showAddDialog true and clears editingId`() {
        viewModel.showDialog()
        val state = viewModel.uiState.value
        assertTrue(state.showAddDialog)
        assertNull(state.editingId)
    }

    @Test
    fun `showDialogWithPreset populates name icon and color`() {
        viewModel.showDialogWithPreset("Work", "briefcase", "#FF0000")
        val state = viewModel.uiState.value
        assertTrue(state.showAddDialog)
        assertEquals("Work", state.newName)
        assertEquals("briefcase", state.selectedIcon)
        assertEquals("#FF0000", state.selectedColor)
    }

    @Test
    fun `showEditDialog populates fields from category`() {
        val cat = Category(id = 42L, name = "Travel", icon = "flight", color = "#00FF00")
        viewModel.showEditDialog(cat)
        val state = viewModel.uiState.value
        assertTrue(state.showAddDialog)
        assertEquals(42L, state.editingId)
        assertEquals("Travel", state.newName)
        assertEquals("flight", state.selectedIcon)
        assertEquals("#00FF00", state.selectedColor)
    }

    @Test
    fun `dismissDialog resets all fields to defaults`() {
        viewModel.showDialogWithPreset("Work", "briefcase", "#FF0000")
        viewModel.dismissDialog()
        val state = viewModel.uiState.value
        assertFalse(state.showAddDialog)
        assertNull(state.editingId)
        assertEquals("", state.newName)
        assertEquals("folder", state.selectedIcon)
        assertEquals("#6D28D9", state.selectedColor)
    }

    @Test
    fun `setNewName updates newName in state`() {
        viewModel.setNewName("Finance")
        assertEquals("Finance", viewModel.uiState.value.newName)
    }

    @Test
    fun `addCategory calls saveCategory and dismisses dialog`() = runTest {
        coEvery { saveCategory(any()) } returns 1L
        viewModel.setNewName("Health")
        viewModel.addCategory()
        coVerify(exactly = 1) { saveCategory(any()) }
        assertFalse(viewModel.uiState.value.showAddDialog)
    }

    @Test
    fun `addCategory does nothing when name is blank`() = runTest {
        viewModel.setNewName("   ")
        viewModel.addCategory()
        coVerify(exactly = 0) { saveCategory(any()) }
    }

    @Test
    fun `delete calls deleteCategory use case`() = runTest {
        val cat = Category(id = 1L, name = "Test")
        coEvery { deleteCategory(cat) } returns Unit
        viewModel.delete(cat)
        coVerify(exactly = 1) { deleteCategory(cat) }
    }
}
