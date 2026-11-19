# AGENTS.md — BibleBible AI Agent Instructions

Permanent coding rules, verification commands, and workflow conventions for **Codex**, **Google Antigravity**, and human contributors working in this repository.

## 1. Persistent Context Workflow
Before starting development on any new task or conversation:
1. Review `AGENTS.md`, `README.md`, `docs/PROJECT_CONTEXT.md`, and `docs/HANDOFF.md`.
2. Verify that documented branch state, module boundaries, and file paths match the current codebase.
3. Read relevant records in `docs/adr/` before proposing architectural changes.

When completing a significant milestone or preparing to switch conversations:
1. Update `docs/HANDOFF.md` with completed work, outstanding issues, relevant files, verified test results, and next steps.
2. Update `docs/PROJECT_CONTEXT.md` only when meaningful architectural or technical decisions change (or add a new numbered ADR under `docs/adr/`).
3. Keep documentation concise and avoid duplicating information already obvious from source code.
4. Clearly distinguish verified results (commands actually run) from assumptions or untested changes.
5. Never overwrite unrelated decisions or existing documentation.

## 2. Architecture & Coding Conventions
- **Stack**: Kotlin Multiplatform (`2.1.21`), Compose Multiplatform (`1.7.3`), Ktor (`2.3.12`), SQLDelight (`2.3.2`), Kotlinx Serialization, Napier logging.
- **Targets**: `androidMain`, `iosMain`, `desktopMain`, and native SwiftUI wrapper `/iosApp`.
- **State Management**: Shared `@ThreadLocal` state objects (`BibleIQDataModel`, `BibleAPIDataModel`, `GeminiModel`, `CloudSessionManager`) paired with repository caches (`DailyVerseRepository`, `BiblicalShortsRepository`).
- **Platform Hardware Integration**: Keep UI declarative in `commonMain`, and isolate platform hardware/SDK boundaries behind `expect`/`actual` declarations (e.g., `DriverFactory`, `SetSystemBarColor`, `ShortsVideoPlayer`).
- **UI Testing Constraint**: Do **not** use `rememberInfiniteTransition()` for continuous idle animations in shared composables, as it keeps the `MonotonicFrameClock` permanently active and stalls Compose Multiplatform `runComposeUiTest`. Use coroutine-driven `LaunchedEffect` + `delay(...)` loops or explicit animation toggles when continuous motion is needed.

## 3. Security & Repository Boundaries
- **Zero Client Secrets**: Never embed or commit API keys, tokens, keystores, or `local.properties` values into Git or client `BuildKonfig`. Run `python3 tools/verify_no_client_secrets.py` before pushing.
- **Backend Separation**: `backend/` is intentionally listed in `.gitignore` and deployed directly to Google Cloud Run (`biblebible-api`). Do not remove `backend/` from `.gitignore` or commit cloud credentials.
- **Public-Domain Content**: The first-party Bible API and Biblical Shorts catalog use only public-domain translations (`KJV`, `BSB`, `WEB`, `BBE`, `RV1909`, `SVD`) and original devotional summaries/lessons.

## 4. Verification Commands
Run the following checks before marking work complete:
- **KMP Desktop & Android Host Unit Tests**:
  ```bash
  ./gradlew :composeApp:desktopTest :composeApp:testAndroidHostTest --console=plain --max-workers=2
  ```
- **Secret Leak Check**:
  ```bash
  python3 tools/verify_no_client_secrets.py
  ```
- **Cloud Backend Tests** (run inside `backend/`):
  ```bash
  cd backend && npm test
  ```
