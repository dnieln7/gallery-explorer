#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
REPORT_MD="${ROOT_DIR}/app/build/reports/detekt.md"
REPORT_HTML="${ROOT_DIR}/app/build/reports/detekt.html"

cd "${ROOT_DIR}"

status=0
if ! ./gradlew :app:detekt "$@"; then
    status=$?
fi

echo "Detekt markdown report: ${REPORT_MD}"
echo "Detekt HTML report: ${REPORT_HTML}"

if [[ -f "${REPORT_MD}" ]]; then
    echo "Markdown report available."
else
    echo "Markdown report not found."
fi

if [[ -f "${REPORT_HTML}" ]]; then
    echo "HTML report available."
else
    echo "HTML report not found."
fi

exit "${status}"
