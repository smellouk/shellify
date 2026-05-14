# Codebase Concerns

**Analysis Date:** 2026-05-15

---

## Known Issues

### HTTPS App Links Fall Back to Browser
- Risk: `https://shellify.app/add?...` deep links open in the browser instead of the app.
- Files: `app/src/main/AndroidManifest.xml` (lines 59–69), `.planning/deeplinks.md`
- Cause: `assetlinks.json` has never been hosted at `https://shellify.app/.well-known/assetlinks.json`; Android cannot verify domain ownership at install time.
- Current mitigation: `shellify://` custom scheme works as a primary fallback.
- Fix: Host the `assetlinks.json` with the release certificate SHA-256 fingerprint. Low effort, unblocks HTTPS-based sharing (SMS, email, QR codes).

### Database Version Mismatch
- Risk: Schema file and the Room `@Database` annotation are out of sync — the exported schema at `app/schemas/io.shellify.app.data.local.AppDatabase/12.json` represents version 12, but `AppDatabase.kt` declares `version = 1`.
- Files: `core/database/src/main/java/io/shellify/app/data/local/AppDatabase.kt:18`, `app/schemas/io.shellify.app.data.local.AppDatabase/12.json`
- Impact: If the schema is ever bumped in code without a matching migration, Room will crash on existing installs. The `exportSchema = false` flag additionally means Room does not write a schema JSON during build, so future audits cannot track incremental changes.
- Fix: Set `exportSchema = true`, align the `version` constant with the highest schema file number, and add explicit `Migration` objects for every version gap.

---

## TODO/FIXME Hotspots

### Single XXX Comment (SAF URI Helper)
- File: `feature/settings/src/main/java/io/shellify/app/presentation/settings/GlobalSettingsScreen.kt:1886`
- Context: Private helper `uriToDisplayName()` notes the SAF tree URI format `"primary:Download/Shellify" or "XXXX-XXXX:Backups"`. Not a blocking issue but marks a brittle string-parsing path for removable storage volume IDs.

No other `TODO`, `FIXME`, or `HACK` markers exist in the Kotlin source. The codebase is in good shape in this regard.

---

## Technical Debt Areas

### AppNavigation Function Suppresses Complexity Detector
- Issue: `AppNavigation.kt` carries `@Suppress("CognitiveComplexMethod")` on a 452-line routing function.
- File: `app/src/main/java/io/shellify/app/presentation/navigation/AppNavigation.kt:90`
- Impact: The navigation graph is a single monolithic `when`/composable block. Adding new routes increases risk of navigation regressions and makes it harder to test individual routes in isolation.
- Fix: Split into per-feature navigation sub-graphs using `NavGraphBuilder` extension functions, one per feature module.

### GlobalSettingsScreen is Extremely Large (2,222 lines)
- File: `feature/settings/src/main/java/io/shellify/app/presentation/settings/GlobalSettingsScreen.kt`
- Impact: The single file contains all settings sections, helper composables, and utility functions. Locating a specific setting requires scanning hundreds of lines. Merge conflicts are frequent.
- Fix: Extract each major settings section (engine, backup, privacy, translate, appearance) into its own composable file under `feature/settings/src/main/java/io/shellify/app/presentation/settings/sections/`.

### OnboardingScreen is Very Large (1,989 lines)
- File: `feature/onboarding/src/main/java/io/shellify/app/presentation/onboarding/OnboardingScreen.kt`
- Impact: Similar to GlobalSettingsScreen — all onboarding pages live in one file, complicating targeted changes.
- Fix: One composable file per onboarding step/page.

### Multiple Independent OkHttpClient Instances
- Issue: Three separate `OkHttpClient` instances are created independently — each with its own thread pool and connection pool.
- Files:
  - `core/pwa/src/main/java/io/shellify/app/core/pwa/FaviconFetcher.kt:19`
  - `core/iconpack/src/main/java/io/shellify/app/core/iconpack/SimpleIconsManager.kt:41`
  - `core/engine/src/main/java/io/shellify/app/core/engine/GeckoEngineManager.kt:73`
