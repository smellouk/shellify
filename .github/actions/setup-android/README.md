# setup-android

Composite action that configures Java 17 (Temurin) and Gradle for Android builds.

Run `actions/checkout` before this action — it does not check out the repo itself.

## Usage

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: ./.github/actions/setup-android
```

## Inputs

None.
