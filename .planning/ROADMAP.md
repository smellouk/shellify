# Roadmap: Shellify

**Created:** 2026-05-15
**Milestone:** v2 — Privacy-First Feature Parity
**Phases:** 23 (coarse granularity)
**Requirements:** 43 v1 requirements + 5 test migration requirements, 100% mapped

---

## Summary

| # | Phase | Goal | Requirements | Success Criteria |
|---|-------|------|--------------|------------------|
| 1 | Web Integration | Shellify as the default web handler | INTG-01–09 (9) | 5 |
| 2 | Privacy & Tor | 5/5 | Complete   | 2026-05-25 |
| 3 | Productivity & Insights | Power-user tooling and on-device usage awareness | PROD-01–05, ANLT-01–07 (12) | 5 |
| 4 | Platform & Discovery | Shellify as a full Android citizen | PLAT-01–06, DISC-01–03, NOTF-01–04 (13) | 5 |
| 5 | E2E Test Migration | Replace Espresso E2E tests with Maestro | TEST-01–05 (5) | 4 |
| 6 | PWA Notification Handling | Complete in-app notification experience for PWAs | TBD | TBD |
| 7 | Run Music in the Background | Keep audio playing from PWAs when the screen is off or the app is backgrounded | TBD | TBD |
| 8 | Swipe to Refresh in WebViewActivity | Let users pull-to-refresh the current page in any PWA | STR-01–06 | 4 |
| 9 | Inject JS Script to Website for PWA Editing | Allow users to inject custom JavaScript into any PWA for per-app page customisation | TBD | TBD |
| 10 | Browser Fingerprint Hard Ghosting | Spoof or randomise browser fingerprint signals per PWA to prevent cross-site tracking | TBD | TBD |
| 11 | Web Casting Support | Cast media from any PWA to Chromecast or other casting targets | TBD | TBD |
| 12 | Picture-in-Picture Video | Float a video from any PWA over other apps while multitasking | TBD | TBD |
| 13 | App Shortcuts (Long-Press Icon) | Android adaptive shortcuts menu per PWA for quick in-app actions | TBD | TBD |
| 14 | Notification Badges | Show unread count on PWA home screen icons | TBD | TBD |
| 15 | Find in Page | In-page text search triggered from the control center | TBD | TBD |
| 16 | Shareable App Configs | Export a PWA's full config as a shellify:// link or QR code for community sharing | TBD | TBD |
| 17 | Reading Mode | Strip page chrome to article text and images, à la Firefox Reader View | TBD | TBD |
| 18 | Per-App Custom Proxy | Set a custom SOCKS5/HTTP proxy per PWA beyond the global Tor option | TBD | TBD |
| 19 | Usage Limits Per App | Daily time cap with a soft block screen, integrated with analytics data | TBD | TBD |
| 20 | Import Bookmarks from Chrome/Firefox as Apps | One-tap migration of browser bookmarks into Shellify PWAs | TBD | TBD |
| 21 | Profiles Support | Guest and named profiles to switch between independent sets of added apps | TBD | TBD |
| 22 | Network Request Log | 6/6 | Complete    | 2026-05-24 |
| 23 | Console Log Viewer | Surface console.log output in a slide-up panel for custom JS debugging | TBD | TBD |
| 24 | Community Plugin Engine | Install community plugins from a marketplace to extend per-PWA behaviour via JS injection | TBD | TBD |

---

## Phase 1: Web Integration

**Goal:** Make Shellify the default web handler — incoming links route into the right PWA, and users can share links back out with a Shellify deep link.

**Requirements:** INTG-01, INTG-02, INTG-03, INTG-04, INTG-05, INTG-06, INTG-07, INTG-08, INTG-09

**Plans:** 5 plans (3 original + 2 gap-closure)

Plans:

**Wave 1**
- [x] 01-01-PLAN.md — Domain foundation: FindAppsForUrlUseCase, DeepLinkHandler.parseOpen/buildOpen, incognito WebViewActivity, link-dispatcher module scaffold

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 01-02-PLAN.md — LinkDispatcherActivity with three-state bottom sheet, LinkDispatcherViewModel, string resources (EN/FR/AR)
- [x] 01-03-PLAN.md — AppShareSheet "Copy app link" extension, App Links /open manifest entry

