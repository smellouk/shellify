package io.shellify.app.core.webbridge

object ReadingModeBridge {

    fun buildScript(readabilityJs: String, noContentMessage: String): String = """
(function() {
  if (window.__shellifyReaderLoaded) return;
  window.__shellifyReaderLoaded = true;

  $readabilityJs

  var docClone = document.cloneNode(true);
  var article = new Readability(docClone).parse();

  if (!article) {
    document.body.innerHTML = '<div style="padding:24px;font-family:sans-serif;">'
      + '<p>' + "${noContentMessage.replace("\"", "\\\"")}" + '</p></div>';
    return;
  }

  var title = article.title || '';
  var byline = article.byline || '';
  var content = article.content || '';

  document.body.innerHTML =
    '<style>' + readerCss() + '</style>'
    + '<div class="shellify-reader">'
    + '<h1 class="shellify-reader-title">' + escapeHtml(title) + '</h1>'
    + (byline ? '<p class="shellify-reader-byline">' + escapeHtml(byline) + '</p>' : '')
    + '<div class="shellify-reader-body">' + sanitizeHtml(content) + '</div>'
    + '</div>';

  function sanitizeHtml(html) {
    var tmp = document.createElement('div');
    tmp.innerHTML = html;
    var all = tmp.querySelectorAll('*');
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      var attrs = Array.prototype.slice.call(el.attributes);
      for (var j = 0; j < attrs.length; j++) {
        var name = attrs[j].name.toLowerCase();
        var val  = attrs[j].value.toLowerCase().replace(/\s/g,'');
        if (name.indexOf('on') === 0 || val.indexOf('javascript:') === 0) {
          el.removeAttribute(attrs[j].name);
        }
      }
    }
    return tmp.innerHTML;
  }

  function escapeHtml(s) {
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }

  function readerCss() {
    return '@media (prefers-color-scheme: light) {'
      + '.shellify-reader { background:#fff; color:#1a1a1a; }'
      + '.shellify-reader-byline { color:#555; }'
      + '}'
      + '@media (prefers-color-scheme: dark) {'
      + '.shellify-reader { background:#1a1a1a; color:#e8e8e8; }'
      + '.shellify-reader-byline { color:#aaa; }'
      + '}'
      + '.shellify-reader { max-width:65ch; margin:0 auto; padding:24px 16px;'
      +   'font-size:18px; line-height:1.7; font-family:sans-serif; }'
      + '.shellify-reader-title { font-size:1.5em; margin-bottom:0.25em; }'
      + '.shellify-reader-byline { font-size:0.9em; margin-top:0; margin-bottom:1.5em; }'
      + '.shellify-reader-body img { max-width:100%; height:auto; display:block; margin:1em 0; }'
      + '.shellify-reader-body { overflow-wrap:break-word; }';
  }
})();
""".trimIndent()
}
