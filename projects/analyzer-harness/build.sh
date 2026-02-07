#!/usr/bin/env bash
# Build script for analyzer harness: WAR + all harness Docker images.
# Does not build the webapp image; the oe service uses the mounted WAR.
#
# Usage: ./build.sh [options]
#   --skip-war     Skip Maven WAR build (use existing target/OpenELIS-Global.war)
#   --skip-images  Skip Docker image builds (only build WAR)
#   --help         Show this help

set -e

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"
COMPOSE_DEV="$HARNESS_DIR/docker-compose.dev.yml"
COMPOSE_ANALYZER="$HARNESS_DIR/docker-compose.analyzer-test.yml"

SKIP_WAR=false
SKIP_IMAGES=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-war)
      SKIP_WAR=true
      shift
      ;;
    --skip-images)
      SKIP_IMAGES=true
      shift
      ;;
    --help)
      head -12 "$0" | tail -9
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
  echo "[1/2] Building OpenELIS WAR (repo root)..."
  cd "$REPO_ROOT"
  mvn clean install -DskipTests -Dmaven.test.skip=true -q
  if [ ! -f "target/OpenELIS-Global.war" ]; then
    echo "ERROR: WAR not produced at target/OpenELIS-Global.war" >&2
    exit 1
  fi
  echo "  ✓ WAR: target/OpenELIS-Global.war"
  echo ""
else
  echo "[1/2] Skipping WAR build (--skip-war)"
  if [ ! -f "$REPO_ROOT/target/OpenELIS-Global.war" ]; then
    echo "WARN: target/OpenELIS-Global.war not found; oe service may fail to start." >&2
  fi
  echo ""
fi

if [ "$SKIP_IMAGES" != true ]; then
  echo "[2/2] Building harness Docker images (astm-simulator, openelis-analyzer-bridge)..."
  cd "$HARNESS_DIR"
  docker compose -f "$COMPOSE_DEV" -f "$COMPOSE_ANALYZER" build
  echo "  ✓ Images built"
  echo ""
else
  echo "[2/2] Skipping Docker image build (--skip-images)"
  echo ""
fi

echo "========================================"
echo "  Build complete"
echo "========================================"
echo "  Start harness: ./reset-env.sh [--full-reset]"
echo ""