**Wave 3** *(gap closure — blocked on Wave 2 completion; from 01-VERIFICATION.md)*
- [x] 01-04-PLAN.md — Fix AddAsNew base64url encoding (Gap 1, INTG-04); add 22 missing FR/AR translations (Gap 3)

**Wave 4** *(gap closure — blocked on Wave 3 completion; touches LinkDispatcherActivity after Plan 04)*
- [x] 01-05-PLAN.md — Extract WebViewIntentFactory into new core:navigation module; drop feature:link-dispatcher → feature:webview gradle dep; Konsist test enforces no cross-feature imports (Gap 2)

**Cross-cutting constraints:**
- `core:domain` must use `java.net.URI` (not `android.net.Uri`) — all use cases in `FindAppsForUrlUseCase`
- `android:autoVerify` must NOT appear on the generic http/https intent-filter in `feature/link-dispatcher` manifest
- All new string resources must be added to EN, FR, and AR locale files simultaneously

**Success Criteria:**
1. Tapping a link in Gmail/Slack/any app shows Shellify in the Android "Open with" chooser
2. A URL matching a known PWA domain opens that PWA directly with no bottom sheet
3. A URL matching multiple PWAs shows a bottom-sheet listing the matches
4. `shellify://open?url=Y` opens the matching PWA (URL-only per D-04); locked apps gate with biometrics via WebViewActivity
5. `https://shellify.app/open?url=…` opens directly in the Shellify dispatcher with no browser intermediary

**Dependencies:** None — no schema changes required

---

## Phase 2: Privacy & Tor

**Goal:** Give users maximum per-app control over their privacy footprint and enable anonymous browsing via the Tor network.

**Requirements:** PRIV-01, PRIV-02, PRIV-03, PRIV-04, PRIV-05, TOR-01, TOR-02, TOR-03, TOR-04, TOR-05

**Plans:** 5/5 plans complete

Plans:

**Wave 1** *(parallel — no dependencies between these plans)*
- [x] 02-01-PLAN.md — Domain + DB foundation: 6 new WebApp boolean fields (stealthMode, cookieAutoWipe, alwaysIncognito, trackerBlockingEnabled, useTor, preserveTorIdentity); MIGRATION_5_6 (5→6); AppDatabase v6 bump; WebAppMapper round-trip; instrumented Migration5To6Test
- [x] 02-04-PLAN.md — Tor infrastructure (autonomous=false, package legitimacy checkpoint): Guardian Project Maven repo + tor-android:0.4.9.8 + jtorctl:0.4.5.7; ProxyConfig sealed class (None/Socks5/Http); TorState sealed class; TorManager skeleton (StateFlow + grace-period release + NEWNYM); GeckoEngineManager refactor to multi-runtime cache keyed by ProxyConfig

**Wave 2** *(depends on Plan 01)*
- [x] 02-02-PLAN.md — Privacy hardening (PRIV-01/02/03/05): AdBlockFilterCache tracker rule set + easyprivacy_domains.txt asset; AdBlocker trackerBlockingEnabled gate; WebViewActivity stealth-aware applyTaskDescription + onStop cookie auto-wipe + alwaysIncognito auto-route; WebViewControlCenter stealth toggle + open incognito row; HomeScreen long-press Open incognito; AppSettingsScreen Privacy section (4 toggles); 11 new EN/FR/AR strings

**Wave 3** *(depends on Plan 02 — shares WebViewActivity / WebViewControlCenter)*
- [x] 02-03-PLAN.md — Panic button (PRIV-04): WebViewViewModel.executePanicWipe (clearData per app + DeleteAllAppsUseCase + ThemeManager/PasswordManager clearAll + NavigateHome); WebViewServiceProvider extended with deleteAllApps + getWebApps; WebViewActivity panic icon with 2000ms long-press + ConfirmDialog; 5 new EN/FR/AR strings

**Wave 4** *(depends on Plans 01, 02, 03, and 04 — wires Tor end-to-end)*
- [x] 02-05-PLAN.md — Tor UI + integration (TOR-01–05 + D-07): ShellifyApplication TorManager wiring; WebViewServiceProvider.torManager; WebViewViewModel TorState gate on LoadUrl + onAppReady/onSessionStop/onNewTorIdentity; GeckoViewEngine ProxyConfig threading; WebViewActivity bootstrap chip + incognito badge; WebViewControlCenter New Tor identity row; AppSettingsScreen Tor section (Route through Tor toggle disabled when SYSTEM_WEBVIEW, Preserve identity toggle, New Tor identity row); AddViewModel .onion exemption; 11 new EN/FR/AR strings

