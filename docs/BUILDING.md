# Building

This project uses a small native Android build script rather than Gradle.

## Requirements

- Java Development Kit 8 or newer
- Android SDK platform `android-33` containing `android.jar`
- Android Asset Packaging Tool 2 (`aapt2`)
- D8
- APK Signer
- Zip

Set `ANDROID_SDK_ROOT` if the Android SDK is not under `~/android-sdk`. If the Android framework resource package is not at `/system/framework/framework-res.apk`, set `ANDROID_FRAMEWORK_RES`.

## Local and QA builds

Use `build.sh` for every local, testing, and QA APK. It uses the local **debug certificate** by default. Keep the current `versionName` unchanged and increment only `VERSION_CODE` for every APK installed over the previous one:

```sh
VERSION_NAME=2.3.0 VERSION_CODE=43 ./build.sh
VERSION_NAME=2.3.0 VERSION_CODE=44 ./build.sh
```

The artifact keeps the normal product filename:

```text
build/Currency-Converter-Widget-2.3.0.apk
```

A debug-signed QA APK cannot update an installed production-signed APK because Android requires matching signing certificates. Uninstall the production app before installing a debug QA APK, or restore the production release when testing an upgrade path.

Do not create patch or hotfix versions, or add a QA suffix, during QA. Those version changes are reserved for an explicit release-tag request.

## Low-level script

`build.sh` requires explicit `VERSION_NAME` and `VERSION_CODE` values. Use it directly for local and QA builds; use `release.sh` only for an explicitly requested production release.

Generated build output and signing keys are ignored and must not be committed.
