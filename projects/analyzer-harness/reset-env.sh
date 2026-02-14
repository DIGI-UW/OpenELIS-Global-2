#!/bin/bash
# reset-env.sh - Rebuild/reset the analyzer harness test environment
#
# Usage: ./reset-env.sh [options]
#
# Options:
#   --build        Build WAR + harness Docker images first (start from scratch)
#   --full-reset   Remove DB (and other) volumes before starting (wipe DB)
#   --skip-fixtures   Skip loading test fixtures after startup
#   --skip-letsencrypt Do not run Let's Encrypt setup even when LETSENCRYPT_* env is set
#   --help            Show this help message
#
# Start from scratch: ./build.sh && ./reset-env.sh --full-reset
# Or: ./reset-env.sh --build --full-reset
#
# Uses same env/Let's Encrypt as main when .env exists (e.g. LETSENCRYPT_DOMAIN=analyzers.openelis-global.org).
# Fixture loading uses direct psql to localhost:15432 (harness DB port).

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"
COMPOSE_DEV="$HARNESS_DIR/docker-compose.dev.yml"
COMPOSE_ANALYZER="$HARNESS_DIR/docker-compose.analyzer-test.yml"
COMPOSE_LETSENCRYPT="$HARNESS_DIR/docker-compose.letsencrypt.yml"
if [ -f "$HARNESS_DIR/.env" ]; then
  ENV_FILE="$HARNESS_DIR/.env"
elif [ -f "$REPO_ROOT/.env" ]; then
  ENV_FILE="$REPO_ROOT/.env"
else
  ENV_FILE=""
fi
# Source .env so LETSENCRYPT_DOMAIN/LETSENCRYPT_EMAIL are available for optional cert setup
[ -n "$ENV_FILE" ] && set -a && . "$ENV_FILE" && set +a

