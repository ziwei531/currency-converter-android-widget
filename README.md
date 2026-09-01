# Currency Converter Widget

A small native Android home-screen widget for showing up to fifteen independent currency conversion pairs in one widget instance.

## Features

- Add, edit, remove, and reorder up to fifteen conversion pairs
- Choose the base currency first and the target currency second for every new conversion
- Mix different base currencies in the same widget
- Search currencies by name or code
- Tap the widget to configure it, tap an individual conversion row to edit that pair, tap the graph button to open Google's chart search for that pair, or tap the refresh button to update rates
- Configure either the no-key daily ExchangeRate-API feed or the more frequently updated fxRatesAPI feed
- Store an fxRatesAPI key locally using Android Keystore-backed encryption
- See the latest rate, update time, and cached values when offline
- Follow the system's light or dark mode and resize the widget on the home screen

## How it works

Each widget stores up to fifteen ordered pairs, such as:

- US Dollar → Malaysian Ringgit
- British Pound → Euro
- Singapore Dollar → Japanese Yen

The configuration screen keeps each pair independent, so a second base currency does not require another widget instance. Existing configurations are migrated automatically: the old one-base/multiple-target format becomes multiple pairs that share the saved base. Rates are cached using both currencies, preventing values from different bases from being mixed.

Tap the widget body to open its conversion-pair list. The **Rate provider settings** entry opens the provider and API-key screen. Tapping an individual conversion row opens that pair's edit screen directly. Tapping the graph icon opens a Google search for the pair, such as `USD MYR`, where Google can display its conversion chart. The **refresh button** fetches the latest rates. When all configured rows do not fit in the widget's current height, the conversion list can be scrolled. Rates are updated automatically according to the selected provider, and the last saved value remains visible if a refresh cannot reach the service.

### Screenshots

![Currency Converter Widget configuration screen](docs/configuration-screen-2.1.0.jpg)

![Currency conversion widget](docs/widget-example-2.2.0.jpg)

## Rate data source

The widget supports two rate providers, selected from the configuration screen:

- **ExchangeRate-API Open Access** is the default no-key option. Its public dataset updates once per day.
- **fxRatesAPI** uses an API key entered by the user. Its documented feed is aggregated from multiple sources and updates according to the account plan, so it can provide fresher and potentially more accurate rates.

The fxRatesAPI key is never written to ordinary app preferences or included in logs. It is encrypted with an AES-GCM key held by the Android Keystore, with only the ciphertext stored in a private, backup-excluded preference file. The key is local to the device and is removed when the app is uninstalled or the app data is cleared. The app does not transmit the key anywhere except as the HTTPS credential required by fxRatesAPI requests.

ExchangeRate-API uses this URL pattern:

```text
https://open.er-api.com/v6/latest/{BASE_CURRENCY}
```

fxRatesAPI uses its authenticated latest endpoint:

```text
https://api.fxratesapi.com/latest?base={BASE_CURRENCY}&api_key={API_KEY}
```

Both providers return rates quoted from the selected base currency. The widget groups pairs with the same base into one request and caches the last successful values locally for offline display. Availability, limits, accuracy, and terms are controlled by the upstream provider.

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
QA_VERSION_NAME=2.3.0-qa.1 QA_VERSION_CODE=40 ./build-qa.sh
```

`build.sh` accepts explicit `VERSION_NAME` and `VERSION_CODE` overrides. During feature QA, use the separate QA wrapper and keep the feature version unchanged:

```sh
QA_VERSION_NAME=2.3.0-qa.1 QA_VERSION_CODE=36 ./build-qa.sh
QA_VERSION_NAME=2.3.0-qa.2 QA_VERSION_CODE=37 ./build-qa.sh
```

Increment the QA version code for every APK installed over the previous one. QA fixes remain part of the same `2.3.0-qa.N` cycle. Only after QA approval should we build the production release:

```sh
VERSION_NAME=2.3.0 VERSION_CODE=38 ./build.sh
```

After a released version, fixes use the next patch version, such as `2.3.1`. The version code must always be higher than every APK already installed on the device.

For a GitHub production release, use the production-only gate after QA approval:

```sh
RELEASE_VERSION=2.3.0 \
RELEASE_VERSION_CODE=41 \
PRODUCTION_KEYSTORE=/secure/path/release.keystore \
PRODUCTION_KEYSTORE_PASSWORD='…' \
PRODUCTION_KEY_ALIAS='release' \
PRODUCTION_KEY_PASSWORD='…' \
./release.sh
```

This rejects QA suffixes, builds the exact production version, verifies the embedded metadata and signature, and produces `build/Currency-Converter-Widget-2.3.0.apk`. Only that verified artifact should be attached to the matching GitHub release tag `v2.3.0`.

The signed APK is written to `build/Currency-Converter-Widget-<version>.apk`, making QA and production artifacts distinguishable by filename. The build also verifies the APK signature. Generated build output and signing keys are intentionally ignored by Git.

## Install

Download the APK from the [published releases](../../releases), copy it to a location visible to Android Files, open it, and approve installation if Android asks. After installation, add **Currency Converter Widget** from the home-screen widget picker.

## Refresh behavior

The ExchangeRate-API Open Access endpoint updates once per day, so the widget checks it hourly while Android may defer background work under battery-saving policies. fxRatesAPI refreshes according to the selected account plan. The refresh button remains available for manual checks, and the last successful value remains visible when a request fails.
