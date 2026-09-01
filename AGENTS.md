# Agent Instructions

This is a native Java Android widget project.

Before editing:

1. Read `context/java-coding-preference.md`.
2. Read `context/git-commit-convention.md` before committing.
3. Keep the widget small, cacheable, and safe when offline.
4. Do not commit generated build output, APKs, signing keys, or local SDK paths.
5. During QA, use `build-qa.sh` with a `2.x.y-qa.N` version; do not create patch or hotfix versions until Zi Wei explicitly approves QA. GitHub releases must use only the production artifact from `release.sh` with an exact `x.y.z` version.
6. Keep `README.md` current with the implemented features, workflows, and build outputs.

The Java preference is adapted from the shared JavaScript coding preferences. Java language rules take precedence where a JavaScript style cannot be valid Java; see the explicit exceptions in the preference file.
