# IQ Bible v2 migration

Uses the IQ Bible **RapidAPI** subscription and `X-RapidAPI-Key` / `X-RapidAPI-Host`; the separate API.Bible credential is not used. Contract: https://api.iqbible.com/docs.md and https://rapidapi.com/vibrantmiami/api/iq-bible-v21.

## Configuration and live verification

The default `IQ_BIBLE_V2_HOST` is `iq-bible-v21.p.rapidapi.com`, confirmed from the v2 RapidAPI listing. It can be overridden with a Gradle property, environment variable, or local.properties value. Do not put a URL/path in this value. `IQ_BIBLE_API_KEY` accepts the existing local.properties value or an environment variable. Never commit either credential or generated BuildKonfig output.

Live verification succeeded after the managed secret's allowed domains included `iq-bible-v21.p.rapidapi.com`. The earlier 403 response was resolved without changing the API key or subscription. A managed environment value may be a placeholder resolved by the outbound proxy only for permitted hosts; do not embed that placeholder in a distributed app. Release builds must receive the real credential through their existing local/CI secret configuration.

Run `python3 scripts/check-iq-bible-v2.py` for nine read-only checks: catalog, Genesis 1 in all five translations, Genesis chapter metadata, and KJV/BBE daily verses. All passed with the configured credential. Each Genesis 1 response contained 31 verses; chapter metadata returned 50 chapters. The live catalog confirms `eng_kjv` (King James Version), `eng_bbe` (Bible in Basic English), `spa_rv` (Reina-Valera 1909), `arb_svd` (Arabic Van Dyck Bible), and `eng_asv` (American Standard Version 1901).

The RapidAPI proxy exposes paths directly under `/`, unlike the direct provider's documented `/api/v2` prefix. The client and smoke script use `/bibles/...`; `/api/v2/bibles` returned 404 through RapidAPI. The script never prints the credential.

## Behavior

- The selector contains KJV, BBE, Spanish RV1909, Arabic SVD, and ASV in that order. KJV remains the initial default. Saved supported choices are preserved; removed choices fall back to KJV.
- Reading requests use `/bibles/{version}/{USFM}/{chapter}` and translation-specific `/meta`. Book selection still opens chapter 1.
- V2 verse envelopes are mapped into the reader's existing model. Provider-local book numbers and row ids are not reused as canonical navigation ids. `v2:` cache keys keep old v1 data separate without destructive cache migration.
- The home grid scrolls as one feed: daily verse card, spacing, then the books. Loading and retry states leave book navigation available. Arabic verse text uses RTL layout.
- Daily verses are cached in memory per translation and UTC date. The home view checks the date once per minute, refreshing after the UTC day changes; failures and cancellations can be retried. Restarting the app may make a fresh request. There is no persistent offline daily-verse cache yet.

`desktopTest` covers the v2 request/response contract, mapping, error responses, cache expiry, translation fallback, repository integration, and the daily card. These deterministic tests cannot prove live subscription access or translation availability. iOS device execution still requires macOS/Xcode.

A suggested next feed card is a daily devotional using `/devotionals/today`. Keep it optional and clearly attributed; it is commentary, separate from the scripture card. This PR does not add that feature.
