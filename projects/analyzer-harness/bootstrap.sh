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

FORCE_BUILD_PLUGINS=false
FORCE_RELOAD_CONFIG=false
for arg in "$@"; do
  case $arg in
    --force-build-plugins) FORCE_BUILD_PLUGINS=true ;;
    --force-reload-config) FORCE_RELOAD_CONFIG=true ;;
  esac
done

echo "Bootstrap: REPO_ROOT=$REPO_ROOT HARNESS_DIR=$HARNESS_DIR"

# --- Load .env (harness-local first, then repo root) so template substitution
#     and downstream tools see LETSENCRYPT_DOMAIN etc. Required — no silent
#     fallback, because nginx server_name and cert lookups depend on it. ---
if [ -f "$HARNESS_DIR/.env" ]; then
  set -a; . "$HARNESS_DIR/.env"; set +a
elif [ -f "$REPO_ROOT/.env" ]; then
  set -a; . "$REPO_ROOT/.env"; set +a
fi
if [ -z "${LETSENCRYPT_DOMAIN:-}" ]; then
  echo -e "${RED}ERROR: LETSENCRYPT_DOMAIN is not set. Add it to $REPO_ROOT/.env before running bootstrap.${NC}" >&2
  exit 1
fi
# Bridge upload UI sits on a subdomain — defaults to "bridge.<primary>" so that
# a single .env value drives both certificates and nginx routing. Override in
# .env only if you want a non-standard bridge host (e.g. separate TLD).
: "${LETSENCRYPT_BRIDGE_DOMAIN:=bridge.${LETSENCRYPT_DOMAIN}}"
export LETSENCRYPT_DOMAIN LETSENCRYPT_BRIDGE_DOMAIN

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
# --- Generic analyzer plugins (GenericASTM, GenericFile, GenericHL7) ---
# These 3 are the only plugins needed for dashboard-configured analyzers.
# Legacy plugins are NOT loaded by default — use HARNESS_PLUGINS=all to include them.
#
# Options:
#   generic (default) - GenericASTM + GenericFile + GenericHL7 only
#   all               - all plugin JARs (includes legacy — backward-compatible)
#
# Flags (parsed at top of script):
#   --force-build-plugins  - rebuild generic plugin JARs even if they exist
HARNESS_PLUGINS="${HARNESS_PLUGINS:-generic}"
GENERIC_PLUGIN_DIRS="GenericASTM GenericFile GenericHL7"
PLUGIN_BUILD_DIR="$REPO_ROOT/plugins/analyzers"
PLUGIN_SUBMODULE_DIR="$REPO_ROOT/plugins/plugins"

_ensure_generic_plugins() {
  local force="${1:-false}"
  local built=0
  for plugin_name in $GENERIC_PLUGIN_DIRS; do
    local src_dir="$PLUGIN_BUILD_DIR/$plugin_name"
    local jar_pattern="$src_dir/target/${plugin_name}*.jar"
    # Build if JAR missing or force requested
    if [ "$force" = "true" ] || ! ls $jar_pattern 1>/dev/null 2>&1; then
      if [ -f "$src_dir/pom.xml" ]; then
        echo "  Building $plugin_name..."
        (cd "$src_dir" && mvn package -DskipTests -q) || {
          echo -e "  ${RED}FAIL: $plugin_name build failed${NC}"
          continue
        }
        built=$((built + 1))
      else
        echo -e "  ${YELLOW}WARN: $src_dir/pom.xml not found${NC}"
        continue
      fi
    fi
    # Copy JAR to harness plugins (overwrite to pick up rebuilds).
    # Select deterministically and skip non-runtime artifacts.
    local jar=""
    for candidate in "$src_dir"/target/"${plugin_name}"*.jar; do
      [ -f "$candidate" ] || continue
      case "$candidate" in
        *-sources.jar|*-javadoc.jar|*-tests.jar|*-test.jar|*-original.jar) continue ;;
      esac
      jar="$candidate"
      break
    done
    if [ -n "$jar" ]; then
      cp "$jar" "$HARNESS_VOLUME/plugins/"
    fi
  done
  local total=$(ls "$HARNESS_VOLUME/plugins"/Generic*.jar 2>/dev/null | wc -l | tr -d ' ')
  if [ "$built" -gt 0 ]; then
    echo -e "  ${GREEN}✓ Generic plugins: $total loaded ($built built)${NC}"
  else
    echo -e "  ${GREEN}✓ Generic plugins: $total loaded${NC}"
  fi
}

