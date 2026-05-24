# run-instrumentation-tests

Composite action that runs Android instrumentation tests on an x86_64 emulator with KVM acceleration and AVD caching via `reactivecircus/android-emulator-runner`.

## Inputs

| Input | Required | Default | Description |
|---|---|---|---|
| `gradle-task` | Yes | — | Gradle task (e.g. `:app:connectedDebugAndroidTest`) |
| `api-level` | No | `29` | Android API level for the emulator |
| `upload-results` | No | `false` | Upload test result artifacts after the run |
| `artifact-name` | No | `instrumentation-test-results` | Artifact name when `upload-results` is `true` |
| `signing-store-file` | No | `''` | Keystore path — leave empty for debug builds |
| `signing-store-password` | No | `''` | Keystore password |
| `signing-key-alias` | No | `''` | Key alias |
| `signing-key-password` | No | `''` | Key password |

## Usage

```yaml
- uses: ./.github/actions/run-instrumentation-tests
  with:
    gradle-task: :app:connectedDebugAndroidTest
```

The calling job requires `permissions: checks: write` for the JUnit report action to post PR annotations.
