#!/usr/bin/env python3
"""
Generate docs/privacy.html and docs/terms.html from docs/legal/*.md.

Run from the repository root:
    python3 scripts/generate_legal.py
"""
import html
import re
from pathlib import Path

ROOT      = Path(__file__).parent.parent
LEGAL_DIR = ROOT / "docs" / "legal"
DOCS_DIR  = ROOT / "docs"


# ── Inline markdown → HTML ────────────────────────────────────────────────────

def inline(text: str) -> str:
    """Convert inline markdown elements to HTML. HTML-escapes plain text."""
    # Split on code spans first so their contents are never further processed.
    parts = re.split(r"(`[^`]+`)", text)
    out = []
    for part in parts:
        if part.startswith("`") and part.endswith("`") and len(part) > 1:
            out.append(f"<code>{html.escape(part[1:-1])}</code>")
        else:
            p = html.escape(part)
            # Bold before italic to handle **...*...*
            p = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", p)
            p = re.sub(r"\*(.+?)\*",     r"<em>\1</em>",         p)
            # Links – URLs in hrefs are kept as-is (already escaped by html.escape)
            p = re.sub(
                r"\[([^\]]+)\]\(([^)]+)\)",
                lambda m: f'<a href="{m.group(2)}" rel="noopener">{m.group(1)}</a>',
                p,
            )
            out.append(p)
    return "".join(out)


# ── Block markdown → HTML ─────────────────────────────────────────────────────

def _parse_table(rows: list[str]) -> str:
    def cells(row: str) -> list[str]:
        return [c.strip() for c in row.strip().strip("|").split("|")]

    headers = cells(rows[0])
    thead = "<tr>" + "".join(f"<th>{inline(h)}</th>" for h in headers) + "</tr>"
    tbody = "".join(
        "<tr>" + "".join(f"<td>{inline(c)}</td>" for c in cells(r)) + "</tr>"
        for r in rows[2:]          # rows[1] is the separator
    )
    return (
        '<table class="data-table">\n'
        f"<thead>{thead}</thead>\n"
        f"<tbody>{tbody}</tbody>\n"
        "</table>"
    )


def convert_body(text: str) -> str:
    """Convert a markdown section body to HTML block elements."""
    # Strip horizontal rules (---) used as section separators in the source.
    text = re.sub(r"^---$", "", text, flags=re.MULTILINE)
    lines = text.strip().splitlines()
    blocks: list[str] = []
    i = 0

    while i < len(lines):
        line = lines[i]

        # Blank line
        if not line.strip():
            i += 1
            continue

        # Table: pipe-containing line followed by a separator row
        if "|" in line and i + 1 < len(lines) and re.match(r"^[\s|:\-]+$", lines[i + 1]):
            table_rows = []
            while i < len(lines) and "|" in lines[i]:
                table_rows.append(lines[i])
                i += 1
            blocks.append(_parse_table(table_rows))
            continue

        # Unordered list
        if re.match(r"^[-*] ", line):
            items: list[str] = []
            while i < len(lines) and re.match(r"^[-*] ", lines[i]):
                items.append(f"<li>{inline(lines[i][2:].strip())}</li>")
                i += 1
            blocks.append("<ul>\n" + "\n".join(items) + "\n</ul>")
            continue

        # Ordered list
        if re.match(r"^\d+\. ", line):
            items = []
            while i < len(lines) and re.match(r"^\d+\. ", lines[i]):
                content = re.sub(r"^\d+\. ", "", lines[i])
                items.append(f"<li>{inline(content.strip())}</li>")
                i += 1
            blocks.append("<ol>\n" + "\n".join(items) + "\n</ol>")
            continue

        # Paragraph: accumulate until blank / list / table
        para: list[str] = []
        while (
            i < len(lines)
            and lines[i].strip()
            and not re.match(r"^[-*] ", lines[i])
            and not re.match(r"^\d+\. ", lines[i])
            and "|" not in lines[i]
        ):
            para.append(lines[i].strip())
            i += 1

        if para:
            raw = " ".join(para)
            body = inline(raw)
            # Trademark paragraph gets a branded callout box.
            if "proprietary trademarks" in raw:
                blocks.append(f'<div class="trademark-box"><p>{body}</p></div>')
            else:
                blocks.append(f"<p>{body}</p>")
        continue

    return "\n".join(blocks)


# ── Markdown file parser ──────────────────────────────────────────────────────

