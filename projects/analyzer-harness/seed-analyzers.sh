#!/usr/bin/env bash
# Seed the M1 priority analyzer instances through the OpenELIS REST API.
#
# Profile content, revisions, and defaults come from the Bridge catalog. This
# script owns only harness instance names and explicit connection values.

set -euo pipefail

# --no-mock-network: the mock sends to each connection's own Bridge listener
#   port, so a stack without the Docker socket skips per-analyzer networks.
# --networks-only: create the mock transports and no analyzer, for a stack whose
#   analyzers are all set up by the stories that run on it (the evidence recording).
# A newly created analyzer is set up as an operator would in the setup screens:
# its shipped defaults confirmed, applied and activated. An analyzer whose
# defaults leave a row of an assay that is on unresolved stays in Setup; an
# assay that starts off (no local test carries its code, as for the BC-5380's
# manual differential) is not mapped, as in Verify. This seed runs on dev, demo
# and testing stacks only, never at a lab.
ENSURE_CONNECTIONS=false
MOCK_NETWORK=true
NETWORKS_ONLY=false
for arg in "$@"; do
  case "$arg" in
    --ensure-connections) ENSURE_CONNECTIONS=true ;;
    --no-mock-network) MOCK_NETWORK=false ;;
    --networks-only) NETWORKS_ONLY=true ;;
    *) echo "Usage: $0 [--ensure-connections] [--no-mock-network] [--networks-only]" >&2; exit 2 ;;
  esac
done

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
LAB_UNITS_API="$BASE_URL/api/OpenELIS-Global/rest/test-catalog/lab-units"
HARNESS_LAB_UNIT_NAME="${HARNESS_LAB_UNIT_NAME:-Molecular Biology}"

TEST_USER="${TEST_USER:-admin}"
TEST_PASS="${TEST_PASS:-adminADMIN!}"

CURL_TLS_FLAG=--no-insecure
if [ "${DEV_STACK_TLS:-self-signed}" = "self-signed" ]; then
  CURL_TLS_FLAG=--insecure
fi

GENEXPERT_PROFILE_ID="cepheid-genexpert-astm"
FLUOROCYCLER_PROFILE_ID="hain-fluorocycler-xt-madagascar"
QUANTSTUDIO_PROFILE_ID="thermo-quantstudio"
BC5380_PROFILE_ID="mindray-bc5380"
BC5380_LAB_UNIT_NAME="${BC5380_LAB_UNIT_NAME:-Hematology}"

CATALOG_FILE="$(mktemp)"
ANALYZERS_FILE="$(mktemp)"
LAB_UNITS_FILE="$(mktemp)"
RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$CATALOG_FILE" "$ANALYZERS_FILE" "$LAB_UNITS_FILE" "$RESPONSE_FILE"' EXIT

fetch_json() {
  local url="$1"
  local output="$2"
  local label="$3"
  local attempt
  local status
  for attempt in 1 2 3 4 5; do
    status="$(curl -sS "$CURL_TLS_FLAG" --connect-timeout 5 --max-time 30 -o "$output" -w "%{http_code}" -u "$TEST_USER:$TEST_PASS" "$url" || true)"
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

resolve_lab_unit_id() {
  python3 - "$LAB_UNITS_FILE" "$1" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    lab_units = json.load(handle)

matches = [unit for unit in lab_units if unit.get("name") == sys.argv[2]]
if len(matches) != 1:
    raise SystemExit(f"expected one active lab unit named {sys.argv[2]!r}; found {len(matches)}")
lab_unit_id = matches[0].get("id")
if lab_unit_id is None or str(lab_unit_id).strip() == "":
    raise SystemExit(f"lab unit {sys.argv[2]!r} has no ID")
print(lab_unit_id)
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
  local connection_values="{}"
  [ "$#" -ge 4 ] && connection_values="$4"
  local lab_unit_id="${5:-$LAB_UNIT_ID}"

  local payload
  payload="$(
    python3 - "$name" "$profile_id" "$profile_revision" "$lab_unit_id" "$connection_values" <<'PY'
import json
import sys

name, profile_id, revision, lab_unit_id, connection_values = sys.argv[1:]
payload = {
    "name": name,
    "profileId": profile_id,
    "profileRevision": int(revision),
    "testUnitIds": [lab_unit_id],
    "connectionValues": json.loads(connection_values),
}
print(json.dumps(payload, separators=(",", ":")))
PY
  )"

  local analyzer_id
  analyzer_id="$(find_analyzer_id "$name")"
  if [ "$ENSURE_CONNECTIONS" = true ] && [ -n "$analyzer_id" ]; then
    echo "  Preserved existing connection: $name"
    return 0
  fi
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
  status="$(curl -sS "$CURL_TLS_FLAG" --connect-timeout 5 --max-time 45 -o "$RESPONSE_FILE" -w "%{http_code}" -X "$method" "$url" -u "$TEST_USER:$TEST_PASS" -H "Content-Type: application/json" -d "$payload")"
  if [ "$status" != "$expected_status" ]; then
    echo "ERROR: Failed to $action $name (HTTP $status)" >&2
    sed 's/^/  /' "$RESPONSE_FILE" >&2
    return 1
  fi
  echo "  $action_label: $name ($profile_id@$profile_revision)"
  if [ "$action" = "create" ]; then
    set_up_like_an_operator "$(find_analyzer_id "$name")" "$name"
  fi
}

