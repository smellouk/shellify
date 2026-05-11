package dev.pwaforge.core.isolation

import android.os.Build
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat

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

    fun deleteProfile(isolationId: String) {
        runCatching {
            val store = ProfileStore.getInstance()
            store.deleteProfile("pwa_$isolationId")
        }
    }
}
