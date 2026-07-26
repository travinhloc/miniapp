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
| Schema | `meta`, `rooms`, `messages` |

Passphrase is **independent** of the Ed25519 identity keyset.

### Wipe hook (Panic later)

`LocalDatabaseWiper.wipe()` / `VanishxLocalDatabase.wipe()`:

1. Close Room  
2. Delete DB (+ wal/shm)  
3. Clear passphrase prefs  

Identity keyset wipe stays for story 3.2.

## 3. Firebase mailbox (story 2.1)

See [`MAILBOX.md`](MAILBOX.md) — RTDB schema, rules, Anonymous Auth, remote data source.

## 4. Room invite (story 2.2)

| Purpose | Choice |
|---------|--------|
| Room id | 16 random bytes → URL-safe Base64, take 22 chars |
| Room key | 32 random bytes → URL-safe Base64 (E2EE in 2.3) |
| Invite URI | `vanishx://r/{roomId}?k={roomKey}&e={expiresAtMs}` |
| QR | ZXing encode + JourneyApps scan |
| TTL Free | 1h / 6h / 24h / 7d |

**Tradeoff:** room key is in the query string (capability URL). Convenient for QR/share; anyone with the link has the key. HTTPS App Links / key fragmentation can harden later (3.1).

Local `rooms` row stores `roomKey` + `role` (`creator` \| `member`) in SQLCipher.

## 5. Out of scope (later)

- Room message E2EE payload (2.3)
- App lock / FLAG_SECURE / Panic UI (3.2+)

## Code map

- `data/crypto/TinkIdentityKeyStore.kt` — identity  
- `data/crypto/RoomSecretsGenerator.kt` — room id/key  
- `domain/model/InviteUriCodec.kt` — invite URI  
- `data/local/db/VanishxLocalDatabase.kt` — SQLCipher Room + wipe  
- `data/local/db/DatabasePassphraseStore.kt` — DB passphrase  
- `data/remote/FirebaseMailboxRemoteDataSource.kt` — RTDB mailbox  
- `domain/usecase/EnsureIdentityUseCase.kt` — identity bootstrap  
- `domain/usecase/CreateRoomUseCase.kt` / `JoinRoomUseCase.kt` — invite flow  
