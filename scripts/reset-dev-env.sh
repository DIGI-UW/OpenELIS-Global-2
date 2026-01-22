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
#   3. Configures Let's Encrypt certificates for analyzers.openelis-global.org
#   4. Waits for webapp to be ready
#   5. Loads test fixtures (unless --skip-fixtures)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default options
SKIP_BUILD=false
SKIP_FIXTURES=false
FULL_RESET=false
LETSENCRYPT_DOMAIN="${LETSENCRYPT_DOMAIN:-analyzers.openelis-global.org}"

# Parse arguments
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
            head -20 "$0" | tail -18
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Change to project root
cd "$(dirname "$0")/.."
PROJECT_ROOT=$(pwd)

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  OpenELIS Dev Environment Reset${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Step 1: Stop containers
echo -e "${YELLOW}[1/5] Stopping containers...${NC}"
if [ "$FULL_RESET" = true ]; then
    echo -e "  ${YELLOW}→ Full reset: removing volumes${NC}"
    docker compose -f dev.docker-compose.yml down -v 2>/dev/null || true
else
    docker compose -f dev.docker-compose.yml down 2>/dev/null || true
fi
echo -e "  ${GREEN}✓ Containers stopped${NC}"

# Step 2: Build WAR
if [ "$SKIP_BUILD" = true ]; then
    echo -e "${YELLOW}[2/5] Skipping WAR build (--skip-build)${NC}"
else
    echo -e "${YELLOW}[2/5] Building WAR file...${NC}"
    mvn clean install -DskipTests -Dmaven.test.skip=true -q
    echo -e "  ${GREEN}✓ WAR built successfully${NC}"
fi

# Step 3: Start containers with Let's Encrypt
echo -e "${YELLOW}[3/5] Starting containers with Let's Encrypt...${NC}"
export LETSENCRYPT_DOMAIN
docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml up -d
echo -e "  ${GREEN}✓ Containers started${NC}"

# Step 4: Wait for webapp
echo -e "${YELLOW}[4/5] Waiting for webapp to be ready...${NC}"
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

# Step 5: Load fixtures
if [ "$SKIP_FIXTURES" = true ]; then
    echo -e "${YELLOW}[5/5] Skipping fixtures (--skip-fixtures)${NC}"
else
    echo -e "${YELLOW}[5/5] Loading test fixtures...${NC}"

    # Storage fixtures
    echo -e "  Loading storage fixtures..."
    ./src/test/resources/load-test-fixtures.sh --no-verify

    # Analyzer fixtures
    echo -e "  Loading analyzer fixtures..."
    ./src/test/resources/load-analyzer-test-data.sh --reset --no-verify

    echo -e "  ${GREEN}✓ All fixtures loaded${NC}"
fi

# Summary
echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Dev Environment Ready!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo -e "Access points:"
echo -e "  ${BLUE}React UI:${NC}  https://localhost/"
echo -e "  ${BLUE}Legacy UI:${NC} https://localhost/api/OpenELIS-Global/"
echo -e "  ${BLUE}Credentials:${NC} admin / adminADMIN!"
echo ""
echo -e "Let's Encrypt domain: ${LETSENCRYPT_DOMAIN}"
echo ""

# Verify Let's Encrypt
if docker logs openelisglobal-proxy 2>&1 | grep -q "Let's Encrypt certificates found"; then
    echo -e "${GREEN}✓ Let's Encrypt certificates active${NC}"
else
    echo -e "${YELLOW}⚠ Let's Encrypt certificates not detected (using self-signed)${NC}"
fi

# Show service status
echo ""
docker compose -f dev.docker-compose.yml ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null || docker compose -f dev.docker-compose.yml ps