def parse_md(path: Path) -> tuple[str, str, list[tuple[int, str, str]]]:
    """Return (page_title, last_updated, sections).

    sections is a list of (number, title, body_markdown).
    """
    text = path.read_text(encoding="utf-8")

    h1 = re.search(r"^#\s+(.+)$", text, re.MULTILINE)
    page_title = h1.group(1).strip() if h1 else path.stem

    date = re.search(r"\*\*Last updated:\s*([^*]+)\*\*", text)
    last_updated = date.group(1).strip() if date else ""

    pattern = re.compile(r"^##\s+(\d+)\.\s+(.+)$", re.MULTILINE)
    matches = list(pattern.finditer(text))
    sections: list[tuple[int, str, str]] = []
    for idx, m in enumerate(matches):
        num   = int(m.group(1))
        title = m.group(2).strip()
        start = m.end()
        end   = matches[idx + 1].start() if idx + 1 < len(matches) else len(text)
        body  = text[start:end].strip()
        sections.append((num, title, body))

    return page_title, last_updated, sections


# ── Shared HTML fragments ─────────────────────────────────────────────────────

_GH_SVG = (
    '<svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">'
    '<path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07'
    ".55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94"
    "-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58"
    " 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3"
    ".64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0"
    ' 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1'
    ".04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0"
    " 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01"
    ' 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>'
    "</svg>"
)

_LEGAL_CSS = """\
<style>
.legal-wrap {
  max-width: 1100px; margin: 0 auto; padding: 48px 24px 0;
  display: grid; grid-template-columns: 220px 1fr; gap: 56px; align-items: start;
}
.toc {
  position: sticky; top: calc(56px + 24px);
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius-xl); padding: 20px;
  transition: background .25s ease, border-color .25s ease;
}
.toc-title {
  font-size: .6875rem; text-transform: uppercase; letter-spacing: .1em;
  color: var(--text-subtle); font-weight: 700; margin-bottom: 12px;
}
.toc-link {
  display: block; padding: 6px 10px; border-radius: var(--radius-md);
  font-size: .8125rem; color: var(--text-muted); text-decoration: none;
  line-height: 1.4; border: 1px solid transparent; transition: all .15s ease;
}
.toc-link:hover { background: var(--surface-2); color: var(--text); text-decoration: none; }
.toc-link.active {
  background: var(--primary-dim); color: var(--primary);
  border-color: rgba(124,77,255,.2);
}
[data-theme="light"] .toc-link.active { border-color: rgba(98,0,238,.15); }
.toc-sep { height: 1px; background: var(--border); margin: 12px 0; transition: background .25s ease; }
.toc-alt a {
  display: flex; align-items: center; gap: 6px; font-size: .75rem;
  color: var(--text-subtle); text-decoration: none; padding: 5px 10px;
  border-radius: var(--radius-md); transition: all .15s ease;
}
.toc-alt a:hover { color: var(--primary); text-decoration: none; }
.doc { min-width: 0; }
.doc-header {
  margin-bottom: 40px; padding-bottom: 32px;
  border-bottom: 1px solid var(--border); transition: border-color .25s ease;
}
.doc-tag {
  display: inline-block; font-size: .6875rem; font-weight: 700;
  padding: 3px 10px; border-radius: var(--radius-full);
  text-transform: uppercase; letter-spacing: .06em; margin-bottom: 16px;
}
.doc-tag-primary { background: var(--primary-dim); color: var(--primary); }
.doc-tag-accent  { background: rgba(255,179,0,.12); color: var(--accent); }
[data-theme="light"] .doc-tag-accent { background: rgba(230,81,0,.1); color: var(--accent); }
.doc-header h1 {
  font-size: clamp(1.75rem,3vw,2.5rem); font-weight: 900;
  letter-spacing: -.03em; margin-bottom: 12px;
}
.doc-meta { display: flex; gap: 20px; flex-wrap: wrap; color: var(--text-muted); font-size: .875rem; }
.doc-meta a { color: var(--primary); }
.doc-section { margin-bottom: 40px; scroll-margin-top: 96px; }
.doc-section h2 {
  font-size: 1.125rem; font-weight: 700; margin-bottom: 16px; padding-bottom: 12px;
  border-bottom: 1px solid var(--border); display: flex; align-items: center; gap: 10px;
  transition: border-color .25s ease;
}
.sec-num {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; border-radius: var(--radius-full);
  background: var(--surface-2); border: 1px solid var(--border);
  font-size: .6875rem; font-family: var(--font-mono); color: var(--text-subtle); flex-shrink: 0;
}
.doc-section p, .doc-section li { color: var(--text-muted); line-height: 1.85; font-size: .9375rem; }
.doc-section p { margin-bottom: 14px; }
.doc-section ul, .doc-section ol { padding-left: 24px; margin-bottom: 14px; }
.doc-section li { margin-bottom: 4px; }
.doc-section strong { color: var(--text); font-weight: 600; }
.doc-section a { color: var(--primary); }
.data-table {
  width: 100%; border-collapse: collapse; margin: 16px 0; font-size: .875rem;
  border: 1px solid var(--border); border-radius: var(--radius-lg);
  overflow: hidden; transition: border-color .25s ease;
}
.data-table th {
  background: var(--surface-2); padding: 10px 14px; text-align: left; font-weight: 600;
  border-bottom: 1px solid var(--border); color: var(--text-muted);
  font-size: .75rem; text-transform: uppercase; letter-spacing: .05em;
}
.data-table td {
  padding: 10px 14px; border-bottom: 1px solid var(--border);
  color: var(--text-muted); vertical-align: top; transition: background .15s ease;
}
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--surface-2); }
.doc-note {
  border-radius: var(--radius-lg); padding: 16px 20px; margin-bottom: 32px;
  font-size: .9rem; color: var(--text-muted); line-height: 1.7;
}
.doc-note-teal { background: var(--secondary-dim); border: 1px solid rgba(0,229,204,.2); }
[data-theme="light"] .doc-note-teal { border-color: rgba(0,123,101,.2); }
.doc-note-amber { background: rgba(255,179,0,.08); border: 1px solid rgba(255,179,0,.2); }
[data-theme="light"] .doc-note-amber { background: rgba(230,81,0,.06); border-color: rgba(230,81,0,.2); }
.trademark-box {
  background: var(--surface-2); border: 1px solid var(--border);
  border-left: 3px solid var(--primary); border-radius: var(--radius-md);
  padding: 16px 20px; margin: 16px 0;
}
.trademark-box p { margin-bottom: 0 !important; }
@media (max-width: 820px) {
  .legal-wrap { grid-template-columns: 1fr; gap: 0; padding-top: 32px; }
  .toc { position: static; margin-bottom: 32px; }
}
</style>"""

