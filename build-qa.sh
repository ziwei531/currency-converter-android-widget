#!/usr/bin/env bash
set -euo pipefail

: "${QA_VERSION_NAME:?Set QA_VERSION_NAME, for example 2.3.0-qa.1}"
: "${QA_VERSION_CODE:?Set QA_VERSION_CODE higher than the last installed build}"

if [[ ! "$QA_VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+-qa\.[0-9]+$ ]]; then
	printf 'QA version must match x.y.z-qa.N: %s\n' "$QA_VERSION_NAME" >&2
	exit 1
fi
if [[ ! "$QA_VERSION_CODE" =~ ^[0-9]+$ ]]; then
	printf 'QA version code must be numeric: %s\n' "$QA_VERSION_CODE" >&2
	exit 1
fi

VERSION_NAME="$QA_VERSION_NAME" \
VERSION_CODE="$QA_VERSION_CODE" \
SIGNING_MODE=qa \
./build.sh