**Cross-cutting constraints:**
- DB migration 5→6 requires explicit Migration object + committed `6.json` schema
- TorManager MUST NOT create its own OkHttpClient (CONCERNS.md flag)
- `loadUrl()` MUST be gated on `TorState.Ready` for Tor apps — never call before the bootstrap completes
- All new string resources added to EN, FR, and AR locale files simultaneously
- Guardian Project Maven artifacts require blocking-human verification (Plan 04 Task 0) before install lines land


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

## Phase 5: E2E Test Migration

**Goal:** Replace all Espresso-based E2E instrumentation tests with Maestro flows — eliminating JVM/device flakiness, enabling plain-YAML test authoring, and unblocking faster CI iteration.

**Requirements:** TEST-01, TEST-02, TEST-03, TEST-04, TEST-05

**Plans:**
1. Audit & inventory — catalogue every existing Espresso E2E test class; map each to a user flow; mark unit/integration tests that stay as-is (out of scope)
2. Maestro setup — add `maestro` CLI to CI toolchain; create `.maestro/` directory at repo root; update `run-instrumentation-tests` composite action to execute `maestro test` instead of `connectedDebugAndroidTest`
3. Flow migration — rewrite each inventoried Espresso flow as a `.yaml` Maestro flow file (one file per user journey: add app, open app, lock/unlock, backup/restore, settings)
4. Remove Espresso E2E code — delete migrated test classes from `app/src/androidTest/`; keep any non-E2E instrumentation tests (e.g. Room migration tests) untouched
5. CI wiring — update `pull-request.yml`, `main.yml`, and `build_apk.yml` instrumentation-test jobs to run Maestro flows against an emulator; upload Maestro test reports as artifacts

**Success Criteria:**
1. All previously covered E2E user journeys have an equivalent Maestro `.yaml` flow and pass on CI
2. No Espresso E2E test classes remain in `app/src/androidTest/` (Room migration tests exempted)
3. `pull-request.yml` instrumentation-test job runs Maestro flows and fails the PR on regression
4. Maestro test report artifact is uploaded on every CI run

**Dependencies:**
- Maestro requires a running Android emulator or physical device on CI — verify emulator AVD setup in `run-instrumentation-tests` action
- Room migration tests in `app/src/androidTest/` must be identified and excluded from deletion

---

## Phase 6: PWA Notification Handling

**Goal:** Deliver a complete in-app notification experience for PWAs — receive, display, persist, and act on Web Push notifications sent by installed web apps, with per-app permission controls and DND scheduling.

**Requirements:** NOTF-01, NOTF-02, NOTF-03, NOTF-04

**Depends on:** Phase 4 (Platform & Discovery) — Web Push infrastructure via GeckoView

**Plans:** 6 plans across 3 waves

**Wave 1** *(foundation — parallel)*
- [x] 06-01-PLAN.md — Domain layer (NotificationPermission, PwaNotification, WebApp fields, 4 use cases) + DB migration 2→3 + NotificationEntity/DAO/RepositoryImpl
- [x] 06-02-PLAN.md — core:webbridge module (NotificationBridge, ShellifyBridge, JsInjector) + TranslateBridge migration from core:translate

**Wave 2** *(engine + UI — parallel, depends on Wave 1)*
- [x] 06-03-PLAN.md — Engine: BrowserEngineCallback extension + WebNotificationDelegate + PermissionDelegate in GeckoViewEngine
- [x] 06-04-PLAN.md — Settings UI: Notifications section in AppSettingsScreen, TimePickerHourDialog, DndTimeRow, AppSettingsViewModel methods, 19 strings (EN/FR/AR), Roborazzi goldens

**Wave 3** *(integration — depends on Wave 2)*
- [x] 06-05-PLAN.md — WebViewActivity wiring: PwaNotificationDispatcher (permission/DND/rate-limit gates), ShellifyBridge attachment, NotificationBridge injection, permission AlertDialog, POST_NOTIFICATIONS runtime request
- [x] 06-06-PLAN.md — BackgroundNotificationService (ForegroundService with specialUse type) + NotificationHistoryScreen + AppNavigation route + ShellifyApplication DI + 30-day TTL pruning + instrumented navigation test

