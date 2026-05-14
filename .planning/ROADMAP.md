# Roadmap: Shellify

**Created:** 2026-05-15
**Milestone:** v2 — Privacy-First Feature Parity
**Phases:** 4 (coarse granularity)
**Requirements:** 43 v1 requirements, 100% mapped

---

## Summary

| # | Phase | Goal | Requirements | Success Criteria |
|---|-------|------|--------------|------------------|
| 1 | Web Integration | Shellify as the default web handler | INTG-01–09 (9) | 5 |
| 2 | Privacy & Tor | Maximum per-app isolation and anonymity | PRIV-01–05, TOR-01–05 (10) | 5 |
| 3 | Productivity & Insights | Power-user tooling and on-device usage awareness | PROD-01–05, ANLT-01–07 (12) | 5 |
| 4 | Platform & Discovery | Shellify as a full Android citizen | PLAT-01–06, DISC-01–03, NOTF-01–04 (13) | 5 |

---

## Phase 1: Web Integration

**Goal:** Make Shellify the default web handler — incoming links route into the right PWA, and users can share links back out with a Shellify deep link.

**Requirements:** INTG-01, INTG-02, INTG-03, INTG-04, INTG-05, INTG-06, INTG-07, INTG-08, INTG-09

**Plans:**
1. `LinkDispatcherActivity` — new Activity with `ACTION_VIEW` + `ACTION_SEND` intent filters; domain matching via `FindAppsForUrlUseCase`; bottom-sheet chooser UI
2. `shellify://open` deep link — `parseOpen()` in `DeepLinkHandler`; `MainActivity.onNewIntent` branch; lock-gate via `DeepLinkConfirmDialog`
3. HTTPS App Links — `assetlinks.json` hosted at `shellify.app/.well-known/`; `AppShareSheet` "Share to this app" option; HomeScreen "Copy app link" long-press

**Success Criteria:**
1. Tapping a link in Gmail/Slack/any app shows Shellify in the Android "Open with" chooser
2. A URL matching a known PWA domain opens that PWA directly with no bottom sheet
3. A URL matching multiple PWAs shows a bottom-sheet listing the matches
4. `shellify://open?appId=X&url=Y` opens the correct PWA at the given URL; locked apps gate with biometrics
5. `https://shellify.app/add?url=…` opens directly in the Shellify add-app screen with no browser intermediary

**Dependencies:** None — no schema changes required

---

## Phase 2: Privacy & Tor

**Goal:** Give users maximum per-app control over their privacy footprint and enable anonymous browsing via the Tor network.

**Requirements:** PRIV-01, PRIV-02, PRIV-03, PRIV-04, PRIV-05, TOR-01, TOR-02, TOR-03, TOR-04, TOR-05

**Plans:**
1. Privacy hardening — stealth mode (`ShortcutManager` + recents exclusion); cookie auto-wipe (`IsolationManager.clearSession` on `onStop`); incognito session (ephemeral profile, no DB write); tracking protection (extended domain blocklist in `AdBlocker`)
2. Panic button — hold-gesture in `WebViewActivity`; wipe all `IsolationManager` profiles + Room DB + DataStore; confirmation dialog
3. Tor integration — add `tor-android` dependency; `TorManager` daemon lifecycle (start on first Tor-app launch, stop when none active); dual `GeckoRuntime` in `GeckoEngineManager` (standard + SOCKS5); `WebApp.useTor` flag + DB migration; `.onion` URL support in `PwaAnalyzer` and `AddWebAppScreen`

**Success Criteria:**
1. Enabling stealth mode for a PWA removes its name/icon from the Android recents screen
2. With cookie auto-wipe enabled, cookies and session storage are absent when the app is reopened after close
3. An incognito session leaves no new entries in the sessions table or cookies after it ends
4. Panic button wipe results in an empty app list and all DataStore keys cleared
5. A `.onion` URL loads successfully in GeckoView when Tor mode is active; the Tor daemon starts automatically on first use

**Dependencies:**
- DB migration required for `WebApp.useTor` column (bump `AppDatabase` version, add `Migration` object)
- GeckoView engine must be selected per-app for TOR-01–05 — surface constraint in UI

---

## Phase 3: Productivity & Insights

**Goal:** Give power users control over how pages look and behave, and surface on-device usage data so users understand their browsing patterns.

**Requirements:** PROD-01, PROD-02, PROD-03, PROD-04, PROD-05, ANLT-01, ANLT-02, ANLT-03, ANLT-04, ANLT-05, ANLT-06, ANLT-07

