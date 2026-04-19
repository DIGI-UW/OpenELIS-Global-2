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
HARNESS_PLUGIN_DIR="$HARNESS_DIR/volume/plugins"
source "$HARNESS_DIR/compose-stack.sh"
if [ -f "$HARNESS_DIR/.env" ]; then
  ENV_FILE="$HARNESS_DIR/.env"
elif [ -f "$REPO_ROOT/.env" ]; then
  ENV_FILE="$REPO_ROOT/.env"
else
  ENV_FILE=""
fi
# Source .env so LETSENCRYPT_DOMAIN/LETSENCRYPT_EMAIL are available for optional cert setup
[ -n "$ENV_FILE" ] && set -a && . "$ENV_FILE" && set +a

ENV_ARGS=()
if [ -n "$ENV_FILE" ]; then
  ENV_ARGS=(--env-file "$ENV_FILE")
fi

FULL_RESET=false
SKIP_FIXTURES=false
DO_BUILD=false
USE_LETSENCRYPT=false
SKIP_LETSENCRYPT=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            DO_BUILD=true
            shift
            ;;
        --letsencrypt)
            USE_LETSENCRYPT=true
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

LOCAL_COMPOSE_FILES=($(compose_args_local "$USE_LETSENCRYPT"))

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
    docker compose "${ENV_ARGS[@]}" "${LOCAL_COMPOSE_FILES[@]}" down --remove-orphans -v 2>/dev/null || true
else
    docker compose "${ENV_ARGS[@]}" "${LOCAL_COMPOSE_FILES[@]}" down --remove-orphans 2>/dev/null || true
fi
echo -e "  ${GREEN}✓ Stack stopped${NC}"

# Bootstrap volume dir if needed (idempotent)
"$HARNESS_DIR/bootstrap.sh"

# Ensure repo volume dirs exist so proxy bind mounts work (and so valid certs in volume/letsencrypt are used)
mkdir -p "$REPO_ROOT/volume/letsencrypt" "$REPO_ROOT/volume/nginx/certbot"

# Step 2: Stage plugin jars for runtime loading (parity with CI)
echo -e "${YELLOW}[2/5] Staging analyzer plugin jars for runtime loading...${NC}"
cd "$REPO_ROOT"
mkdir -p "$HARNESS_PLUGIN_DIR"

if ! find "$REPO_ROOT/plugins/analyzers" -type f -path "*/target/*.jar" \
    ! -name "*sources.jar" ! -name "*javadoc.jar" | grep -q .; then
    echo -e "  ${YELLOW}→ No built plugin jars found; building plugins first${NC}"
    mvn clean install -DskipTests -Dmaven.test.skip=true -f "$REPO_ROOT/plugins/pom.xml"
fi

rm -rf "$HARNESS_PLUGIN_DIR"/*
find "$REPO_ROOT/plugins/analyzers" -type f -path "*/target/*.jar" \
    ! -name "*sources.jar" ! -name "*javadoc.jar" \
    -exec cp {} "$HARNESS_PLUGIN_DIR/" \;
PLUGIN_COUNT=$(find "$HARNESS_PLUGIN_DIR" -maxdepth 1 -type f -name "*.jar" | wc -l | tr -d ' ')
echo -e "  ${GREEN}✓ Staged ${PLUGIN_COUNT} plugin jars${NC}"

# Step 3: Start stack
if [ "$USE_LETSENCRYPT" = true ]; then
    echo -e "${YELLOW}[3/5] Starting harness stack (dev + analyzer-test + letsencrypt)...${NC}"
else
    echo -e "${YELLOW}[3/5] Starting harness stack (dev + analyzer-test)...${NC}"
fi
cd "$HARNESS_DIR"
docker compose "${ENV_ARGS[@]}" "${LOCAL_COMPOSE_FILES[@]}" up -d --remove-orphans
echo -e "  ${GREEN}✓ Stack started${NC}"

# Step 4: Wait for OE login over the harness proxy
echo -e "${YELLOW}[4/5] Waiting for OE login readiness...${NC}"
MAX_WAIT=240
WAIT_INTERVAL=5
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if "$HARNESS_DIR/scripts/verify-login.sh" >/dev/null 2>&1; then
        echo -e "  ${GREEN}✓ Login ready (${ELAPSED}s)${NC}"
        break
    fi
    sleep $WAIT_INTERVAL
    ELAPSED=$((ELAPSED + WAIT_INTERVAL))
    echo -e "  Waiting... (${ELAPSED}s)"
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo -e "  ${RED}✗ Login not ready after ${MAX_WAIT}s${NC}"
    exit 1
fi

# Step 4b: Optional Let's Encrypt setup
if [ "$USE_LETSENCRYPT" = true ] && [ "$SKIP_LETSENCRYPT" = false ] && [ -n "${LETSENCRYPT_DOMAIN:-}" ] && [ -n "${LETSENCRYPT_EMAIL:-}" ]; then
    echo -e "${YELLOW}[3b] Setting up Let's Encrypt for ${LETSENCRYPT_DOMAIN}...${NC}"
    if "$HARNESS_DIR/scripts/generate-letsencrypt-certs.sh"; then
        docker compose "${ENV_ARGS[@]}" "${LOCAL_COMPOSE_FILES[@]}" restart proxy
        echo -e "  ${GREEN}✓ Let's Encrypt cert obtained/renewed; proxy restarted${NC}"
    else
        echo -e "  ${YELLOW}⚠ Let's Encrypt setup failed (e.g. DNS/port 80); proxy keeps self-signed${NC}"
    fi
else
    if [ "$SKIP_LETSENCRYPT" = true ]; then
        echo -e "${YELLOW}[3b] Skipping Let's Encrypt (--skip-letsencrypt)${NC}"
    elif [ "$USE_LETSENCRYPT" = false ]; then
        echo -e "${YELLOW}[3b] Skipping Let's Encrypt (local self-signed mode)${NC}"
    else
        echo -e "${YELLOW}[3b] Skipping Let's Encrypt (set LETSENCRYPT_DOMAIN and LETSENCRYPT_EMAIL in .env to enable)${NC}"
    fi
fi

# Step 5: Load fixtures (from repo root, direct psql to harness DB on 15432)
if [ "$SKIP_FIXTURES" = true ]; then
    echo -e "${YELLOW}[5/5] Skipping fixtures (--skip-fixtures)${NC}"
else
    echo -e "${YELLOW}[5/5] Loading fixtures (DB_PORT=15432)...${NC}"
    cd "$REPO_ROOT"
    export DB_PORT=15432
    export DB_HOST="${DB_HOST:-localhost}"

    if [ "$FULL_RESET" = true ]; then
        ./src/test/resources/load-test-fixtures.sh --profile=harness --no-verify
    else
        ./src/test/resources/load-test-fixtures.sh --profile=harness --reset --no-verify
    fi

    # Seed 4 harness analyzers via REST API (parity with CI and /restart-analyzer-harness)
    set -a && [ -f .env ] && . ./.env && set +a
    if [ -n "${TEST_PASS:-}" ]; then
        BASE_URL=https://localhost bash projects/analyzer-harness/seed-analyzers.sh
        echo -e "  ${GREEN}✓ Analyzers seeded${NC}"
    else
        echo -e "  ${YELLOW}⚠ TEST_PASS not set; run seed-analyzers.sh manually after adding to .env${NC}"
    fi
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
