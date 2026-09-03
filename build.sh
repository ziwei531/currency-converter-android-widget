#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD="$ROOT/build"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
ANDROID_PLATFORM="$ANDROID_SDK_ROOT/platforms/android-33/android.jar"
ANDROID_FRAMEWORK_RES="${ANDROID_FRAMEWORK_RES:-/system/framework/framework-res.apk}"
VERSION_CODE="${VERSION_CODE:?Set VERSION_CODE explicitly}"
VERSION_NAME="${VERSION_NAME:?Set VERSION_NAME explicitly}"
APK_NAME="${APK_NAME:-Currency-Converter-Widget-${VERSION_NAME}.apk}"
APK="$BUILD/$APK_NAME"
SIGNING_MODE="${SIGNING_MODE:-qa}"
EXPECTED_APK_NAME="Currency-Converter-Widget-${VERSION_NAME}.apk"
if [[ "$SIGNING_MODE" == "qa" ]]; then
	LATEST_RELEASE_TAG="$(git -C "$ROOT" describe --tags --abbrev=0 --match 'v[0-9]*.[0-9]*.[0-9]*' 2>/dev/null || true)"
	if [[ -n "$LATEST_RELEASE_TAG" && "$VERSION_NAME" != "${LATEST_RELEASE_TAG#v}" ]]; then
		printf 'QA VERSION_NAME must match the latest release tag (%s), not %s.\n' "${LATEST_RELEASE_TAG#v}" "$VERSION_NAME" >&2
		exit 1
	fi
	if [[ "$APK_NAME" != "$EXPECTED_APK_NAME" ]]; then
		printf 'QA APK_NAME must match the version (%s), not %s.\n' "$EXPECTED_APK_NAME" "$APK_NAME" >&2
		exit 1
	fi
fi
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
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
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

if [[ "$SIGNING_MODE" == "production" ]]; then
	: "${PRODUCTION_KEYSTORE:?Set PRODUCTION_KEYSTORE for a production build}"
	: "${PRODUCTION_KEYSTORE_PASSWORD:?Set PRODUCTION_KEYSTORE_PASSWORD for a production build}"
	: "${PRODUCTION_KEY_ALIAS:?Set PRODUCTION_KEY_ALIAS for a production build}"
	: "${PRODUCTION_KEY_PASSWORD:?Set PRODUCTION_KEY_PASSWORD for a production build}"
	KEYSTORE="$PRODUCTION_KEYSTORE"
	KEYSTORE_ARGS=(
		--ks-pass "env:PRODUCTION_KEYSTORE_PASSWORD"
		--ks-key-alias "$PRODUCTION_KEY_ALIAS"
		--key-pass "env:PRODUCTION_KEY_PASSWORD"
	)
else
	KEYSTORE="$ROOT/debug.keystore"
	KEYSTORE_ARGS=(
		--ks-pass pass:android
		--ks-key-alias androiddebugkey
		--key-pass pass:android
	)
	if [[ "$SIGNING_MODE" != "qa" ]]; then
		printf 'Signing mode must be qa or production: %s\n' "$SIGNING_MODE" >&2
		exit 1
	fi
fi
if [ ! -f "$KEYSTORE" ]; then
	if [[ "$SIGNING_MODE" == "production" ]]; then
		printf 'Production keystore does not exist: %s\n' "$KEYSTORE" >&2
		exit 1
	fi
	keytool -genkeypair -v -keystore "$KEYSTORE" -storepass android -keypass android \
		-alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
		-dname 'CN=Android Debug,O=Android,C=US' >/dev/null 2>&1
fi

apksigner sign --ks "$KEYSTORE" "${KEYSTORE_ARGS[@]}" \
	--out "$APK" "$BUILD/Currency-Converter-Widget-unsigned.apk"
apksigner verify --verbose "$APK"

if [[ "$SIGNING_MODE" == "production" ]]; then
	printf '\nBuild channel: production (production certificate)\n'
else
	printf '\nBuild channel: QA/testing (debug certificate)\n'
fi
printf 'Built: %s\n' "$APK"
ls -lh "$APK"
