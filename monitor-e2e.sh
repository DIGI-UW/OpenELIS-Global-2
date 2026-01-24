#!/bin/bash
# Monitor E2E test execution progress

LOG_FILE="/tmp/e2e-execution-v2.log"
REPORT_PATTERN="/tmp/e2e-analysis-*"

echo "=========================================="
echo "E2E Test Execution Monitor"
echo "=========================================="
echo ""

# Check if tests are running
if pgrep -f "run-e2e-chunks.sh" > /dev/null; then
  echo "✅ Tests are RUNNING"
  echo ""
else
  echo "⏸️  Tests are NOT running (may have completed or not started)"
  echo ""
fi

# Find latest report
LATEST_REPORT=$(ls -td $REPORT_PATTERN 2>/dev/null | head -1)

if [ -z "$LATEST_REPORT" ]; then
  echo "⚠️  No report directory found yet"
  exit 0
fi

echo "Report directory: $LATEST_REPORT"
echo ""

# Count completed chunks
COMPLETED=$(find "$LATEST_REPORT" -name "chunk-*.result" 2>/dev/null | wc -l)
PASSED=$(grep -l "PASSED" "$LATEST_REPORT"/chunk-*.result 2>/dev/null | wc -l)
FAILED=$(grep -l "FAILED" "$LATEST_REPORT"/chunk-*.result 2>/dev/null | wc -l)

echo "Progress: $COMPLETED/12 chunks completed"
echo "  ✅ Passed: $PASSED"
echo "  ❌ Failed: $FAILED"
echo ""

# Show recent activity
if [ -f "$LOG_FILE" ]; then
  echo "Recent activity:"
  tail -20 "$LOG_FILE" | grep -E "(CHUNK|→ Running|✓|✖)" | tail -10
fi

echo ""
echo "To view full progress: tail -f $LOG_FILE"
echo "To analyze failures: ./analyze-e2e-failures.sh"
