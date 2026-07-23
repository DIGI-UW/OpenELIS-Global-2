#!/usr/bin/env bash
# Full harness gate: login-ready OpenELIS, healthy bridge, healthy ASTM simulator.
# Single source of truth for CI and local parity (see e2e-playwright-analyzer-harness-reusable.yml).
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
  "curl -k -s -f --connect-timeout 2 --max-time 3 https://localhost:8442/actuator/health"

wait_for_url \
  "simulator readiness" \
  "${SIMULATOR_TIMEOUT_SECONDS}" \
  "curl -s -f --connect-timeout 2 --max-time 3 http://localhost:8085/health"
