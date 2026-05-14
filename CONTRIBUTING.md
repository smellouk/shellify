# Contributing to Shellify

Thank you for your interest in contributing! This document explains how to get set up, the standards we follow, and how to submit changes.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Commit Message Convention](#commit-message-convention)
- [Branch Naming](#branch-naming)
- [Pull Request Process](#pull-request-process)
- [Code Style](#code-style)
- [Testing](#testing)
- [Module Structure](#module-structure)
- [Reporting Issues](#reporting-issues)

---

## Code of Conduct

Be respectful and constructive. Harassment, hate speech, or personal attacks will not be tolerated.

---

## Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Ladybug (2024.2) or newer |
| JDK | 17 |
| Android SDK | Compile SDK 36, Min SDK 23 |

### Setup

```bash
git clone https://github.com/smellouk/shellify.git
cd shellify
./gradlew assembleDebug
```

No manual SDK path configuration is needed — convention plugins in `build-logic/` handle all defaults.

---

## Commit Message Convention

This project uses **Conventional Commits**. The changelog is auto-generated from commit messages, so please follow the format:

```
<type>(<scope>): <short description>

[optional body]
[optional footer]
```

### Types

| Type | When to use |
|---|---|
| `feat` | A new feature visible to the user |
| `fix` | A bug fix |
| `perf` | A performance improvement |
| `refactor` | Code change that isn't a fix or feature |
| `test` | Adding or updating tests |
| `docs` | Documentation only |
| `ci` | CI/CD workflow changes |
| `build` | Build system or dependency changes |
| `chore` | Maintenance tasks that don't fit above |
| `revert` | Reverting a previous commit |

### Scope (optional)

Use the module name: `feat(core:engine): ...`, `fix(feature:webview): ...`

### Examples

```
feat(feature:add): support importing apps via QR code scan
fix(core:isolation): restore cookies correctly on Android 12
refactor(core:database): simplify WebAppMapper conversion
test(core:crypto): add edge case tests for empty passphrase
```

---

## Branch Naming

```
feat/<short-description>
fix/<short-description>
refactor/<short-description>
chore/<short-description>
```

---

## Pull Request Process

1. Fork the repository and create a branch from `main`
2. Make your changes, following the code style rules below
3. Add or update tests for your change
4. Run the full check suite locally (see [Testing](#testing))
5. Open a PR against `main` — the PR template will guide you
6. CI must pass before merging
7. At least one approval is required

For large changes, open a discussion issue first to align on approach before writing code.

---

## Code Style

### Kotlin / Detekt

Static analysis runs automatically on every PR. To run locally:

```bash
./gradlew detekt
```

Config: `config/detekt/detekt.yml`

Key rules:
- Max line length: 140 characters
- Max function complexity: 15
- Max method length: 60 lines
- Naming conventions enforced for classes, functions, variables

To suppress a rule inline:
```kotlin
@Suppress("MagicNumber")
val timeout = 5000
```

### Android Lint

```bash
./gradlew lintDebug
```

Config: `config/lint/lint.xml`

### No comments for "what"

Write self-documenting code. Comments should only explain *why* something non-obvious is done, never *what* the code does.

---

## Testing

### Run all checks

```bash
# Unit tests (includes Konsist architecture checks)
./gradlew testDebugUnitTest

# Screenshot verification (compare against golden images)
./gradlew verifyRoborazziDebug

# Static analysis
./gradlew detekt lintDebug
```

### Update screenshot goldens

If you change UI, regenerate the golden images:

```bash
./gradlew recordRoborazziDebug
```

Commit the updated images alongside your code change.

### Test coverage expectations

- New use cases, mappers, and utilities require unit tests
- New Compose screens require at least one Roborazzi screenshot test
- Architecture layer rules are enforced by Konsist — they run automatically with unit tests

---

## Module Structure

```
app/          → Application class, MainActivity, navigation, DI wiring
build-logic/  → Gradle convention plugins (no boilerplate in modules)
core/         → Infrastructure modules (database, crypto, engine, …)
feature/      → Presentation modules (Compose screens + ViewModels only)
config/       → Detekt and Lint configuration
```

**Rules enforced by Konsist:**
- `feature:*` modules must not import `core:database` directly
- `feature:*` modules must not import other `feature:*` modules
- `core:domain` must not have Android dependencies

When adding a new module, use the appropriate convention plugin:

```kotlin
// Android library with Compose
plugins { id("shellify.android.library"); id("shellify.compose") }

// Pure Kotlin (no Android)
plugins { id("shellify.jvm.library") }
```

See `build-logic/README.md` for details.

---

## Reporting Issues

Use the GitHub issue templates:

- **Bug** — unexpected behavior with steps to reproduce
- **Feature request** — problem + proposed solution
- **Task** — internal engineering work
- **Question** — anything else

For security vulnerabilities, email [contact@shellify.app](mailto:contact@shellify.app) directly — do not open a public issue.
