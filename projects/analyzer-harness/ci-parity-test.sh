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
#   projects/analyzer-harness/ci-parity-test.sh --test-file playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts
#   projects/analyzer-harness/ci-parity-test.sh --shard 2/2
#   projects/analyzer-harness/ci-parity-test.sh --artifact-dir /tmp/oe-ci-parity
#   projects/analyzer-harness/ci-parity-test.sh --build

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/frontend"
source "$SCRIPT_DIR/compose-stack.sh"
source "$SCRIPT_DIR/playwright-project-policy.sh"
CI_COMPOSE_FILES=($(compose_args_ci))
CI_PARITY_OVERLAY="$SCRIPT_DIR/docker-compose.ci-parity-isolated.yml"
CI_COMPOSE_FILES+=(-f "$CI_PARITY_OVERLAY")
FIXTURE_SCRIPT="$REPO_ROOT/src/test/resources/load-test-fixtures.sh"
SEED_SCRIPT="$REPO_ROOT/projects/analyzer-harness/seed-analyzers.sh"
MVP_TRAFFIC_SCRIPT="$REPO_ROOT/projects/analyzer-harness/seed-mvp-traffic.sh"
FIXTURE_DB_TARGET_TEST="$REPO_ROOT/projects/analyzer-harness/scripts/test-fixture-loader-db-target.sh"
REUSABLE_WORKFLOW="$REPO_ROOT/.github/workflows/e2e-playwright-reusable.yml"

PRECHECK_ONLY=false
SEED_ONLY=false
BUILD_SOURCE=false
KEEP_STACK=false
SHARD=""
ARTIFACT_DIR=""
TEST_USER_INPUT="${TEST_USER:-}"
TEST_PASS_INPUT="${TEST_PASS:-}"
MODE="parity"
PLAYWRIGHT_PROJECT=""
PLAYWRIGHT_TEST_FILE=""
PLAYWRIGHT_SLOWMO_INPUT="${PLAYWRIGHT_SLOWMO:-0}"

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
    --build)
      BUILD_SOURCE=true
      shift
      ;;
    --keep-stack)
      KEEP_STACK=true
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
        echo "ERROR: --project requires a harness Playwright project" >&2
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

read -r parity_digest parity_octet < <(python3 - "$REPO_ROOT" <<'PY'
import hashlib
import sys

digest = hashlib.sha256(sys.argv[1].encode()).hexdigest()
print(digest[:8], 64 + int(digest[:4], 16) % 160)
PY
)
parity_slug="$(printf '%s' "$(basename "$REPO_ROOT")" | tr '[:upper:]' '[:lower:]' | tr -c 'a-z0-9-' '-')"
export CI_PARITY_IMAGE_PREFIX="oe2-ci-${parity_slug}-${parity_digest}"
export CI_PARITY_COMPOSE_PROJECT="${CI_PARITY_IMAGE_PREFIX}-$(date -u +%Y%m%d%H%M%S)-$$"
export CI_PARITY_ANALYZER_SUBNET_PREFIX="10.${parity_octet}"
export COMPOSE_PROJECT_NAME="$CI_PARITY_COMPOSE_PROJECT"

if [[ -z "$ARTIFACT_DIR" ]]; then
  ARTIFACT_DIR="/tmp/oe-ci-parity-$(date +%Y%m%d_%H%M%S)"
fi
mkdir -p "$ARTIFACT_DIR"
PRECHECK_LOG="$ARTIFACT_DIR/preflight.log"
RUN_LOG="$ARTIFACT_DIR/run.log"

pass() { echo "PASS: $*" | tee -a "$PRECHECK_LOG"; }
fail() { echo "FAIL: $*" | tee -a "$PRECHECK_LOG"; PRECHECK_FAILED=true; }
note() { echo "INFO: $*" | tee -a "$PRECHECK_LOG"; }

container_id() {
  docker compose "${CI_COMPOSE_FILES[@]}" ps -a -q "$1"
}

published_port() {
  local published
  for attempt in 1 2 3 4 5 6 7 8 9 10; do
    published="$(docker compose "${CI_COMPOSE_FILES[@]}" port "$1" "$2" 2>/dev/null || true)"
    if [[ "$published" =~ :([0-9]+)$ ]]; then
      printf '%s\n' "${BASH_REMATCH[1]}"
      return 0
    fi
    sleep 1
  done
  echo "ERROR: no published port for $1:$2 in $CI_PARITY_COMPOSE_PROJECT" >&2
  return 1
}

PRECHECK_FAILED=false

