#!/usr/bin/env bash
# Prepare the assembled OGC-1054 result-review story.
#
# This is fixture setup, not a browser test. It completes the site mappings
# needed to run the two priority connections, creates one operational QC lot,
# and sends analyzer-native traffic through mock -> Bridge -> OpenELIS.

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
BRIDGE_ADMIN_URL="${BRIDGE_ADMIN_URL:-https://localhost:8442}"
BRIDGE_USER="${BRIDGE_USER:-admin}"
BRIDGE_PASS="${BRIDGE_PASS:-adminADMIN!}"
TEST_USER="${TEST_USER:-admin}"
TEST_PASS="${TEST_PASS:-adminADMIN!}"

OE_API="$BASE_URL/api/OpenELIS-Global/rest"
GENEXPERT_NAME="Cepheid GeneXpert (ASTM Mode)"
GENEXPERT_PROFILE="genexpert-astm"
FLUOROCYCLER_NAME="FluoroCycler XT"
FLUOROCYCLER_PROFILE="fluorocycler-xt"

KNOWN_ACCESSION="DEV01261000000000001"
UNKNOWN_TEST_ACCESSION="DEV01261000000000002"
UNKNOWN_VALUE_ACCESSION="DEV01261000000000003"
UNKNOWN_VALUE_TEST_CODE="MTB-RIF"
UNKNOWN_VALUE_RAW="REVIEW REQUIRED"
FILE_ACCESSION_ONE="DEV01263000000000001"
FILE_ACCESSION_TWO="DEV01263000000000002"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

fetch_json() {
  local url="$1"
  local output="$2"
  local label="$3"
  local status
  status="$(curl -sk --connect-timeout 5 --max-time 45 -o "$output" -w "%{http_code}" \
    -u "$TEST_USER:$TEST_PASS" "$url" || true)"
  if [ "$status" != "200" ]; then
    echo "ERROR: $label returned HTTP $status" >&2
    sed 's/^/  /' "$output" >&2
    return 1
  fi
}

send_json() {
  local method="$1"
  local url="$2"
  local payload_file="$3"
  local output="$4"
  local label="$5"
  local expected="${6:-200}"
  local status
  status="$(curl -sk --connect-timeout 5 --max-time 60 -o "$output" -w "%{http_code}" \
    -u "$TEST_USER:$TEST_PASS" -X "$method" -H "Content-Type: application/json" \
    --data-binary "@$payload_file" "$url" || true)"
  if [ "$status" != "$expected" ]; then
    echo "ERROR: $label returned HTTP $status (expected $expected)" >&2
    sed 's/^/  /' "$output" >&2
    return 1
  fi
}

send_mock_json() {
  local path="$1"
  local payload_file="$2"
  local output="$3"
  local label="$4"
  local status
  status="$(curl -sS --connect-timeout 5 --max-time 60 -o "$output" -w "%{http_code}" \
    -X POST -H "Content-Type: application/json" --data-binary "@$payload_file" \
    "$MOCK_URL$path" || true)"
  if [ "$status" != "200" ]; then
    echo "ERROR: $label returned HTTP $status" >&2
    sed 's/^/  /' "$output" >&2
    return 1
  fi
}

analyzer_field() {
  local analyzer_name="$1"
  local field="$2"
  local analyzers_file="$TMP_DIR/analyzers.json"
  fetch_json "$OE_API/analyzer/analyzers" "$analyzers_file" "Analyzer list"
  python3 - "$analyzers_file" "$analyzer_name" "$field" <<'PY'
import json
import sys

path, name, field = sys.argv[1:]
with open(path, encoding="utf-8") as handle:
    analyzers = json.load(handle).get("analyzers", [])
matches = [item for item in analyzers if item.get("name") == name]
if len(matches) != 1:
    raise SystemExit(f"expected exactly one analyzer named {name!r}; found {len(matches)}")
value = matches[0].get(field)
if value is None or str(value).strip() == "":
    raise SystemExit(f"analyzer {name!r} has no {field}")
print(value)
PY
}

