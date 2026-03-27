#!/usr/bin/env bash
# seed-analyzers.sh — Create harness analyzers via OE REST API
#
# Creates 7 analyzers using profile-based defaultConfigId, which triggers:
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

MOCK_URL="${MOCK_URL:-http://localhost:8085}"

# Create dynamic Docker network per TCP analyzer — each gets a unique IP
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

  if [ -z "$ip" ]; then
    echo "  WARN: Failed to create mock network for ${name} — using fallback 172.21.1.100" >&2
    echo "172.21.1.100"
  else
    echo "  Mock network: ${name} → ${ip}" >&2
    echo "$ip"
  fi
}

echo "Seeding analyzers via REST API at ${API}"
echo ""

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
