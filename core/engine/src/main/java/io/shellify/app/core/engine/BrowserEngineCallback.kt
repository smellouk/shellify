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
}
