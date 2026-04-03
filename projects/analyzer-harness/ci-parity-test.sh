#!/usr/bin/env bash
# ci-parity-test.sh
#
# Local analyzer-harness CI parity runner.
# Mirrors the analyzer-harness reusable workflow sequence and captures evidence.
#
# By default this script is strict:
# - It verifies prerequisite state in preflight.
# - It does not auto-install dependencies or build images.
# - It fails fast with actionable diagnostics.
#
# Usage:
#   projects/analyzer-harness/ci-parity-test.sh
#   projects/analyzer-harness/ci-parity-test.sh --preflight-only
#   projects/analyzer-harness/ci-parity-test.sh --seed-only
#   projects/analyzer-harness/ci-parity-test.sh --mode video
#   projects/analyzer-harness/ci-parity-test.sh --project harness-demo-video
#   projects/analyzer-harness/ci-parity-test.sh --test-file playwright/tests/demo/harness/analyzer-demo-flow.spec.ts
#   projects/analyzer-harness/ci-parity-test.sh --shard 2/2
#   projects/analyzer-harness/ci-parity-test.sh --artifact-dir /tmp/oe-ci-parity

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/frontend"
source "$SCRIPT_DIR/compose-stack.sh"
CI_COMPOSE_FILES=($(compose_args_ci))
FIXTURE_SCRIPT="$REPO_ROOT/src/test/resources/load-test-fixtures.sh"
SEED_SCRIPT="$REPO_ROOT/projects/analyzer-harness/seed-analyzers.sh"
REUSABLE_WORKFLOW="$REPO_ROOT/.github/workflows/e2e-playwright-analyzer-harness-reusable.yml"

PRECHECK_ONLY=false
SEED_ONLY=false
SHARD=""
ARTIFACT_DIR=""
TEST_USER_INPUT="${TEST_USER:-}"
TEST_PASS_INPUT="${TEST_PASS:-}"
MODE="parity"
PLAYWRIGHT_PROJECT=""
PLAYWRIGHT_TEST_FILE=""
PLAYWRIGHT_SLOWMO_INPUT="${PLAYWRIGHT_SLOWMO:-500}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --preflight-only)
      PRECHECK_ONLY=true
      shift
      ;;
    --seed-only)
      SEED_ONLY=true
      shift
      ;;
    --shard)
      SHARD="${2:-}"
      if [[ -z "$SHARD" ]]; then
        echo "ERROR: --shard requires value like 1/2 or 2/2" >&2
        exit 2
      fi
      shift 2
      ;;
    --mode)
      MODE="${2:-}"
      if [[ -z "$MODE" ]]; then
        echo "ERROR: --mode requires value parity|video" >&2
        exit 2
      fi
      if [[ "$MODE" != "parity" && "$MODE" != "video" ]]; then
        echo "ERROR: unsupported --mode '$MODE' (expected parity|video)" >&2
        exit 2
      fi
      shift 2
      ;;
    --project)
      PLAYWRIGHT_PROJECT="${2:-}"
      if [[ -z "$PLAYWRIGHT_PROJECT" ]]; then
        echo "ERROR: --project requires value harness-demo|harness-demo-video" >&2
        exit 2
      fi
      shift 2
      ;;
    --test-file)
      PLAYWRIGHT_TEST_FILE="${2:-}"
      if [[ -z "$PLAYWRIGHT_TEST_FILE" ]]; then
        echo "ERROR: --test-file requires a Playwright spec path" >&2
        exit 2
      fi
      shift 2
      ;;
    --slowmo)
      PLAYWRIGHT_SLOWMO_INPUT="${2:-}"
      if [[ -z "$PLAYWRIGHT_SLOWMO_INPUT" ]]; then
        echo "ERROR: --slowmo requires milliseconds value" >&2
        exit 2
      fi
      shift 2
      ;;
    --artifact-dir)
      ARTIFACT_DIR="${2:-}"
      if [[ -z "$ARTIFACT_DIR" ]]; then
        echo "ERROR: --artifact-dir requires a directory path" >&2
        exit 2
      fi
      shift 2
      ;;
    --help|-h)
      sed -n '1,40p' "$0"
      exit 0
      ;;
    *)
      echo "ERROR: Unknown option: $1" >&2
      exit 2
      ;;
  esac
done

if [[ -z "$ARTIFACT_DIR" ]]; then
  ARTIFACT_DIR="/tmp/oe-ci-parity-$(date +%Y%m%d_%H%M%S)"
