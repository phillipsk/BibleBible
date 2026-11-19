# PROJECT_CONTEXT.md — BibleBible Technical Context

Long-term architectural context, module responsibilities, constraints, and technical tradeoffs for BibleBible.

## 1. Platform & Toolchain Baseline
- **Kotlin Multiplatform**: `2.1.21`
- **Compose Multiplatform**: `1.7.3` (Compose Material + Material3 dependencies)
- **Android SDK**: `minSdk = 24`, `compileSdk = 37`, `targetSdk = 37`, Java `VERSION_1_8` bytecode
- **Networking**: Ktor `2.3.12` (`okhttp` on Android, `darwin` on iOS, `cio` on Desktop) with `Resources` and `ContentNegotiation` (`kotlinx-serialization-json` `1.11.0`)
- **Persistence**: SQLDelight `2.3.2` (`BibleBibleDatabase`, schema version `5`) storing cached chapters, chapter counts, reading history, and user preferences
- **Build Configuration**: `BuildKonfig` `0.15.1` injects `CLOUD_GATEWAY_URL`, `AI_SUMMARIES_ENABLED`, and `ENABLE_DIAGNOSTIC_LOGGING` (no secrets)

## 2. Module Responsibilities
| Path | Responsibility |
| :--- | :--- |
| `composeApp/src/commonMain` | Shared UI screens, navigation state (`BibleIQDataModel`, `BibleAPIDataModel`, `GeminiModel`), Cloud session management (`CloudSessionManager`), Ktor repositories (`DailyVerseRepository`, `BiblicalShortsRepository`), and SQLDelight queries. |
| `composeApp/src/androidMain` | `MainActivity`, Android SQLDelight `DriverFactory`, `SetSystemBarColor`, and `ShortsVideoPlayer` using `AndroidView` + `TextureView` + `MediaPlayer`. |
| `composeApp/src/iosMain` | `MainViewController`, Native SQLDelight `DriverFactory`, and `ShortsVideoPlayer` using `UIKitView` + `AVFoundation` (`AVPlayer`/`AVPlayerLayer`). |
| `composeApp/src/desktopMain` | Desktop JVM SQLite `DriverFactory`, desktop font scaling, and deterministic `ShortsStoryboardBackdrop` rendering for Desktop and Compose UI tests. |
| `iosApp/` | Xcode SwiftUI entry point wrapping `ComposeApp` framework. |
| `backend/` *(git-ignored)* | First-party Cloud Run API (`biblebible-api` in `us-central1`), Top 50 Biblical Shorts catalog + MP4 media streaming (`206 Partial Content`), FFmpeg video synthesis pipeline, and GCP budget automation. |

## 3. Core Runtime Endpoints (Cloud Gateway)
- `GET /v1/config` & `POST /v1/auth/token`: Returns runtime configuration (`aiSummariesEnabled`, `geminiModel`, `supportedVersions`) and short-lived HMAC-SHA256 `sessionToken`.
- `GET /GetVersions`, `GET /GetBooks`, `GET /GetChapterCount`, `GET /GetChapter`: Serves normalized public-domain scripture data (`KJV`, `BSB`, `WEB`, `BBE`, `RV1909`, `SVD`) with immutable edge caching.
- `GET /GetVerseOfTheDay`: Serves a deterministic daily verse for the active translation.
- `GET /GetBiblicalShorts` & `GET /v1/shorts`: Serves the curated Top 50 Biblical Shorts (<30s) catalog with timed scenes, scripture metadata, life lessons, and video stream URLs.
- `GET /v1/shorts/media/:fileName`: Streams H.264/AAC MP4 shorts with HTTP byte-range (`206 Partial Content`) support (or redirects/points to Cloud Storage / Cloud CDN when `SHORTS_CDN_BASE_URL` is configured).
- `POST /v1/gemini/summary`: Session-authenticated gateway to Gemini chapter summaries with server-side rate limiting and in-memory summary caching.

## 4. Known Constraints & Tradeoffs
- **Navigation Pattern**: Uses `@ThreadLocal` observable state flags (`showHomePage`, `showBiblicalShorts`) on `BibleIQDataModel` instead of third-party navigation libraries to keep KMP binary size small and state deterministic in UI tests.
- **Compose UI Test Clock**: Continuous frame animations must not use `rememberInfiniteTransition()` in shared composables, because `runComposeUiTest` waits for animation idleness and will time out after 5 seconds.
- **Spend Cap Discipline**: All Cloud Run, GCS media hosting, and Gemini summary workloads are sized and rate-limited to remain strictly under **\$100/month** (estimated **\$4–\$30/month** at 1K–10K MAU).

## 5. Architecture Decision Records
- [ADR 0001: First-Party Cloud Gateway & Session Tokens](adr/0001-first-party-cloud-gateway-and-session-tokens.md)
- [ADR 0002: Compose Multiplatform vs. Native iOS/Android for Video Shorts](adr/0002-compose-multiplatform-vs-native-for-video-shorts.md)
- [ADR 0003: Biblical Shorts Google Cloud Pipeline & \$100/mo Budget Cap](adr/0003-biblical-shorts-google-cloud-pipeline-and-budget-cap.md)
