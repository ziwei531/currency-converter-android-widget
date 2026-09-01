# Agent Instructions

This is a native Java Android widget project.

Before editing:

1. Read `context/java-coding-preference.md`.
2. Read `context/git-commit-convention.md` before committing.
3. Keep the widget small, cacheable, and safe when offline.
4. Do not commit generated build output, APKs, signing keys, or local SDK paths.
5. During QA/local APK builds, keep the current feature `versionName` and bump only `versionCode`; never bump a patch or hotfix version until Zi Wei explicitly asks for a release-tag push. GitHub releases must use only the production artifact from `release.sh` with an exact `x.y.z` version.
6. Keep `README.md` current with the implemented features, workflows, and build outputs.
7. Keep `CHANGELOG.md` unchanged during QA; update it only when preparing an approved release tag.
8. Keep `README.md` concise; place detailed build, release, and operational guidance in dedicated `docs/` references.
