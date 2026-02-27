#!/bin/bash
# test-genexpert-astm.sh — Push GeneXpert ASTM messages via the mock server.
#
# Supports both direct HTTP push to OE and bridge-routed ASTM TCP push.
#
# Usage:
#   ./scripts/test-genexpert-astm.sh                  # 1 message, HTTP push to OE
#   ./scripts/test-genexpert-astm.sh 5                # 5 messages
#   ./scripts/test-genexpert-astm.sh 1 bridge         # 1 message via bridge (ASTM TCP)
#   ./scripts/test-genexpert-astm.sh 3 direct         # 3 messages, HTTP direct to OE
#
# Prerequisites:
#   - Harness running with --profile genexpert
#   - genexpert-simulator healthy on port 8085

set -e

COUNT="${1:-1}"
MODE="${2:-direct}"
API_URL="${3:-http://localhost:8085}"

echo "================================================================"
echo "  GeneXpert ASTM Push Test"
echo "================================================================"
echo "  Messages:  $COUNT"
echo "  Mode:      $MODE"
echo "  API URL:   $API_URL"
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

# Build push request based on mode
if [ "$MODE" = "bridge" ]; then
    echo "Pushing $COUNT GeneXpert ASTM message(s) via bridge (ASTM TCP)..."
    BODY="{\"destination\": \"tcp://openelis-analyzer-bridge:12001\", \"count\": $COUNT}"
elif [ "$MODE" = "direct" ]; then
    echo "Pushing $COUNT GeneXpert ASTM message(s) direct to OE (HTTP)..."
    BODY="{\"destination\": \"https://oe:8443\", \"count\": $COUNT}"
else
    echo "ERROR: Unknown mode '$MODE'. Use 'direct' or 'bridge'."
    exit 1
fi

# Trigger push via simulate endpoint
curl -sf -X POST "$API_URL/simulate/astm/genexpert_astm" \
    -H "Content-Type: application/json" \
    -d "$BODY" | python3 -m json.tool

echo
echo "================================================================"
echo "  Push complete."
echo "  Check analyzer import queue: https://localhost/AnalyzerResults"
echo "================================================================"
