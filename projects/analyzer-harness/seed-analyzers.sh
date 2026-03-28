#!/usr/bin/env bash
# seed-analyzers.sh — Create harness analyzers via OE REST API
#
# Default (clean mode): wipes stale analyzer data, then creates 7 seed analyzers
# with mock networks. Ensures a clean baseline on every startup.
#
# --no-clean: skips cleanup, runs idempotently (for manual testing).
#
# Creates 7 analyzers using profile-based defaultConfigId, which triggers:
#   - autoCreateTestMappings() from profile LOINCs
#   - autoCreateFromProfile() for FILE analyzers (FileImportConfig)
#   - registerWithBridge() for TCP analyzers (bridge transport binding)
#
# Usage:
#   ./seed-analyzers.sh                          # clean + seed (default)
#   ./seed-analyzers.sh --no-clean               # seed only (idempotent)
#   BASE_URL=https://myhost TEST_USER=u TEST_PASS=p ./seed-analyzers.sh

set -euo pipefail

CLEAN=true
if [[ "${1:-}" == "--no-clean" ]]; then
  CLEAN=false
fi

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

MOCK_URL="${MOCK_URL:-http://localhost:8085}"

# Stable IP assignments per analyzer (must match FIXED_SUBNETS in analyzer_network_manager.py).
# Format: 10.42.{subnet_id}.10 — each analyzer always gets the same IP.
declare -A STABLE_IPS=(
  [genexpert]="10.42.20.10"
  [bc5380]="10.42.21.10"
  [bs200]="10.42.22.10"
  [bs300]="10.42.23.10"
)

# Create dynamic Docker network per TCP analyzer — each gets a unique, stable IP
# so the bridge can identify them individually.
create_mock_network() {
  local name="$1"
  local template="$2"
  local port="${3:-0}"

  local resp
  resp=$(curl -sk -X POST "${MOCK_URL}/analyzers" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${name}\",\"template\":\"${template}\",\"port\":${port}}" 2>/dev/null)

  local ip
  ip=$(echo "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('ip',''))" 2>/dev/null)

  local expected_ip="${STABLE_IPS[$name]:-}"

  if [ -n "$ip" ]; then
    echo "  Mock network: ${name} → ${ip}" >&2
    echo "$ip"
  elif [ -n "$expected_ip" ]; then
    echo "  WARN: Mock API failed for ${name} — using stable IP ${expected_ip}" >&2
    echo "$expected_ip"
  else
    echo "  WARN: Failed to create mock network for ${name} — using fallback 172.21.1.100" >&2
    echo "172.21.1.100"
  fi
}

echo "Seeding analyzers via REST API at ${API}"
echo ""

# Clean stale data (default). Ensures clean baseline on every startup.
resolve_db_container() {
  if [ -n "${DB_CONTAINER:-}" ]; then
    echo "$DB_CONTAINER"
    return 0
  fi

  for candidate in analyzer-harness-db-1 openelisglobal-database; do
    if docker ps -a --format '{{.Names}}' | rg -x "$candidate" >/dev/null; then
      echo "$candidate"
      return 0
    fi
  done

  local detected
  detected=$(docker ps -a --format '{{.Names}}' | rg 'db' | head -n 1 || true)
  if [ -n "$detected" ]; then
    echo "$detected"
    return 0
  fi

  echo "ERROR: Could not determine database container for analyzer cleanup." >&2
  return 1
}

DB_CONTAINER="$(resolve_db_container)"

if [ "$CLEAN" = true ]; then
  echo "Cleaning stale analyzer data..."
  docker exec -i "$DB_CONTAINER" psql -U clinlims -d clinlims -c \
    "DELETE FROM clinlims.analyzer_results; DELETE FROM clinlims.analyzer;" \
    2>&1 | sed 's/^/  /'
  echo "  DB cleanup done"

  # Remove mock networks (per-analyzer endpoint)
  for name in genexpert bc5380 bs200 bs300; do
    curl -sk -X DELETE "${MOCK_URL}/analyzers/${name}" 2>/dev/null || true
  done
  echo "  Mock network cleanup done"
  echo ""
fi

# Create dynamic networks for TCP analyzers
echo "Creating dynamic mock networks..."
GX_IP=$(create_mock_network "genexpert" "genexpert_astm" 9600)
BC5380_IP=$(create_mock_network "bc5380" "mindray_bc5380" 5380)
BS200_IP=$(create_mock_network "bs200" "mindray_bs200" 6001)
BS300_IP=$(create_mock_network "bs300" "mindray_bs300" 6002)
echo ""

