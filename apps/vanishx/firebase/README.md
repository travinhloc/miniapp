# VanishX Firebase Hosting (Epic 14)

Staging project: **`vanihx-staging`**

Canonical invite: `https://vanihx-staging.web.app/join?token=`

## Deploy App Links (story 14.1)

From this directory:

```bash
firebase deploy --only hosting
```

Serves:

- `https://vanihx-staging.web.app/.well-known/assetlinks.json`
- `/join?token=` landing (story 14.2): clipboard `VANISHX_INVITE` · Android → Play · iOS copy-only · desktop QR

`assetlinks.json` currently lists **debug** SHA-256 for `com.vault.vanishx.staging`. Add the **release** fingerprint before Play / productionDebug signed with the release keystore (R.2).

Landing tests:

```bash
node --test hosting/join/join-logic.test.cjs
```

Verify on a device after install:

```bash
adb shell pm get-app-links com.vault.vanishx.staging
```
