# docs/scripts

Build scripts for the Shellify website.

| Script | Purpose |
|---|---|
| `generate_legal.py` | Converts `legal/privacy.md` and `legal/terms.md` to standalone HTML pages, injecting the shared site layout and `assets/site.css` |

Run from the repo root:

```bash
python3 docs/scripts/generate_legal.py
```
