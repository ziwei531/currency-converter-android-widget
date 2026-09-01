# Agent Instructions

This is a native Java Android widget project.

Before editing:

1. Read `context/java-coding-preference.md`.
2. Read `context/git-commit-convention.md` before committing.
3. Keep the widget small, cacheable, and safe when offline.
4. Do not commit generated build output, APKs, signing keys, or local SDK paths.
5. Use the old build workflow: local/testing/QA APKs use `build.sh` with the debug certificate, keep the current `versionName`, and bump only `versionCode`; never append a QA suffix or assume a patch/hotfix version. Production releases use `release.sh` with the production certificate and an exact `x.y.z` version. Change the patch or hotfix version only when an explicit release-tag push is requested. GitHub releases must use only the verified production artifact.
6. Keep `README.md` current with the implemented features, workflows, and build outputs.
7. Keep `CHANGELOG.md` unchanged during QA; update it only when preparing an approved release tag.
8. Keep `README.md` concise; place detailed build, release, and operational guidance in dedicated `docs/` references.
9. Keep all public documentation, comments, examples, and operational guidance maintainer-agnostic. Do not include personal names, private paths, personal context, account details, or instructions written for one specific user. Use generic roles such as `release owner` and generic placeholders for local paths, credentials, and identifiers.
