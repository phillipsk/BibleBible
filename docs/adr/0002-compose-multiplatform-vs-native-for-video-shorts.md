# ADR 0002: Compose Multiplatform vs. Native Android/iOS for Video Shorts & Repository Strategy

- **Status**: Accepted
- **Date**: 2026-11-19

## Context
Adding a vertical Reels / Biblical Shorts feed (<30s videos) introduces hardware video decoding, audio playback, and smooth vertical paging. This raised two strategic questions:
1. Should BibleBible switch now from **Kotlin Compose Multiplatform (KMP)** to separate **native Android (Kotlin/Jetpack Compose)** and **native iOS (Swift/SwiftUI)** implementations because of video/hardware integration?
2. If native implementations are pursued later, should they live in one repository or separate repositories, and when is the right time to split?

## Decision
**Stay on Kotlin Compose Multiplatform in a single repository for now, while delegating video playback to native platform hardware views via `expect`/`actual`.**

### 1. Why Video Rendering Does NOT Require Leaving Compose Multiplatform
- **Native Hardware Pipeline via `AndroidView` and `UIKitView`**: Compose Multiplatform does not decode video frames in shared Kotlin or Skia. Instead, an `expect @Composable fun ShortsVideoPlayer(...)` bridges directly to:
  - **Android (`androidMain`)**: `AndroidView` hosting `android.view.TextureView` + `android.media.MediaPlayer` (or Media3/ExoPlayer), backed by Android `MediaCodec` hardware decoding.
  - **iOS (`iosMain`)**: `UIKitView` hosting a native `UIView` backed by Apple `AVFoundation` (`AVPlayer` + `AVPlayerLayer`) and hardware VideoToolbox decoding.
- Because the OS video compositor renders hardware surfaces directly, video playback framerate, battery efficiency, and memory handling match pure native apps.
- Meanwhile, 90%+ of the feature—the 50-item catalog DTOs, Ktor repository, SQLDelight preferences, `VerticalPager` feed, category chips, lesson overlays, and one-tap **Read Chapter** navigation—remains written once and unit-tested in `commonMain`/`commonTest`.

### 2. Why Not Split into Separate Native Repos Right Now (Especially with AI Agents)
- **AI Token & Context Efficiency**: Maintaining two separate repositories (Android + iOS) forces AI coding agents to duplicate data models, API contracts, edge-case fixes, tests, and context documents across two independent codebases—roughly **2x the token usage and drift risk** for every feature change.
- **Reversibility & Low Migration Risk**: This decision does **not** lock us out of native iOS later:
  - `/iosApp` is already a full native Xcode SwiftUI target importing `ComposeApp.framework`.
  - If an iOS-only hardware feature ever outgrows `UIKitView` (for example, advanced PiP, SharePlay, or custom Metal shaders), you can replace only that SwiftUI screen inside `/iosApp` while keeping the shared KMP data layer (`BiblicalShortsRepository`, `BibleIQDataModel`, `CloudSessionManager`) intact.
- **When to Revisit**: Revisit splitting iOS UI to pure SwiftUI only if:
  1. App Store users report specific Skia/Compose scroll gesture friction or Xcode build-time bottlenecks on iOS Reels, OR
  2. You add deep Apple-only OS integrations (Widgets, Live Activities, Picture-in-Picture, CarPlay) that warrant dedicated SwiftUI screens—even then, keeping shared KMP business logic in a monorepo (`composeApp` / `shared` + `iosApp`) is industry best practice rather than maintaining two disconnected GitHub repos.

## Consequences
- Immediate delivery of Biblical Shorts across Android, iOS, and Desktop from a single PR and test suite.
- Hardware video decoding on both mobile OSes via `ShortsVideoPlayer` `expect`/`actual`.
- Zero duplicated repository overhead or split API contract drift.