prepare_profile_mapping() {
  local profile_id="$1"
  local profile_revision="$2"
  local held_test_code="${3:-}"
  local held_result_value="${4:-}"
  local mapping_file="$TMP_DIR/$profile_id-mapping.json"
  local catalog_file="$TMP_DIR/mapping-catalog.json"
  local selection_file="$TMP_DIR/$profile_id-catalog-selections.json"
  local update_file="$TMP_DIR/$profile_id-update.json"
  local confirm_file="$TMP_DIR/$profile_id-confirm.json"
  local response_file="$TMP_DIR/$profile_id-response.json"
  local mapping_url="$OE_API/analyzer-types/$profile_id/mapping?revision=$profile_revision"
  local mapping_action
  local test_id

  fetch_json "$mapping_url" "$mapping_file" "$profile_id mapping"
  fetch_json "$OE_API/analyzer-types/mapping-catalog/tests" "$catalog_file" \
    "OpenELIS mapping catalog"
  python3 - "$mapping_file" "$catalog_file" "$selection_file" <<'PY'
import json
import sys

mapping_path, catalog_path, destination = sys.argv[1:]
with open(mapping_path, encoding="utf-8") as handle:
    mapping = json.load(handle)
with open(catalog_path, encoding="utf-8") as handle:
    catalog = json.load(handle)

selections = {}
for test in mapping.get("tests", []):
    loinc = test.get("loinc")
    candidates = [
        item for item in catalog
        if loinc and loinc in item.get("loincCodes", [])
    ]
    candidates.sort(key=lambda item: int(item["id"]))
    selections[test["sourceRowKey"]] = candidates[0]["id"] if candidates else None

with open(destination, "w", encoding="utf-8") as handle:
    json.dump(selections, handle, separators=(",", ":"))
PY

  while IFS= read -r test_id; do
    fetch_json "$OE_API/analyzer-types/mapping-catalog/tests/$test_id/result-options" \
      "$TMP_DIR/result-options-$test_id.json" "OpenELIS result options for test $test_id"
  done < <(jq -r 'to_entries[].value // empty' "$selection_file" | sort -u)

  mapping_action="$(python3 - "$mapping_file" "$selection_file" "$TMP_DIR" "$update_file" \
    "$held_test_code" "$held_result_value" <<'PY'
import json
import os
import sys

mapping_path, selection_path, options_dir, destination, held_test_code, held_result_value = sys.argv[1:]
with open(mapping_path, encoding="utf-8") as handle:
    mapping = json.load(handle)
with open(selection_path, encoding="utf-8") as handle:
    selections = json.load(handle)

tests = []
results = []
changed = False
for test in mapping.get("tests", []):
    test_id = selections.get(test["sourceRowKey"])
    test_state = "BOUND" if test_id else "EXCLUDED"
    if test.get("mappingState") != test_state or str(test.get("testId") or "") != str(test_id or ""):
        changed = True
    tests.append({
        "sourceRowKey": test["sourceRowKey"],
        "mappingState": test_state,
        "testId": test_id,
    })

    options = []
    if test_id:
        with open(os.path.join(options_dir, f"result-options-{test_id}.json"), encoding="utf-8") as handle:
            options = json.load(handle)
    for result in test.get("results", []):
        if (
            test["sourceRowKey"] == held_test_code
            and result["rawValue"] == held_result_value
        ):
            changed = True
            continue
        matches = [
            option for option in options
            if option.get("label", "").strip().casefold() == result["rawValue"].strip().casefold()
        ]
        result_state = "BOUND" if len(matches) == 1 else "EXCLUDED"
        result_option_id = matches[0]["id"] if len(matches) == 1 else None
        if (
            result.get("mappingState") != result_state
            or str(result.get("resultOptionId") or "") != str(result_option_id or "")
        ):
            changed = True
        results.append({
            "sourceRowKey": test["sourceRowKey"],
            "rawValue": result["rawValue"],
            "mappingState": result_state,
            "testResultId": result_option_id,
        })

payload = {
    "baseBindingFingerprint": mapping.get("bindingFingerprint"),
    "tests": tests,
    "results": results,
}
with open(destination, "w", encoding="utf-8") as handle:
    json.dump(payload, handle, separators=(",", ":"))
print("update" if changed else "current")
PY
)"
  echo "$mapping_action"

  if [ "$mapping_action" = "update" ]; then
    send_json PUT "$mapping_url" "$update_file" "$response_file" "Complete $profile_id site mapping"
    fetch_json "$mapping_url" "$mapping_file" "$profile_id updated mapping"
  fi

  python3 - "$mapping_file" "$confirm_file" <<'PY'
import json
import sys

source, destination = sys.argv[1:]
with open(source, encoding="utf-8") as handle:
    mapping = json.load(handle)

confirmed = []
excluded = []
for test in mapping.get("tests", []):
    target = confirmed if test.get("mappingState") == "BOUND" else excluded
    target.append({"sourceRowKey": test["sourceRowKey"], "rawValue": None})
    for result in test.get("results", []):
        target = confirmed if result.get("mappingState") == "BOUND" else excluded
        target.append({"sourceRowKey": test["sourceRowKey"], "rawValue": result["rawValue"]})

