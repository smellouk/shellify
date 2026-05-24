# .github/workflows

CI/CD workflow definitions.

| Workflow | Trigger | Purpose |
|---|---|---|
| `pull_request.yml` | Pull request open / synchronize | Runs detekt, lint, unit tests, screenshot verification, and architecture checks on every PR |
| `main.yml` | Push to `main` | Full check suite + debug APK build + instrumentation tests on an emulator |
| `release.yml` | Push of a `v*` tag | Signed release APK build, changelog generation via git-cliff, GitHub Release creation |

All workflows use the composite actions in [`../actions/`](../actions/) to avoid duplication.
