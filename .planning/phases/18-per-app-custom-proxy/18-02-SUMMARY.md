---
phase: 18-per-app-custom-proxy
plan: "02"
subsystem: core:engine
tags: [proxy, engine, socks5, http-proxy, bleed-through-prevention, credentials, tdd]
dependency_graph:
  requires: [18-01]
  provides: [ProxyConfig credentials, applyProxySystemProperties SOCKS5/HTTP/None, proxyConfigFor custom proxy routing]
  affects: [core:engine]
tech_stack:
  added: []
  patterns: [TDD RED/GREEN/REFACTOR, JVM system properties, sealed-class when dispatch, smart-cast workaround via local val]
key_files:
  created: []
  modified:
    - core/engine/src/main/java/io/shellify/app/core/engine/ProxyConfig.kt
    - core/engine/src/main/java/io/shellify/app/core/engine/GeckoEngineManager.kt
    - core/engine/src/main/java/io/shellify/app/core/engine/GeckoViewEngine.kt
    - core/engine/src/test/java/io/shellify/app/core/engine/ProxyConfigTest.kt
    - core/engine/src/test/java/io/shellify/app/core/engine/GeckoEngineManagerProxyTest.kt
    - core/engine/src/test/java/io/shellify/app/core/engine/GeckoViewEngineProxyConfigTest.kt
decisions:
  - proxyConfigFor uses a local val for customProxyHost to work around Kotlin smart-cast restriction on cross-module public API properties
  - applyProxySystemProperties now clears all eight JVM proxy properties on every branch to prevent bleed-through on SOCKS5 to HTTP switches
  - Custom proxy takes priority over Tor in proxyConfigFor priority chain (custom > Tor > None) as last-resort defence even though mutual exclusion is enforced upstream
metrics:
  duration: ~5min
  completed: "2026-05-27"
  tasks_completed: 3
  files_changed: 6
---

# Phase 18 Plan 02: Engine Layer Proxy Routing Summary

ProxyConfig sealed class gains optional username/password credentials on Socks5 and Http variants; GeckoEngineManager.applyProxySystemProperties becomes a full SOCKS5/HTTP/None state machine with cross-type bleed-through prevention; proxyConfigFor expanded to four-branch when with custom proxy priority ahead of Tor and defensive port>0 + non-blank host guards.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Extend ProxyConfig with optional credentials and test equality semantics | d254821 | ProxyConfig.kt, ProxyConfigTest.kt |
| 2 | Rewrite applyProxySystemProperties to handle SOCKS5, HTTP, and None with bleed-through prevention | ba76d50 | GeckoEngineManager.kt, GeckoEngineManagerProxyTest.kt |
| 3 | Expand proxyConfigFor to read custom proxy from WebApp with port>0 + non-blank host guard | 143fbcd | GeckoViewEngine.kt, GeckoViewEngineProxyConfigTest.kt |

## What Was Built

### Task 1: ProxyConfig credential extension

Added `username: String? = null` and `password: String? = null` parameters to both `ProxyConfig.Socks5` and `ProxyConfig.Http`. Null defaults preserve backward compatibility with all existing two-argument call sites, including the Tor call `ProxyConfig.Socks5(TOR_PROXY_HOST, TOR_PROXY_PORT)`.

Extended `ProxyConfigTest` with three new tests:
- `Socks5 with explicit nulls equals default two-arg constructor` — verifies default equivalence
- `Socks5 inequality when one credential differs` — verifies data class structural equality on password
- `Http accepts optional credentials symmetrically` — verifies Http variant behaves identically

All 7 ProxyConfigTest tests pass.

### Task 2: applyProxySystemProperties state machine

Replaced the original single-branch `when (is Socks5 -> set; else -> clear)` with a full three-branch state machine:

- **Socks5 branch**: sets `socksProxyHost`/`socksProxyPort`; sets `java.net.socks.username`/`password` when credentials present, clears them otherwise; always clears all four HTTP properties to prevent bleed-through.
- **Http branch**: sets `http.proxyHost`/`http.proxyPort`; sets `http.proxyUser`/`http.proxyPassword` when credentials present, clears them otherwise; always clears all four SOCKS properties to prevent bleed-through.
- **None/else branch**: clears all eight properties.