fi
mkdir -p "$ARTIFACT_DIR"
PRECHECK_LOG="$ARTIFACT_DIR/preflight.log"
RUN_LOG="$ARTIFACT_DIR/run.log"

pass() { echo "PASS: $*" | tee -a "$PRECHECK_LOG"; }
fail() { echo "FAIL: $*" | tee -a "$PRECHECK_LOG"; PRECHECK_FAILED=true; }
note() { echo "INFO: $*" | tee -a "$PRECHECK_LOG"; }

PRECHECK_FAILED=false

require_command() {
  local cmd="$1"
  if command -v "$cmd" >/dev/null 2>&1; then
    pass "command '$cmd' is available"
  else
    fail "command '$cmd' is missing"
  fi
}

check_file() {
  local path="$1"
  if [[ -f "$path" ]]; then
    pass "file exists: $path"
  else
    fail "missing file: $path"
  fi
}

resolve_test_pass() {
  if [[ -n "$TEST_PASS_INPUT" ]]; then
    TEST_PASS_RESOLVED="$TEST_PASS_INPUT"
    return 0
  fi

  if [[ -f "$REPO_ROOT/.env" ]]; then
    TEST_PASS_RESOLVED="$(python3 - <<'PY'
import os
from pathlib import Path
env = Path(os.environ["REPO_ROOT"]) / ".env"
for line in env.read_text().splitlines():
    line = line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    k, v = line.split("=", 1)
    if k.strip() == "TEST_PASS":
        print(v.strip().strip('"').strip("'"))
        break
PY
)"
  else
    TEST_PASS_RESOLVED=""
  fi
}

require_images_for_compose() {
  local missing=0
  local images_file="$ARTIFACT_DIR/required-images.txt"

  docker compose "${CI_COMPOSE_FILES[@]}" config --images \
    | awk 'NF' \
    | sort -u > "$images_file"

  if [[ ! -s "$images_file" ]]; then
    fail "could not resolve required compose images from config --images"
    return
  fi

  while IFS= read -r image; do
    if docker image inspect "$image" >/dev/null 2>&1; then
      pass "image present: $image"
    else
      fail "required image missing for --no-build: $image"
      missing=1
    fi
  done < "$images_file"

  if [[ "$missing" -eq 1 ]]; then
    note "build missing images first via the shared harness build flow, e.g.: ./projects/analyzer-harness/build.sh --skip-war"
  fi
}

check_playwright_chromium_installed() {
  local linux_cache="${HOME}/.cache/ms-playwright"
  local mac_cache="${HOME}/Library/Caches/ms-playwright"

  if [[ -d "$linux_cache" ]] && ls "$linux_cache"/chromium-* >/dev/null 2>&1; then
    pass "playwright chromium cache is present ($linux_cache)"
    return
  fi

  if [[ -d "$mac_cache" ]] && ls "$mac_cache"/chromium-* >/dev/null 2>&1; then
    pass "playwright chromium cache is present ($mac_cache)"
    return
  fi

  fail "playwright chromium browser is missing (run: cd frontend && npx playwright install chromium --with-deps)"
}

with_timeout_wait() {
  local seconds="$1"
  local description="$2"
  local cmd="$3"

  if command -v timeout >/dev/null 2>&1; then
    timeout "$seconds" bash -c "$cmd"
    return $?
  fi

  local start now
  start="$(date +%s)"
  while true; do
    if bash -c "$cmd"; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start >= seconds )); then
      echo "Timed out waiting for: $description" >&2
      return 124
    fi
    sleep 2
  done
}

collect_failure_artifacts() {
  mkdir -p "$ARTIFACT_DIR/docker-logs"
  mkdir -p "$ARTIFACT_DIR/oe-logs"
  mkdir -p "$ARTIFACT_DIR/tomcat-logs"

  docker compose "${CI_COMPOSE_FILES[@]}" ps \
    > "$ARTIFACT_DIR/docker-logs/compose-ps.txt" 2>&1 || true

  for service in openelisglobal-webapp openelis-analyzer-bridge openelis-astm-simulator; do
    docker logs "$service" > "$ARTIFACT_DIR/docker-logs/${service}.log" 2>&1 || true
  done

  docker cp openelisglobal-webapp:/var/lib/openelis-global/logs/. "$ARTIFACT_DIR/oe-logs/" 2>/dev/null || true
  docker cp openelisglobal-webapp:/usr/local/tomcat/logs/. "$ARTIFACT_DIR/tomcat-logs/" 2>/dev/null || true

  if [[ -d "$FRONTEND_DIR/test-results" ]]; then
    mkdir -p "$ARTIFACT_DIR/playwright"
    cp -R "$FRONTEND_DIR/test-results" "$ARTIFACT_DIR/playwright/" 2>/dev/null || true
  fi
  if [[ -d "$FRONTEND_DIR/blob-report" ]]; then
    mkdir -p "$ARTIFACT_DIR/playwright"
    cp -R "$FRONTEND_DIR/blob-report" "$ARTIFACT_DIR/playwright/" 2>/dev/null || true
  fi
}