- Impact: Each instance holds 5 idle connection threads by default. Three instances = up to 15 idle threads consuming memory. No connection reuse between fetch operations.
- Fix: Create a single shared `OkHttpClient` (e.g., in `core/pwa` or a new `core/network`) and inject it via the DI graph.

### Mixed Storage Primitives (SharedPreferences alongside DataStore)
- Issue: Several modules still use raw `SharedPreferences` instead of the project-wide DataStore approach.
- Files:
  - `core/locale/src/main/java/io/shellify/app/core/locale/LocaleHelper.kt`
  - `core/iconpack/src/main/java/io/shellify/app/core/iconpack/SimpleIconsManager.kt`
  - `core/engine/src/main/java/io/shellify/app/core/engine/GeckoEngineManager.kt`
  - `core/crypto/src/main/java/io/shellify/app/core/crypto/CryptoManager.kt` (by design — stores encrypted keystore material; acceptable)
- Impact: Inconsistent persistence layer; SharedPreferences writes block the main thread on older devices; no type safety.
- Fix: Migrate `LocaleHelper` and `SimpleIconsManager` to DataStore Preferences. `GeckoEngineManager` stores binary install state — DataStore is appropriate there too.

### Room Schema Export Disabled
- File: `core/database/src/main/java/io/shellify/app/data/local/AppDatabase.kt:19` — `exportSchema = false`
- Impact: Room does not generate schema JSON files during build. Without them, it is impossible to audit migration correctness with Room's built-in `MigrationTestHelper`, and schema drift goes undetected.
- Fix: Set `exportSchema = true` and configure `ksp { arg("room.schemaLocation", ...) }` in `core/database/build.gradle.kts` (the `app` module already has this KSP arg, which applies to the wrong compilation unit).

---

## Risky Dependencies

### GeckoView Pinned to a 2024-07-04 Snapshot (10+ Months Old)
- Version: `128.0.20240704121409` (July 2024)
- Files: `gradle/libs.versions.toml:25`, `core/engine/src/main/java/io/shellify/app/core/engine/GeckoEngineManager.kt:45`
- Risk: GeckoView 128 is well past its security-maintenance window. Mozilla releases security patches roughly every 4 weeks. Running a July 2024 build in mid-2026 means ~22 unpatched browser-engine releases, each potentially carrying CVE fixes for renderer-level vulnerabilities (memory corruption, CSP bypass, etc.).
- Impact: Critical for a privacy-focused app that acts as a browser engine. Users with Gecko mode enabled are exposed to known browser-engine CVEs.
- Fix: Update to the latest stable GeckoView release. The runtime download path in `GeckoEngineManager.downloadAndInstall()` already supports version parameters — update `GECKO_VERSION` and `KNOWN_SHA256` entries.

### GeckoView Gradle Dependency is arm64-v8a Only
- Dependency: `geckoview-arm64-v8a` (the only ABI declared in `libs.versions.toml`)
- Files: `gradle/libs.versions.toml:67`, `core/engine/build.gradle.kts:23`
- Risk: The in-process `GeckoRuntime` initialization path (`GeckoEngineManager.buildRuntime()`) will fail silently or crash on `armeabi-v7a` or `x86_64` devices because the native libs for those ABIs are not bundled in the APK. The runtime download path handles multi-ABI, but only if the download succeeds — first-run experience on non-arm64 hardware is broken.
- Fix: Either declare all four ABI Gradle dependencies, use ABI splits/App Bundles, or clearly guard the GeckoView engine option behind an arm64 runtime check.

### Biometric Library at 1.1.0 (Latest is 1.2.x)
- Version: `androidx.biometric:biometric:1.1.0`
- File: `gradle/libs.versions.toml:17`
- Risk: The 1.1.x line is in maintenance mode. Class 3 biometrics (StrongBox-backed) and the Credential Manager APIs are only available in 1.2+. Some vendors patched CryptoObject bugs only in 1.2.
- Fix: Bump to `1.2.0-alpha05` or the latest stable `1.2.x` once released.

