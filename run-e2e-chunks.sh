#!/bin/bash
# Run E2E tests in manageable chunks, analyze failures, and document issues
# Mirrors CI execution: npx cypress run --browser chrome --headless

set -e

FRONTEND_DIR="/home/ubuntu/OpenELIS-Global-2/frontend"
REPORT_DIR="/tmp/e2e-analysis-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$REPORT_DIR"

# Test chunks (5-10 tests per chunk for manageable execution)
CHUNKS=(
  "cypress/e2e/login.cy.js cypress/e2e/home.cy.js"
  "cypress/e2e/AdminE2E/organizationManagement.cy.js cypress/e2e/AdminE2E/providerManagement.cy.js cypress/e2e/patientEntry.cy.js cypress/e2e/orderEntity.cy.js"
  "cypress/e2e/AdminE2E/barcode.cy.js cypress/e2e/AdminE2E/batchTestReassignmentandCancelation.cy.js cypress/e2e/AdminE2E/calculatedValueTestsManagement.cy.js cypress/e2e/AdminE2E/dictionaryMenu.cy.js"
  "cypress/e2e/AdminE2E/generalConfigurations.cy.js cypress/e2e/AdminE2E/notifyUser.cy.js cypress/e2e/AdminE2E/programEntry.cy.js cypress/e2e/AdminE2E/reflexTestsManagement.cy.js"
  "cypress/e2e/AdminE2E/resultReportingConfig.cy.js cypress/e2e/AdminE2E/testManagement.cy.js cypress/e2e/AdminE2E/userManagement.cy.js cypress/e2e/batchOrderEntry.cy.js"
  "cypress/e2e/dashboard.cy.js cypress/e2e/help.cy.js cypress/e2e/labNumberManagement.cy.js cypress/e2e/modifyOrder.cy.js cypress/e2e/nonConform.cy.js"
  "cypress/e2e/notebookWorkflow.cy.js cypress/e2e/notebookWorkflowArchiving.cy.js cypress/e2e/notebookWorkflowRouting.cy.js cypress/e2e/notebookWorkflowStorage.cy.js"
  "cypress/e2e/patientMerge.cy.js cypress/e2e/report.cy.js cypress/e2e/result.cy.js cypress/e2e/validation.cy.js cypress/e2e/workplan.cy.js"
  "cypress/e2e/storageAssignment.cy.js cypress/e2e/storageBoxCRUD.cy.js cypress/e2e/storageBoxCRUD-integration.cy.js cypress/e2e/storageDashboard.cy.js"
  "cypress/e2e/storageDashboardMetrics.cy.js cypress/e2e/storageDisposal.cy.js cypress/e2e/storageFilters.cy.js cypress/e2e/storageLocationCRUD-smoke.cy.js"
  "cypress/e2e/storageLocationCRUD.cy.js cypress/e2e/storageLocationCRUD-integration.cy.js cypress/e2e/storageLocationExpandableRows.cy.js cypress/e2e/storageSamplesTable.cy.js"
  "cypress/e2e/storageSearch.cy.js cypress/e2e/storageViewStorage.cy.js"
)

cd "$FRONTEND_DIR"

echo "=========================================="
echo "E2E Test Execution - Chunked Analysis"
echo "=========================================="
echo "Report directory: $REPORT_DIR"
echo "Total chunks: ${#CHUNKS[@]}"
echo ""

CHUNK_NUM=1
TOTAL_FAILURES=0
FAILED_CHUNKS=()

