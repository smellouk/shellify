package io.shellify.app.core.deeplink

import android.net.Uri
import android.util.Base64

object DeepLinkHandler {
    fun parse(uri: Uri): Pair<String, String>? {
        val isCustom = uri.scheme == "shellify" && uri.host == "add"
        val isHttps = uri.scheme == "https" && uri.host == "shellify.app" && uri.path?.startsWith("/add") == true
        if (!isCustom && !isHttps) return null
        val rawUrl = uri.getQueryParameter("url")?.takeIf { it.isNotBlank() } ?: return null
        val name = uri.getQueryParameter("name") ?: ""
        val url = decodeUrl(rawUrl) ?: return null   // reject non-https or decode failures
        return url to name
    }

    fun buildCustomScheme(url: String, name: String): String =
        Uri.Builder().scheme("shellify").authority("add")
            .appendQueryParameter("url", encodeUrl(url))
            .appendQueryParameter("name", name)
            .build().toString()

    fun buildHttps(url: String, name: String): String =
        Uri.Builder().scheme("https").authority("shellify.app").path("/add")
            .appendQueryParameter("url", encodeUrl(url))
            .appendQueryParameter("name", name)
            .build().toString()

    private fun encodeUrl(url: String): String =
        Base64.encodeToString(url.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    /**
     * Decodes a base64url-encoded URL and returns it only if it uses https.
     * Returns null on decode failure or when the decoded value is not an https URL —
     * the caller should silently drop the deeplink in that case.
     */
    private fun decodeUrl(raw: String): String? = runCatching {
        val decoded = String(Base64.decode(raw, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        decoded.takeIf { it.startsWith("https://") }
    }.getOrNull()
}
