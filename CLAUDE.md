# Shellify — Claude Code Guide

Do not add Co-Authored-By lines to git commits.

**README rule:** Before working in any directory, read its README.md if one exists. When your changes affect a directory, update its README.md to reflect them. When creating a new directory, create a README.md.

Shellify. Wraps websites in isolated WebView containers with per-app ad blocking, biometric lock, and encrypted backup. Local-first, no cloud, no analytics.

---

## Build Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # install on connected device
./gradlew testDebugUnitTest      # unit tests (includes Konsist arch checks)
./gradlew verifyRoborazziDebug   # screenshot regression tests
./gradlew recordRoborazziDebug   # regenerate screenshot goldens (after UI changes)
./gradlew detekt                 # static analysis
./gradlew lintDebug              # lint
./gradlew detekt lintDebug testDebugUnitTest  # full local check suite
```

---

## Quality Checks

After making any code change, run all applicable checks. **Always launch them as parallel Bash tool calls in a single message — never chain them with `&&` or run them sequentially.**

| Check | Command | When to run |
|---|---|---|
| Static analysis | `./gradlew detekt --continue` | Every change |
| Lint | `./gradlew lintDebug --continue` | Every change |
| Unit tests + arch | `./gradlew testDebugUnitTest --continue` | Every change |
| Screenshot regression | `./gradlew verifyRoborazziDebug --continue` | UI changes only |

**Parallel example** — fire all four in one message with separate Bash tool calls:
- Bash: `./gradlew detekt --continue`
- Bash: `./gradlew lintDebug --continue`
- Bash: `./gradlew testDebugUnitTest --continue`
- Bash: `./gradlew verifyRoborazziDebug --continue`  ← skip if no UI touched

If screenshot tests fail after a UI change, regenerate goldens (`./gradlew recordRoborazziDebug --continue`) and commit the updated images alongside the code change.

---

## Architecture

Clean Architecture with strict layer separation enforced at compile time by **Konsist**.

```
feature:*  →  core:domain  (Compose screens + ViewModels, no data access)
core:*     →  core:domain  (infrastructure implementations)
core:domain               (pure Kotlin, zero Android deps)
app                       (NavHost, DI wiring, ShellifyApplication)
```

**Hard rules — Konsist will fail the build if violated:**
- `feature:*` must not import `core:database` directly
- `feature:*` must not import other `feature:*` modules
- `core:domain` must not have any Android dependencies
- ViewModels must use use cases, not repository interfaces directly

DI is **manual** — no Hilt/Koin. Dependencies are wired in `ShellifyApplication` and passed down via ViewModel factories.

---

## Module Layout

```
app/           → ShellifyApplication, MainActivity, NavHost, DI wiring
build-logic/   → Gradle convention plugins (no boilerplate in modules)
core/domain    → Models, repository interfaces, use cases (pure Kotlin)
core/database  → Room + SQLCipher, DAOs, entities, migrations
core/crypto    → AES-256-GCM, Argon2id, PBKDF2
core/security  → Android Keystore, biometrics, password management
core/backup    → Encrypted .pwab backup/restore
core/engine    → WebView abstraction + GeckoView integration + ad-block
core/isolation → Per-app cookie and profile isolation
core/pwa       → PWA manifest parser, favicon/icon fetching
core/translate → In-page translation (Google Translate JS injection)
core/theme     → DataStore-backed Material You theming
core/ui        → Shared Compose components, design system
feature/home   → App list, search, categories
feature/add    → Add/edit PWA form
feature/webview → WebView activity
feature/settings → Per-app and global settings
feature/onboarding → First-run flow
```

### Adding new code

| What | Where |
|---|---|
| New feature screen | `feature/<name>/src/main/java/io/shellify/feature/<name>/` |
| New use case | `core/domain/src/main/java/io/shellify/app/domain/usecase/` |
| New domain model | `core/domain/src/main/java/io/shellify/app/domain/model/` |
| New infrastructure | `core/<name>/` with `shellify.android.library` plugin |
| Shared UI component | `core/ui/src/main/java/io/shellify/app/core/ui/` |
| Feature-local strings | `feature/<name>/src/main/res/values/strings.xml` |
| Shared UI strings | `core/ui/src/main/res/values/strings.xml` |
| Colors | `core/ui/src/main/java/io/shellify/app/presentation/theme/Color.kt` |
| Spacing / sizes | `core/ui/src/main/java/io/shellify/app/presentation/theme/Dimens.kt` |
| Typography / text sizes | `core/ui/src/main/java/io/shellify/app/presentation/theme/Theme.kt` (Typography object) |

---

## Convention Plugins

Always use the appropriate plugin — never configure SDK versions or JVM target manually.

```kotlin
plugins { id("shellify.android.library") }          // Android library
plugins { id("shellify.android.library"); id("shellify.compose") }  // + Compose
plugins { id("shellify.jvm.library") }              // pure Kotlin
plugins { id("shellify.android.library"); id("shellify.ksp") }      // + Room/KSP
```

App module only: `shellify.android.application`

---

## Naming Conventions

| Thing | Convention |
|---|---|
| Classes, composables | `PascalCase` |
| Functions, variables | `camelCase` |
| Constants, enum entries | `SCREAMING_SNAKE_CASE` |
| Boolean properties | Must start with `is`, `has`, `can`, `should`, `show`, `enable`, `disable`, etc. |
| ViewModels | `*ViewModel` |
| UI state classes | `*UiState` (must be `data class`) |
| Use cases | `*UseCase` (must have single `operator fun invoke()`) |
| Repository interfaces | `*Repository` |
| Repository impls | `*RepositoryImpl` |
| Room entities | `*Entity` |
| Mappers | `*Mapper` |
| Root package | `io.shellify.app` |

---

## Code Style

- **Max line length:** 140 characters
- **Max function length:** 60 lines (`@Composable` exempt)
- **Max complexity:** 15 (cyclomatic and cognitive)
- **Max parameters:** 8 (`@Composable` exempt)
- **Indentation:** 4 spaces, no tabs
- **No semicolons**
- **No wildcard imports** (except `androidx.compose.material3.*`, `androidx.compose.material.icons.*`, `java.util.*`)
- **No `System.out.*` or `.printStackTrace()`** — use Logcat
- **No `java.lang.Math`** — use `kotlin.math`
- **No `java.util.stream.*`** — use Kotlin collections
- **No hardcoded user-visible strings** — all translatable text must go in `strings.xml` (feature-local or `core/ui`); never inline string literals in Composables or Activities
- **No hardcoded colors** — use tokens from `Color.kt` via `MaterialTheme.colorScheme`; never use `Color(0xFF…)` inline
- **No hardcoded dimensions** — spacing, padding, icon sizes, and corner radii must be defined in `Dimens.kt`; never use raw `dp`/`sp` literals outside that file
- **No hardcoded text sizes** — define in `Theme.kt` Typography and reference via `MaterialTheme.typography`
- **Comments explain *why*, not *what*.** Self-documenting code is expected.
- `FIXME`, `HACK`, `STOPSHIP` are forbidden (detekt will fail).

Suppress a rule inline when justified:
```kotlin
@Suppress("MagicNumber")
val timeout = 5000
```

---

## Testing

- **Unit tests:** JUnit 4 + MockK — required for all use cases, mappers, and utilities
- **Instrumented tests:** ALL live in `app/src/androidTest/` — never in feature modules
- **Compose UI tests:** Required for new screens (at least one Roborazzi screenshot test)
- **Architecture tests:** Konsist — runs automatically with `testDebugUnitTest`; do not suppress failures
- **DB tests:** Room in-memory database via `MigrationTestHelper`

After any UI change:
```bash
./gradlew recordRoborazziDebug   # update goldens
```
Commit the updated golden images alongside the code change.

---

## Commit Convention

Format: `<type>(<scope>): <description>`

Scope = module name: `feat(feature:add): ...`, `fix(core:isolation): ...`

Types: `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `ci`, `build`, `chore`, `revert`