required_analyzers=(
  "Cepheid GeneXpert (ASTM Mode)"
  "QuantStudio 5"
  "QuantStudio 7"
  "FluoroCycler XT"
  "Mindray BC-5380"
  "Mindray BS-200"
  "Mindray BS-300"
)

mapping_count_for_analyzer() {
  local analyzer_name="$1"
  docker exec -i openelisglobal-database psql -U clinlims -d clinlims -t -A \
    -c "SELECT COUNT(*) FROM clinlims.analyzer_test_map m JOIN clinlims.analyzer a ON a.id = m.analyzer_id WHERE a.name = '${analyzer_name}';" \
    2>/dev/null | tr -d '[:space:]' || echo 0
}

verify_required_mappings() {
  local max_attempts=12
  local sleep_seconds=5
  local attempt=1
  local missing=0
  local mappings=0

  while (( attempt <= max_attempts )); do
    missing=0
    echo "Verifying analyzer test mappings (attempt ${attempt}/${max_attempts})..." | tee -a "$RUN_LOG"
    for analyzer in "${required_analyzers[@]}"; do
      mappings="$(mapping_count_for_analyzer "$analyzer")"
      mappings="${mappings:-0}"
      echo "$analyzer: $mappings test mappings" | tee -a "$RUN_LOG"
      if [[ "$mappings" == "0" ]]; then
        missing=1
      fi
    done
    if [[ "$missing" -eq 0 ]]; then
      echo "Analyzer mapping gate passed." | tee -a "$RUN_LOG"
      return 0
    fi
    if (( attempt < max_attempts )); then
      echo "Mappings still missing; waiting ${sleep_seconds}s before retry..." | tee -a "$RUN_LOG"
      sleep "$sleep_seconds"
    fi
    attempt=$((attempt + 1))
  done

  echo "ERROR: Analyzer mapping gate failed. At least one required analyzer has 0 test mappings." | tee -a "$RUN_LOG"
  return 1
}

verify_bridge_registry() {
  local registry_file="$1"
  if [[ ! -s "$registry_file" ]]; then
    echo "ERROR: bridge registry capture failed or empty" | tee -a "$RUN_LOG"
    return 1
  fi

  local missing
  missing="$(
    python3 - "$registry_file" <<'PY'
import json, sys
try:
    registry = json.load(open(sys.argv[1]))
except Exception:
    print("__PARSE_ERROR__")
    raise SystemExit(0)

if isinstance(registry, dict):
    entries = [value for value in registry.values() if isinstance(value, dict)]
elif isinstance(registry, list):
    entries = [value for value in registry if isinstance(value, dict)]
else:
    entries = []

names = {item.get("name", "") for item in entries}
required = {
    "Cepheid GeneXpert (ASTM Mode)",
    "QuantStudio 5",
    "QuantStudio 7",
    "FluoroCycler XT",
    "Mindray BC-5380",
    "Mindray BS-200",
    "Mindray BS-300",
}
missing = sorted(required - names)
print("\n".join(missing))
PY
  )"

  if [[ "$missing" == "__PARSE_ERROR__" ]]; then
    echo "ERROR: bridge registry payload is not valid JSON" | tee -a "$RUN_LOG"
    return 1
  fi

  if [[ -n "$missing" ]]; then
    echo "ERROR: bridge registry missing required analyzers:" | tee -a "$RUN_LOG"
    echo "$missing" | tee -a "$RUN_LOG"
    return 1
  fi

  echo "Bridge registry gate passed." | tee -a "$RUN_LOG"
  return 0
}

{
  echo "=== CI Parity Preflight ==="
  echo "Repo root: $REPO_ROOT"
  echo "Artifacts: $ARTIFACT_DIR"
  echo
} | tee "$PRECHECK_LOG"

export REPO_ROOT

require_command docker
require_command curl
require_command python3
require_command npm

