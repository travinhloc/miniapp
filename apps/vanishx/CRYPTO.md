# VanishX crypto scheme

**Scheme version:** `1`  
**Do not invent Signal / X3DH here.**

## 1. Device identity (story 1.2)

| Purpose | Choice |
|---------|--------|
| Identity keypair | **Ed25519** via [Google Tink](https://developers.google.com/tink) (`KeyTemplates.get("ED25519")`) |
| Keyset at rest | Tink `AndroidKeysetManager` + Android Keystore master key (`android-keystore://vanishx_identity_master_key`) |
| Pref file | `vanishx_identity_prefs` (excluded from Auto Backup) |
| Anonymous User ID | `vx_` + first 22 chars of URL-safe Base64(SHA-256(Ed25519 public key bytes)) |

Private key never logged. No phone / email / advertising ID / `ANDROID_ID`.

## 2. Local database (story 1.3)

| Purpose | Choice |
|---------|--------|
| Engine | **SQLCipher** via `net.zetetic:sqlcipher-android` + Room |
| Passphrase | Random 32 bytes in EncryptedSharedPreferences (`vanishx_db_passphrase_prefs`) via AndroidX `MasterKeys` |
| DB file | `vanishx.db` (excluded from Auto Backup) |
| Schema | `meta`, `rooms` (+ `peerPub`), `messages` (+ `recalled`), `blocked_peers` |

Passphrase is **independent** of the Ed25519 identity keyset.

### Wipe hook (Panic — story 3.2)

`PanicWipeUseCase` / `LocalDatabaseWiper.wipe()`:

1. Close Room  
2. Delete DB (+ wal/shm)  
3. Clear passphrase prefs  
4. Clear Tink identity keyset prefs  
5. Clear pending invite + PIN store  
6. Bootstrap fresh identity  

App lock: Unlock PIN vs Panic PIN (`SecurityPinStore`). `FLAG_SECURE` on `MainActivity`.

## 3. Firebase mailbox (story 2.1)

See [`MAILBOX.md`](MAILBOX.md) — RTDB schema, rules, Anonymous Auth, remote data source.

## 4. Room invite (story 2.2)

| Purpose | Choice |
|---------|--------|
| Room id | 16 random bytes → URL-safe Base64, take 22 chars |
| Room key | 32 random bytes → URL-safe Base64 (E2EE in 2.3) |
| Invite URI | `vanishx://r/{roomId}?k={roomKey}&e={expiresAtMs}` |
| QR | ZXing encode + JourneyApps scan |
| TTL Free Host | Clock starts when guest **enters** · default 24h |
| TTL Pro Host | No room clock (chat forever) |

**Tradeoff:** room key is in the query string (capability URL). Convenient for QR/share; anyone with the link has the key. HTTPS App Links / key fragmentation can harden later (3.1).

Local `rooms` row stores `roomKey` + `role` (`creator` \| `member`) in SQLCipher.

## 5. Room message E2EE (story 2.3)

| Purpose | Choice |
|---------|--------|
| AEAD | **AES-256-GCM** via Tink `AesGcmJce` |
| Key | Invite `roomKey` (32 raw bytes, URL-safe Base64) |
| AAD | UTF-8 `roomId` |
| Wire | `vx1.` + URL-safe Base64(IV ‖ ciphertext ‖ tag) |
| Message TTL | Room clock when set; Pro Host uses long wire TTL |
| Receive | Sync on room open + RTDB listener while screen visible |
| After ingest | `remove()` remote node (transient mailbox) |

Plaintext never leaves the device. Free has **no recall**. Pro recall (4.2) = stub entitlement + delete mailbox node (IAP deferred).

## 6. Out of scope (later)

- Biometric unlock · RevenueCat / real IAP paywall

## 7. Block & Report (story 3.3)

| Purpose | Choice |
|---------|--------|
| Block key | Peer Ed25519 `publicKeyBase64` |
| Persist | SQLCipher `blocked_peers` (+ `rooms.peerPub` when known) |
| Side effect | Leave room (`status=left`), clear local messages, unsubscribe FCM topic |
| Join | Reject if remote `creatorPub` is blocked |
| Sync | Drop remote messages from blocked `senderPub` |
| Report | RTDB `/reports/{id}` write-once (`MAILBOX.md`) |

## 8. Pro recall stub (story 4.2)

| Purpose | Choice |
|---------|--------|
| Entitlement | Local stub prefs (`StubProEntitlementRepository`) — IAP later |
| Gate UI | Staging debug Home toggle · Room shows Recall only if Pro |
| Recall | Delete RTDB message if present · mark local `recalled` + clear body |
| Peer already synced | Best-effort only (no `recalls/` fan-out in MVP) |

## Code map

- `data/crypto/TinkIdentityKeyStore.kt` — identity  
- `data/crypto/RoomSecretsGenerator.kt` — room id/key  
- `data/crypto/RoomMessageCipher.kt` — AES-GCM room messages  
- `domain/model/InviteUriCodec.kt` — invite URI  
- `data/local/db/VanishxLocalDatabase.kt` — SQLCipher Room + wipe  
- `data/local/db/DatabasePassphraseStore.kt` — DB passphrase  
- `data/remote/FirebaseMailboxRemoteDataSource.kt` — RTDB mailbox  
- `data/billing/StubProEntitlementRepository.kt` — Pro stub  
- `domain/usecase/EnsureIdentityUseCase.kt` — identity bootstrap  
- `domain/usecase/CreateRoomUseCase.kt` / `JoinRoomUseCase.kt` — invite flow  
- `domain/usecase/SendRoomMessageUseCase.kt` / `SyncRoomMailboxUseCase.kt` — mũi tên  
- `domain/usecase/RecallRoomMessageUseCase.kt` — Pro recall  
