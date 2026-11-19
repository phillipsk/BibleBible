# Stable build refresh

This change targets the stable releases available on October 29, 2026. It keeps the current Material UI and the direct-provider API behavior from PR #53, including its logging protections, release versions, signing configuration, and R8 settings.

| Component | Pinned stable version |
| --- | --- |
| JDK / JVM bytecode | 21 / 17 |
| Gradle | 9.8.2 |
| Android Gradle Plugin | 9.5.0 |
| Kotlin / Compose compiler | 2.4.21 |
| Compose Multiplatform | 1.12.2 |
| AndroidX Compose tooling/test | 1.12.1 |
| Material icons (latest published core artifact) | 1.7.3 |
| AndroidX Activity | 1.13.0 |
| Ktor | 3.6.0 |
| SQLDelight | 2.4.1 |
| BuildKonfig | 0.23.0 |
| Coroutines | 1.11.0 |
| Kotlin serialization | 1.12.0 |
| Kotlin datetime | 0.8.0 |
| Multiplatform Lifecycle | 2.11.0 |
| Robolectric | 4.17 |

Versions were checked against the official Google Maven, Maven Central, Gradle Plugin Portal, and Gradle release metadata. Prerelease versions were excluded. The Gradle wrapper pins the official distribution SHA-256.

## Android module split

AGP 9 uses a dedicated Kotlin Multiplatform Android library plugin. `composeApp` now uses that supported target instead of combining the Android application plugin with the legacy KMP `androidTarget`. The small `androidApp` module packages the shared Android activity and resources into the same application ID (`email.kevinphillips.biblebible`). Its version remains 7.0 (17), and the iOS version remains 5.7 (7).

No application classes or resources move between directories. The existing Android host tests and device tests are mapped into the new plugin's `androidHostTest` and `androidDeviceTest` source sets. Android build commands now start with `:androidApp`; test commands remain in `:composeApp`.

The unused legacy Google generative-AI Android SDK has been removed from both Android and iOS dependencies. Gemini continues to use the shared Ktor REST client. Ktor's obsolete `ktor-client-serialization` artifact is also removed; JSON serialization is handled by content negotiation and the Kotlin serialization plugin.

Kotlin datetime's `Clock` is now supplied by `kotlin.time`, and the date formatting code uses the current `LocalDate.day` API. Deprecated `ClickableText` is replaced by a text button with the same style and explicit button semantics. Stable Compose resource APIs no longer need experimental opt-ins. Compose test entry points use the current v2 runner, which queues composition coroutines like production rather than running them eagerly. Compose no longer brings the icons artifact transitively, so the existing icons are now declared explicitly at their latest published stable version (1.7.3).

## Development checks

Use JDK 21 and Android SDK platform 37.0. Build-tools 36.0.0 are selected by AGP. The shared Kotlin and Java bytecode target is 17. Core-library desugaring preserves Java-time API support on Android API 24–25, below the platform introduction of `java.time`. API credentials are optional for builds and mocked tests; keep any real keys and release-signing properties in ignored `local.properties`.

```bash
./gradlew :androidApp:assembleDebug :desktopApp:jvmJar
./gradlew :composeApp:desktopTest :composeApp:testAndroidHostTest
./gradlew :composeApp:connectedAndroidDeviceTest
```

On a headless Linux machine, run Compose UI checks under `xvfb-run -a` and select `SKIKO_RENDER_API=SOFTWARE`.

For Apple Silicon macOS with Xcode and an installed iPhone simulator:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :composeApp:iosSimulatorArm64Test -PiosSimulatorDevice=SIMULATOR_UDID
```

The latest Compose and Lifecycle artifacts no longer publish `iosX64`, so the obsolete Intel simulator target is removed. Physical iOS arm64 and Apple Silicon iOS simulator arm64 remain supported; the existing iOS 15.2 minimum deployment target is preserved.

The Xcode project now searches the actual shared-framework output directory and links the correctly cased `ComposeApp` framework; the obsolete `shared` module path is removed.

The workflow in `.github/workflows/build.yml` builds Android debug and minified unsigned release packages and desktop and runs host tests on Linux. Its separate macOS ARM job selects a real installed iPhone simulator, builds the shared framework, runs the shared iOS tests, and builds the SwiftUI simulator app without signing credentials. Simulator results require that job to execute; a Linux build cannot establish iOS readiness. Workflows need no API keys or signing credentials.
