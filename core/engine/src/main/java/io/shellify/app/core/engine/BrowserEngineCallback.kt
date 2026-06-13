package io.shellify.app.core.engine

import android.graphics.Bitmap
import android.view.View

interface BrowserEngineCallback {
    fun onPageStarted(url: String?)
    fun onPageFinished(url: String?)
    fun onProgressChanged(progress: Int)
    fun onTitleChanged(title: String?)
    fun onIconReceived(icon: Bitmap?)
    fun onError(errorCode: Int, description: String)
    fun onSslError(error: String)
    fun onExternalLink(url: String)
    fun onShowCustomView(view: View?, callback: Any?)
    fun onHideCustomView()
    fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
    )

    fun onNotificationReceived(title: String, body: String?, iconUrl: String?, tag: String?)

    fun onNotificationPermissionRequested(onResult: (Boolean) -> Unit)

    fun onRequestIntercepted(url: String, blocked: Boolean) {}

    /**
     * A page opened a new window (`window.open()` / `target="_blank"`) — typically an OAuth /
     * "Sign in with Google" popup. The engine has created [view] to host that window; the host
     * must attach it as an overlay so the popup is visible and interactive. Default no-op for
     * hosts that do not support popups.
     */
    fun onShowPopup(view: View) {}

    /**
     * A popup previously surfaced via [onShowPopup] requested to close (e.g. the OAuth flow
     * finished and called `window.close()`). The host must detach [view] from its overlay.
     */
    fun onClosePopup(view: View) {}
}
