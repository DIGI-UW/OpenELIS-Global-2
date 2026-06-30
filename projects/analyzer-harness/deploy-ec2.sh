#!/usr/bin/env bash
# deploy-ec2.sh — Deploy/redeploy the analyzer harness on an EC2 instance.
#
# Runs ON the EC2 instance (called via SSH from CI or local CLI).
# Modeled on reset-env.sh but uses published Docker images instead of local builds.
#
# Usage: deploy-ec2.sh [options]
#   --full-reset       Wipe DB volumes before starting (clean slate)
#   --skip-fixtures    Skip loading test fixtures
#   --skip-seed        Skip analyzer seeding via REST API
#   --env-file FILE    Source image tags + config from FILE (default: .env)
#   --help             Show this help
#
# Prerequisites:
#   - Docker + Docker Compose v2 installed
#   - Running as deploy user with docker group access
#   - Deploy bundle rsynced to /opt/analyzer-harness/ (compose files, volume/, etc.)
#
# Expected directory layout (created by rsync from CI or local CLI):
#   /opt/analyzer-harness/
#   ├── docker-compose.{dev,analyzer-test,deploy,letsencrypt}.yml
#   ├── deploy-ec2.sh (this script)
#   ├── seed-analyzers.sh
#   ├── .env
#   ├── volume/           (harness config: database/, properties/, nginx/, plugins/, etc.)
#   ├── bridge-config/    (configuration.yml for analyzer bridge)
#   ├── analyzer-profiles/ (ASTM/HL7/FILE profile JSONs)
#   ├── e2e-fixtures/     (genexpert_astm.json for simulator)
#   ├── certbot/          (nginx certbot challenge dir)
#   ├── letsencrypt/      (Let's Encrypt certs)
#   ├── nginx-proxy/      (docker-entrypoint.sh)
#   └── test-fixtures/    (load-test-fixtures.sh + SQL files)

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DEPLOY_DIR"

FULL_RESET=false
SKIP_FIXTURES=false
SKIP_SEED=false
ENV_FILE="$DEPLOY_DIR/.env"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --full-reset)   FULL_RESET=true; shift ;;
    --skip-fixtures) SKIP_FIXTURES=true; shift ;;
    --skip-seed)    SKIP_SEED=true; shift ;;
    --env-file)     ENV_FILE="$2"; shift 2 ;;
    --help)         head -16 "$0" | tail -14; exit 0 ;;
    *)              echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

# --- Source environment ---
if [ -f "$ENV_FILE" ]; then
  set -a && . "$ENV_FILE" && set +a
  echo -e "${GREEN}✓ Loaded environment from $ENV_FILE${NC}"
else
  echo -e "${YELLOW}⚠ No .env file at $ENV_FILE — using defaults${NC}"
fi

# deploy.yml MUST be last — it overrides all ../../ paths from earlier files
COMPOSE_FILES="-f docker-compose.dev.yml -f docker-compose.analyzer-test.yml"
if [ -f "$DEPLOY_DIR/docker-compose.letsencrypt.yml" ]; then
  COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.letsencrypt.yml"
fi
COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.deploy.yml"

