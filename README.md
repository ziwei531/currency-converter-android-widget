# Currency Converter Widget

A small native Android home-screen widget for comparing exchange rates across independent widget instances.

## Features

- Configure one base currency and up to five target currencies per widget
- Add multiple independent widget instances with different base currencies
- Search currencies by code or name
- Display full currency names such as `Malaysian Ringgit` and `British Pound`
- Show larger typography across single, compact, standard, and expanded layouts
- Show a visible `Refreshing...` state during manual and automatic refreshes
- Display cached values when offline
- Support light and dark widget palettes through Android night resources
- Use a compact single-currency layout without sacrificing readable typography
- Support horizontal and vertical resizing with explicit minimum resize bounds
- Keep launcher-owned background geometry separate from content insets
- Open configuration from the widget body or settings button
- Reject duplicate targets and base-equals-target selections
- Store configuration and cached rates separately for each widget instance
- Refresh automatically on an inexact 30-minute cadence while widgets exist
- Restore refresh scheduling after device reboot
- Request only `INTERNET` and `RECEIVE_BOOT_COMPLETED`
- Use the no-key ExchangeRate-API Open Access endpoint
- Support Android API 26 and newer; target and compile against API 33

## Build

The project uses a small native Android build script. The required tools are:

- Java Development Kit 8 or newer
- Android SDK platform `android-33` containing `android.jar`
- Android Asset Packaging Tool 2 (`aapt2`)
- D8
- APK Signer
- Zip

Set `ANDROID_SDK_ROOT` or update the SDK path in `build.sh` if the Android SDK is not under `~/android-sdk`.

If the Android framework resource package is not at `/system/framework/framework-res.apk`, set `ANDROID_FRAMEWORK_RES` to its location before building.

```sh
./build.sh
```

The signed APK is written to:

```text
build/Currency-Converter-Widget.apk
```

The build also verifies the APK signature. Generated build output and signing keys are intentionally ignored by Git.

## Termux build notes

This project was originally built and tested from native Termux on Android. On Termux, install the required command-line tools using the platform's package manager, keep the official Android platform stub outside the repository, and run the same `./build.sh` command.

The repository does not require Termux at runtime or as a product dependency; these notes only document one supported native build environment.

## Install

Download the APK from the [published releases](../../releases), copy it to a location visible to Android Files, open it, and approve installation if Android asks. After installation, add **Currency Converter Widget** from the home-screen widget picker.

### Agent installation guide

```sh
# Run from the repository root with an Android device connected through adb.
gh release download v1.0.0 --pattern 'Currency-Converter-Widget.apk'
adb install -r Currency-Converter-Widget.apk
```

## Refresh behavior

Android may delay background work under battery-saving policies, so the 30-minute interval is an intended cadence rather than a hard real-time guarantee. Manual refresh remains available from each widget.
