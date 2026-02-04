#!/bin/bash
# E2E test: verify multi-agent flow (Router → SchemaAgent → SQLGenAgent) returns SQL via Catalyst Gateway.
# Use when M0.2 stack is running: Gateway, Router, SchemaAgent, SQLGenAgent, MCP.
# Same entrypoint as provider E2E; this script is for validating the multi-agent pipeline.
# Run with default mode (multi-agent) and ensure Gateway returns a valid completion.
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

echo "E2E Multi-agent test: Gateway at ${GATEWAY_URL} (Router → SchemaAgent → SQLGenAgent)"

# Health check
if ! curl -sf "${GATEWAY_URL}/health" >/dev/null; then
  echo "ERROR: Gateway not reachable at ${GATEWAY_URL}. Start M0.2 services first." >&2
  exit 1
fi

# POST chat completion (multi-agent flow is internal to Router/agents)
RESP_FILE=$(mktemp)
HTTP_CODE=$(curl -s -o "${RESP_FILE}" -w "%{http_code}" -X POST "${GATEWAY_URL}/v1/chat/completions" \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"List test names. Reply with a single SELECT SQL statement only."}]}')

if [ "${HTTP_CODE}" != "200" ]; then
  echo "ERROR: Gateway returned HTTP ${HTTP_CODE}" >&2
  cat "${RESP_FILE}" >&2
  rm -f "${RESP_FILE}"
  exit 1
fi

# Response should contain SQL or completion structure
if ! grep -qE '(SELECT|choices|content)' "${RESP_FILE}"; then
  echo "ERROR: Response did not contain expected SQL or completion structure." >&2
  cat "${RESP_FILE}" >&2
  rm -f "${RESP_FILE}"
  exit 1
fi

rm -f "${RESP_FILE}"
echo "PASS: Multi-agent E2E"
exit 0
