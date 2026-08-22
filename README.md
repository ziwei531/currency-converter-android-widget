# Currency Converter Widget

A small native Android home-screen widget built entirely in Termux.

## Features

- Fetches current reference rates for provider-supported fiat currencies
- Displays a cached value when offline
- Manual refresh button
- Configure one base currency and up to five target currencies
- Uses compact, standard, and expanded layouts so one or two rates stay large while a five-rate widget remains readable after resizing
- Keeps the same card inset across every responsive layout, so returning from configuration cannot visually remove the widget padding
- Opening the app saves defaults for new widgets; tapping an existing widget configures only that widget
- Tap the widget body or gear button to open configuration
- Rejects duplicate targets and base=target selections
- Stores configuration and cache separately for each widget instance
- Automatic inexact refresh every 30 minutes while the widget exists
- Refresh scheduling is restored after device reboot
- Larger widget typography for the title, rate, and timestamp
- Requests only `INTERNET` and `RECEIVE_BOOT_COMPLETED`
- Uses the no-key ExchangeRate-API Open Access endpoint
- Minimum Android API 26; target/compile API 33

## Build in Termux

```sh
cd /path/to/currency-converter-android-widget
./build.sh
```

Output:

```text
build/Currency-Converter-Widget.apk
```

The build uses Termux packages `aapt2`, `d8`, `apksigner`, and `zip`, plus the official Android 33 platform stub at `~/android-sdk/platforms/android-33/android.jar`.

## Install

Copy the APK to a location visible to Android Files, then open it and approve installation if Android asks. After installation, add **Currency Converter Widget** from the home-screen widget picker.

The APK is debug-signed for personal use, not Play Store distribution.

Android may delay background work under battery-saving policies, so the interval is not a hard real-time guarantee. Manual refresh remains immediate.
