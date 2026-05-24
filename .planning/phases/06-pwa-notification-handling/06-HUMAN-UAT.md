---
status: complete
phase: 06-pwa-notification-handling
source: [06-VERIFICATION.md]
started: 2026-05-23T00:00:00Z
updated: 2026-05-24T00:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. End-to-end GeckoView notification delivery
expected: trigger `new Notification(...)` from a PWA; notification appears in Android shade and in NotificationHistory screen
result: pass

### 2. End-to-end System WebView notification delivery
expected: ShellifyBridge intercepts JS call; notification dispatched via PwaNotificationDispatcher
result: pass

### 3. First-time permission dialog
expected: AlertDialog shown on first notification request; GRANTED/DENIED persisted; POST_NOTIFICATIONS OS dialog on API 33+
result: pass

### 4. DND suppression
expected: device clock at 23:00 with 22→8 DND window — notification suppressed; outside window — delivered
result: pass

### 5. BackgroundNotificationService keepalive
expected: foreground service keeps GeckoRuntime alive; push delivered while WebView is closed
result: pass

### 6. Instrumented navigation test
expected: `./gradlew connectedDebugAndroidTest` passes NotificationHistoryNavigationTest on emulator
result: pass

## Summary

total: 6
passed: 6
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
