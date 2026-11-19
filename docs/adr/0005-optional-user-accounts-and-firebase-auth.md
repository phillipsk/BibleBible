# ADR 0005: Optional User Accounts, Firebase Auth REST Gateway, and Cross-Device Sync

- **Status**: Accepted
- **Date**: 2026-11-19
- **Context**: Release 3.1 Architecture

## Context
BibleBible readers frequently access Scripture across multiple devices (Android phones, iPhones, iPads, and macOS/Desktop). Without user accounts, bookmarks, reading history, saved chapter positions, and font preferences remain isolated on a single device. Furthermore, App Store Guideline 4.8 requires offering Sign in with Apple whenever any third-party social login (such as Google) is provided, and Guideline 5.1.1(v) strictly mandates complete in-app account and data deletion for any app offering account creation.

## Decisions

### 1. Optional ("Anonymous-First") User Experience
Reading the Bible, listening to chapter audio, viewing Biblical Shorts reels, and requesting AI summaries remain completely accessible without creating an account. Sign-in is strictly an opt-in capability that unlocks cross-device cloud synchronization and higher AI summary allowances.

### 2. Supported Auth Providers
- **Sign in with Apple**: Native `AuthenticationServices` sheet on iOS with SHA256 nonce; web OAuth on Android/Desktop. Meets App Store Guideline 4.8.
- **Sign in with Google**: Android Credential Manager (`androidx.credentials`), iOS `ASWebAuthenticationSession` with PKCE, and Desktop system browser loopback redirect.
- **Passwordless Email Magic Link**: Zero passwords to store, hash, or leak; sends deep-linkable URLs with Desktop code entry fallback.
- **Facebook Login**: Deferred to post-3.1 to avoid heavy SDK bloat and Meta review overhead for a scripture reading app.

### 3. KMP REST Architecture (No Desktop SDK Blockers)
Firebase's native mobile SDKs do not natively support Desktop JVM. To keep `commonMain` 100% unified across Android, iOS, and Desktop, client code invokes Firebase Authentication exclusively through its official **Identity Toolkit and Secure Token REST APIs** via Ktor.

### 4. First-Party Session Exchange & Authorization Tiering
Clients exchange verified Firebase ID tokens at `POST /v1/auth/session` on the Google Cloud Run gateway for a signed first-party HMAC-SHA256 session token (`sub: "user:<uid>"`, `tier: "user"`). The backend verifies the RS256 token against cached Google public certificates (`securetoken@system.gserviceaccount.com`). Anonymous clients continue receiving `tier: "anon"` tokens via `POST /v1/auth/token`.

### 5. Cross-Device Sync & Last-Write-Wins (LWW)
Sync records (`savedPosition`, `preferences`, `readingHistory`, `bookmarks`) are stored under `users/{uid}` in Google Cloud Firestore (Native mode). `PUT /v1/me/sync` enforces atomic Last-Write-Wins (LWW) per record key using client and server timestamps, capping reading history at 500 records.

### 6. App Store & GDPR Account Deletion Compliance
`DELETE /v1/me` wipes all Firestore user records and permanently deletes the Firebase Authentication user account in a single atomic flow, followed by local token eviction.

### 7. Zero Secret Leakage & Privacy Guardrails
The public Firebase Web API key is delivered dynamically to clients via `GET /v1/config`. Server-side credentials and HMAC secrets remain in Google Cloud Secret Manager. Telemetry (`AnalyticsTracker`) records lifecycle events (`auth_sign_in_started`, `auth_sync_completed`, etc.) without ever logging user IDs, emails, or PII.

## Consequences
- Readers enjoy effortless synchronization across Android, iOS, and Desktop.
- Zero dependencies on non-standard Desktop SDKs.
- Fully compliant with App Store and Google Play store review requirements.
