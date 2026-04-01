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

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

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
  if [ -f "$REPO_ROOT/.env" ]; then
    set -a && . "$REPO_ROOT/.env" && set +a
    TEST_PASS="${TEST_PASS:-}"
  fi
  if [ -z "$TEST_PASS" ]; then
    echo "ERROR: TEST_PASS not set. Export it or add to .env" >&2
    exit 1
  fi
fi

sql_escape() {
  printf '%s' "${1//\'/\'\'}"
}

psql_query() {
  docker exec -i "$DB_CONTAINER" psql -U clinlims -d clinlims -t -A -F $'\t' -c "$1"
}

profile_path_for_default_config() {
  local default_config_id="$1"
  local profile_type="${default_config_id%%/*}"
  local profile_name="${default_config_id#*/}"
  echo "$REPO_ROOT/projects/analyzer-profiles/${profile_type}/${profile_name}.json"
}

profile_mappings() {
  local default_config_id="$1"
  local profile_path
  profile_path="$(profile_path_for_default_config "$default_config_id")"

  if [ ! -f "$profile_path" ]; then
    echo "ERROR: Profile file not found for ${default_config_id}: ${profile_path}" >&2
    return 1
  fi

  python3 - "$profile_path" <<'PY'
import json
import sys

profile_path = sys.argv[1]
with open(profile_path, encoding="utf-8") as handle:
    data = json.load(handle)

for mapping in data.get("default_test_mappings", []):
    code = mapping.get("test_code") or mapping.get("analyzer_code") or ""
    loinc = mapping.get("loinc") or ""
    if code and loinc:
        print(f"{code}\t{loinc}")
PY
}

verify_profile_catalog_ready() {
  local analyzer_name="$1"
  local default_config_id="$2"
  local missing=()

  while IFS=$'\t' read -r test_code loinc; do
    [ -z "$test_code" ] && continue

    local active_test_id
    active_test_id="$(psql_query "SELECT id FROM clinlims.test WHERE loinc = '$(sql_escape "$loinc")' AND is_active = 'Y' ORDER BY id LIMIT 1;")"

    if [ -z "$active_test_id" ]; then
      missing+=("${test_code} (LOINC ${loinc})")
    fi
  done < <(profile_mappings "$default_config_id")

  if [ "${#missing[@]}" -gt 0 ]; then
    echo "ERROR: Harness catalog cannot realize required profile mappings for ${analyzer_name} (${default_config_id})." >&2
    echo "       Missing active OE tests for: ${missing[*]}" >&2
    echo "       Fix the authoritative harness startup catalog under projects/analyzer-harness/config-templates/ first." >&2
    return 1
  fi
}

verify_realized_analyzer_mappings() {
  local analyzer_name="$1"
  local default_config_id="$2"
  local analyzer_id
  local missing=()

  analyzer_id="$(psql_query "SELECT id FROM clinlims.analyzer WHERE name = '$(sql_escape "$analyzer_name")' ORDER BY id DESC LIMIT 1;")"
  if [ -z "$analyzer_id" ]; then
    echo "ERROR: Could not resolve analyzer id for '${analyzer_name}' during mapping verification." >&2
    return 1
  fi

  while IFS=$'\t' read -r test_code loinc; do
    [ -z "$test_code" ] && continue

    local mapped_test_id
    mapped_test_id="$(psql_query "SELECT test_id FROM clinlims.analyzer_test_map WHERE analyzer_id = ${analyzer_id} AND analyzer_test_name = '$(sql_escape "$test_code")' LIMIT 1;")"

    if [ -z "$mapped_test_id" ]; then
      missing+=("${test_code} (LOINC ${loinc})")
    fi
  done < <(profile_mappings "$default_config_id")

  if [ "${#missing[@]}" -gt 0 ]; then
    echo "ERROR: Analyzer '${analyzer_name}' was created, but required profile mappings were not realized." >&2
    echo "       Missing analyzer_test_map rows for: ${missing[*]}" >&2
    echo "       Harness boot must stop here because Playwright assertions would be invalid." >&2
    return 1
  fi
}

verify_seed_contract() {
  echo "Verifying harness profile prerequisites and realized mappings..."
  verify_profile_catalog_ready "Cepheid GeneXpert (ASTM Mode)" "astm/genexpert-astm"
  verify_profile_catalog_ready "QuantStudio 5" "file/quantstudio"
  verify_profile_catalog_ready "QuantStudio 7" "file/quantstudio"
  verify_profile_catalog_ready "FluoroCycler XT" "file/fluorocycler-xt"
  verify_profile_catalog_ready "Mindray BC-5380" "hl7/mindray-bc5380"
  verify_profile_catalog_ready "Mindray BS-200" "hl7/mindray-bs200"
  verify_profile_catalog_ready "Mindray BS-300" "hl7/mindray-bs300"

  verify_realized_analyzer_mappings "Cepheid GeneXpert (ASTM Mode)" "astm/genexpert-astm"
  verify_realized_analyzer_mappings "QuantStudio 5" "file/quantstudio"
  verify_realized_analyzer_mappings "QuantStudio 7" "file/quantstudio"
  verify_realized_analyzer_mappings "FluoroCycler XT" "file/fluorocycler-xt"
  verify_realized_analyzer_mappings "Mindray BC-5380" "hl7/mindray-bc5380"
  verify_realized_analyzer_mappings "Mindray BS-200" "hl7/mindray-bs200"
  verify_realized_analyzer_mappings "Mindray BS-300" "hl7/mindray-bs300"
  echo "  Verified: harness catalog and analyzer mappings match seeded profiles"
}

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

