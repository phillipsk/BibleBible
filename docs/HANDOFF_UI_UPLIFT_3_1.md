# Release 3.1 UI Uplift, Biblical Shorts Redesign & Feature Flags — Handoff

- **Branch**: `feature/ui-uplift-shorts-and-flags` → PR targeting `release/3.1`
- **Status**: All 5 Tiers implemented and verified

## Summary of Changes by Tier

### Tier 1 — Backend & Client Feature Flags (`GET /v1/config`)
- **Backend** (`backend/src/server.js`, `backend/scripts/deploy-gcp.sh`, `backend/test/server.test.js`):
  - Added `features: { biblicalShortsEnabled, verseOfTheDayEnabled }` to `GET /v1/config` driven by env vars `FEATURE_BIBLICAL_SHORTS` and `FEATURE_VERSE_OF_THE_DAY` (default `true`).
  - Toggle either flag to `false` on Cloud Run to hide the corresponding card on the Home screen without shipping an app update.
- **Client** (`CloudConfigRepository.kt`, `BibleHomeScreen.kt`):
  - Added `CloudFeatureFlagsDto` to `CloudRuntimeConfigDto` and exposed `CloudSessionManager.biblicalShortsEnabled` and `CloudSessionManager.verseOfTheDayEnabled`.
  - Conditionally renders `VerseOfTheDay` and `BiblicalShortsEntryCard` in `BibleHomeScreen.kt`.

### Tier 2 — Navigation, Chapter Transitions & Home Header
- **Home Header**: Changed `"Read the Bible"` → `"Study the Word"` using `MaterialTheme.typography.h1.fontFamily` (Kalam, matching `"BibleBible"` in `HomeTitle.kt`).
- **Chapter-to-Chapter Transition & Loading**:
  - Upgraded `LoadingScreen.kt` with a high-contrast warm gold (`#FBD38D`) spinner and centered status text (`"Loading <Book> <Chapter>…"`), eliminating the invisible blue-on-blue blank screen flash.
  - Wrapped `BibleScripturesPager.kt` page content in a `220ms` `Crossfade` transition.
- **Android Back Button**:
  - Added `PlatformBackHandler` (`expect`/`actual` → `androidx.activity.compose.BackHandler` on Android) wired into `BibleHomeScreen.kt`: collapses expanded bottom sheet → closes Biblical Shorts → returns from Reader to Home → allows normal Android exit on Home.

### Tier 3 — Biblical Shorts Redesign & Persistence
- **Default Sound Off**: `isMuted` defaults to `true` (`"Sound Off"` / `"Sound On"`).
- **Decluttered Canvas**:
  - Removed category chips from the top overlay (eliminating overlap with the video's `#1 | HOPE & RENEWAL` header).
  - Removed duplicate bottom badges, title, narrative summary, and `Key Lesson` box.
  - Moved the compact category chips row to the bottom bar above `Read Chapter` / `Next Short`.
- **Weekly Swipe Hint**:
  - Animated `"Swipe for next short"` pill appears on the 1st and 2nd short viewed, capped at 2 impressions per 7-day window (`ShortsPlaybackLogic` + `ShortsPreferencesStore` via SharedPreferences / NSUserDefaults / Java Preferences).
- **Randomized Order + Resume Position**:
  - `ShortsProgressManager` shuffles the 50-lesson catalog once per user, persists the deck order, and saves `lastShortId` so returning users resume right where they left off.

### Tier 4 — Home Screen & Button Visual Uplift
- **Verse of the Day Card**: Restyled with deep navy-slate gradient, subtle warm gold border (`#FBD38D`), warm gold eyebrow pill, and crisp typography matching `BiblicalShortsEntryCard`.
- **Book Grid Buttons**: Restyled with `RoundedCornerShape(14.dp)`, subtle translucent border (`Color(0x38FFFFFF)`), refined elevation, and balanced spacing.

## Deployment Note for Backend Feature Flags
`backend/` is intentionally untracked in Git. Deploy backend updates to Cloud Run via:
```bash
./backend/scripts/deploy-gcp.sh gen-lang-client-0150678391 us-central1
```
To toggle flags live on Cloud Run without redeploying code:
```bash
gcloud run services update biblebible-api --region us-central1 \
  --update-env-vars FEATURE_BIBLICAL_SHORTS=false,FEATURE_VERSE_OF_THE_DAY=true
```
