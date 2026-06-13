# Changelog

All notable changes to Shellify are documented here.
Entries are auto-generated from conventional commits using [git-cliff](https://git-cliff.org).

## [1.4.3] - 2026-06-13

### Bug Fixes

- Support OAuth/Sign-in-with-Google popups on both engines (#17)

### Documentation

- Update planning state after Phase 17 (Reading Mode) completion
- **tools**: Add Popups tab to developer tools
- **tools**: Persist active tab and fix popup data-URL navigation

### Features

- **feature:webview**: Reading Mode via Readability.js (#12)

## [1.4.2] - 2026-05-26

### Bug Fixes

- **app**: Add ProGuard keep rules for tor-android and jtorctl

## [1.4.1] - 2026-05-26

### Bug Fixes

- **core:engine**: Surface TorState.Error on silent startup failures and add 90s timeout

## [1.4.0] - 2026-05-26

### Documentation

- Plan phase 2 — privacy and tor

### Features

- **feature:webview**: Add swipe-to-refresh with per-app toggle and HTTPS enforcement
- **core**: Add network request logging infrastructure
- **feature:onboarding**: Add versioned What's New screen for phase 2 features (#11)

### Maintenance

- Migrate to AGP 9.2.0, Gradle 9.4.1, Kotlin 2.3.21, Compose BOM 2026.05.00
- Upgrade JDK 17 → 21 in setup-android action
- Restore verifyRoborazziRelease and fix srcDirs deprecation

## [1.3.0] - 2026-05-24

### Bug Fixes

- **core:engine**: Upgrade GeckoView to 140.x and guard against version mismatch
- **core:database**: Enable schema export and add migration test
- **app**: Keep GeckoView classes from R8 to prevent native SIGSEGV in release
- **feature:webview**: Back press navigates browser history instead of exiting app
- **feature:webview**: Improve recents chip label and task description
- **core:isolation**: Make clearData suspend and await cookie removal before reload
- **core:isolation**: Replace suspendCancellableCoroutine with fire-and-forget removeAllCookies
- **feature:settings**: Add background container to global notifications icon
- **feature:webview**: Guard viewModel/engine isInitialized in onResume and onDestroy
- **feature:webview**: Enforce minimum splash display duration
- **feature:settings**: Use OS notification state as source of truth in per-app settings
- Address post-review findings in notification permission flow
- **feature:webview**: Restore background GeckoSession in BackgroundNotificationService
- **app**: Remove stale themeManager arg from AppSettingsViewModel e2e builders

### Documentation

- Revert LICENSE copyright to Apache template placeholder
- Correct and expand technical debt in CONCERNS.md and CLAUDE.md
- **privacy**: Update logo image reference from icon.png to icon.webp
- **legal**: Soften branding language per legal advice
- **site**: Add assetlinks.json for Android App Links verification
- **app**: Update CLAUDE.md — reflect actual Konsist boundaries and WebView MVVM pattern
- **site**: Fix changelog date format and add Changelog link to legal page sidebars
- **planning**: Expand roadmap to 23 phases
- **legal**: Disclose JavaScript bridges; bump consent to v3
- Add missing README files for .github and docs subdirectories

### Features

- **feature:onboarding**: Add versioned consent with update re-consent screen
- **feature:webview**: Add per-PWA splash screen and suppress double splash
- **feature:settings**: Add per-app control center toggle with tests
- **feature:webview**: Add typed error screen and screenshot test coverage
- **feature:webview**: Apply PWA theme color to all Compose overlays in WebViewActivity
- **feature:home**: Add delete app to context menu with confirmation dialog
- **feature:webview**: Add clear data button to control center
- **feature:add**: Validate URL on save, analyze, and icon fetch
- **feature:webview**: Hide control center FAB until page finishes loading
- Link dispatcher — route shared URLs and App Links into installed PWAs
- In-app notifications — per-app toggle, DND, global control, history
- Expose PWA notification permission via ShellifyBridge
- **docs**: Make tools.html PWA-compatible
- **docs**: Add PWA manifest and meta to all site pages
- **feature:settings**: Replace notifications toggle with OS-redirect UX
- Complete PWA notification permission request flow for System WebView and GeckoView

### Maintenance

- **site**: Regenerate legal pages for v1.0.0
- **deps**: Bump actions/checkout from 4 to 6 (#5)
- **deps**: Bump actions/download-artifact from 4 to 8 (#3)
- **deps**: Bump actions/upload-artifact from 4 to 7 (#4)
- **deps**: Bump softprops/action-gh-release from 2 to 3 (#2)
- **deps**: Bump amannn/action-semantic-pull-request from 5 to 6 (#1)
- **main**: Skip workflow for github-actions bot pushes
- **site**: Regenerate legal pages for v1.1.0
- **release**: Merge changelog and site update into single publish job
- **main**: Skip workflow for bot commits pushed via PAT
- Restore canonical Apache 2.0 license text
- **planning**: Fix worktree config key for GSD executor
- **planning**: Update roadmap wave structure and state for phase 01

### Refactoring

- **feature:webview**: Extract WebViewPasswordDialog and WebViewControlCenter composables
- **feature:webview**: Extract WebViewViewModel — full MVVM split

### Testing

- **app**: Update AppSettingsScreen goldens after Features section renamed to Control center
- **app**: Replace all hardcoded strings in Android tests with R.string resources
- **feature:webview**: Add kotlinx.coroutines.test dependency for WebViewViewModelTest
- **notification**: Complete UAT — 6/6 passed, phase 6 marked complete

## [1.0.0] - 2026-05-17

### Bug Fixes

- Fix build errors and deprecation warnings
- Four UX fixes: analyze dialog, status bar color, shortcut icon, color/icon picker
- Fix shortcut icon: use createWithBitmap, drop adaptive framing and host-app badge
- **feature:add**: Show pending icon in preview immediately after analysis, before Apply All
- **core:pwa**: Fix icon selection: exclude monochrome, prefer maskable over any/unknown
- Fix Clean Architecture violations: move EngineType to domain, decouple shortcut from presentation
- **feature:home**: Remove tip card from Shortcuts empty state
- **core:theme**: Adapt screen background to accent color and replace violet swatch with green
- Unify accent palette and fix onboarding background
- **feature:onboarding**: Fix onboarding language re-select and complete token coverage
- **feature:onboarding**: Mirror directional icons in RTL for onboarding navigation
- **feature:onboarding**: Fix language card icon alignment in RTL
- **feature:home**: Fix suggestion card height and token cleanup in HomeScreen
- **feature:add**: Remove gap between app name and URL in suggestion cards
- **feature:add**: Fix suggestion card height consistency across locales
- **feature:add**: Remove font padding gap between app name and URL in suggestion cards
- **feature:add**: Polish Add screen spacing and divider consistency
- Fix accessibility: replace fixed heights with heightIn on text containers
- **feature:home**: Restore chip pill background on feature icons
- **feature:home**: Fix AppCard layout: feature icons row starts at card edge
- **feature:home**: Fix spacing in AppCard and restore status bar color in WebView
- Use scrim view for status bar color on all API levels
- Fix status bar scrim: always use edge-to-edge so insets reach container
- **feature:webview**: Fix WebView status bar color and FAB nav bar overlap
- **feature:home**: Compact search bar: match button height and fix text clipping
- **feature:home**: Fix SVG shortcut icon clipping and add icon preview tap-through
- **core:ui**: Polish UI: card borders, icon picker card, status bar, home title branding
- **feature:onboarding**: Wrap onboarding password fields in a card
- **core:backup**: Fix backup to include icon pack data and user preferences
- **core:backup**: Fix backup restore completeness, backup UI gating, and shortcut icon padding
- **feature:webview**: Overhaul WebView bottom sheet UI and fix silent auto-translation
- **core:translate**: Fix translate toggle off not stopping translation on page reload
- **core:translate**: Remove API key, default to translate.fedilab.app
- Fix hardcoded colors, dead param, and missing translations
- **feature:webview**: Drop base64 padding to avoid %3D%3D in shared links
- Fix 10 security vulnerabilities found in audit
- Fix 6 failing Android instrumented tests
- **feature:onboarding**: Fix unenforceable suspension clause in consent screen
- **feature:onboarding**: Strengthen acceptable-use reserve clause with explicit grounds
- **feature:onboarding**: Strengthen privacy disclaimer to cover store, transmit
- **feature:onboarding**: Disclose local OS sharing feature in consent screen
- **feature:onboarding**: Strengthen consent_not_2 with intentionally + transmit
- **core:database**: Reset database to version 1, remove all migrations
- **app**: Correct datastore paths and add missing strings
- **app**: Specify PKCS12 store type to resolve release signing on CI
- **feature:settings**: Update privacy, terms, and changelog links to .html URLs

### Documentation

- Add planning docs for upcoming features
- Add Apache 2.0 license, README, legal docs, and app feature updates
- Add new features documents
- Add per-module README documentation for all 32 folders
- Initialize GSD project with roadmap (4 phases, 43 requirements)
- **CLAUDE.md**: Forbid hardcoded strings, colors, dimensions, and text sizes
- **readme**: Fix outdated and inaccurate content
- Add project README and update core/ui documentation
- Add changelog page, update nav, replace demo recording, and fix create app title
- **site**: Update landing page and add app icon
- Reorganize README header in Neo-Store style

### Features

- **core:crypto**: Add at-rest encryption: Android Keystore AES-256-GCM + SQLCipher
- **feature:add**: Redesign Create App screen with expandable feature cards
- **feature:home**: Add empty state illustration to HomeScreen
- **core:theme**: Add dynamic theme switching: ThemeMode + ThemeManager + Appearance sheet
- **core:pwa**: Extract icon from apple-touch-icon and og:image when no PWA manifest icons
- **feature:add**: Add Fetch Icon button to Basic Info card
- **feature:settings**: Add Global Settings screen and bottom navigation tabs
- **feature:home**: Add Shortcuts tab for managing launcher shortcuts
- **feature:home**: Add Change icon option for shortcuts via image picker
- Add per-app locking, categories with icon/color, and backup/restore
- Add pluggable browser engine, WebView improvements, and global settings enhancements
- **core:theme**: Add design token system and user-selectable theme color
- Redesign all screens to match design handoff specs
- **feature:home**: Redesign AppCard to compact horizontal layout
- **feature:home**: Quick-add from suggestions + empty state polish
- **core:iconpack**: Add Simple Icons pack: import, picker, and icon selection
- **core:domain**: Introduce IconSource sealed class for typed icon storage
- **feature:home**: Shortcuts screen: add/remove flow, grid/list toggle, per-icon colors
- **feature:add**: Pre-fill Add Category dialog from suggestion chips
- **feature:home**: Add Change Icon flow to Shortcuts screen with unified SVG rendering
- **core:backup**: Overhaul backup/restore + polish UI dialogs and shortcut tracking
- **feature:settings**: Improve app settings: full Basic Info editing, favicon fallback, and context menu clear data
- **core:translate**: Switch translation engine from Google to LibreTranslate
- **core:translate**: Add LibreTranslate API key support and debug logging
- **feature:settings**: Add GeckoView engine selector in App Settings with missing-engine warning
- **core:engine**: Add GeckoView SHA-256 integrity verification with UI feedback
- **feature:webview**: Add deeplink and QR code sharing for quick app install
- **feature:home**: Extract AppShareSheet as shared composable, add Share to home screen context menu
- **feature:webview**: Encode URL as base64url in deeplinks, keep name as plain text
- **feature:settings**: Add Open Source Licenses screen to settings
- **feature:settings**: Add Changelog screen to settings
- **feature:settings**: Replace in-app changelog with shellify.app/changelog link
- **feature:settings**: Add Privacy Policy link to Settings → About
- **feature:onboarding**: Add first-launch consent screen with legal disclaimer
- **feature:onboarding**: Update consent screen with lawyer-reviewed legal text
- **feature:onboarding**: Add no-facilitation clause to consent screen disclaimer
- **feature:onboarding**: Add legal and abuse contact links to consent screen
- **feature:onboarding**: Add impersonation and phishing clauses to acceptable use
- **feature:settings**: Implement lawyer-recommended backup session data disclosures
- **feature:webview**: Implement lawyer-recommended deep-link sharing disclosures and confirmation
- **feature:webview**: Extract DeepLinkConfirmDialog and add 12 smoke tests
- **site**: Add GitHub Pages site with landing, privacy, and terms pages
- **app**: Refresh launcher icon and splash screen assets
- **app**: Introduce theme variants and dimension tokens
- **app**: Add Arabic and French string translations
- **core**: Update backup, database, engine, and theme modules
- **app**: Update app module, manifest, and navigation
- **feature**: Update add, home, and settings screens
- **shortcuts**: Gate icon-pack button on pack availability and use app theme color for icon background
- **site**: Add assetlinks.json for Android App Links and disable Jekyll

### Maintenance

- Initial scaffold PWAForge Android app
- Add .gitignore, remove build artifacts from tracking
- Misc in-progress changes (WebView, isolation, add/settings screens)
- **core:database**: Add Room schema exports for DB migrations 10–12
- Add new app icon
- Enable Gradle configuration cache
- Enable Gradle local build cache
- Add CI workflows, issue templates, PR template, and contributing guide
- Add codebase map to .planning/codebase/
- Configure GSD git settings and worktree isolation
- Enable squash merge by default for phase branches
- Add Dependabot, PR title check, and SECURITY.md
- Release build setup and README polish
- **deps**: Bump gradle/actions from 4 to 6 (#4)
- **deps**: Bump actions/checkout from 4 to 6 (#2)
- **deps**: Bump actions/setup-java from 4 to 5 (#5)
- **deps**: Bump actions/download-artifact from 4 to 8 (#3)
- **deps**: Bump orhun/git-cliff-action from 3 to 4 (#1)
- **release**: Pass GITHUB_TOKEN to git-cliff for private repo access
- **pull_request**: Fix invalid disallowScopes yaml syntax
- **release**: Remove GITHUB_REPO from git-cliff to avoid PR metadata fetch
- **release**: Defer CHANGELOG and site commits until after GitHub release
- **release**: Fix circular dependency by removing needs from build job
- **release**: Run changelog and update-site in parallel after build
- **release**: Add keystore fingerprint guard and consolidate artifacts
- Add test logger, SARIF reporting, and reusable composite actions
- Update pull-request workflow, build-logic plugins, lint rules, and dependencies
- **build-logic**: Migrate to compilerOptions DSL and enable allWarningsAsErrors
- Fix dependabot PR failures in workflow and build
- **deps**: Bump github/codeql-action from 3 to 4 (#2)
- **release**: Run assembleRelease and bundleRelease as separate steps
- **release**: Fix artifact glob to match nested paths
- **main**: Set 1-day retention for release artifacts

### Refactoring

- Add comprehensive string resources for all UI text
- Move all hardcoded strings to resources and add circular reveal theme animation
- Move hardcoded values to design tokens, localize all UI strings, add translations
- **feature:add**: Tighten suggestion card padding using Dimens tokens
- Replace all hardcoded .dp values with Dimens tokens across screens
- Replace hardcoded .sp values with Dimens tokens in CategoryScreen and GlobalSettingsScreen
- **feature:home**: Replace textSizeEmptyTitle with typography.titleLarge in HomeScreen
- Replace hardcoded dp/sp values and fontSize tokens with Dimens and M3 typography
- **feature:home**: Move feature icons to dedicated row with right-aligned menu
- Convert 5 AlertDialogs to ModalBottomSheet
- Rename app from PWAForge to Shellify with new package io.shellify.app
- Enforce Clean Architecture, migrate all VMs to use cases, extract shared UI components
- Extract 10 feature modules, add 48 E2E smoke tests, reorganize androidTest
- **feature:onboarding**: Merge redundant acceptable-use clauses 7/8/9 into one

### Reverted Changes

- **core:translate**: Revert to Google Translate, remove LibreTranslate and debug logging

### Testing

- Add 95 Android instrumented integration tests
- **feature:onboarding**: Add consent screen smoke tests and route sanity checks
- **feature:onboarding**: Add consent-gate tests to prevent regression on first-launch routing
- Add Roborazzi screenshot testing with 20 golden images across 6 screens
- **app**: Add E2E instrumented tests for core security and engine features
- **app**: Add screenshot and unit tests with Roborazzi golden images
- **app**: Update AddScreen screenshot goldens for Create App title change
- **app**: Update AddScreenTest to assert Create App title is always shown


