#!/usr/bin/env bash
# Seed the M1 priority analyzer instances through the OpenELIS REST API.
#
# Profile content and revisions come from the Bridge catalog. This script owns
# only harness instance values: names, network address/port, and FILE inboxes.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

CLEAN=true
if [[ "$#" -gt 0 && "$1" == "--no-clean" ]]; then
  CLEAN=false
fi

if [ -f "$REPO_ROOT/.env" ]; then
  set -a
  . "$REPO_ROOT/.env"
  set +a
fi

BASE_URL="${BASE_URL:-https://localhost}"
MOCK_URL="${MOCK_URL:-http://localhost:8085}"
ANALYZER_API="$BASE_URL/api/OpenELIS-Global/rest/analyzer/analyzers"
TYPE_API="$BASE_URL/api/OpenELIS-Global/rest/analyzer-types"

TEST_USER="${TEST_USER:-admin}"
TEST_PASS="${TEST_PASS:-adminADMIN!}"

GENEXPERT_PROFILE_ID="genexpert-astm"
FLUOROCYCLER_PROFILE_ID="fluorocycler-xt"
QUANTSTUDIO_PROFILE_ID="quantstudio"

CATALOG_FILE="$(mktemp)"
ANALYZERS_FILE="$(mktemp)"
RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$CATALOG_FILE" "$ANALYZERS_FILE" "$RESPONSE_FILE"' EXIT

fetch_json() {
  local url="$1"
  local output="$2"
  local label="$3"
  local status
  status="$(curl -sk --connect-timeout 5 --max-time 30 -o "$output" -w "%{http_code}" -u "$TEST_USER:$TEST_PASS" "$url")"
  if [ "$status" != "200" ]; then
    echo "ERROR: $label returned HTTP $status" >&2
    return 1
  fi
}

resolve_active_revision() {
  local profile_id="$1"
  python3 - "$CATALOG_FILE" "$profile_id" <<'PY'
import json
import sys

catalog_path, profile_id = sys.argv[1:]
with open(catalog_path, encoding="utf-8") as handle:
    catalog = json.load(handle)

matches = [
    item
    for item in catalog.get("types", [])
    if item.get("profileId") == profile_id and item.get("status") == "ACTIVE"
]
if len(matches) != 1:
    raise SystemExit(
        f"expected exactly one active revision for {profile_id}; found {len(matches)}"
    )
revision = matches[0].get("revision")
if not isinstance(revision, int) or revision < 1:
    raise SystemExit(f"active revision for {profile_id} is invalid: {revision!r}")
print(revision)
PY
}

analyzer_exists() {
  local analyzer_name="$1"
  fetch_json "$ANALYZER_API" "$ANALYZERS_FILE" "Analyzer list"
  python3 - "$ANALYZERS_FILE" "$analyzer_name" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    analyzers = json.load(handle).get("analyzers", [])
raise SystemExit(0 if any(item.get("name") == sys.argv[2] for item in analyzers) else 1)
PY
}

delete_harness_analyzers() {
  fetch_json "$ANALYZER_API" "$ANALYZERS_FILE" "Analyzer list"
  while IFS=$'\t' read -r analyzer_id analyzer_name; do
    [ -z "$analyzer_id" ] && continue
    local status
    status="$(curl -sk --connect-timeout 5 --max-time 30 -o "$RESPONSE_FILE" -w "%{http_code}" -X POST -u "$TEST_USER:$TEST_PASS" "$ANALYZER_API/$analyzer_id/delete")"
    if [ "$status" != "200" ]; then
      echo "ERROR: Failed to delete harness analyzer $analyzer_name (HTTP $status)" >&2
      return 1
    fi
    echo "  Deleted: $analyzer_name"
  done < <(
    python3 - "$ANALYZERS_FILE" <<'PY'
import json
import sys

names = {
    "Cepheid GeneXpert (ASTM Mode)",
    "QuantStudio 5",
    "QuantStudio 7",
    "FluoroCycler XT",
}
with open(sys.argv[1], encoding="utf-8") as handle:
    analyzers = json.load(handle).get("analyzers", [])
for analyzer in analyzers:
    if analyzer.get("name") in names:
        print(f"{analyzer.get('id', '')}\t{analyzer['name']}")
PY
  )
}

create_profile_analyzer() {
  local name="$1"
  local profile_id="$2"
  local profile_revision="$3"
  local ip_address=""
  local port=""
  local import_directory=""
  [ "$#" -ge 4 ] && ip_address="$4"
  [ "$#" -ge 5 ] && port="$5"
  [ "$#" -ge 6 ] && import_directory="$6"

  if analyzer_exists "$name"; then
    echo "  Exists:  $name (skipped)"
    return 0
  fi

  local payload
  payload="$(
    python3 - "$name" "$profile_id" "$profile_revision" "$ip_address" "$port" "$import_directory" <<'PY'
import json
import sys

name, profile_id, revision, ip_address, port, import_directory = sys.argv[1:]
payload = {
    "name": name,
    "profileId": profile_id,
    "profileRevision": int(revision),
}
if ip_address:
    payload["ipAddress"] = ip_address
if port:
    payload["port"] = int(port)
if import_directory:
    payload["importDirectory"] = import_directory
print(json.dumps(payload, separators=(",", ":")))
PY
  )"

  local status
  status="$(curl -sk --connect-timeout 5 --max-time 45 -o "$RESPONSE_FILE" -w "%{http_code}" -X POST "$ANALYZER_API" -u "$TEST_USER:$TEST_PASS" -H "Content-Type: application/json" -d "$payload")"
  if [ "$status" != "201" ]; then
    echo "ERROR: Failed to create $name (HTTP $status)" >&2
    sed 's/^/  /' "$RESPONSE_FILE" >&2
    return 1
  fi
  echo "  Created: $name ($profile_id@$profile_revision)"
}

