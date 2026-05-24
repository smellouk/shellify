# run-screenshot-tests

Composite action that runs Roborazzi screenshot verification and uploads diff images as an artifact when verification fails.

## Inputs

| Input | Required | Description |
|---|---|---|
| `gradle-task` | Yes | Gradle task to run (e.g. `verifyRoborazziDebug`, `verifyRoborazziRelease`) |

## Usage

```yaml
- uses: ./.github/actions/run-screenshot-tests
  with:
    gradle-task: verifyRoborazziDebug
```

Diffs are uploaded to an artifact named `screenshot-diffs` and retained for 7 days. To update goldens locally, run `./gradlew recordRoborazziDebug` and commit the updated images.