**Cross-cutting constraints:**
- DB migration 2→3 requires explicit Migration object + regenerated schemas/3.json (exportSchema = true)
- core:webbridge migration removes TranslateBridge from core:translate; all callers updated to new package
- feature:settings must NOT import feature:webview (Konsist); BackgroundNotificationService start/stop intents constructed in AppNavigation, not in AppSettingsScreen
- POST_NOTIFICATIONS runtime permission required on API 33+; FOREGROUND_SERVICE_SPECIAL_USE permission required on API 34+
- Per-app rate limit: max 100 notifications per app per UTC day (T-06-04 mitigation)
- All 19 new string keys + 3 bg-service strings added to EN/FR/AR simultaneously

**Success Criteria:**
1. A GeckoView PWA calling `new Notification('title', {body, icon})` results in an Android notification posted via NotificationManagerCompat with channel id `pwa_notifications_{isolationId}`
2. A System WebView PWA calling `new Notification(...)` after the ShellifyBridge is injected fires the bridge and posts an Android notification (best-effort; service workers not supported on System WebView)
3. First-time notification permission request shows a Material 3 AlertDialog with the PWA name; the user's choice persists as GRANTED/DENIED on the WebApp
4. AppSettings → Notifications section lets the user toggle permission, set DND start/end hours (hour-only TimePicker), toggle background notifications (GeckoView only), and open the per-app history
5. DND active for the current hour silently drops notifications (no post, no save), including midnight-crossing windows (e.g. 22→8)
6. NotificationHistoryScreen lists notifications for the app sorted newest-first with relative timestamps; auto-pruned to 30 days at app startup
7. BackgroundNotificationService keeps GeckoRuntime alive when backgroundNotificationsEnabled = true; respects per-app coordination with WebViewActivity to avoid duplicate sessions

---

## Phase 7: Run Music in the Background

**Goal:** Keep audio playing from PWAs when the screen is off or the app is backgrounded — enabling Spotify, YouTube Music, podcast, and radio PWAs to behave like native media apps.

**Requirements:** TBD — run `/gsd-plan-phase 7` to define

**Depends on:** None

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 8: Swipe to Refresh in WebViewActivity

**Goal:** Let users pull-to-refresh the current page in any PWA via a standard swipe-down gesture, matching the UX of native Android apps.

**Requirements:** STR-01, STR-02, STR-03, STR-04, STR-05, STR-06

**Depends on:** None

**Plans:** 3 plans across 2 waves

**Wave 1** *(foundation)*
- [x] 08-01-PLAN.md — Domain + DB layer: WebApp.swipeToRefreshEnabled, WebAppEntity column, MIGRATION_3_4 (3→4), WebAppMapper round-trip, migration test + mapper tests, 4.json schema commit

**Wave 2** *(parallel — both depend on Wave 1)*
- [x] 08-02-PLAN.md — Engine + View layer: swiperefreshlayout 1.2.0 dependency, GeckoViewEngine geckoScrollY field + canScrollUp(), SwipeRefreshLayout wrapping container in WebViewActivity, isRefreshing=false in onPageFinished + onError + onSslError
- [x] 08-03-PLAN.md — Settings UI: AppSettingsViewModel.toggleSwipeToRefresh(), ToggleListItem in Control center section, settings_swipe_to_refresh strings in EN/FR/AR, ViewModel unit test, Roborazzi golden update

**Cross-cutting constraints:**
- BrowserEngine.reload() already exists — do NOT modify BrowserEngine.kt
- SwipeRefreshLayout wraps container FrameLayout; container is not replaced
- isRefreshing = false must be set in onPageFinished, onError, AND onSslError
- Room migration 3→4 requires MIGRATION_3_4 object and committed 4.json schema file
- All string resources added to EN, FR, and AR locale files simultaneously

