---
phase: 18-per-app-custom-proxy
plan: "01"
subsystem: core:domain, core:database
tags: [proxy, domain-model, database-migration, schema-v7]
dependency_graph:
  requires: []
  provides: [ProxyType enum, WebApp proxy fields, DB schema v7, MIGRATION_6_7, WebAppMapper proxy round-trip]
  affects: [core:domain, core:database]
tech_stack:
  added: []
  patterns: [TDD RED/GREEN/REFACTOR, Room schema export, runCatching enum fallback]
key_files:
  created:
    - core/domain/src/main/java/io/shellify/app/domain/model/ProxyType.kt
    - core/database/schemas/io.shellify.app.data.local.AppDatabase/7.json
    - core/database/src/test/java/io/shellify/app/data/local/migration/Migration6To7Test.kt
    - core/domain/src/test/java/io/shellify/app/domain/model/ProxyTypeTest.kt
  modified:
    - core/domain/src/main/java/io/shellify/app/domain/model/WebApp.kt
    - core/database/src/main/java/io/shellify/app/data/local/entity/WebAppEntity.kt
    - core/database/src/main/java/io/shellify/app/data/local/migration/Migrations.kt
    - core/database/src/main/java/io/shellify/app/data/local/AppDatabase.kt
    - core/database/src/main/java/io/shellify/app/data/mapper/WebAppMapper.kt
    - core/database/src/test/java/io/shellify/app/data/mapper/WebAppMapperTest.kt
decisions:
  - ProxyType stored as String in DB entity (matching LockType/EngineType pattern) then parsed to enum via runCatching fallback in mapper
  - Five separate ALTER TABLE statements in MIGRATION_6_7 (one per column) matching MIGRATION_5_6 style
  - customProxyType defaults to "NONE" string in entity and ProxyType.NONE in domain model for safe defaults across upgrade
metrics:
  duration: ~30min
  completed: "2026-05-26"
  tasks_completed: 2
  files_changed: 10
---

# Phase 18 Plan 01: Per-App Custom Proxy Domain and DB Foundation Summary

Pure-Kotlin ProxyType enum plus five proxy fields on WebApp/WebAppEntity, DB schema bumped 6 to 7 via MIGRATION_6_7, WebAppMapper round-trips all five fields with safe NONE fallback on unknown enum strings.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Add ProxyType enum and WebApp proxy fields (domain layer) | 3e0a855 | ProxyType.kt, WebApp.kt, ProxyTypeTest.kt |
| 2 | Add WebAppEntity proxy columns, MIGRATION_6_7, bump DB to v7, extend WebAppMapper, generate 7.json | 3af2296 | 7 files modified/created |

## What Was Built

### Task 1: Domain layer

Created `ProxyType.kt` in `io.shellify.app.domain.model` with exactly three entries: `NONE`, `SOCKS5`, `HTTP`. No Android imports — satisfies Konsist `core:domain` Android-free constraint.

Extended `WebApp.kt` with five new properties immediately after the Tor block with default-safe values:

```kotlin
// Custom proxy
val customProxyType: ProxyType = ProxyType.NONE,
val customProxyHost: String? = null,
val customProxyPort: Int = 0,
val customProxyUsername: String? = null,
val customProxyPassword: String? = null,
```

All existing call sites continue to compile because every field has a default. Created `ProxyTypeTest.kt` with 10 tests covering enum value count, valueOf round-trip for all three entries, and all five default proxy field values.

### Task 2: Database and mapper layer

Added five `@ColumnInfo`-annotated columns to `WebAppEntity` immediately after the Tor block, mirroring the MIGRATION_5_6/Tor column pattern. `customProxyType` stores as `String = "NONE"` matching the existing `lockType`/`engineType` string encoding pattern.

Added `MIGRATION_6_7` with exactly five `execSQL("ALTER TABLE web_apps ADD COLUMN ...")` statements using the SQL from 18-PATTERNS.md verbatim. Bumped `AppDatabase` to `version = 7` and registered `MIGRATION_6_7` in `addMigrations()`.

Extended `WebAppMapper.toDomain()` with:
```kotlin
customProxyType = runCatching { ProxyType.valueOf(customProxyType) }.getOrDefault(ProxyType.NONE),
```
Uses the same safe enum parsing as `EngineType`/`LockType` (T-18-05 mitigation).

Extended `WebAppMapper.toEntity()` with:
```kotlin
customProxyType = customProxyType.name,
```

Auto-generated `7.json` via KSP (`exportSchema = true`) and committed alongside code changes.

## Schema Version Delta

**6 → 7**: Added columns `custom_proxy_type TEXT NOT NULL DEFAULT 'NONE'`, `custom_proxy_host TEXT`, `custom_proxy_port INTEGER NOT NULL DEFAULT 0`, `custom_proxy_username TEXT`, `custom_proxy_password TEXT` to `web_apps` table.

## Test Coverage

| Test File | Tests | Status |
|-----------|-------|--------|
| ProxyTypeTest.kt | 10 | Green |
| Migration6To7Test.kt | 8 | Green |
| WebAppMapperTest.kt (proxy extensions) | 9 new tests | Green |
| Full `:core:domain:test` suite | 131 | Green |
| Full `:core:database:testDebugUnitTest` suite | 92 | Green |
| Konsist architecture tests (`:app:testDebugUnitTest`) | 132 | Green |
| detekt | - | Green |
| lint | - | Green |

## Threat Model Coverage

| Threat | Status |
|--------|--------|
| T-18-04 DoS on upgrade (MIGRATION_6_7 atomicity) | Mitigated — migration registered atomically with version bump; Migration6To7Test asserts startVersion/endVersion + 5 statements |
| T-18-05 Tampering via unknown proxy type string | Mitigated — `runCatching { ProxyType.valueOf(...) }.getOrDefault(ProxyType.NONE)` with dedicated test |
| T-18-06 Credential disclosure at rest | Accepted — SQLCipher AES-256 is the protection layer per D-04 |

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — this plan delivers storage/domain foundation only. No UI rendering or stubs exist.

## Self-Check: PASSED

- ProxyType.kt exists: FOUND
- WebApp.kt has customProxyType: FOUND
- AppDatabase version = 7: FOUND (line 28)
- MIGRATION_6_7 registered: FOUND (2 matches in AppDatabase.kt)
- 7.json exists with all 5 proxy columns: FOUND
- Commits 3e0a855 and 3af2296 exist in git log
