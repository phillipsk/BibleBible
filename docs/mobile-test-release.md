# Mobile test releases

The `Mobile test release` GitHub Actions workflow builds signed releases and
publishes them to Google Play internal testing and Apple TestFlight. A
`test-release-*` tag runs both jobs. A `testflight-release-*` tag runs only the
iOS job, which is useful when the Android version code has already been
uploaded.

## One-time repository setup

Release credentials belong in **Settings > Secrets and variables > Actions >
Repository secrets**. They are not GitHub Codespaces secrets, environment
secrets, or local shell values. The workflow requires these names:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
- `ANDROID_UPLOAD_STORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- `IOS_DISTRIBUTION_CERTIFICATE_BASE64`
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`
- `ASC_PRIVATE_KEY`
- `ASC_KEY_ID`
- `ASC_ISSUER_ID`
- `GEMINI_API_KEY`
- `IQ_BIBLE_API_KEY`
- `API_BIBLE_KEY`

The repository includes the public App Store provisioning profile used for
manual distribution signing. The workflow's App Store Connect API key may use
the **Developer** role for authenticated TestFlight uploads. Renewing or
replacing the provisioning profile requires an **Admin** role key; a Developer
key cannot create profiles through the App Store Connect API.

Maintainers with GitHub CLI repository-secret permission can install all of
them without exposing values in command arguments or logs:

```bash
python3 tools/configure_mobile_release_secrets.py \
  --signing-dir /secure/path/BibleBible-Cloud-Signing \
  --google-service-account /secure/path/google-play-service-account.json
```

The caller must be authenticated with `gh` and have repository `Secrets: read
and write` permission. The script reads application API keys from
`GEMINI_API_KEY`, `IQ_BIBLE_API_KEY`, and `API_BIBLE_KEY` in its environment.

## Publishing

Update Android `versionCode` and iOS `CURRENT_PROJECT_VERSION` before every
new build. Push a unique tag from the release commit:

```bash
git tag test-release-<version>-<build>
git push origin test-release-<version>-<build>
```

For an iOS-only retry or release:

```bash
git tag testflight-release-<version>-<build>
git push origin testflight-release-<version>-<build>
```

GitHub's macOS runner archives the iOS app with App Store Connect API-key
authentication and uploads the exported archive to TestFlight. Apple then
processes the build before it appears for internal testers.
