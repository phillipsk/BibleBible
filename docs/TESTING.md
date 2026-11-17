# Regression tests

Run `./gradlew :composeApp:desktopTest :composeApp:testDebugUnitTest` (JDK 21 and the Android SDK from the version catalog). On a Linux host with no display, prefix with `xvfb-run -a`. No provider credentials are needed; all added HTTP tests use Ktor MockEngine.

- `commonTest`: production HTTP configuration and typed IQ requests, response decoding, canonical book mapping, translation availability, scripture formatting and model state. Runs on each supported test target.
- `desktopTest`: real in-memory SQLite checks for translation/chapter isolation, cache retention, chapter counts, preferences and history; repository-to-reader integration through an injected HTTP client; Compose book selection and reader rendering.
- Android unit tests: common tests plus the shared reader rendering scenario under Robolectric. These do not substitute for on-device Android lifecycle tests.
- iOS: common non-UI tests can run with `:composeApp:iosSimulatorArm64Test` on an Apple Silicon Mac with Xcode and its simulator. Linux cannot execute iOS tests. No iOS execution is claimed by the Linux CI job.

The production repository and pager accept optional test dependencies; their defaults preserve current behavior. The pager test explicitly supplies a no-network loader and initializes the real model, so it cannot silently use a provider key or depend on global state from a previous test.

The desktop driver is currently disabled in production. SQLite integration tests instantiate the generated schema directly: they validate the SQL contract, not Android/iOS driver lifecycle behavior. Live provider smoke tests remain separate from deterministic regression tests.
