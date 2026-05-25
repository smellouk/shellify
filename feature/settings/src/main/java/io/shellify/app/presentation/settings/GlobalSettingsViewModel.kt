package io.shellify.app.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.shellify.app.core.backup.BackupManager
import io.shellify.app.core.backup.BackupSchedule
import io.shellify.app.core.backup.BackupScheduler
import io.shellify.app.core.backup.BackupSettings
import io.shellify.app.domain.model.EngineType
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.security.verifyPassword
import io.shellify.app.core.shortcut.PwaShortcutManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import io.shellify.core.ui.R as CoreUiR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PasswordDialogMode { SET, CHANGE, REMOVE }

data class GlobalSettingsUiState(
    val isLoaded: Boolean = false,
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accentColor: Int? = null,
    val defaultUaMode: UserAgentMode = UserAgentMode.CHROME_MOBILE,
    val defaultEngineType: EngineType = EngineType.SYSTEM_WEBVIEW,
    val geckoSafeBrowsing: Boolean = false,
    // Notifications
    val globalNotificationsEnabled: Boolean = false,
    // App lock password
    val hasPassword: Boolean = false,
    val wipeOnFailedAttempts: Boolean = false,
    val screenshotProtection: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val passwordDialogMode: PasswordDialogMode = PasswordDialogMode.SET,
    // Remove password warning
    val showRemovePasswordWarning: Boolean = false,
    // Backup
    val backupEnabled: Boolean = false,
    val backupHasPassword: Boolean = false,
    val backupDirectoryUri: String? = null,
    val backupSchedule: BackupSchedule = BackupSchedule.NONE,
    val backupLastTime: Long = 0L,
    val backupRunning: Boolean = false,
    val backupResultMessage: String? = null,
    val restoreComplete: Boolean = false,
    val showBackupPasswordDialog: Boolean = false,
    val showRestoreConfirmDialog: Boolean = false,
    val showImportPasswordDialog: Boolean = false,
    val importSourceUri: Uri? = null,
)