_THEME_SCRIPT = """\
<script>
(function () {
  const root = document.documentElement;
  const btn  = document.getElementById('theme-btn');
  function applyTheme(t) {
    root.dataset.theme = t;
    btn.textContent = t === 'dark' ? '🌙' : '☀️';
    try { localStorage.setItem('shellify-theme', t); } catch (e) {}
  }
  const stored = (() => { try { return localStorage.getItem('shellify-theme'); } catch (e) { return null; } })();
  applyTheme(stored || (window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'));
  btn.addEventListener('click', () => applyTheme(root.dataset.theme === 'dark' ? 'light' : 'dark'));
})();
(function () {
  const links    = document.querySelectorAll('.toc-link[href^="#"]');
  const sections = [...links].map(l => document.querySelector(l.getAttribute('href'))).filter(Boolean);
  function update() {
    let cur = sections[0];
    sections.forEach(s => { if (s.getBoundingClientRect().top < 120) cur = s; });
    links.forEach(l => l.classList.toggle('active', l.getAttribute('href') === '#' + cur?.id));
  }
  window.addEventListener('scroll', update, { passive: true });
  update();
})();
</script>"""


# ── Page assembly ─────────────────────────────────────────────────────────────

def _toc(sections: list[tuple[int, str, str]], alt: list[tuple[str, str]]) -> str:
    items = "\n".join(
        f'    <a class="toc-link" href="#s{n}">{n}. {html.escape(t)}</a>'
        for n, t, _ in sections
    )
    alt_links = "\n".join(
        f'      <a href="{href}">{label}</a>' for label, href in alt
    )
    return (
        "  <aside class=\"toc\" aria-label=\"Table of contents\">\n"
        "    <div class=\"toc-title\">Contents</div>\n"
        f"{items}\n"
        "    <div class=\"toc-sep\"></div>\n"
        f"    <div class=\"toc-alt\">\n{alt_links}\n    </div>\n"
        "  </aside>"
    )


def _sections_html(sections: list[tuple[int, str, str]]) -> str:
    parts = []
    for num, title, body in sections:
        parts.append(
            f'    <div class="doc-section" id="s{num}">\n'
            f'      <h2><span class="sec-num">{num}</span> {html.escape(title)}</h2>\n'
            f"{convert_body(body)}\n"
            "    </div>"
        )
    return "\n\n".join(parts)


