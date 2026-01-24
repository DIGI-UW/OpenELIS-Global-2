#!/bin/bash
#
# Analyze E2E test failures from screenshots and logs
# Generates detailed failure reports with solutions
#
# Usage: ./analyze-e2e-failures.sh [report-dir]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REPORT_DIR=${1:-"cypress/e2e-reports"}
SCREENSHOT_DIR="cypress/screenshots"
ANALYSIS_FILE="$REPORT_DIR/failure_analysis_$(date +%Y%m%d_%H%M%S).md"

mkdir -p "$REPORT_DIR"

echo "Analyzing E2E test failures..."
echo "Report directory: $REPORT_DIR"
echo "Screenshot directory: $SCREENSHOT_DIR"
echo ""

# Find all error logs
ERROR_LOGS=($(find "$REPORT_DIR" -name "*.log" -type f | sort))
SCREENSHOTS=($(find "$SCREENSHOT_DIR" -name "*.png" -type f 2>/dev/null | sort || true))

{
  echo "# E2E Test Failure Analysis"
  echo "Generated: $(date)"
  echo ""
  echo "## Summary"
  echo ""
  echo "- Error logs found: ${#ERROR_LOGS[@]}"
  echo "- Screenshots found: ${#SCREENSHOTS[@]}"
  echo ""
  
  if [ ${#ERROR_LOGS[@]} -eq 0 ] && [ ${#SCREENSHOTS[@]} -eq 0 ]; then
    echo "✅ No failures detected!"
    exit 0
  fi
  
  echo "## Error Logs Analysis"
  echo ""
  
  for log in "${ERROR_LOGS[@]}"; do
    echo "### $(basename "$log")"
    echo ""
    
    # Extract test failures
    FAILURES=$(grep -E "failing|failed|FAIL|Error:|AssertionError|Timeout" "$log" 2>/dev/null | head -20 || true)
    
    if [ -n "$FAILURES" ]; then
      echo "**Failures detected:**"
      echo '```'
      echo "$FAILURES"
      echo '```'
      echo ""
    else
      echo "No failures found in this log."
      echo ""
    fi
    
    # Extract test names that failed
    FAILED_TESTS=$(grep -E "Running:|failing|failed" "$log" 2>/dev/null | grep -E "Running:" | tail -5 || true)
    if [ -n "$FAILED_TESTS" ]; then
      echo "**Tests that ran:**"
      echo '```'
      echo "$FAILED_TESTS"
      echo '```'
      echo ""
    fi
  done
  
  echo "## Screenshot Analysis"
  echo ""
  
  if [ ${#SCREENSHOTS[@]} -gt 0 ]; then
    echo "Found ${#SCREENSHOTS[@]} screenshot(s):"
    echo ""
    
    for screenshot in "${SCREENSHOTS[@]}"; do
      echo "### $(basename "$screenshot")"
      echo ""
      echo "- **Path:** \`$screenshot\`"
      echo "- **Size:** $(du -h "$screenshot" | cut -f1)"
      echo ""
      
      # Try to extract test name from path
      TEST_NAME=$(echo "$screenshot" | sed -E 's|.*/([^/]+)\.cy\.js.*|\1|' || echo "unknown")
      echo "- **Test:** $TEST_NAME"
      echo ""
    done
  else
    echo "No screenshots found."
    echo ""
  fi
  
  echo "## Common Failure Patterns"
  echo ""
  
  # Analyze common patterns
  TIMEOUT_COUNT=$(grep -c "timeout\|Timeout\|TIMEOUT" "$REPORT_DIR"/*.log 2>/dev/null || echo "0")
  ELEMENT_NOT_FOUND=$(grep -c "element.*not found\|Element.*not found\|getElementById\|querySelector" "$REPORT_DIR"/*.log 2>/dev/null || echo "0")
  NETWORK_ERROR=$(grep -c "network\|Network\|ECONNREFUSED\|404\|500" "$REPORT_DIR"/*.log 2>/dev/null || echo "0")
  
  echo "- Timeout errors: $TIMEOUT_COUNT"
  echo "- Element not found: $ELEMENT_NOT_FOUND"
  echo "- Network errors: $NETWORK_ERROR"
  echo ""
  
  echo "## Proposed Solutions"
  echo ""
  
  if [ "$TIMEOUT_COUNT" -gt 0 ]; then
    echo "### Timeout Issues"
    echo ""
    echo "**Problem:** Tests timing out waiting for elements or actions"
    echo ""
    echo "**Solutions:**"
    echo "1. Increase \`defaultCommandTimeout\` in cypress.config.js (currently 3000ms)"
    echo "2. Use Cypress retry-ability with \`.should()\` instead of \`cy.wait()\`"
    echo "3. Check if application is slow to load - verify Docker containers are running"
    echo "4. Add explicit waits for slow operations (e.g., API calls)"
    echo ""
  fi
  
  if [ "$ELEMENT_NOT_FOUND" -gt 0 ]; then
    echo "### Element Not Found Issues"
    echo ""
    echo "**Problem:** Cypress cannot find DOM elements"
    echo ""
    echo "**Solutions:**"
    echo "1. Verify selectors are correct (use data-testid attributes)"
    echo "2. Check if elements are rendered (may need to wait for React to render)"
    echo "3. Ensure viewport size matches expected (currently 1920x1080)"
    echo "4. Check if elements are hidden or in modals that need to be opened"
    echo ""
  fi
  
  if [ "$NETWORK_ERROR" -gt 0 ]; then
    echo "### Network Issues"
    echo ""
    echo "**Problem:** API calls failing or application not accessible"
    echo ""
    echo "**Solutions:**"
    echo "1. Verify Docker containers are running: \`docker compose ps\`"
    echo "2. Check application is accessible: \`curl https://localhost\`"
    echo "3. Verify baseUrl in cypress.config.js matches running application"
    echo "4. Check for SSL certificate issues (self-signed cert warnings)"
    echo ""
  fi
  
  echo "## Next Steps"
  echo ""
  echo "1. Review screenshots manually to understand UI state at failure"
  echo "2. Check browser console logs in test output for JavaScript errors"
  echo "3. Verify test data fixtures are loaded correctly"
  echo "4. Run individual failing tests with: \`npx cypress run --spec <test-file>\`"
  echo "5. Enable video recording for debugging: Set \`video: true\` in cypress.config.js"
  echo ""
  
} > "$ANALYSIS_FILE"

echo "Analysis complete!"
echo "Report saved to: $ANALYSIS_FILE"
echo ""
cat "$ANALYSIS_FILE"
