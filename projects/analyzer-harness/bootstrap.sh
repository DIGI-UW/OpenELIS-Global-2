#!/bin/bash
# bootstrap.sh - Idempotent setup of analyzer harness volume and dependencies.
# Run from repo root or from projects/analyzer-harness. Initializes submodules,
# creates volume/ tree, copies/adapts config from root volume (hostname-safe for
# Docker network). Safe to run multiple times.

set -e

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"
ROOT_VOLUME="$REPO_ROOT/volume"
HARNESS_VOLUME="$HARNESS_DIR/volume"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "Bootstrap: REPO_ROOT=$REPO_ROOT HARNESS_DIR=$HARNESS_DIR"

# --- Submodules ---
echo "Initializing submodules (tools/analyzer-mock-server, tools/openelis-analyzer-bridge, plugins)..."
cd "$REPO_ROOT"
git submodule update --init tools/analyzer-mock-server tools/openelis-analyzer-bridge plugins 2>/dev/null || true
echo -e "  ${GREEN}✓ Submodules initialized${NC}"

# --- Volume directory tree ---
mkdir -p "$HARNESS_VOLUME/database/dbInit"
mkdir -p "$HARNESS_VOLUME/properties"
mkdir -p "$HARNESS_VOLUME/nginx"
mkdir -p "$HARNESS_VOLUME/analyzer"
mkdir -p "$HARNESS_VOLUME/menu"
mkdir -p "$HARNESS_VOLUME/logs/oeLogs"
mkdir -p "$HARNESS_VOLUME/logs/tomcatLogs"
mkdir -p "$HARNESS_VOLUME/plugins"
# Copy analyzer plugin JARs from submodule (idempotent: skips existing)
PLUGIN_SRC="$REPO_ROOT/plugins/plugins"
if [ -d "$PLUGIN_SRC" ] && ls "$PLUGIN_SRC"/*.jar 1>/dev/null 2>&1; then
  cp -n "$PLUGIN_SRC"/*.jar "$HARNESS_VOLUME/plugins/" 2>/dev/null || true
  echo -e "  ${GREEN}✓ Analyzer plugins copied ($(ls "$HARNESS_VOLUME/plugins/"*.jar 2>/dev/null | wc -l) JARs)${NC}"
else
  echo -e "  ${YELLOW}WARN: No plugin JARs found at plugins/plugins/. Run 'cd plugins && mvn package' to build them.${NC}"
fi
mkdir -p "$HARNESS_VOLUME/programs"
mkdir -p "$HARNESS_VOLUME/configuration"
mkdir -p "$HARNESS_VOLUME/analyzer-imports"

# --- Copy/adapt from root volume (idempotent: only if source exists and target missing or we overwrite nginx) ---
copy_if_missing() {
  local src="$1"
  local dst="$2"
  if [ -e "$src" ] && [ ! -e "$dst" ]; then
    cp "$src" "$dst"
    echo "  copied $dst"
  fi
}

# Direct copies
copy_if_missing "$ROOT_VOLUME/database/database.env" "$HARNESS_VOLUME/database/database.env"
copy_if_missing "$ROOT_VOLUME/properties/datasource.password" "$HARNESS_VOLUME/properties/datasource.password"
copy_if_missing "$ROOT_VOLUME/properties/SystemConfiguration.properties" "$HARNESS_VOLUME/properties/SystemConfiguration.properties"

# common.properties: adapt fhir.openelis.org -> fhir
if [ -f "$ROOT_VOLUME/properties/common.properties" ] && [ ! -f "$HARNESS_VOLUME/properties/common.properties" ]; then
  sed 's/fhir\.openelis\.org/fhir/g' "$ROOT_VOLUME/properties/common.properties" > "$HARNESS_VOLUME/properties/common.properties"
  echo "  copied+adapted common.properties"
fi

# hapi_application.yaml: adapt db.openelis.org -> db, fhir.openelis.org -> fhir
if [ -f "$ROOT_VOLUME/properties/hapi_application.yaml" ] && [ ! -f "$HARNESS_VOLUME/properties/hapi_application.yaml" ]; then
  sed -e 's/db\.openelis\.org/db/g' -e 's/fhir\.openelis\.org/fhir/g' "$ROOT_VOLUME/properties/hapi_application.yaml" > "$HARNESS_VOLUME/properties/hapi_application.yaml"
  echo "  copied+adapted hapi_application.yaml"
fi

# dbInit directory
if [ -d "$ROOT_VOLUME/database/dbInit" ]; then
  for f in "$ROOT_VOLUME/database/dbInit"/*; do
    [ -e "$f" ] || continue
    dst="$HARNESS_VOLUME/database/dbInit/$(basename "$f")"
    if [ ! -e "$dst" ]; then
      cp "$f" "$dst"
      echo "  copied dbInit/$(basename "$f")"
    fi
  done
fi

# nginx.conf: always regenerate from root so hostnames match harness network (frontend, oe)
if [ -f "$ROOT_VOLUME/nginx/nginx.conf" ]; then
  sed -e 's/frontend\.openelis\.org/frontend/g' -e 's/oe\.openelis\.org/oe/g' "$ROOT_VOLUME/nginx/nginx.conf" > "$HARNESS_VOLUME/nginx/nginx.conf"
  echo "  generated volume/nginx/nginx.conf (hostnames -> frontend, oe)"
fi

# Placeholders so bind mounts exist
if [ ! -f "$HARNESS_VOLUME/analyzer/analyzer-test-map.csv" ]; then
  touch "$HARNESS_VOLUME/analyzer/analyzer-test-map.csv"
  echo "  created placeholder analyzer/analyzer-test-map.csv"
fi
if [ ! -f "$HARNESS_VOLUME/menu/menu_config.json" ]; then
  echo '{}' > "$HARNESS_VOLUME/menu/menu_config.json"
  echo "  created placeholder menu/menu_config.json"
fi

# --- WAR check ---
WAR="$REPO_ROOT/target/OpenELIS-Global.war"
if [ ! -f "$WAR" ]; then
  echo -e "  ${YELLOW}WARN: $WAR not found. Run ./build.sh or mvn clean install -DskipTests -Dmaven.test.skip=true from repo root.${NC}"
else
  echo -e "  ${GREEN}✓ WAR found${NC}"
fi

echo -e "${GREEN}Bootstrap done.${NC}"
