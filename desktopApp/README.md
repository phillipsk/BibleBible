# BibleBible Desktop (macOS)

The `desktopApp` module packages the Kotlin Multiplatform Compose Desktop application for macOS (`email.kevinphillips.biblebible`), conforming to `release/3.2` security and storage standards.

## Local Run & Release Smoke Testing

Run the development desktop app directly:

```bash
./gradlew :desktopApp:run
```

Run the ProGuard-obfuscated release build locally to verify startup and Ktor/SQLite behavior:

```bash
./gradlew :desktopApp:runRelease
```

Run the shared desktop unit and UI test suite:

```bash
./gradlew :composeApp:desktopTest --console=plain --max-workers=2
```

## Storage & Sandbox Paths

- On macOS, the SQLite database (`BibleBibleDB.db`), SQLite extraction temp files (`sqlite-tmp/`), and user preferences (`preferences.properties`) are stored under `~/Library/Application Support/BibleBible` (which resolves inside `~/Library/Containers/email.kevinphillips.biblebible/Data/Library/Application Support/BibleBible` when sandboxed).
- Any existing `~/.biblebible/BibleBibleDB.db` is copied forward automatically on first launch if no database exists yet in Application Support.

## Mac App Store Signing Configuration

Keep certificates, `.p8` keys, and provisioning profiles outside the repository (for example, in `~/Desktop/BibleBible-Cloud-Signing/`). Configure non-secret paths in `local.properties`:

```properties
MAC_APPSTORE_SIGN_IDENTITY=Kevin Phillips (7JJ7U4528N)
MAC_APP_PROVISIONING_PROFILE=/path/to/BibleBible_MacAppStore.provisionprofile
MAC_RUNTIME_PROVISIONING_PROFILE=/path/to/BibleBible_MacAppStore_JVM.provisionprofile
```

Required Keychain certificates:
- `3rd Party Mac Developer Application: Kevin Phillips (7JJ7U4528N)`
- `3rd Party Mac Developer Installer: Kevin Phillips (7JJ7U4528N)`

Required App Store Connect API key (Admin role) placed at `~/.appstoreconnect/private_keys/AuthKey_<KEY_ID>.p8` with `chmod 600`.

## Building, Validating, & Uploading `.pkg`

Build and sign the Mac App Store installer package:

```bash
./desktopApp/package_desktop.sh
```

Build, validate with Apple, and upload to App Store Connect / TestFlight:

```bash
VALIDATE_APP=true UPLOAD_APP=true ASC_KEY_ID=8WY897B3M2 ASC_ISSUER_ID=73b0ed16-6145-4448-81fc-4123f13cf3de ./desktopApp/package_desktop.sh
```

Or invoke `xcrun altool` directly on `desktopApp/build/compose/binaries/main-release/pkg/BibleBible-3.2.0.pkg`:

```bash
xcrun altool --validate-app -f desktopApp/build/compose/binaries/main-release/pkg/BibleBible-3.2.0.pkg -t macos --apiKey 8WY897B3M2 --apiIssuer 73b0ed16-6145-4448-81fc-4123f13cf3de
xcrun altool --upload-app   -f desktopApp/build/compose/binaries/main-release/pkg/BibleBible-3.2.0.pkg -t macos --apiKey 8WY897B3M2 --apiIssuer 73b0ed16-6145-4448-81fc-4123f13cf3de
```
