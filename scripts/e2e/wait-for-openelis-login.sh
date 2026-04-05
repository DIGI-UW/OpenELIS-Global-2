#!/usr/bin/env bash
# Wait until ValidateLogin succeeds (same contract as analyzer harness).
# Prefer this over curling the app root URL, which can succeed before auth is ready.
#
# Env: TEST_USER, TEST_PASS (optional; defaults match verify-login.sh)
#      BASE_URL (default https://localhost)
#      TIMEOUT_SECONDS (default 240)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "${SCRIPT_DIR}/wait-utils.sh"

BASE_URL="${BASE_URL:-https://localhost}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-240}"

echo "Waiting for OpenELIS login readiness at ${BASE_URL} (timeout ${TIMEOUT_SECONDS}s)..."
run_with_timeout_wait \
  "${TIMEOUT_SECONDS}" \
  "OpenELIS login readiness" \
  "bash \"${REPO_ROOT}/projects/analyzer-harness/scripts/verify-login.sh\" --base-url \"${BASE_URL}\" >/dev/null 2>&1"
echo "OpenELIS login is ready."