def _page(
    *,
    page_title: str,
    meta_desc: str,
    nav_privacy_active: bool,
    nav_terms_active: bool,
    doc_tag: str,
    doc_tag_cls: str,
    h1: str,
    last_updated: str,
    note_cls: str,
    note_body: str,
    toc_html: str,
    sections: str,
) -> str:
    pa = ' class="active nav-hide-mobile"' if nav_privacy_active else ' class="nav-hide-mobile"'
    ta = ' class="active nav-hide-mobile"' if nav_terms_active   else ' class="nav-hide-mobile"'

    return f"""\
<!DOCTYPE html>
<!-- Generated by scripts/generate_legal.py — edit docs/legal/*.md, not this file -->
<html lang="en" data-theme="dark">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{html.escape(page_title)}</title>
<meta name="description" content="{html.escape(meta_desc)}">
<link rel="stylesheet" href="assets/site.css">
{_LEGAL_CSS}
</head>
<body>

<nav class="site-nav" role="navigation" aria-label="Main">
  <div class="nav-inner">
    <a href="index.html" class="nav-logo">Shellify</a>
    <div class="nav-links">
      <a href="index.html" class="nav-hide-mobile">Home</a>
      <a href="privacy.html"{pa}>Privacy Policy</a>
      <a href="terms.html"{ta}>Terms</a>
      <button class="theme-toggle" id="theme-btn" aria-label="Toggle theme" title="Toggle dark/light mode">🌙</button>
      <a href="https://github.com/smellouk/shellify" class="btn-github" rel="noopener">
        {_GH_SVG}
        GitHub
      </a>
    </div>
  </div>
</nav>

<main>
<div class="legal-wrap">

{toc_html}

  <article class="doc">
    <div class="doc-header">
      <span class="doc-tag {doc_tag_cls}">{html.escape(doc_tag)}</span>
      <h1>{html.escape(h1)}</h1>
      <div class="doc-meta">
        <span>Last updated: {html.escape(last_updated)}</span>
        <span>·</span>
        <a href="mailto:contact@shellify.app">contact@shellify.app</a>
      </div>
    </div>

    <div class="doc-note {note_cls}">
      {note_body}
    </div>

{sections}
  </article>
</div>
</main>

<footer class="site-footer">
  <div class="wrap">
    <div class="footer-inner">
      <span class="footer-logo">Shellify</span>
      <div class="footer-links">
        <a href="privacy.html">Privacy Policy</a>
        <a href="terms.html">Terms of Service</a>
        <a href="https://github.com/smellouk/shellify" rel="noopener">GitHub</a>
        <a href="mailto:contact@shellify.app">Contact</a>
      </div>
      <span class="footer-copy">© 2026 Shellify · Apache 2.0</span>
    </div>
  </div>
</footer>

{_THEME_SCRIPT}
</body>
</html>
"""


# ── Per-page generators ───────────────────────────────────────────────────────

def gen_privacy(last_updated: str, sections: list[tuple[int, str, str]]) -> str:
    return _page(
        page_title="Privacy Policy — Shellify",
        meta_desc="Shellify is local-first and collects no personal data. Read our full privacy policy.",
        nav_privacy_active=True,
        nav_terms_active=False,
        doc_tag="Privacy Policy",
        doc_tag_cls="doc-tag-primary",
        h1="Your data stays yours",
        last_updated=last_updated,
        note_cls="doc-note-teal",
        note_body=(
            "<strong>Short version:</strong> Shellify has no backend. It collects no personal "
            "data, creates no user accounts, and uses no analytics or crash-reporting SDKs. "
            "Everything stays on your device, encrypted."
        ),
        toc_html=_toc(sections, [
            ("📄 Terms of Service", "terms.html"),
            ("← Back to Shellify",  "index.html"),
        ]),
        sections=_sections_html(sections),
    )


def gen_terms(last_updated: str, sections: list[tuple[int, str, str]]) -> str:
    return _page(
        page_title="Terms of Service — Shellify",
        meta_desc="Shellify Terms of Service. A local-first Android app with no backend — simple terms, clear intent.",
        nav_privacy_active=False,
        nav_terms_active=True,
        doc_tag="Terms of Service",
        doc_tag_cls="doc-tag-accent",
        h1="Simple rules, clear intent",
        last_updated=last_updated,
        note_cls="doc-note-amber",
        note_body=(
            '<strong>Short version:</strong> Shellify is a local tool. You\'re responsible for '
            'what you do with it. Don\'t use it illegally or to harm others. The "Shellify" name '
            "and logo are trademarks and are not open-source licensed. Governed by German law."
        ),
        toc_html=_toc(sections, [
            ("🔒 Privacy Policy",  "privacy.html"),
            ("← Back to Shellify", "index.html"),
        ]),
        sections=_sections_html(sections),
    )


# ── Entry point ───────────────────────────────────────────────────────────────

def main() -> None:
    _, last_updated, sections = parse_md(LEGAL_DIR / "privacy.md")
    out = DOCS_DIR / "privacy.html"
    out.write_text(gen_privacy(last_updated, sections), encoding="utf-8")
    print(f"✓ {out.relative_to(ROOT)}  ({len(sections)} sections)")

    _, last_updated, sections = parse_md(LEGAL_DIR / "terms.md")
    out = DOCS_DIR / "terms.html"
    out.write_text(gen_terms(last_updated, sections), encoding="utf-8")
    print(f"✓ {out.relative_to(ROOT)}  ({len(sections)} sections)")


if __name__ == "__main__":
    main()