FULL_RESET=false
SKIP_FIXTURES=false
DO_BUILD=false
SKIP_LETSENCRYPT=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            DO_BUILD=true
            shift
            ;;
        --full-reset)
            FULL_RESET=true
            shift
            ;;
        --skip-fixtures)
            SKIP_FIXTURES=true
            shift
            ;;
        --skip-letsencrypt)
            SKIP_LETSENCRYPT=true
            shift
            ;;
        --help)
            head -30 "$0" | tail -27
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  Analyzer Harness – Reset test env${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Optional: build WAR + images first (start from scratch)
if [ "$DO_BUILD" = true ]; then
    echo -e "${YELLOW}[0/4] Building WAR + harness images...${NC}"
    "$HARNESS_DIR/build.sh"
    echo -e "  ${GREEN}✓ Build complete${NC}"
    echo ""
fi

# Step 1: Stop stack (optionally remove volumes)
echo -e "${YELLOW}[1/4] Stopping harness stack...${NC}"
cd "$HARNESS_DIR"
if [ "$FULL_RESET" = true ]; then
    echo -e "  ${YELLOW}→ Full reset: removing volumes${NC}"
    docker compose ${ENV_FILE:+--env-file "$ENV_FILE"} -f "$COMPOSE_DEV" -f "$COMPOSE_ANALYZER" -f "$COMPOSE_LETSENCRYPT" down -v 2>/dev/null || true
else
    docker compose ${ENV_FILE:+--env-file "$ENV_FILE"} -f "$COMPOSE_DEV" -f "$COMPOSE_ANALYZER" -f "$COMPOSE_LETSENCRYPT" down 2>/dev/null || true
fi
echo -e "  ${GREEN}✓ Stack stopped${NC}"

# Bootstrap volume dir if needed (idempotent)
"$HARNESS_DIR/bootstrap.sh"

# Ensure repo volume dirs exist so proxy bind mounts work (and so valid certs in volume/letsencrypt are used)
mkdir -p "$REPO_ROOT/volume/letsencrypt" "$REPO_ROOT/volume/nginx/certbot"

# Step 2: Start stack (with LetsEncrypt override so proxy uses same env/domain as main)
echo -e "${YELLOW}[2/4] Starting harness stack (dev + analyzer-test + letsencrypt)...${NC}"
docker compose ${ENV_FILE:+--env-file "$ENV_FILE"} -f "$COMPOSE_DEV" -f "$COMPOSE_ANALYZER" -f "$COMPOSE_LETSENCRYPT" up -d
echo -e "  ${GREEN}✓ Stack started${NC}"

# Step 3: Wait for webapp
echo -e "${YELLOW}[3/4] Waiting for webapp...${NC}"
MAX_WAIT=120
WAIT_INTERVAL=5
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if curl -sk https://localhost/ 2>/dev/null | grep -q "OpenELIS\|Login"; then
        echo -e "  ${GREEN}✓ Webapp ready (${ELAPSED}s)${NC}"
        break
    fi
    sleep $WAIT_INTERVAL
    ELAPSED=$((ELAPSED + WAIT_INTERVAL))
    echo -e "  Waiting... (${ELAPSED}s)"
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo -e "  ${RED}✗ Webapp not ready after ${MAX_WAIT}s${NC}"
    exit 1
fi

# Step 3b: Let's Encrypt setup when LETSENCRYPT_DOMAIN + LETSENCRYPT_EMAIL are set
if [ "$SKIP_LETSENCRYPT" = false ] && [ -n "${LETSENCRYPT_DOMAIN:-}" ] && [ -n "${LETSENCRYPT_EMAIL:-}" ]; then
    echo -e "${YELLOW}[3b] Setting up Let's Encrypt for ${LETSENCRYPT_DOMAIN}...${NC}"
    if "$HARNESS_DIR/scripts/generate-letsencrypt-certs.sh"; then
        docker compose ${ENV_FILE:+--env-file "$ENV_FILE"} -f "$COMPOSE_DEV" -f "$COMPOSE_ANALYZER" -f "$COMPOSE_LETSENCRYPT" restart proxy
        echo -e "  ${GREEN}✓ Let's Encrypt cert obtained/renewed; proxy restarted${NC}"
    else
        echo -e "  ${YELLOW}⚠ Let's Encrypt setup failed (e.g. DNS/port 80); proxy keeps self-signed${NC}"
    fi
else
    if [ "$SKIP_LETSENCRYPT" = true ]; then
        echo -e "${YELLOW}[3b] Skipping Let's Encrypt (--skip-letsencrypt)${NC}"
    else
        echo -e "${YELLOW}[3b] Skipping Let's Encrypt (set LETSENCRYPT_DOMAIN and LETSENCRYPT_EMAIL in .env to enable)${NC}"
    fi
fi

# Step 4: Load fixtures (from repo root, direct psql to harness DB on 15432)
if [ "$SKIP_FIXTURES" = true ]; then
    echo -e "${YELLOW}[4/4] Skipping fixtures (--skip-fixtures)${NC}"
else
    echo -e "${YELLOW}[4/4] Loading fixtures (DB_PORT=15432)...${NC}"
    cd "$REPO_ROOT"
    export DB_PORT=15432
    export DB_HOST="${DB_HOST:-localhost}"

    if [ "$FULL_RESET" = true ]; then
        ./src/test/resources/load-test-fixtures.sh --no-verify
    else
        ./src/test/resources/load-test-fixtures.sh --reset --no-verify
    fi
    # 011 dataset is loaded by load-test-fixtures.sh (once); no separate call to avoid duplicate INSERT.

    echo -e "  ${GREEN}✓ Fixtures loaded${NC}"
fi

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Harness test env ready${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo -e "  UI: https://localhost/"
echo -e "  Login: admin / adminADMIN!"
echo ""
echo -e "  ${YELLOW}Let's Encrypt: uses repo volume/letsencrypt (see docs/LETSENCRYPT_SETUP.md).${NC}"
echo -e "    From repo root: LETSENCRYPT_DOMAIN=analyzers.openelis-global.org ./scripts/generate-letsencrypt-certs.sh"
echo ""
