# Release 2.1: direct provider access

This release requires no backend. Provider keys remain recoverable from a client
application, including after R8 and the reversible build-time mask. Rotate/revoke
compromised credentials independently of deploying the app.

## Scope

- Diagnostic logging defaults to off on every platform.
- Gemini credentials use `x-goog-api-key`, not URL query parameters.
- Diagnostic HTTP logs redact provider authentication headers.
- Requests explicitly send a 2,048-token output budget and one candidate.
- Billing/authentication/quota failures produce useful messages without logging provider bodies.
- The existing chapter navigation and persistence remain in use. PR #53's new
  caches and pager refactor are deferred.
- Android: 7.0 (17). iOS: 5.7 (7), following the existing Info.plist version pattern.

## Android release

R8 uses the existing `composeApp/proguard-rules.pro` and Android's optimized
baseline rules. Resource shrinking is unchanged from the previous release setup.

Set these values in ignored `local.properties` (never commit the values):

```properties
ANDROID_RELEASE_STORE_FILE=<existing upload keystore path>
ANDROID_RELEASE_KEY_ALIAS=<existing upload alias>
ANDROID_RELEASE_STORE_PASSWORD=<store password>
ANDROID_RELEASE_KEY_PASSWORD=<key password>
```

```sh
./gradlew :composeApp:bundleRelease :composeApp:assembleRelease -PENABLE_DIAGNOSTIC_LOGGING=false -PAI_SUMMARIES_ENABLED=true
```

Verify the release signing certificate matches the prior upload key and retain
`composeApp/build/outputs/mapping/release/mapping.txt` for deobfuscation.

## iOS release

`iosApp/iosApp/Info.plist` owns the marketing and build versions. Xcode's `iosApp`
scheme archives the Release configuration for the existing team and bundle ID.
Export the archive with the App Store Connect distribution method for TestFlight.
Store upload and public rollout are separate from preparing signed artifacts.

## Validation

```sh
./gradlew :composeApp:desktopTest --tests 'data.*'
```

Smoke-test the signed minified Android app and the iOS simulator: startup,
chapter selection/swiping, version selection, font controls, history/home, and
AI summary success after the Google project has usable billing credits.
