#!/usr/bin/env bash
# Build script for analyzer harness: WAR + plugins + all harness Docker images.
# Does not build the webapp image; the oe service uses the mounted WAR.
#
# Usage: ./build.sh [options]
#   --skip-war      Skip Maven WAR build (use existing target/OpenELIS-Global.war)
#   --skip-plugins  Skip plugin build
#   --skip-images   Skip Docker image builds (only build WAR)
#   --help          Show this help

set -e

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"
COMPOSE_DEV="$HARNESS_DIR/docker-compose.dev.yml"
COMPOSE_ANALYZER="$HARNESS_DIR/docker-compose.analyzer-test.yml"

SKIP_WAR=false
SKIP_PLUGINS=false
SKIP_IMAGES=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-war)
      SKIP_WAR=true
      shift
      ;;
    --skip-plugins)
      SKIP_PLUGINS=true
      shift
      ;;
    --skip-images)
      SKIP_IMAGES=true
      shift
      ;;
    --help)
      head -13 "$0" | tail -10
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

echo "========================================"
echo "  Analyzer Harness – Build"
echo "========================================"
echo ""

if [ "$SKIP_WAR" != true ]; then
  echo "[1/3] Building OpenELIS WAR (repo root)..."
  cd "$REPO_ROOT"
  mvn clean install -DskipTests -Dmaven.test.skip=true -q
  if [ ! -f "target/OpenELIS-Global.war" ]; then
    echo "ERROR: WAR not produced at target/OpenELIS-Global.war" >&2
    exit 1
  fi
  echo "  ✓ WAR: target/OpenELIS-Global.war"
  echo ""
else
  echo "[1/3] Skipping WAR build (--skip-war)"
  if [ ! -f "$REPO_ROOT/target/OpenELIS-Global.war" ]; then
    echo "WARN: target/OpenELIS-Global.war not found; oe service may fail to start." >&2
  fi
  echo ""
fi

if [ "$SKIP_WAR" != true ] && [ "$SKIP_PLUGINS" != true ]; then
  PLUGINS_DIR="$REPO_ROOT/plugins"
  if [ -d "$PLUGINS_DIR" ] && [ -f "$PLUGINS_DIR/pom.xml" ]; then
    echo "[2/3] Building analyzer plugins..."
    cd "$PLUGINS_DIR"
    mvn clean install -DskipTests -Dmaven.test.skip=true -q
    echo "  ✓ Plugins: $(ls plugins/*.jar 2>/dev/null | wc -l) JARs built"
    echo ""
  else
    echo "[2/3] Skipping plugins (submodule not initialized — run: git submodule update --init plugins)"
    echo ""
  fi
elif [ "$SKIP_PLUGINS" = true ]; then
  echo "[2/3] Skipping plugin build (--skip-plugins)"
  echo ""
else
  echo "[2/3] Skipping plugin build (WAR build was skipped)"
  echo ""
fi

if [ "$SKIP_IMAGES" != true ]; then
  echo "[3/3] Building harness Docker images (astm-simulator, openelis-analyzer-bridge)..."
  cd "$HARNESS_DIR"
  docker compose -f "$COMPOSE_DEV" -f "$COMPOSE_ANALYZER" build
  echo "  ✓ Images built"
  echo ""
else
  echo "[3/3] Skipping Docker image build (--skip-images)"
  echo ""
fi

echo "========================================"
echo "  Build complete"
echo "========================================"
echo "  Start harness: ./reset-env.sh [--full-reset]"
echo ""