### Room at 2.6.1 (Latest is 2.7.x)
- Version: `androidx.room:room-*:2.6.1`
- File: `gradle/libs.versions.toml:11`
- Risk: Room 2.7 introduced coroutine-native query APIs and performance improvements for large tables. 2.6.1 is not vulnerable but falls behind on async query support.
- Fix: Bump to `2.7.x` when schema migration is resolved to avoid compounding migration risk.

### Coil 2.7.0 (Coil 3.x Available)
- Version: `io.coil-kt:coil-compose:2.7.0`
- File: `gradle/libs.versions.toml:16`
- Risk: Coil 3 is a major rewrite with breaking API changes; not a security risk, but continuing on 2.x will require a larger migration effort the longer it is deferred.
- Fix: Plan a Coil 3 migration alongside any upcoming dependency refresh cycle.

---

## Performance Concerns

### GeckoRuntime Initialized on First WebView Launch
- File: `core/engine/src/main/java/io/shellify/app/core/engine/GeckoEngineManager.kt:132-150`
- Problem: `GeckoRuntime.create()` is called lazily when the first GeckoView engine is requested. This happens on the main thread inside `WebViewActivity` startup, adding 200–500ms of jank on first open.
- Fix: Warm up the runtime in the background during app idle time (e.g., after `HomeScreen` renders) using `lifecycleScope.launch(Dispatchers.IO)`.

### IsolationManager Cookie Restore is Synchronous Before Page Load
- File: `core/isolation/src/main/java/io/shellify/app/core/isolation/IsolationManager.kt:46-50`
- Problem: On API < 33, `restoreSession()` must complete before `loadUrl()` is called. If the encrypted cookie store is large, this adds measurable latency on older devices before the page begins loading.
- Fix: Investigate whether cookies can be partially restored or the restore can be parallelised with early network DNS prefetch.

### WebViewActivity Uses `CoroutineScope(Dispatchers.Main)` Directly
- File: `core/isolation/src/main/java/io/shellify/app/core/isolation/IsolationManager.kt:30`
- Problem: A standalone `CoroutineScope` is created in `IsolationManager` with no lifecycle awareness. If the hosting Activity is destroyed before the scope's jobs finish (e.g., rapid back-press during cookie save), the coroutine leaks until it completes.
- Fix: Pass a lifecycle-bound scope from `WebViewActivity` into `IsolationManager.onSessionEnd()` or use `lifecycleScope`.

---

## Security Considerations

### Third-Party Cookies Enabled Globally
- Risk: `WebViewManager` enables `setAcceptThirdPartyCookies(webView, true)` for all apps unconditionally.
- File: `core/engine/src/main/java/io/shellify/app/core/webview/WebViewManager.kt:43`
- Justification in code: "third-party cookies needed for OAuth / SSO flows"
- Problem: This is a blanket setting rather than a per-app opt-in. Apps that do not use OAuth still receive cross-site tracking cookies, which undermines the isolation model.
- Recommendation: Default to `false`; expose a per-app "Allow third-party cookies" toggle in `AppSettingsScreen` and only enable it for apps where the user opts in.

### Production Logs Not Gated Behind BuildConfig.DEBUG
- Risk: All `Log.i/d/w/e` calls in `core/engine/` fire in release builds.
- Files: `core/engine/src/main/java/io/shellify/app/core/engine/GeckoEngineManager.kt`, `core/engine/src/main/java/io/shellify/app/core/engine/GeckoViewEngine.kt`, `core/engine/src/main/java/io/shellify/app/core/engine/GeckoNativeLoader.kt`
- Problem: Logcat messages in release builds leak internal paths, version strings, SHA-256 hashes, and error details to anyone with ADB access.
- Recommendation: Wrap non-error logs in `if (BuildConfig.DEBUG)` or replace with a thin logging facade that strips debug/info levels in release builds.

