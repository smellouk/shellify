package io.shellify.app.core.engine

import android.content.Context
import android.view.View
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.WebApp

interface BrowserEngine {
    val engineType: EngineType
    fun createView(context: Context, app: WebApp, callback: BrowserEngineCallback): View
    fun loadUrl(url: String)
    fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)? = null)
    fun canGoBack(): Boolean
    fun goBack()
    fun reload()
    fun stopLoading()
    fun getCurrentUrl(): String?
    fun getView(): View?
    fun destroy()
    fun clearCache(includeDiskFiles: Boolean = true)

    /**
     * Closes the most recently opened popup window (window.open() / OAuth), tearing down its
     * session and firing [BrowserEngineCallback.onClosePopup] so the host detaches the overlay.
     * Returns true if a popup was closed, false if none were open. Default: no popup support.
     */
    fun closeTopPopup(): Boolean = false
}
