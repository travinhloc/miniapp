# VanishX Firebase (Hosting + Functions)

Staging project: **`vanihx-staging`**

Canonical invite: `https://vanihx-staging.web.app/join?token=`

## Cloud Functions (Epic 15 Done)

Requires **Blaze**. From this directory:

```bash
npm --prefix functions test
firebase deploy --only functions,database
```

- `onMailboxMessageCreated` — data-only FCM to `vx_room_{roomId}` (`type=message`)
- `onRoomSignalCreated` — Ping fan-out (`type=ping`) then delete the signal node
- Payload builder: `functions/push-payload.js` (no ciphertext)
- Runtime: **Node 22** · RTDB triggers **1st gen** (`require("firebase-functions/v1")`) — tránh Eventarc IAM của 2nd gen

Prod Functions wait on **R.1**. Client never holds an FCM server key.

## Deploy App Links (story 14.1)

From this directory:

```bash
firebase deploy --only hosting
```

Serves:

- `https://vanihx-staging.web.app/.well-known/assetlinks.json`
- `/join?token=` landing (story 14.2): clipboard `VANISHX_INVITE` · Android opens installed app (`intent://` + staging/prod package) · Play only if prod host and page still visible · iOS copy-only · desktop QR

`assetlinks.json` currently lists **debug** SHA-256 for `com.vault.vanishx.staging`. Add the **release** fingerprint before Play / productionDebug signed with the release keystore (R.2).

Landing tests:

```bash
node --test hosting/join/join-logic.test.cjs
```

Verify on a device after install:

```bash
adb shell pm get-app-links com.vault.vanishx.staging
```
