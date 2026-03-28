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

# 5. Mindray BC-5380 (HL7/MLLP — hematology)
create_analyzer "Mindray BC-5380" '{
  "name": "Mindray BC-5380",
  "analyzerType": "HEMATOLOGY",
  "pluginTypeId": "generic-hl7",
  "ipAddress": "172.21.1.1",
  "port": 5380,
  "protocolVersion": "HL7_V2_3_1",
  "identifierPattern": "MINDRAY.*BC.?5380|BC.?5380",
  "status": "ACTIVE",
  "defaultConfigId": "hl7/mindray-bc5380"
}'

# 6. Mindray BS-200 (HL7/MLLP — chemistry)
create_analyzer "Mindray BS-200" '{
  "name": "Mindray BS-200",
  "analyzerType": "CHEMISTRY",
  "pluginTypeId": "generic-hl7",
  "ipAddress": "172.21.1.1",
  "port": 6001,
  "protocolVersion": "HL7_V2_3_1",
  "identifierPattern": "MINDRAY.*BS.?200|BS200",
  "status": "ACTIVE",
  "defaultConfigId": "hl7/mindray-bs200"
}'

# 7. Mindray BS-300 (HL7/MLLP — chemistry)
create_analyzer "Mindray BS-300" '{
  "name": "Mindray BS-300",
  "analyzerType": "CHEMISTRY",
  "pluginTypeId": "generic-hl7",
  "ipAddress": "172.21.1.1",
  "port": 6002,
  "protocolVersion": "HL7_V2_3_1",
  "identifierPattern": "MINDRAY.*BS.?300|BS300",
  "status": "ACTIVE",
  "defaultConfigId": "hl7/mindray-bs300"
}'

echo ""
echo "Done. 7 analyzers seeded (4 ASTM/FILE + 3 HL7/MLLP)."
