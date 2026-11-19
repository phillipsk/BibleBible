# BibleBible
This is a Bible reading Kotlin Compose Multiplatform app across Android, iOS, and Desktop, backed by a first-party Google Cloud Run gateway for public-domain scriptures, Verse of the Day, Gemini AI chapter summaries, and <30-second Biblical Shorts reels.

Contributions by Kevin Phillips

## Download
[![App Store](previews/app_store_badge.svg)](https://apps.apple.com/us/app/my-biblebible/id6478799350)

<a href='https://play.google.com/store/apps/details?id=email.kevinphillips.biblebible'>
<img src="https://play.google.com/intl/en_gb/badges/static/images/badges/en_badge_web_generic.png" width=240 />
</a>

## Project Context & AI Agent Memory
To keep development fast and preserve architectural context across conversations, this repository follows a structured 4-file memory layout plus ADRs:
- [`AGENTS.md`](AGENTS.md) — Permanent AI rules, coding conventions, security boundaries, and verification commands.
- [`docs/PROJECT_CONTEXT.md`](docs/PROJECT_CONTEXT.md) — Long-term architectural decisions, module boundaries, SDK constraints, and cost guardrails.
- [`docs/HANDOFF.md`](docs/HANDOFF.md) — Current task status, blockers, verified tests, and next steps.
- [`docs/adr/`](docs/adr/) — Architecture Decision Records (`0001` Cloud Gateway, `0002` KMP vs. Native Video, `0003` Biblical Shorts Google Cloud Pipeline & <\$100/mo cap).

## Architecture Overview
- **Zero Client-Side Secrets**: All provider API calls (public-domain scriptures, daily verse, Gemini summaries, and Biblical Shorts metadata/streaming) route through the first-party Google Cloud Run gateway configured via `CLOUD_GATEWAY_URL`. No `IQ_BIBLE_API_KEY` or `GEMINI_API_KEY` is bundled into mobile binaries.
- **Biblical Shorts (Reels)**: Users can enter the vertical Reels experience directly from the Home Screen entry card underneath **Verse of the Day** (featuring an animated left-to-right swipe arrow). Each of the **50 curated Biblical Shorts** (<30s) pairs hardware-accelerated native video playback (`TextureView` + `MediaPlayer` on Android, `AVPlayer` on iOS) with scripture references, timed narrative/lesson captions, category filters, and one-tap **Read Chapter** navigation directly to that book and chapter.
- **Public-Domain Translations**: Supports `KJV`, `BSB`, `WEB`, `BBE`, `RV1909`, and `SVD`.

## Build and test
Use JDK 21 and Android SDK 37.0. The Android launcher is `androidApp`, shared multiplatform code is `composeApp`, and the desktop launcher is `desktopApp`.

### Configuration (`local.properties`)
Optional non-sensitive runtime configuration in `local.properties` (git-ignored):
```properties
CLOUD_GATEWAY_URL=https://biblebible-api-687901748717.us-central1.run.app
AI_SUMMARIES_ENABLED=true
ENABLE_DIAGNOSTIC_LOGGING=false
```

```bash
# Build Android & Desktop artifacts
./gradlew :androidApp:assembleDebug :desktopApp:jvmJar

# Run shared KMP Desktop & Android host test suites
./gradlew :composeApp:desktopTest :composeApp:testAndroidHostTest

# Verify zero secrets exist in tracked files
python3 tools/verify_no_client_secrets.py

# Cloud backend tests (untracked backend/ directory)
cd backend && npm test
```

See [build configuration and stable versions](docs/build-modernization.md) and [platform test commands](composeApp/README_TESTS.md). API credentials are optional for builds and mocked tests.

## Approach
To keep things simple and avoid third-party navigation and state management libraries, I have taken a unique approach. Instead of using a traditional navigation library or Koin DI framework, I manage the app's state with a Kotlin object and mimic navigation using animated visibility composables.

## Project Modules
- `/androidApp` — Android application launcher module depending on `:composeApp`.
- `/composeApp` — Shared Kotlin Multiplatform UI and data layers:
  - `commonMain` — Compose UI (`BibleHomeScreen`, `VerseOfTheDayCard`, `BiblicalShortsEntryCard`, `BiblicalShortsScreen`, `BibleScripturesPager`), Ktor HTTP clients, SQLDelight database (`BibleBibleDatabase`), `CloudSessionManager`, `DailyVerseRepository`, `BiblicalShortsRepository`.
  - `androidMain` — Android entry point, SQLDelight Android driver, hardware-accelerated `ShortsVideoPlayer` (`TextureView` + `MediaPlayer`).
  - `iosMain` — iOS `ComposeUIViewController` bridge, SQLDelight native driver, hardware-accelerated `ShortsVideoPlayer` (`AVPlayer` + `AVPlayerLayer`).
  - `desktopMain` — JVM Desktop Compose UI, SQLite driver, storyboard `ShortsVideoPlayer`.
- `/iosApp` — Native Xcode / SwiftUI host consuming `ComposeApp.framework`.
- `/backend` *(git-ignored, deployed directly to Google Cloud Run)* — Node.js 20 Cloud Run API, Gemini TTS + Gemini Image + FFmpeg reel generator, GCS sync, and Cloud Billing budget script.

## Previews
### Android (top) and iOS (bottom)
https://github.com/phillipsk/BibleBible/assets/7585083/a41d07b4-9dd4-447c-a873-49963f12f51d

https://github.com/phillipsk/BibleBible/assets/7585083/af616ab8-ec06-4b2b-a58b-13e7cac5d439
