# Fix Screen Capture Permission Crash

The application crashes with a `SecurityException` when attempting to register a screen capture observer on Android 14+ (API 34). This is because the `android.permission.DETECT_SCREEN_CAPTURE` permission is required for the `ScreenCaptureCallback` API but is not declared in the manifest.

## User Review Required

> [!IMPORTANT]
> The `android.permission.DETECT_SCREEN_CAPTURE` permission was introduced in Android 14. This change adds it to the manifest of the `vanishx` app.

## Proposed Changes

### apps/vanishx

#### [MODIFY] [AndroidManifest.xml](file:///Users/loctra/Workspace/01_Projects/Active/miniapp/apps/vanishx/src/main/AndroidManifest.xml)
- Add `<uses-permission android:name="android.permission.DETECT_SCREEN_CAPTURE" />` to the manifest.

## Verification Plan

### Automated Tests
- Build the project to ensure manifest merger works correctly.
- Since I cannot easily run UI tests that trigger screen capture in this environment, I will rely on manual verification by the user.

### Manual Verification
- Deploy the app to an Android 14+ device.
- Navigate to the Room screen (where `ScreenCaptureEffect` is used).
- Verify that the app no longer crashes.
