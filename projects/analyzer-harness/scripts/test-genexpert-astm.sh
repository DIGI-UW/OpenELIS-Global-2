#!/bin/bash
# test-genexpert-astm.sh — Push GeneXpert ASTM messages via the mock server.
#
# Usage:
#   ./scripts/test-genexpert-astm.sh                  # 1 message through Bridge
#   ./scripts/test-genexpert-astm.sh 5                # 5 messages
#   ./scripts/test-genexpert-astm.sh 1 DEV01261000000000001
#
# Prerequisites:
#   - Harness running with --profile genexpert
#   - genexpert-simulator healthy on port 8085

set -e

COUNT="${1:-1}"
SAMPLE_ID="${2:-DEV01261000000000001}"
API_URL="${API_URL:-http://localhost:8085}"
BRIDGE_DESTINATION="${BRIDGE_DESTINATION:-tcp://openelis-analyzer-bridge:9600}"

echo "================================================================"
echo "  GeneXpert ASTM Push Test"
echo "================================================================"
echo "  Messages:  $COUNT"
echo "  Sample:    $SAMPLE_ID"
echo "  API URL:   $API_URL"
echo "  Bridge:    $BRIDGE_DESTINATION"
echo "================================================================"
echo

# Health check
echo "Checking genexpert-simulator health..."
if ! curl -sf "$API_URL/health" > /dev/null 2>&1; then
    echo "ERROR: genexpert-simulator not reachable at $API_URL"
    echo "Is the harness running with --profile genexpert?"
    echo ""
    echo "Start with:"
    echo "  docker compose -f docker-compose.dev.yml \\"
    echo "    -f docker-compose.analyzer-test.yml \\"
    echo "    --profile genexpert up -d"
    exit 1
fi
echo "  OK"
echo

echo "Pushing $COUNT GeneXpert ASTM message(s) through Bridge..."
BODY="{\"destination\": \"$BRIDGE_DESTINATION\", \"count\": $COUNT, \"sample_id\": \"$SAMPLE_ID\"}"

# Trigger push via simulate endpoint
curl -sf -X POST "$API_URL/simulate/astm/genexpert" \
    -H "Content-Type: application/json" \
    -d "$BODY" | python3 -m json.tool

echo
echo "================================================================"
echo "  Push complete."
echo "  Review results: https://localhost/AnalyzerResults?id=2"
echo "================================================================"