check_file "$HARNESS_BASE_COMPOSE"
check_file "$CI_BUILD_COMPOSE"
check_file "$CI_HARNESS_COMPOSE"
check_file "$FIXTURE_SCRIPT"
check_file "$SEED_SCRIPT"
check_file "$REUSABLE_WORKFLOW"
check_file "$FRONTEND_DIR/package-lock.json"

if [[ -f "$REPO_ROOT/.env" ]]; then
  pass ".env exists at repo root"
elif [[ -f "$REPO_ROOT/.env.example" ]]; then
  cp "$REPO_ROOT/.env.example" "$REPO_ROOT/.env"
  pass ".env missing but materialized from .env.example (matches CI prepare step)"
else
  fail ".env and .env.example are both missing"
fi

if [[ -n "$TEST_USER_INPUT" ]]; then
  TEST_USER_RESOLVED="$TEST_USER_INPUT"
else
  TEST_USER_RESOLVED="admin"
fi

resolve_test_pass
if [[ -n "${TEST_PASS_RESOLVED:-}" ]]; then
  pass "TEST_PASS resolved from env or .env"
else
  fail "TEST_PASS not set (export TEST_PASS or add TEST_PASS in .env)"
fi

if compgen -G "$REPO_ROOT/volume/plugins/*.jar" >/dev/null; then
  pass "plugin jars present in volume/plugins"
else
  fail "no plugin jars found in volume/plugins (stage plugin jars before parity run)"
fi

if [[ "$SEED_ONLY" == false ]]; then
  if [[ -d "$FRONTEND_DIR/node_modules" ]] && [[ -x "$FRONTEND_DIR/node_modules/.bin/playwright" ]]; then
    pass "frontend dependencies installed (node_modules/.bin/playwright present)"
  else
    fail "frontend dependencies missing (run: cd frontend && npm ci)"
  fi

  check_playwright_chromium_installed
fi
require_images_for_compose

if [[ "$PRECHECK_FAILED" == true ]]; then
  echo "Preflight failed. See $PRECHECK_LOG" >&2
  exit 2
fi

if [[ "$PRECHECK_ONLY" == true ]]; then
  echo "Preflight passed. Exiting due to --preflight-only."
  exit 0
fi

if [[ -z "$PLAYWRIGHT_PROJECT" ]]; then
  if [[ "$MODE" == "video" ]]; then
    PLAYWRIGHT_PROJECT="harness-demo-video"
  else
    PLAYWRIGHT_PROJECT="harness-demo"
  fi
fi

if [[ "$PLAYWRIGHT_PROJECT" != "harness-demo" && "$PLAYWRIGHT_PROJECT" != "harness-demo-video" ]]; then
  echo "ERROR: unsupported project '$PLAYWRIGHT_PROJECT' (expected harness-demo or harness-demo-video)" >&2
  exit 2
fi

if [[ "$PLAYWRIGHT_PROJECT" == "harness-demo-video" && -n "$SHARD" ]]; then
  echo "ERROR: sharding is unsupported in harness-demo-video mode" >&2
  exit 2
fi

{
  echo "=== CI Parity Run ==="
  date
  echo "Artifacts: $ARTIFACT_DIR"
  echo
} | tee "$RUN_LOG"

mkdir -p "$REPO_ROOT/projects/analyzer-harness/volume/analyzer-imports"
for slug in \
  quantstudio-5 \
  quantstudio-7 \
  fluorocycler-xt \
  demo--quantstudio-5 \
  demo--quantstudio-7 \
  demo--fluorocycler-xt; do
  mkdir -p "$REPO_ROOT/projects/analyzer-harness/volume/analyzer-imports/$slug/incoming"
done
chmod -R a+rwX "$REPO_ROOT/projects/analyzer-harness/volume/analyzer-imports" || true

(
  cd "$REPO_ROOT"
  docker compose "${CI_COMPOSE_FILES[@]}" up -d --no-build
) 2>&1 | tee -a "$RUN_LOG"

with_timeout_wait 60 "webapp cert material" "docker exec openelisglobal-webapp sh -c 'test -s /etc/openelis-global/keystore && test -s /etc/openelis-global/truststore'" 2>&1 | tee -a "$RUN_LOG"
with_timeout_wait 60 "bridge cert material" "docker exec openelis-analyzer-bridge sh -c 'test -s /etc/openelis-global/keystore && test -s /etc/openelis-global/truststore'" 2>&1 | tee -a "$RUN_LOG"

