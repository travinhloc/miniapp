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
    expiresAt: number   // 0 = pending activate (Free) or forever (Pro Host); else absolute ms
    hostPro: boolean    // Pro Host → no room clock
    activatedAt: number? // set when guest first enters (activate)
    creatorPub: string  // optional base64 Ed25519 public key
    icebreaker: string? // optional · 1..80 chars (Epic 7.5)
  messages/{messageId}/
    ciphertext: string  // base64 — required
    senderPub: string   // base64
    createdAt: number
    expiresAt: number   // message TTL (must be > now on write)
  presence/{deviceId}/          // Epic 9 — no plaintext
    online: boolean
    updatedAt: number
  read/{deviceId}/              // read watermark
    messageId: string
    updatedAt: number
  typing/{deviceId}/            // ephemeral (~3s client TTL)
    at: number
  reactions/{messageId}/{deviceId}/
    emoji: string               // ≤8 chars
    at: number

/reports/{reportId}     // UGC (story 3.3) — client write-only
  roomId: string
  reporterPub: string
  peerPub: string?      // optional
  reason: string?       // optional ≤500
  createdAt: number
```

**Forbidden fields** (rules reject via `$other: false`): `plaintext`, `body`, `text`, `message`, or any other key.

`roomId` is a capability secret from the invite (MVP: knowing the path ⇒ can read while authenticated).

## Media (Epic 11)

RTDB remains a small encrypted envelope only (`MessagePlaintextCodec` v2). Ciphertext blobs use
Firebase Storage at `/rooms/{roomId}/att/{messageId}/{attId}` and are encrypted locally with
`RoomBlobCipher` before upload. The recipient downloads and decrypts the blob before removing the
mailbox envelope, preserving the existing pickup-queue behavior.

`deviceId` = Firebase-safe form of the device Ed25519 pubkey (`firebaseSafeKey`).

## Client API

- `MailboxRemoteDataSource` — write / read / delete message / **deleteAllMessages** / **writeReport** + write room meta
- Engagement (Epic 9): **setPresence** / **observePresence** · **setReadWatermark** / **observeReadWatermarks** · **setTyping** / **clearTyping** / **observeTyping** · **setReaction** / **clearReaction** / **observeReactions**
- `FirebaseMailboxRemoteDataSource` — RTDB implementation
- Expired room (4.1): `PurgeExpiredRoomUseCase` clears local SQLCipher messages + remote `messages/` node (meta kept)
- Home resume (4.1): `SyncActiveMailboxesUseCase` re-resolves TTL, purges expired, syncs active rooms
- **Pickup queue:** outbound ciphertext stays on RTDB until the **peer** downloads it (or TTL / recall). Sender sync must not delete own messages just because they exist locally.
- FCM (3.1): `firebase-messaging` · topic `vx_room_{roomId}` · notification → `vanishx://open/{roomId}`
- Pending invite (3.1 / 14.1 / 14.3): `PendingInviteStore` + URI capture; clipboard `VANISHX_INVITE` **once per cold start** if no App Link (`CaptureClipboardInviteUseCase`). Android 12+ paste toast accepted (E14-6). New invites are `https://{host}/join?token=` (opaque). `vanishx://r/…` still parses.
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

**Staging:** rules đã Publish (2026-08-09), gồm mailbox + engagement (`presence` / `read` / `typing` / `reactions`). Prod vẫn chờ Epic R / DoD.

## Media attachments (Epic 11)

RTDB `messages/*/ciphertext` may hold envelope **`v:2`** (metadata only). Blob bytes live on Firebase Storage:

```
/rooms/{roomId}/att/{messageId}/{attId}   // AES-GCM ciphertext (RoomBlobCipher)
```

See [`STORAGE.md`](STORAGE.md) · [`CRYPTO.md`](CRYPTO.md) · product spec `vanishx-media-spec-vi.md`.

## Out of scope

- WebRTC · HTTPS App Links · Cloud Functions fan-out · Caption (E11-5)
