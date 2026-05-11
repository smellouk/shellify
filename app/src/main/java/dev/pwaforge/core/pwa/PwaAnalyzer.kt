package dev.pwaforge.core.pwa

import dev.pwaforge.domain.model.PwaIcon
import dev.pwaforge.domain.model.PwaManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches and parses a site's Web App Manifest.
 * Falls back to HTML <meta> tags when no manifest is found.
 */
class PwaAnalyzer(private val client: OkHttpClient) {

    suspend fun analyze(rawUrl: String): PwaManifest = withContext(Dispatchers.IO) {
        val url = rawUrl.trimEnd('/')
        val html = fetchHtml(url) ?: return@withContext PwaManifest()

        val manifestUrl = extractManifestUrl(html, url)
        val manifest = if (manifestUrl != null) fetchManifest(manifestUrl, url) else PwaManifest()

        // Supplement missing fields from <meta> / <link> tags
        return@withContext manifest.copy(
            name = manifest.name ?: extractMeta(html, "application-name")
                ?: extractMeta(html, "og:title")
                ?: extractTitle(html),
            description = manifest.description ?: extractMeta(html, "description")
                ?: extractMeta(html, "og:description"),
            themeColor = manifest.themeColor ?: extractMeta(html, "theme-color"),
            // When the manifest had no icons, harvest from HTML link/meta tags
            icons = manifest.icons.ifEmpty {
                buildList {
                    // <link rel="apple-touch-icon"> — highest quality fallback
                    extractLinkHref(html, "apple-touch-icon", url)?.let {
                        add(PwaIcon(src = it, sizes = "180x180"))
                    }
                    extractLinkHref(html, "apple-touch-icon-precomposed", url)?.let {
                        add(PwaIcon(src = it, sizes = "180x180"))
                    }
                    // og:image — Open Graph image used by most modern sites
                    extractMeta(html, "og:image")?.let { raw ->
                        val absolute = resolveUrl(raw, url)
                        add(PwaIcon(src = absolute))
                    }
                }
            },
        )
    }

    private fun fetchHtml(url: String): String? = runCatching {
        val request = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; PWAForge/1.0)")
            .build()
        client.newCall(request).execute().use { it.body?.string() }
    }.getOrNull()

    private fun extractManifestUrl(html: String, baseUrl: String): String? {
        val regex = Regex("""<link[^>]+rel=["']manifest["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val href = regex.find(html)?.groupValues?.get(1) ?: return null
        return when {
            href.startsWith("http") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> "${extractOrigin(baseUrl)}$href"
            else -> "$baseUrl/$href"
        }
    }

    private fun fetchManifest(manifestUrl: String, siteUrl: String): PwaManifest = runCatching {
        val request = Request.Builder().url(manifestUrl).build()
        val body = client.newCall(request).execute().use { it.body?.string() } ?: return PwaManifest()
        parseManifest(JSONObject(body), siteUrl)
    }.getOrDefault(PwaManifest())

    private fun parseManifest(json: JSONObject, siteUrl: String): PwaManifest {
        val icons = mutableListOf<PwaIcon>()
        json.optJSONArray("icons")?.let { arr: JSONArray ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val src = obj.optString("src").takeIf { it.isNotBlank() } ?: continue
                val absoluteSrc = when {
                    src.startsWith("http") -> src
                    src.startsWith("/") -> "${extractOrigin(siteUrl)}$src"
                    else -> "$siteUrl/$src"
                }
                icons += PwaIcon(
                    src = absoluteSrc,
                    sizes = obj.optString("sizes").takeIf { it.isNotBlank() },
                    type = obj.optString("type").takeIf { it.isNotBlank() },
                    purpose = obj.optString("purpose").takeIf { it.isNotBlank() },
                )
            }
        }
        return PwaManifest(
            name = json.optString("name").takeIf { it.isNotBlank() },
            shortName = json.optString("short_name").takeIf { it.isNotBlank() },
            description = json.optString("description").takeIf { it.isNotBlank() },
            startUrl = json.optString("start_url").takeIf { it.isNotBlank() },
            themeColor = json.optString("theme_color").takeIf { it.isNotBlank() },
            backgroundColor = json.optString("background_color").takeIf { it.isNotBlank() },
            display = json.optString("display").takeIf { it.isNotBlank() },
            icons = icons,
        )
    }

    /** Extracts href from `<link rel="[rel]" href="...">`, handles both attribute orderings. */
    private fun extractLinkHref(html: String, rel: String, baseUrl: String): String? {
        val patterns = listOf(
            Regex("""<link[^>]+rel=["']${Regex.escape(rel)}["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<link[^>]+href=["']([^"']+)["'][^>]+rel=["']${Regex.escape(rel)}["']""", RegexOption.IGNORE_CASE),
        )
        val href = patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) } ?: return null
        return resolveUrl(href, baseUrl)
    }

    private fun resolveUrl(href: String, baseUrl: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("//") -> "https:$href"
        href.startsWith("/") -> "${extractOrigin(baseUrl)}$href"
        else -> "$baseUrl/$href"
    }

    private fun extractMeta(html: String, name: String): String? {
        val patterns = listOf(
            Regex("""<meta[^>]+name=["']${Regex.escape(name)}["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+name=["']${Regex.escape(name)}["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+property=["']${Regex.escape(name)}["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) }
    }

    private fun extractTitle(html: String): String? =
        Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.trim()

    private fun extractOrigin(url: String): String {
        val proto = if (url.startsWith("https")) "https" else "http"
        val host = url.removePrefix("https://").removePrefix("http://").substringBefore('/')
        return "$proto://$host"
    }

    companion object {
        fun create(): PwaAnalyzer = PwaAnalyzer(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        )
    }
}
