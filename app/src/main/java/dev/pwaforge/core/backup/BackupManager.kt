package dev.pwaforge.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import dev.pwaforge.core.iconpack.SimpleIconsManager
import dev.pwaforge.core.iconpack.SimpleIconsState
import dev.pwaforge.core.isolation.CookieJarManager
import dev.pwaforge.core.security.PasswordManager
import dev.pwaforge.core.theme.ThemeManager
import dev.pwaforge.data.local.AppDatabase
import dev.pwaforge.domain.model.EngineType
import dev.pwaforge.domain.model.UserAgentMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val cookieJarManager: CookieJarManager,
    private val themeManager: ThemeManager,
    private val passwordManager: PasswordManager,
    private val backupSettings: BackupSettings,
    private val simpleIconsManager: SimpleIconsManager,
) {

    // ── Public API ────────────────────────────────────────────────────────────

    /** Creates an encrypted .pwab file inside [directoryUri] (SAF tree URI). Returns the filename. */
    suspend fun backup(password: String, directoryUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val zipBytes = buildZipArchive()
            val encrypted = BackupCrypto.encrypt(zipBytes, password)
            val filename = "pwaforge_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pwab"
            val dir = DocumentFile.fromTreeUri(context, directoryUri)
                ?: error("Cannot open backup directory")
            val file = dir.createFile("application/octet-stream", filename)
                ?: error("Cannot create backup file in selected directory")
            context.contentResolver.openOutputStream(file.uri)!!.use { it.write(encrypted) }
            filename
        }
    }

    /** Decrypts and restores a .pwab file from [sourceUri]. */
    suspend fun restore(password: String, sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val encrypted = context.contentResolver.openInputStream(sourceUri)!!.readBytes()
            val zipBytes = try {
                BackupCrypto.decrypt(encrypted, password)
            } catch (e: Exception) {
                error("Wrong password or corrupted backup")
            }
            applyZipArchive(zipBytes)
        }
    }

    // ── Build archive ─────────────────────────────────────────────────────────

    private suspend fun buildZipArchive(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->

            // Metadata
            zip.entry("meta.json") {
                JSONObject().apply {
                    put("version", 1)
                    put("timestamp", System.currentTimeMillis())
                    put("package", context.packageName)
                    put("apiLevel", Build.VERSION.SDK_INT)
                }.toString().toByteArray()
            }

            // Database — portable SQL dump (works across devices with different SQLCipher keys)
            zip.entry("db/dump.sql") { exportDatabaseSql().toByteArray(Charsets.UTF_8) }

            // Icons
            File(context.filesDir, "icons").walkSafe { file, relative ->
                zip.entry("icons/$relative") { file.readBytes() }
            }

            // Icon pack data
            File(context.filesDir, "icon_packs").walkSafe { file, relative ->
                zip.entry("icon_packs/$relative") { file.readBytes() }
            }

            // DataStore binary files — kept for the encrypted backup password (device-specific)
            val datastoreDir = File(context.filesDir.parent!!, "datastore")
            listOf(
                "pwa_theme.preferences_pb",
                "pwa_password.preferences_pb",
                "pwa_backup.preferences_pb",
            ).forEach { name ->
                val f = File(datastoreDir, name)
                if (f.exists()) zip.entry("datastore/$name") { f.readBytes() }
            }

            // Settings snapshot — applied via manager APIs on restore so DataStore flows update
            // immediately without a process restart.
            zip.entry("settings.json") { buildSettingsJson().toByteArray(Charsets.UTF_8) }

            // SharedPreferences — user locale + icon pack import state
            val prefsDir = File(context.filesDir.parent!!, "shared_prefs")
            listOf("locale_prefs.xml", "simple_icons.xml").forEach { name ->
                val f = File(prefsDir, name)
                if (f.exists()) zip.entry("shared_prefs/$name") { f.readBytes() }
            }

            // Cookies — decrypted for cross-device portability, re-encrypted by BackupCrypto
            val cookies = cookieJarManager.exportAll()
            if (cookies.isNotEmpty()) {
                zip.entry("cookies.json") {
                    JSONObject(cookies).toString().toByteArray(Charsets.UTF_8)
                }
            }

            // WebView profile directories (API 33+) — best-effort binary copy
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val profilesDir = File(context.filesDir.parent!!, "app_webview/profiles")
                profilesDir.walkSafe { file, relative ->
                    zip.entry("webview/$relative") { file.readBytes() }
                }
            }
        }
        return out.toByteArray()
    }

    private suspend fun buildSettingsJson(): String {
        val themeMode      = themeManager.themeMode.first()
        val dynamicColor   = themeManager.dynamicColor.first()
        val accentColor    = themeManager.accentColor.first()
        val defaultUaMode  = themeManager.defaultUaMode.first()
        val defaultEngine  = themeManager.defaultEngineType.first()
        val languageCode   = themeManager.languageCode.first()
        val wipe           = passwordManager.wipeOnFailedAttempts.first()
        val screenshot     = passwordManager.screenshotProtection.first()
        val passwordHash   = passwordManager.passwordHash.first()
        val bkEnabled      = backupSettings.enabled.first()
        val bkSchedule     = backupSettings.schedule.first()
        val bkDirectory    = backupSettings.directoryUri.first()
        val bkLastTime     = backupSettings.lastBackupTime.first()
        val bkPasswordEnc  = backupSettings.getEncryptedPassword()
        val iconsState     = simpleIconsManager.state.value
        val iconsImported  = iconsState is SimpleIconsState.Imported
        val iconsCount     = if (iconsImported) (iconsState as SimpleIconsState.Imported).iconCount else 0

        return JSONObject().apply {
            put("theme_mode",              themeMode.name)
            put("dynamic_color",           dynamicColor)
            if (accentColor != null)  put("accent_color", accentColor)
            put("default_ua_mode",         defaultUaMode.name)
            put("default_engine",          defaultEngine.name)
            put("language_code",           languageCode)
            put("wipe_on_failed_attempts", wipe)
            put("screenshot_protection",   screenshot)
            if (passwordHash != null) put("password_hash", passwordHash)
            put("backup_enabled",          bkEnabled)
            put("backup_schedule",         bkSchedule.name)
            if (bkDirectory != null)  put("backup_directory_uri", bkDirectory)
            put("backup_last_time",        bkLastTime)
            if (bkPasswordEnc != null) put("backup_password_encrypted", bkPasswordEnc)
            put("simple_icons_imported",   iconsImported)
            put("simple_icons_count",      iconsCount)
        }.toString()
    }

    // ── Apply archive ─────────────────────────────────────────────────────────

    private suspend fun clearExistingData() {
        // Database
        val db = database.openHelper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM `web_apps`")
            db.execSQL("DELETE FROM `categories`")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        // Files
        File(context.filesDir, "icons").deleteRecursively()
        File(context.filesDir, "icon_packs").deleteRecursively()

        // SharedPreferences
        val prefsDir = File(context.filesDir.parent!!, "shared_prefs")
        listOf("locale_prefs.xml", "simple_icons.xml").forEach {
            File(prefsDir, it).delete()
        }

        // Cookies
        cookieJarManager.importAll(emptyMap())

        // WebView profiles
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            File(context.filesDir.parent!!, "app_webview/profiles").deleteRecursively()
        }
    }

    private suspend fun applyZipArchive(zipBytes: ByteArray) {
        clearExistingData()
        val cookieMap = mutableMapOf<String, String>()

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val bytes = zip.readBytes()
                    when {
                        entry.name == "db/dump.sql" ->
                            restoreDatabaseSql(bytes.toString(Charsets.UTF_8))

                        entry.name.startsWith("icons/") -> {
                            val name = entry.name.removePrefix("icons/")
                            if (name.isNotBlank()) {
                                val dir = File(context.filesDir, "icons").also { it.mkdirs() }
                                File(dir, name).writeBytes(bytes)
                            }
                        }

                        entry.name.startsWith("icon_packs/") -> {
                            val name = entry.name.removePrefix("icon_packs/")
                            if (name.isNotBlank()) {
                                val dir = File(context.filesDir, "icon_packs").also { it.mkdirs() }
                                File(dir, name).writeBytes(bytes)
                            }
                        }

                        entry.name.startsWith("datastore/") -> {
                            val name = entry.name.removePrefix("datastore/")
                            if (name.isNotBlank()) {
                                val dir = File(context.filesDir.parent!!, "datastore").also { it.mkdirs() }
                                val file = File(dir, name).also { it.writeBytes(bytes) }
                                // Reload in-memory state for the backup password (device-specific
                                // Keystore-encrypted value that can't go into settings.json).
                                when (name) {
                                    "pwa_backup.preferences_pb" -> backupSettings.reloadFromFile(file)
                                }
                            }
                        }

                        entry.name == "settings.json" ->
                            applySettingsJson(bytes.toString(Charsets.UTF_8))

                        entry.name.startsWith("shared_prefs/") -> {
                            val name = entry.name.removePrefix("shared_prefs/")
                            if (name.isNotBlank()) {
                                val dir = File(context.filesDir.parent!!, "shared_prefs").also { it.mkdirs() }
                                File(dir, name).writeBytes(bytes)
                            }
                        }

                        entry.name == "cookies.json" -> {
                            val json = JSONObject(bytes.toString(Charsets.UTF_8))
                            json.keys().forEach { key -> cookieMap[key] = json.getString(key) }
                        }

                        entry.name.startsWith("webview/") &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                            val relative = entry.name.removePrefix("webview/")
                            if (relative.isNotBlank()) {
                                val target = File(context.filesDir.parent!!, "app_webview/$relative")
                                target.parentFile?.mkdirs()
                                target.writeBytes(bytes)
                            }
                        }
                    }
                }
                entry = zip.nextEntry
            }
        }

        if (cookieMap.isNotEmpty()) cookieJarManager.importAll(cookieMap)
    }

    private suspend fun applySettingsJson(json: String) {
        val obj = JSONObject(json)

        // Theme
        runCatching { themeManager.setThemeMode(dev.pwaforge.core.theme.ThemeMode.valueOf(obj.getString("theme_mode"))) }
        runCatching { themeManager.setDynamicColor(obj.getBoolean("dynamic_color")) }
        runCatching { themeManager.setAccentColor(if (obj.has("accent_color")) obj.getInt("accent_color") else null) }
        runCatching { themeManager.setDefaultUaMode(UserAgentMode.valueOf(obj.getString("default_ua_mode"))) }
        runCatching { themeManager.setDefaultEngineType(EngineType.valueOf(obj.getString("default_engine"))) }
        runCatching { themeManager.setLanguageCode(obj.optString("language_code", "en")) }

        // Security
        runCatching { passwordManager.setWipeOnFailedAttempts(obj.getBoolean("wipe_on_failed_attempts")) }
        runCatching { passwordManager.setScreenshotProtection(obj.getBoolean("screenshot_protection")) }
        if (obj.has("password_hash")) {
            runCatching { passwordManager.restorePasswordHash(obj.getString("password_hash")) }
        } else {
            passwordManager.clearPassword()
        }

        // Backup settings
        runCatching { backupSettings.setEnabled(obj.getBoolean("backup_enabled")) }
        runCatching { backupSettings.setSchedule(BackupSchedule.valueOf(obj.getString("backup_schedule"))) }
        val bkDir = obj.optString("backup_directory_uri", "")
        if (bkDir.isNotEmpty()) {
            // Re-take the SAF write permission. This succeeds on same-device restores where the
            // original grant still exists; it throws SecurityException after reinstall or on a
            // different device. In that case we leave the directory unset so the user re-selects.
            val permGranted = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(bkDir),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }.isSuccess
            if (permGranted) runCatching { backupSettings.setDirectoryUri(bkDir) }
        }
        val bkLastTime = obj.optLong("backup_last_time", 0L)
        if (bkLastTime > 0L) runCatching { backupSettings.setLastBackupTime(bkLastTime) }
        val bkPasswordEnc = obj.optString("backup_password_encrypted", "")
        if (bkPasswordEnc.isNotEmpty()) runCatching { backupSettings.setEncryptedPassword(bkPasswordEnc) }

        // Simple Icons
        val iconsImported = obj.optBoolean("simple_icons_imported", false)
        val iconsCount = obj.optInt("simple_icons_count", 0)
        simpleIconsManager.restoreState(iconsImported, iconsCount)
    }

    // ── Database helpers ──────────────────────────────────────────────────────

    private fun exportDatabaseSql(): String {
        val sb = StringBuilder()
        val db = database.openHelper.readableDatabase
        listOf("categories", "web_apps").forEach { table ->
            val cursor = db.query("SELECT * FROM `$table`")
            cursor.use {
                val cols = it.columnNames
                while (it.moveToNext()) {
                    val values = cols.indices.map { i ->
                        val v = it.getString(i)
                        if (v == null) "NULL" else "'${v.replace("'", "''")}'"
                    }
                    sb.appendLine(
                        "INSERT OR REPLACE INTO `$table` " +
                        "(${cols.joinToString(",") { "`$it`" }}) " +
                        "VALUES (${values.joinToString(",")});"
                    )
                }
            }
        }
        return sb.toString()
    }

    private fun restoreDatabaseSql(sql: String) {
        val db = database.openHelper.writableDatabase
        db.beginTransaction()
        try {
            sql.lineSequence().filter { it.isNotBlank() }.forEach { stmt ->
                runCatching { db.execSQL(stmt) }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private inline fun ZipOutputStream.entry(name: String, block: () -> ByteArray) {
        putNextEntry(ZipEntry(name))
        write(block())
        closeEntry()
    }

    private inline fun File.walkSafe(crossinline action: (file: File, relative: String) -> Unit) {
        if (!exists()) return
        walkTopDown().filter { it.isFile }.forEach { file ->
            runCatching { action(file, file.relativeTo(this).path) }
        }
    }
}
