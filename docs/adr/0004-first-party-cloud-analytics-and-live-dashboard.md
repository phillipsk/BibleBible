# ADR 0004: First-Party Cloud Analytics Collector & Live Web Dashboard

- **Status**: Accepted
- **Date**: 2026-11-19

## Context
BibleBible needed comprehensive, privacy-preserving analytics across the entire app lifecycle (startup, screen transitions, book/chapter reads, translation changes, font-size/zoom gestures, Verse of the Day, Biblical Shorts engagement, AI summaries, and errors) without adding third-party mobile tracking SDK bloat or risking PII leaks, while providing a visual dashboard to review usage immediately.

## Decision
1. **First-Party KMP Telemetry Engine (`AnalyticsTracker`)**:
   - Implemented in `composeApp/src/commonMain/kotlin/data/analytics/AnalyticsTracker.kt` using a lock-free coroutine `Channel` + bounded coalescing queue (750ms window, max 25 events per HTTP batch).
   - Automatically tags every event with platform (`android`, `ios`, `desktop` via `getPlatformName()`), a random per-launch `sessionId`, ISO timestamp, category, and non-PII properties.
2. **Cloud Run Ingestion, JSON Summary & Live HTML Dashboard (`backend/src/analyticsService.js`)**:
   - `POST /v1/analytics/events`: Ingests batched client events (`202 Accepted`), maintains fast aggregated KPI counters, top books, top translations, top Biblical Shorts, and recent event stream, and emits structured JSON entries to Google Cloud Logging (`component: "biblebible-analytics"`) in production.
   - `GET /v1/analytics/summary`: Serves a structured JSON report for programmatic inspection, alerts, or BigQuery/Looker export.
   - `GET /analytics` (`GET /v1/analytics/dashboard`): Serves a responsive, auto-refreshing live HTML dashboard showing KPI cards, bar charts (Platforms, Categories, Top Translations, Top Books, Top Shorts, Top Event Names), and a filterable Live Event Stream.
3. **Zero Added Cost Within Existing \$100/Month Budget**:
   - Reuses the existing Cloud Run service (`biblebible-api`) and Cloud Logging free tier (50 GiB/month included) at ~\$0.00 marginal monthly cost.

## Consequences
- Full-sweep app telemetry across Android, iOS, and Desktop without vendor lock-in or third-party SDK dependencies.
- Immediate human-readable dashboard at `<CLOUD_GATEWAY_URL>/analytics` plus machine-readable JSON at `<CLOUD_GATEWAY_URL>/v1/analytics/summary`.
- Note: Because Cloud Run instances are stateless across cold restarts, in-memory aggregates persist in `/tmp` per container instance while durable long-term event logs flow continuously into Google Cloud Logging (which can be routed to a BigQuery log sink anytime if multi-month historical SQL queries are needed).
