# HANDOFF.md: Current Development State

## Current Branch & Release Artifacts
- **Active Release Branch**: `release/3.1` (open PR #63 into `main`)
- **Android Release Version**: `versionName = "3.1"`, `versionCode = 23`
- **iOS Release Version**: `MARKETING_VERSION = 5.8`, `CURRENT_PROJECT_VERSION = 19`
- **CI Test Release Run**: `.github/workflows/mobile-test-release.yml` run `#63` (`45394576867`) uploading Android `3.1 (23)` to Google Play Internal Testing and iOS `5.8 (19)` to TestFlight.
- **Cloud Run Backend**: `biblebible-api` revision `biblebible-api-00017-vw9` (`us-central1`) at `https://biblebible-api-687901748717.us-central1.run.app`.

## Completed Work on `release/3.1`
1. **Literary Serif Bible Reader & Cloud Config Flags**:
   - Bundled SIL OFL `Newsreader` fonts (`newsreader_regular.ttf`, `newsreader_semibold.ttf`), `22sp` default text size, Warm Coral (`#F48B8B`) red-letter Words of Jesus spans, superscript gold verse `1`, and SQLite-safe `ChapterFormatMeta` side table keeping SQLDelight schema at `8L`.
   - Clean white `ChapterTopBar`, gold `#FBD38D` selected chapter tab indicator in `BibleScripturesPager`, and gold `#FBD38D` settings drawer accents (`BottomSheetConfigView`, including top drag bar).
2. **Sticky AI Chapter Summaries & Loading Polish**:
   - Gold `#FBD38D` AI checkmark and outline border (`isAiButtonHighlighted` in `GeminiModel.kt`), locked across same-book chapter swipes and reset on failure, toggle off, Home/back navigation, book changes, or deep links (`restoreReadingLocation`).
   - Stripped markdown horizontal rules (`---`, `***`, `___`) and em-dashes/double hyphens in both backend (`geminiGateway.js` `:v2` cache key + `sanitizeSummaryText`) and client (`GeminiSummary.kt`).
   - Wall-clock-driven `GoldSpinningProgressIndicator` and `GoldSlidingProgressBar` in `LoadingScreen.kt` so loading indicators spin smoothly even with Android `animator_duration_scale = 0`.
3. **Biblical Words / Shorts Polish**:
   - Fixed iOS tap/double-tap interception (`UIKitInteropProperties(isInteractive = false)`) and physical-device audio (`AVAudioSessionCategoryPlayback`).
   - Applied Sample 1 translucent top bar (`0.68f` white title, `0x4709111E` frosted mute circle), daily 2-reel cap for `1x Pause · 2x Sound` legend (`GESTURE_LEGEND_MAX_PER_DAY = 2`), and continuous auto-advance after 2 completed plays (`SHORTS_AUTO_ADVANCE_AFTER_PLAYS = 2`).
4. **Device & Reading Analytics + GitHub Actions Hardening**:
   - Added persistent app-scoped `deviceId` (`X-Client-Device-Id`), per-book and per-chapter active reading time tracking, and Shorts autoplay idle detection aggregated in backend `/v1/analytics/summary` and `/analytics`.
   - Added `release/**` trigger and `verify_no_client_secrets.py` step to `.github/workflows/build.yml`.

5. **Reader Follow-up**:
   - Chapter pages use the AI loading card while AI mode is active and the chapter or its summary is pending. Scripture mode uses the scripture shimmer while its chapter loads.
   - The reader top bar uses larger book titles. Reading Tips teaches pinch and double-tap and stays dismissed after either gesture or a tap on Got it.

## Reader Follow-up Verification
- Desktop tests: 113 passed. Android host tests: 76 passed. Android debug APK built successfully.
- Smoke test the side-by-side BibleBible Dev build on both phones. Open Deuteronomy 2 and Genesis 23, swipe between chapters with AI locked, then return to scripture and try both font gestures. Confirm the tip remains dismissed when reopening the reader.
- Wait for physical-device smoke-test feedback before preparing new Google Play and TestFlight builds.

## Verified Results
- `cd backend && npm test`: **24/24 passed**.
- `python3 tools/verify_no_client_secrets.py`: **248 files scanned, 0 leaks found**.
- `./gradlew :composeApp:testAndroidHostTest :composeApp:desktopTest :composeApp:compileKotlinIosArm64 :androidApp:assembleDebug`:
  - `androidHostTest`: **74 tests, 0 failed, 0 skipped**.
  - `desktopTest`: **109 tests, 0 failed, 0 skipped**.
  - `compileKotlinIosArm64` and `assembleDebug`: **BUILD SUCCESSFUL**.

## Remaining Next Steps
1. Complete manual smoke testing on Google Play Internal Testing (`3.1 (23)`) and Apple TestFlight (`5.8 (19)`).
2. Confirm Google Play **Managed Publishing** is enabled in Play Console before promoting/submitting Production review, and set App Store Connect `5.8 (19)` to **Manually release this version** when submitting to App Review.
3. Open/merge PR from `release/3.1` to `main` once approved and released.
