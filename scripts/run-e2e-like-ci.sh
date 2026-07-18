#!/bin/bash
#
# run-e2e-like-ci.sh - Run the Playwright core E2E lane EXACTLY like CI does.
#
# CI parity means three things (each was a source of local-only "mystery"
# failures when it diverged):
#   1. A FRESH database every run (CI never reuses a volume; dirty-volume
#      reloads are the only place fixture cleanup FK errors can happen).
#   2. The workflow's exact fixture command (load-test-fixtures.sh, NOT the
#      minimal scripts/load-ci-fixtures.sh, which lacks the demo patient and
#      storage fixtures).
#   3. The workflow's exact Playwright invocation (core-app + core-demo
#      projects; workers=1 comes from playwright.config.ts, same as CI).
#
# Usage:
#   ./scripts/run-e2e-like-ci.sh                    # fresh DB + fixtures + core suites
#   ./scripts/run-e2e-like-ci.sh --keep-db          # skip DB recreate (NOT CI parity;
#                                                   # dirty-DB runs are unsupported)
#   ./scripts/run-e2e-like-ci.sh -- --grep "US3"    # args after -- go to playwright
#
# The analyzer-harness lane has its own parity runner:
#   ./projects/analyzer-harness/ci-parity-test.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

KEEP_DB=false
PW_ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --keep-db) KEEP_DB=true; shift ;;
    --) shift; PW_ARGS=("$@"); break ;;
    *) PW_ARGS+=("$1"); shift ;;
  esac
done

echo -e "${GREEN}=============================================${NC}"
echo -e "${GREEN}Playwright Core E2E (CI Replication Mode)${NC}"
echo -e "${GREEN}=============================================${NC}"
echo ""

# Step 1: Fresh stack. CI builds a brand-new database for every run; a reused
# db-data volume is the one environment CI can never reproduce.
if [ "$KEEP_DB" = true ]; then
  echo -e "${YELLOW}[1/4] --keep-db: reusing existing stack/database (NOT CI parity)${NC}"
  if ! docker ps | grep -q "openelisglobal-webapp"; then
    echo -e "${RED}ERROR: stack not running. Re-run without --keep-db.${NC}"
    exit 1
  fi
else
  echo -e "${YELLOW}[1/4] Recreating stack with a FRESH database (like CI)...${NC}"
  docker compose -f build.docker-compose.yml down -v --remove-orphans
  docker compose -f build.docker-compose.yml up -d --build --wait --wait-timeout 600
fi
echo -e "${GREEN}✓ Stack ready${NC}"
echo ""

# Step 2: Fixtures — the workflow's exact command
# (.github/workflows/e2e-playwright-reusable.yml, "Load core fixtures").
echo -e "${YELLOW}[2/4] Loading core fixtures (CI command)...${NC}"
./src/test/resources/load-test-fixtures.sh --profile=core --no-verify
echo -e "${GREEN}✓ Fixtures loaded${NC}"
echo ""

# Step 3: Frontend dependencies (lockfile-faithful, like CI).
echo -e "${YELLOW}[3/4] Checking frontend dependencies...${NC}"
cd frontend
if [ ! -d "node_modules" ]; then
  if [ -f "package-lock.json" ]; then
    echo "  Installing with npm ci..."
    npm ci > /dev/null 2>&1
  else
    echo -e "${RED}ERROR: package-lock.json not found in ./frontend${NC}"
    exit 1
  fi
fi
echo -e "${GREEN}✓ Dependencies ready${NC}"
echo ""

# Step 4: Run the core lane exactly as CI does (same projects, same env;
# no --shard locally — one machine runs the full set).
echo -e "${YELLOW}[4/4] Running Playwright core suites...${NC}"
export BASE_URL="${BASE_URL:-https://localhost}"
export TEST_USER="${TEST_USER:-admin}"
export TEST_PASS="${TEST_PASS:-adminADMIN!}"
CMD=(npm run pw:test -- --project=core-app --project=core-demo)
CMD+=("${PW_ARGS[@]}")
printf 'Running: %s\n' "${CMD[*]}"
echo ""

if "${CMD[@]}"; then
  echo ""
  echo -e "${GREEN}=============================================${NC}"
  echo -e "${GREEN}✓ Core E2E PASSED (CI parity)${NC}"
  echo -e "${GREEN}=============================================${NC}"
else
  echo ""
  echo -e "${RED}=============================================${NC}"
  echo -e "${RED}✗ Core E2E FAILED${NC}"
  echo -e "${RED}=============================================${NC}"
  echo ""
  echo "Report: frontend/playwright-report/  Traces: frontend/test-results/"
  exit 1
fi
