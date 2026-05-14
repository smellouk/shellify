# Requirements: Shellify

**Defined:** 2026-05-15
**Core Value:** Each PWA runs in its own isolated, locked, privacy-hardened container — native-app convenience without sacrificing control over data or identity.

## v1 Requirements

### Integration

- [ ] **INTG-01**: User can open any HTTP/HTTPS link from another app in Shellify via the Android "Open with" chooser
- [ ] **INTG-02**: When the incoming URL's domain matches exactly one installed PWA, Shellify opens that PWA directly without prompting
- [ ] **INTG-03**: When the incoming URL matches multiple PWAs, user sees a bottom-sheet chooser to select which PWA to open
- [ ] **INTG-04**: When the incoming URL matches no installed PWA, user is offered "Add as new app" or "Open temporarily" (ephemeral session)
- [ ] **INTG-05**: User can share a URL from another app directly into Shellify via the Android share sheet (ACTION_SEND text/plain)
- [ ] **INTG-06**: App handles `shellify://open?appId=X&url=Y` deep link to open a specific PWA at a given URL
- [ ] **INTG-07**: `shellify://open` links respect the target PWA's lock setting (biometric/password gate before navigation)
- [ ] **INTG-08**: User can share a "link to this app" (`shellify://open?appId=X`) from within the WebView toolbar
- [ ] **INTG-09**: `https://shellify.app/add` and `https://shellify.app/open` links open directly in the app via verified Android App Links (no browser prompt)

### Privacy

- [ ] **PRIV-01**: User can enable stealth mode per app to disguise its icon and name in the Android recents screen
- [ ] **PRIV-02**: User can enable per-app cookie auto-wipe so all session data is cleared when the PWA is closed
- [ ] **PRIV-03**: User can open any PWA in an incognito session (ephemeral profile, no persistence after close)
- [ ] **PRIV-04**: User can trigger a panic button that immediately wipes all Shellify app data with a single gesture
- [ ] **PRIV-05**: User can enable per-app tracker blocking (known tracker domains blocked beyond ad rules)

### Tor

- [ ] **TOR-01**: User can enable per-app Tor routing (available only when GeckoView engine is selected for that app)
- [ ] **TOR-02**: When Tor mode is active, the app routes all traffic through a local SOCKS5 proxy backed by the Tor daemon (tor-android)
- [ ] **TOR-03**: User can add `.onion` URLs as PWAs and open them when Tor mode is active
- [ ] **TOR-04**: User sees a loading indicator during Tor circuit establishment on first connection (~5–10s)
- [ ] **TOR-05**: Tor toggle is visibly disabled in per-app settings when the system WebView engine is selected

### Productivity

- [ ] **PROD-01**: User can write custom JavaScript that is injected into every page load for a given PWA
- [ ] **PROD-02**: User can write custom CSS that is applied to every page for a given PWA
- [ ] **PROD-03**: User can enable forced dark mode per PWA (CSS color inversion for sites without native dark support)
- [ ] **PROD-04**: User can set a custom font size scale per PWA (text zoom override)
- [ ] **PROD-05**: User can download files encountered while browsing a PWA (download manager integration)

### Analytics

- [ ] **ANLT-01**: App automatically records session start/end timestamps for each PWA (on-device only, no telemetry)
- [ ] **ANLT-02**: App tracks launch count and last-used timestamp per PWA
- [ ] **ANLT-03**: App tracks ads-blocked count per session and accumulates a lifetime total per PWA
- [ ] **ANLT-04**: User can view a global Insights screen: top 5 apps by time, total time today / this week, total ads blocked
- [ ] **ANLT-05**: User can view per-app usage stats: launch count, time spent (today / this week / all-time), ads blocked, 7-day spark-line
- [ ] **ANLT-06**: User can clear all usage statistics from Global Settings → Privacy
- [ ] **ANLT-07**: User can disable usage tracking entirely from Global Settings → Privacy (skips session recording)

### Platform

- [ ] **PLAT-01**: User can set Shellify as the default Android home screen launcher (HOME intent-filter)
- [ ] **PLAT-02**: Shellify launcher displays the PWA grid with categories, reusing existing HomeScreen composables
- [ ] **PLAT-03**: User can swipe up from the Shellify launcher to open a native Android app drawer
- [ ] **PLAT-04**: Shellify launcher renders the system wallpaper behind the PWA grid
- [ ] **PLAT-05**: Per-app lock (biometric/password) is enforced when launching PWAs from the Shellify launcher
- [ ] **PLAT-06**: User can add a 4×2 Shellify widget to the Android home screen showing pinned PWA icons as tappable shortcuts

