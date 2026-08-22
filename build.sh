#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD="$ROOT/build"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
ANDROID_PLATFORM="$ANDROID_SDK_ROOT/platforms/android-33/android.jar"
ANDROID_FRAMEWORK_RES="${ANDROID_FRAMEWORK_RES:-/system/framework/framework-res.apk}"
rm -rf "$BUILD"
mkdir -p "$BUILD/compiled" "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"

# Compile and link Android resources with the installed Android build tools.
aapt2 compile --dir "$ROOT/res" -o "$BUILD/resources.zip"
aapt2 link \
  -I "$ANDROID_FRAMEWORK_RES" \
  --manifest "$ROOT/AndroidManifest.xml" \
  --java "$BUILD/gen" \
  --min-sdk-version 26 \
  --target-sdk-version 33 \
  --version-code 26 \
  --version-name 1.0.2 \
  -o "$BUILD/resources.ap_" "$BUILD/resources.zip"

# Compile the widget against the selected Android framework API.
javac -source 8 -target 8 -encoding UTF-8 \
  -classpath "$ANDROID_PLATFORM" \
  -d "$BUILD/classes" \
  $(find "$ROOT/src" "$BUILD/gen" -name '*.java' -print)

# Convert bytecode to Android DEX.
d8 --lib "$ANDROID_PLATFORM" \
  --output "$BUILD/dex" \
  $(find "$BUILD/classes" -name '*.class' -print)

cp "$BUILD/resources.ap_" "$BUILD/Currency-Converter-Widget-unsigned.apk"
(cd "$BUILD/dex" && zip -q -j "$BUILD/Currency-Converter-Widget-unsigned.apk" classes.dex)

KEYSTORE="$ROOT/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -v -keystore "$KEYSTORE" -storepass android -keypass android \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname 'CN=Android Debug,O=Android,C=US' >/dev/null 2>&1
fi

apksigner sign --ks "$KEYSTORE" --ks-pass pass:android \
  --ks-key-alias androiddebugkey --key-pass pass:android \
  --out "$BUILD/Currency-Converter-Widget.apk" "$BUILD/Currency-Converter-Widget-unsigned.apk"
apksigner verify --verbose "$BUILD/Currency-Converter-Widget.apk"

printf '\nBuilt: %s\n' "$BUILD/Currency-Converter-Widget.apk"
ls -lh "$BUILD/Currency-Converter-Widget.apk"
