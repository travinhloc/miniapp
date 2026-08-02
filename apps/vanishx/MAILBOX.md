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
    icebreaker: string? // optional · 1..80 chars (Epic 7.5)
  messages/{messageId}/
    ciphertext: string  // base64 — required
    senderPub: string   // base64
    createdAt: number
    expiresAt: number   // message TTL (must be > now on write)

/reports/{reportId}     // UGC (story 3.3) — client write-only
  roomId: string
  reporterPub: string
  peerPub: string?      // optional
  reason: string?       // optional ≤500
  createdAt: number
```

**Forbidden fields** (rules reject via `$other: false`): `plaintext`, `body`, `text`, `message`, or any other key.

`roomId` is a capability secret from the invite (MVP: knowing the path ⇒ can read while authenticated).

## Client API

- `MailboxRemoteDataSource` — write / read / delete message / **deleteAllMessages** / **writeReport** + write room meta
- `FirebaseMailboxRemoteDataSource` — RTDB implementation
- Expired room (4.1): `PurgeExpiredRoomUseCase` clears local SQLCipher messages + remote `messages/` node (meta kept)
- Home resume (4.1): `SyncActiveMailboxesUseCase` re-resolves TTL, purges expired, syncs active rooms
- FCM (3.1): `firebase-messaging` · topic `vx_room_{roomId}` · notification → `vanishx://open/{roomId}`
- Pending invite (3.1): `PendingInviteStore` + `ConsumePendingInviteUseCase` after identity bootstrap
- Block / Report (3.3): local `blocked_peers` by peer pubkey · leave room · RTDB `/reports`
- Pro recall (4.2): stub Pro (staging debug) · `RecallRoomMessageUseCase` deletes RTDB message if still present · local `recalled` flag (IAP deferred)

## `google-services.json`

| Flavor | Path | Notes |
|--------|------|--------|
| staging (dev) | `src/staging/google-services.json` | Real `vanihx-staging`; **gitignored** |
| staging (CI) | `src/staging/google-services.placeholder.json` | Committed; copied to `google-services.json` if missing |
| production | `src/production/google-services.json` | Placeholder until prod Firebase exists |

Dev: file thật phải có `project_info.firebase_url`. Tạo RTDB rồi tải lại JSON vào `src/staging/`.

CI: không có file thật → Gradle copy từ `*.placeholder.json` (đủ cho `process*GoogleServices`).

## Deploy rules

1. Firebase Console → Realtime Database → Rules, paste `firebase/database.rules.json`, **Publish**  
   **or** `firebase deploy --only database` if the Firebase CLI is linked to this project.
2. Enable **Anonymous** sign-in under Authentication.
3. Run `./gradlew :apps:vanishx:installStagingDebug`, create/join a room, send a message.

## Out of scope

- WebRTC · Panic/IAP · polish UI · HTTPS App Links · Cloud Functions fan-out
