package dev.pwaforge.domain.model

data class PwaManifest(
    val name: String? = null,
    val shortName: String? = null,
    val description: String? = null,
    val startUrl: String? = null,
    val themeColor: String? = null,
    val backgroundColor: String? = null,
    val display: String? = null,
    val icons: List<PwaIcon> = emptyList(),
) {
    /**
     * Returns the best icon URL using the same priority AppForge uses:
     *  1. Exclude monochrome-only icons
     *  2. Prefer "maskable" purpose (full-bleed colourful icons)
     *  3. Among same purpose, prefer largest size
     *  4. Fall back to any remaining icon
     */
    fun bestIconUrl(baseUrl: String): String? {
        val usable = icons.filter { !it.isPurposeOnly("monochrome") }
        val pool = usable.ifEmpty { icons }  // if everything is monochrome, allow it

        val preferred = pool
            .filter { it.hasPurpose("maskable") }
            .maxByOrNull { sizeScore(it.sizes) }
            ?: pool.filter { it.hasPurpose("any") || it.purpose == null }
                .maxByOrNull { sizeScore(it.sizes) }
            ?: pool.maxByOrNull { sizeScore(it.sizes) }

        val src = preferred?.src ?: return null
        return if (src.startsWith("http")) src
        else "$baseUrl/$src".replace("//", "/").let {
            if (it.startsWith("http:/") && !it.startsWith("http://"))
                it.replace("http:/", "http://") else it
        }
    }

    private fun sizeScore(sizes: String?): Int =
        sizes?.split("x")?.firstOrNull()?.trim()?.toIntOrNull() ?: 0
}

data class PwaIcon(
    val src: String,
    val sizes: String? = null,
    val type: String? = null,
    val purpose: String? = null,   // "any" | "maskable" | "monochrome" (space-separated in spec)
) {
    fun hasPurpose(p: String) = purpose?.split(" ")?.any { it.equals(p, ignoreCase = true) } == true
    fun isPurposeOnly(p: String): Boolean {
        val parts = purpose?.split(" ")?.map { it.trim().lowercase() } ?: return false
        return parts.isNotEmpty() && parts.all { it == p }
    }
}
