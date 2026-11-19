# Privacy Policy for BibleBible

**Last updated: November 19, 2026**

Thank you for choosing **BibleBible** ("we", "us", or "our"). We are deeply committed to protecting your privacy and ensuring you have an uplifting, distraction-free scripture reading experience. This Privacy Policy explains what information our application processes, how it is used, and how your privacy is protected when you use the BibleBible mobile and desktop applications.

---

## 1. Zero Personal Data Collection (No PII)

BibleBible is designed from the ground up to respect your privacy:
- **No Account Required**: You can read scripture, listen to audio chapters, watch Biblical Shorts, and generate AI chapter summaries without creating an account or providing personal details.
- **No Personal Identifiers**: We do not collect, request, or store your name, email address, phone number, physical address, contacts, photos, or biometrics.
- **No Advertising or Cross-App Tracking**: We do not collect or access device advertising identifiers (such as Apple's IDFA or Google's GAID/AAID). We do not track you across other applications or websites, and we never participate in advertising data networks.

---

## 2. Anonymous Operational Analytics & Telemetry

To ensure app stability, detect crashes, optimize media streaming, and understand which features provide the most value to readers, BibleBible includes a lightweight, first-party operational telemetry engine:
- **What is Collected**:
  - Feature interactions (e.g., book/chapter selections, Bible version toggles, Biblical Shorts views and completion rates, search queries, and audio playback events).
  - Diagnostic and performance metrics (e.g., AI summary request latencies, network errors, and app startup lifecycle events).
  - Device platform family (e.g., iOS, Android, or Desktop) and operating system version.
- **How Telemetry is Identified**:
  - Telemetry is associated strictly with an **ephemeral session identifier** randomly generated in device memory on each app launch.
  - This identifier resets when the application is terminated and is never linked to your identity, IP address profile, or hardware serial numbers.
- **First-Party Processing**:
  - All telemetry is transmitted over encrypted HTTPS (TLS 1.3) directly to our own Google Cloud infrastructure (`biblebible-api`). Telemetry is never forwarded or sold to third-party analytics providers.

---

## 3. Artificial Intelligence (AI) Summarization

When you request an AI chapter summary, the application securely contacts our Cloud Gateway:
- **Scripture Content Only**: Only the requested scripture chapter reference and standard public-domain scripture text are transmitted to our Google Cloud Gemini service to generate the theological summary.
- **No User Profiling**: AI prompts do not contain user personal information, device identifiers, or reading history.
- **No Training on Private User Data**: Summaries are cached server-side to minimize redundant requests and compute resources.

---

## 4. Media Streaming (Biblical Shorts)

When you view Biblical Shorts video reels:
- Video media assets (H.264/AAC MP4 files) are streamed directly from Google Cloud Storage via secure HTTPS.
- Streaming requests utilize standard HTTP Range headers for smooth, buffer-free playback.
- No personal user data or watching history is permanently linked to your personal identity.

---

## 5. Local Device Storage

BibleBible stores your preferences locally on your device using encrypted, sandboxed storage:
- Stored locally: your selected Bible version, font size, theme preference, last-read chapter bookmark, and local reading history.
- This data remains on your physical device and is never automatically uploaded to any remote server without your explicit action.

---

## 6. Information Sharing & Third Parties

We do not sell, rent, monetize, or trade your data under any circumstances. We only interact with trusted cloud service providers (Google Cloud Platform) solely to host our backend APIs, stream media, and run AI inference under strict enterprise data protection agreements.

---

## 7. Children's Privacy

BibleBible does not address anyone under the age of 13 for data collection purposes. We do not knowingly collect personal information from children or adults. If you have reason to believe that any personal information has been transmitted to us, please contact us immediately.

---

## 8. App Store Disclosures

### Apple App Store (Privacy Nutrition Labels)
- **Data Used to Track You**: None (0%).
- **Data Linked to You**: None (0%).
- **Data Not Linked to You**:
  - **Usage Data**: Product Interaction (feature usage, video reel views).
  - **Diagnostics**: Performance Data & Error Diagnostics.

### Google Play Store (Data Safety)
- **Data Collected**: App activity (App interactions) & App info/performance (Diagnostics).
- **Data Sharing**: No data shared with third parties.
- **Security Practices**: Data is encrypted in transit over HTTPS (TLS 1.3). No personal account required.

---

## 9. Changes to this Privacy Policy

We may update our Privacy Policy periodically to reflect app updates or regulatory enhancements. Any changes will be published on this page with an updated "Last updated" date.

---

## 10. Contact Us

If you have questions, feedback, or concerns regarding this Privacy Policy, please reach out to:
- **Email**: support@biblebible.app
- **Repository**: https://github.com/phillipsk/BibleBible
