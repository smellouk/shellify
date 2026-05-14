package dev.pwaforge.core.engine

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.geckoview.ContentBlocking
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

sealed class GeckoInstallState {
    data object NotInstalled : GeckoInstallState()
    data class Downloading(val progress: Float, val message: String) : GeckoInstallState()
    data object Installing : GeckoInstallState()
    data class Installed(val verified: Boolean) : GeckoInstallState()
    data class Error(val message: String) : GeckoInstallState()
}

class GeckoEngineManager(private val context: Context) {

    companion object {
        private const val TAG = "GeckoEngineManager"
        private const val PREFS_NAME = "gecko_engine"
        private const val KEY_INSTALLED = "installed"
        private const val KEY_VERSION = "version"
        private const val KEY_VERIFIED = "sha256_verified"
        private const val KEY_SHA256   = "sha256_hash"

        const val GECKO_VERSION = "128.0.20240704121409"
        private const val MAVEN_BASE = "https://maven.mozilla.org/maven2/org/mozilla/geckoview"

        private val ABI_ARTIFACT = mapOf(
            "arm64-v8a"   to "geckoview-arm64-v8a",
            "armeabi-v7a" to "geckoview-armeabi-v7a",
            "x86_64"      to "geckoview-x86_64",
            "x86"         to "geckoview-x86",
        )

        // SHA-256 of the AAR for each ABI at GECKO_VERSION — fetched from maven.mozilla.org
        private val KNOWN_SHA256 = mapOf(
            "arm64-v8a"   to "23c1fb46874310d8ba2708f12022e6b3d719a8434efbb95090f4f8a8ae57621c",
            "armeabi-v7a" to "c908a15b8e3e40967deedfb70b007ddc42a479caf00077e38c0b5dc56674d6eb",
            "x86_64"      to "ff45bf56fb6fd4caed09f94e8ea95ffa3de3059e08ce2f80dbde9621753c481e",
            "x86"         to "be651ec149648afa8e4628434cb2c05ac9b7973347f26232a076a3ced79a6cfd",
        )

        // libmozglue must be loaded before libxul (dependency order)
        private val PRELOAD_ORDER = listOf("libmozglue.so", "liblgpllibs.so", "libxul.so")

        @Volatile private var sharedRuntime: GeckoRuntime? = null
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _installState = MutableStateFlow<GeckoInstallState>(
        if (isInstalled()) GeckoInstallState.Installed(verified = prefs.getBoolean(KEY_VERIFIED, false))
        else GeckoInstallState.NotInstalled
    )
    val installState: StateFlow<GeckoInstallState> = _installState.asStateFlow()

    private val _latestVersion = MutableStateFlow<String?>(null)
    val latestVersion: StateFlow<String?> = _latestVersion.asStateFlow()

    val updateAvailable: Boolean
        get() {
            val latest = _latestVersion.value ?: return false
            val installed = getInstalledVersion() ?: return false
            return isNewerVersion(candidate = latest, current = installed)
        }

    private fun isNewerVersion(candidate: String, current: String): Boolean {
        val c = candidate.split(".").mapNotNull { it.toLongOrNull() }
        val i = current.split(".").mapNotNull { it.toLongOrNull() }
        for (idx in 0 until maxOf(c.size, i.size)) {
            val cv = c.getOrElse(idx) { 0L }
            val iv = i.getOrElse(idx) { 0L }
            if (cv > iv) return true
            if (cv < iv) return false
        }
        return false
    }

    @Volatile private var cancelRequested = false

    fun isInstalled(): Boolean {
        if (!prefs.getBoolean(KEY_INSTALLED, false)) return false
        val dir = getLibsDir()
        return dir.exists() && dir.listFiles()?.any { it.extension == "so" } == true
    }

    fun getInstalledVersion(): String? = prefs.getString(KEY_VERSION, null)
    fun getInstalledSha256(): String? = prefs.getString(KEY_SHA256, null)

    fun getInstalledSizeMb(): Int {
        val dir = File(context.filesDir, "gecko_engine")
        if (!dir.exists()) return 0
        val bytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return (bytes / (1024 * 1024)).toInt()
    }

    // ── GeckoRuntime lifecycle ─────────────────────────────────────────────────

    fun getRuntime(): GeckoRuntime {
        return sharedRuntime ?: synchronized(this) {
            sharedRuntime ?: buildRuntime().also { sharedRuntime = it }
        }
    }

    private fun buildRuntime(): GeckoRuntime {
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .contentBlocking(
                ContentBlocking.Settings.Builder()
                    .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
                    .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                    .build()
            )
            .build()
        return GeckoRuntime.create(context.applicationContext, settings)
    }


    // ── Download & install ────────────────────────────────────────────────────

    suspend fun downloadAndInstall(version: String = GECKO_VERSION): Boolean = withContext(Dispatchers.IO) {
        cancelRequested = false
        val abi = Build.SUPPORTED_ABIS.firstOrNull()
            ?.takeIf { it in ABI_ARTIFACT } ?: "arm64-v8a"
        val artifact = ABI_ARTIFACT[abi]!!
        val url = "$MAVEN_BASE/$artifact/$version/$artifact-$version.aar"
        Log.i(TAG, "Downloading GeckoView from $url")

        try {
            _installState.value = GeckoInstallState.Downloading(0f, "Connecting…")
            val tempAar = File(context.cacheDir, "geckoview_temp.aar")

            val ok = downloadFile(url, tempAar) { p ->
                if (!cancelRequested)
                    _installState.value = GeckoInstallState.Downloading(p * 0.85f, "Downloading…")
            }

            if (cancelRequested) {
                tempAar.delete()
                _installState.value = GeckoInstallState.NotInstalled
                return@withContext false
            }
            if (!ok) {
                tempAar.delete()
                _installState.value = GeckoInstallState.Error("Download failed")
                return@withContext false
            }

            // ── Integrity verification ────────────────────────────────────────
            _installState.value = GeckoInstallState.Downloading(0.9f, "Verifying…")
            val expectedHash = if (version == GECKO_VERSION) {
                KNOWN_SHA256[abi]
            } else {
                fetchMavenSha256(artifact, version)
            }
            val verified: Boolean
            if (expectedHash == null) {
                Log.w(TAG, "No expected hash available for $artifact $version — skipping verification")
                verified = false
            } else {
                val actualHash = sha256(tempAar)
                if (actualHash != expectedHash) {
                    Log.e(TAG, "SHA-256 mismatch! expected=$expectedHash actual=$actualHash")
                    tempAar.delete()
                    _installState.value = GeckoInstallState.Error("Integrity check failed — download may be corrupted or tampered")
                    return@withContext false
                }
                Log.i(TAG, "SHA-256 verified: $actualHash")
                verified = true
            }

            _installState.value = GeckoInstallState.Installing
            val extracted = extractSoFiles(tempAar, abi)
            tempAar.delete()

            if (!extracted) {
                _installState.value = GeckoInstallState.Error("Extraction failed — no .so files found in AAR")
                return@withContext false
            }

            prefs.edit()
                .putBoolean(KEY_INSTALLED, true)
                .putString(KEY_VERSION, version)
                .putBoolean(KEY_VERIFIED, verified)
                .putString(KEY_SHA256, if (verified) expectedHash else null)
                .apply()
            _installState.value = GeckoInstallState.Installed(verified = verified)
            Log.i(TAG, "GeckoView installed successfully (ABI=$abi, verified=$verified)")
            true
        } catch (e: CancellationException) {
            _installState.value = GeckoInstallState.NotInstalled
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            _installState.value = GeckoInstallState.Error(e.message ?: "Unknown error")
            false
        }
    }

    suspend fun checkForUpdate(): String? = withContext(Dispatchers.IO) {
        try {
            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
            val artifact = ABI_ARTIFACT[abi] ?: ABI_ARTIFACT["arm64-v8a"]!!
            val metaUrl = "$MAVEN_BASE/$artifact/maven-metadata.xml"
            val request = Request.Builder().url(metaUrl).header("User-Agent", "Mozilla/5.0").build()
            val body = httpClient.newCall(request).execute().use { it.body?.string() } ?: return@withContext null

            // Parse <release> or last <version> from maven-metadata.xml
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser().apply { setInput(body.reader()) }
            var latest: String? = null
            var inRelease = false
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.name == "release") inRelease = true
                else if (eventType == XmlPullParser.TEXT && inRelease) { latest = xpp.text.trim(); inRelease = false }
                else if (eventType == XmlPullParser.START_TAG && xpp.name == "version") {
                    xpp.next()
                    if (xpp.eventType == XmlPullParser.TEXT) latest = xpp.text.trim()
                }
                eventType = xpp.next()
            }
            Log.i(TAG, "Latest GeckoView version: $latest (installed: ${getInstalledVersion()})")
            if (latest != null) _latestVersion.value = latest
            latest
        } catch (e: Exception) {
            Log.w(TAG, "Version check failed: ${e.message}")
            null
        }
    }

    suspend fun updateEngine(): Boolean {
        val target = _latestVersion.value ?: return false
        return downloadAndInstall(version = target)
    }

    fun clearDataForContext(isolationId: String) {
        try {
            sharedRuntime?.storageController?.clearDataForSessionContext(isolationId)
        } catch (e: Exception) {
            Log.w(TAG, "clearDataForContext failed: ${e.message}")
        }
    }

    fun cancelDownload() { cancelRequested = true }

    fun uninstall() {
        File(context.filesDir, "gecko_engine").deleteRecursively()
        prefs.edit().remove(KEY_INSTALLED).remove(KEY_VERSION).remove(KEY_VERIFIED).remove(KEY_SHA256).apply()
        _installState.value = GeckoInstallState.NotInstalled
        try { sharedRuntime?.shutdown() } catch (_: Exception) {}
        sharedRuntime = null
        Log.i(TAG, "GeckoView uninstalled")
    }

    // ── File helpers ──────────────────────────────────────────────────────────

    private fun getLibsDir(): File {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return File(context.filesDir, "gecko_engine/lib/$abi").also { it.mkdirs() }
    }

    private fun downloadFile(url: String, dest: File, onProgress: (Float) -> Unit): Boolean {
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP ${response.code} for $url")
            return false
        }
        val body = response.body ?: return false
        val total = body.contentLength()
        var read = 0L
        val buf = ByteArray(8192)
        FileOutputStream(dest).use { out ->
            body.byteStream().use { input ->
                var n: Int
                while (input.read(buf).also { n = it } != -1) {
                    if (cancelRequested) return false
                    out.write(buf, 0, n)
                    read += n
                    if (total > 0) onProgress(read.toFloat() / total)
                }
            }
        }
        return dest.exists() && dest.length() > 0
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(8192)
        file.inputStream().use { input ->
            var n: Int
            while (input.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun fetchMavenSha256(artifact: String, version: String): String? {
        return try {
            val url = "$MAVEN_BASE/$artifact/$version/$artifact-$version.aar.sha256"
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            httpClient.newCall(request).execute().use { it.body?.string()?.trim() }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch SHA-256 from Maven: ${e.message}")
            null
        }
    }

    private fun extractSoFiles(aarFile: File, abi: String): Boolean {
        val outDir = getLibsDir()
        val prefix = "jni/$abi/"
        var count = 0
        ZipInputStream(aarFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.startsWith(prefix) && entry.name.endsWith(".so")) {
                    val name = entry.name.substringAfterLast("/")
                    FileOutputStream(File(outDir, name)).use { zis.copyTo(it) }
                    Log.d(TAG, "Extracted $name")
                    count++
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        Log.i(TAG, "Extracted $count .so files")
        return count > 0
    }
}