Code comments on both active branches document the GeckoView Necko caveat (JVM-layer-only properties, not honored by Gecko's C++ network stack for browser traffic — Research Pitfalls 1 and 2).

Added G9/G10/G11/G12 tests following the existing G5/G6 try/finally cleanup pattern. All 12 GeckoEngineManagerProxyTest tests pass.

### Task 3: proxyConfigFor custom proxy routing

Rewrote `proxyConfigFor` from a single-line `if (useTor)` to a four-branch `when`:
1. `customProxyType == SOCKS5 && host non-blank && port > 0` → `ProxyConfig.Socks5(host, port, username, password)`
2. `customProxyType == HTTP && host non-blank && port > 0` → `ProxyConfig.Http(host, port, username, password)`
3. `useTor` → `ProxyConfig.Socks5("127.0.0.1", 9050)`
4. `else` → `ProxyConfig.None`

A local `val proxyHost = app.customProxyHost` is required to satisfy the Kotlin smart-cast requirement — `customProxyHost` is a public API property declared in a different module (`core:domain`) and Kotlin cannot smart-cast cross-module properties in a `when` condition guard.

Added PC-3/PC-4/PC-5/PC-6/PC-7 tests covering custom SOCKS5 with credentials, custom HTTP, port-0 guard, blank-host guard, and custom-proxy-wins-over-Tor priority assertion. All 9 GeckoViewEngineProxyConfigTest tests pass.

## API Changes

**ProxyConfig.Socks5** before:
```kotlin
data class Socks5(val host: String, val port: Int) : ProxyConfig()
```
After:
```kotlin
data class Socks5(val host: String, val port: Int, val username: String? = null, val password: String? = null) : ProxyConfig()
```
Same change applies to `ProxyConfig.Http`. All existing call sites compile unchanged.

## Test Coverage

| Test File | Tests | Status |
|-----------|-------|--------|
| ProxyConfigTest | 7 (4 existing + 3 new) | Green |
| GeckoEngineManagerProxyTest | 12 (8 existing + 4 new) | Green |
| GeckoViewEngineProxyConfigTest | 9 (4 existing + 5 new) | Green |
| Full :core:engine:testDebugUnitTest suite | 88 | Green |
| detekt | - | Green |
| lint | - | Green |

## Threat Model Coverage

| Threat | Status |
|--------|--------|
| T-18-03 Tampering via proxyConfigFor (port=0 silent failure) | Mitigated — port>0 + non-blank host guards in proxyConfigFor; PC-5/PC-6 verify fallback to None |
| T-18-06 Proxy property bleed-through on SOCKS5 to HTTP switch | Mitigated — applyProxySystemProperties clears all inactive-type properties; G9/G10/G11/G12 verify |
| T-18-01 Credential disclosure via JVM properties | Accepted — process-internal only; documented in code comments that JVM props are best-effort for Gecko Necko |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Kotlin smart-cast restriction on cross-module nullable property**
- **Found during:** Task 3 GREEN phase
- **Issue:** `app.customProxyHost` is a public API `String?` property declared in `core:domain` (different module). Kotlin 2.x cannot smart-cast cross-module public properties inside a `when` guard, resulting in compilation error "Smart cast to 'String' is impossible"
- **Fix:** Assigned `val proxyHost = app.customProxyHost` before the `when` expression; the local `val` is smart-castable and the host guard `!proxyHost.isNullOrBlank()` correctly narrows to non-null within each branch
- **Files modified:** GeckoViewEngine.kt
- **Commit:** 143fbcd (included in Task 3 commit)

## Known Stubs

None — this plan delivers engine routing infrastructure only. No UI rendering or data stubs.

## Threat Flags

No new network endpoints, auth paths, or trust-boundary surface introduced beyond what is in the plan's threat model.

## Self-Check: PASSED

- ProxyConfig.kt has 2 `val username: String?` and 2 `val password: String?` matches: FOUND
- GeckoEngineManager.kt has http.proxyHost (setProperty + clearProperty) and java.net.socks.username: FOUND
- GeckoEngineManager.kt clearProperty count = 20 (>= 8): FOUND
- GeckoViewEngine.kt has ProxyType.SOCKS5, ProxyType.HTTP, 2 customProxyPort > 0, 2 isNullOrBlank: FOUND
- :core:engine:testDebugUnitTest suite = 88 tests, all green: FOUND
- Commits d254821, ba76d50, 143fbcd exist in git log: FOUND
