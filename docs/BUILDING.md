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

Use `build.sh` for local and QA APKs. Keep the current feature `versionName` unchanged and increment only `VERSION_CODE` for every APK installed over the previous one:

```sh
VERSION_NAME=2.3.0 VERSION_CODE=41 ./build.sh
VERSION_NAME=2.3.0 VERSION_CODE=42 ./build.sh
```

Local and QA builds use the local debug certificate and produce artifacts such as:

```text
build/Currency-Converter-Widget-2.3.0.apk
```

Do not create patch or hotfix versions during QA. Those version changes are reserved for an explicit release-tag request.

## Low-level script

`build.sh` requires explicit `VERSION_NAME` and `VERSION_CODE` values. Use it directly for local and QA builds; use `release.sh` only for an explicitly requested production release.

Generated build output and signing keys are ignored and must not be committed.
