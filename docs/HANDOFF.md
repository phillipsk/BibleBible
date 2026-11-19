# HANDOFF.md — Current Development State

## Current Branch & Context
- **Branch**: `feature/biblical-shorts-and-docs` (and alias `feature/biblical-shorts`), rebased onto `release/3.0` / `main` (`c944478`, `tag: v3.0`)
- **Worktree**: `/Users/phillipsk7461/Documents/BibleBibleAntigravity/biblebible-shorts`
- **Active Feature**: Persistent Project Memory Docs (`AGENTS.md`, `docs/`), AI-Narrated Motion Biblical Shorts Cloud Backend (<\$100/mo), Live GCP Deployment, and Home Screen -> Vertical Reels KMP UI with exact-chapter reader integration.

## Completed Work
- Rebased cleanly onto `release/3.0` / `main` (`c944478`) and connected `BiblicalShortsScreen`'s **Read Chapter** action to `BibleIQDataModel.restoreReadingLocation(book, shortItem.chapter)` so selecting a short opens that exact book and chapter in `BibleScripturesPager`.
- Created persistent context & architecture documentation: `AGENTS.md`, `README.md`, `docs/PROJECT_CONTEXT.md`, `docs/HANDOFF.md`, and ADRs `0001`, `0002`, and `0003`.
- Built and deployed the Top 50 Biblical Shorts pipeline (`backend/`, untracked):
  - 50 curated <30s Old & New Testament narratives/lessons with 3 timed scenes each (`shortsCatalog.js`) and HTTP `206 Partial Content` streaming (`shortsService.js`, `server.js`).
  - Synthesized all 50 vertical `720x1280` H.264/AAC MP4 reels (`-movflags +faststart`, 79 MB total) with Gemini Image artwork camera motion and 50/50 Gemini TTS spoken narration + ambient harmonic pads (`generate-shorts-videos.mjs`).
  - Uploaded all 50 MP4s (54.5 MiB) to `gs://gen-lang-client-0150678391-biblebible-shorts/shorts/` and deployed `biblebible-api` revision `biblebible-api-00003-lqt` in `us-central1`.
  - Configured Cloud Billing budget `BibleBible Monthly Cap ($100)` (`798e52d4-1a22-43a8-9895-372b04fe8ff9`) on `billingAccounts/0198D2-D43B38-EC3C1E`.
- Added automated launch-demo hooks (`auto_demo` Intent extra on Android `MainActivity`, `BIBLEBIBLE_AUTO_DEMO=1` env var on iOS `MainViewController`) for reproducible simulator/emulator verification recordings.

## Verified Results
- `cd backend && npm test`: **7/7 tests passed**.
- Live Cloud Run `GET https://biblebible-api-687901748717.us-central1.run.app/GetBiblicalShorts?limit=2`: **HTTP 200 OK**.
- Live Cloud Run & GCS `Range: bytes=0-1023` on `short-01-creation-light.mp4`: **HTTP/2 206**, `content-type: video/mp4`.
- `./gradlew :composeApp:desktopTest :composeApp:testAndroidHostTest :androidApp:assembleDebug`:
  - `desktopTest`: **20 suites, 71 tests, 0 failures, 0 errors, 0 skipped**
  - `testAndroidHostTest`: **12 suites, 42 tests, 0 failures, 0 errors, 0 skipped**
  - `:androidApp:assembleDebug`: **BUILD SUCCESSFUL** (`androidApp-debug.apk`, 18 MB)
- `python3 tools/verify_no_client_secrets.py`: **OK** (0 secret leaks across 206 scanned files).

## Next Steps
1. Record and review end-to-end iOS Simulator & Android Emulator demo MP4 videos saved to `/Users/phillipsk7461/Desktop/BibleBible-Shorts-Demo/`.
2. Open a PR from `feature/biblical-shorts` / `feature/biblical-shorts-and-docs` into `main` / `release/3.0`.
