# PROJECT_CONTEXT.md: BibleBible Technical Context

Long-term architectural context, module responsibilities, constraints, and technical tradeoffs for BibleBible.

## 1. Platform & Toolchain Baseline
- **Kotlin Multiplatform**: `2.1.21`
- **Compose Multiplatform**: `1.7.3` (Compose Material + Material3 dependencies)
- **Android SDK**: `minSdk = 24`, `compileSdk = 37`, `targetSdk = 37` (`versionCode = 24`, `versionName = "3.1"`)
- **iOS Target**: `MARKETING_VERSION = 5.8`, `CURRENT_PROJECT_VERSION = 20`
- **Networking**: Ktor `2.3.12` (`okhttp` on Android, `darwin` on iOS, `cio` on Desktop) with `Resources`, `ContentNegotiation` (`kotlinx-serialization-json` `1.11.0`), and `X-Client-Device-Id` / HMAC `Authorization` headers
- **Persistence**: SQLDelight `2.3.2` (`BibleBibleDatabase`, schema version `8`) storing cached chapters, chapter counts, reading history, user preferences, and `ChapterFormatMeta` (literary paragraph breaks & red-letter `wordsOfJesus` ranges)
- **Build Configuration**: `BuildKonfig` `0.15.1` injects `CLOUD_GATEWAY_URL`, `AI_SUMMARIES_ENABLED`, and `ENABLE_DIAGNOSTIC_LOGGING` (no secrets); Debug builds use `.debug` applicationId suffix for side-by-side installation

## 2. Module Responsibilities
| Path | Responsibility |
| :--- | :--- |
| `composeApp/src/commonMain` | Shared UI screens, literary Newsreader reader (`BibleScriptures`, `BibleScripturesPager`, `GeminiSummary`, `LoadingScreen`), navigation state (`BibleState`, `GeminiModel`), Cloud runtime config & session management (`CloudSessionManager`), Ktor repositories, analytics (`AnalyticsTracker`), and SQLDelight queries. |
| `composeApp/src/androidMain` | `MainActivity`, Android SQLDelight `DriverFactory`, `SetSystemBarColor`, and `ShortsVideoPlayer` using `AndroidView` + `TextureView` + Media3/ExoPlayer. |
| `composeApp/src/iosMain` | `MainViewController`, Native SQLDelight `DriverFactory`, and `ShortsVideoPlayer` using non-interactive `UIKitView` overlays + `AVPlayer` with `AVAudioSessionCategoryPlayback`. |
| `composeApp/src/desktopMain` | Desktop JVM SQLite `DriverFactory`, desktop font scaling, and deterministic `ShortsVideoPlayer` rendering for Desktop and Compose UI tests. |
| `iosApp/` | Xcode SwiftUI entry point wrapping `ComposeApp` framework. |
| `backend/` *(git-ignored)* | First-party Cloud Run API (`biblebible-api` in `us-central1`), HelloAO scripture normalization (`v2` paragraph + `wordsOfJesus` spans), Gemini summary gateway & sanitizer, 24 curated Biblical Shorts clips + MP4 byte-range streaming, and per-book/deviceId analytics collector & HTML dashboard. |

## 3. Core Runtime Endpoints (Cloud Gateway)
- `GET /v1/config` & `POST /v1/auth/token`: Returns runtime configuration (`aiSummariesEnabled`, `geminiModel`, `supportedVersions`, reader layout/font/red-letter flags, book grid/shimmer flags, Biblical Shorts/VOTD toggles) and short-lived HMAC-SHA256 `sessionToken`.
- `GET /GetVersions`, `GET /GetBooks`, `GET /GetChapterCount`, `GET /GetChapter`: Serves normalized public-domain scripture data (`KJV`, `BSB`, `WEB`, `BBE`, `RV1909`, `SVD`) with literary paragraph breaks (`p`), `wordsOfJesus` ranges (`r`), and immutable edge caching.
- `GET /GetVerseOfTheDay`: Serves deterministic daily verse data for the active translation.
- `GET /GetBiblicalShorts` & `GET /v1/shorts`: Serves the 24 curated Biblical Words / Shorts (<30s) catalog with timed scenes, scripture metadata, Practice Today takeaways, and video stream URLs.
- `GET /v1/shorts/media/:fileName`: Streams H.264/AAC MP4 shorts with HTTP byte-range (`206 Partial Content`) support.
- `POST /v1/gemini/summary`: Session-authenticated gateway to Gemini chapter summaries with server-side rate limiting, sanitized markdown (no `---` horizontal rules or em-dashes), and in-memory `:v2` summary caching.
- `POST /v1/analytics/events`, `GET /v1/analytics/summary`, `GET /analytics`: First-party KMP telemetry batch collector tracking `deviceId`, active chapter/book reading time, autoplay idle advances, and live auto-refreshing HTML dashboard.

## 4. Known Constraints & Tradeoffs
- **Navigation Pattern**: Uses `@ThreadLocal` observable state flags (`showHomePage`, `showBiblicalShorts`, `restoreReadingLocation`) on `BibleState` for deterministic KMP state restoration and deep links. Sticky AI summary mode (`aiModeLocked`) persists across same-book chapter swipes and resets on summary failure, toggle off, Home/back navigation, book changes, or explicit navigation jumps.
- **Animation Scale & Compose UI Test Clock**: Spinners in `LoadingScreen.kt` use wall-clock `Canvas` loops so rotation continues even when Android `animator_duration_scale = 0`; continuous idle `rememberInfiniteTransition()` animations are avoided where they stall `runComposeUiTest` clocks.
- **Spend Cap Discipline**: All Cloud Run, GCS media hosting, and Gemini summary workloads are sized and rate-limited under the Cloud Billing budget cap.

## 5. Architecture Decision Records
- [ADR 0001: First-Party Cloud Gateway & Session Tokens](adr/0001-first-party-cloud-gateway-and-session-tokens.md)
- [ADR 0002: Compose Multiplatform vs. Native iOS/Android for Video Shorts](adr/0002-compose-multiplatform-vs-native-for-video-shorts.md)
- [ADR 0003: Biblical Shorts Google Cloud Pipeline & Monthly Budget Cap](adr/0003-biblical-shorts-google-cloud-pipeline-and-budget-cap.md)
- [ADR 0004: First-Party Cloud Analytics & Live Dashboard](adr/0004-first-party-cloud-analytics-and-live-dashboard.md)
