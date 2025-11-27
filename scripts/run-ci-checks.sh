#!/bin/bash
# Script to run CI checks locally before pushing
# This replicates the GitHub Actions CI workflow (.github/workflows/ci.yml)
#
# Usage:
#   ./scripts/run-ci-checks.sh          # Run all checks
#   ./scripts/run-ci-checks.sh --skip-submodules  # Skip submodule build (faster)
#   ./scripts/run-ci-checks.sh --skip-tests       # Skip tests (formatting only)

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse arguments
SKIP_SUBMODULES=false
SKIP_TESTS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-submodules)
            SKIP_SUBMODULES=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--skip-submodules] [--skip-tests]"
            exit 1
            ;;
    esac
done

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Running CI Checks Locally${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Step 1: Check formatting
echo -e "${YELLOW}[1/3] Checking code formatting...${NC}"
if mvn spotless:check; then
    echo -e "${GREEN}✓ Formatting check passed${NC}"
else
    echo -e "${RED}✗ Formatting check failed${NC}"
    echo -e "${YELLOW}Run 'mvn spotless:apply' to fix formatting issues${NC}"
    exit 1
fi
echo ""

# Step 2: Build submodules (if not skipped)
if [ "$SKIP_SUBMODULES" = false ]; then
    echo -e "${YELLOW}[2/3] Building submodules...${NC}"
    if [ -d "dataexport" ]; then
        cd dataexport
        if mvn clean install; then
            echo -e "${GREEN}✓ Submodules built successfully${NC}"
            cd ..
        else
            echo -e "${RED}✗ Submodule build failed${NC}"
            cd ..
            exit 1
        fi
    else
        echo -e "${YELLOW}⚠ dataexport directory not found, skipping submodule build${NC}"
    fi
    echo ""
else
    echo -e "${YELLOW}[2/3] Skipping submodule build (--skip-submodules)${NC}"
    echo ""
fi

# Step 3: Build and run tests (if not skipped)
if [ "$SKIP_TESTS" = false ]; then
    echo -e "${YELLOW}[3/3] Building and running tests...${NC}"
    echo -e "${YELLOW}This may take several minutes...${NC}"
    if mvn clean install -Dspotless.check.skip=true; then
        echo -e "${GREEN}✓ Build and tests passed${NC}"
    else
        echo -e "${RED}✗ Build or tests failed${NC}"
        exit 1
    fi
    echo ""
else
    echo -e "${YELLOW}[3/3] Skipping tests (--skip-tests)${NC}"
    echo ""
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All CI checks passed! ✓${NC}"
echo -e "${GREEN}Safe to push to GitHub${NC}"
echo -e "${GREEN}========================================${NC}"
