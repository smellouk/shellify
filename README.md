<div align="center">

# Shellify

<img src="docs/icon.webp" alt="Shellify icon" width="96" height="96" />

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36%20(Android%2015)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/about/versions/15)
[![Main](https://github.com/smellouk/shellify/actions/workflows/main.yml/badge.svg)](https://github.com/smellouk/shellify/actions/workflows/main.yml)

**Turn any website into an isolated, ad-free app on your home screen.**

Shellify is a local-first Android PWA launcher. It wraps websites in individual WebView containers with per-app isolation, ad blocking, translation, biometric locking, and encrypted backup — no account, no cloud, no tracking.

### Signing Certificate Fingerprints

Use these to verify the APK signature.

```
SHA-256: EF:A3:C6:37:40:77:87:EE:97:18:58:7C:DE:FE:3F:86:02:E2:41:05:49:BE:DE:71:8F:4E:4D:74:EB:0C:23:21
SHA-1: 75:F2:73:AF:01:93:EF:08:F3:F2:2F:8C:B2:EA:FE:8B:BC:A0:27:73
```
</div>


---

<table>
<tr>
<td valign="top" width="60%">

## Features

- **PWA launcher** — Add any HTTPS website as a standalone home-screen app with its own icon, name, and theme color
- **Per-app isolation** — Each app gets its own cookie jar, local storage, and (Android 13+) dedicated WebView profile
- **Ad blocking** — Built-in content blocker with per-app custom rules
- **Translation** — In-page translation via Google Translate; configurable per app
- **App lock** — Optional password or biometric lock per app or globally
- **Two browser engines** — Android System WebView (default) or Mozilla GeckoView (optional download)
- **User-agent switching** — Chrome, Firefox, Safari, Edge, or custom user-agent per app
- **Encrypted backup** — AES-256-GCM backup files protected with a user-chosen password (Argon2id key derivation); manual or scheduled
- **Home-screen shortcuts** — Android launcher shortcuts for individual web apps
- **Icon packs** — Simple Icons integration for brand logos; custom icon selection
- **Material You** — Dynamic theming, dark/light/system mode, custom accent colors
- **Deep linking** — Share app configurations via QR code, link, or `shellify://` URI
- **Multilingual** — English, French, and Arabic

</td>
<td valign="top" align="center" width="40%">

## Demo

![Shellify demo](docs/demo.gif)

</td>
</tr>
</table>

---

## Requirements

| Tool | Version |
|---|---|
| Android Studio | Ladybug or newer |
| JDK | 17 |
| Gradle | wrapper included |
| Android SDK | Compile SDK 36, Min SDK 26 |

Minimum device: **Android 8.0 (API 26)**
Target: **Android 15 (API 36)**

---

## Getting Started

```bash
# Clone
git clone https://github.com/smellouk/shellify.git
cd shellify

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build release APK (requires signing config)
./gradlew assembleRelease
```

The project uses **convention plugins** defined in `build-logic/` — no manual SDK path configuration is required beyond a standard Android Studio setup.

---

## Architecture

Shellify follows **Clean Architecture** with strict layer separation enforced by Konsist at compile time.

```mermaid
graph TB
    subgraph Presentation["Presentation — feature:*"]
        screens["Compose Screens · ViewModels"]
    end
    subgraph Infrastructure["Infrastructure — core:*"]
        infra["database · engine · security · backup · crypto\nisolation · pwa · translate · theme · locale\niconpack · shortcut · deeplink · ui"]
    end
    subgraph Domain["Domain — core:domain  ·  Pure Kotlin, no Android deps"]
        domain["Models · Repository Interfaces · Use Cases"]
    end

    Presentation --> Infrastructure
    Presentation --> Domain
    Infrastructure --> Domain
```

- **`core:domain`** — Pure Kotlin (no Android dependency). Defines all models, repository interfaces, and use cases.
- **`core:*`** — Infrastructure modules that implement domain interfaces (database, engine, security, backup, etc.).
- **`feature:*`** — Presentation layer only. Each feature module contains screens and ViewModels; no direct data access.
- **`core:ui`** — Shared design system: composables, typography, spacing tokens, Material 3 theme.

Dependency direction: `feature → core:domain`, `core:* → core:domain`. Feature modules never depend on each other.

---

## Module Map

### Core

| Module | Description |
|---|---|
| `core:domain` | Models, repository interfaces, use cases (pure Kotlin) |
| `core:database` | Room + SQLCipher database, DAOs, entities, migrations |
| `core:crypto` | AES-256-GCM, PBKDF2, Argon2id cryptographic utilities |
| `core:security` | Android Keystore integration, biometrics, password management |
| `core:backup` | Encrypted `.pwab` backup/restore (Argon2id + AES-256-GCM) |
| `core:engine` | WebView abstraction, GeckoView integration, ad-block injection |
| `core:isolation` | Per-app cookie and profile isolation management |
| `core:pwa` | PWA manifest parser and icon/color analyzer |
| `core:translate` | In-page translation via Google Translate (`translate.googleapis.com`) |
| `core:theme` | DataStore-backed theme, language, and global preferences |
| `core:locale` | Runtime language switching |
| `core:iconpack` | Simple Icons download, import, and SVG rendering |
| `core:shortcut` | Android launcher shortcut creation |
| `core:deeplink` | Deep-link parsing and URI building (`shellify://` and `https://shellify.app`) |
| `core:ui` | Shared Compose components, Material 3 design system |

### Feature

| Module | Description |
|---|---|
| `feature:home` | App list with search, categories, and quick-pick suggestions |
| `feature:add` | Add/edit PWA form — URL analysis, manifest detection, icon fetch |
| `feature:category` | Category creation and management |
| `feature:settings` | Per-app settings — fullscreen, ad-block, translation, lock, shortcut |
| `feature:webview` | WebView activity — ad-block toggle, translate, fullscreen, lock UI |
| `feature:onboarding` | Consent screen and 7-step first-run onboarding flow |
| `feature:translate` | Translation settings screen |
| `feature:shortcuts` | Shortcut list management |
| `feature:shortcut` | Launcher shortcut creation entry point |
| `feature:share` | QR code generation and deep-link sharing |

### App

| Module | Description |
|---|---|
| `app` | Application class, MainActivity, NavHost, bottom navigation, DI wiring |

---

## Module Dependency Graph

```mermaid
graph TB
    subgraph APP[":app"]
        app["ShellifyApplication · MainActivity · NavHost\nManual DI wiring — no framework"]
    end

    subgraph FL["feature layer  ·  Compose UI + ViewModels"]
        direction LR
        f_home["home"]
        f_add["add"]
        f_category["category"]
        f_settings["settings"]
        f_webview["webview"]
        f_onboarding["onboarding"]
        f_translate["translate"]
        f_shortcuts["shortcuts"]
        f_share["share"]
        f_shortcut["shortcut"]
    end

    subgraph CL["core layer  ·  Infrastructure & Data"]
        subgraph CDATA["Data & Security"]
            c_crypto["crypto\nAES-256-GCM · Keystore"]
            c_database["database\nRoom + SQLCipher"]
            c_security["security\nPassword · Biometrics"]
            c_backup["backup\nArgon2id · .pwab"]
        end
        subgraph CWEB["Web & Content"]
            c_engine["engine\nWebView + GeckoView · AdBlocker"]
            c_isolation["isolation\nPer-app profiles & cookie jars"]
            c_pwa["pwa\nManifest parser · Icon analyser"]
            c_translate["translate\nJS injection · Google Translate"]
        end
        subgraph CUX["UX & System"]
            c_ui["ui\nDesign system · Shared components"]
            c_theme["theme\nMaterial You · DataStore prefs"]
            c_locale["locale\nRuntime i18n"]
            c_iconpack["iconpack\nSimple Icons SVG"]
            c_shortcut["shortcut\nLauncher shortcuts"]
            c_deeplink["deeplink\nURI parser · QR code"]
        end
    end

    subgraph DL["core:domain  ·  Pure Kotlin — no Android deps"]
        d_domain["WebApp · Category · PwaManifest · IconSource\nWebAppRepository · CategoryRepository\nGetWebApps · SaveWebApp · DeleteWebApp · …"]
    end

    app --> FL & CL & DL

    f_home      --> d_domain & c_ui & c_pwa & c_shortcut
    f_add       --> d_domain & c_ui & c_pwa & c_iconpack & c_engine & c_shortcut
    f_category  --> d_domain & c_ui
    f_settings  --> d_domain & c_ui & c_engine & c_security & c_isolation & c_backup & c_theme
    f_webview   --> d_domain & c_ui & c_engine & c_isolation & c_security & c_translate
    f_onboarding --> d_domain & c_ui & c_backup & c_pwa & c_security & c_theme & c_locale
    f_translate --> d_domain & c_ui
    f_shortcuts --> d_domain & c_ui & c_iconpack & c_pwa & c_shortcut
    f_share     --> d_domain & c_ui & c_security & c_deeplink
    f_shortcut  --> c_shortcut

    c_database  --> d_domain & c_crypto
    c_security  --> d_domain & c_crypto
    c_isolation --> d_domain & c_crypto & c_engine
    c_backup    --> d_domain & c_database & c_crypto & c_security & c_isolation & c_iconpack & c_theme
    c_engine    --> d_domain
    c_pwa       --> d_domain
    c_translate --> d_domain
    c_ui        --> d_domain & c_iconpack
    c_deeplink  --> d_domain & c_security
    c_iconpack  --> d_domain
    c_shortcut  --> d_domain
```

---

## Navigation

Navigation uses **Jetpack Compose Navigation**. All routes are defined in `Screen.kt`; the graph is assembled in `AppNavigation.kt`.

**Start destination logic:**

```
consent not given  →  ConsentScreen
onboarding not done  →  OnboardingScreen
otherwise  →  HomeScreen
```

Bottom navigation (Home, Categories, Shortcuts, Settings) is only visible on top-level routes.

---

## Deep Linking

Two URI schemes are registered for importing app configurations:

| Scheme | Example |
|---|---|
| Custom | `shellify://add?url=<base64url-encoded-https-url>&name=<app-name>` |
| HTTPS | `https://shellify.app/add?url=<base64url-encoded-https-url>&name=<app-name>` |

The URL parameter must be **Base64url-encoded** (no padding) and must decode to an `https://` URL — HTTP is rejected.

A confirmation dialog showing the destination host is displayed before any app is added.

**Test with ADB:**

```bash
# Encode a URL
ENCODED=$(printf '%s' "https://youtube.com" | base64 | tr '+/' '-_' | tr -d '=')

# Fire the deep link
adb shell am start -a android.intent.action.VIEW \
  -d "shellify://add?url=${ENCODED}&name=YouTube" \
  io.shellify.app
```

---

## Backup

Backup files use the `.pwab` extension (an encrypted ZIP archive).

- **Encryption:** Argon2id key derivation → AES-256-GCM cipher
- **Contents:** Database dump, icons, settings, cookies (re-encrypted into archive), WebView profiles (Android 13+)
- **Excluded:** App password hash, database passphrase, backup password — all device-specific secrets are never exported
- **Storage:** User-selected folder via Android Storage Access Framework (SAF)
- **Schedule:** Manual, weekly, or monthly via WorkManager
- **Cross-device:** `.pwab` files are portable — restore on any device using the same password

---

## Testing

```bash
# Unit tests — includes Konsist architecture checks
./gradlew testDebugUnitTest

# Screenshot regression tests
./gradlew verifyRoborazziDebug

# Instrumented tests (requires connected device or emulator)
./gradlew :app:connectedDebugAndroidTest

# Full local check suite
./gradlew detekt lintDebug testDebugUnitTest
```

| Layer | Framework |
|---|---|
| Unit tests | JUnit 4 + MockK |
| Compose UI tests | Compose UI Test (JUnit4 rule) |
| Screenshot tests | Roborazzi (Robolectric runner) — golden images committed alongside UI changes |
| Architecture tests | Konsist — enforces Clean Architecture layer boundaries |
| Database tests | Room in-memory database |

Instrumented smoke tests cover navigation, consent gate, deep-link dialogs, and key screens end-to-end.

---

## Code Quality

| Tool | Purpose |
|---|---|
| **Detekt** | Static analysis + ktlint formatting; config in `config/detekt/detekt.yml` |
| **Android Lint** | Resource, accessibility, and API-level checks; config in `config/lint/lint.xml` |
| **Konsist** | Architecture consistency — fails the build if layer rules are violated |

```bash
# Run Detekt
./gradlew detekt

# Run Lint
./gradlew lintDebug
```

---

## Tech Stack

| Component | Library |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Database | Room + SQLCipher |
| Preferences | DataStore |
| Networking | OkHttp |
| Image loading | Coil (+ SVG) |
| Browser engine | System WebView + GeckoView (optional) |
| QR codes | ZXing Core |
| Biometrics | AndroidX Biometric |
| Background work | WorkManager |
| DI | Manual (ViewModel factories, no framework) |
| Build | AGP + KSP |

---

## Roadmap

The full roadmap is at [`.planning/ROADMAP.md`](.planning/ROADMAP.md). The active milestone targets **v2 — Privacy-First Feature Parity**.

| Phase | Goal | Status |
|---|---|---|
| 1 · Web Integration | Make Shellify the default web handler — incoming links route into the right PWA | Planned |
| 2 · Privacy & Tor | Per-app stealth mode, panic button, and anonymous browsing via the Tor network | Planned |
| 3 · Productivity & Insights | Custom JS/CSS injection, download manager, and on-device usage analytics | Planned |
| 4 · Platform & Discovery | Home-screen widget, PWA directory, push notifications, launcher integration | Planned |
| 5 · E2E Test Migration | Replace Espresso E2E tests with Maestro | Planned |

---

## Localization

Three languages are supported at runtime, switchable from the onboarding screen or settings:

| Code | Language |
|---|---|
| `en` | English (default) |
| `fr` | French |
| `ar` | Arabic |

String resources live in `core/ui/src/main/res/values/strings.xml` (canonical English) and mirrored to `app/src/main/res/values-fr/` and `app/src/main/res/values-ar/`.

---

## Privacy

Shellify is local-first. No account, no cloud sync, no analytics, no crash reporting.

The only outbound network calls are:

- Favicon fetch from the user's chosen domain
- PWA manifest detection from the user's chosen domain
- Optional Simple Icons library download (`cdn.jsdelivr.net`)
- Optional GeckoView engine download (`maven.mozilla.org`), user-initiated
- Translation requests (`translate.googleapis.com`), only when translation is enabled for an app

Full details: [`docs/legal/privacy.md`](docs/legal/privacy.md)

---

## Legal

- **Terms of Service:** [`docs/legal/terms.md`](docs/legal/terms.md)
- **Privacy Policy:** [`docs/legal/privacy.md`](docs/legal/privacy.md)
- **Trademark:** The "Shellify" name, logo, and branding are proprietary trademarks and are **not** covered by the open-source license. See [`NOTICE`](NOTICE).

---

## Contact

[contact@shellify.app](mailto:contact@shellify.app)