lookup_mock_network_ip() {
  local name="$1"
  curl -sk --connect-timeout 3 --max-time 15 "$MOCK_URL/analyzers" |
    python3 -c '
import json
import sys

name = sys.argv[1]
for analyzer in json.load(sys.stdin).get("analyzers", []):
    if analyzer.get("name") == name:
        print(analyzer.get("ip", ""))
        break
' "$name"
}

create_mock_network() {
  local name="$1"
  local template="$2"
  local port="$3"
  local attempt
  local status
  local payload
  payload="$(python3 -c 'import json,sys; print(json.dumps({"name":sys.argv[1],"template":sys.argv[2],"port":int(sys.argv[3])}))' "$name" "$template" "$port")"

  for attempt in 1 2 3 4 5; do
    status="$(curl -sk --connect-timeout 3 --max-time 20 -o "$RESPONSE_FILE" -w "%{http_code}" -X POST "$MOCK_URL/analyzers" -H "Content-Type: application/json" -d "$payload" || true)"
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
      python3 - "$RESPONSE_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    print(json.load(handle).get("ip", ""))
PY
      return 0
    fi
    if [ "$status" = "409" ]; then
      lookup_mock_network_ip "$name"
      return 0
    fi
    sleep "$attempt"
  done

  echo "ERROR: Mock network $name was not created (last HTTP $status)" >&2
  sed 's/^/  /' "$RESPONSE_FILE" >&2
  return 1
}

verify_profile_pins() {
  fetch_json "$ANALYZER_API" "$ANALYZERS_FILE" "Analyzer list"
  python3 - "$ANALYZERS_FILE" "$GENEXPERT_REVISION" "$QUANTSTUDIO_REVISION" "$FLUOROCYCLER_REVISION" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    analyzers = json.load(handle).get("analyzers", [])

expected = {
    "Cepheid GeneXpert (ASTM Mode)": ("genexpert-astm", int(sys.argv[2])),
    "QuantStudio 5": ("quantstudio", int(sys.argv[3])),
    "QuantStudio 7": ("quantstudio", int(sys.argv[3])),
    "FluoroCycler XT": ("fluorocycler-xt", int(sys.argv[4])),
}
problems = []
for name, pin in expected.items():
    matches = [
        item
        for item in analyzers
        if item.get("name") == name
        and (item.get("profileId"), item.get("profileRevision")) == pin
    ]
    if len(matches) != 1:
        problems.append(f"{name}: expected one {pin[0]}@{pin[1]} instance, found {len(matches)}")

if problems:
    raise SystemExit("\n".join(problems))
print("  Verified: all M1 harness analyzers persist their exact Bridge profile pins")
PY
}

echo "Resolving priority profiles from $TYPE_API..."
fetch_json "$TYPE_API" "$CATALOG_FILE" "Analyzer Types catalog"
GENEXPERT_REVISION="$(resolve_active_revision "$GENEXPERT_PROFILE_ID")"
FLUOROCYCLER_REVISION="$(resolve_active_revision "$FLUOROCYCLER_PROFILE_ID")"
QUANTSTUDIO_REVISION="$(resolve_active_revision "$QUANTSTUDIO_PROFILE_ID")"
echo "  $GENEXPERT_PROFILE_ID@$GENEXPERT_REVISION"
echo "  $FLUOROCYCLER_PROFILE_ID@$FLUOROCYCLER_REVISION"
echo "  $QUANTSTUDIO_PROFILE_ID@$QUANTSTUDIO_REVISION"

if [ "$CLEAN" = true ]; then
  echo "Removing only the named harness analyzers..."
  delete_harness_analyzers
  curl -sk --connect-timeout 3 --max-time 10 -X DELETE "$MOCK_URL/analyzers/genexpert" >/dev/null 2>&1 || true
fi

echo "Creating GeneXpert mock transport..."
GENEXPERT_IP="$(create_mock_network "genexpert" "genexpert_astm" 9600)"
if [ -z "$GENEXPERT_IP" ]; then
  echo "ERROR: GeneXpert mock transport returned no IP address" >&2
  exit 1
fi
echo "  genexpert -> $GENEXPERT_IP:9600"

echo "Creating profile-pinned analyzer instances..."
create_profile_analyzer "Cepheid GeneXpert (ASTM Mode)" "$GENEXPERT_PROFILE_ID" "$GENEXPERT_REVISION" "$GENEXPERT_IP" "9600"
create_profile_analyzer "QuantStudio 5" "$QUANTSTUDIO_PROFILE_ID" "$QUANTSTUDIO_REVISION" "" "" "/data/analyzer-imports/quantstudio-5/incoming"
create_profile_analyzer "QuantStudio 7" "$QUANTSTUDIO_PROFILE_ID" "$QUANTSTUDIO_REVISION" "" "" "/data/analyzer-imports/quantstudio-7/incoming"
create_profile_analyzer "FluoroCycler XT" "$FLUOROCYCLER_PROFILE_ID" "$FLUOROCYCLER_REVISION" "" "" "/data/analyzer-imports/fluorocycler-xt/incoming"

verify_profile_pins
echo "Done. Four instances use the three validated M1 Bridge profile families."
