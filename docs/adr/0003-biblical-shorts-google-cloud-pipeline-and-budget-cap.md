# ADR 0003: Biblical Shorts Google Cloud Pipeline, Streaming Architecture, and <\$100/Month Cost Cap

- **Status**: Accepted
- **Date**: 2026-11-19

## Context
BibleBible needs a reliable, professional backend to source, generate, store, and stream **50 Biblical Shorts** (<30 seconds each) highlighting foundational Bible narratives and practical life lessons, while enforcing a strict **total Google Cloud spending cap under \$100/month**.

## Decision

### 1. Content & AI Generation Pipeline
1. **Top 50 Narrative & Lesson Catalog (`shortsCatalog.js`)**:
   - Curates 50 canonical Old and New Testament stories across 5 categories (`Faith & Courage`, `Grace & Forgiveness`, `Wisdom & Peace`, `Miracles of Jesus`, `Hope & Renewal`), each strictly 20–28 seconds (`< 30s`), tied to a public-domain scripture reference (`bookId`, `chapter`, `verse`), narrative summary, actionable life lesson, and 3 timed scene captions.
2. **Pre-Generated Video Assets (Never Live-Generated Per User)**:
   - Generating AI video on demand per user request would blow through a \$100/mo budget within hours. Instead, all 50 shorts are generated **once offline/batch** and served statically.
   - **Baseline Pipeline (`backend/scripts/generate-shorts-videos.mjs`)**: Uses Gemini Flash scripting + FFmpeg (`720x1280` 9:16 H.264 `yuv420p` + AAC stereo ambient chord audio + `-movflags +faststart` for instant mobile playback start).
   - **Optional AI Enhancement Upgrade**:
     - Adding Google Cloud Text-to-Speech (Neural2/WaveNet narration) + Imagen 3 keyframe art for all 50 clips costs ~\$4.52 total one-time.
     - Selective Veo video generation (if desired later for hero clips) should be capped at <=10 clips/month so generation never threatens the \$100/mo ceiling.

### 2. Hosting & Reliable Mobile Streaming
- **Metadata API**: Cloud Run (`biblebible-api`) serves `GET /GetBiblicalShorts` and `GET /v1/shorts` (with optional `category`, `bookId`, `limit` filters) with `Cache-Control: public, max-age=3600, s-maxage=86400`.
- **Media Streaming**:
  - Supports **Google Cloud Storage + Cloud CDN** via `SHORTS_CDN_BASE_URL` (e.g. `https://storage.googleapis.com/<bucket>/shorts`) for production scale.
  - Also serves `GET /v1/shorts/media/:fileName` directly from Cloud Run with full HTTP **`Range: bytes=start-end` (`206 Partial Content`)** support, `Accept-Ranges: bytes`, `Content-Type: video/mp4`, and `Cache-Control: public, max-age=604800, s-maxage=2592000, immutable` so Android `MediaPlayer` and iOS `AVPlayer` seek and buffer reliably out of the box.

### 3. Monthly Cost Estimate & Hard Guardrails (<\$100/mo Cap)
Assumptions: 50 vertical 9:16 MP4 shorts (~3 MB each = ~150 MB total corpus), H.264 + AAC `-movflags +faststart`, Cloud Run `min-instances=0`, `max-instances=2`.

| Cost Component | Usage Assumption | Estimated Cost |
| :--- | :--- | :--- |
| **One-Time Batch Creation (50 shorts)** | FFmpeg local/Cloud Build synthesis (+\$0.02 Gemini script, optional ~\$4.50 Imagen 3 + Cloud TTS) | **\$0.00 – \$4.52 one-time** |
| **Cloud Storage (`Standard`, `us-central1`)** | ~150 MB media + backups (within 5 GB Always Free tier) | **~\$0.003 / month** |
| **Cloud Run Compute (`biblebible-api`)** | Scale-to-zero (`min-instances=0`), max 2 instances, 256Mi | **\$0.00 – \$1.50 / month** |
| **Media Egress + CDN Caching** | 1,000 MAU (~20 GB/mo) to 10,000 MAU (~200 GB/mo @ ~\$0.08–\$0.12/GB) | **\$2.40 – \$24.00 / month** |
| **Gemini Chapter Summaries** | `gemini-3.5-flash` capped at `GEMINI_MAX_DAILY_REQUESTS=500` + response cache | **\$1.50 – \$4.00 / month** |
| **Total Monthly Recurring Spend** | **1K – 10K Monthly Active Users** | **\$4.00 – \$30.00 / month** |

**Budget Safety Margin**: **\$70–\$96/month headroom** below the **\$100/month cap**.
Hard technical guardrails:
1. `backend/scripts/setup-budget-cap.sh` provisions a **\$100.00/month Cloud Billing Budget** with alerts at 50% (\$50), 80% (\$80), and 95% (\$95).
2. Cloud Run stays capped at `--max-instances=2` and `--min-instances=0` to prevent runaway instance scaling.
3. Gemini Gateway keeps `GEMINI_MAX_RPM=20` and `GEMINI_MAX_DAILY_REQUESTS=500`.

## Consequences
- Predictable monthly spend far under \$100/month with zero per-user AI video generation cost risk.
- Immediate zero-cold-dependency local and Cloud Run streaming via `206 Partial Content`, ready to offload to a public GCS bucket/CDN at any time via `SHORTS_CDN_BASE_URL`.
