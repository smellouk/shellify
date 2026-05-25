package io.shellify.app.core.navigation

import android.content.Context
import android.content.Intent
import io.shellify.app.domain.model.LockType

interface WebViewIntentFactory {
    fun previewIntent(context: Context, url: String, name: String, lockType: LockType? = null): Intent
}
