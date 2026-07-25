# VanishX crypto scheme (story 1.2)

**Scheme version:** `1`  
**Scope:** Device identity bootstrap only. Room message E2EE / Firebase mailbox arrive in later stories.  
**Do not invent Signal / X3DH here.**

## Algorithms

| Purpose | Choice |
|---------|--------|
| Identity keypair | **Ed25519** via [Google Tink](https://developers.google.com/tink) (`KeyTemplates.get("ED25519")`) |
| Keyset at rest | Tink `AndroidKeysetManager` encrypted with Android Keystore master key (`android-keystore://vanishx_identity_master_key`) |
| Pref file | `vanishx_identity_prefs` (excluded from Auto Backup) |
| Anonymous User ID | `vx_` + first 22 chars of URL-safe Base64(SHA-256(Ed25519 public key bytes)) |

## What is stored

- **Private key:** only inside Tink encrypted keyset (Keystore-wrapped). Never logged.
- **Public key:** derived for display / future QR share (Base64).
- **Anonymous ID:** deterministic from public key — no phone, email, advertising ID, or `ANDROID_ID`.

## Idempotency

First app open creates the keyset. Later opens load the same keyset → same anonymous ID.

## Out of scope (later stories)

- SQLCipher DB password (1.3)
- Room keys / mailbox ciphertext (2.x)
- Panic wipe clearing this keyset (post-MVP)

## Code map

- `data/crypto/TinkIdentityKeyStore.kt` — generate / load
- `data/crypto/AnonymousIdDeriver.kt` — ID derivation
- `domain/usecase/EnsureIdentityUseCase.kt` — app entry bootstrap