# Create dynamic Docker network per TCP analyzer — each gets a unique, stable IP
# so the bridge can identify them individually.
lookup_mock_network_ip() {
  local name="$1"
  local response_file
  local error_file
  response_file="$(mktemp)"
  error_file="$(mktemp)"

  local curl_exit
  set +e
  curl -sk --connect-timeout 3 --max-time 15 "${MOCK_URL}/analyzers" \
    >"$response_file" 2>"$error_file"
  curl_exit=$?
  set -e

  local ip=""
  ip="$(
    python3 - "$name" "$response_file" <<'PY' 2>/dev/null || true
import json
import sys

name = sys.argv[1]
path = sys.argv[2]

with open(path, "r", encoding="utf-8") as handle:
    payload = json.load(handle)

for analyzer in payload.get("analyzers", []):
    if analyzer.get("name") == name:
        print(analyzer.get("ip", ""))
        break
PY
  )"

  if [ -z "$ip" ] && { [ "$curl_exit" -ne 0 ] || [ -s "$error_file" ]; }; then
    echo "WARN: mock analyzer lookup for ${name} failed (curl exit ${curl_exit})" >&2
    sed 's/^/  /' "$error_file" >&2 || true
  fi

  rm -f "$response_file" "$error_file"
  echo "$ip"
}

create_mock_network() {
  local name="$1"
  local template="$2"
  local port="${3:-0}"
  local response_file
  local error_file
  response_file="$(mktemp)"
  error_file="$(mktemp)"

  local curl_exit
  set +e
  curl -sk --connect-timeout 3 --max-time 15 -X POST "${MOCK_URL}/analyzers" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${name}\",\"template\":\"${template}\",\"port\":${port}}" \
    >"$response_file" 2>"$error_file"
  curl_exit=$?
  set -e

  local resp
  resp="$(python3 - "$response_file" <<'PY' 2>/dev/null || true
from pathlib import Path
import sys

print(Path(sys.argv[1]).read_text(encoding="utf-8"))
PY
  )"

  local ip=""
  ip="$(
    printf '%s' "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('ip',''))" 2>/dev/null || true
  )"

  if [ -n "$ip" ]; then
    rm -f "$response_file" "$error_file"
    echo "  Mock network: ${name} → ${ip}" >&2
    echo "$ip"
    return 0
  fi

  echo "WARN: mock API create response for ${name} did not return an IP (curl exit ${curl_exit})" >&2
  if [ -s "$error_file" ]; then
    echo "  curl stderr:" >&2
    sed 's/^/    /' "$error_file" >&2
  fi
  if [ -n "$resp" ]; then
    echo "  response body:" >&2
    printf '%s\n' "$resp" | sed 's/^/    /' >&2
  fi

  local recovered_ip=""
  local attempt
  for attempt in 1 2 3; do
    sleep 1
    recovered_ip="$(lookup_mock_network_ip "$name")"
    if [ -n "$recovered_ip" ]; then
      rm -f "$response_file" "$error_file"
      echo "WARN: recovered mock network for ${name} via GET verification fallback on attempt ${attempt}: ${recovered_ip}" >&2
      echo "$recovered_ip"
      return 0
    fi
  done

  rm -f "$response_file" "$error_file"
  echo "ERROR: Failed to create mock network for ${name}; mock API did not return an IP." >&2
  echo "       This is a hard failure because analyzer registration must match actual transport source." >&2
  return 1
}

echo "Seeding analyzers via REST API at ${API}"
echo ""

# Clean stale data (default). Ensures clean baseline on every startup.
resolve_db_container() {
  local names
  names="$(docker ps -a --format '{{.Names}}' || true)"

  if [ -n "${DB_CONTAINER:-}" ]; then
    echo "$DB_CONTAINER"
    return 0
  fi

  for candidate in analyzer-harness-db-1 openelisglobal-database; do
    if printf '%s\n' "$names" | grep -Fx "$candidate" >/dev/null; then
      echo "$candidate"
      return 0
    fi
  done

  local detected
  detected=$(printf '%s\n' "$names" | grep -i 'db' | head -n 1 || true)
  if [ -n "$detected" ]; then
    echo "$detected"
    return 0
  fi

  echo "ERROR: Could not determine database container for analyzer cleanup." >&2
  if [ -n "$names" ]; then
    echo "Known containers: $names" | tr '\n' ' ' >&2
    echo >&2
  else
    echo "Known containers: (none)" >&2
  fi
  echo "Set DB_CONTAINER explicitly to override auto-detection." >&2
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
    curl -sk --connect-timeout 3 --max-time 10 -X DELETE "${MOCK_URL}/analyzers/${name}" 2>/dev/null || true
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

# 1. GeneXpert (ASTM) — fixed mock IP from dedicated analyzer network.
# Source-binding is authoritative, so the seeded registration must match the
# actual mock-network source identity used by ASTM pushes in the harness.
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
verify_seed_contract
echo ""
echo "Done. 7 analyzers seeded (4 ASTM/FILE + 3 HL7/MLLP)."