### Discovery

- [ ] **DISC-01**: User can browse a curated list of known PWAs within the app (PWA directory)
- [ ] **DISC-02**: Tapping a PWA directory entry auto-populates the add-app form with URL, name, and icon
- [ ] **DISC-03**: User can search / filter the PWA directory by name or category

### Notifications

- [ ] **NOTF-01**: PWAs can request and receive Web Push notifications via the GeckoView service worker bridge (GeckoView engine only)
- [ ] **NOTF-02**: Push notifications from PWAs appear as standard Android notifications, one channel per app
- [ ] **NOTF-03**: User can manage per-PWA notification permission (allow / block) from the app's settings
- [ ] **NOTF-04**: User can configure per-app notification DND hours to suppress notifications during a configurable time window

## v2 Requirements

### Cloud & Sync

- **SYNC-01**: User can sync `.pwab` backup files to Google Drive
- **SYNC-02**: User can sync `.pwab` backup files to a custom WebDAV endpoint
- **SYNC-03**: WorkManager backup job can upload automatically to the configured cloud target

### Organization

- **ORG-01**: User can assign multiple tags to a PWA (many-to-many, in addition to single category)
- **ORG-02**: User can filter the home screen by tag

### Security

- **SEC-01**: User can autofill saved credentials into a PWA (password manager integration with Android Autofill)

### Browser UX

- **BRWS-01**: User can enter reader mode to strip page clutter for a distraction-free reading experience

## Out of Scope

| Feature | Reason |
|---------|--------|
| External analytics / telemetry | Privacy principle — all data stays on-device, no network calls |
| Cloud backup (v1) | Requires server infrastructure; deferred to v2 |
| Reader mode (v1) | Lower leverage than selected productivity features; deferred to v2 |
| Password manager autofill | High security and UX complexity; deferred post-privacy hardening |
| Floating bubble launcher overlay | OS restrictions make reliable overlays complex; deferred |
| Multi-ABI GeckoView | Avoids APK size explosion; GeckoView features are arm64-only by design |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| INTG-01 | Phase 1 | Pending |
| INTG-02 | Phase 1 | Pending |
| INTG-03 | Phase 1 | Pending |
| INTG-04 | Phase 1 | Pending |
| INTG-05 | Phase 1 | Pending |
| INTG-06 | Phase 1 | Pending |
| INTG-07 | Phase 1 | Pending |
| INTG-08 | Phase 1 | Pending |
| INTG-09 | Phase 1 | Pending |
| PRIV-01 | Phase 2 | Pending |
| PRIV-02 | Phase 2 | Pending |
| PRIV-03 | Phase 2 | Pending |
| PRIV-04 | Phase 2 | Pending |
| PRIV-05 | Phase 2 | Pending |
| TOR-01 | Phase 2 | Pending |
| TOR-02 | Phase 2 | Pending |
| TOR-03 | Phase 2 | Pending |
| TOR-04 | Phase 2 | Pending |
| TOR-05 | Phase 2 | Pending |
| PROD-01 | Phase 3 | Pending |
| PROD-02 | Phase 3 | Pending |
| PROD-03 | Phase 3 | Pending |
| PROD-04 | Phase 3 | Pending |
| PROD-05 | Phase 3 | Pending |
| ANLT-01 | Phase 3 | Pending |
| ANLT-02 | Phase 3 | Pending |
| ANLT-03 | Phase 3 | Pending |
| ANLT-04 | Phase 3 | Pending |
| ANLT-05 | Phase 3 | Pending |
| ANLT-06 | Phase 3 | Pending |
| ANLT-07 | Phase 3 | Pending |
| PLAT-01 | Phase 4 | Pending |
| PLAT-02 | Phase 4 | Pending |
| PLAT-03 | Phase 4 | Pending |
| PLAT-04 | Phase 4 | Pending |
| PLAT-05 | Phase 4 | Pending |
| PLAT-06 | Phase 4 | Pending |
| DISC-01 | Phase 4 | Pending |
| DISC-02 | Phase 4 | Pending |
| DISC-03 | Phase 4 | Pending |
| NOTF-01 | Phase 4 | Pending |
| NOTF-02 | Phase 4 | Pending |
| NOTF-03 | Phase 4 | Pending |
| NOTF-04 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 43 total
- Mapped to phases: 43
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-15*
*Last updated: 2026-05-15 after initial definition*