api_send() {
  local method="$1"
  local url="$2"
  local body="$3"
  curl -sS "$CURL_TLS_FLAG" --connect-timeout 5 --max-time 45 -o "$RESPONSE_FILE" -w "%{http_code}" -X "$method" "$url" \
    -u "$TEST_USER:$TEST_PASS" -H "Content-Type: application/json" --data-binary "$body"
}

# Confirm every resolved row of the analyzer's own mapping, apply it and activate, as Verify,
# Apply and "Finish and activate" do. An unresolved row of an assay that is on leaves the
# analyzer in Setup; assays that are off are not mapped.
set_up_like_an_operator() {
  local analyzer_id="$1"
  local name="$2"
  local request
  local status
  local undecided
  fetch_json "$ANALYZER_API/$analyzer_id/mapping" "$RESPONSE_FILE" "$name mapping"
  undecided="$(python3 - "$RESPONSE_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    mapping = json.load(handle)
codes = set()
off = {t["sourceRowKey"] for t in mapping.get("tests", []) if not t.get("subIdentity") and t.get("enabled") is False}
for test in mapping.get("tests", []):
    if test["sourceRowKey"] in off:
        continue
    states = [test.get("mappingState")] + [r.get("mappingState") for r in test.get("results", [])]
    if any(state not in ("BOUND", "EXCLUDED") for state in states):
        codes.add(test["sourceRowKey"])
print(" ".join(sorted(codes)))
PY
)"
  if [ -n "$undecided" ]; then
    echo "  Left in Setup: $name has unresolved mapping rows to review in OpenELIS ($undecided)"
    return 0
  fi
  request="$(python3 - "$RESPONSE_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    mapping = json.load(handle)
confirmed, excluded = [], []
for test in mapping.get("tests", []):
    sub = test.get("subIdentity") or ""
    rows = [(test.get("mappingState"), None)] + [(r.get("mappingState"), r.get("rawValue")) for r in test.get("results", [])]
    for state, raw in rows:
        row = {"sourceRowKey": test["sourceRowKey"], "subIdentity": sub, "rawValue": raw}
        if state == "BOUND":
            confirmed.append(row)
        elif state == "EXCLUDED":
            excluded.append(row)
print(json.dumps({
    "baseMappingFingerprint": mapping["mappingFingerprint"],
    "recognitionFingerprint": mapping["controlRecognition"]["recognitionFingerprint"],
    "confirmedRows": confirmed,
    "excludedRows": excluded,
}))
PY
)"
  status="$(api_send POST "$ANALYZER_API/$analyzer_id/mapping/confirm" "$request")"
  [ "$status" = "200" ] || { echo "ERROR: Could not confirm $name's mapping (HTTP $status)" >&2; sed 's/^/  /' "$RESPONSE_FILE" >&2; return 1; }
  fetch_json "$ANALYZER_API/$analyzer_id/mapping" "$RESPONSE_FILE" "$name mapping"
  request="$(python3 -c 'import json,sys; m=json.load(open(sys.argv[1])); print(json.dumps({"mappingId": m["mappingId"], "revision": m["mappingRevision"], "mappingFingerprint": m["mappingFingerprint"]}))' "$RESPONSE_FILE")"
  status="$(api_send PUT "$ANALYZER_API/$analyzer_id/mapping/apply" "$request")"
  [ "$status" = "200" ] || { echo "ERROR: Could not apply $name's mapping (HTTP $status)" >&2; sed 's/^/  /' "$RESPONSE_FILE" >&2; return 1; }
  status="$(api_send POST "$ANALYZER_API/$analyzer_id/activate" '{}')"
  [ "$status" = "200" ] || { echo "ERROR: Could not activate $name (HTTP $status)" >&2; sed 's/^/  /' "$RESPONSE_FILE" >&2; return 1; }
  echo "  Confirmed, applied and activated: $name"
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
  python3 - "$ANALYZERS_FILE" "$GENEXPERT_PROFILE_ID" "$GENEXPERT_REVISION" "$QUANTSTUDIO_PROFILE_ID" \
    "$QUANTSTUDIO_REVISION" "$FLUOROCYCLER_PROFILE_ID" "$FLUOROCYCLER_REVISION" "$BC5380_PROFILE_ID" "$BC5380_REVISION" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    analyzers = json.load(handle).get("analyzers", [])

