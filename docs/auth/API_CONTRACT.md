# BibleBible Accounts — Client ↔ Cloud Run API Contract (v1)

Identity provider: **Firebase Authentication** (project `gen-lang-client-0150678391`).
Clients talk to Firebase Auth via its **REST API** from `commonMain` (no Firebase native SDKs), then
exchange the Firebase ID token for a first-party `sessionToken` at the Cloud Run gateway.

Login is **optional**. Anonymous clients keep using `POST /v1/auth/token`.

## 1. Runtime config additions — `GET /v1/config`

New optional fields (absent/blank ⇒ auth UI hidden):

```json
{
  "auth": {
    "enabled": true,
    "firebaseApiKey": "AIza…",            // public Firebase Web API key (identifier, not a secret)
    "firebaseAuthDomain": "gen-lang-client-0150678391.firebaseapp.com",
    "providers": ["google", "apple", "email"],
    "googleWebClientId": "….apps.googleusercontent.com",   // Android Credential Manager serverClientId
    "googleIosClientId": "….apps.googleusercontent.com",   // iOS ASWebAuthenticationSession PKCE
    "googleDesktopClientId": "….apps.googleusercontent.com",// Desktop loopback PKCE (code exchanged by backend)
    "appleServicesId": "email.kevinphillips.biblebible.signin", // Apple web flow (Android/Desktop)
    "emailLinkContinueUrl": "https://<gateway>/v1/auth/email/complete"
  }
}
```

## 2. Firebase Auth REST calls (client-side, key = `firebaseApiKey`)

| Purpose | Endpoint |
|---|---|
| Google / Apple sign-in with provider ID token | `POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=KEY` body `{ "postBody": "id_token=<TOKEN>&providerId=google.com"  // or apple.com (+ "&nonce=<RAW_NONCE>" for Apple)
, "requestUri": "http://localhost", "returnSecureToken": true, "returnIdpCredential": true }` |
| Send email sign-in link | `POST …/v1/accounts:sendOobCode?key=KEY` body `{ "requestType": "EMAIL_SIGNIN", "email": "...", "continueUrl": "<emailLinkContinueUrl>?email=<urlenc>", "canHandleCodeInApp": true }` |
| Complete email link | `POST …/v1/accounts:signInWithEmailLink?key=KEY` body `{ "email": "...", "oobCode": "..." }` |
| Refresh ID token | `POST https://securetoken.googleapis.com/v1/token?key=KEY` form `grant_type=refresh_token&refresh_token=...` |

Firebase returns `idToken` (1h), `refreshToken`, `localId` (uid), `expiresIn`.

### Email link return path
The emailed link lands on `GET /v1/auth/email/complete?oobCode=…&email=…` (Cloud Run). That page
immediately tries `biblebible://auth/email?oobCode=…&email=…` (custom scheme, Android intent filter +
iOS `CFBundleURLTypes`) and shows a fallback **"Copy sign-in code"** (the `oobCode`) for Desktop users,
who paste it into the app's email sheet.

### Desktop Google sign-in
Desktop opens the system browser to Google's OAuth endpoint (`googleDesktopClientId`, PKCE S256,
redirect `http://127.0.0.1:<random-port>/callback`). On callback, Desktop sends
`POST /v1/auth/google/desktop-exchange` `{ "code", "codeVerifier", "redirectUri" }` → backend
exchanges with the desktop client secret from Secret Manager → returns `{ "idToken": "<google id_token>" }`.
Desktop then calls `signInWithIdp` as above.

## 3. Gateway endpoints

All responses JSON. Errors: `{ "error": "<message>", "code": "<machine_code>" }`.

### `POST /v1/auth/session`
- Header: `Authorization: Bearer <Firebase idToken>`
- Body: `{ "platform": "android" | "ios" | "desktop" }`
- 200:
```json
{
  "sessionToken": "<HS256 JWT, sub=user:<uid>, tier=user>",
  "tokenExpiresInSeconds": 3600,
  "user": {
    "uid": "abc", "email": "a@b.c", "displayName": "Ann", "photoUrl": null,
    "providers": ["google.com"], "createdAt": "2026-11-19T12:00:00Z"
  }
}
```
- 401 `invalid_id_token` / `revoked_id_token`; 503 `auth_unavailable` (Firebase not configured).

Clients call this after every Firebase sign-in **and** whenever the session token expires
(refresh Firebase ID token first, then re-exchange).

### `GET /v1/me` — Bearer user sessionToken → `{ "user": {…}, "preferences": {…} }`

### `GET /v1/me/sync` — Bearer user sessionToken
```json
{ "serverTime": "2026-11-19T12:00:00Z",
  "records": {
    "savedPosition":  { "value": { "versionId": "KJV", "bookId": 43, "chapter": 3 }, "updatedAt": 1763553600000 },
    "preferences":    { "value": { "fontSize": 18, "theme": "dark", "versionId": "KJV" }, "updatedAt": 1763553600000 },
    "readingHistory": { "value": [ { "bookId": 43, "chapter": 3, "versionId": "KJV", "readAt": 1763553600000 } ], "updatedAt": 1763553600000 }
  } }
```

### `PUT /v1/me/sync` — Bearer user sessionToken
Body: `{ "records": { "<key>": { "value": <json>, "updatedAt": <epochMillis> } } }`
Server applies **last-write-wins per record key** (keeps whichever `updatedAt` is larger), returns the
same shape as `GET`. Allowed keys: `savedPosition`, `preferences`, `readingHistory`, `bookmarks`.
`readingHistory` is capped at 500 entries; body max 256 KB.

### `DELETE /v1/me` — Bearer user sessionToken
Deletes `users/{uid}` (and subcollections) and the Firebase Auth user. 204. Client then signs out locally.

### Tiers
`sessionToken` payload gains `tier: "anon" | "user"` and, for users, `uid`. Gemini rate limits are
tier-aware (signed-in users get a higher daily allowance). `/v1/me*` require `tier == "user"` (401 `user_session_required`).

## 4. Client storage rules
- Firebase `refreshToken` → platform secure storage only (Android EncryptedSharedPreferences/Keystore,
  iOS Keychain, Desktop: OS keychain or AES-encrypted file in app data dir). Never in SQLDelight, logs, or analytics.
- `uid`/email never sent to analytics. Auth analytics events: `auth_sign_in_started|succeeded|failed`
  (`provider`, `reason`), `auth_signed_out`, `auth_account_deleted`.
