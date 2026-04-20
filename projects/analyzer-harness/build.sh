#!/usr/bin/env bash
# Build script for analyzer harness: WAR + harness Docker images + CI parity images.
#
# The local reset flow uses the dev/analyzer-test compose stack, but the local
# CI parity flow (`ci-parity-test.sh`) uses `build.docker-compose.yml` plus the
# analyzer harness CI overlay with `--no-build`. After a cold Docker cleanup we
# need both image sets available, otherwise the supported reboot path and the
# supported parity path drift apart.
#
# Usage: ./build.sh [options]
#   --skip-war     Skip Maven WAR build (use existing target/OpenELIS-Global.war)
#   --skip-images  Skip Docker image builds (only build WAR)
#   --help         Show this help

set -e

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"
source "$HARNESS_DIR/compose-stack.sh"

LOCAL_COMPOSE_FILES=($(compose_args_local false))
CI_COMPOSE_FILES=($(compose_args_ci))

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
  echo "[2/2] Building harness Docker images (dev stack + parity image set)..."
  cd "$HARNESS_DIR"

  # Dev stack: explicitly name the services that contain branch-local source
  # (oe + frontend). Without explicit service names, `docker compose build`
  # silently skips services that are missing a `build:` directive — which
  # masked the root cause of a class of bugs where the harness ran develop's
  # published images instead of the local branch. Explicit names make a
  # missing `build:` directive error out loudly instead.
  #
  # The frontend service's compose entry declares `target: dev` so this
  # selects the dev stage of the single multi-stage frontend/Dockerfile.
  # No separate base-image pre-build step needed (layer cache dedupes the
  # shared stages automatically).
  docker compose "${LOCAL_COMPOSE_FILES[@]}" build oe.openelis.org frontend.openelis.org

  # Confirm the expected image tags exist locally so compose up doesn't
  # silently fall back to the pulled :develop tags.
  for tag in \
    itechuw/openelis-global-2-dev:develop \
    itechuw/openelis-global-2-frontend-dev:develop; do
    if ! docker image inspect "$tag" >/dev/null 2>&1; then
      echo "ERROR: expected image '$tag' not present after build." >&2
      echo "Check that the compose file has a 'build:' directive for the" >&2
      echo "corresponding service and that the Dockerfile path is correct." >&2
      exit 1
    fi
    echo "  ✓ $tag"
  done

  cd "$REPO_ROOT"
  docker compose "${CI_COMPOSE_FILES[@]}" build
  echo "  ✓ Images built (dev stack: oe + frontend; CI parity set)"
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
