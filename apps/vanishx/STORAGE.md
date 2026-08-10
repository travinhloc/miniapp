# VanishX Firebase Storage (Epic 11)

Opaque **encrypted** attachment blobs. Plaintext never uploaded.

## Bucket (staging)

From `src/staging/google-services.json`:

```
storage_bucket: vanihx-staging.firebasestorage.app
```

## First-time enable (required)

Nếu gửi media hiện lỗi **`Object does not exist at location`** → bucket Storage chưa được tạo / chưa Publish rules.

1. Firebase Console → project **`vanihx-staging`**
2. **Build → Storage → Get started** (tạo bucket mặc định nếu chưa có)
3. **Rules** → paste toàn bộ `firebase/storage.rules` → **Publish**
4. Confirm bucket name khớp `storage_bucket` trong `google-services.json` (thường `vanihx-staging.firebasestorage.app` hoặc `vanihx-staging.appspot.com`)
5. Reinstall staging app / cold start → thử attach lại

Deploy CLI (nếu đã link):

```bash
firebase deploy --only storage
```

## Path

```
/rooms/{roomId}/att/{messageId}/{attId}
```

## Rules

Source: `firebase/storage.rules`

- `auth != null` (Anonymous Auth — cùng mailbox)
- Write size &lt; 40 MB
- Same MVP capability model as RTDB (knowing `roomId`)

## Client

- Upload/download/delete: `FirebaseMediaStorageRemoteDataSource`
- Encrypt before upload: `RoomBlobCipher` (AAD = `roomId|attId`)
- Local cache: `context.noBackupFilesDir/media/…` via `LocalMediaStore`