cleanup() {
  local exit_code=$?
  trap - EXIT
  if [[ "$KEEP_STACK" == false && "$PRECHECK_ONLY" == false ]]; then
    if [[ "$exit_code" -ne 0 ]]; then
      collect_failure_artifacts || true
    fi
    docker compose "${CI_COMPOSE_FILES[@]}" down --volumes > "$ARTIFACT_DIR/cleanup.log" 2>&1 || true
  fi
  return "$exit_code"
}
trap cleanup EXIT

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
    note "build this checkout's parity images with: ./projects/analyzer-harness/ci-parity-test.sh --build"
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

  for service in oe.openelis.org openelis-analyzer-bridge astm-simulator; do
    docker logs "$(container_id "$service")" > "$ARTIFACT_DIR/docker-logs/${service}.log" 2>&1 || true
  done

  local webapp_container
  webapp_container="$(container_id oe.openelis.org)"
  docker cp "$webapp_container":/var/lib/openelis-global/logs/. "$ARTIFACT_DIR/oe-logs/" 2>/dev/null || true
  docker cp "$webapp_container":/usr/local/tomcat/logs/. "$ARTIFACT_DIR/tomcat-logs/" 2>/dev/null || true

  if [[ -d "$FRONTEND_DIR/test-results" ]]; then
    mkdir -p "$ARTIFACT_DIR/playwright"
    cp -R "$FRONTEND_DIR/test-results" "$ARTIFACT_DIR/playwright/" 2>/dev/null || true
  fi
  if [[ -d "$FRONTEND_DIR/blob-report" ]]; then
    mkdir -p "$ARTIFACT_DIR/playwright"
    cp -R "$FRONTEND_DIR/blob-report" "$ARTIFACT_DIR/playwright/" 2>/dev/null || true
  fi
}

verify_analyzer_connections() {
  local connections_file="$1"
  if [[ ! -s "$connections_file" ]]; then
    echo "ERROR: analyzer connection capture failed or empty" | tee -a "$RUN_LOG"
    return 1
  fi

  local missing
  missing="$(
    python3 - "$connections_file" <<'PY'
import json, sys
try:
    response = json.load(open(sys.argv[1]))
except Exception:
    print("__PARSE_ERROR__")
    raise SystemExit(0)

entries = response.get("analyzers", []) if isinstance(response, dict) else []

names = {
    item.get("name", "")
    for item in entries
    if item.get("connected") is True and item.get("bridgeConnectionId")
}
required = {
    "Cepheid GeneXpert (ASTM Mode)",
    "QuantStudio 5",
    "QuantStudio 7",
    "FluoroCycler XT",
}
missing = sorted(required - names)
print("\n".join(missing))
PY
  )"

  if [[ "$missing" == "__PARSE_ERROR__" ]]; then
    echo "ERROR: analyzer connection payload is not valid JSON" | tee -a "$RUN_LOG"
    return 1
  fi

  if [[ -n "$missing" ]]; then
    echo "ERROR: required analyzers are missing durable Bridge connection references:" | tee -a "$RUN_LOG"
    echo "$missing" | tee -a "$RUN_LOG"
    return 1
  fi

  echo "Analyzer connection reference gate passed." | tee -a "$RUN_LOG"
  return 0
}