# 1. GeneXpert (ASTM) — dynamic IP from mock network
create_analyzer "Cepheid GeneXpert (ASTM Mode)" "{
  \"name\": \"Cepheid GeneXpert (ASTM Mode)\",
  \"analyzerType\": \"MOLECULAR\",
  \"pluginTypeId\": \"generic-astm\",
  \"ipAddress\": \"${GX_IP}\",
  \"port\": 9600,
  \"protocolVersion\": \"ASTM_LIS2_A2\",
  \"communicationMode\": \"ANALYZER_INITIATED\",
  \"identifierPattern\": \"GENEXPERT|CEPHEID\",
  \"status\": \"ACTIVE\",
  \"defaultConfigId\": \"astm/genexpert-astm\"
}"

# 2. QuantStudio 5 (FILE/EXCEL .xls)
create_analyzer "QuantStudio 5" '{
  "name": "QuantStudio 5",
  "analyzerType": "MOLECULAR",
  "pluginTypeId": "generic-file",
  "status": "ACTIVE",
  "defaultConfigId": "file/quantstudio"
}'

# 3. QuantStudio 7 (FILE/EXCEL — same profile as QS5, brace glob matches both .xls/.xlsx)
create_analyzer "QuantStudio 7" '{
  "name": "QuantStudio 7",
  "analyzerType": "MOLECULAR",
  "pluginTypeId": "generic-file",
  "status": "ACTIVE",
  "defaultConfigId": "file/quantstudio"
}'

# 4. FluoroCycler XT (FILE/EXCEL)
create_analyzer "FluoroCycler XT" '{
  "name": "FluoroCycler XT",
  "analyzerType": "MOLECULAR",
  "pluginTypeId": "generic-file",
  "status": "ACTIVE",
  "defaultConfigId": "file/fluorocycler-xt"
}'

# 5. Mindray BC-5380 (HL7/MLLP — hematology) — dynamic IP
create_analyzer "Mindray BC-5380" "{
  \"name\": \"Mindray BC-5380\",
  \"analyzerType\": \"HEMATOLOGY\",
  \"pluginTypeId\": \"generic-hl7\",
  \"ipAddress\": \"${BC5380_IP}\",
  \"port\": 5380,
  \"protocolVersion\": \"HL7_V2_3_1\",
  \"communicationMode\": \"ANALYZER_INITIATED\",
  \"identifierPattern\": \"MINDRAY.*BC.?5380|BC.?5380\",
  \"status\": \"ACTIVE\",
  \"defaultConfigId\": \"hl7/mindray-bc5380\"
}"

# 6. Mindray BS-200 (HL7/MLLP — chemistry) — dynamic IP
create_analyzer "Mindray BS-200" "{
  \"name\": \"Mindray BS-200\",
  \"analyzerType\": \"CHEMISTRY\",
  \"pluginTypeId\": \"generic-hl7\",
  \"ipAddress\": \"${BS200_IP}\",
  \"port\": 6001,
  \"protocolVersion\": \"HL7_V2_3_1\",
  \"communicationMode\": \"ANALYZER_INITIATED\",
  \"identifierPattern\": \"MINDRAY.*BS.?200|BS200\",
  \"status\": \"ACTIVE\",
  \"defaultConfigId\": \"hl7/mindray-bs200\"
}"

# 7. Mindray BS-300 (HL7/MLLP — chemistry) — dynamic IP
create_analyzer "Mindray BS-300" "{
  \"name\": \"Mindray BS-300\",
  \"analyzerType\": \"CHEMISTRY\",
  \"pluginTypeId\": \"generic-hl7\",
  \"ipAddress\": \"${BS300_IP}\",
  \"port\": 6002,
  \"protocolVersion\": \"HL7_V2_3_1\",
  \"communicationMode\": \"ANALYZER_INITIATED\",
  \"identifierPattern\": \"MINDRAY.*BS.?300|BS300\",
  \"status\": \"ACTIVE\",
  \"defaultConfigId\": \"hl7/mindray-bs300\"
}"

echo ""
echo "Done. 7 analyzers seeded (4 ASTM/FILE + 3 HL7/MLLP)."
