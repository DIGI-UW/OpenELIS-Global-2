#!/bin/bash
#
# run-e2e-like-ci.sh - Run the Playwright core E2E lane EXACTLY like CI does,
#                      without taking over the machine.
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
# ISOLATION. Parity requires build.docker-compose.yml, because that is what
# .github/workflows/e2e-playwright-reusable.yml uses. But that file claims
# global names: fixed container_names, fixed host ports (15432/8080/8443/
# 8081/8444/80/443), fixed image tags, and network subnet 172.20.1.0/24. Run
# it raw and it collides with any other OpenELIS stack on the host, and its
# `down -v` destroys that stack's volumes instead of its own. Developer
# machines here routinely have several stacks up, so "just stop everything
# first" is not a workable instruction.
#
# So this script layers build.docker-compose.worktree.yml, which drops those
# global claims (see that file), and passes a project name derived from this
# worktree's path. Nothing CI exercises changes: same Dockerfiles, same build
# contexts, same topology, same env, same fixtures, same specs. Only identity
# and port binding differ, and `down -v` is scoped to this worktree's project.
# Two worktrees can run this simultaneously.
#
# Because ports are ephemeral, BASE_URL is read back from the running proxy
# rather than assumed to be https://localhost.
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

# Per-worktree Compose project, mirroring scripts/dev-stack's make_context():
# a readable slug plus a hash of the absolute path, so two checkouts of this
# repo never share containers, volumes, networks or image tags.
SLUG="$(basename "$PROJECT_ROOT" | tr '[:upper:]' '[:lower:]' \
        | sed 's/[^a-z0-9][^a-z0-9]*/-/g; s/^-//; s/-$//' | cut -c1-28 | sed 's/-$//')"
DIGEST="$(printf '%s' "$PROJECT_ROOT" | shasum -a 256 | cut -c1-8)"
export E2E_STACK_PROJECT="oe2-${SLUG:-worktree}-${DIGEST}-e2e"

COMPOSE=(docker compose -p "$E2E_STACK_PROJECT"
         -f build.docker-compose.yml
         -f build.docker-compose.worktree.yml)

echo -e "${GREEN}=============================================${NC}"
echo -e "${GREEN}Playwright Core E2E (CI Replication Mode)${NC}"
echo -e "${GREEN}=============================================${NC}"
echo -e "Compose project: ${YELLOW}${E2E_STACK_PROJECT}${NC}  (isolated to this worktree)"
echo ""

# Step 0: Submodules. CI checks out with `submodules: recursive`
# (.github/workflows/e2e-playwright-reusable.yml), and several submodules are
# build inputs rather than optional extras: ./Dockerfile does
# `WORKDIR /build/dataexport/dataexport-core` and runs maven there. A fresh
# worktree has none of them checked out, so without this the failure lands
# roughly twenty minutes into the image build as "there is no POM in this
# directory", which reads like a broken Dockerfile rather than a missing
# checkout step.
echo -e "${YELLOW}[0/4] Checking submodules (CI uses submodules: recursive)...${NC}"
if git submodule status --recursive 2>/dev/null | grep -q '^-'; then
  echo "  Uninitialized submodules found; initializing (this can take a while)..."
  git submodule update --init --recursive
fi
echo -e "${GREEN}✓ Submodules ready${NC}"
echo ""

# Step 1: Fresh stack. CI builds a brand-new database for every run; a reused
# db-data volume is the one environment CI can never reproduce. `down -v` is
# safe here because -p scopes it to this worktree's project.
if [ "$KEEP_DB" = true ]; then
  echo -e "${YELLOW}[1/4] --keep-db: reusing existing stack/database (NOT CI parity)${NC}"
  if ! "${COMPOSE[@]}" ps --status running --quiet oe.openelis.org 2>/dev/null | grep -q .; then
    echo -e "${RED}ERROR: stack '$E2E_STACK_PROJECT' not running. Re-run without --keep-db.${NC}"
    exit 1
  fi
else
  echo -e "${YELLOW}[1/4] Recreating stack with a FRESH database (like CI)...${NC}"
  # Keep the UAT fixture endpoint unavailable in ordinary deployments. The
  # local CI-parity stack is disposable and needs it for the Playwright setup.
  export OE_UAT_SCENARIOS_ENABLED=true
  "${COMPOSE[@]}" down -v --remove-orphans
  "${COMPOSE[@]}" up -d --build --wait --wait-timeout 600
fi
echo -e "${GREEN}✓ Stack ready${NC}"
echo ""

# Ports are ephemeral, so resolve the ones this run actually got. The fixture
# loader shells into the database with `docker exec "${DB_CONTAINER:-...}"`,
# whose default is the fixed name this overlay removed, so hand it the real
# container id.
DB_CONTAINER="$("${COMPOSE[@]}" ps -q db.openelis.org | head -1)"
export DB_CONTAINER
if [ -z "$DB_CONTAINER" ]; then
  echo -e "${RED}ERROR: could not resolve the database container for $E2E_STACK_PROJECT${NC}"
  exit 1
fi

PROXY_HTTPS_PORT="$("${COMPOSE[@]}" port proxy 443 2>/dev/null | awk -F: '{print $NF}')"
if [ -z "$PROXY_HTTPS_PORT" ]; then
  echo -e "${RED}ERROR: proxy has no published 443 port; is the stack up?${NC}"
  exit 1
fi

# Step 2: Fixtures — the workflow's exact command
# (.github/workflows/e2e-playwright-reusable.yml, "Load core fixtures").
echo -e "${YELLOW}[2/4] Loading core fixtures (CI command)...${NC}"
./src/test/resources/load-test-fixtures.sh --profile=core --no-verify
echo -e "${GREEN}✓ Fixtures loaded${NC}"
echo ""

# Step 3: Frontend dependencies (lockfile-faithful, like CI).
#
# CI runs `npm ci --legacy-peer-deps` on every job
# (.github/workflows/e2e-playwright-reusable.yml), so it can never be out of
# date with the lockfile. Installing only when node_modules is absent is not
# equivalent: switching to a branch that adds a dependency leaves a stale tree
# and Playwright dies at collection time with "Cannot find package X imported
# from <spec>", which looks like a broken spec rather than a stale install.
# Reinstall whenever the lockfile is newer than the last install, and keep CI's
# --legacy-peer-deps so the resolved tree matches.
echo -e "${YELLOW}[3/4] Checking frontend dependencies...${NC}"
cd frontend
if [ ! -f "package-lock.json" ]; then
  echo -e "${RED}ERROR: package-lock.json not found in ./frontend${NC}"
  exit 1
fi
if [ ! -d "node_modules" ] || [ "package-lock.json" -nt "node_modules/.package-lock.json" ]; then
  echo "  Installing with npm ci --legacy-peer-deps (CI command)..."
  npm ci --legacy-peer-deps > /dev/null 2>&1
fi
echo -e "${GREEN}✓ Dependencies ready${NC}"
echo ""

# Step 4: Run the core lane exactly as CI does (same projects, same env;
# no --shard locally — one machine runs the full set).
echo -e "${YELLOW}[4/4] Running Playwright core suites...${NC}"
export BASE_URL="${BASE_URL:-https://localhost:${PROXY_HTTPS_PORT}}"
export TEST_USER="${TEST_USER:-admin}"
export TEST_PASS="${TEST_PASS:-adminADMIN!}"
echo "  BASE_URL: $BASE_URL"
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
  echo "Stack:  docker compose -p $E2E_STACK_PROJECT ... logs"
  exit 1
fi
