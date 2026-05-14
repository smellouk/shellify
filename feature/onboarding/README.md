# `feature:onboarding`

> First impressions count — guide new users through consent, preferences, and their first PWA in seven steps.

## Overview

`feature:onboarding` is the mandatory entry point for every new installation of Shellify. It consists of two distinct screens: a non-skippable privacy consent gate (`ConsentScreen`) and an optional seven-step wizard (`OnboardingScreen`). The completion flag is stored in DataStore; once set, the flow is never shown again.

## Purpose

- Obtain informed consent (GDPR/privacy) before any user data is touched.
- Let the user configure language, backup, and password on first launch rather than requiring a settings detour.
- Walk through key features (add app, category, translation, permissions) so new users understand Shellify's capabilities immediately.
- Persist the completion state so the `:app` module can route cold launches directly to `feature:home`.

## Key Classes / Files

### `ConsentScreen`

```kotlin
@Composable
fun ConsentScreen(
    onConsentAccepted: () -> Unit,
    onConsentDeclined: () -> Unit,
)
```

- Displays the full privacy policy text (scrollable).
- Two buttons: **Accept** (proceeds to `OnboardingScreen`) and **Decline** (closes the app).
- Acceptance is persisted by `OnboardingViewModel.markConsentAccepted()` before invoking `onConsentAccepted`.
- Declining calls `finish()` on the host Activity — Shellify will not launch without consent.

### `OnboardingViewModel`

```kotlin
class OnboardingViewModel(
    private val themeManager: ThemeManager,
    private val backupManager: BackupManager,
    private val securityManager: SecurityManager,
    private val onboardingRepository: OnboardingRepository,
) : ViewModel()
```

| Responsibility | Detail |
|---|---|
| Step tracking | `currentStep: StateFlow<Int>` (0–6); `nextStep()` / `previousStep()` |
| Consent persistence | `markConsentAccepted()` → DataStore |
| Completion persistence | `markOnboardingComplete()` → DataStore; called after step 7 or final skip |
| Language selection | `selectLanguage(locale)` → `ThemeManager.selectedLanguage` + `AppCompatDelegate` |
| Backup setup | delegates to `BackupManager.configure(uri, schedule)` |
| Password setup | delegates to `SecurityManager.setGlobalPassword(password)` |
| Skip individual steps | each step exposes a `skip()` callback; wizard always advances forward |

### `OnboardingScreen`

Seven-step pager (Compose `HorizontalPager`):

| Step | Content |
|---|---|
| 1 | Language picker: `en` / `fr` / `ar` (RTL-aware) |
| 2 | Backup setup: choose folder via SAF picker + schedule toggle |
| 3 | Password setup: optional PIN / password with confirm field |
| 4 | Add first app: CTA card that routes to `feature:add`; returns here on back |
| 5 | Category creation: CTA card that routes to `feature:category` |
| 6 | Translation demo: brief explainer + toggle to enable for the just-added app |
| 7 | Permissions walkthrough: request notifications + storage (via `rememberPermissionState`) |

Each step shows a **Skip** link (except step 1, which can still be changed later in global settings) and a **Next / Finish** button.

## Dependencies

```kotlin
// feature/onboarding/build.gradle.kts
dependencies {
    implementation(project(":core:backup"))
    implementation(project(":core:domain"))
    implementation(project(":core:locale"))
    implementation(project(":core:pwa"))
    implementation(project(":core:security"))
    implementation(project(":core:theme"))
    implementation(project(":core:ui"))
}
```

Navigation targets (runtime):

- `feature:add` — step 4 CTA
- `feature:category` — step 5 CTA

## Usage / How to navigate here

`ShellifyApplication` checks the consent + onboarding completion flags synchronously on the main thread startup (via `runBlocking` on the DataStore first-emission):

```kotlin
// app/src/main/java/.../MainActivity.kt
val startDestination = if (!prefs.consentAccepted) "consent"
                       else if (!prefs.onboardingComplete) "onboarding"
                       else "home"
```

Once `markOnboardingComplete()` is called, the start destination permanently becomes `"home"`.

## Mermaid Diagram

```mermaid
flowchart TD
    A([App Launch]) --> B{Consent accepted?}
    B -- No --> C[ConsentScreen]
    C -- Decline --> X([App exits])
    C -- Accept --> D{Onboarding complete?}
    B -- Yes --> D
    D -- Yes --> Z([HomeScreen])
    D -- No --> S1[Step 1: Language]
    S1 -->|Next / Skip| S2[Step 2: Backup]
    S2 -->|Next / Skip| S3[Step 3: Password]
    S3 -->|Next / Skip| S4[Step 4: Add first app]
    S4 -->|CTA| ADD[feature:add]
    ADD -->|back| S4
    S4 -->|Next / Skip| S5[Step 5: Categories]
    S5 -->|CTA| CAT[feature:category]
    CAT -->|back| S5
    S5 -->|Next / Skip| S6[Step 6: Translation demo]
    S6 -->|Next / Skip| S7[Step 7: Permissions]
    S7 -->|Finish| MARK[markOnboardingComplete]
    MARK --> Z
```

## Configuration

- **Consent re-display**: to force the consent screen again (e.g., after a privacy policy update), increment `CONSENT_VERSION` in `OnboardingRepository`. The screen reappears if the stored version is lower than the current one.
- **Step order**: the step list is defined as a `List<OnboardingStep>` sealed class in `OnboardingViewModel`. Reordering or removing steps requires only changing that list — the `HorizontalPager` derives its page count dynamically.
- **RTL support**: step 1 language selection immediately triggers `AppCompatDelegate.setApplicationLocales()`, so the wizard itself re-renders in RTL for Arabic without requiring a restart.
- **Permission requests**: step 7 uses `accompanist-permissions` (or Compose `rememberPermissionState`). Declined permissions are not re-requested within the wizard; the user can grant them later via system settings.