{
  echo "=== CI Parity Preflight ==="
  echo "Repo root: $REPO_ROOT"
  echo "Compose project: $CI_PARITY_COMPOSE_PROJECT"
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
check_file "$CI_PARITY_OVERLAY"
check_file "$FIXTURE_SCRIPT"
check_file "$SEED_SCRIPT"
check_file "$MVP_TRAFFIC_SCRIPT"
check_file "$FIXTURE_DB_TARGET_TEST"
check_file "$REUSABLE_WORKFLOW"
check_file "$FRONTEND_DIR/package-lock.json"
if [[ "$BUILD_SOURCE" == true ]]; then
  require_command mvn
fi

if bash "$FIXTURE_DB_TARGET_TEST"; then
  pass "fixture loader honors an explicit database container"
else
  fail "fixture loader ignored an explicit database container"
fi

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

if [[ "$SEED_ONLY" == false ]]; then
  if [[ -d "$FRONTEND_DIR/node_modules" ]] && [[ -x "$FRONTEND_DIR/node_modules/.bin/playwright" ]]; then
    pass "frontend dependencies installed (node_modules/.bin/playwright present)"
  else
    fail "frontend dependencies missing (run: cd frontend && npm ci)"
  fi

  check_playwright_chromium_installed
fi
if [[ "$BUILD_SOURCE" == true && "$PRECHECK_ONLY" == true ]]; then
  fail "--build cannot be combined with --preflight-only"
fi
if [[ "$BUILD_SOURCE" == true && "$PRECHECK_FAILED" == false ]]; then
  note "Building the current checkout WAR and worktree-scoped CI images"
  git -C "$REPO_ROOT" rev-parse HEAD > "$ARTIFACT_DIR/source-head.txt"
  (
    cd "$REPO_ROOT"
    mvn -q clean install -DskipTests -Dmaven.test.skip=true
    docker compose "${CI_COMPOSE_FILES[@]}" build
  ) 2>&1 | tee "$ARTIFACT_DIR/build.log"
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

PLAYWRIGHT_PROJECT="$(resolve_harness_playwright_project "$MODE" "$PLAYWRIGHT_PROJECT")"

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

WEBAPP_CONTAINER="$(container_id oe.openelis.org)"
BRIDGE_CONTAINER="$(container_id openelis-analyzer-bridge)"
DB_CONTAINER="$(container_id db.openelis.org)"
BASE_URL="https://localhost:$(published_port proxy 443)"
BRIDGE_URL="https://localhost:$(published_port openelis-analyzer-bridge 8443)"
MOCK_URL="http://localhost:$(published_port astm-simulator 8080)"
echo "Isolated parity endpoints: OpenELIS=$BASE_URL Bridge=$BRIDGE_URL Mock=$MOCK_URL" | tee -a "$RUN_LOG"

with_timeout_wait 60 "webapp cert material" "docker exec $WEBAPP_CONTAINER sh -c 'test -s /etc/openelis-global/keystore && test -s /etc/openelis-global/truststore'" 2>&1 | tee -a "$RUN_LOG"
with_timeout_wait 60 "bridge cert material" "docker exec $BRIDGE_CONTAINER sh -c 'test -s /etc/openelis-global/keystore && test -s /etc/openelis-global/truststore'" 2>&1 | tee -a "$RUN_LOG"

(
  cd "$REPO_ROOT"
  export TEST_USER="$TEST_USER_RESOLVED"
  export TEST_PASS="$TEST_PASS_RESOLVED"
  export TIMEOUT_SECONDS=240
  export BASE_URL
  bash scripts/e2e/wait-for-openelis-login.sh
) 2>&1 | tee -a "$RUN_LOG"
with_timeout_wait 120 "bridge readiness" "curl -k -s -f --connect-timeout 2 --max-time 3 $BRIDGE_URL/actuator/health > /dev/null" 2>&1 | tee -a "$RUN_LOG"
with_timeout_wait 120 "simulator readiness" "curl -s -f --connect-timeout 2 --max-time 3 $MOCK_URL/health > /dev/null" 2>&1 | tee -a "$RUN_LOG"

(
  cd "$REPO_ROOT"
  DB_CONTAINER="$DB_CONTAINER" ./src/test/resources/load-test-fixtures.sh --profile=harness --no-verify
) 2>&1 | tee -a "$RUN_LOG"

(
  cd "$REPO_ROOT"
  BASE_URL="$BASE_URL" \
  MOCK_URL="$MOCK_URL" \
  BRIDGE_ADMIN_URL="$BRIDGE_URL" \
  TEST_USER="$TEST_USER_RESOLVED" \
  TEST_PASS="$TEST_PASS_RESOLVED" \
  DB_CONTAINER="$DB_CONTAINER" \
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

analyzer_connections="$ARTIFACT_DIR/analyzer-connections.json"
curl -k -s -u "$TEST_USER_RESOLVED:$TEST_PASS_RESOLVED" \
  "$BASE_URL/api/OpenELIS-Global/rest/analyzer/analyzers" > "$analyzer_connections" || true
if ! verify_analyzer_connections "$analyzer_connections"; then
  collect_failure_artifacts
  exit 6
fi
echo "Analyzer connection references captured at $analyzer_connections" | tee -a "$RUN_LOG"

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
  BASE_URL="$BASE_URL" \
  TEST_USER="$TEST_USER_RESOLVED" \
  TEST_PASS="$TEST_PASS_RESOLVED" \
  PLAYWRIGHT_VIDEO="$([[ "$PLAYWRIGHT_PROJECT" == "harness-demo-video" ]] && echo "on" || echo "off")" \
  PLAYWRIGHT_SLOWMO="$PLAYWRIGHT_SLOWMO_INPUT" \
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
