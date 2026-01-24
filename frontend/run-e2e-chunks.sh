#!/bin/bash
#
# Run E2E tests in manageable chunks, mirroring CI behavior
# Analyzes failures and screenshots, documents issues
#
# Usage: ./run-e2e-chunks.sh [chunk-size] [start-from]
#   chunk-size: Number of test files per chunk (default: 5)
#   start-from: Test file index to start from (default: 0)
#
# Example: ./run-e2e-chunks.sh 5 0  # Run first 5 tests
#          ./run-e2e-chunks.sh 5 5  # Run next 5 tests

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

CHUNK_SIZE=${1:-5}
START_FROM=${2:-0}
REPORT_DIR="cypress/e2e-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create report directory
mkdir -p "$REPORT_DIR"

# Get list of all test files (matching CI order from cypress.config.js)
TEST_FILES=(
  "cypress/e2e/login.cy.js"
  "cypress/e2e/home.cy.js"
  "cypress/e2e/AdminE2E/organizationManagement.cy.js"
  "cypress/e2e/AdminE2E/providerManagement.cy.js"
  "cypress/e2e/patientEntry.cy.js"
  "cypress/e2e/orderEntity.cy.js"
  "cypress/e2e/AdminE2E/barcode.cy.js"
  "cypress/e2e/AdminE2E/batchTestReassignmentandCancelation.cy.js"
  "cypress/e2e/AdminE2E/calculatedValueTestsManagement.cy.js"
  "cypress/e2e/AdminE2E/dictionaryMenu.cy.js"
  "cypress/e2e/AdminE2E/generalConfigurations.cy.js"
  "cypress/e2e/AdminE2E/MenuConfig/billingMenuConfig.cy.js"
  "cypress/e2e/AdminE2E/MenuConfig/globalMenuConfig.cy.js"
  "cypress/e2e/AdminE2E/MenuConfig/nonConformMenuConfig.cy.js"
  "cypress/e2e/AdminE2E/MenuConfig/patientMenuConfig.cy.js"
  "cypress/e2e/AdminE2E/MenuConfig/studyMenuConfig.cy.js"
  "cypress/e2e/AdminE2E/notifyUser.cy.js"
  "cypress/e2e/AdminE2E/programEntry.cy.js"
  "cypress/e2e/AdminE2E/reflexTestsManagement.cy.js"
  "cypress/e2e/AdminE2E/resultReportingConfig.cy.js"
  "cypress/e2e/AdminE2E/testManagement.cy.js"
  "cypress/e2e/AdminE2E/userManagement.cy.js"
  "cypress/e2e/batchOrderEntry.cy.js"
  "cypress/e2e/dashboard.cy.js"
  "cypress/e2e/help.cy.js"
  "cypress/e2e/labNumberManagement.cy.js"
  "cypress/e2e/modifyOrder.cy.js"
  "cypress/e2e/nonConform.cy.js"
  "cypress/e2e/notebookWorkflow.cy.js"
  "cypress/e2e/notebookWorkflowArchiving.cy.js"
  "cypress/e2e/notebookWorkflowRouting.cy.js"
  "cypress/e2e/notebookWorkflowStorage.cy.js"
  "cypress/e2e/patientMerge.cy.js"
  "cypress/e2e/report.cy.js"
  "cypress/e2e/result.cy.js"
  "cypress/e2e/storageAssignment.cy.js"
  "cypress/e2e/storageBoxCRUD-integration.cy.js"
  "cypress/e2e/storageBoxCRUD.cy.js"
  "cypress/e2e/storageDashboard.cy.js"
  "cypress/e2e/storageDashboardMetrics.cy.js"
  "cypress/e2e/storageDisposal.cy.js"
  "cypress/e2e/storageFilters.cy.js"
  "cypress/e2e/storageLocationCRUD-integration.cy.js"
  "cypress/e2e/storageLocationCRUD-smoke.cy.js"
  "cypress/e2e/storageLocationCRUD.cy.js"
  "cypress/e2e/storageLocationExpandableRows.cy.js"
  "cypress/e2e/storageSamplesTable.cy.js"
  "cypress/e2e/storageSearch.cy.js"
  "cypress/e2e/storageViewStorage.cy.js"
  "cypress/e2e/validation.cy.js"
  "cypress/e2e/workplan.cy.js"
)

