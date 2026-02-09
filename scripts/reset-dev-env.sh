#!/bin/bash
# reset-dev-env.sh - Reset development environment with full rebuild
#
# Usage: ./scripts/reset-dev-env.sh [options]
#
# Options:
#   --skip-build     Skip WAR rebuild (use existing WAR)
#   --skip-fixtures  Skip loading test fixtures
#   --full-reset     Remove volumes (wipe DB) before starting
#   --help           Show this help message
#
# This script:
#   1. Rebuilds the WAR file (unless --skip-build)
#   2. Restarts containers with force-recreate
#   3. Waits for webapp to be ready
#   4. Loads test fixtures (unless --skip-fixtures)

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SKIP_BUILD=false
SKIP_FIXTURES=false
FULL_RESET=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-fixtures)
            SKIP_FIXTURES=true
            shift
            ;;
        --full-reset)
            FULL_RESET=true
            shift
            ;;
        --help)
            head -16 "$0" | tail -14
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

COMPOSE_FILES="-f dev.docker-compose.yml -f docker-compose.letsencrypt.yml"

cd "$(dirname "$0")/.."
PROJECT_ROOT=$(pwd)

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  OpenELIS Dev Environment Reset${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Step 1: Stop containers
echo -e "${YELLOW}[1/4] Stopping containers...${NC}"
if [ "$FULL_RESET" = true ]; then
    echo -e "  ${YELLOW}→ Full reset: removing volumes${NC}"
    docker compose $COMPOSE_FILES down -v 2>/dev/null || true
else
    docker compose $COMPOSE_FILES down 2>/dev/null || true
fi
echo -e "  ${GREEN}✓ Containers stopped${NC}"

# Step 2: Build WAR
if [ "$SKIP_BUILD" = true ]; then
    echo -e "${YELLOW}[2/4] Skipping WAR build (--skip-build)${NC}"
else
    echo -e "${YELLOW}[2/4] Building WAR file...${NC}"
    mvn clean install -DskipTests -Dmaven.test.skip=true -q
    echo -e "  ${GREEN}✓ WAR built successfully${NC}"
fi

# Step 3: Start containers
echo -e "${YELLOW}[3/4] Starting containers...${NC}"
docker compose $COMPOSE_FILES up -d
echo -e "  ${GREEN}✓ Containers started${NC}"

# Step 4: Wait for webapp
echo -e "${YELLOW}[4/4] Waiting for webapp to be ready...${NC}"
MAX_WAIT=120
WAIT_INTERVAL=5
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if curl -sk https://localhost/api/OpenELIS-Global/LoginPage 2>/dev/null | grep -q "Login\|OpenELIS"; then
        echo -e "  ${GREEN}✓ Webapp ready (${ELAPSED}s)${NC}"
        break
    fi
    sleep $WAIT_INTERVAL
    ELAPSED=$((ELAPSED + WAIT_INTERVAL))
    echo -e "  Waiting... (${ELAPSED}s)"
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo -e "  ${RED}✗ Webapp not ready after ${MAX_WAIT}s${NC}"
    echo -e "  Check logs: docker logs openelisglobal-webapp"
    exit 1
fi

# Load fixtures
if [ "$SKIP_FIXTURES" = true ]; then
    echo -e "  Skipping fixtures (--skip-fixtures)"
else
    echo -e "  Loading test fixtures..."
    ./src/test/resources/load-test-fixtures.sh --no-verify
    echo -e "  ${GREEN}✓ Fixtures loaded${NC}"
fi

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Dev Environment Ready!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo -e "  ${BLUE}React UI:${NC}  https://localhost/"
echo -e "  ${BLUE}Legacy UI:${NC} https://localhost/api/OpenELIS-Global/"
echo -e "  ${BLUE}Credentials:${NC} admin / adminADMIN!"
echo ""
docker compose $COMPOSE_FILES ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null || docker compose $COMPOSE_FILES ps
