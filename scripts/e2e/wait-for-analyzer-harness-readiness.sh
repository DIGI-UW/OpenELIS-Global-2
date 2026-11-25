#!/usr/bin/env bash
# Full harness gate: login-ready OpenELIS, healthy bridge, healthy ASTM simulator.
# Single source of truth for CI and local parity (called by e2e-playwright-reusable.yml).
#
# Env: TEST_USER, TEST_PASS, BASE_URL, TIMEOUT_SECONDS (login; default 240)
#      BRIDGE_TIMEOUT_SECONDS (default 120), SIMULATOR_TIMEOUT_SECONDS (default 120)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/wait-utils.sh"

BASE_URL="${BASE_URL:-https://localhost}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-240}"
BRIDGE_TIMEOUT_SECONDS="${BRIDGE_TIMEOUT_SECONDS:-120}"
SIMULATOR_TIMEOUT_SECONDS="${SIMULATOR_TIMEOUT_SECONDS:-120}"
BRIDGE_ADMIN_URL="${BRIDGE_ADMIN_URL:-https://localhost:8442}"
MOCK_SIMULATOR_URL="${MOCK_SIMULATOR_URL:-http://localhost:8085}"

wait_for_url() {
  local label="$1"
  local timeout_seconds="$2"
  local curl_command="$3"

  echo "Waiting for ${label}..."
  run_with_timeout_wait "${timeout_seconds}" "${label}" "${curl_command} > /dev/null"
  echo "${label} is ready."
}

bash "${SCRIPT_DIR}/wait-for-openelis-login.sh"

wait_for_url \
  "bridge readiness" \
  "${BRIDGE_TIMEOUT_SECONDS}" \
  "curl -k -s -f --connect-timeout 2 --max-time 3 $BRIDGE_ADMIN_URL/actuator/health"

wait_for_url \
  "simulator readiness" \
  "${SIMULATOR_TIMEOUT_SECONDS}" \
  "curl -s -f --connect-timeout 2 --max-time 3 $MOCK_SIMULATOR_URL/health"
