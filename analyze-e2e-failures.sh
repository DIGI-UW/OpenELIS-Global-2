#!/bin/bash
# Comprehensive E2E failure analysis and documentation
# Run after E2E chunk execution completes

REPORT_DIR="${1:-/tmp/e2e-analysis-*}"

# Find latest report directory
LATEST_REPORT=$(ls -td $REPORT_DIR 2>/dev/null | head -1)

if [ -z "$LATEST_REPORT" ] || [ ! -d "$LATEST_REPORT" ]; then
  echo "Error: No report directory found. Expected pattern: $REPORT_DIR"
  echo "Available: $(ls -td /tmp/e2e-analysis-* 2>/dev/null | head -5)"
  exit 1
fi

ANALYSIS_FILE="$LATEST_REPORT/FULL_ANALYSIS.md"

cat > "$ANALYSIS_FILE" <<'EOF'
# E2E Test Failure Analysis

**Generated:** $(date)
**Report Directory:** $LATEST_REPORT

## Executive Summary

EOF

# Count failures
TOTAL_CHUNKS=$(find "$LATEST_REPORT" -name "chunk-*.result" | wc -l)
FAILED_CHUNKS=$(grep -l "FAILED" "$LATEST_REPORT"/chunk-*.result 2>/dev/null | wc -l)
PASSED_CHUNKS=$((TOTAL_CHUNKS - FAILED_CHUNKS))

cat >> "$ANALYSIS_FILE" <<EOF
- **Total Chunks:** $TOTAL_CHUNKS
- **Passed:** $PASSED_CHUNKS
- **Failed:** $FAILED_CHUNKS
- **Success Rate:** $(( PASSED_CHUNKS * 100 / TOTAL_CHUNKS ))%

## Failure Categories

EOF

# Categorize failures
echo "### 1. Authentication/Login Failures" >> "$ANALYSIS_FILE"
grep -l "FAILED" "$LATEST_REPORT"/chunk-*.result 2>/dev/null | while read result_file; do
  chunk_num=$(basename "$result_file" | sed 's/chunk-\([0-9]*\)\.result/\1/')
  log_file="$LATEST_REPORT/chunk-${chunk_num}.log"
  if grep -qi "login\|auth\|unauthorized\|401\|403" "$log_file" 2>/dev/null; then
    echo "- Chunk $chunk_num: Authentication issue" >> "$ANALYSIS_FILE"
  fi
done

echo "" >> "$ANALYSIS_FILE"
echo "### 2. Timeout Failures" >> "$ANALYSIS_FILE"
grep -l "FAILED" "$LATEST_REPORT"/chunk-*.result 2>/dev/null | while read result_file; do
  chunk_num=$(basename "$result_file" | sed 's/chunk-\([0-9]*\)\.result/\1/')
  log_file="$LATEST_REPORT/chunk-${chunk_num}.log"
  if grep -qi "timeout\|timed out\|waiting\|exceeded" "$log_file" 2>/dev/null; then
    echo "- Chunk $chunk_num: Timeout issue" >> "$ANALYSIS_FILE"
  fi
done

echo "" >> "$ANALYSIS_FILE"
echo "### 3. Element Not Found Failures" >> "$ANALYSIS_FILE"
grep -l "FAILED" "$LATEST_REPORT"/chunk-*.result 2>/dev/null | while read result_file; do
  chunk_num=$(basename "$result_file" | sed 's/chunk-\([0-9]*\)\.result/\1/')
  log_file="$LATEST_REPORT/chunk-${chunk_num}.log"
  if grep -qi "element.*not found\|could not find\|not visible\|not exist" "$log_file" 2>/dev/null; then
    echo "- Chunk $chunk_num: Element not found" >> "$ANALYSIS_FILE"
  fi
done

echo "" >> "$ANALYSIS_FILE"
echo "### 4. Network/API Failures" >> "$ANALYSIS_FILE"
grep -l "FAILED" "$LATEST_REPORT"/chunk-*.result 2>/dev/null | while read result_file; do
  chunk_num=$(basename "$result_file" | sed 's/chunk-\([0-9]*\)\.result/\1/')
  log_file="$LATEST_REPORT/chunk-${chunk_num}.log"
  if grep -qi "network\|api\|fetch\|xhr\|500\|502\|503\|504" "$log_file" 2>/dev/null; then
    echo "- Chunk $chunk_num: Network/API issue" >> "$ANALYSIS_FILE"
  fi
done

