package dev.pwaforge.core.iconpack

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class SimpleIconsState {
    data object NotImported : SimpleIconsState()
    data class Downloading(val progress: Float) : SimpleIconsState()
    data object Processing : SimpleIconsState()
    data class Imported(val iconCount: Int) : SimpleIconsState()
    data class Error(val message: String) : SimpleIconsState()
}

class SimpleIconsManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "simple_icons"
        private const val KEY_IMPORTED = "imported"
        private const val KEY_ICON_COUNT = "icon_count"
        private const val DATA_FILE_NAME = "simple_icons.json"
        private const val CDN_URL =
            "https://cdn.jsdelivr.net/npm/simple-icons/data/simple-icons.json"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    val dataFile: File get() = File(context.filesDir, "icon_packs/$DATA_FILE_NAME")

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<SimpleIconsState> = _state.asStateFlow()

    private fun initialState(): SimpleIconsState =
        if (prefs.getBoolean(KEY_IMPORTED, false) && dataFile.exists())
            SimpleIconsState.Imported(prefs.getInt(KEY_ICON_COUNT, 0))
        else SimpleIconsState.NotImported

    suspend fun download(): Unit = withContext(Dispatchers.IO) {
        _state.value = SimpleIconsState.Downloading(0f)
        val tempFile = File(context.cacheDir, "simple_icons_temp.json")
        try {
            val request = Request.Builder()
                .url(CDN_URL)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                tempFile.delete()
                _state.value = SimpleIconsState.Error("HTTP ${response.code}")
                return@withContext
            }
            val body = response.body ?: run {
                _state.value = SimpleIconsState.Error("Empty response")
                return@withContext
            }
            val total = body.contentLength()
            var read = 0L
            val buf = ByteArray(8192)
            FileOutputStream(tempFile).use { out ->
                body.byteStream().use { input ->
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0)
                            _state.value = SimpleIconsState.Downloading(read.toFloat() / total * 0.9f)
                    }
                }
            }
            processAndStore(tempFile)
        } catch (e: CancellationException) {
            tempFile.delete()
            _state.value = SimpleIconsState.NotImported
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            _state.value = SimpleIconsState.Error(e.message ?: "Download failed")
        }
    }

    suspend fun importFromUri(uri: Uri): Unit = withContext(Dispatchers.IO) {
        _state.value = SimpleIconsState.Processing
        val tempFile = File(context.cacheDir, "simple_icons_import.json")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { out -> input.copyTo(out) }
            } ?: run {
                _state.value = SimpleIconsState.Error("Cannot read file")
                return@withContext
            }
            processAndStore(tempFile)
        } catch (e: CancellationException) {
            tempFile.delete()
            _state.value = SimpleIconsState.NotImported
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            _state.value = SimpleIconsState.Error(e.message ?: "Import failed")
        }
    }

    private fun processAndStore(source: File) {
        try {
            val count = JSONArray(source.readText()).length()
            dataFile.parentFile?.mkdirs()
            source.copyTo(dataFile, overwrite = true)
            source.delete()
            prefs.edit()
                .putBoolean(KEY_IMPORTED, true)
                .putInt(KEY_ICON_COUNT, count)
                .apply()
            _state.value = SimpleIconsState.Imported(iconCount = count)
        } catch (e: Exception) {
            source.delete()
            _state.value = SimpleIconsState.Error("Invalid Simple Icons JSON")
        }
    }

    fun remove() {
        dataFile.delete()
        prefs.edit().remove(KEY_IMPORTED).remove(KEY_ICON_COUNT).apply()
        _state.value = SimpleIconsState.NotImported
    }

    /** Called after backup restore to update both SharedPreferences and the live state flow. */
    fun restoreState(imported: Boolean, iconCount: Int) {
        if (imported && iconCount > 0) {
            prefs.edit().putBoolean(KEY_IMPORTED, true).putInt(KEY_ICON_COUNT, iconCount).apply()
            _state.value = SimpleIconsState.Imported(iconCount)
        } else {
            prefs.edit().remove(KEY_IMPORTED).remove(KEY_ICON_COUNT).apply()
            _state.value = SimpleIconsState.NotImported
        }
    }
}
