# Mobile test releases

The `Mobile test release` GitHub Actions workflow builds signed releases and
publishes them to Google Play internal testing and Apple TestFlight. A
`test-release-*` tag runs both jobs. A `testflight-release-*` tag runs only the
iOS job, and a `play-release-*` tag runs only Android. Platform-specific tags
allow retries without reusing a build number in the other store. A manual
workflow run can publish `both`, `android`, or `ios`.

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
key cannot create profiles through the App Store Connect API. Releases fail
early with a clear renewal message when the profile has fewer than 30 days
remaining.

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
new build; both stores reject reused build numbers. Update `versionName` and
`MARKETING_VERSION` for a public version change. Push a unique tag from the
release commit:

```bash
git tag test-release-<version>-<build>
git push origin test-release-<version>-<build>
```

For an iOS-only retry or release:

```bash
git tag testflight-release-<version>-<build>
git push origin testflight-release-<version>-<build>
```

For an Android-only release:

```bash
git tag play-release-<version>-<build>
git push origin play-release-<version>-<build>
```

GitHub's macOS runner archives the iOS app with App Store Connect API-key
authentication and uploads the exported archive to TestFlight. Apple then
processes the build before it appears for internal testers.

The delivery workflow currently lives in the stacked delivery PR. Merge the
stack in order—platform/reading-state base, UI modernization, then mobile
delivery—so the workflow is available from the repository's default branch.

## Android internal testers

Tester membership is managed in Google Play Console under **Testing > Internal
testing > Testers**. Add the Google account used by the Android device to an
email list or Google Group, save the track, and open the displayed opt-in link
while signed into that account. After opting in, install or update BibleBible
from Google Play. The Android Publisher API used by this workflow publishes
bundles and track releases but does not administer tester email lists.