**Success Criteria:**
1. Pulling down from the top of any PWA page triggers the circular spinner and reloads the page via engine.reload()
2. The spinner is tinted to the PWA themeColor (with fallback to holo_blue_bright); spinner disappears when the page finishes loading or on error
3. Per-app toggle in AppSettings Control center section lets users disable swipe-to-refresh; with the toggle off, SwipeRefreshLayout.isEnabled = false and no spinner appears
4. GeckoView engine does not trigger swipe-to-refresh when the page is scrolled below the top (canScrollUp() guard active via geckoScrollY reset on navigation)

---

## Phase 9: Inject JS Script to Website for PWA Editing

**Goal:** Allow users to write and inject custom JavaScript into any PWA on page load, enabling per-app tweaks such as hiding UI elements, overriding behaviours, or adding convenience scripts.

**Requirements:** TBD — run `/gsd-plan-phase 9` to define

**Depends on:** None

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 10: Browser Fingerprint Hard Ghosting

**Goal:** Spoof or randomise per-PWA browser fingerprint signals (User-Agent, canvas, WebGL, fonts, screen metrics, timezone, language) so each app presents a distinct, unlinkable identity to tracking scripts.

**Requirements:** TBD — run `/gsd-plan-phase 10` to define

**Depends on:** Phase 2 (Privacy & Tor) — builds on the privacy hardening and per-app isolation foundations

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 11: Web Casting Support

**Goal:** Let users cast media from any PWA to Chromecast or other casting targets directly from the control center.

**Requirements:** TBD — run `/gsd-plan-phase 11` to define

**Depends on:** None

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 12: Picture-in-Picture Video

**Goal:** Float a playing video from any PWA in a resizable overlay so users can keep watching while switching apps or returning to the home screen.

**Requirements:** TBD — run `/gsd-plan-phase 12` to define

**Depends on:** None

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 13: App Shortcuts (Long-Press Icon)

**Goal:** Surface per-PWA Android adaptive shortcuts on long-press of the home screen icon, enabling quick actions like "Open inbox" or "New post" without opening the app first.

**Requirements:** TBD — run `/gsd-plan-phase 13` to define

**Depends on:** None

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 14: Notification Badges

**Goal:** Show unread count badges on PWA home screen icons, giving users at-a-glance awareness of pending notifications without opening the app.

**Requirements:** TBD — run `/gsd-plan-phase 14` to define

**Depends on:** Phase 6 (PWA Notification Handling) — badge counts driven by Web Push notifications

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 15: Find in Page

**Goal:** Provide an in-page text search (Ctrl+F equivalent) accessible from the WebView control center, letting users locate content on any page.

**Requirements:** TBD — run `/gsd-plan-phase 15` to define

**Depends on:** None

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 16: Shareable App Configs

**Goal:** Let users export a PWA's full configuration (URL, icon, custom JS, CSS, block rules) as a `shellify://` deep link or QR code so the community can share ready-made app setups.

**Requirements:** TBD — run `/gsd-plan-phase 16` to define

**Depends on:** Phase 9 (Inject JS Script) — config bundle includes custom scripts

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 17: Reading Mode

**Goal:** Strip page chrome down to article text and images, à la Firefox Reader View, for distraction-free reading in any PWA.

**Requirements:** TBD — run `/gsd-plan-phase 17` to define

**Depends on:** None

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 18: Per-App Custom Proxy

**Goal:** Allow users to configure a custom SOCKS5 or HTTP proxy for each PWA individually, enabling corporate VPN routing, regional access, and granular network control beyond the global Tor option.

**Requirements:** TBD — run `/gsd-plan-phase 18` to define

**Depends on:** Phase 2 (Privacy & Tor) — builds on per-app network routing foundations

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 19: Usage Limits Per App

**Goal:** Let users set a daily time cap per PWA with a soft block screen when the limit is reached, positioning Shellify as a digital wellbeing tool alongside its privacy features.

**Requirements:** TBD — run `/gsd-plan-phase 19` to define

**Depends on:** Phase 3 (Productivity & Insights) — session tracking data powers the time accounting

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 20: Import Bookmarks from Chrome/Firefox as Apps

**Goal:** Let users import browser bookmarks from Chrome or Firefox and convert them into Shellify PWAs in one step, dramatically lowering the migration barrier for new users.

**Requirements:** TBD — run `/gsd-plan-phase 20` to define

**Depends on:** None

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 21: Profiles Support

**Goal:** Introduce guest and named profiles so users can maintain independent sets of apps, sessions, and settings — enabling work/personal separation or shared-device use cases.

