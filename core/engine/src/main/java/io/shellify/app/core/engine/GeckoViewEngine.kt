package io.shellify.app.core.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.model.WebApp
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

// Thin, test-injectable data holder so unit tests can exercise fan-out without a real WebNotification.
internal data class NotificationPayload(
    val title: String?,
    val body: String?,
    val iconUrl: String?,
    val tag: String?,
)

// Extracted so unit tests can exercise notification fan-out without a real GeckoSession or GeckoRuntime.
internal fun dispatchNotification(payload: NotificationPayload, cb: BrowserEngineCallback) {
    val title = payload.title ?: return
    cb.onNotificationReceived(title, payload.body, payload.iconUrl, payload.tag)
}

// Bridge from the GeckoView 140 WebNotification type to the testable payload.
internal fun dispatchNotification(notification: WebNotification, cb: BrowserEngineCallback) {
    dispatchNotification(
        NotificationPayload(
            title = notification.title,
            body = notification.text,
            iconUrl = notification.imageUrl,
            tag = notification.tag,
        ),
        cb,
    )
}

// Extracted so unit tests can exercise ContentBlocking.Delegate fan-out without a real GeckoSession.
// event.isBlocking() distinguishes actually-blocked (true) from tracked-but-not-blocked (false).
internal fun dispatchContentBlocked(uri: String, isBlocking: Boolean, cb: BrowserEngineCallback) {
    cb.onRequestIntercepted(uri, blocked = isBlocking)
}

// Extracted for symmetric testability with dispatchContentBlocked.
// onContentLoaded fires for resources that were loaded (not blocked) — always blocked=false.
internal fun dispatchContentLoaded(uri: String, cb: BrowserEngineCallback) {
    cb.onRequestIntercepted(uri, blocked = false)
}

