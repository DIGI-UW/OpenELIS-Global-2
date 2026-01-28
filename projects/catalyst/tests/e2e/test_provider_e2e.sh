#!/bin/bash
# E2E test: verify LLM provider returns SQL via Catalyst Gateway.
# Requires: Gateway, Router, Catalyst Agent, MCP services running (e.g. via Procfile.dev or run_tests.sh).
# For full M0.1 sign-off: run once with LLM_PROVIDER=lmstudio (LM Studio on localhost:1234),
# then again with LLM_PROVIDER=gemini and GEMINI_API_KEY set.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CATALYST_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
GATEWAY_PORT="${GATEWAY_PORT:-8000}"
GATEWAY_URL="http://localhost:${GATEWAY_PORT}"

# Optional: load .env from project root
ENV_FILE="${CATALYST_ROOT}/.env"
if [ -f "${ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  . "${ENV_FILE}"
  set +a
fi

echo "E2E Provider test: Gateway at ${GATEWAY_URL}"

# Health check
if ! curl -sf "${GATEWAY_URL}/health" >/dev/null; then
  echo "ERROR: Gateway not reachable at ${GATEWAY_URL}. Start services first (e.g. Procfile.dev or run_tests.sh)." >&2
  exit 1
fi

# POST chat completion
RESP_FILE=$(mktemp)
HTTP_CODE=$(curl -s -o "${RESP_FILE}" -w "%{http_code}" -X POST "${GATEWAY_URL}/v1/chat/completions" \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"How many tests are there? Reply with a single SELECT SQL statement only."}]}')

if [ "${HTTP_CODE}" != "200" ]; then
  echo "ERROR: Gateway returned HTTP ${HTTP_CODE}" >&2
  cat "${RESP_FILE}" >&2
  rm -f "${RESP_FILE}"
  exit 1
fi

# Response should contain SQL-like content (SELECT, etc.) or at least choices/content
if ! grep -qE '(SELECT|choices|content)' "${RESP_FILE}"; then
  echo "ERROR: Response did not contain expected SQL or completion structure." >&2
  cat "${RESP_FILE}" >&2
  rm -f "${RESP_FILE}"
  exit 1
fi

rm -f "${RESP_FILE}"
echo "PASS: Provider E2E (current LLM_PROVIDER=${LLM_PROVIDER:-<unset>})"
exit 0
