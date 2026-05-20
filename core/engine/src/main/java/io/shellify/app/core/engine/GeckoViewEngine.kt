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
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

class GeckoViewEngine(
    private val context: Context,
    private val engineManager: GeckoEngineManager,
) : BrowserEngine {

    companion object {
        private const val TAG = "GeckoViewEngine"
    }

    override val engineType = EngineType.GECKOVIEW

    private var geckoView: GeckoView? = null
    private var session: GeckoSession? = null
    private var callback: BrowserEngineCallback? = null
    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var canGoBackFlag = false

    // For crash recovery
    private var lastUaMode = GeckoSessionSettings.USER_AGENT_MODE_MOBILE
    private var lastUaOverride: String? = null
    private var lastApp: WebApp? = null
    private var contextId: String? = null

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

        s.open(engineManager.getRuntime())

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

        s.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                currentUrl = url
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
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                cb.onPageFinished(currentUrl)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                cb.onProgressChanged(progress)
            }
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
            if (ctx != null) {
                engineManager.getRuntime().storageController.clearDataForSessionContext(ctx)
            } else {
                engineManager.getRuntime().storageController.clearData(StorageController.ClearFlags.ALL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "clearCache failed", e)
        }
    }
}