for CHUNK in "${CHUNKS[@]}"; do
  echo "----------------------------------------"
  echo "CHUNK $CHUNK_NUM/${#CHUNKS[@]}: Running tests..."
  echo "Tests: $CHUNK"
  echo ""
  
  CHUNK_LOG="$REPORT_DIR/chunk-${CHUNK_NUM}.log"
  CHUNK_RESULT="$REPORT_DIR/chunk-${CHUNK_NUM}.result"
  CHUNK_FAILED=false
  
  # Run each test file in chunk individually (Cypress doesn't support multiple --spec flags well)
  for TEST_FILE in $CHUNK; do
    echo "  → Running: $TEST_FILE"
    TEST_LOG="$REPORT_DIR/chunk-${CHUNK_NUM}-$(basename $TEST_FILE .cy.js).log"
    
    if xvfb-run -a npx cypress run --browser chrome --headless --spec "$TEST_FILE" >> "$CHUNK_LOG" 2>&1; then
      echo "    ✓ PASSED" | tee -a "$CHUNK_LOG"
    else
      EXIT_CODE=$?
      echo "    ✖ FAILED (exit code: $EXIT_CODE)" | tee -a "$CHUNK_LOG"
      CHUNK_FAILED=true
      
      # Copy screenshots for failed test
      if [ -d "cypress/screenshots" ] && [ "$(ls -A cypress/screenshots 2>/dev/null)" ]; then
        mkdir -p "$REPORT_DIR/chunk-${CHUNK_NUM}-screenshots"
        cp -r cypress/screenshots/* "$REPORT_DIR/chunk-${CHUNK_NUM}-screenshots/" 2>/dev/null || true
      fi
    fi
  done
  
  if [ "$CHUNK_FAILED" = true ]; then
    echo "❌ CHUNK $CHUNK_NUM: FAILED"
    echo "FAILED" > "$CHUNK_RESULT"
    TOTAL_FAILURES=$((TOTAL_FAILURES + 1))
    FAILED_CHUNKS+=("$CHUNK_NUM")
  else
    echo "✅ CHUNK $CHUNK_NUM: PASSED"
    echo "PASSED" > "$CHUNK_RESULT"
  fi
  
  # Extract summary from log
  echo "" >> "$CHUNK_RESULT"
  tail -50 "$CHUNK_LOG" | grep -E "(Tests:|Passing|Failing|Pending|Skipped|✓|✖|Spec Ran)" >> "$CHUNK_RESULT" || true
  
  CHUNK_NUM=$((CHUNK_NUM + 1))
  echo ""
done

# Generate summary report
SUMMARY_FILE="$REPORT_DIR/SUMMARY.md"
cat > "$SUMMARY_FILE" <<EOF
# E2E Test Execution Summary

**Date:** $(date)
**Total Chunks:** ${#CHUNKS[@]}
**Failed Chunks:** ${TOTAL_FAILURES}
**Success Rate:** $(( (${#CHUNKS[@]} - TOTAL_FAILURES) * 100 / ${#CHUNKS[@]} ))%

## Failed Chunks

EOF

if [ ${#FAILED_CHUNKS[@]} -eq 0 ]; then
  echo "✅ All chunks passed!" >> "$SUMMARY_FILE"
else
  for CHUNK_NUM in "${FAILED_CHUNKS[@]}"; do
    echo "### Chunk $CHUNK_NUM" >> "$SUMMARY_FILE"
    echo "" >> "$SUMMARY_FILE"
    echo "**Tests:** \`${CHUNKS[$((CHUNK_NUM - 1))]}\`" >> "$SUMMARY_FILE"
    echo "" >> "$SUMMARY_FILE"
    echo "**Result:**" >> "$SUMMARY_FILE"
    cat "$REPORT_DIR/chunk-${CHUNK_NUM}.result" >> "$SUMMARY_FILE"
    echo "" >> "$SUMMARY_FILE"
    echo "**Screenshots:** \`$REPORT_DIR/chunk-${CHUNK_NUM}-screenshots/\`" >> "$SUMMARY_FILE"
    echo "" >> "$SUMMARY_FILE"
    echo "**Full Log:** \`$REPORT_DIR/chunk-${CHUNK_NUM}.log\`" >> "$SUMMARY_FILE"
    echo "" >> "$SUMMARY_FILE"
  done
fi

echo ""
echo "=========================================="
echo "EXECUTION COMPLETE"
echo "=========================================="
echo "Summary: $SUMMARY_FILE"
echo "Failed chunks: ${FAILED_CHUNKS[*]}"
echo "Total failures: $TOTAL_FAILURES"
echo ""
echo "To analyze failures:"
echo "  cat $SUMMARY_FILE"
echo "  ls -la $REPORT_DIR/chunk-*-screenshots/"
echo ""

cat "$SUMMARY_FILE"
