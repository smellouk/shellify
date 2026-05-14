package io.shellify.app.core.deeplink

import android.net.Uri

object DeepLinkHandler {
    fun parse(uri: Uri): Pair<String, String>? {
        val isCustom = uri.scheme == "shellify" && uri.host == "add"
        val isHttps = uri.scheme == "https" && uri.host == "shellify.app" && uri.path?.startsWith("/add") == true
        if (!isCustom && !isHttps) return null
        val url = uri.getQueryParameter("url")?.takeIf { it.isNotBlank() } ?: return null
        val name = uri.getQueryParameter("name") ?: ""
        return url to name
    }

    fun buildCustomScheme(url: String, name: String): String =
        Uri.Builder().scheme("shellify").authority("add")
            .appendQueryParameter("url", url)
            .appendQueryParameter("name", name)
            .build().toString()

    fun buildHttps(url: String, name: String): String =
        Uri.Builder().scheme("https").authority("shellify.app").path("/add")
            .appendQueryParameter("url", url)
            .appendQueryParameter("name", name)
            .build().toString()
}
