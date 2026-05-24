# setup-keystore

Composite action that decodes a base64-encoded keystore to `${{ runner.temp }}/keystore.jks` and optionally verifies its SHA-256 fingerprint to guard against keystore substitution.

## Inputs

| Input | Required | Description |
|---|---|---|
| `keystore-base64` | Yes | Base64-encoded keystore (stored as a repository secret) |
| `store-password` | No | Keystore password — required when `expected-fingerprint` is set |
| `expected-fingerprint` | No | Expected SHA-256 fingerprint; the step fails if it doesn't match |

## Outputs

| Output | Description |
|---|---|
| `keystore-path` | Absolute path to the decoded keystore (`${{ runner.temp }}/keystore.jks`) |

## Usage

```yaml
- uses: ./.github/actions/setup-keystore
  with:
    keystore-base64: ${{ secrets.KEYSTORE_BASE64 }}
```