genexpert = (sys.argv[2], int(sys.argv[3]))
quantstudio = (sys.argv[4], int(sys.argv[5]))
fluorocycler = (sys.argv[6], int(sys.argv[7]))
bc5380 = (sys.argv[8], int(sys.argv[9]))
expected = {
    "Cepheid GeneXpert (ASTM Mode)": genexpert,
    "QuantStudio 5": quantstudio,
    "QuantStudio 7": quantstudio,
    "FluoroCycler XT": fluorocycler,
    "Mindray BC-5380": bc5380,
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
BC5380_REVISION="$(resolve_active_revision "$BC5380_PROFILE_ID")"
echo "  $GENEXPERT_PROFILE_ID@$GENEXPERT_REVISION"
echo "  $FLUOROCYCLER_PROFILE_ID@$FLUOROCYCLER_REVISION"
echo "  $QUANTSTUDIO_PROFILE_ID@$QUANTSTUDIO_REVISION"
echo "  $BC5380_PROFILE_ID@$BC5380_REVISION"

echo "Resolving active lab units from $LAB_UNITS_API..."
fetch_json "$LAB_UNITS_API" "$LAB_UNITS_FILE" "Active lab units"
LAB_UNIT_ID="$(resolve_lab_unit_id "$HARNESS_LAB_UNIT_NAME")"
BC5380_LAB_UNIT_ID="$(resolve_lab_unit_id "$BC5380_LAB_UNIT_NAME")"
echo "  $HARNESS_LAB_UNIT_NAME: lab unit $LAB_UNIT_ID"
echo "  $BC5380_LAB_UNIT_NAME: lab unit $BC5380_LAB_UNIT_ID"

if [ "$MOCK_NETWORK" = true ]; then
  if [ "$ENSURE_CONNECTIONS" = false ]; then
    curl -sk --connect-timeout 3 --max-time 10 -X DELETE "$MOCK_URL/analyzers/genexpert" >/dev/null 2>&1 || true
  fi

  echo "Creating GeneXpert mock transport..."
  GENEXPERT_IP="$(create_mock_network "genexpert" "genexpert_astm" 9600)"
  if [ -z "$GENEXPERT_IP" ]; then
    echo "ERROR: GeneXpert mock transport returned no IP address" >&2
    exit 1
  fi
  echo "  genexpert -> $GENEXPERT_IP:9600"
fi

if [ "$NETWORKS_ONLY" = true ]; then
  echo "Done. Mock transports only; no analyzer instances."
  exit 0
fi

echo "Creating profile-pinned analyzer instances..."
reconcile_profile_analyzer "Cepheid GeneXpert (ASTM Mode)" "$GENEXPERT_PROFILE_ID" "$GENEXPERT_REVISION" '{"senderId":"OE2-TEST-GENEXPERT"}'
reconcile_profile_analyzer "QuantStudio 5" "$QUANTSTUDIO_PROFILE_ID" "$QUANTSTUDIO_REVISION" '{"directory":"/data/analyzer-imports/quantstudio-5/incoming"}'
reconcile_profile_analyzer "QuantStudio 7" "$QUANTSTUDIO_PROFILE_ID" "$QUANTSTUDIO_REVISION" '{"directory":"/data/analyzer-imports/quantstudio-7/incoming"}'
reconcile_profile_analyzer "FluoroCycler XT" "$FLUOROCYCLER_PROFILE_ID" "$FLUOROCYCLER_REVISION" '{"directory":"/data/analyzer-imports/fluorocycler-xt/incoming"}'
reconcile_profile_analyzer "Mindray BC-5380" "$BC5380_PROFILE_ID" "$BC5380_REVISION" '{"senderId":"OE2-TEST-BC5380"}' "$BC5380_LAB_UNIT_ID"

if [ "$ENSURE_CONNECTIONS" = true ]; then
  echo "Done. Missing harness connections created; existing configuration and review data preserved."
  exit 0
fi

verify_profile_pins
echo "Done. Five instances use the four pinned Bridge profile families; mappings and traffic remain untouched."
