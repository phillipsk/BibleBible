# ADR 0001: First-Party Cloud Gateway and Short-Lived Session Tokens

- **Status**: Accepted (Introduced in PR #60 `feature/cloud-bible-api-and-token-gateway`)
- **Date**: 2026-11-16

## Context
Previously, mobile builds injected third-party API keys (`IQ_BIBLE_API_KEY`, `GEMINI_API_KEY`) via `BuildKonfig` with reversible XOR obfuscation. Reverse-engineering a shipped binary could recover third-party credentials, and RapidAPI introduced unnecessary latency and license exposure.

## Decision
1. Move all provider credentials (`GEMINI_API_KEY`, `CLIENT_TOKEN_SECRET`) exclusively into **Google Cloud Secret Manager** consumed by a first-party **Google Cloud Run** service (`biblebible-api`).
2. Serve public-domain Bible translations (`KJV`, `BSB`, `WEB`, `BBE`, `RV1909`, `SVD`) directly from the first-party API with immutable HTTP edge caching (`s-maxage=31536000, immutable`).
3. Bootstrap clients via `GET /v1/config` and `POST /v1/auth/token`, issuing short-lived HMAC-SHA256 bearer session tokens (`sessionToken`) verified on `POST /v1/gemini/summary`.
4. Keep `backend/` untracked in Git (`.gitignore`) and verify zero secret leakage in tracked files using `tools/verify_no_client_secrets.py`.

## Consequences
- Zero third-party secrets exist in Android, iOS, or Desktop client binaries.
- Server-side rate limiting (`GEMINI_MAX_RPM`, `GEMINI_MAX_DAILY_REQUESTS`) and response caching protect Gemini API spend.
- New features (like Biblical Shorts) can plug cleanly into the existing Cloud Run gateway and `CloudSessionManager` pattern.
