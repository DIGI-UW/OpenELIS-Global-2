#!/usr/bin/env bash
# seed-analyzers.sh — Create harness analyzers via OE REST API
#
# Creates 4 analyzers using profile-based defaultConfigId, which triggers:
#   - autoCreateTestMappings() from profile LOINCs
#   - autoCreateFromProfile() for FILE analyzers (FileImportConfig)
#   - registerWithBridge() for TCP analyzers (bridge transport binding)
#
# Usage:
#   ./seed-analyzers.sh                          # defaults: https://localhost, admin/adminADMIN!
#   BASE_URL=https://myhost TEST_USER=u TEST_PASS=p ./seed-analyzers.sh
#
# Idempotency: API returns 409 if analyzer name already exists (skipped gracefully).

set -euo pipefail

BASE_URL="${BASE_URL:-https://localhost}"
TEST_USER="${TEST_USER:-admin}"
TEST_PASS="${TEST_PASS:-}"
API="${BASE_URL}/api/OpenELIS-Global/rest/analyzer/analyzers"

if [ -z "$TEST_PASS" ]; then
  # Try sourcing .env from repo root
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
  REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
  if [ -f "$REPO_ROOT/.env" ]; then
    set -a && . "$REPO_ROOT/.env" && set +a
    TEST_PASS="${TEST_PASS:-}"
  fi
  if [ -z "$TEST_PASS" ]; then
    echo "ERROR: TEST_PASS not set. Export it or add to .env" >&2
    exit 1
  fi
fi

create_analyzer() {
  local name="$1"
  local json="$2"

  local http_code
  http_code=$(curl -sk -o /dev/null -w "%{http_code}" \
    -X POST "$API" \
    -u "${TEST_USER}:${TEST_PASS}" \
    -H "Content-Type: application/json" \
    -d "$json")

  case "$http_code" in
    201) echo "  Created: $name" ;;
    409) echo "  Exists:  $name (skipped)" ;;
    *)   echo "  FAILED:  $name (HTTP $http_code)" >&2; return 1 ;;
  esac
}

update_file_import_pattern() {
  local analyzer_name="$1"
  local file_pattern="$2"
  local db_container
  db_container="$(docker ps --format '{{.Names}}' | grep -E '^openelisglobal-database$|analyzer-harness.*-db-' | head -1)"
  if [ -z "$db_container" ]; then
    echo "  WARN: could not resolve database container for $analyzer_name" >&2
    return 1
  fi

  docker exec "$db_container" psql -U clinlims -d clinlims -c "
    UPDATE clinlims.file_import_configuration fic
    SET file_pattern = '${file_pattern}'
    FROM clinlims.analyzer a
    WHERE fic.analyzer_id = CAST(a.id AS integer)
      AND a.name = '${analyzer_name}';
  " >/dev/null

  echo "  Updated file pattern for ${analyzer_name}: ${file_pattern}"
}

echo "Seeding analyzers via REST API at ${API}"
echo ""

# 1. GeneXpert (ASTM) — connects to mock simulator at 172.21.1.100:9600
create_analyzer "Cepheid GeneXpert (ASTM Mode)" '{
  "name": "Cepheid GeneXpert (ASTM Mode)",
  "analyzerType": "MOLECULAR",
  "pluginTypeId": "generic-astm",
  "ipAddress": "172.21.1.100",
  "port": 9600,
  "protocolVersion": "ASTM_LIS2_A2",
  "identifierPattern": "GENEXPERT|CEPHEID",
  "status": "ACTIVE",
  "defaultConfigId": "astm/genexpert-astm"
}'

# 2. QuantStudio 5 (FILE/EXCEL .xls)
create_analyzer "QuantStudio 5" '{
  "name": "QuantStudio 5",
  "analyzerType": "MOLECULAR",
  "pluginTypeId": "generic-file",
  "status": "ACTIVE",
  "defaultConfigId": "file/quantstudio"
}'

# 3. QuantStudio 7 (FILE/EXCEL .xlsx)
create_analyzer "QuantStudio 7" '{
  "name": "QuantStudio 7",
  "analyzerType": "MOLECULAR",
  "pluginTypeId": "generic-file",
  "status": "ACTIVE",
  "defaultConfigId": "file/quantstudio"
}'
update_file_import_pattern "QuantStudio 7" "*.xlsx"

# 4. FluoroCycler XT (FILE/EXCEL)
create_analyzer "FluoroCycler XT" '{
  "name": "FluoroCycler XT",
  "analyzerType": "MOLECULAR",
  "pluginTypeId": "generic-file",
  "status": "ACTIVE",
  "defaultConfigId": "file/fluorocycler-xt"
}'

echo ""
echo "Done. 4 analyzers seeded."
