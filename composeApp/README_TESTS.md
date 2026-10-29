# BibleBible tests

Use JDK 21. The Android app is packaged by `:androidApp`; shared code and its tests live in `:composeApp`. See [the build refresh notes](../docs/build-modernization.md) for the pinned toolchain and platform requirements.

## Desktop and Android host tests

```bash
./gradlew :composeApp:desktopTest
./gradlew :composeApp:testAndroidHostTest
```

The desktop task includes the shared data and Compose tests. Android host tests run the same shared tests and the Android-specific Robolectric tests. On a headless Linux machine:

```bash
SKIKO_RENDER_API=SOFTWARE xvfb-run -a ./gradlew :composeApp:desktopTest :composeApp:testAndroidHostTest
```

Test reports are written under `composeApp/build/reports/tests/`. Mocked data and transport tests need no API credentials. They exercise serialization, model updates, reading history, and Gemini request/error handling without contacting providers.

## Android device tests

Start an emulator or connect a device first, then run:

```bash
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:connectedAndroidDeviceTest
```

A single instrumentation test class can be selected with the Android runner argument:

```bash
./gradlew :composeApp:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=ui.ExampleUiTest
```

The existing directories remain supported through explicit Gradle source mappings:

- `src/commonTest/kotlin/`: shared tests and UI fixtures.
- `src/desktopTest/kotlin/`: desktop UI test entry points.
- `src/test/kotlin/` and `src/test/resources/`: Android host tests and Robolectric configuration.
- `src/androidTest/kotlin/`: Android device tests and fake data.

## iOS simulator tests

These require Apple Silicon macOS, Xcode, and an installed iPhone simulator. Linux cannot run this target, and current Compose no longer supports the Intel `iosX64` simulator target.

```bash
xcrun simctl list devices available
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :composeApp:iosSimulatorArm64Test -PiosSimulatorDevice=SIMULATOR_UDID
```

The macOS job in `.github/workflows/build.yml` selects and boots an available iPhone simulator and runs the shared test executable. Test compilation or framework linking alone does not prove simulator execution.

## UI regression coverage

Use stable test tags and fake data for UI tests. Read test counts and failure details from the current run rather than treating a successful build or an open emulator as passing tests. Rotation, text reflow, saved-state recreation, and reading-position expiry are separate behaviors; keep their assertions independent.