TOTAL_TESTS=${#TEST_FILES[@]}
END_INDEX=$((START_FROM + CHUNK_SIZE))

if [ $END_INDEX -gt $TOTAL_TESTS ]; then
  END_INDEX=$TOTAL_TESTS
fi

echo "=========================================="
echo "E2E Test Runner (CI-Compatible)"
echo "=========================================="
echo "Total tests: $TOTAL_TESTS"
echo "Chunk size: $CHUNK_SIZE"
echo "Running tests: $START_FROM to $((END_INDEX - 1))"
echo "Report directory: $REPORT_DIR"
echo "=========================================="
echo ""

# Extract chunk of tests
CHUNK_TESTS=("${TEST_FILES[@]:START_FROM:CHUNK_SIZE}")

# Build spec pattern
SPEC_PATTERN=""
for test in "${CHUNK_TESTS[@]}"; do
  if [ -z "$SPEC_PATTERN" ]; then
    SPEC_PATTERN="$test"
  else
    SPEC_PATTERN="$SPEC_PATTERN,$test"
  fi
done

LOG_FILE="$REPORT_DIR/chunk_${START_FROM}_to_$((END_INDEX - 1))_${TIMESTAMP}.log"
SCREENSHOT_DIR="cypress/screenshots"

# Clean previous screenshots for this chunk
rm -rf "$SCREENSHOT_DIR"/* 2>/dev/null || true

echo "Running tests..."
echo "Log file: $LOG_FILE"
echo ""

# Run tests with xvfb-run (headless) matching CI command
# CI uses: npx cypress run --browser chrome --headless
xvfb-run -a npx cypress run \
  --browser chrome \
  --headless \
  --spec "$SPEC_PATTERN" \
  2>&1 | tee "$LOG_FILE"

EXIT_CODE=${PIPESTATUS[0]}

echo ""
echo "=========================================="
echo "Test Execution Complete"
echo "=========================================="
echo "Exit code: $EXIT_CODE"
echo ""

# Analyze results
if [ $EXIT_CODE -ne 0 ]; then
  echo "❌ TESTS FAILED - Analyzing failures..."
  echo ""
  
  # Count failures
  FAILURE_COUNT=$(grep -c "failing\|failed\|FAIL" "$LOG_FILE" 2>/dev/null || echo "0")
  echo "Failures detected: $FAILURE_COUNT"
  
  # Check for screenshots
  SCREENSHOT_COUNT=$(find "$SCREENSHOT_DIR" -name "*.png" 2>/dev/null | wc -l)
  echo "Screenshots captured: $SCREENSHOT_COUNT"
  
  if [ $SCREENSHOT_COUNT -gt 0 ]; then
    echo ""
    echo "Screenshot locations:"
    find "$SCREENSHOT_DIR" -name "*.png" -type f | while read -r screenshot; do
      echo "  - $screenshot"
    done
  fi
  
  # Extract error details
  ERROR_REPORT="$REPORT_DIR/errors_${TIMESTAMP}.md"
  echo ""
  echo "Generating error report: $ERROR_REPORT"
  
  {
    echo "# E2E Test Failure Report"
    echo "Generated: $(date)"
    echo ""
    echo "## Test Chunk"
    echo "- Tests: $START_FROM to $((END_INDEX - 1))"
    echo "- Files: ${CHUNK_TESTS[*]}"
    echo ""
    echo "## Failures"
    echo ""
    grep -A 10 "failing\|failed\|FAIL\|Error:" "$LOG_FILE" | head -100 || echo "No detailed errors found in log"
    echo ""
    echo "## Screenshots"
    if [ $SCREENSHOT_COUNT -gt 0 ]; then
      find "$SCREENSHOT_DIR" -name "*.png" -type f | while read -r screenshot; do
        echo "- \`$screenshot\`"
      done
    else
      echo "No screenshots found"
    fi
    echo ""
    echo "## Full Log"
    echo "See: \`$LOG_FILE\`"
  } > "$ERROR_REPORT"
  
  echo "Error report saved to: $ERROR_REPORT"
  echo ""
  echo "Next steps:"
  echo "1. Review screenshots in: $SCREENSHOT_DIR"
  echo "2. Review error report: $ERROR_REPORT"
  echo "3. Review full log: $LOG_FILE"
  echo "4. Fix issues and re-run: ./run-e2e-chunks.sh $CHUNK_SIZE $START_FROM"
else
  echo "✅ ALL TESTS PASSED"
  echo ""
  echo "Next chunk: ./run-e2e-chunks.sh $CHUNK_SIZE $END_INDEX"
fi

exit $EXIT_CODE
