#!/usr/bin/env bash
# Build all analyzer plugins. Handles the OpenELIS classes JAR prerequisite.
#
# Usage: ./build-plugins.sh [options] [mvn-args...]
#   --rebuild-oe   Force rebuild of the OpenELIS classes JAR
#   --help         Show this help
#
# Examples:
#   ./build-plugins.sh                             # Build all plugins
#   ./build-plugins.sh -pl :GeneXpert -am          # Build one plugin
set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGINS_DIR="$REPO_ROOT/plugins"

# Check submodule
if [ ! -f "$PLUGINS_DIR/pom.xml" ]; then
  echo "Initializing plugins submodule..."
  git -C "$REPO_ROOT" submodule update --init plugins
fi

REBUILD_OE=false
MVN_ARGS=()
for arg in "$@"; do
  case "$arg" in
    --rebuild-oe) REBUILD_OE=true ;;
    --help) head -12 "$0" | tail -9; exit 0 ;;
    *) MVN_ARGS+=("$arg") ;;
  esac
done

# Check if classes JAR exists in local Maven cache
OE_VERSION="3.2.1.2"
CLASSES_JAR="$HOME/.m2/repository/org/openelisglobal/openelisglobal/$OE_VERSION/openelisglobal-${OE_VERSION}.jar"

if [ ! -f "$CLASSES_JAR" ] || [ "$REBUILD_OE" = true ]; then
  echo "Installing OpenELIS classes JAR (required for plugins)..."
  if [ -x "$PLUGINS_DIR/scripts/install-oe-jar.sh" ]; then
    "$PLUGINS_DIR/scripts/install-oe-jar.sh"
  else
    echo "Error: $PLUGINS_DIR/scripts/install-oe-jar.sh not found or not executable." >&2
    echo "Run: git submodule update --init plugins" >&2
    exit 1
  fi
fi

echo ""
echo "Building plugins..."
cd "$PLUGINS_DIR"
if [ ${#MVN_ARGS[@]} -eq 0 ]; then
  mvn clean install -DskipTests -Dmaven.test.skip=true
else
  mvn "${MVN_ARGS[@]}"
fi

echo ""
echo "Built plugin JARs:"
ls -1 plugins/*.jar 2>/dev/null || echo "  (none found — check build output above)"
