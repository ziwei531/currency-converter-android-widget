#!/usr/bin/env bash
set -euo pipefail

: "${RELEASE_VERSION:?Set RELEASE_VERSION, for example 2.3.0}"
: "${RELEASE_VERSION_CODE:?Set RELEASE_VERSION_CODE higher than every installed build}"
: "${PRODUCTION_KEYSTORE:?Set PRODUCTION_KEYSTORE for a production build}"
: "${PRODUCTION_KEYSTORE_PASSWORD:?Set PRODUCTION_KEYSTORE_PASSWORD for a production build}"
: "${PRODUCTION_KEY_ALIAS:?Set PRODUCTION_KEY_ALIAS for a production build}"
: "${PRODUCTION_KEY_PASSWORD:?Set PRODUCTION_KEY_PASSWORD for a production build}"

if [[ ! "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
	printf 'Release version must be production semver without a QA suffix: %s\n' "$RELEASE_VERSION" >&2
	exit 1
fi

if [[ ! "$RELEASE_VERSION_CODE" =~ ^[0-9]+$ ]]; then
	printf 'Release version code must be numeric: %s\n' "$RELEASE_VERSION_CODE" >&2
	exit 1
fi

APK_NAME="Currency-Converter-Widget-${RELEASE_VERSION}.apk"
VERSION_NAME="$RELEASE_VERSION" \
VERSION_CODE="$RELEASE_VERSION_CODE" \
SIGNING_MODE=production \
PRODUCTION_KEYSTORE="$PRODUCTION_KEYSTORE" \
PRODUCTION_KEYSTORE_PASSWORD="$PRODUCTION_KEYSTORE_PASSWORD" \
PRODUCTION_KEY_ALIAS="$PRODUCTION_KEY_ALIAS" \
PRODUCTION_KEY_PASSWORD="$PRODUCTION_KEY_PASSWORD" \
APK_NAME="$APK_NAME" \
./build.sh

APK="build/$APK_NAME"
PACKAGE_INFO="$(aapt2 dump badging "$APK")"
if [[ "$PACKAGE_INFO" != *"versionName='$RELEASE_VERSION'"* || "$PACKAGE_INFO" != *"versionCode='$RELEASE_VERSION_CODE'"* ]]; then
	printf 'APK metadata does not match the requested production release.\n' >&2
	exit 1
fi
if [[ "$PACKAGE_INFO" == *qa* || "$PACKAGE_INFO" == *debug* || "$PACKAGE_INFO" == *snapshot* ]]; then
	printf 'APK metadata contains a non-production marker.\n' >&2
	exit 1
fi
apksigner verify --verbose "$APK" >/dev/null
CERTIFICATES="$(apksigner verify --print-certs "$APK")"
if [[ "$CERTIFICATES" == *"CN=Android Debug"* || "$CERTIFICATES" == *"androiddebugkey"* ]]; then
	printf 'Production APK is signed with a debug certificate.\n' >&2
	exit 1
fi

printf '\nProduction release artifact verified: %s\n' "$APK"
