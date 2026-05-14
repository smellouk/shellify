package io.shellify.app.core.translate

object TranslateBridge {

    fun buildScript(
        targetLang: String,
        autoTranslate: Boolean,
    ): String {
        // Restrict to IETF-style codes (letters and hyphens only, max 10 chars) so the
        // value is safe to embed in a JS string literal even if the call site ever changes.
        val safeLang = targetLang.filter { it.isLetter() || it == '-' }.take(10)
        return buildScriptInternal(safeLang, autoTranslate)
    }

    private fun buildScriptInternal(targetLang: String, autoTranslate: Boolean): String = """
(function() {
  if (window.__shellifyTranslateLoaded) return;
  window.__shellifyTranslateLoaded = true;

  const TARGET = '${targetLang.replace("'", "\\'")}';

  async function translateText(text) {
    if (!text || !text.trim()) return text;
    try {
      const url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl='
        + encodeURIComponent(TARGET) + '&dt=t&q=' + encodeURIComponent(text);
      const res = await fetch(url);
      const json = await res.json();
      return json[0].map(function(x){ return x[0]; }).join('');
    } catch(e) { return text; }
  }

  async function translatePage() {
    const walker = document.createTreeWalker(
      document.body,
      NodeFilter.SHOW_TEXT,
      { acceptNode: function(n) {
          const tag = n.parentElement && n.parentElement.tagName;
          if (['SCRIPT','STYLE','NOSCRIPT','CODE','PRE'].includes(tag)) return NodeFilter.FILTER_REJECT;
          return n.nodeValue.trim() ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP;
      }}
    );
    const nodes = [];
    while (walker.nextNode()) nodes.push(walker.currentNode);

    for (let i = 0; i < nodes.length; i += 50) {
      const chunk = nodes.slice(i, i + 50);
      const texts = chunk.map(function(n){ return n.nodeValue; });
      const joined = texts.join('\n​​\n');
      const translated = await translateText(joined);
      const parts = translated.split('\n​​\n');
      chunk.forEach(function(n, j) { if (parts[j]) n.nodeValue = parts[j]; });
    }
  }

  window.__shellifyTranslate = translatePage;

  ${if (autoTranslate) "translatePage();" else ""}
})();
""".trimIndent()
}