Changelog is auto-generated from commits on `v*` tag push via git-cliff.

---

## Known Gotchas

**Database schema tracking disabled** — `AppDatabase.kt` declares `version = 1` with `exportSchema = false`. No schema files are generated, so there is no migration history to diff against. Any schema bump requires explicit `Migration` objects for all version gaps. Do not change the schema until `exportSchema = true` is re-enabled and a clean baseline is committed. See `.planning/codebase/CONCERNS.md`.

**GeckoView Gradle dependency is arm64-only** — `libs.versions.toml` declares only `geckoview-arm64-v8a` for the compile-time API. At runtime, `GeckoEngineManager` detects `Build.SUPPORTED_ABIS` and downloads the correct ABI artifact (arm64-v8a, armeabi-v7a, x86_64, x86). Native `.so` files are excluded from the APK and downloaded on demand. Do not add a bundled non-arm64 geckoview dependency without updating the download and preload logic.

**`AppNavigation.kt` is monolithic** — The single `NavHost` composable is 452 lines with a suppressed complexity warning. Adding new routes here is fine; refactoring it is a separate task.

**`GlobalSettingsScreen.kt` (2,222 lines) and `OnboardingScreen.kt` (1,989 lines)** — Known large files. When editing, target the specific section; do not reorganize the whole file unless that's the explicit task.

**Four independent `OkHttpClient` instances** — `FaviconFetcher`, `SimpleIconsManager`, `GeckoEngineManager`, and `PwaAnalyzer` each create their own client with separate connection pools and timeouts. Do not add a fifth; if adding networking, reuse one of the existing clients until a shared `core:network` module is created. See `.planning/codebase/CONCERNS.md`.

**Third-party cookies are globally enabled** — `WebViewManager` sets `setAcceptThirdPartyCookies(webView, true)` for all apps. This is a known trade-off for OAuth/SSO flows; do not change it without a per-app toggle. See `.planning/codebase/CONCERNS.md`.

**Lint `abortOnError = false`** — Lint issues never fail the build. Do not rely on lint to catch regressions; use detekt and Konsist instead.

---

## Key Files

| File | Purpose |
|---|---|
| `app/src/main/java/io/shellify/app/ShellifyApplication.kt` | DI wiring root |
| `app/src/main/java/io/shellify/app/presentation/navigation/AppNavigation.kt` | NavHost and all routes |
| `core/domain/src/main/java/io/shellify/app/domain/model/WebApp.kt` | Primary domain model |
| `core/database/src/main/java/io/shellify/app/data/local/AppDatabase.kt` | Room DB — check version before any migration |
| `gradle/libs.versions.toml` | All dependency versions — always update here, not in module build files |
| `config/detekt/detekt.yml` | Detekt rules |
| `config/lint/lint.xml` | Lint rules |
| `build-logic/src/main/kotlin/` | Convention plugin sources |
