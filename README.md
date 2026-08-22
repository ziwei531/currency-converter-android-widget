# USD/MYR Rate Widget

A small native Android home-screen widget built entirely in Termux.

## Features

- Fetches the latest USD → MYR mid-market rate from Frankfurter
- Displays a cached value when offline
- Manual refresh button
- Android scheduled widget refresh hint: 30 minutes
- Only requests `INTERNET`
- Minimum Android API 26; target/compile API 33

## Build in Termux

```sh
cd ~/projects/usd-myr-widget
./build.sh
```

Output:

```text
build/USD-MYR-Rate.apk
```

The build uses Termux packages `aapt2`, `d8`, `apksigner`, and `zip`, plus the official Android 33 platform stub at `~/android-sdk/platforms/android-33/android.jar`.

## Install

Copy the APK to a location visible to Android Files, then open it and approve installation if Android asks. After installation, add **USD MYR Currency Converter** from the home-screen widget picker.

The APK is debug-signed for personal use, not Play Store distribution.
