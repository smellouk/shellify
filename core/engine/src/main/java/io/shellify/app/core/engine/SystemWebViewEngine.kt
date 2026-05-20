package io.shellify.app.core.engine

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.shellify.app.core.adblock.AdBlocker
import io.shellify.app.core.webview.WebViewManager
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp

class SystemWebViewEngine(private val adBlocker: AdBlocker) : BrowserEngine {

    override val engineType = EngineType.SYSTEM_WEBVIEW
    private var webView: WebView? = null

    override fun createView(context: Context, app: WebApp, callback: BrowserEngineCallback): View {
        val wv = WebView(context)
        WebViewManager.configure(wv, app)

        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                if (app.adBlockEnabled) adBlocker.shouldBlock(request) else null

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
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


        }

        webView = wv
        return wv
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
        webView?.apply { stopLoading(); clearHistory(); removeAllViews(); destroy() }
        webView = null
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        webView?.clearCache(includeDiskFiles)
    }
}
