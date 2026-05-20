package io.shellify.app.core.isolation

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebView
import io.shellify.app.core.crypto.CryptoManager
import io.shellify.app.core.engine.GeckoEngineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Single entry-point for all PWA isolation logic.
 *
 * - API 33+  → WebView Profiles (complete isolation: cookies, localStorage, IndexedDB, cache)
 * - API 23–32 → CookieJarManager (encrypted cookie save/restore) +
 *               same-origin policy already isolates localStorage per domain
 *
 * Usage order matters:
 *   1. Call [attachProfile] BEFORE the WebView is added to the view hierarchy (API 33+ requirement).
 *   2. Call [restoreSession] (suspend) BEFORE loadUrl — awaits cookie restore on API < 33.
 */
class IsolationManager(
    context: Context,
    crypto: CryptoManager,
    private val geckoEngineManager: GeckoEngineManager? = null
) {

    val cookieJarManager = CookieJarManager(context, crypto)
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Must be called BEFORE the WebView is attached to a window.
     * On API 33+ assigns the named profile; no-op on older APIs.
     */
    fun attachProfile(webView: WebView, isolationId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            WebViewProfileManager.applyProfile(webView, isolationId)
        }
    }

    /**
     * Must be called AFTER attaching to window but BEFORE loadUrl.
     * On API < 33, restores the saved encrypted cookie jar synchronously.
     */
    suspend fun restoreSession(isolationId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            cookieJarManager.restoreFor(isolationId)
        }
    }

    fun onSessionEnd(isolationId: String, visitedUrls: Set<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            scope.launch { cookieJarManager.saveAndClearFor(isolationId, visitedUrls) }
        }
    }

    suspend fun clearData(isolationId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            WebViewProfileManager.deleteProfile(isolationId)
        } else {
            val cm = CookieManager.getInstance()
            suspendCancellableCoroutine { cont ->
                cm.removeAllCookies { cont.resume(Unit) }
            }
            cm.flush()
            cookieJarManager.deleteFor(isolationId)
        }
        geckoEngineManager?.clearDataForContext(isolationId)
    }

}
