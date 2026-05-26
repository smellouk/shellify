package io.shellify.app.core.engine

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
        private const val KEY_SHA256 = "sha256_hash"

        const val GECKO_VERSION = "140.0.20250707120347"
        private const val MAVEN_BASE = "https://maven.mozilla.org/maven2/org/mozilla/geckoview"

        private val ABI_ARTIFACT = mapOf(
            "arm64-v8a" to "geckoview-arm64-v8a",
            "armeabi-v7a" to "geckoview-armeabi-v7a",
            "x86_64" to "geckoview-x86_64",
            "x86" to "geckoview-x86",
        )

        // SHA-256 of the AAR for each ABI at GECKO_VERSION — fetched from maven.mozilla.org
        private val KNOWN_SHA256 = mapOf(
            "arm64-v8a" to "ac09410e56d92310a05df56df4eeafbfbcf82243dc66a214b788e2a1b413fa45",
            "armeabi-v7a" to "34aefeb7a5400a4cec4475d41ee6f231c50f1cd04dd5c82ea550ffa96fffaebf",
            "x86_64" to "d294025a1c5c8d293677f8a645ff8a39edff3124c53f301917bc02b69e36f612",
            "x86" to "eec957f0b8242588a846a60e59a524d1fc25adedcd7b520399bfc9da2dfe1409",
        )

        // libmozglue must be loaded before libxul (dependency order)
        private val PRELOAD_ORDER = listOf("libmozglue.so", "liblgpllibs.so", "libxul.so")

    }

    // GeckoView enforces exactly ONE GeckoRuntime per process — a second GeckoRuntime.create()
    // throws IllegalStateException. A single runtime is created lazily on first use and reused for
    // every subsequent call. Proxy routing (SOCKS5 / direct) is controlled via JVM system properties
    // which are checked per-connection, so applying them before each session.open() is sufficient
    // without needing a separate runtime per ProxyConfig (T-02-20, WR-02-fix).
    @Volatile private var runtime: GeckoRuntime? = null

    // Override in tests to supply mock GeckoRuntime instances without calling GeckoRuntime.create().
    internal var runtimeFactory: (ProxyConfig) -> GeckoRuntime = ::buildRuntime

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _installState = MutableStateFlow<GeckoInstallState>(
        if (isInstalled()) GeckoInstallState.Installed(
            verified = prefs.getBoolean(
                KEY_VERIFIED,
                false
            )
        )
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

    @Volatile
    private var cancelRequested = false

    @Volatile
    private var _safeBrowsingEnabled: Boolean = false

    fun isSafeBrowsingEnabled(): Boolean = _safeBrowsingEnabled

    fun applySafeBrowsing(enabled: Boolean) {
        _safeBrowsingEnabled = enabled
        val level = if (enabled) ContentBlocking.SafeBrowsing.DEFAULT else ContentBlocking.SafeBrowsing.NONE
        runtime?.settings?.contentBlocking?.setSafeBrowsing(level)
    }

    fun isInstalled(): Boolean {
        if (!prefs.getBoolean(KEY_INSTALLED, false)) return false
        if (prefs.getString(KEY_VERSION, null) != GECKO_VERSION) return false
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

    /**
     * Returns the single [GeckoRuntime] for this process.
     *
     * GeckoView allows exactly ONE [GeckoRuntime] per process; calling [GeckoRuntime.create] a
     * second time throws [IllegalStateException]. The runtime is created lazily on first call and
     * reused thereafter regardless of [proxyConfig].
     *
     * Proxy routing is managed via JVM system properties ([socksProxyHost] / [socksProxyPort])
     * which the JVM socket layer checks per-connection. They are applied on every [getRuntime]
     * call so the proxy is always correct for new connections opened immediately after.
     *
     * Callers that do not pass a [proxyConfig] get [ProxyConfig.None] (back-compat default).
     */
    fun getRuntime(proxyConfig: ProxyConfig = ProxyConfig.None): GeckoRuntime {
        // Apply proxy system properties on EVERY call — not just at creation — so the active
        // SOCKS5 / direct routing reflects the caller's intent for new socket connections.
        applyProxySystemProperties(proxyConfig)
        // Fast path: return the existing runtime without taking the lock.
        runtime?.let { return it }
        // Slow path: serialize creation so GeckoRuntime.create() is called at most once.
        return synchronized(this) {
            runtime ?: runtimeFactory(proxyConfig).also { runtime = it }
        }
    }

    private fun applyProxySystemProperties(proxyConfig: ProxyConfig) {
        when (proxyConfig) {
            is ProxyConfig.Socks5 -> {
                System.setProperty("socksProxyHost", proxyConfig.host)
                System.setProperty("socksProxyPort", proxyConfig.port.toString())
                // java.net.socks.username / java.net.socks.password are JVM-standard property names
                // (Oracle Networking Properties docs). They affect the JVM socket layer only.
                // NOTE: GeckoView's Gecko C++ network layer does not honor these JVM properties
                // for browser-level page traffic — credentials apply only to JVM-layer sockets
                // (e.g. OkHttpClient). This is a known platform limitation; see Research pitfall 2.
                if (proxyConfig.username != null) {
                    System.setProperty("java.net.socks.username", proxyConfig.username)
                    System.setProperty("java.net.socks.password", proxyConfig.password ?: "")
                } else {
                    System.clearProperty("java.net.socks.username")
                    System.clearProperty("java.net.socks.password")
                }
                // Clear HTTP proxy properties to prevent bleed-through on SOCKS5 <-> HTTP switches.
                System.clearProperty("http.proxyHost")
                System.clearProperty("http.proxyPort")
                System.clearProperty("http.proxyUser")
                System.clearProperty("http.proxyPassword")
            }
            is ProxyConfig.Http -> {
                // http.proxyHost / http.proxyPort affect JVM socket layer only.
                // NOTE: GeckoView's Necko network stack handles browser connections independently
                // of these JVM properties; HTTP proxy may not route page traffic. See Research pitfall 1.
                System.setProperty("http.proxyHost", proxyConfig.host)
                System.setProperty("http.proxyPort", proxyConfig.port.toString())
                if (proxyConfig.username != null) {
                    System.setProperty("http.proxyUser", proxyConfig.username)
                    System.setProperty("http.proxyPassword", proxyConfig.password ?: "")
                } else {
                    System.clearProperty("http.proxyUser")
                    System.clearProperty("http.proxyPassword")
                }
                // Clear SOCKS properties to prevent bleed-through.
                System.clearProperty("socksProxyHost")
                System.clearProperty("socksProxyPort")
                System.clearProperty("java.net.socks.username")
                System.clearProperty("java.net.socks.password")
            }
            else -> {
                System.clearProperty("socksProxyHost")
                System.clearProperty("socksProxyPort")
                System.clearProperty("java.net.socks.username")
                System.clearProperty("java.net.socks.password")
                System.clearProperty("http.proxyHost")
                System.clearProperty("http.proxyPort")
                System.clearProperty("http.proxyUser")
                System.clearProperty("http.proxyPassword")
            }
        }
    }

    // Proxy system properties are applied by applyProxySystemProperties() before this factory
    // is invoked, so the runtime inherits the correct SOCKS5 / direct config at creation time.
    @Suppress("UnusedParameter")
    private fun buildRuntime(proxyConfig: ProxyConfig): GeckoRuntime {
        val safeBrowsingLevel = if (_safeBrowsingEnabled) ContentBlocking.SafeBrowsing.DEFAULT else ContentBlocking.SafeBrowsing.NONE
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .contentBlocking(
                ContentBlocking.Settings.Builder()
                    .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
                    .safeBrowsing(safeBrowsingLevel)
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                    .build()
            )
            .build()
        return GeckoRuntime.create(context.applicationContext, settings)
    }


    // ── Download & install ────────────────────────────────────────────────────

    suspend fun downloadAndInstall(version: String = GECKO_VERSION): Boolean =
        withContext(Dispatchers.IO) {
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
                        _installState.value =
                            GeckoInstallState.Downloading(p * 0.85f, "Downloading…")
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
                    Log.w(
                        TAG,
                        "No expected hash available for $artifact $version — skipping verification"
                    )
                    verified = false
                } else {
                    val actualHash = sha256(tempAar)
                    if (actualHash != expectedHash) {
                        Log.e(TAG, "SHA-256 mismatch! expected=$expectedHash actual=$actualHash")
                        tempAar.delete()
                        _installState.value =
                            GeckoInstallState.Error("Integrity check failed — download may be corrupted or tampered")
                        return@withContext false
                    }
                    Log.i(TAG, "SHA-256 verified: $actualHash")
                    verified = true
                }

                _installState.value = GeckoInstallState.Installing
                val extracted = extractSoFiles(tempAar, abi)
                tempAar.delete()

                if (!extracted) {
                    _installState.value =
                        GeckoInstallState.Error("Extraction failed — no .so files found in AAR")
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
            val body = httpClient.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext null

            // Parse <release> or last <version> from maven-metadata.xml.
            // Prefer the <release> tag; only fall back to <version> if no <release> was found.
            // Without foundRelease, every subsequent <version> element overwrites latest, which
            // may point to an older build than <release> when <version> elements follow <release>
            // in document order (WR-07).
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser().apply { setInput(body.reader()) }
            var latest: String? = null
            var inRelease = false
            var foundRelease = false
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.name == "release") inRelease = true
                else if (eventType == XmlPullParser.TEXT && inRelease) {
                    latest = xpp.text.trim()
                    inRelease = false
                    foundRelease = true
                } else if (!foundRelease && eventType == XmlPullParser.START_TAG && xpp.name == "version") {
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
        val rt = runtime ?: return
        try {
            rt.storageController.clearDataForSessionContext(isolationId)
        } catch (e: Exception) {
            Log.w(TAG, "clearDataForContext failed: ${e.message}")
        }
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    fun uninstall() {
        File(context.filesDir, "gecko_engine").deleteRecursively()
        prefs.edit().remove(KEY_INSTALLED).remove(KEY_VERSION).remove(KEY_VERIFIED)
            .remove(KEY_SHA256).apply()
        _installState.value = GeckoInstallState.NotInstalled
        runtime?.let {
            try { it.shutdown() } catch (_: Exception) { }
        }
        runtime = null
        Log.i(TAG, "GeckoView uninstalled")
    }

    // ── File helpers ──────────────────────────────────────────────────────────

    private fun getLibsDir(): File {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return File(context.filesDir, "gecko_engine/lib/$abi").also { it.mkdirs() }
    }

    private fun downloadFile(url: String, dest: File, onProgress: (Float) -> Unit): Boolean {
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        // Wrap response in .use{} so the connection is always returned to OkHttp's pool,
        // including on error paths. Without this the socket leaks on !isSuccessful (CR-07).
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP ${response.code} for $url")
                return@use false
            }
            val body = response.body ?: return@use false
            val total = body.contentLength()
            var read = 0L
            val buf = ByteArray(8192)
            FileOutputStream(dest).use { out ->
                body.byteStream().use { input ->
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        if (cancelRequested) return@use false
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
            dest.exists() && dest.length() > 0
        }
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
