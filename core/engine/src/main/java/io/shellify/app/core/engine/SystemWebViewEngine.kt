package io.shellify.app.core.engine

import android.content.Context
import android.graphics.Bitmap
import android.os.Message
import android.view.View
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import io.shellify.app.core.adblock.AdBlocker
import io.shellify.app.core.webview.WebViewManager
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp

// Extracted so unit tests can verify the main-frame guard and blocked flag without
// requiring a real WebView or AdBlocker (same pattern used for dispatchNotification).
internal fun dispatchInterceptedRequest(url: String, isForMainFrame: Boolean, blocked: Boolean, cb: BrowserEngineCallback?) {
    if (!isForMainFrame) {
        cb?.onRequestIntercepted(url, blocked = blocked)
    }
}

// Extracted for unit testing: a non-http(s) scheme (tel:, mailto:, intent:, custom OAuth schemes)
// is handed to the host as an external link; http(s) navigations stay inside the WebView/popup.
internal fun isExternalScheme(url: String): Boolean =
    !url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)

class SystemWebViewEngine(private val adBlocker: AdBlocker) : BrowserEngine {

    override val engineType = EngineType.SYSTEM_WEBVIEW
    private var webView: WebView? = null
    private var storedCallback: BrowserEngineCallback? = null

    // Popup WebViews created for window.open() / OAuth flows. Tracked so they can be destroyed
    // with the engine even if the page never fires onCloseWindow.
    private val popups = mutableListOf<WebView>()

    override fun createView(context: Context, app: WebApp, callback: BrowserEngineCallback): View {
        storedCallback = callback
        val wv = WebView(context)
        WebViewManager.configure(wv, app)

        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val result = if (app.adBlockEnabled) adBlocker.shouldBlock(request, app.trackerBlockingEnabled) else null
                dispatchInterceptedRequest(
                    url = request.url.toString(),
                    isForMainFrame = request.isForMainFrame,
                    blocked = result != null,
                    cb = storedCallback,
                )
                return result
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (isExternalScheme(url)) {
                    callback.onExternalLink(url)
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) =
                callback.onPageStarted(url)

            override fun onPageFinished(view: WebView, url: String) =
                callback.onPageFinished(url)

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                if (request.isForMainFrame) {
                    callback.onError(error.errorCode, error.description.toString())
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError,
            ) {
                handler.cancel()
                callback.onSslError(error.toString())
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) =
                callback.onProgressChanged(newProgress)

            override fun onReceivedTitle(view: WebView, title: String) =
                callback.onTitleChanged(title)

            override fun onShowCustomView(view: View, cb: CustomViewCallback) =
                callback.onShowCustomView(view, cb)

            override fun onHideCustomView() =
                callback.onHideCustomView()

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message,
            ): Boolean = handleCreateWindow(view.context, app, callback, resultMsg)
        }

        webView = wv
        return wv
    }

    // Honour window.open() / target="_blank" (OAuth, "Sign in with Google") by spawning a real
    // popup WebView. The host displays it as an overlay via onShowPopup; the popup self-dismisses
    // through onCloseWindow when the flow finishes. Returns true so the link is not also opened
    // in the parent frame.
    private fun handleCreateWindow(
        context: Context,
        app: WebApp,
        callback: BrowserEngineCallback,
        resultMsg: Message,
    ): Boolean {
        val popup = createPopupWebView(context, app, callback)
        callback.onShowPopup(popup)
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
    }

    private fun createPopupWebView(context: Context, app: WebApp, callback: BrowserEngineCallback): WebView {
        val popup = WebView(context)
        WebViewManager.configure(popup, app)
        popup.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                if (app.adBlockEnabled) adBlocker.shouldBlock(request, app.trackerBlockingEnabled) else null

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (isExternalScheme(url)) {
                    callback.onExternalLink(url)
                    return true
                }
                return false
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                callback.onSslError(error.toString())
            }
        }
        popup.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message,
            ): Boolean = handleCreateWindow(view.context, app, callback, resultMsg)

            override fun onCloseWindow(window: WebView) = closePopup(window, callback)
        }
        popups.add(popup)
        return popup
    }

    private fun closePopup(popup: WebView, callback: BrowserEngineCallback) {
        popups.remove(popup)
        callback.onClosePopup(popup)
        popup.stopLoading()
        popup.destroy()
    }

    override fun closeTopPopup(): Boolean {
        val popup = popups.lastOrNull() ?: return false
        val callback = storedCallback ?: return false
        closePopup(popup, callback)
        return true
    }

    fun getWebView(): WebView? = webView

    override fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    override fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {
        webView?.evaluateJavascript(script, resultCallback)
    }

    override fun canGoBack() = webView?.canGoBack() ?: false
    override fun goBack() {
        webView?.goBack()
    }

    override fun reload() {
        webView?.reload()
    }

    override fun stopLoading() {
        webView?.stopLoading()
    }

    override fun getCurrentUrl() = webView?.url
    override fun getView(): View? = webView

    override fun destroy() {
        popups.toList().forEach { it.stopLoading(); it.removeAllViews(); it.destroy() }
        popups.clear()
        webView?.apply { stopLoading(); clearHistory(); removeAllViews(); destroy() }
        webView = null
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        webView?.clearCache(includeDiskFiles)
    }
}
