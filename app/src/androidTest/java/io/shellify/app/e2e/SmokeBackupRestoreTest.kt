package io.shellify.app.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shellify.app.core.backup.BackupCrypto
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.iconpack.SimpleIconsState
import io.shellify.app.presentation.settings.GlobalSettingsScreen
import io.shellify.app.presentation.settings.GlobalSettingsUiState
import io.shellify.app.presentation.settings.GlobalSettingsViewModel
import io.shellify.app.presentation.theme.ShellifyTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException

/**
 * E2E smoke tests for backup and restore.
 *
 * Two layers:
 *  1. BackupCrypto — AES-GCM encrypt/decrypt round-trip, wrong-password rejection
 *  2. GlobalSettingsScreen — backup section UI states (enabled, disabled, running)
 */
@RunWith(AndroidJUnit4::class)
class SmokeBackupRestoreTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── BackupCrypto round-trip ───────────────────────────────────────────────

    @Test
    fun backupCrypto_encryptDecrypt_roundTrip() {
        val original = "Hello, Shellify backup!".toByteArray()
        val password = "strong-backup-password"

        val encrypted = BackupCrypto.encrypt(original, password)
        val decrypted = BackupCrypto.decrypt(encrypted, password)

        assertArrayEquals("Decrypted bytes must match original", original, decrypted)
    }

    @Test
    fun backupCrypto_encryptedOutput_isLargerThanInput() {
        val data = ByteArray(1024) { it.toByte() }
        val encrypted = BackupCrypto.encrypt(data, "password")
        // Output = salt(32) + iv(12) + ciphertext(input) + GCM tag(16)
        assertTrue("Encrypted size must exceed plaintext", encrypted.size > data.size)
    }

    @Test
    fun backupCrypto_samePlaintext_producesDifferentCiphertexts() {
        val data = "same content".toByteArray()
        val enc1 = BackupCrypto.encrypt(data, "pw")
        val enc2 = BackupCrypto.encrypt(data, "pw")
        // Different random salts and IVs each time
        assert(!enc1.contentEquals(enc2)) { "Two encryptions of same data should differ" }
        // But both should decrypt correctly
        assertArrayEquals(data, BackupCrypto.decrypt(enc1, "pw"))
        assertArrayEquals(data, BackupCrypto.decrypt(enc2, "pw"))
    }

    @Test
    fun backupCrypto_wrongPassword_throwsException() {
        val data = "sensitive backup data".toByteArray()
        val encrypted = BackupCrypto.encrypt(data, "correct-password")

        var threw = false
        try {
            BackupCrypto.decrypt(encrypted, "wrong-password")
        } catch (e: BadPaddingException) {
            threw = true
        } catch (e: IllegalBlockSizeException) {
            threw = true
        } catch (e: Exception) {
            // Some AES-GCM implementations wrap in AEADBadTagException which extends BadPaddingException
            threw = true
        }
        assertTrue("Decrypting with wrong password must throw", threw)
    }

    @Test
    fun backupCrypto_truncatedCiphertext_throwsException() {
        val data = "backup payload".toByteArray()
        val encrypted = BackupCrypto.encrypt(data, "password")
        val truncated = encrypted.copyOf(encrypted.size / 2)

        var threw = false
        try {
            BackupCrypto.decrypt(truncated, "password")
        } catch (e: Exception) {
            threw = true
        }
        assertTrue("Decrypting truncated ciphertext must throw", threw)
    }

    // ── GlobalSettingsScreen: backup UI states ────────────────────────────────

    @Test
    fun globalSettings_backupSectionHeader_isVisible() {
        setScreen(GlobalSettingsUiState(isLoaded = true))
        composeTestRule.onNodeWithText("Backup").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun globalSettings_encryptedBackupLabel_isVisible() {
        setScreen(GlobalSettingsUiState(isLoaded = true))
        composeTestRule.onNodeWithText("Encrypted backup").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun globalSettings_backupDisabled_showsDisabledStatus() {
        setScreen(GlobalSettingsUiState(isLoaded = true, backupEnabled = false))
        composeTestRule.onNodeWithText("Disabled").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun globalSettings_backupEnabled_showsEnabledStatus() {
        setScreen(GlobalSettingsUiState(isLoaded = true, backupEnabled = true))
        composeTestRule.onNodeWithText("Enabled").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun globalSettings_importBackupButton_isVisible() {
        setScreen(GlobalSettingsUiState(isLoaded = true))
        composeTestRule
            .onNodeWithContentDescription("Import Backup")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun setScreen(state: GlobalSettingsUiState) {
        val geckoManager = mockk<GeckoEngineManager>(relaxed = true)
        every { geckoManager.installState } returns MutableStateFlow(GeckoInstallState.NotInstalled)
        every { geckoManager.latestVersion } returns MutableStateFlow(null)

        val iconsManager = mockk<SimpleIconsManager>(relaxed = true)
        every { iconsManager.state } returns MutableStateFlow(SimpleIconsState.NotImported)

        val vm = mockk<GlobalSettingsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state)
        every { vm.geckoEngineManager } returns geckoManager
        every { vm.simpleIconsManager } returns iconsManager

        composeTestRule.setContent {
            ShellifyTheme { GlobalSettingsScreen(viewModel = vm) }
        }
    }
}
