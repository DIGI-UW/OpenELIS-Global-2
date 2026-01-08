#!/bin/bash
# Script to run frontend CI checks locally before pushing
# This replicates the GitHub Actions frontend QA workflow (.github/workflows/frontend-qa.yml)
#
# Usage:
#   ./scripts/run-frontend-ci-checks.sh          # Run all checks
#   ./scripts/run-frontend-ci-checks.sh --skip-e2e  # Skip E2E tests (faster)

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse arguments
SKIP_E2E=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-e2e)
            SKIP_E2E=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--skip-e2e]"
            exit 1
            ;;
    esac
done

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Running Frontend CI Checks Locally${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if we're in the right directory
if [ ! -d "frontend" ]; then
    echo -e "${RED}Error: frontend directory not found${NC}"
    echo "Please run this script from the project root directory"
    exit 1
fi

cd frontend

# Step 1: Check formatting
echo -e "${YELLOW}[1/3] Checking frontend code formatting...${NC}"
# Use prettier check command (matches CI workflow)
if npx prettier ./ --check; then
    echo -e "${GREEN}✓ Formatting check passed${NC}"
else
    echo -e "${RED}✗ Formatting check failed${NC}"
    echo -e "${YELLOW}Run 'npm run format' or 'npx prettier ./ --write' to fix formatting issues${NC}"
    exit 1
fi
echo ""

# Step 2: Run unit tests
echo -e "${YELLOW}[2/3] Running frontend unit tests...${NC}"
if npm test -- --watchAll=false --coverage=false; then
    echo -e "${GREEN}✓ Unit tests passed${NC}"
else
    echo -e "${RED}✗ Unit tests failed${NC}"
    exit 1
fi
echo ""

# Step 3: Run E2E tests (if not skipped)
if [ "$SKIP_E2E" = false ]; then
    echo -e "${YELLOW}[3/3] Running E2E tests...${NC}"
    echo -e "${YELLOW}This may take several minutes...${NC}"
    if npm run cy:run; then
        echo -e "${GREEN}✓ E2E tests passed${NC}"
    else
        echo -e "${RED}✗ E2E tests failed${NC}"
        exit 1
    fi
    echo ""
else
    echo -e "${YELLOW}[3/3] Skipping E2E tests (--skip-e2e)${NC}"
    echo ""
fi

cd ..

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All frontend CI checks passed! ✓${NC}"
echo -e "${GREEN}Safe to push to GitHub${NC}"
echo -e "${GREEN}========================================${NC}"
