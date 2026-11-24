# AGENTS.md: BibleBible AI Agent Instructions

Permanent coding rules, verification commands, and workflow conventions for contributors and coding agents working in this repository.

## 1. Persistent Context Workflow
Before starting development on any new task or conversation:
1. Review `AGENTS.md`, `README.md`, `docs/PROJECT_CONTEXT.md`, `docs/HANDOFF.md`, and `docs/mobile-test-release.md`.
2. Verify that documented branch state, module boundaries, and file paths match the current codebase.
3. Read relevant records in `docs/adr/` before proposing architectural changes.

When completing a significant milestone or preparing to switch conversations:
1. Update `docs/HANDOFF.md` with completed work, outstanding issues, relevant files, verified test results, and next steps.
2. Update `docs/PROJECT_CONTEXT.md` when meaningful architectural or technical decisions change (or add a numbered ADR under `docs/adr/`).
3. Keep documentation concise and clearly separate verified command results from assumptions.
4. Never overwrite unrelated decisions or existing documentation.

## 2. Architecture & Coding Conventions
- **Stack**: Kotlin Multiplatform (`2.1.21`), Compose Multiplatform (`1.7.3`), Ktor (`2.3.12`), SQLDelight (`2.3.2`), Kotlinx Serialization, Napier logging.
- **Targets**: `androidMain`, `iosMain`, `desktopMain`, and native SwiftUI wrapper `/iosApp` (Debug builds use `email.kevinphillips.biblebible.debug` so they install side-by-side without overwriting Release installs).
- **State Management**: Shared `@ThreadLocal` state objects (`BibleState`, `GeminiModel`, `CloudSessionManager`, `AnalyticsTracker`) paired with repository helpers (`BibleRepository`, `DailyVerseRepository`, `BiblicalShortsRepository`, `AppPrefsRepository`).
- **Database Schema**: Keep `BibleBibleDatabase` SQLDelight schema version at `8L` unless a deliberate migration is required; store supplemental metadata such as paragraph/red-letter formatting in side tables (`ChapterFormatMeta`).
- **Platform Hardware & Gesture Integration**:
  - Keep shared UI in `commonMain` and isolate platform hardware/SDK boundaries behind `expect`/`actual` declarations (`DriverFactory`, `SetSystemBarColor`, `ShortsVideoPlayer`).
  - On iOS `UIKitView` overlays (`ShortsVideoPlayer.ios.kt`), keep `UIKitInteropProperties(isInteractive = false, isNativeAccessibilityEnabled = false)` and `userInteractionEnabled = false` so Compose gestures handle taps/swipes, and configure `AVAudioSessionCategoryPlayback` so video audio plays cleanly on physical devices.
- **UI Animations & Testability**:
  - Do **not** use `rememberInfiniteTransition()` when continuous motion is needed on Android test devices or Compose UI tests: Android developer option `animator_duration_scale = 0` freezes infinite transitions, and `runComposeUiTest` waits on transition idleness. Use wall-clock-driven `Canvas` / coroutine loops (such as `GoldSpinningProgressIndicator` in `LoadingScreen.kt`) or deterministic test flags.
  - Keep AI summary formatting clean on both server and client: strip horizontal rule lines (`---`, `***`, `___`) and avoid em-dashes or double hyphens in user-facing copy.

## 3. Security & Repository Boundaries
- **Zero Client Secrets**: Never embed or commit API keys, tokens, keystores, or `local.properties` values into Git or client `BuildKonfig`. Run `python3 tools/verify_no_client_secrets.py` before pushing.
- **Backend Separation**: `backend/` is intentionally listed in `.gitignore` and deployed directly to Google Cloud Run (`biblebible-api`). Do not remove `backend/` from `.gitignore` or commit cloud credentials.
- **Public-Domain Content**: The first-party Bible API and Biblical Shorts catalog use only public-domain translations (`KJV`, `BSB`, `WEB`, `BBE`, `RV1909`, `SVD`) and original devotional summaries/lessons.

## 4. Verification & Release Workflow
- **KMP Desktop & Android Host Unit Tests**:
  ```bash
  ./gradlew :composeApp:desktopTest :composeApp:testAndroidHostTest --console=plain --max-workers=2
  ```
- **iOS & Android Debug Compilation**:
  ```bash
  ./gradlew :composeApp:compileKotlinIosArm64 :androidApp:assembleDebug
  ```
- **Secret Leak Check**:
  ```bash
  python3 tools/verify_no_client_secrets.py
  ```
- **Cloud Backend Tests** (run inside git-ignored `backend/`):
  ```bash
  cd backend && npm test
  ```
- **Mobile Store Builds**: Follow [docs/mobile-test-release.md](docs/mobile-test-release.md) and `.github/workflows/mobile-test-release.yml` to publish Android (`internal` track) and iOS (`TestFlight`) test builds before Managed Publishing / Manual Release submission.