(
  cd "$REPO_ROOT"
  export TEST_USER="$TEST_USER_RESOLVED"
  export TEST_PASS="$TEST_PASS_RESOLVED"
  export TIMEOUT_SECONDS=240
  bash scripts/e2e/wait-for-openelis-login.sh
) 2>&1 | tee -a "$RUN_LOG"
with_timeout_wait 120 "bridge readiness" "curl -k -s -f --connect-timeout 2 --max-time 3 https://localhost:8442/actuator/health > /dev/null" 2>&1 | tee -a "$RUN_LOG"
with_timeout_wait 120 "simulator readiness" "curl -s -f --connect-timeout 2 --max-time 3 http://localhost:8085/health > /dev/null" 2>&1 | tee -a "$RUN_LOG"

(
  cd "$REPO_ROOT"
  ./src/test/resources/load-test-fixtures.sh --analyzers=full --no-verify
) 2>&1 | tee -a "$RUN_LOG"

(
  cd "$REPO_ROOT"
  BASE_URL="https://localhost" \
  TEST_USER="$TEST_USER_RESOLVED" \
  TEST_PASS="$TEST_PASS_RESOLVED" \
  DB_CONTAINER="openelisglobal-database" \
  bash projects/analyzer-harness/seed-analyzers.sh
) 2>&1 | tee -a "$RUN_LOG"

if rg -n "WARN: Mock API failed|fallback|using stable IP|using fallback" "$RUN_LOG" >/dev/null 2>&1; then
  echo "ERROR: seed step emitted fallback warnings; refusing to proceed." | tee -a "$RUN_LOG"
  collect_failure_artifacts
  exit 3
fi

if [[ "$SEED_ONLY" == true ]]; then
  echo "CI parity seed-only run succeeded. Artifacts: $ARTIFACT_DIR" | tee -a "$RUN_LOG"
  exit 0
fi

for dir in \
  quantstudio-5 \
  quantstudio-7 \
  fluorocycler-xt \
  demo--quantstudio-5 \
  demo--quantstudio-7 \
  demo--fluorocycler-xt; do
  with_timeout_wait 120 "incoming dir $dir" "[ -d \"$REPO_ROOT/projects/analyzer-harness/volume/analyzer-imports/$dir/incoming\" ]" 2>&1 | tee -a "$RUN_LOG"
done
chmod -R a+rwX "$REPO_ROOT/projects/analyzer-harness/volume/analyzer-imports" || true

if ! verify_required_mappings; then
  collect_failure_artifacts
  exit 5
fi

bridge_registry="$ARTIFACT_DIR/bridge-registry.json"
curl -k -s "https://localhost:8442/api/analyzers" > "$bridge_registry" || true
if ! verify_bridge_registry "$bridge_registry"; then
  collect_failure_artifacts
  exit 6
fi
echo "Bridge registry captured at $bridge_registry" | tee -a "$RUN_LOG"

PLAYWRIGHT_CMD=(npm run pw:test -- --project="$PLAYWRIGHT_PROJECT" --workers=1)
if [[ -n "$SHARD" ]]; then
  PLAYWRIGHT_CMD+=(--shard="$SHARD")
fi
if [[ -n "$PLAYWRIGHT_TEST_FILE" ]]; then
  PLAYWRIGHT_CMD+=("$PLAYWRIGHT_TEST_FILE")
fi

set +e
(
  cd "$FRONTEND_DIR"
  CI=true \
  ANALYZER_HARNESS=true \
  BASE_URL=https://localhost \
  TEST_USER="$TEST_USER_RESOLVED" \
  TEST_PASS="$TEST_PASS_RESOLVED" \
  PLAYWRIGHT_VIDEO="$([[ "$PLAYWRIGHT_PROJECT" == "harness-demo-video" ]] && echo "on" || echo "off")" \
  PLAYWRIGHT_SLOWMO="$PLAYWRIGHT_SLOWMO_INPUT" \
  FILE_IMPORT_POLL_MS=5000 \
  FILE_IMPORT_DROP_BUFFER_MS=45000 \
  "${PLAYWRIGHT_CMD[@]}"
) 2>&1 | tee -a "$RUN_LOG"
pw_exit=$?
set -e

if [[ "$pw_exit" -ne 0 ]]; then
  echo "Playwright failed with exit code $pw_exit" | tee -a "$RUN_LOG"
  collect_failure_artifacts
  exit "$pw_exit"
fi

echo "CI parity run succeeded. Artifacts: $ARTIFACT_DIR" | tee -a "$RUN_LOG"
