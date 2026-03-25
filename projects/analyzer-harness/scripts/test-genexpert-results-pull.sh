#!/bin/bash
# test-genexpert-results-pull.sh — Exercise OpenELIS -> Analyzer query-results API.
#
# Usage:
#   ANALYZER_ID=2006 ACCESSION=2026-A01 TOKEN="<bearer>" ./scripts/test-genexpert-results-pull.sh

set -euo pipefail

ANALYZER_ID="${ANALYZER_ID:-2006}"
ACCESSION="${ACCESSION:-2026-A01}"
OE_BASE_URL="${OE_BASE_URL:-https://localhost/api}"
TOKEN="${TOKEN:-}"
TEST_CODES="${TEST_CODES:-}"

ENDPOINT="${OE_BASE_URL}/rest/analyzer/analyzers/${ANALYZER_ID}/query-results"

echo "================================================================"
echo "  GeneXpert Results Pull Test"
echo "================================================================"
echo "  Analyzer ID: ${ANALYZER_ID}"
echo "  Accession:   ${ACCESSION}"
echo "  Endpoint:    ${ENDPOINT}"
echo "================================================================"
echo

AUTH_HEADER=()
if [ -n "$TOKEN" ]; then
  AUTH_HEADER=(-H "Authorization: Bearer ${TOKEN}")
fi

if [ -n "$TEST_CODES" ]; then
  # TEST_CODES format: GLUCOSE,HGB
  IFS=',' read -r -a codes <<< "$TEST_CODES"
  json_codes=$(printf '"%s",' "${codes[@]}")
  json_codes="[${json_codes%,}]"
  payload="{\"accessionNumber\":\"${ACCESSION}\",\"testCodes\":${json_codes}}"
else
  payload="{\"accessionNumber\":\"${ACCESSION}\"}"
fi

tmp_body="$(mktemp)"
http_code=$(curl -ks -o "${tmp_body}" -w "%{http_code}" -X POST "${ENDPOINT}" \
  -H "Content-Type: application/json" \
  "${AUTH_HEADER[@]}" \
  -d "${payload}")
response="$(cat "${tmp_body}")"
rm -f "${tmp_body}"

echo "HTTP status: ${http_code}"
if [[ ! "${http_code}" =~ ^2 ]]; then
  echo "Request failed; supply TOKEN for secured environments."
fi

if echo "${response}" | python3 -m json.tool >/dev/null 2>&1; then
  echo "${response}" | python3 -m json.tool
else
  echo "${response}"
fi

echo
echo "Results pull request complete."
