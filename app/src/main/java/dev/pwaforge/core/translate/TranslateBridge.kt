package dev.pwaforge.core.translate

object TranslateBridge {

    fun buildScript(
        targetLang: String,
        instanceUrl: String,
        autoTranslate: Boolean,
    ): String = """
(function() {
  if (window.__pwaforgeTranslateLoaded) return;
  window.__pwaforgeTranslateLoaded = true;

  const TARGET = '${targetLang.replace("'", "\\'")}';
  const INSTANCE = '${instanceUrl.trimEnd('/').replace("'", "\\'")}';

  async function translateBatch(texts) {
    try {
      const res = await fetch(INSTANCE + '/translate', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({q: texts, source: 'auto', target: TARGET, format: 'text'}),
      });
      const json = await res.json();
      return Array.isArray(json.translatedText) ? json.translatedText : [json.translatedText];
    } catch(e) { return texts; }
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
      const translated = await translateBatch(chunk.map(function(n){ return n.nodeValue; }));
      chunk.forEach(function(n, j) { if (translated[j]) n.nodeValue = translated[j]; });
    }
  }

  window.__pwaforgeTranslate = translatePage;

  ${if (autoTranslate) "translatePage();" else ""}
})();
""".trimIndent()
}
