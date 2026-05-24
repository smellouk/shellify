# run-unit-tests

Composite action that runs Gradle unit tests, publishes a JUnit summary via `mikepenz/action-junit-report`, and uploads HTML reports as a build artifact.

## Inputs

| Input | Required | Description |
|---|---|---|
| `gradle-task` | Yes | Gradle task to run (e.g. `testDebugUnitTest`, `testReleaseUnitTest`) |

## Usage

```yaml
- uses: ./.github/actions/run-unit-tests
  with:
    gradle-task: testDebugUnitTest
```

The calling job requires `permissions: checks: write` for the JUnit report action to post PR annotations.