**Plans:**
1. Custom JS/CSS + dark mode + font size — per-app fields in `WebApp` domain model and DB (`customJs`, `customCss`, `forceDarkMode`, `fontSizePercent`); inject in `WebViewActivity.onPageFinished`; settings UI in `AppSettingsScreen`
2. Download manager — handle `WebViewClient.onDownloadRequest` in `WebViewActivity`; `DownloadManager` API integration; progress notification per download
3. Analytics data layer — new `core/analytics` module: `SessionEntity`, `SessionDao` (insert/update/aggregate queries), `AnalyticsRepositoryImpl`; `WebApp.totalAdsBlocked` denormalized column; DB migrations; session start/end hooks in `WebViewActivity.onResume/onStop`; `adsBlockedThisSession()` counter in `AdBlocker` + `WebViewServiceProvider`
4. Analytics UI — `feature/analytics`: `AnalyticsScreen` (global Insights tab) + `AnalyticsViewModel`; per-app stats section in `AppSettingsScreen`; clear-stats and disable-tracking controls in Global Settings → Privacy

**Success Criteria:**
1. Custom JavaScript entered in per-app settings executes on every page load for that PWA
2. Custom CSS entered in per-app settings applies on every page for that PWA; force dark mode visibly inverts colours on a light site
3. Tapping a download link triggers the Android DownloadManager and shows a progress notification
4. The Insights screen shows top 5 apps by time, total time today, and lifetime ads blocked after at least one session
5. Disabling usage tracking in Global Settings stops new session rows from being created; clearing stats results in an empty `sessions` table

**Dependencies:**
- DB migrations for `WebApp` columns (`customJs`, `customCss`, `forceDarkMode`, `fontSizePercent`, `totalAdsBlocked`) and new `sessions` table
- `adsBlockedThisSession()` counter requires changes to `AdBlocker` and `WebViewServiceProvider`

---

## Phase 4: Platform & Discovery

**Goal:** Make Shellify a full Android platform citizen — a launchable home screen, a widget, a notification hub, and a curated entry point for discovering new PWAs.

**Requirements:** PLAT-01, PLAT-02, PLAT-03, PLAT-04, PLAT-05, PLAT-06, DISC-01, DISC-02, DISC-03, NOTF-01, NOTF-02, NOTF-03, NOTF-04

**Plans:**
1. Launcher mode — new `feature/launcher` module with `LauncherActivity` (`HOME` + `DEFAULT` intent-filter); reuse `HomeScreen` composables; `WallpaperManager` background; native app drawer (swipe-up gesture → installed-apps list); `KeyguardManager` lock-screen awareness; per-app lock enforcement on PWA tap
2. Home screen widget — `AppWidgetProvider` implementation in `feature/widget`; 4×2 grid of pinned PWA icons; tap opens `WebViewActivity` for the selected app; `RemoteViews` rendering with Coil-loaded icons
3. PWA directory — static or remotely-fetched JSON catalogue; `feature/directory` with search/filter UI; "Install" tap pre-fills `AddWebAppScreen`; directory entries include URL, name, icon URL, category
4. Web Push notifications — GeckoView `WebPushController` integration in `GeckoEngineManager` (arm64 guard); per-app Android `NotificationChannel`; permission management in `AppSettingsScreen`; per-app DND hour range in DataStore + `AlarmManager`/`NotificationManager` integration

**Success Criteria:**
1. User can select Shellify in "Choose default launcher" and the PWA grid appears as the home screen with the system wallpaper visible behind it
2. Swiping up from the Shellify launcher opens a native-apps drawer listing all installed Android apps
3. A 4×2 Shellify widget can be added to a stock launcher's home screen; tapping a PWA icon opens it directly in `WebViewActivity`
4. The PWA directory lists at least 20 curated entries; tapping one pre-populates the add form with URL and name
5. A test PWA using the Web Push API delivers a notification to the Android notification shade; disabling permission for that app suppresses future notifications

**Dependencies:**
- GeckoView arm64 guard required for NOTF-01 (Web Push); feature must degrade gracefully on non-arm64 devices
- Launcher mode requires `android.permission.SET_WALLPAPER` (or read-only `WallpaperManager.getDrawable`)
- PWA directory JSON source must be defined (static asset bundled in APK or remote URL)

---

## Deferred (v2)

| Feature | Reason |
|---------|--------|
| Cloud backup (Google Drive / WebDAV) | Server infrastructure dependency; local backup is complete |
| Tags (many-to-many) | Categories cover v1; deferred to reduce schema complexity |
| Password manager (Autofill) | High security surface; deferred post-Tor and privacy hardening |
| Reader mode | Lower leverage; deferred post-productivity phase |
| Floating bubble launcher | OS overlay restrictions; deferred |

---

*Roadmap created: 2026-05-15*
*Last updated: 2026-05-15 after initialization*
