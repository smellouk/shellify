# Shellify

## What This Is

Shellify wraps websites in isolated WebView containers with per-app ad blocking, biometric lock, and encrypted backup. It is a local-first Android app — no cloud, no analytics, no account required — that turns any web service into a native-feeling, sandboxed app. The target user values privacy, control, and clean separation between web services.

## Core Value

Each PWA runs in its own isolated, locked, privacy-hardened container — users get native-app convenience without sacrificing control over their data or identity.

## Requirements

### Validated

- ✓ Add, edit, and delete PWA shortcuts with custom icons and names — existing
- ✓ Per-app isolated WebView session (cookie/profile isolation) — existing
- ✓ Per-app and global ad blocking via rule engine — existing
- ✓ Per-app biometric and password lock — existing
- ✓ Encrypted `.pwab` backup and restore (WorkManager-scheduled) — existing
- ✓ GeckoView engine alternative to system WebView — existing
- ✓ In-page Google Translate bridge via JS injection — existing
- ✓ Category organization for PWAs — existing
- ✓ Launcher shortcuts and shortcut trampoline activity — existing
- ✓ QR code and `shellify://add` deep link for sharing PWA configs — existing
- ✓ Material You dynamic theming — existing
- ✓ First-run onboarding flow — existing

### Active

- [ ] Web integration: "Open with" Shellify from any app, domain-matched PWA routing
- [ ] `shellify://open` deep link and HTTPS App Links verification
- [ ] Privacy hardening: stealth mode, cookie auto-wipe, incognito sessions, panic button, tracker blocking
- [ ] Tor / .onion routing via GeckoView SOCKS5 proxy per app
- [ ] Productivity layer: per-app custom JS/CSS injection, download manager, force dark mode, font size
- [ ] On-device analytics: session tracking, usage insights, ads-blocked stats
- [ ] Launcher mode: Shellify as Android home screen with native app drawer
- [ ] Home screen widget (4×2 PWA icon grid)
- [ ] PWA directory: browse and one-tap install curated web apps
- [ ] Web Push notifications via GeckoView service worker bridge

### Out of Scope

- Cloud-side analytics / telemetry — privacy principle: all data stays on-device
- Cloud backup / WebDAV sync — deferred; backup infrastructure is complete but cloud target adds server dependency
- Tags (many-to-many labeling) — categories cover v1 needs; tags add schema complexity without proportional value
- Reader mode — nice-to-have, lower leverage than other productivity features in v1
- Password manager (autofill) — high security complexity; deferred post-Tor/privacy hardening

## Context

- Clean Architecture + MVVM; manual DI via `ShellifyApplication`; no Hilt/Koin
- Kotlin + Jetpack Compose + Room (SQLCipher) + GeckoView (arm64-only)
- 18 modules: `:app`, `:core:*` (12 modules), `:feature:*` (11 modules)
- `AppDatabase` declares `version = 1` but schema file is `12.json` — **every new migration must fill all gaps**; `exportSchema = false` is currently set
- GeckoView is arm64-only — guard all GeckoView-specific features (Tor, Web Push) with an ABI check
- `AppNavigation.kt` is 452 lines with suppressed complexity; add routes here, do not refactor
- Three independent `OkHttpClient` instances exist — do not add a fourth; reuse existing clients
- Third-party cookies globally enabled for OAuth/SSO — do not change without per-app toggle

## Constraints

- **Platform**: Android minSdk 23 (API 23) — all new features must support Android 6.0+
- **Engine**: GeckoView-dependent features (Tor, Web Push) require arm64 device + GeckoView engine selected
- **Privacy**: No external network calls for analytics, no cloud storage of user data
- **Architecture**: Konsist enforces layer separation at build time — `feature:*` must not import `core:database` or other features
- **DB migrations**: Every schema change requires explicit `Migration` objects; do not change `exportSchema` without a plan for gap migrations

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Local-first, no cloud | Privacy is the product's core identity | ✓ Good |
| Manual DI over Hilt/Koin | Simpler dependency graph for a single-developer project | — Pending |
| GeckoView arm64-only | Avoids multi-ABI APK size explosion | — Pending |
| YOLO mode for GSD | Project is well-understood; fast iteration preferred over checkpoints | — Pending |
| Coarse phase granularity | 4 broad phases cover the full feature backlog without over-slicing | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-15 after initialization*
