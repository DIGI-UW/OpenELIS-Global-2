#!/bin/bash
# test-genexpert-orders-pull.sh — Simulate analyzer-initiated ASTM Q-record query.
#
# Usage:
#   ACCESSION=2026-A01 ./scripts/test-genexpert-orders-pull.sh
#
# Notes:
#   - This emulates the analyzer querying OpenELIS for orders (orders-pull pathway).
#   - Default endpoint targets the deployed OE ASTM import endpoint.

set -euo pipefail

ACCESSION="${ACCESSION:-2026-A01}"
ASTM_ENDPOINT="${ASTM_ENDPOINT:-https://localhost/api/OpenELIS-Global/analyzer/astm}"

echo "================================================================"
echo "  GeneXpert Orders Pull Test"
echo "================================================================"
echo "  Accession: ${ACCESSION}"
echo "  Endpoint:  ${ASTM_ENDPOINT}"
echo "================================================================"
echo

q_message=$(
  cat <<EOF
H|\^&|||GENEXPERT^CEPHEID^1.0|||||||LIS2-A2
Q|1|${ACCESSION}||ALL
L|1|N
EOF
)

tmp_body="$(mktemp)"
http_code=$(curl -ks -o "${tmp_body}" -w "%{http_code}" -X POST "${ASTM_ENDPOINT}" \
  -H "Content-Type: text/plain; charset=utf-8" \
  --data-binary "${q_message}")
response="$(cat "${tmp_body}")"
rm -f "${tmp_body}"

echo "HTTP status: ${http_code}"
if [[ ! "${http_code}" =~ ^2 ]]; then
  echo "Orders-pull request failed."
fi
if [ -n "${response}" ]; then
  echo "${response}"
fi

echo
echo "Orders pull query sent."
