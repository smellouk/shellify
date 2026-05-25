package io.shellify.app.core.theme

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.UserAgentMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.themeStore by preferencesDataStore(name = "pwa_theme")

class ThemeManager(private val context: Context) {

    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyDynamicColor = booleanPreferencesKey("dynamic_color")
    private val keyDefaultUa = stringPreferencesKey("default_ua")
    private val keyDefaultEngine = stringPreferencesKey("default_engine")
    private val keyGeckoSafeBrowsing = booleanPreferencesKey("gecko_safe_browsing")
    private val keyGlobalNotificationsEnabled = booleanPreferencesKey("global_notifications_enabled")

    val themeMode: Flow<ThemeMode> = context.themeStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[keyThemeMode] ?: "") }.getOrDefault(ThemeMode.SYSTEM)
    }

    val dynamicColor: Flow<Boolean> = context.themeStore.data.map { prefs ->
        prefs[keyDynamicColor] ?: true
    }

    val defaultUaMode: Flow<UserAgentMode> = context.themeStore.data.map { prefs ->
        runCatching {
            UserAgentMode.valueOf(
                prefs[keyDefaultUa] ?: ""
            )
        }.getOrDefault(UserAgentMode.CHROME_MOBILE)
    }

    val defaultEngineType: Flow<EngineType> = context.themeStore.data.map { prefs ->
        runCatching {
            EngineType.valueOf(
                prefs[keyDefaultEngine] ?: ""
            )
        }.getOrDefault(EngineType.SYSTEM_WEBVIEW)
    }

    val geckoSafeBrowsing: Flow<Boolean> = context.themeStore.data.map { prefs ->
        prefs[keyGeckoSafeBrowsing] ?: false
    }

    val globalNotificationsEnabled: Flow<Boolean> = context.themeStore.data.map { prefs ->
        prefs[keyGlobalNotificationsEnabled] ?: false
    }

    private val keyAccentColor = stringPreferencesKey("accent_color")

    val accentColor: Flow<Int?> = context.themeStore.data.map { prefs ->
        val raw = prefs[keyAccentColor]?.toIntOrNull() ?: -1
        if (raw == -1) null else raw
    }

    suspend fun setAccentColor(color: Int?) {
        context.themeStore.edit { it[keyAccentColor] = (color ?: -1).toString() }
    }

    private val keyLanguageCode = stringPreferencesKey("language_code")

    val languageCode: Flow<String> = context.themeStore.data.map { prefs ->
        prefs[keyLanguageCode] ?: "en"
    }

    suspend fun setLanguageCode(code: String) {
        context.themeStore.edit { it[keyLanguageCode] = code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeStore.edit { it[keyThemeMode] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeStore.edit { it[keyDynamicColor] = enabled }
    }

    suspend fun setDefaultUaMode(mode: UserAgentMode) {
        context.themeStore.edit { it[keyDefaultUa] = mode.name }
    }

    suspend fun setDefaultEngineType(engine: EngineType) {
        context.themeStore.edit { it[keyDefaultEngine] = engine.name }
    }

    suspend fun setGeckoSafeBrowsing(enabled: Boolean) {
        context.themeStore.edit { it[keyGeckoSafeBrowsing] = enabled }
    }

    suspend fun setGlobalNotificationsEnabled(enabled: Boolean) {
        context.themeStore.edit { it[keyGlobalNotificationsEnabled] = enabled }
    }

    private val keyOnboardingDone = booleanPreferencesKey("onboarding_done")
    private val keyOnboardingPage = intPreferencesKey("onboarding_page")
    private val keyConsentGiven = booleanPreferencesKey("consent_given")
    private val keyConsentVersion = intPreferencesKey("consent_version")

    val onboardingDone: Flow<Boolean> =
        context.themeStore.data.map { it[keyOnboardingDone] ?: false }
    val onboardingPage: Flow<Int> = context.themeStore.data.map { it[keyOnboardingPage] ?: 0 }
    val consentGiven: Flow<Boolean> =
        context.themeStore.data.map { it[keyConsentGiven] ?: false }

    /**
     * The version of the terms the user last accepted.
     * Existing users who accepted via the old boolean key are migrated to version 1.
     * 0 = never consented, 1 = accepted before versioning was introduced.
     */
    val consentVersion: Flow<Int> = context.themeStore.data.map { prefs ->
        prefs[keyConsentVersion] ?: if (prefs[keyConsentGiven] == true) 1 else 0
    }

    suspend fun setOnboardingDone() {
        context.themeStore.edit { it[keyOnboardingDone] = true }
    }

    suspend fun saveOnboardingPage(p: Int) {
        context.themeStore.edit { it[keyOnboardingPage] = p }
    }

    suspend fun setConsentGiven() {
        context.themeStore.edit {
            it[keyConsentGiven] = true
            it[keyConsentVersion] = CURRENT_CONSENT_VERSION
        }
    }

    suspend fun setConsentVersion(version: Int) {
        context.themeStore.edit { it[keyConsentVersion] = version }
    }

    /** Removes consent keys so the next read returns the zero/unset state. Test use only. */
    suspend fun clearConsentForTesting() {
        context.themeStore.edit { prefs ->
            prefs.remove(keyConsentGiven)
            prefs.remove(keyConsentVersion)
        }
    }

    /**
     * Sets only the legacy boolean key, leaving consent_version absent.
     * Reproduces the on-disk state of users who consented before versioning was introduced.
     * Test use only.
     */
    suspend fun seedLegacyConsentForTesting() {
        context.themeStore.edit { prefs ->
            prefs[keyConsentGiven] = true
            prefs.remove(keyConsentVersion)
        }
    }

    /**
     * Clears all DataStore preferences. Used by the panic wipe to remove all theme and
     * user preference data as part of the atomic wipe sequence (per D-02 / T-02-13).
     */
    suspend fun clearAll() {
        context.themeStore.edit { it.clear() }
    }

    companion object {
        const val CURRENT_CONSENT_VERSION = 3
    }

    /** Reads a restored DataStore file and applies its contents to the live DataStore instance. */
    suspend fun reloadFromFile(restoredFile: File) {
        val tempFile = File(context.cacheDir, "tmp_theme_restore.preferences_pb")
        restoredFile.copyTo(tempFile, overwrite = true)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val tempStore = PreferenceDataStoreFactory.create(scope = scope) { tempFile }
        try {
            val restored = tempStore.data.first()
            context.themeStore.edit { current ->
                current.clear()
                @Suppress("UNCHECKED_CAST")
                restored.asMap().forEach { (key, value) ->
                    (current as MutablePreferences)[key as Preferences.Key<Any>] = value
                }
            }
        } finally {
            tempFile.delete()
            scope.cancel()
        }
    }
}
