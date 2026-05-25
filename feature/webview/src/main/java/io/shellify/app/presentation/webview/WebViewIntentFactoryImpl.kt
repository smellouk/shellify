package io.shellify.app.presentation.webview

import android.content.Context
import android.content.Intent
import io.shellify.app.core.navigation.WebViewIntentFactory
import io.shellify.app.domain.model.LockType

class WebViewIntentFactoryImpl : WebViewIntentFactory {
    override fun previewIntent(context: Context, url: String, name: String, lockType: LockType?): Intent =
        WebViewActivity.previewIntent(context, url, name, lockType)
}