payload = {
    "baseBindingFingerprint": mapping["bindingFingerprint"],
    "recognitionFingerprint": mapping["controlRecognition"]["recognitionFingerprint"],
    "confirmedRows": confirmed,
    "excludedRows": excluded,
}
with open(destination, "w", encoding="utf-8") as handle:
    json.dump(payload, handle, separators=(",", ":"))
PY

  if [ "$(python3 - "$mapping_file" <<'PY'
import json, sys
mapping = json.load(open(sys.argv[1], encoding="utf-8"))
print(mapping.get("confirmation", {}).get("state", "UNCONFIRMED"))
PY
)" != "CURRENT" ]; then
    send_json POST "$OE_API/analyzer-types/$profile_id/mapping/confirm?revision=$profile_revision" \
      "$confirm_file" "$response_file" "Confirm $profile_id site mapping"
    fetch_json "$mapping_url" "$mapping_file" "$profile_id confirmed mapping"
  fi

  cp "$mapping_file" "$TMP_DIR/current-$profile_id-mapping.json"
}

adopt_mapping_and_activate() {
  local analyzer_id="$1"
  local profile_id="$2"
  local mapping_file="$TMP_DIR/current-$profile_id-mapping.json"
  local selection_file="$TMP_DIR/$profile_id-selection.json"
  local response_file="$TMP_DIR/$profile_id-activation.json"

  python3 - "$mapping_file" "$selection_file" <<'PY'
import json
import sys

source, destination = sys.argv[1:]
with open(source, encoding="utf-8") as handle:
    mapping = json.load(handle)
payload = {
    "siteBindingId": mapping["siteBindingId"],
    "revision": mapping["siteBindingRevision"],
    "bindingFingerprint": mapping["bindingFingerprint"],
}
with open(destination, "w", encoding="utf-8") as handle:
    json.dump(payload, handle, separators=(",", ":"))
PY

  send_json PUT "$OE_API/analyzer/analyzers/$analyzer_id/site-binding" "$selection_file" \
    "$response_file" "Adopt $profile_id site mapping"
  printf '{}\n' > "$TMP_DIR/empty.json"
  send_json POST "$OE_API/analyzer/analyzers/$analyzer_id/activate" "$TMP_DIR/empty.json" \
    "$response_file" "Activate analyzer $analyzer_id"
}

ensure_qc_lot() {
  local analyzer_id="$1"
  local mapping_file="$TMP_DIR/current-$GENEXPERT_PROFILE-mapping.json"
  local lot_file="$TMP_DIR/control-lot.json"
  local response_file="$TMP_DIR/control-lot-response.json"
  local existing_file="$TMP_DIR/control-lot-existing.json"
  local status

  status="$(curl -sk --connect-timeout 5 --max-time 30 -o "$existing_file" -w "%{http_code}" \
    -u "$TEST_USER:$TEST_PASS" "$OE_API/qc/controlLot/byLotNumber/LOT-HIVVL-N" || true)"
  if [ "$status" = "200" ]; then
    python3 - "$existing_file" "$mapping_file" "$analyzer_id" <<'PY'
import json
import sys

existing_path, mapping_path, analyzer_id = sys.argv[1:]
with open(existing_path, encoding="utf-8") as handle:
    existing = json.load(handle)
with open(mapping_path, encoding="utf-8") as handle:
    mapping = json.load(handle)

matches = [test for test in mapping.get("tests", []) if test.get("rawCode") == "HIV-VL"]
if len(matches) != 1 or not matches[0].get("testId"):
    raise SystemExit("GeneXpert HIV-VL site mapping is not bound to one OpenELIS test")

expected = {
    "lotNumber": "LOT-HIVVL-N",
    "testId": str(matches[0]["testId"]),
    "instrumentId": str(analyzer_id),
    "calculationMethod": "MANUFACTURER_FIXED",
    "manufacturerMean": 1250.0,
    "manufacturerStdDev": 125.0,
    "status": "ACTIVE",
}
mismatches = [
    f"{field}: expected {value!r}, found {existing.get(field)!r}"
    for field, value in expected.items()
    if str(existing.get(field)) != str(value)
]
if mismatches:
    raise SystemExit(
        "Existing OGC-1054 QC lot conflicts with the assembled story:\n  "
        + "\n  ".join(mismatches)
    )

print("  Existing operational QC lot matches the assembled story")
PY
    return 0
  elif [ "$status" != "404" ]; then
    echo "ERROR: Control lot lookup returned HTTP $status" >&2
    sed 's/^/  /' "$existing_file" >&2
    return 1
  fi

  python3 - "$mapping_file" "$lot_file" "$analyzer_id" <<'PY'
import datetime
import json
import sys

mapping_path, destination, analyzer_id = sys.argv[1:]
with open(mapping_path, encoding="utf-8") as handle:
    mapping = json.load(handle)
matches = [test for test in mapping.get("tests", []) if test.get("rawCode") == "HIV-VL"]
if len(matches) != 1 or not matches[0].get("testId"):
    raise SystemExit("GeneXpert HIV-VL site mapping is not bound to one OpenELIS test")
now = datetime.datetime.now(datetime.timezone.utc)
payload = {
    "productName": "GeneXpert HIV-1 Viral Load Control",
    "lotNumber": "LOT-HIVVL-N",
    "manufacturer": "Cepheid",
    "controlLevel": "N",
    "testId": str(matches[0]["testId"]),
    "instrumentId": str(analyzer_id),
    "calculationMethod": "MANUFACTURER_FIXED",
    "manufacturerMean": 1250.0,
    "manufacturerStdDev": 125.0,
    "activationDate": now.isoformat().replace("+00:00", "Z"),
    "expirationDate": (now + datetime.timedelta(days=365)).isoformat().replace("+00:00", "Z"),
    "status": "ACTIVE",
    "unitOfMeasure": "copies/mL",
    "internalNotes": "OGC-1054 assembled story fixture",
    "externalNotes": "",
}
with open(destination, "w", encoding="utf-8") as handle:
    json.dump(payload, handle, separators=(",", ":"))
PY

  send_json POST "$OE_API/qc/controlLot" "$lot_file" "$response_file" "Create operational QC lot"
}