### Deprecated Window APIs Used Without API-Level Guard
- Risk: `window.statusBarColor`, `ActivityManager.TaskDescription(name, bitmap)`, and `onBackPressed()` are deprecated but called unconditionally.
- Files:
  - `feature/webview/src/main/java/io/shellify/app/presentation/webview/WebViewActivity.kt:658-683`
  - `feature/settings/src/main/java/io/shellify/app/presentation/settings/GlobalSettingsScreen.kt:171,1423`
- Impact: Not a security concern, but these APIs will be removed in future Android SDK levels; the suppressions will hide compile-time warnings that should drive migration.
- Fix: Replace with `WindowCompat` / `OnBackPressedDispatcher` APIs behind API-level checks.

### Lint `abortOnError = false` and `warningsAsErrors = false`
- File: `app/build.gradle.kts:69-70`
- Problem: Lint issues never fail the build. Security-relevant lint rules (e.g., `SetJavaScriptEnabled`, `AllowAllHostnameVerifier`, `HardcodedDebugMode`) are silently ignored.
- Fix: Enable `abortOnError = true` for CI builds; use `app/config/lint/lint.xml` to explicitly baseline-suppress known intentional issues rather than blanket-disabling error promotion.

---

## Improvement Opportunities

### Analytics Module Not Yet Implemented
- Status: Fully designed in `.planning/analytics.md` but no code exists.
- Missing: `core/analytics/` module, `sessions` table (DB migration 12→13), `WebViewActivity` session hooks, and `feature/analytics/` UI.
- Impact: No usage data is collected; features like "top apps" and "time in app" cannot be built until the session recording layer exists.

### Browser Replacement (LinkDispatcherActivity) Not Yet Implemented
- Status: Fully designed in `.planning/browser-replacement.md` but no code exists.
- Missing: `LinkDispatcherActivity`, `FindAppsForUrlUseCase`, `shellify://open` deep-link path in `MainActivity.onNewIntent`, and outbound share integration in `AppShareSheet`.
- Impact: Shellify does not appear in the Android "Open with…" chooser, which is a key PWA launcher differentiator.

### Tor / .onion Support Not Yet Implemented
- Status: Designed in `.planning/tor-onion-support.md` — no implementation.
- Missing: `TorManager`, SOCKS5 proxy wiring into `GeckoEngineManager`, `WebApp.useTor` DB column, `.onion` validation in `PwaAnalyzer`.

### Launcher Mode Not Yet Implemented
- Status: Designed in `.planning/launcher-mode.md` — no implementation.
- Missing: `LauncherActivity` with `HOME` intent category, wallpaper support, native app drawer.

### No Test Coverage for Core Backup, Isolation, PwaAnalyzer, or Engine
- Files with no unit tests:
  - `core/backup/BackupManager.kt` (417 lines) — only `BackupCryptoTest` exists
  - `core/isolation/IsolationManager.kt`, `CookieJarManager.kt` — zero tests
  - `core/pwa/PwaAnalyzer.kt`, `FaviconFetcher.kt` — zero tests
  - `core/engine/GeckoEngineManager.kt`, `GeckoViewEngine.kt`, `SystemWebViewEngine.kt` — only `AdBlockerTest` and `AdBlockFilterCacheTest` exist
  - `core/crypto/CryptoManager.kt` — zero tests
- Impact: The most security-sensitive and integration-heavy code (crypto, backup, isolation) has the least test coverage. Regressions here are likely to go undetected.
- Priority: High for `CryptoManager` and `BackupManager` (data integrity risk); medium for `IsolationManager` and `PwaAnalyzer`.

### `core/database` Has `exportSchema = false` — Migration Safety Net Missing
- Described above under Technical Debt. Flagged again here as an improvement: enabling schema export and wiring `MigrationTestHelper` in an instrumented test would catch schema mismatches at CI time rather than production runtime.

---

*Concerns audit: 2026-05-15*
