#!/bin/bash
#
# run-e2e-like-ci.sh - Run E2E tests EXACTLY like CI does
#
# Usage:
#   ./scripts/run-e2e-like-ci.sh           # Run all tests (fail-fast OFF)
#   E2E_FAIL_FAST=true ./scripts/run-e2e-like-ci.sh  # Run with fail-fast (dev mode)
#   ./scripts/run-e2e-like-ci.sh --spec "cypress/e2e/analyzer*.cy.js"  # Run specific tests
#

set -e  # Exit on error

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Running E2E Tests (CI Replication Mode)${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Get the project root (one directory up from scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# Step 1: Verify Docker containers are running
echo -e "${YELLOW}[1/5] Checking Docker containers...${NC}"
if ! docker ps | grep -q "openelisglobal-webapp"; then
  echo -e "${RED}ERROR: Docker containers not running${NC}"
  echo "Start containers with:"
  echo "  docker compose -f build.docker-compose.yml up -d --build --wait --wait-timeout 600"
  exit 1
fi

if ! docker ps | grep "openelisglobal-database" | grep -q "healthy"; then
  echo -e "${YELLOW}WARNING: Database not yet healthy, waiting 30 seconds...${NC}"
  sleep 30
fi
echo -e "${GREEN}✓ Containers running${NC}"
echo ""

# Step 2: Load test fixtures (EXACTLY like CI - same files and ON_ERROR_STOP=on)
echo -e "${YELLOW}[2/5] Loading test fixtures (CI parity)...${NC}"
./scripts/load-ci-fixtures.sh -f build.docker-compose.yml
echo -e "${GREEN}✓ Fixtures loaded${NC}"
echo ""

# Step 3: Configure fail-fast mode
echo -e "${YELLOW}[3/5] Checking E2E_FAIL_FAST setting...${NC}"
if [ "$E2E_FAIL_FAST" = "true" ]; then
  export CYPRESS_FAIL_FAST_ENABLED=true
  echo -e "${GREEN}  Fail-fast: ENABLED (will stop on first failure)${NC}"
else
  echo -e "${YELLOW}  Fail-fast: DISABLED (will run all tests)${NC}"
  echo "  Set E2E_FAIL_FAST=true to enable fail-fast"
fi
echo ""

# Step 4: Install dependencies (if needed)
echo -e "${YELLOW}[4/5] Checking frontend dependencies...${NC}"
cd frontend
if [ ! -d "node_modules" ]; then
  echo "  Installing dependencies with npm ci (lockfile-faithful, like CI)..."
  if [ -f "package-lock.json" ]; then
    npm ci > /dev/null 2>&1
  else
    echo -e "${RED}ERROR: package-lock.json not found in ./frontend${NC}"
    echo "  Run 'npm install' once and commit the lockfile, then re-run this script."
    exit 1
  fi
fi
echo -e "${GREEN}✓ Dependencies ready${NC}"
echo ""

# Step 5: Run Cypress EXACTLY like CI
echo -e "${YELLOW}[5/5] Running Cypress E2E tests...${NC}"
echo "  Command: npx cypress run --browser chrome --headless $@"
echo ""

# Ensure ELECTRON_RUN_AS_NODE doesn't break Cypress (same workaround as npm cy:* scripts)
unset ELECTRON_RUN_AS_NODE

# Run Cypress directly (not via npm cy:* scripts) to match the CI workflow command
# and allow arbitrary $@ forwarding without conflicting with script-embedded flags.
if npx cypress run --browser chrome --headless "$@"; then
  echo ""
  echo -e "${GREEN}========================================${NC}"
  echo -e "${GREEN}✓ All E2E tests PASSED${NC}"
  echo -e "${GREEN}========================================${NC}"
  exit 0
else
  echo ""
  echo -e "${RED}========================================${NC}"
  echo -e "${RED}✗ E2E tests FAILED${NC}"
  echo -e "${RED}========================================${NC}"
  echo ""
  echo "Screenshots saved to: frontend/cypress/screenshots/"
  echo ""
  echo "To debug a specific test:"
  echo "  ./scripts/run-e2e-like-ci.sh --spec \"cypress/e2e/TESTNAME.cy.js\""
  echo ""
  echo "To enable fail-fast (stop on first failure):"
  echo "  E2E_FAIL_FAST=true ./scripts/run-e2e-like-ci.sh"
  exit 1
fi
