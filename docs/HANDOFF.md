# HANDOFF.md — Current Development State

## Current Branches & PRs
- **Target Release Branch**: `release/3.1` (branched off `c944478` / `release/3.0` / `v3.0`).
- **Feature Branches**:
  - `feature/biblical-shorts`: [PR #61](https://github.com/phillipsk/BibleBible/pull/61) (`feature/biblical-shorts` -> `release/3.1`).
  - `feature/analytics`: [PR #62](https://github.com/phillipsk/BibleBible/pull/62) (`feature/analytics` -> `release/3.1`).
- **Worktree**: `/Users/phillipsk7461/Documents/BibleBibleAntigravity/biblebible-shorts`

## Completed Work
1. **Biblical Shorts (<30s Video Reels)**:
   - 50 curated Old & New Testament video narratives generated in 720x1280 MP4 (H.264/AAC, 79 MB total) hosted on Google Cloud Storage `gs://gen-lang-client-0150678391-biblebible-shorts/shorts/`.
   - Streaming endpoint `GET /v1/shorts` with HTTP 206 Partial Content range streaming on Cloud Run.
   - Enforced $100/mo spending cap via GCP Cloud Billing budget `BibleBible Monthly Cap ($100)` (ID: `798e52d4-1a22-43a8-9895-372b04fe8ff9`).
   - Compose Multiplatform client:
     - Entry card with animated swipe arrow below Verse of the Day on `BibleHomeScreen`.
     - Full-screen reels pager with category filters, sound/pause toggles, and "Read Chapter" navigation.
     - Platform-native video player integration via `TextureView` on Android, `AVPlayer` on iOS, and deterministic preview on Desktop.
   - Recorded mobile demo tours saved to Desktop:
     - `ios-biblical-shorts-demo.mp4` (26 MB, 16.8s)
     - `android-biblical-shorts-demo.mp4` (12 MB, 18.2s)
2. **First-Party Analytics & Live Dashboard**:
   - `AnalyticsTracker` with background channel coalescing, 15-second batch flushes, offline queueing, and flush-on-exit lifecycle hooks.
   - Full sweep of 22 files across reader, search, audio, AI summary, and Biblical Shorts.
   - Backend collector on Cloud Run: `POST /v1/analytics/events`, `GET /v1/analytics/summary`, structured Cloud Logging (`component: "biblebible-analytics"`), and live auto-refreshing HTML dashboard at `GET /analytics`.
3. **Privacy & Store Compliance Updates**:
   - Updated `PrivacyPolicy.md` with explicit disclosures for anonymous operational telemetry, zero PII, no advertising/ATT requirement, and Google Gemini AI processing.
   - Store compliance specifications for Apple App Store (Nutrition labels: "Not Linked to You", zero tracking) and Google Play (Data Safety: App interactions & Diagnostics, encrypted in transit).
4. **Strategic Architecture Reports**:
   - `docs/reports/release-3.1-strategy-report.html`: Standalone in-depth analysis covering Privacy Policy, Store Compliance, Authentication & Token Cost/Abuse Prevention, and Version Control (GitHub vs. Google ecosystem).
   - Architectural Decision Records: [ADR 0001](adr/0001-cloud-gateway-and-token-security.md), [ADR 0002](adr/0002-kmp-vs-native-video-player.md), [ADR 0003](adr/0003-biblical-shorts-and-budget-cap.md), [ADR 0004](adr/0004-first-party-cloud-analytics-and-live-dashboard.md).

## Verification Matrix
- `cd backend && npm test`: **8/8 suites passing**.
- `desktopTest`: **21 suites, 72 tests passed** (0 failures, 0 errors).
- `testAndroidHostTest`: **13 suites, 43 tests passed** (0 failures, 0 errors).
- `compileKotlinIosSimulatorArm64`: **BUILD SUCCESSFUL**.
- Secret leak scan (`python3 tools/verify_no_client_secrets.py`): **0 leaks found across 207 files**.
- Live Cloud Run service: `https://biblebible-api-687901748717.us-central1.run.app` (`/v1/config`, `/v1/summary`, `/v1/shorts`, `/v1/analytics/events`, `/v1/analytics/summary`, `/analytics`).