**Requirements:** TBD — run `/gsd-plan-phase 21` to define

**Depends on:** Phase 2 (Privacy & Tor) — isolation primitives underpin per-profile separation

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 22: Network Request Log

**Goal:** Surface a lightweight per-session log of every domain contacted, request blocked, and data volume transferred for each PWA, building transparency and trust with privacy-focused users.

**Requirements:** D-01, D-02, D-03, D-04, D-05 (from CONTEXT.md)

**Depends on:** None

**Plans:** 6/6 plans complete

**Wave 1** *(parallel — no dependencies)*
- [x] 22-01-PLAN.md — core:domain layer: NetworkRequestLog model, NetworkRequestLogRepository interface, LogNetworkRequestUseCase, GetNetworkLogUseCase, DeleteOldNetworkLogsUseCase + unit tests
- [x] 22-02-PLAN.md — Engine capture: BrowserEngineCallback.onRequestIntercepted default no-op, SystemWebViewEngine block-body refactor + sub-resource callback, GeckoViewEngine ContentBlocking.Delegate + onPageStart + engine unit tests

**Wave 2** *(depends on Wave 1 domain layer)*
- [x] 22-03-PLAN.md — core:database layer: NetworkRequestLogEntity, NetworkRequestLogDao, NetworkRequestLogMapper, NetworkRequestLogRepositoryImpl, MIGRATION_4_5, AppDatabase v5 bump + migration test

**Wave 3** *(parallel — both depend on Wave 1; Plan 04 also depends on Wave 2)*
- [x] 22-04-PLAN.md — feature:webview in-session layer: NetworkRequestLogger, NetworkLogSheet (live ModalBottomSheet overlay), WebViewControlCenter network log entry, WebViewActivity wiring, WebViewServiceProvider use case extension
- [x] 22-05-PLAN.md — feature:settings history screen: NetworkLogHistoryViewModel (session-grouped UiState), NetworkLogHistoryScreen (Scaffold + LazyColumn), ViewModel unit tests + Roborazzi screenshot tests

**Wave 4** *(depends on Wave 3 outputs)*
- [x] 22-06-PLAN.md — Integration: Screen.NetworkLog route, AppNavigation composable, ShellifyApplication DI wiring + startup prune, AppSettingsViewModel command + AppSettingsScreen entry, all 13 string resources (EN + FR + AR)

**Success Criteria:**
1. Tapping the Network log button in the WebViewActivity control center opens a live bottom sheet listing domains contacted during the current session, with blocked domains shown in red with a shield icon
2. Domain rows expand (tap) to reveal individual full URLs via AnimatedVisibility
3. When the GeckoView engine is active, a muted notice informs the user that detailed per-request logging is not available
4. AppSettings -> Network log navigates to a 30-day history screen showing past sessions grouped by date, with the same blocked-domain visual treatment
5. DB migration 4->5 creates the network_request_logs table; existing data is preserved; app does not crash on upgrade
---

## Phase 23: Console Log Viewer

**Goal:** Surface `console.log`, `console.warn`, and `console.error` output from any PWA in a slide-up developer panel, making custom JS injection (Phase 9) practical to debug.

**Requirements:** TBD — run `/gsd-plan-phase 23` to define

**Depends on:** Phase 9 (Inject JS Script) — primary use case is debugging injected scripts

**Plans:**
- TBD

**Success Criteria:**
1. TBD

---

## Phase 24: Community Plugin Engine for PWA Apps

**Goal:** Let users discover, install, and manage community-authored plugins from a built-in marketplace. Each plugin injects JavaScript into a target PWA on page load, enabling the community to ship new per-app behaviours (ad-hiders, UI tweaks, automation scripts, dark-mode overrides, etc.) without Shellify releasing a native update. Plugins are sandboxed to their target PWA — they never touch Shellify's own UI or other apps.

**Requirements:** TBD — run `/gsd-plan-phase 24` to define

**Depends on:** Phase 9 (Inject JS Script to Website for PWA Editing) — the plugin runtime reuses the JS injection pipeline established in Phase 9 as its execution layer

**Plans:**
- TBD

**Success Criteria:**
1. TBD

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
*Last updated: 2026-05-25 — Phase 24 added (Community Plugin Engine)*
