#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(CDPATH="" cd "${SCRIPT_DIR}/../.." && pwd)"
ARTIFACT_DIR="${REPO_ROOT}/specs/201-e2e-playwright-risk-parity/artifacts"
TMP_ROOT="${REPO_ROOT}/tmp/m8a-evidence"

usage() {
  cat <<EOF
Usage: $(basename "$0") --cypress-run-id <id> --playwright-run-id <id> [--head-sha <sha>]

Downloads normalized CI artifacts and generates:
  - artifacts/cutoff-scope.json
  - artifacts/runtime-metrics.json
  - artifacts/parity-report.json
  - artifacts/parity-report.md
  - artifacts/m8a-gate-check.md
EOF
}

CYPRESS_RUN_ID=""
PLAYWRIGHT_RUN_ID=""
HEAD_SHA=""
REPO_SLUG=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --cypress-run-id)
      CYPRESS_RUN_ID="$2"
      shift 2
      ;;
    --playwright-run-id)
      PLAYWRIGHT_RUN_ID="$2"
      shift 2
      ;;
    --head-sha)
      HEAD_SHA="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${CYPRESS_RUN_ID}" || -z "${PLAYWRIGHT_RUN_ID}" ]]; then
  echo "Both --cypress-run-id and --playwright-run-id are required." >&2
  usage
  exit 1
fi

if [[ -z "${HEAD_SHA}" ]]; then
  HEAD_SHA="$(git -C "${REPO_ROOT}" rev-parse HEAD)"
fi

REPO_SLUG="$(gh repo view --json nameWithOwner --jq '.nameWithOwner' 2>/dev/null || true)"
if [[ -z "${REPO_SLUG}" ]]; then
  REPO_SLUG="DIGI-UW/OpenELIS-Global-2"
fi

mkdir -p "${TMP_ROOT}/cypress" "${TMP_ROOT}/playwright" "${ARTIFACT_DIR}"
rm -rf "${TMP_ROOT}/cypress"/* "${TMP_ROOT}/playwright"/*

download_normalized_artifacts() {
  local run_id="$1"
  local framework="$2"
  local output_dir="$3"
  local downloaded=0
  local artifact_names=""

  set +e
  artifact_names="$(
    gh api "repos/${REPO_SLUG}/actions/runs/${run_id}/artifacts" --jq '.artifacts[].name'
  )"
  local list_exit=$?
  set -e
  if [[ ${list_exit} -ne 0 || -z "${artifact_names}" ]]; then
    return 1
  fi

  while IFS= read -r artifact_name; do
    [[ -n "${artifact_name}" ]] || continue
    if [[ "${artifact_name}" != "${framework}-normalized-"* ]]; then
      continue
    fi
    echo "Downloading ${framework} artifact ${artifact_name} from run ${run_id}..."
    set +e
    gh run download "${run_id}" --name "${artifact_name}" --dir "${output_dir}"
    local download_exit=$?
    set -e
    if [[ ${download_exit} -eq 0 ]]; then
      downloaded=$((downloaded + 1))
    fi
  done <<< "${artifact_names}"

  [[ ${downloaded} -gt 0 ]]
}

echo "Downloading Cypress normalized artifacts from run ${CYPRESS_RUN_ID}..."
set +e
download_normalized_artifacts "${CYPRESS_RUN_ID}" "cypress" "${TMP_ROOT}/cypress"
CYPRESS_DOWNLOAD_EXIT=$?
set -e

echo "Downloading Playwright normalized artifacts from run ${PLAYWRIGHT_RUN_ID}..."
set +e
download_normalized_artifacts "${PLAYWRIGHT_RUN_ID}" "playwright" "${TMP_ROOT}/playwright"
PLAYWRIGHT_DOWNLOAD_EXIT=$?
set -e

if [[ ${CYPRESS_DOWNLOAD_EXIT} -ne 0 ]]; then
  echo "Warning: unable to download Cypress artifacts for run ${CYPRESS_RUN_ID}."
fi
if [[ ${PLAYWRIGHT_DOWNLOAD_EXIT} -ne 0 ]]; then
  echo "Warning: unable to download Playwright artifacts for run ${PLAYWRIGHT_RUN_ID}."
fi

PLAYWRIGHT_FILES="$(
  rg --files "${TMP_ROOT}/playwright" -g "**/playwright-normalized-*.json" | sort | paste -sd, -
)"
CYPRESS_FILES="$(
  rg --files "${TMP_ROOT}/cypress" -g "**/cypress-normalized-*.json" | sort | paste -sd, -
)"

if [[ -z "${PLAYWRIGHT_FILES}" || -z "${CYPRESS_FILES}" ]]; then
  echo "Missing normalized artifacts." >&2
  echo "  Playwright files: ${PLAYWRIGHT_FILES:-<none>}" >&2
  echo "  Cypress files:    ${CYPRESS_FILES:-<none>}" >&2
  exit 2
fi

echo "Generating runtime metrics..."
node "${REPO_ROOT}/scripts/e2e/export-runtime-metrics.js" \
  --input "${PLAYWRIGHT_FILES},${CYPRESS_FILES}" \
  --output "${ARTIFACT_DIR}/runtime-metrics.json" \
  --budget-ms-playwright 2100000 \
  --budget-ms-cypress 3300000

echo "Generating parity report..."
node "${REPO_ROOT}/scripts/e2e/compare-e2e-results.js" \
  --playwright "${PLAYWRIGHT_FILES}" \
  --cypress "${CYPRESS_FILES}" \
  --parity-matrix "${REPO_ROOT}/specs/201-e2e-playwright-risk-parity/parity-matrix.csv" \
  --runtime-metrics "${ARTIFACT_DIR}/runtime-metrics.json" \
  --output-json "${ARTIFACT_DIR}/parity-report.json" \
  --output-md "${ARTIFACT_DIR}/parity-report.md"

echo "Freezing M8a cutoff scope..."
node "${REPO_ROOT}/scripts/e2e/freeze-m8a-cutoff.js" \
  --inventory "${REPO_ROOT}/specs/201-e2e-playwright-risk-parity/artifacts/inventory.json" \
  --parity-matrix "${REPO_ROOT}/specs/201-e2e-playwright-risk-parity/parity-matrix.csv" \
  --head-sha "${HEAD_SHA}" \
  --cypress-run-id "${CYPRESS_RUN_ID}" \
  --playwright-run-id "${PLAYWRIGHT_RUN_ID}" \
  --output "${ARTIFACT_DIR}/cutoff-scope.json"

echo "Evaluating M8a gate..."
set +e
node "${REPO_ROOT}/scripts/e2e/check-m8a-parity-gate.js" \
  --cutoff "${ARTIFACT_DIR}/cutoff-scope.json" \
  --output "${ARTIFACT_DIR}/m8a-gate-check.md"
GATE_EXIT=$?
set -e

if [[ ${GATE_EXIT} -ne 0 ]]; then
  echo "M8a gate failed (blocking rows remain)."
else
  echo "M8a gate passed."
fi

echo "Artifacts generated in: ${ARTIFACT_DIR}"
exit 0
