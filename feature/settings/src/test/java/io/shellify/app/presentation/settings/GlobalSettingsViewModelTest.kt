package io.shellify.app.presentation.settings

import android.app.NotificationManager
import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.shellify.app.core.backup.BackupManager
import io.shellify.app.core.backup.BackupSchedule
import io.shellify.app.core.backup.BackupSettings
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.model.WebApp
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.DeleteAllCategoriesUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import io.shellify.core.ui.R as CoreUiR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class GlobalSettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val themeManager = mockk<ThemeManager>(relaxed = true)
    private val isolationManager = mockk<IsolationManager>(relaxed = true)
    private val getWebApps = mockk<GetWebAppsUseCase>()
    private val saveWebApp = mockk<SaveWebAppUseCase>()
    private val deleteAllAppsUseCase = mockk<DeleteAllAppsUseCase>(relaxed = true)
    private val deleteAllCategoriesUseCase = mockk<DeleteAllCategoriesUseCase>(relaxed = true)
    private val passwordManager = mockk<PasswordManager>(relaxed = true)
    private val backupSettings = mockk<BackupSettings>(relaxed = true)
    private val backupManager = mockk<BackupManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val geckoEngineManager = mockk<GeckoEngineManager>(relaxed = true)
    private val simpleIconsManager = mockk<SimpleIconsManager>(relaxed = true)

    private lateinit var viewModel: GlobalSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { themeManager.themeMode } returns MutableStateFlow(ThemeMode.SYSTEM)
        every { themeManager.dynamicColor } returns MutableStateFlow(true)
        every { themeManager.accentColor } returns MutableStateFlow(null)
        every { themeManager.defaultUaMode } returns MutableStateFlow(UserAgentMode.CHROME_MOBILE)
        every { themeManager.defaultEngineType } returns MutableStateFlow(EngineType.SYSTEM_WEBVIEW)
        every { themeManager.geckoSafeBrowsing } returns MutableStateFlow(false)
        every { themeManager.globalNotificationsEnabled } returns MutableStateFlow(true)
        every { passwordManager.passwordHash } returns MutableStateFlow(null)
        every { passwordManager.wipeOnFailedAttempts } returns MutableStateFlow(false)
        every { passwordManager.screenshotProtection } returns MutableStateFlow(false)
        every { backupSettings.enabled } returns MutableStateFlow(false)
        every { backupSettings.hasPassword } returns MutableStateFlow(false)
        every { backupSettings.directoryUri } returns MutableStateFlow(null)
        every { backupSettings.schedule } returns MutableStateFlow(BackupSchedule.NONE)
        every { backupSettings.lastBackupTime } returns MutableStateFlow(0L)
        val notificationManager = mockk<NotificationManager>(relaxed = true)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { notificationManager.notificationChannels } returns emptyList()
        every { getWebApps() } returns flowOf(emptyList())
        coEvery { saveWebApp(any()) } returns 0L
        every { context.getString(CoreUiR.string.settings_backup_error_no_password) } returns
            "Set a backup password first"
        every { context.getString(CoreUiR.string.settings_backup_error_no_folder) } returns
            "Select a backup folder first"
        every { context.getString(CoreUiR.string.settings_backup_success, any()) } answers
            { "Backed up: ${args[1]}" }
        every { context.getString(CoreUiR.string.settings_backup_failed, any()) } answers
            { "Backup failed: ${args[1]}" }
        every { context.getString(CoreUiR.string.settings_restore_failed, any()) } answers
            { "Restore failed: ${args[1]}" }

        viewModel = GlobalSettingsViewModel(
            themeManager = themeManager,
            isolationManager = isolationManager,
            getWebApps = getWebApps,
            saveWebApp = saveWebApp,
            deleteAllAppsUseCase = deleteAllAppsUseCase,
            deleteAllCategoriesUseCase = deleteAllCategoriesUseCase,
            passwordManager = passwordManager,
            backupSettings = backupSettings,
            backupManager = backupManager,
            context = context,
            geckoEngineManager = geckoEngineManager,
            simpleIconsManager = simpleIconsManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loaded with theme and password values`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertFalse(state.hasPassword)
        assertFalse(state.backupEnabled)
    }

    @Test
    fun `showSetPasswordDialog sets correct dialog mode`() {
        viewModel.showSetPasswordDialog()
        val state = viewModel.uiState.value
        assertTrue(state.showPasswordDialog)
        assertEquals(PasswordDialogMode.SET, state.passwordDialogMode)
    }

    @Test
    fun `showChangePasswordDialog sets CHANGE mode`() {
        viewModel.showChangePasswordDialog()
        assertEquals(PasswordDialogMode.CHANGE, viewModel.uiState.value.passwordDialogMode)
        assertTrue(viewModel.uiState.value.showPasswordDialog)
    }

    @Test
    fun `dismissPasswordDialog hides dialog`() {
        viewModel.showSetPasswordDialog()
        viewModel.dismissPasswordDialog()
        assertFalse(viewModel.uiState.value.showPasswordDialog)
    }

    @Test
    fun `showRemovePasswordDialog shows remove warning`() {
        viewModel.showRemovePasswordDialog()
        assertTrue(viewModel.uiState.value.showRemovePasswordWarning)
    }

    @Test
    fun `confirmRemovePasswordWarning transitions to REMOVE dialog`() {
        viewModel.showRemovePasswordDialog()
        viewModel.confirmRemovePasswordWarning()
        val state = viewModel.uiState.value
        assertFalse(state.showRemovePasswordWarning)
        assertTrue(state.showPasswordDialog)
        assertEquals(PasswordDialogMode.REMOVE, state.passwordDialogMode)
    }

    @Test
    fun `showClearAllDialog and dismissClearAllDialog toggle flag`() {
        viewModel.showClearAllDialog()
        assertTrue(viewModel.uiState.value.showClearAllDialog)
        viewModel.dismissClearAllDialog()
        assertFalse(viewModel.uiState.value.showClearAllDialog)
    }

    @Test
    fun `clearAll calls isolationManager for each app and closes dialog`() = runTest {
        val app = WebApp(id = 1L, name = "App", url = "https://x.com", isolationId = "iso-1")
        every { getWebApps() } returns flowOf(listOf(app))
        viewModel.showClearAllDialog()
        viewModel.clearAll()
        advanceUntilIdle()
        coVerify(exactly = 1) { isolationManager.clearData("iso-1") }
        assertFalse(viewModel.uiState.value.showClearAllDialog)
    }

    @Test
    fun `showDeleteAllAppsDialog and dismiss toggle flag`() {
        viewModel.showDeleteAllAppsDialog()
        assertTrue(viewModel.uiState.value.showDeleteAllAppsDialog)
        viewModel.dismissDeleteAllAppsDialog()
        assertFalse(viewModel.uiState.value.showDeleteAllAppsDialog)
    }

    @Test
    fun `deleteAllCategories calls use case and closes dialog`() = runTest {
        viewModel.showDeleteAllCategoriesDialog()
        viewModel.deleteAllCategories()
        advanceUntilIdle()
        coVerify(exactly = 1) { deleteAllCategoriesUseCase() }
        assertFalse(viewModel.uiState.value.showDeleteAllCategoriesDialog)
    }

    @Test
    fun `setThemeMode delegates to themeManager`() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        coVerify(exactly = 1) { themeManager.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `backupNow sets error message when no backup password set`() = runTest {
        coEvery { backupSettings.getPassword() } returns null
        viewModel.backupNow()
        advanceUntilIdle()
        assertEquals("Set a backup password first", viewModel.uiState.value.backupResultMessage)
    }

    @Test
    fun `clearBackupMessage clears backupResultMessage`() = runTest {
        coEvery { backupSettings.getPassword() } returns null
        viewModel.backupNow()
        advanceUntilIdle()
        viewModel.clearBackupMessage()
        assertNull(viewModel.uiState.value.backupResultMessage)
    }

    @Test
    fun `showBackupPasswordDialog and dismiss toggle flag`() {
        viewModel.showBackupPasswordDialog()
        assertTrue(viewModel.uiState.value.showBackupPasswordDialog)
        viewModel.dismissBackupPasswordDialog()
        assertFalse(viewModel.uiState.value.showBackupPasswordDialog)
    }

    @Test
    fun `setGeckoSafeBrowsing delegates to themeManager`() = runTest {
        viewModel.setGeckoSafeBrowsing(true)
        advanceUntilIdle()
        coVerify(exactly = 1) { themeManager.setGeckoSafeBrowsing(true) }
    }

    @Test
    fun `geckoSafeBrowsing state reflects themeManager flow`() = runTest {
        val flow = MutableStateFlow(false)
        every { themeManager.geckoSafeBrowsing } returns flow
        val vm = GlobalSettingsViewModel(
            themeManager = themeManager,
            isolationManager = isolationManager,
            getWebApps = getWebApps,
            saveWebApp = saveWebApp,
            deleteAllAppsUseCase = deleteAllAppsUseCase,
            deleteAllCategoriesUseCase = deleteAllCategoriesUseCase,
            passwordManager = passwordManager,
            backupSettings = backupSettings,
            backupManager = backupManager,
            context = context,
            geckoEngineManager = geckoEngineManager,
            simpleIconsManager = simpleIconsManager,
        )
        advanceUntilIdle()
        assertFalse(vm.uiState.value.geckoSafeBrowsing)

        flow.value = true
        advanceUntilIdle()
        assertTrue(vm.uiState.value.geckoSafeBrowsing)
    }

    @Test
    fun `setGeckoSafeBrowsing applies to geckoEngineManager immediately on init`() = runTest {
        every { themeManager.geckoSafeBrowsing } returns MutableStateFlow(true)
        val vm = GlobalSettingsViewModel(
            themeManager = themeManager,
            isolationManager = isolationManager,
            getWebApps = getWebApps,
            saveWebApp = saveWebApp,
            deleteAllAppsUseCase = deleteAllAppsUseCase,
            deleteAllCategoriesUseCase = deleteAllCategoriesUseCase,
            passwordManager = passwordManager,
            backupSettings = backupSettings,
            backupManager = backupManager,
            context = context,
            geckoEngineManager = geckoEngineManager,
            simpleIconsManager = simpleIconsManager,
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.geckoSafeBrowsing)
        coVerify(atLeast = 1) { geckoEngineManager.applySafeBrowsing(true) }
    }

    @Test
    fun `setGlobalNotificationsEnabled true delegates to themeManager`() = runTest {
        viewModel.setGlobalNotificationsEnabled(true)
        advanceUntilIdle()
        coVerify(exactly = 1) { themeManager.setGlobalNotificationsEnabled(true) }
    }

    @Test
    fun `setGlobalNotificationsEnabled false delegates to themeManager`() = runTest {
        viewModel.setGlobalNotificationsEnabled(false)
        advanceUntilIdle()
        coVerify(exactly = 1) { themeManager.setGlobalNotificationsEnabled(false) }
    }

    @Test
    fun `globalNotificationsEnabled state reflects themeManager flow`() = runTest {
        val flow = MutableStateFlow(false)
        every { themeManager.globalNotificationsEnabled } returns flow
        val vm = GlobalSettingsViewModel(
            themeManager = themeManager,
            isolationManager = isolationManager,
            getWebApps = getWebApps,
            saveWebApp = saveWebApp,
            deleteAllAppsUseCase = deleteAllAppsUseCase,
            deleteAllCategoriesUseCase = deleteAllCategoriesUseCase,
            passwordManager = passwordManager,
            backupSettings = backupSettings,
            backupManager = backupManager,
            context = context,
            geckoEngineManager = geckoEngineManager,
            simpleIconsManager = simpleIconsManager,
        )
        advanceUntilIdle()
        assertFalse(vm.uiState.value.globalNotificationsEnabled)

        flow.value = true
        advanceUntilIdle()
        assertTrue(vm.uiState.value.globalNotificationsEnabled)
    }

    @Test
    fun `uninstallGeckoEngine switches default engine to SYSTEM_WEBVIEW when GECKOVIEW`() = runTest {
        // Inject GECKOVIEW as default engine via state flow
        every { themeManager.defaultEngineType } returns MutableStateFlow(EngineType.GECKOVIEW)
        val vm = GlobalSettingsViewModel(
            themeManager = themeManager,
            isolationManager = isolationManager,
            getWebApps = getWebApps,
            saveWebApp = saveWebApp,
            deleteAllAppsUseCase = deleteAllAppsUseCase,
            deleteAllCategoriesUseCase = deleteAllCategoriesUseCase,
            passwordManager = passwordManager,
            backupSettings = backupSettings,
            backupManager = backupManager,
            context = context,
            geckoEngineManager = geckoEngineManager,
            simpleIconsManager = simpleIconsManager,
        )
        advanceUntilIdle()
        vm.uninstallGeckoEngine()
        advanceUntilIdle()
        coVerify(atLeast = 1) { themeManager.setDefaultEngineType(EngineType.SYSTEM_WEBVIEW) }
    }
}
