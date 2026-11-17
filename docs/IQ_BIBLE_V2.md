# IQ Bible v2 migration

Uses the IQ Bible **RapidAPI** subscription and `X-RapidAPI-Key` / `X-RapidAPI-Host`; the separate API.Bible credential is not used. Contract: https://api.iqbible.com/docs.md and https://rapidapi.com/vibrantmiami/api/iq-bible-v21.

## Configuration and live verification — required before merge

The default `IQ_BIBLE_V2_HOST` is `iq-bible-v21.p.rapidapi.com`, confirmed from the v2 RapidAPI listing. It can be overridden with a Gradle property, environment variable, or local.properties value. Do not put a URL/path in this value. `IQ_BIBLE_API_KEY` accepts the existing local.properties value or an environment variable. Never commit either credential or generated BuildKonfig output.

Network access is now working. The existing credential successfully fetched Genesis 1 in BBE from v1 (HTTP 200, 31 verses). An authenticated request to the confirmed v2 catalog endpoint returned HTTP 403 with `{"message":"You are not subscribed to this API."}`. Enable a subscription to the separate IQ Bible v2 RapidAPI listing for this key's application before retrying. The live v2 catalog and passage responses remain unverified, so this migration stays draft.

After enabling v2 subscription access, run `python3 scripts/check-iq-bible-v2.py`. It makes nine read-only requests to check the catalog, Genesis 1 in all five translations, chapter metadata, and KJV/BBE daily verses. A RapidAPI v2 subscription may be required separately from v1. The check never prints the credential.

Configured remote ids are `eng_kjv`, `eng_bbe`, `spa_rv`, `arb_svd`, `eng_asv`. In particular, confirm `eng_bbe` in the live catalog and confirm that `spa_rv` is the intended Reina-Valera edition before merging; do not silently replace BBE or relabel a different Spanish edition as RV1909.

## Behavior

- The selector contains KJV, BBE, Spanish RV1909, Arabic SVD, and ASV in that order. KJV remains the initial default. Saved supported choices are preserved; removed choices fall back to KJV.
- Reading requests use `/api/v2/bibles/{version}/{USFM}/{chapter}` and translation-specific `/meta`. Book selection still opens chapter 1.
- V2 verse envelopes are mapped into the reader's existing model. Provider-local book numbers and row ids are not reused as canonical navigation ids. `v2:` cache keys keep old v1 data separate without destructive cache migration.
- The home grid scrolls as one feed: daily verse card, spacing, then the books. Loading and retry states leave book navigation available. Arabic verse text uses RTL layout.
- Daily verses are cached in memory per translation and UTC date. The home view checks the date once per minute, refreshing after the UTC day changes; failures and cancellations can be retried. Restarting the app may make a fresh request. There is no persistent offline daily-verse cache yet.

`desktopTest` covers the v2 request/response contract, mapping, error responses, cache expiry, translation fallback, repository integration, and the daily card. These deterministic tests cannot prove live subscription access or translation availability. iOS device execution still requires macOS/Xcode.

A suggested next feed card is a daily devotional using `/api/v2/devotionals/today`. Keep it optional and clearly attributed; it is commentary, separate from the scripture card. This PR does not add that feature.
