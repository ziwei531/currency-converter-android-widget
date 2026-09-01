# Releasing

A release is prepared only after Zi Wei explicitly approves the QA cycle.

## Production build

Use the production-only gate. It rejects QA suffixes, requires an explicit production keystore, verifies the embedded version name and version code, verifies the APK signature, and rejects debug certificates.

```sh
RELEASE_VERSION=2.3.0 \
RELEASE_VERSION_CODE=<next-unused-code> \
PRODUCTION_KEYSTORE=/secure/path/release.keystore \
PRODUCTION_KEYSTORE_PASSWORD='<secret>' \
PRODUCTION_KEY_ALIAS='release' \
PRODUCTION_KEY_PASSWORD='<secret>' \
./release.sh
```

The verified artifact is written to:

```text
build/Currency-Converter-Widget-2.3.0.apk
```

Production builds must use a stable release keystore. Never use `debug.keystore` for a production release. Keep keystores and passwords outside the repository.

## GitHub release

Only the verified production artifact may be attached to the matching tag:

```text
v2.3.0 → Currency-Converter-Widget-2.3.0.apk
```

Do not create or push the tag until the production APK has passed the release gate. Do not upload QA artifacts to a normal GitHub release.

After publishing, verify the tag, release state, release notes, asset name, and asset digest through GitHub before reporting success.