reset_bridge_file_state() {
  local analyzer_id="$1"
  local response_file="$TMP_DIR/bridge-file-reset.json"
  local status

  status="$(curl -sk --connect-timeout 5 --max-time 30 -o "$response_file" -w "%{http_code}" \
    -u "$BRIDGE_USER:$BRIDGE_PASS" -X POST \
    "$BRIDGE_ADMIN_URL/admin/reset?analyzerId=$analyzer_id" || true)"
  if [ "$status" != "200" ]; then
    echo "ERROR: Bridge FILE reset returned HTTP $status" >&2
    sed 's/^/  /' "$response_file" >&2
    return 1
  fi
  python3 - "$response_file" "$analyzer_id" <<'PY'
import json
import sys

path, analyzer_id = sys.argv[1:]
with open(path, encoding="utf-8") as handle:
    response = json.load(handle)
if response.get("reset") is not True or str(response.get("analyzerId")) != analyzer_id:
    raise SystemExit(f"Bridge did not confirm FILE reset for analyzer {analyzer_id}: {response}")
print(
    "  Reset Bridge FILE state: "
    f"{response.get('stateRowsRemoved', 0)} state rows, "
    f"{response.get('filesRemoved', 0)} files"
)
PY
}

push_astm() {
  local label="$1"
  local accession="$2"
  local test_code="$3"
  local value="$4"
  local payload_file="$TMP_DIR/$label-payload.json"
  local response_file="$TMP_DIR/$label-response.json"

  python3 - "$payload_file" "$accession" "$test_code" "$value" <<'PY'
import json
import sys

destination, accession, test_code, value = sys.argv[1:]
payload = {
    "destination": "tcp://openelis-analyzer-bridge:9600",
    "sample_id": accession,
    "results": [{"test_code": test_code, "value": value}],
}
with open(destination, "w", encoding="utf-8") as handle:
    json.dump(payload, handle, separators=(",", ":"))
PY
  send_mock_json "/simulate/astm/genexpert" "$payload_file" "$response_file" "$label ASTM traffic"
  python3 - "$response_file" <<'PY'
import json, sys
response = json.load(open(sys.argv[1], encoding="utf-8"))
if response.get("pushed") != 1:
    raise SystemExit(f"ASTM result was not pushed: {response}")
PY
}

