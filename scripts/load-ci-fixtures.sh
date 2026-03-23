#!/bin/bash
#
# load-ci-fixtures.sh - Load the same test fixtures that CI loads (frontend-qa workflow)
#
# Usage:
#   ./scripts/load-ci-fixtures.sh                    # dev.docker-compose.yml
#   ./scripts/load-ci-fixtures.sh -f build.docker-compose.yml
#
# Loads:
#   src/test/resources/e2e-foundational-data.sql
#
# Prerequisites:
#   - Compose stack is up and db.openelis.org is healthy
#   - Run from repository root (or script resolves root)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

COMPOSE_FILE="dev.docker-compose.yml"
while getopts "f:h" opt; do
  case $opt in
    f) COMPOSE_FILE="$OPTARG" ;;
    h)
      echo "Usage: $0 [-f COMPOSE_FILE]"
      echo "  -f COMPOSE_FILE  Compose file (default: dev.docker-compose.yml)"
      echo ""
      echo "Loads same fixtures as CI (frontend-qa): e2e-foundational-data.sql"
      exit 0
      ;;
    *) exit 1 ;;
  esac
done

PSQL_OPTS="-U clinlims -d clinlims --set=ON_ERROR_STOP=on"
SVC="db.openelis.org"

echo "Loading CI fixtures (compose: $COMPOSE_FILE)..."
docker compose -f "$COMPOSE_FILE" exec -T "$SVC" psql $PSQL_OPTS < src/test/resources/e2e-foundational-data.sql
echo "CI fixtures loaded."