compose() {
  docker compose $COMPOSE_FILES "$@"
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Analyzer Harness – EC2 Deploy${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "  OE_IMAGE:       ${OE_IMAGE:-itechuw/openelis-global-2-dev:develop}"
echo -e "  FRONTEND_IMAGE: ${FRONTEND_IMAGE:-itechuw/openelis-global-2-frontend-dev:develop}"
echo -e "  BRIDGE_IMAGE:   ${BRIDGE_IMAGE:-itechuw/openelis-analyzer-bridge:develop}"
echo -e "  SIMULATOR_IMAGE:${SIMULATOR_IMAGE:-itechuw/astm-mock-server:main}"
echo -e "  Full reset:     $FULL_RESET"
echo ""

# --- Step 1: Ensure volume directories exist ---
echo -e "${YELLOW}[1/6] Ensuring volume directories...${NC}"
mkdir -p "$DEPLOY_DIR/volume/database/dbInit"
mkdir -p "$DEPLOY_DIR/volume/properties"
mkdir -p "$DEPLOY_DIR/volume/nginx"
mkdir -p "$DEPLOY_DIR/volume/analyzer"
mkdir -p "$DEPLOY_DIR/volume/menu"
mkdir -p "$DEPLOY_DIR/volume/logs/oeLogs"
mkdir -p "$DEPLOY_DIR/volume/logs/tomcatLogs"
mkdir -p "$DEPLOY_DIR/volume/plugins"
mkdir -p "$DEPLOY_DIR/volume/programs"
mkdir -p "$DEPLOY_DIR/volume/configuration/backend"
mkdir -p "$DEPLOY_DIR/volume/analyzer-imports"
mkdir -p "$DEPLOY_DIR/certbot"
mkdir -p "$DEPLOY_DIR/letsencrypt"
mkdir -p "$DEPLOY_DIR/projects/analyzer-profiles"
mkdir -p "$DEPLOY_DIR/bridge-config"
mkdir -p "$DEPLOY_DIR/e2e-fixtures"
echo -e "  ${GREEN}✓ Volume directories ready${NC}"

# --- Step 2: Pull images ---
echo -e "${YELLOW}[2/6] Pulling Docker images...${NC}"
compose pull --quiet 2>&1 || compose pull
echo -e "  ${GREEN}✓ Images pulled${NC}"

# --- Step 3: Stop existing stack ---
echo -e "${YELLOW}[3/6] Stopping existing stack...${NC}"
if [ "$FULL_RESET" = true ]; then
  echo -e "  ${YELLOW}→ Full reset: removing volumes${NC}"
  compose down -v 2>/dev/null || true
else
  compose down 2>/dev/null || true
fi
echo -e "  ${GREEN}✓ Stack stopped${NC}"

# --- Step 4: Start stack ---
echo -e "${YELLOW}[4/6] Starting stack...${NC}"
compose up -d
echo -e "  ${GREEN}✓ Stack started${NC}"

# --- Step 5: Wait for webapp ---
echo -e "${YELLOW}[5/6] Waiting for webapp...${NC}"
MAX_WAIT=300
WAIT_INTERVAL=10
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
  echo -e "  ${RED}  Check logs: docker compose $COMPOSE_FILES logs oe${NC}"
  exit 1
fi

# --- Step 6: Load fixtures + seed ---
if [ "$SKIP_FIXTURES" = true ]; then
  echo -e "${YELLOW}[6/6] Skipping fixtures (--skip-fixtures)${NC}"
else
  echo -e "${YELLOW}[6/6] Loading fixtures...${NC}"

  FIXTURE_SCRIPT="$DEPLOY_DIR/test-fixtures/load-test-fixtures.sh"
  if [ -f "$FIXTURE_SCRIPT" ]; then
    # load-test-fixtures.sh auto-detects Docker containers for psql
    if [ "$FULL_RESET" = true ]; then
      bash "$FIXTURE_SCRIPT" --analyzers=full --no-verify
    else
      bash "$FIXTURE_SCRIPT" --analyzers=full --reset --no-verify
    fi
    echo -e "  ${GREEN}✓ Fixtures loaded${NC}"
  else
    echo -e "  ${YELLOW}⚠ Fixture script not found at $FIXTURE_SCRIPT${NC}"
  fi
fi

if [ "$SKIP_SEED" = true ]; then
  echo -e "${YELLOW}  Skipping analyzer seeding (--skip-seed)${NC}"
else
  if [ -n "${TEST_PASS:-}" ]; then
    echo -e "${YELLOW}  Seeding analyzers...${NC}"
    # Set REPO_ROOT so seed-analyzers.sh finds profiles at ./projects/analyzer-profiles/
    # and .env at ./.env (both relative to DEPLOY_DIR)
    REPO_ROOT="$DEPLOY_DIR" BASE_URL=https://localhost bash "$DEPLOY_DIR/seed-analyzers.sh"
    echo -e "  ${GREEN}✓ Analyzers seeded${NC}"
  else
    echo -e "  ${YELLOW}⚠ TEST_PASS not set; skipping analyzer seeding${NC}"
  fi
fi

# --- Cleanup old images ---
echo -e "${YELLOW}Cleaning up unused images...${NC}"
docker system prune -f --filter "until=24h" 2>/dev/null || true

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Deploy complete${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "  UI:        https://$(hostname -f 2>/dev/null || echo 'localhost')/"
echo -e "  Login:     admin / adminADMIN!"
echo -e "  Simulator: http://localhost:8085/health"
echo -e "  Bridge:    https://localhost:8442/actuator/health"
echo ""
