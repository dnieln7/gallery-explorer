#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACTS_DIR="${ROOT_DIR}/build/local-ci-artifacts"
LOGS_DIR="${ARTIFACTS_DIR}/logs"

QUALITY_LOG="${LOGS_DIR}/quality.log"
TEST_LOG="${LOGS_DIR}/test.log"
SCHEMA_LOG="${LOGS_DIR}/schema-check.log"
RELEASE_LOG="${LOGS_DIR}/release-build.log"

QUALITY_PID=""
TEST_PID=""
SCHEMA_PID=""

mkdir -p "${LOGS_DIR}"

cleanup_background_jobs() {
    local pids=()

    if [[ -n "${QUALITY_PID}" ]]; then
        pids+=("${QUALITY_PID}")
    fi

    if [[ -n "${TEST_PID}" ]]; then
        pids+=("${TEST_PID}")
    fi

    if [[ -n "${SCHEMA_PID}" ]]; then
        pids+=("${SCHEMA_PID}")
    fi

    if [[ "${#pids[@]}" -gt 0 ]]; then
        kill "${pids[@]}" 2>/dev/null || true
    fi
}

on_exit() {
    cleanup_background_jobs
}

trap on_exit EXIT

run_job() {
    local job_name="$1"
    local log_file="$2"
    shift 2

    (
        cd "${ROOT_DIR}"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting ${job_name}"
        ./gradlew "$@"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Finished ${job_name}"
    ) > "${log_file}" 2>&1
}

wait_for_job() {
    local job_name="$1"
    local pid="$2"
    local log_file="$3"

    if ! wait "${pid}"; then
        echo
        echo "${job_name} failed. See ${log_file}"
        return 1
    fi
}

copy_path_if_exists() {
    local source_path="$1"
    local target_dir="$2"

    if [[ -e "${source_path}" ]]; then
        mkdir -p "${target_dir}"
        cp -R "${source_path}" "${target_dir}/"
    fi
}

collect_quality_artifacts() {
    local target_dir="${ARTIFACTS_DIR}/quality"

    copy_path_if_exists "${ROOT_DIR}/app/build/reports/detekt" "${target_dir}"

    shopt -s nullglob
    for lint_report in "${ROOT_DIR}"/app/build/reports/lint-results-*; do
        copy_path_if_exists "${lint_report}" "${target_dir}"
    done
    shopt -u nullglob

    copy_path_if_exists \
        "${ROOT_DIR}/app/build/intermediates/lint_intermediate_text_report" \
        "${target_dir}"
}

collect_test_artifacts() {
    local target_dir="${ARTIFACTS_DIR}/test"

    copy_path_if_exists "${ROOT_DIR}/app/build/test-results/testDebugUnitTest" "${target_dir}"
    copy_path_if_exists "${ROOT_DIR}/app/build/reports/tests/testDebugUnitTest" "${target_dir}"
    copy_path_if_exists "${ROOT_DIR}/app/build/reports/kover" "${target_dir}"
}

collect_schema_artifacts() {
    local target_dir="${ARTIFACTS_DIR}/schema-check"

    copy_path_if_exists "${ROOT_DIR}/app/schemas" "${target_dir}"
}

collect_release_artifacts() {
    local target_dir="${ARTIFACTS_DIR}/release-build"

    copy_path_if_exists "${ROOT_DIR}/app/build/outputs" "${target_dir}"
    copy_path_if_exists "${ROOT_DIR}/app/build/reports" "${target_dir}"
}

run_quality() {
    run_job "quality" "${QUALITY_LOG}" :app:detekt :app:lintDebug
}

run_test() {
    run_job \
        "test" \
        "${TEST_LOG}" \
        :app:testDebugUnitTest \
        :app:koverHtmlReportDebug \
        :app:koverVerifyDebug
}

run_schema_check() {
    (
        cd "${ROOT_DIR}"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting schema-check"
        ./gradlew :app:assembleRelease
        git update-index --refresh
        git diff --exit-code -- app/schemas
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Finished schema-check"
    ) > "${SCHEMA_LOG}" 2>&1
}

run_release_build() {
    run_job "release-build" "${RELEASE_LOG}" :app:assembleRelease
}

echo "Artifacts directory: ${ARTIFACTS_DIR}"
echo "Starting parallel jobs: quality, test, schema-check"

run_quality &
QUALITY_PID=$!

run_test &
TEST_PID=$!

run_schema_check &
SCHEMA_PID=$!

wait_for_job "quality" "${QUALITY_PID}" "${QUALITY_LOG}"
collect_quality_artifacts

wait_for_job "test" "${TEST_PID}" "${TEST_LOG}"
collect_test_artifacts

wait_for_job "schema-check" "${SCHEMA_PID}" "${SCHEMA_LOG}"
collect_schema_artifacts

QUALITY_PID=""
TEST_PID=""
SCHEMA_PID=""

echo "Parallel jobs completed successfully"
echo "Starting release-build"

run_release_build
collect_release_artifacts

echo "Local CI completed successfully"
echo "Logs:"
echo "  ${QUALITY_LOG}"
echo "  ${TEST_LOG}"
echo "  ${SCHEMA_LOG}"
echo "  ${RELEASE_LOG}"
echo "Artifacts:"
echo "  ${ARTIFACTS_DIR}"
