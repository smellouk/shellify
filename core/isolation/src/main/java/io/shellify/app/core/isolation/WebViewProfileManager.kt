package io.shellify.app.core.isolation

import android.os.Build
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * API 33+ isolation: each PWA gets its own named WebView profile.
 * A profile has a completely separate cookie store, localStorage, IndexedDB, and cache.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object WebViewProfileManager {

    fun applyProfile(webView: WebView, isolationId: String) {
        runCatching {
            val store = ProfileStore.getInstance()
            val profileName = "pwa_$isolationId"
            store.getOrCreateProfile(profileName)
            WebViewCompat.setProfile(webView, profileName)
        }
        // If ProfileStore is unavailable on this build (shouldn't happen on API 33+),
        // we silently fall back — CookieJarManager handles the API < 33 path.
    }

    suspend fun deleteProfile(isolationId: String) {
        val profileName = "pwa_$isolationId"
        val store = runCatching { ProfileStore.getInstance() }.getOrNull() ?: return
        runCatching {
            withContext(Dispatchers.IO) { store.deleteProfile(profileName) }
        }.onFailure {
            // Profile is in use by a live WebView — clear its data in-place.
            runCatching {
                val profile = store.getOrCreateProfile(profileName)
                profile.webStorage.deleteAllData()
                profile.cookieManager.removeAllCookies(null)
                profile.cookieManager.flush()
            }
        }
    }
}
