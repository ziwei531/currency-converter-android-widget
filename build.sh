#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD="$ROOT/build"
python3 "$ROOT/scripts/verify_widget_layouts.py"
rm -rf "$BUILD"
mkdir -p "$BUILD/compiled" "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"

# Compile and link Android resources with the native Termux AAPT2 package.
aapt2 compile --dir "$ROOT/res" -o "$BUILD/resources.zip"
aapt2 link \
  -I /system/framework/framework-res.apk \
  --manifest "$ROOT/AndroidManifest.xml" \
  --java "$BUILD/gen" \
  --min-sdk-version 26 \
  --target-sdk-version 33 \
  --version-code 7 \
  --version-name 0.6.1 \
  -o "$BUILD/resources.ap_" "$BUILD/resources.zip"

# Compile the widget against the device's Android framework API.
javac -source 8 -target 8 -encoding UTF-8 \
  -classpath "$HOME/android-sdk/platforms/android-33/android.jar" \
  -d "$BUILD/classes" \
  $(find "$ROOT/src" "$BUILD/gen" -name '*.java' -print)

# Convert bytecode to Android DEX.
d8 --lib "$HOME/android-sdk/platforms/android-33/android.jar" \
  --output "$BUILD/dex" \
  $(find "$BUILD/classes" -name '*.class' -print)

cp "$BUILD/resources.ap_" "$BUILD/Rate-Nori-unsigned.apk"
(cd "$BUILD/dex" && zip -q -j "$BUILD/Rate-Nori-unsigned.apk" classes.dex)

KEYSTORE="$ROOT/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -v -keystore "$KEYSTORE" -storepass android -keypass android \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname 'CN=Android Debug,O=Android,C=US' >/dev/null 2>&1
fi

apksigner sign --ks "$KEYSTORE" --ks-pass pass:android \
  --ks-key-alias androiddebugkey --key-pass pass:android \
  --out "$BUILD/Rate-Nori.apk" "$BUILD/Rate-Nori-unsigned.apk"
apksigner verify --verbose "$BUILD/Rate-Nori.apk"

printf '\nBuilt: %s\n' "$BUILD/Rate-Nori.apk"
ls -lh "$BUILD/Rate-Nori.apk"
