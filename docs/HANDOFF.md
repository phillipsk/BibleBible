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
   - The reader top bar reserves a 48 dp back target and fits the book title in the space left by the action buttons. Extra letter spacing is applied when the full title fits, with compact spacing and a smaller title size available for tighter layouts.
   - Reading Tips teaches pinch and double-tap and stays dismissed after either gesture or a tap on Got it.

## Reader Follow-up Verification
- Desktop tests: 113 passed. Android host tests: 76 passed. Android debug APK built successfully.
- Smoke test the side-by-side BibleBible Dev build on both phones. Open Deuteronomy 2 and Genesis 23, swipe between chapters with AI locked, then return to scripture and try both font gestures. Confirm the tip remains dismissed when reopening the reader.
- Wait for physical-device smoke-test feedback before preparing new Google Play and TestFlight builds.

## Home swipe and tutorial reminders
- In the reader, a rightward drag from chapter 1 reveals Home. Later chapters keep their usual chapter paging. Biblical Words uses the same Home reveal, while its leftward swipe still advances to the next clip. Release after about one quarter of the screen to go Home, or release sooner to return to the current screen.
- Reading Tips and the Biblical Words welcome tutorial store dismissal history separately on each installation. Each has one optional follow-up on the next visit after 30 days by default. A second dismissal completes that tutorial. Existing dismissals start the interval on the first check by the updated app.
- The gateway's `GET /v1/config` bootstraps `readerTutorialEnabled`, `readerTutorialReminderDays`, `biblicalShortsTutorialEnabled`, and `biblicalShortsTutorialReminderDays`. Set the interval to `0` to keep initial tips only. Operator settings are documented in the local backend README. The gateway implementation is prepared locally and pending deployment.

- Verification for this follow-up: 116 desktop tests, 78 Android host tests, and 24 gateway tests passed. Both debug builds installed and launched. Android device smoke testing covered paging from Genesis 1 to 2 and back, cancelling a short drag, and returning Home from the reader and Biblical Words.

## Home navigation and release controls
- Swipe left on Home to open Biblical Words when the feed is enabled. Continue dragging upward at the end of the canonical book list to preview and open Genesis 1. Both gestures have individual cloud switches.
- The gateway enforces the AI summary and public In-House Words switches. The app honors the Biblical Shorts switch at feed entry points. Separate switches control the swipe preview, Shorts auto-advance, and Verse of the Day notifications.
- The backend README documents the environment settings and rollout behavior. Gateway and app changes are local pending release review.
- Verification: 117 desktop, 79 Android host, and 25 gateway tests passed. Both debug builds launched. The Galaxy S23 opened Biblical Words from Home and Genesis 1 after two upward swipes at the end of the book grid.

## Genesis entrance
- At the end of the canonical Home grid, the first upward drag raises the Genesis 1 preview from the lower edge while Home fades and drifts upward. A short release returns to the grid. Committing opens the current translation. The preview includes the opening verse for KJV.
- In scripture mode, a downward drag at the chapter top uncovers Home. An upward drag at the chapter bottom previews the next chapter or the next book in canonical KJV order. Revelation ends the sequence. Releasing short stays in place, and a committed turn uses the app’s existing chapter-loading path. Android device checks covered Home to Genesis 1 in one drag, Genesis 1 to Genesis 2, back to the saved Home position, and Jude to Revelation 1. Both debug builds launched and 199 app tests passed.
- Home starts with a full-width bar. The first scroll begins the change to an inset tinted-glass surface while the controls retain their size. The Genesis preview uses a translucent surface that becomes solid at the reader handoff. Android and iOS share the Compose drawing code. Both debug builds launched, the Home bar was visually checked on the Galaxy S23, and 118 desktop and 79 Android host tests passed.

- The Home Verse of the Day card opens its exact verse. A rightward swipe from its original chapter returns to the saved Home position. Once the user settles on a different chapter, normal paging resumes.

## Home verse refresh
- Pull down from the top of Home to request the next curated verse in the selected translation. The original card styling is preserved and the inline verse stays visible during the request and if it fails. The existing verse deep link stays available.
- Only explicit pulls request the next reference. Daily defaults and notification selection still use the date. This requires deploying the gateway change before store review. Book concepts remain in design review.

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
