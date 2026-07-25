# VanishX Firebase mailbox (story 2.1)

Transient ciphertext relay on **Firebase Realtime Database**. Plaintext never leaves the device.

**Ciphertext max:** `16384` UTF-16 code units (≈16 KB) — enforced in `firebase/database.rules.json`.

## Auth

- **Firebase Anonymous Auth** required for all mailbox read/write (`auth != null`).
- Device Ed25519 identity (`CRYPTO.md`) is separate from Firebase UID.

## Schema

```
/rooms/{roomId}
  meta/
    createdAt: number   // epoch ms
    expiresAt: number   // room TTL (must be > now on write)
    creatorPub: string  // optional base64 Ed25519 public key
  messages/{messageId}/
    ciphertext: string  // base64 — required
    senderPub: string   // base64
    createdAt: number
    expiresAt: number   // message TTL (must be > now on write)
```

**Forbidden fields** (rules reject via `$other: false`): `plaintext`, `body`, `text`, `message`, or any other key.

`roomId` is a capability secret from the invite (MVP: knowing the path ⇒ can read while authenticated).

## Client API

- `MailboxRemoteDataSource` — write / read / delete message + write room meta
- `FirebaseMailboxRemoteDataSource` — RTDB implementation
- Staging debug: Home → **RTDB smoke** writes `dGVzdA==`, reads back, deletes

## `google-services.json`

| Flavor | Path | Notes |
|--------|------|--------|
| staging | `src/staging/google-services.json` | Real project (`vanihx-staging`); **gitignored** |
| production | `src/production/google-services.json` | Placeholder until prod Firebase exists |

File **phải có** `project_info.firebase_url` (URL Realtime Database). Nếu thiếu: tạo RTDB trên Console rồi **tải lại** `google-services.json` thay file cũ.

## Deploy rules

1. Firebase Console → Realtime Database → Rules, paste `firebase/database.rules.json`, **Publish**  
   **or** `firebase deploy --only database` if the Firebase CLI is linked to this project.
2. Enable **Anonymous** sign-in under Authentication.
3. Run `./gradlew :apps:vanishx:installStagingDebug`, open app, tap **RTDB smoke**.

## Out of scope

- WebRTC · FCM (3.1) · E2EE payload format (2.3) · create/join UI (2.2)
