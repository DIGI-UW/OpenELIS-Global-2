#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"
LOG_DIR="${ROOT_DIR}/logs"
PROCFILE="${ROOT_DIR}/Procfile.dev"

mkdir -p "${LOG_DIR}"

load_env() {
  if [ -f "${ENV_FILE}" ]; then
    set -a
    # shellcheck disable=SC1090
    . "${ENV_FILE}"
    set +a
  fi
}

kill_processes() {
  pkill -TERM -f "honcho -f ${PROCFILE}" >/dev/null 2>&1 || true
  sleep 0.3
  pkill -KILL -f "honcho -f ${PROCFILE}" >/dev/null 2>&1 || true
}

wait_for_url() {
  local url="$1"
  local attempts="${2:-20}"
  local delay="${3:-1}"
  for _ in $(seq 1 "${attempts}"); do
    if curl -sf "${url}" >/dev/null; then
      return 0
    fi
    sleep "${delay}"
  done
  return 1
}

start_services() {
  # Run honcho from ROOT_DIR so Procfile.dev relative paths work correctly
  # Find honcho in gateway's uv virtualenv
  GATEWAY_VENV="${ROOT_DIR}/catalyst-gateway/.venv"
  if [ -n "${GATEWAY_VENV}" ] && [ -f "${GATEWAY_VENV}/bin/honcho" ]; then
    HONCHO_CMD="${GATEWAY_VENV}/bin/honcho"
    (cd "${ROOT_DIR}" && ${HONCHO_CMD} -f "${PROCFILE}" start) \
      >> "${LOG_DIR}/honcho.out" 2>&1 &
  else
    # Fallback to uv run (must run from ROOT_DIR)
    (cd "${ROOT_DIR}/catalyst-gateway" && uv run honcho -f "${PROCFILE}" start) \
      >> "${LOG_DIR}/honcho.out" 2>&1 &
  fi
  HONCHO_PID=$!
}

cleanup() {
  if [ -n "${HONCHO_PID:-}" ]; then
    kill -TERM "${HONCHO_PID}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

run_gateway_tests() {
  (cd "${ROOT_DIR}/catalyst-gateway" && uv sync --no-install-project --extra dev >/dev/null 2>&1 && PYTHONPATH="${ROOT_DIR}/catalyst-gateway" uv run pytest)
}

run_agent_tests() {
  (cd "${ROOT_DIR}/catalyst-agents" && uv sync --no-install-project --extra dev >/dev/null 2>&1 && PYTHONPATH="${ROOT_DIR}/catalyst-agents" uv run pytest)
}

run_mcp_tests() {
  (cd "${ROOT_DIR}/catalyst-mcp" && uv sync --no-install-project --extra dev >/dev/null 2>&1 && PYTHONPATH="${ROOT_DIR}/catalyst-mcp" uv run pytest)
}

run_integration_tests() {
  (cd "${ROOT_DIR}/catalyst-agents" && uv sync --no-install-project --extra dev >/dev/null 2>&1 && PYTHONPATH="${ROOT_DIR}/catalyst-agents" uv run pytest tests/test_integration.py)
}

run_case() {
  case "$1" in
    all)
      run_gateway_tests
      run_mcp_tests
      run_agent_tests
      run_integration_tests
      ;;
    gateway)
      run_gateway_tests
      ;;
    mcp)
      run_mcp_tests
      ;;
    agents)
      run_agent_tests
      ;;
    integration)
      run_integration_tests
      ;;
    *)
      echo "Unknown selector: $1" >&2
      exit 1
      ;;
  esac
}

load_env
kill_processes
start_services

GATEWAY_PORT="${GATEWAY_PORT:-8000}"
ROUTER_PORT="${ROUTER_PORT:-9100}"
CATALYST_AGENT_PORT="${CATALYST_AGENT_PORT:-9101}"

wait_for_url "http://localhost:${GATEWAY_PORT}/health"
wait_for_url "http://localhost:${ROUTER_PORT}/.well-known/agent-card.json"
wait_for_url "http://localhost:${CATALYST_AGENT_PORT}/.well-known/agent-card.json"

if [ "$#" -eq 0 ]; then
  run_case all
else
  for sel in "$@"; do
    run_case "$sel"
  done
fi

echo "Smoke tests completed successfully."
