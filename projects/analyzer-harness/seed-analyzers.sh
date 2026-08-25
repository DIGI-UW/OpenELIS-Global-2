#!/usr/bin/env bash
# Seed the M1 priority analyzer instances through the OpenELIS REST API.
#
# Profile content, revisions, and defaults come from the Bridge catalog. This
# script owns only harness instance names and explicit connection values.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

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
  local attempt
  local status
  for attempt in 1 2 3 4 5; do
    status="$(curl -sk --connect-timeout 5 --max-time 30 -o "$output" -w "%{http_code}" -u "$TEST_USER:$TEST_PASS" "$url" || true)"
    if [ "$status" = "200" ]; then
      return 0
    fi
    [ "$attempt" -lt 5 ] && sleep "$attempt"
  done
  echo "ERROR: $label returned HTTP $status after $attempt attempts" >&2
  return 1
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

find_analyzer_id() {
  local analyzer_name="$1"
  fetch_json "$ANALYZER_API" "$ANALYZERS_FILE" "Analyzer list"
  python3 - "$ANALYZERS_FILE" "$analyzer_name" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    analyzers = json.load(handle).get("analyzers", [])
matches = [item for item in analyzers if item.get("name") == sys.argv[2]]
if len(matches) > 1:
    raise SystemExit(f"expected at most one analyzer named {sys.argv[2]!r}; found {len(matches)}")
if matches:
    analyzer_id = matches[0].get("id")
    if analyzer_id is None:
        raise SystemExit(f"analyzer named {sys.argv[2]!r} has no ID")
    print(analyzer_id)
PY
}

reconcile_profile_analyzer() {
  local name="$1"
  local profile_id="$2"
  local profile_revision="$3"
  local connection_values="${4:-{}}"

  local payload
  payload="$(
    python3 - "$name" "$profile_id" "$profile_revision" "$connection_values" <<'PY'
import json
import sys

name, profile_id, revision, connection_values = sys.argv[1:]
payload = {
    "name": name,
    "profileId": profile_id,
    "profileRevision": int(revision),
    "connectionValues": json.loads(connection_values),
}
print(json.dumps(payload, separators=(",", ":")))
PY
  )"

  local analyzer_id
  analyzer_id="$(find_analyzer_id "$name")"
  local method="POST"
  local url="$ANALYZER_API"
  local expected_status="201"
  local action="create"
  local action_label="Created"
  if [ -n "$analyzer_id" ]; then
    method="PUT"
    url="$ANALYZER_API/$analyzer_id"
    expected_status="200"
    action="update"
    action_label="Updated"
  fi

  local status
  status="$(curl -sk --connect-timeout 5 --max-time 45 -o "$RESPONSE_FILE" -w "%{http_code}" -X "$method" "$url" -u "$TEST_USER:$TEST_PASS" -H "Content-Type: application/json" -d "$payload")"
  if [ "$status" != "$expected_status" ]; then
    echo "ERROR: Failed to $action $name (HTTP $status)" >&2
    sed 's/^/  /' "$RESPONSE_FILE" >&2
    return 1
  fi
  echo "  $action_label: $name ($profile_id@$profile_revision)"
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

curl -sk --connect-timeout 3 --max-time 10 -X DELETE "$MOCK_URL/analyzers/genexpert" >/dev/null 2>&1 || true

echo "Creating GeneXpert mock transport..."
GENEXPERT_IP="$(create_mock_network "genexpert" "genexpert_astm" 9600)"
if [ -z "$GENEXPERT_IP" ]; then
  echo "ERROR: GeneXpert mock transport returned no IP address" >&2
  exit 1
fi
echo "  genexpert -> $GENEXPERT_IP:9600"

echo "Creating profile-pinned analyzer instances..."
reconcile_profile_analyzer "Cepheid GeneXpert (ASTM Mode)" "$GENEXPERT_PROFILE_ID" "$GENEXPERT_REVISION" '{"port":9600}'
reconcile_profile_analyzer "QuantStudio 5" "$QUANTSTUDIO_PROFILE_ID" "$QUANTSTUDIO_REVISION" '{"directory":"/data/analyzer-imports/quantstudio-5/incoming"}'
reconcile_profile_analyzer "QuantStudio 7" "$QUANTSTUDIO_PROFILE_ID" "$QUANTSTUDIO_REVISION" '{"directory":"/data/analyzer-imports/quantstudio-7/incoming"}'
reconcile_profile_analyzer "FluoroCycler XT" "$FLUOROCYCLER_PROFILE_ID" "$FLUOROCYCLER_REVISION" '{"directory":"/data/analyzer-imports/fluorocycler-xt/incoming"}'

verify_profile_pins
echo "Done. Four instances use the three validated M1 Bridge profile families."