if [ "$HARNESS_PLUGINS" = "all" ]; then
  # Load all plugins (legacy + generic) from submodule
  _ensure_generic_plugins "$FORCE_BUILD_PLUGINS"
  if [ -d "$PLUGIN_SUBMODULE_DIR" ] && ls "$PLUGIN_SUBMODULE_DIR"/*.jar 1>/dev/null 2>&1; then
    cp -n "$PLUGIN_SUBMODULE_DIR"/*.jar "$HARNESS_VOLUME/plugins/" 2>/dev/null || true
    echo -e "  ${GREEN}✓ All plugins: $(ls "$HARNESS_VOLUME/plugins/"*.jar 2>/dev/null | wc -l | tr -d ' ') JARs${NC}"
  fi
else
  # Default: generic only — clean ALL existing JARs first (prevents stale
  # Generic versions accumulating across rebuilds), then copy fresh generics.
  rm -f "$HARNESS_VOLUME/plugins/"*.jar 2>/dev/null
  _ensure_generic_plugins "$FORCE_BUILD_PLUGINS"
  echo -e "  ${GREEN}✓ Harness plugins: generic only ($(ls "$HARNESS_VOLUME/plugins/"*.jar 2>/dev/null | wc -l | tr -d ' ') JARs)${NC}"
fi
mkdir -p "$HARNESS_VOLUME/programs"
mkdir -p "$HARNESS_VOLUME/configuration/backend"
mkdir -p "$HARNESS_VOLUME/analyzer-imports"

# --- Copy authoritative harness startup catalog into volume ---
# These CSVs are loaded by ConfigurationInitializationService on OE startup.
# Do not introduce a second source tree for harness test metadata.
CONFIG_TEMPLATES="$HARNESS_DIR/config-templates"
if [ -d "$CONFIG_TEMPLATES" ]; then
  cp -r "$CONFIG_TEMPLATES"/* "$HARNESS_VOLUME/configuration/backend/" 2>/dev/null || true
  echo -e "  ${GREEN}✓ Configuration templates copied to volume${NC}"
fi

# Clear configuration checksums so CSVs are reloaded on next OE startup.
# Always clear when DB was reset (checksums are on filesystem but data is in DB —
# when DB is dropped, checksums become stale and OE skips loading CSVs).
# Also clear when FORCE_RELOAD_CONFIG is set explicitly.
rm -f "$HARNESS_VOLUME/configuration/backend/"*-checksums.properties 2>/dev/null
echo -e "  ${GREEN}✓ Cleared configuration checksums (CSVs will reload on next startup)${NC}"

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
# Ensure bridge URL is set for analyzer registration
if [ -f "$HARNESS_VOLUME/properties/common.properties" ] && ! grep -q "analyzer.bridge.url" "$HARNESS_VOLUME/properties/common.properties"; then
  echo "" >> "$HARNESS_VOLUME/properties/common.properties"
  echo "# Analyzer bridge URL for registration sync" >> "$HARNESS_VOLUME/properties/common.properties"
  echo "analyzer.bridge.url=https://bridge.openelis.org:8443" >> "$HARNESS_VOLUME/properties/common.properties"
  echo "  added analyzer.bridge.url to common.properties"
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

# nginx.conf: render from the env-driven template in the root volume so
# ${LETSENCRYPT_DOMAIN} flows through to server_name and cert paths without
# editing nginx.conf by hand. Fallback to plain copy if only nginx.conf exists.
if [ -f "$ROOT_VOLUME/nginx/nginx.conf.template" ]; then
  if ! command -v envsubst >/dev/null 2>&1; then
    # No silent fallback — the committed nginx.conf is a stale snapshot of the
    # template and lacks the bridge vhost + env-substituted domain names.
    # Falling back to it silently mis-serves HTTPS and hides config drift.
    echo "ERROR: envsubst is required to render nginx.conf.template but was not found." >&2
    echo "  Install it (apt: gettext-base, brew: gettext) and rerun." >&2
    exit 1
  fi
  # Pass a restricted var set so unrelated shell vars (e.g. $host, $scheme
  # used as nginx runtime variables) don't get substituted out.
  envsubst '${LETSENCRYPT_DOMAIN} ${LETSENCRYPT_BRIDGE_DOMAIN}' \
    < "$ROOT_VOLUME/nginx/nginx.conf.template" \
    > "$HARNESS_VOLUME/nginx/nginx.conf"
  echo "  rendered volume/nginx/nginx.conf from template (LETSENCRYPT_DOMAIN=${LETSENCRYPT_DOMAIN}, LETSENCRYPT_BRIDGE_DOMAIN=${LETSENCRYPT_BRIDGE_DOMAIN})"
elif [ -f "$ROOT_VOLUME/nginx/nginx.conf" ]; then
  cp "$ROOT_VOLUME/nginx/nginx.conf" "$HARNESS_VOLUME/nginx/nginx.conf"
  echo "  copied volume/nginx/nginx.conf (no template found)"
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