class GeckoViewEngine(
    private val context: Context,
    private val engineManager: GeckoEngineManager,
) : BrowserEngine {

    companion object {
        private const val TAG = "GeckoViewEngine"
        // Tor SOCKS5 proxy address. TorService binds on localhost:9050 by default.
        internal const val TOR_PROXY_HOST = "127.0.0.1"
        @Suppress("MagicNumber")
        internal const val TOR_PROXY_PORT = 9050
    }

    /** Returns the ProxyConfig to use when opening a GeckoRuntime for [app]. */
    internal fun proxyConfigFor(app: WebApp): ProxyConfig =
        if (app.useTor) ProxyConfig.Socks5(TOR_PROXY_HOST, TOR_PROXY_PORT) else ProxyConfig.None

    override val engineType = EngineType.GECKOVIEW

    private var geckoView: GeckoView? = null
    private var session: GeckoSession? = null
    private var callback: BrowserEngineCallback? = null
    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var canGoBackFlag = false
    private var geckoScrollY: Int = 0

    // For crash recovery
    private var lastUaMode = GeckoSessionSettings.USER_AGENT_MODE_MOBILE
    private var lastUaOverride: String? = null
    private var lastApp: WebApp? = null
    private var contextId: String? = null

    @androidx.annotation.MainThread
    fun reattachNotificationDelegate() {
        val cb = callback ?: return
        val proxyConfig = lastApp?.let { proxyConfigFor(it) } ?: ProxyConfig.None
        engineManager.getRuntime(proxyConfig).setWebNotificationDelegate(object : WebNotificationDelegate {
            override fun onShowNotification(notification: WebNotification) {
                dispatchNotification(notification, cb)
            }
            override fun onCloseNotification(notification: WebNotification) = Unit
        })
    }

    override fun createView(context: Context, app: WebApp, callback: BrowserEngineCallback): View {
        this.callback = callback
        this.lastApp = app
        this.contextId = app.isolationId

        val uaMode = when (app.uaMode) {
            UserAgentMode.CHROME_DESKTOP -> GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            else -> GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        lastUaMode = uaMode

        val uaOverride = when (app.uaMode) {
            UserAgentMode.DEFAULT -> null
            else -> app.uaMode.uaString
        }
        lastUaOverride = uaOverride

        val session = buildSession(uaMode, uaOverride, callback)
        this.session = session

        // WebNotificationDelegate is runtime-scoped (GeckoView 140 API) — one delegate for all sessions.
        // Overwrite on each createView so the active callback is always current.
        // Request a proxy-aware runtime: Tor apps get Socks5("127.0.0.1", 9050); others use ProxyConfig.None.
        engineManager.getRuntime(proxyConfigFor(app)).setWebNotificationDelegate(object : WebNotificationDelegate {
            override fun onShowNotification(notification: WebNotification) {
                dispatchNotification(notification, callback)
            }

            override fun onCloseNotification(notification: WebNotification) {
                // No-op: notification lifecycle management is the caller's concern (Plan 05).
            }
        })

        val view = GeckoView(context)
        view.setSession(session)
        geckoView = view
        return view
    }

    private fun buildSession(
        uaMode: Int,
        uaOverride: String?,
        cb: BrowserEngineCallback,
    ): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .userAgentMode(uaMode)
            .contextId(contextId)
            .build()

        val s = GeckoSession(settings)
        uaOverride?.let { s.settings.userAgentOverride = it }

        // Use the proxy-aware runtime so sessions for Tor apps route through SOCKS5.
        s.open(engineManager.getRuntime(lastApp?.let { proxyConfigFor(it) } ?: ProxyConfig.None))

        s.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                currentTitle = title
                cb.onTitleChanged(title)
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                if (fullScreen) cb.onShowCustomView(geckoView, null)
                else cb.onHideCustomView()
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                val contentType = response.headers["Content-Type"] ?: "application/octet-stream"
                val contentDisposition = response.headers["Content-Disposition"] ?: ""
                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                cb.onDownloadStart(response.uri, "", contentDisposition, contentType, contentLength)
            }

            override fun onCrash(session: GeckoSession) {
                Log.e(TAG, "GeckoSession crashed — attempting recovery")
                cb.onError(-1, "Engine crashed, recovering…")
                recoverFromCrash()
            }
        }

        // ScrollDelegate is the correct GeckoView 140 API for tracking scroll position.
        // ContentDelegate.onScrollChanged does not exist in this API level.
        s.setScrollDelegate(object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                geckoScrollY = scrollY
            }
        })

        s.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                currentUrl = url
                geckoScrollY = 0
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                canGoBackFlag = canGoBack
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {}

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest,
            ): GeckoResult<AllowOrDeny>? {
                val uri = request.uri
                if (uri.startsWith("tel:") || uri.startsWith("mailto:") || uri.startsWith("intent:")) {
                    cb.onExternalLink(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                loadUrl(uri)
                return null
            }

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: WebRequestError,
            ): GeckoResult<String>? {
                val description = uri ?: error.toString()
                if (error.category == WebRequestError.ERROR_CATEGORY_SECURITY) {
                    cb.onSslError(description)
                } else {
                    cb.onError(error.code, description)
                }
                return GeckoResult.fromValue(null)
            }
        }

        s.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                cb.onPageStarted(url)
                // Log main-frame navigation as a non-blocked entry (D-05: page-level only for GeckoView).
                cb.onRequestIntercepted(url, blocked = false)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                cb.onPageFinished(currentUrl)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                cb.onProgressChanged(progress)
            }
        }

        s.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
                dispatchContentBlocked(event.uri, isBlocking = event.isBlocking(), cb = cb)
            }

            override fun onContentLoaded(session: GeckoSession, event: ContentBlocking.BlockEvent) {
                dispatchContentLoaded(event.uri, cb = cb)
            }
        }

        // Consolidate delegate wiring via factory so both GeckoViewEngine (foreground)
        // and BackgroundNotificationService (background) share the same delegate logic.
        // Persist the grant via StorageController so Notification.permission === 'granted'
        // survives page reloads — GeckoView's VALUE_ALLOW is per-request only without this.
        // Use the same proxy-aware runtime that this session is opened on so that the
        // StorageController.setPermission call targets the correct runtime instance.
        // Calling getRuntime() without args defaults to ProxyConfig.None and would store the
        // permission in the wrong runtime for Tor sessions, reverting to "default" on reload (CR-06).
        NotificationDelegateFactory.attach(s, cb) { perm ->
            val proxyConfig = lastApp?.let { proxyConfigFor(it) } ?: ProxyConfig.None
            engineManager.getRuntime(proxyConfig).storageController.setPermission(
                perm,
                GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW,
            )
        }

        return s
    }

    private fun recoverFromCrash() {
        val view = geckoView ?: return
        val cb = callback ?: return
        val urlToRestore = currentUrl
        try {
            try {
                session?.close()
            } catch (_: Exception) {
            }
            session = null

            val newSession = buildSession(lastUaMode, lastUaOverride, cb)
            view.setSession(newSession)
            session = newSession

            if (!urlToRestore.isNullOrBlank() && urlToRestore != "about:blank") {
                newSession.loadUri(urlToRestore)
                Log.i(TAG, "Crash recovery: restoring $urlToRestore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Crash recovery failed", e)
            cb.onError(-2, "Engine recovery failed: ${e.message}")
        }
    }

    override fun loadUrl(url: String) {
        session?.loadUri(url)
    }

    override fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {
        val s = session ?: run { resultCallback?.invoke(null); return }
        try {
            val encoded = android.util.Base64.encodeToString(
                script.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP,
            )
            s.loadUri("javascript:void(eval(atob('$encoded')))")
        } catch (e: Exception) {
            Log.e(TAG, "evaluateJavascript failed", e)
        }
        resultCallback?.invoke(null)
    }

    override fun canGoBack() = canGoBackFlag

    fun canScrollUp(): Boolean = geckoScrollY > 0

    override fun goBack() {
        session?.goBack()
    }

    override fun reload() {
        session?.reload()
    }

    override fun stopLoading() {
        session?.stop()
    }

    override fun getCurrentUrl() = currentUrl
    override fun getView(): View? = geckoView

    override fun destroy() {
        try {
            session?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing session", e)
        }
        session = null
        geckoView = null
        callback = null
        lastApp = null
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        try {
            val ctx = contextId
            val proxyConfig = lastApp?.let { proxyConfigFor(it) } ?: ProxyConfig.None
            if (ctx != null) {
                engineManager.getRuntime(proxyConfig).storageController.clearDataForSessionContext(ctx)
            } else {
                engineManager.getRuntime(proxyConfig).storageController.clearData(StorageController.ClearFlags.ALL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "clearCache failed", e)
        }
    }
}