push_story_traffic() {
  push_astm known-patient "$KNOWN_ACCESSION" MTB-RIF "NOT DETECTED"
  push_astm unknown-test "$UNKNOWN_TEST_ACCESSION" UNMAPPED-MTB "$UNKNOWN_VALUE_RAW"
  push_astm unknown-value "$UNKNOWN_VALUE_ACCESSION" "$UNKNOWN_VALUE_TEST_CODE" "$UNKNOWN_VALUE_RAW"

  cat > "$TMP_DIR/qc-payload.json" <<'JSON'
{"destination":"tcp://openelis-analyzer-bridge:9600","qc":true,"qc_deviation":0}
JSON
  send_mock_json "/simulate/astm/genexpert" "$TMP_DIR/qc-payload.json" "$TMP_DIR/qc-response.json" \
    "GeneXpert control traffic"
  python3 - "$TMP_DIR/qc-response.json" <<'PY'
import json, sys
response = json.load(open(sys.argv[1], encoding="utf-8"))
if response.get("pushed") != 1:
    raise SystemExit(f"ASTM control was not pushed: {response}")
PY

  cat > "$TMP_DIR/file-payload.json" <<'JSON'
{"target_dir":"/data/analyzer-imports/fluorocycler-xt/incoming"}
JSON
  send_mock_json "/simulate/file/hain_fluorocycler" "$TMP_DIR/file-payload.json" \
    "$TMP_DIR/file-response.json" "FluoroCycler FILE traffic"
  python3 - "$TMP_DIR/file-response.json" <<'PY'
import json, sys
response = json.load(open(sys.argv[1], encoding="utf-8"))
if not response.get("written_path") or not response.get("metadata", {}).get("results"):
    raise SystemExit(f"FILE fixture was not written with parsed metadata: {response}")
PY
}

wait_for_story_state() {
  local genexpert_id="$1"
  local fluorocycler_id="$2"
  local gene_file="$TMP_DIR/gene-results.json"
  local file_file="$TMP_DIR/file-results.json"
  local attempt

  for attempt in $(seq 1 60); do
    fetch_json "$OE_API/AnalyzerResults?id=$genexpert_id" "$gene_file" "GeneXpert result review"
    fetch_json "$OE_API/AnalyzerResults?id=$fluorocycler_id" "$file_file" "FluoroCycler result review"
    if python3 - "$gene_file" "$file_file" \
      "$KNOWN_ACCESSION" "$UNKNOWN_TEST_ACCESSION" "$UNKNOWN_VALUE_ACCESSION" \
      "$FILE_ACCESSION_ONE" "$FILE_ACCESSION_TWO" <<'PY'
import json
import sys

gene_path, file_path, known, unknown_test, unknown_value, file_one, file_two = sys.argv[1:]
gene = json.load(open(gene_path, encoding="utf-8")).get("resultList", [])
file_results = json.load(open(file_path, encoding="utf-8")).get("resultList", [])

checks = [
    any(row.get("accessionNumber") == known and not row.get("importIssueReason") for row in gene),
    any(row.get("accessionNumber") == unknown_test and row.get("importIssueReason") == "unknown_analyzer_test" for row in gene),
    any(row.get("accessionNumber") == unknown_value and row.get("importIssueReason") == "unknown_analyzer_result_value" for row in gene),
    any(row.get("isControl") and row.get("rawTestCode") == "HIV-VL" for row in gene),
    any(row.get("accessionNumber") == file_one for row in file_results),
    any(row.get("accessionNumber") == file_two for row in file_results),
]
raise SystemExit(0 if all(checks) else 1)
PY
    then
      echo "  Result traffic is ready for visible review"
      return 0
    fi
    sleep 2
  done

  echo "ERROR: Analyzer traffic did not reach the visible review state" >&2
  echo "GeneXpert response:" >&2
  sed 's/^/  /' "$gene_file" >&2
  echo "FluoroCycler response:" >&2
  sed 's/^/  /' "$file_file" >&2
  return 1
}

echo "Preparing current site mappings and active priority connections..."
GENEXPERT_ID="$(analyzer_field "$GENEXPERT_NAME" id)"
GENEXPERT_REVISION="$(analyzer_field "$GENEXPERT_NAME" profileRevision)"
FLUOROCYCLER_ID="$(analyzer_field "$FLUOROCYCLER_NAME" id)"
FLUOROCYCLER_REVISION="$(analyzer_field "$FLUOROCYCLER_NAME" profileRevision)"

prepare_profile_mapping "$GENEXPERT_PROFILE" "$GENEXPERT_REVISION" \
  "$UNKNOWN_VALUE_TEST_CODE" "$UNKNOWN_VALUE_RAW"
prepare_profile_mapping "$FLUOROCYCLER_PROFILE" "$FLUOROCYCLER_REVISION"
adopt_mapping_and_activate "$GENEXPERT_ID" "$GENEXPERT_PROFILE"
adopt_mapping_and_activate "$FLUOROCYCLER_ID" "$FLUOROCYCLER_PROFILE"
ensure_qc_lot "$GENEXPERT_ID"
reset_bridge_file_state "$FLUOROCYCLER_ID"

echo "Sending real priority analyzer traffic through Bridge..."
push_story_traffic
wait_for_story_state "$GENEXPERT_ID" "$FLUOROCYCLER_ID"

echo "Done. The OGC-1054 MVP result-review story is ready in the visible UI."
