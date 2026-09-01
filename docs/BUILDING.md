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

## QA builds

Use the QA wrapper during development. Keep the feature version unchanged and increment the version code for every APK installed over the previous one:

```sh
QA_VERSION_NAME=2.3.0-qa.N QA_VERSION_CODE=<next-unused-code> ./build-qa.sh
```

QA builds use the local debug certificate and produce versioned artifacts such as:

```text
build/Currency-Converter-Widget-2.3.0-qa.3.apk
```

Do not create patch or hotfix versions during QA. QA fixes remain part of the same `2.3.0-qa.N` cycle.

## Low-level script

`build.sh` requires explicit `VERSION_NAME` and `VERSION_CODE` values. Use `build-qa.sh` for QA rather than invoking it directly.

Generated build output and signing keys are ignored and must not be committed.