class GlobalSettingsViewModel(
    private val themeManager: ThemeManager,
    private val isolationManager: IsolationManager,
    private val getWebApps: GetWebAppsUseCase,
    private val saveWebApp: SaveWebAppUseCase,
    private val passwordManager: PasswordManager,
    private val backupSettings: BackupSettings,
    private val backupManager: BackupManager,
    private val context: Context,
    val geckoEngineManager: GeckoEngineManager,
    val simpleIconsManager: SimpleIconsManager,
    private val onGeckoInstalled: () -> Unit = {},
) : ViewModel() {

    private val _state = MutableStateFlow(GlobalSettingsUiState())
    val uiState: StateFlow<GlobalSettingsUiState> = _state

    init {
        // Load all values atomically so the UI renders once with complete data
        viewModelScope.launch {
            _state.update {
                it.copy(
                    themeMode = themeManager.themeMode.first(),
                    dynamicColor = themeManager.dynamicColor.first(),
                    accentColor = themeManager.accentColor.first(),
                    defaultUaMode = themeManager.defaultUaMode.first(),
                    defaultEngineType = themeManager.defaultEngineType.first(),
                    geckoSafeBrowsing = themeManager.geckoSafeBrowsing.first().also { v ->
                        geckoEngineManager.applySafeBrowsing(v)
                    },
                    globalNotificationsEnabled = themeManager.globalNotificationsEnabled.first(),
                    hasPassword = passwordManager.passwordHash.first() != null,
                    wipeOnFailedAttempts = passwordManager.wipeOnFailedAttempts.first(),
                    screenshotProtection = passwordManager.screenshotProtection.first(),
                    backupEnabled = backupSettings.enabled.first(),
                    backupHasPassword = backupSettings.hasPassword.first(),
                    backupDirectoryUri = backupSettings.directoryUri.first(),
                    backupSchedule = backupSettings.schedule.first(),
                    backupLastTime = backupSettings.lastBackupTime.first(),
                    isLoaded = true,
                )
            }
        }
        // Continue watching for live updates after the initial load
        viewModelScope.launch {
            themeManager.themeMode.collect { v -> _state.update { it.copy(themeMode = v) } }
        }
        viewModelScope.launch {
            themeManager.dynamicColor.collect { v -> _state.update { it.copy(dynamicColor = v) } }
        }
        viewModelScope.launch {
            themeManager.accentColor.collect { v -> _state.update { it.copy(accentColor = v) } }
        }
        viewModelScope.launch {
            themeManager.defaultUaMode.collect { v -> _state.update { it.copy(defaultUaMode = v) } }
        }
        viewModelScope.launch {
            themeManager.defaultEngineType.collect { v -> _state.update { it.copy(defaultEngineType = v) } }
        }
        viewModelScope.launch {
            themeManager.geckoSafeBrowsing.collect { v ->
                geckoEngineManager.applySafeBrowsing(v)
                _state.update { it.copy(geckoSafeBrowsing = v) }
            }
        }
        viewModelScope.launch {
            themeManager.globalNotificationsEnabled.collect { enabled ->
                _state.update { it.copy(globalNotificationsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            passwordManager.passwordHash.collect { h -> _state.update { it.copy(hasPassword = h != null) } }
        }
        viewModelScope.launch {
            passwordManager.wipeOnFailedAttempts.collect { v ->
                _state.update {
                    it.copy(
                        wipeOnFailedAttempts = v
                    )
                }
            }
        }
        viewModelScope.launch {
            passwordManager.screenshotProtection.collect { v ->
                _state.update {
                    it.copy(
                        screenshotProtection = v
                    )
                }
            }
        }
        viewModelScope.launch {
            backupSettings.enabled.collect { v -> _state.update { it.copy(backupEnabled = v) } }
        }
        viewModelScope.launch {
            backupSettings.hasPassword.collect { v -> _state.update { it.copy(backupHasPassword = v) } }
        }
        viewModelScope.launch {
            backupSettings.directoryUri.collect { v -> _state.update { it.copy(backupDirectoryUri = v) } }
        }
        viewModelScope.launch {
            backupSettings.schedule.collect { v -> _state.update { it.copy(backupSchedule = v) } }
        }
        viewModelScope.launch {
            backupSettings.lastBackupTime.collect { v -> _state.update { it.copy(backupLastTime = v) } }
        }
    }

    // ── Appearance ────────────────────────────────────────────────────────────

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { themeManager.setThemeMode(mode) }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { themeManager.setDynamicColor(v) }
    fun setAccentColor(color: Int?) = viewModelScope.launch { themeManager.setAccentColor(color) }
    fun setDefaultUaMode(mode: UserAgentMode) =
        viewModelScope.launch { themeManager.setDefaultUaMode(mode) }

    fun setDefaultEngineType(engine: EngineType) =
        viewModelScope.launch { themeManager.setDefaultEngineType(engine) }

    fun setGeckoSafeBrowsing(v: Boolean) =
        viewModelScope.launch { themeManager.setGeckoSafeBrowsing(v) }

    // ── Notifications ─────────────────────────────────────────────────────────

    fun setGlobalNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        themeManager.setGlobalNotificationsEnabled(enabled)
    }

    // ── App lock password ─────────────────────────────────────────────────────

    fun showSetPasswordDialog() =
        _state.update {
            it.copy(
                showPasswordDialog = true,
                passwordDialogMode = PasswordDialogMode.SET
            )
        }

    fun showChangePasswordDialog() =
        _state.update {
            it.copy(
                showPasswordDialog = true,
                passwordDialogMode = PasswordDialogMode.CHANGE
            )
        }

    fun showRemovePasswordDialog() =
        _state.update { it.copy(showRemovePasswordWarning = true) }

    fun dismissRemovePasswordWarning() =
        _state.update { it.copy(showRemovePasswordWarning = false) }

    fun confirmRemovePasswordWarning() =
        _state.update {
            it.copy(
                showRemovePasswordWarning = false,
                showPasswordDialog = true,
                passwordDialogMode = PasswordDialogMode.REMOVE
            )
        }

    fun dismissPasswordDialog() = _state.update { it.copy(showPasswordDialog = false) }

    fun setWipeOnFailedAttempts(v: Boolean) =
        viewModelScope.launch { passwordManager.setWipeOnFailedAttempts(v) }

    fun setScreenshotProtection(v: Boolean) =
        viewModelScope.launch { passwordManager.setScreenshotProtection(v) }

    fun setPassword(password: String) = viewModelScope.launch {
        passwordManager.setPassword(password)
        _state.update { it.copy(showPasswordDialog = false) }
    }

    fun removePassword(currentPassword: String, onWrongPassword: () -> Unit) =
        viewModelScope.launch {
            val hash = passwordManager.passwordHash.first()
            if (hash != null && verifyPassword(currentPassword, hash)) {
                getWebApps().first()
                    .filter { it.lockType != LockType.NONE }
                    .forEach { app ->
                        isolationManager.clearData(app.isolationId)
                        saveWebApp(app.copy(lockType = LockType.NONE))
                    }
                passwordManager.clearPassword()
                _state.update { it.copy(showPasswordDialog = false) }
            } else onWrongPassword()
        }

    fun changePassword(currentPassword: String, newPassword: String, onWrongPassword: () -> Unit) =
        viewModelScope.launch {
            val hash = passwordManager.passwordHash.first()
            if (hash != null && verifyPassword(currentPassword, hash)) {
                passwordManager.setPassword(newPassword)
                _state.update { it.copy(showPasswordDialog = false) }
            } else onWrongPassword()
        }

    fun repinShortcutsAfterRestore() = viewModelScope.launch {
        getWebApps().first()
            .filter { it.hasLauncherShortcut }
            .forEach { app -> PwaShortcutManager.createShortcut(context, app) }
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    fun setBackupEnabled(v: Boolean) = viewModelScope.launch {
        backupSettings.setEnabled(v)
        if (!v) BackupScheduler.schedule(context, BackupSchedule.NONE)
    }

    fun setBackupPassword(password: String) = viewModelScope.launch {
        backupSettings.setPassword(password)
        _state.update { it.copy(showBackupPasswordDialog = false) }
    }

    fun setBackupDirectory(uri: Uri) = viewModelScope.launch {
        backupSettings.setDirectoryUri(uri.toString())
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    fun setBackupSchedule(s: BackupSchedule) = viewModelScope.launch {
        backupSettings.setSchedule(s)
        BackupScheduler.schedule(context, s)
    }

    fun backupNow() = viewModelScope.launch {
        val password = backupSettings.getPassword()
            ?: return@launch _state.update {
                it.copy(backupResultMessage = context.getString(CoreUiR.string.settings_backup_error_no_password))
            }
        val uriStr = backupSettings.directoryUri.first()
            ?: return@launch _state.update {
                it.copy(backupResultMessage = context.getString(CoreUiR.string.settings_backup_error_no_folder))
            }

        _state.update { it.copy(backupRunning = true, backupResultMessage = null) }
        backupManager.backup(password, Uri.parse(uriStr)).fold(
            onSuccess = { name ->
                backupSettings.setLastBackupTime(System.currentTimeMillis())
                _state.update {
                    it.copy(
                        backupRunning = false,
                        backupResultMessage = context.getString(CoreUiR.string.settings_backup_success, name)
                    )
                }
            },
            onFailure = { e ->
                _state.update {
                    it.copy(
                        backupRunning = false,
                        backupResultMessage = context.getString(CoreUiR.string.settings_backup_failed, e.message)
                    )
                }
            },
        )
    }

    fun showImportDialog(uri: Uri) =
        _state.update { it.copy(showRestoreConfirmDialog = true, importSourceUri = uri) }

    fun confirmRestore() =
        _state.update { it.copy(showRestoreConfirmDialog = false, showImportPasswordDialog = true) }

    fun dismissRestoreConfirm() =
        _state.update { it.copy(showRestoreConfirmDialog = false, importSourceUri = null) }

    fun dismissImportDialog() =
        _state.update { it.copy(showImportPasswordDialog = false, importSourceUri = null) }

    fun importBackup(password: String) = viewModelScope.launch {
        val uri = _state.value.importSourceUri ?: return@launch
        _state.update { it.copy(backupRunning = true, showImportPasswordDialog = false) }
        backupManager.restore(password, uri).fold(
            onSuccess = {
                _state.update { it.copy(backupRunning = false, restoreComplete = true) }
            },
            onFailure = { e ->
                _state.update {
                    it.copy(
                        backupRunning = false,
                        backupResultMessage = context.getString(CoreUiR.string.settings_restore_failed, e.message)
                    )
                }
            },
        )
    }

    fun clearBackupMessage() = _state.update { it.copy(backupResultMessage = null) }
    fun clearRestoreComplete() = _state.update { it.copy(restoreComplete = false) }

    fun showBackupPasswordDialog() = _state.update { it.copy(showBackupPasswordDialog = true) }
    fun dismissBackupPasswordDialog() = _state.update { it.copy(showBackupPasswordDialog = false) }

    // ── GeckoView engine ──────────────────────────────────────────────────────

    fun installGeckoEngine() = viewModelScope.launch {
        val ok = geckoEngineManager.downloadAndInstall()
        if (ok) onGeckoInstalled()
    }

    fun cancelGeckoInstall() {
        geckoEngineManager.cancelDownload()
    }

    fun uninstallGeckoEngine() {
        if (_state.value.defaultEngineType == EngineType.GECKOVIEW) {
            viewModelScope.launch { themeManager.setDefaultEngineType(EngineType.SYSTEM_WEBVIEW) }
        }
        geckoEngineManager.uninstall()
    }

    fun checkForGeckoUpdate() = viewModelScope.launch { geckoEngineManager.checkForUpdate() }

    fun updateGeckoEngine() = viewModelScope.launch { geckoEngineManager.updateEngine() }

    // ── Simple Icons ──────────────────────────────────────────────────────────

    fun downloadSimpleIcons() = viewModelScope.launch { simpleIconsManager.download() }

    fun importSimpleIcons(uri: android.net.Uri) =
        viewModelScope.launch { simpleIconsManager.importFromUri(uri) }

    fun removeSimpleIcons() = simpleIconsManager.remove()
}