echo "" >> "$ANALYSIS_FILE"
echo "## Detailed Failure Reports" >> "$ANALYSIS_FILE"
echo "" >> "$ANALYSIS_FILE"

# Generate detailed report for each failed chunk
for result_file in "$LATEST_REPORT"/chunk-*.result; do
  if grep -q "FAILED" "$result_file" 2>/dev/null; then
    chunk_num=$(basename "$result_file" | sed 's/chunk-\([0-9]*\)\.result/\1/')
    log_file="$LATEST_REPORT/chunk-${chunk_num}.log"

    echo "### Chunk $chunk_num" >> "$ANALYSIS_FILE"
    echo "" >> "$ANALYSIS_FILE"

    # Extract test files from chunk
    echo "**Test Files:**" >> "$ANALYSIS_FILE"
    grep "→ Running:" "$log_file" 2>/dev/null | sed 's/.*→ Running: /- /' >> "$ANALYSIS_FILE" || echo "- (unknown)" >> "$ANALYSIS_FILE"
    echo "" >> "$ANALYSIS_FILE"

    # Extract error messages
    echo "**Errors:**" >> "$ANALYSIS_FILE"
    grep -E "(Error|FAIL|✖|AssertionError|Timed out)" "$log_file" 2>/dev/null | head -10 | sed 's/^/  - /' >> "$ANALYSIS_FILE" || echo "  - (no specific errors found)" >> "$ANALYSIS_FILE"
    echo "" >> "$ANALYSIS_FILE"

    # Screenshot info
    screenshot_dir="$LATEST_REPORT/chunk-${chunk_num}-screenshots"
    if [ -d "$screenshot_dir" ] && [ "$(ls -A "$screenshot_dir" 2>/dev/null)" ]; then
      echo "**Screenshots:** \`$screenshot_dir\`" >> "$ANALYSIS_FILE"
      find "$screenshot_dir" -name "*.png" | head -5 | sed 's|.*/|  - |' >> "$ANALYSIS_FILE"
      echo "" >> "$ANALYSIS_FILE"
    fi

    echo "**Full Log:** \`$log_file\`" >> "$ANALYSIS_FILE"
    echo "" >> "$ANALYSIS_FILE"
    echo "---" >> "$ANALYSIS_FILE"
    echo "" >> "$ANALYSIS_FILE"
  fi
done

echo "" >> "$ANALYSIS_FILE"
echo "## Recommended Solutions" >> "$ANALYSIS_FILE"
echo "" >> "$ANALYSIS_FILE"
echo "### Common Fixes" >> "$ANALYSIS_FILE"
echo "" >> "$ANALYSIS_FILE"
echo "1. **Authentication Failures:**" >> "$ANALYSIS_FILE"
echo "   - Verify login credentials in test fixtures" >> "$ANALYSIS_FILE"
echo "   - Check session management (cy.session() usage)" >> "$ANALYSIS_FILE"
echo "   - Verify application is fully started before tests run" >> "$ANALYSIS_FILE"
echo "" >> "$ANALYSIS_FILE"
echo "2. **Timeout Failures:**" >> "$ANALYSIS_FILE"
echo "   - Increase defaultCommandTimeout in cypress.config.js" >> "$ANALYSIS_FILE"
echo "   - Add explicit waits for slow-loading elements" >> "$ANALYSIS_FILE"
echo "   - Check application performance" >> "$ANALYSIS_FILE"
echo "" >> "$ANALYSIS_FILE"
echo "3. **Element Not Found:**" >> "$ANALYSIS_FILE"
echo "   - Verify selectors match current UI" >> "$ANALYSIS_FILE"
echo "   - Check if elements are conditionally rendered" >> "$ANALYSIS_FILE"
echo "   - Add data-testid attributes for stable selectors" >> "$ANALYSIS_FILE"
echo "" >> "$ANALYSIS_FILE"
echo "4. **Network/API Failures:**" >> "$ANALYSIS_FILE"
echo "   - Verify backend services are running" >> "$ANALYSIS_FILE"
echo "   - Check API endpoints are accessible" >> "$ANALYSIS_FILE"
echo "   - Review network intercepts in tests" >> "$ANALYSIS_FILE"
echo "" >> "$ANALYSIS_FILE"

# Replace date placeholder
sed -i "s/\$(date)/$(date)/" "$ANALYSIS_FILE"
sed -i "s|\\\$LATEST_REPORT|$LATEST_REPORT|g" "$ANALYSIS_FILE"

cat "$ANALYSIS_FILE"
echo ""
echo "Full analysis saved to: $ANALYSIS_FILE"
