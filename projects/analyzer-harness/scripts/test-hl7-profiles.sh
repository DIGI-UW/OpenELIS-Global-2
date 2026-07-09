#!/bin/bash
# test-hl7-profiles.sh - Validate strict 013 HL7 templates through bridge MLLP.
#
# Usage:
#   ./scripts/test-hl7-profiles.sh
#   ./scripts/test-hl7-profiles.sh 3
#   ./scripts/test-hl7-profiles.sh 1 http://localhost:8085 mllp://openelis-analyzer-bridge:2575
#
# Arguments:
#   COUNT        - Number of messages per template (default: 1)
#   API_URL      - Mock server API URL from host (default: http://localhost:8085)
#   DESTINATION  - Destination for simulator push (default: mllp://openelis-analyzer-bridge:2575)

set -euo pipefail

COUNT="${1:-1}"
API_URL="${2:-http://localhost:8085}"
DESTINATION="${3:-mllp://openelis-analyzer-bridge:2575}"

TEMPLATES=(
  "mindray_bc5380"
  "mindray_bs200"
  "mindray_bs300"
)

echo "================================================================"
echo "  HL7 Profile Push Validation"
echo "================================================================"
echo "  Templates:   ${#TEMPLATES[@]}"
echo "  Count each:  $COUNT"
echo "  API URL:     $API_URL"
echo "  Destination: $DESTINATION"
echo "================================================================"
echo

"$(dirname "$0")/verify-strict-013-fixtures.sh"

if ! curl -sf "$API_URL/health" > /dev/null 2>&1; then
  echo "ERROR: mock server is not reachable at $API_URL"
  exit 1
fi

overall_failures=0

for template in "${TEMPLATES[@]}"; do
  echo ">>> Sending template: $template"
  template_failures=0
  for ((i = 1; i <= COUNT; i++)); do
    response="$(curl -sf -X POST "$API_URL/simulate/hl7/$template" -H "Content-Type: application/json" -d "{\"count\":1,\"destination\":\"$DESTINATION\"}")"
    read -r status pushed sent_count <<< "$(python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("status",""), d.get("pushed",""), d.get("count",""))' <<< "$response")"

    if [[ "$status" != "completed" ]]; then
      echo "    FAIL: simulator response status=$status"
      template_failures=$((template_failures + 1))
      continue
    fi

    if [[ "$pushed" == "1" && "$sent_count" == "1" ]]; then
      echo "    OK: message $i/$COUNT received AA ACK via bridge"
    else
      echo "    FAIL: message $i/$COUNT bridge push failed (pushed=$pushed count=$sent_count)"
      template_failures=$((template_failures + 1))
    fi
  done

  if [[ "$template_failures" -gt 0 ]]; then
    overall_failures=$((overall_failures + 1))
  fi
done

echo
echo "================================================================"
if [[ "$overall_failures" -eq 0 ]]; then
  echo "  Strict 013 templates pushed successfully through bridge MLLP."
  echo "  Verify imported results in: https://localhost/AnalyzerResults"
  echo "================================================================"
  exit 0
else
  echo "  $overall_failures template(s) failed."
  echo "================================================================"
  exit 1
fi
