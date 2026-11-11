#!/usr/bin/env bash
# Sourced lifecycle shared by the core Playwright and Cypress executors.
# Use the workflow's stack plus the existing local identity/port overlay.
start_e2e_stack() {
  local suite="$1"
  REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
  cd "$REPO_ROOT"
  export E2E_STACK_PROJECT="$OE_CI_IMAGE_PREFIX-core"
  COMPOSE=(docker compose -p "$E2E_STACK_PROJECT" -f "$REPO_ROOT/build.docker-compose.yml")
  if [[ "$suite" == core ]]; then
    COMPOSE+=(-f "$REPO_ROOT/.github/ci/ci.memory-limits.yml")
    export OE_UAT_SCENARIOS_ENABLED=true
  else
    export OE_UAT_SCENARIOS_ENABLED=false
  fi
  COMPOSE+=(-f "$REPO_ROOT/build.docker-compose.worktree.yml")
  printf '%s\n' "$E2E_STACK_PROJECT" > "$OE_CI_ARTIFACT_DIR/core-compose-project.txt"
  trap cleanup_e2e_stack EXIT
  [[ -f .env ]] || cp .env.example .env
  "${COMPOSE[@]}" down -v --remove-orphans
  if [[ ! -f "$OE_CI_ARTIFACT_DIR/core-images-built" ]]; then
    "${COMPOSE[@]}" build
    touch "$OE_CI_ARTIFACT_DIR/core-images-built"
  fi
  "${COMPOSE[@]}" up -d --no-build --wait --wait-timeout 600
  export DB_CONTAINER="$("${COMPOSE[@]}" ps -q db.openelis.org)"
  local port
  port="$("${COMPOSE[@]}" port proxy 443)"
  export BASE_URL="https://localhost:${port##*:}"
  export CYPRESS_BASE_URL="$BASE_URL"
  export TEST_USER="${TEST_USER:-admin}" TEST_PASS="${TEST_PASS:-adminADMIN!}"
  export ANALYZER_INGRESS_USER="$TEST_USER" ANALYZER_INGRESS_PASS="$TEST_PASS"
  TIMEOUT_SECONDS=240 bash scripts/e2e/wait-for-openelis-login.sh
  if [[ "$suite" == core ]]; then
    ./src/test/resources/load-test-fixtures.sh --profile=core --no-verify
    "${COMPOSE[@]}" exec -T oe.openelis.org mkdir -p \
      /data/analyzer-imports/e2e-qs5/incoming \
      /data/analyzer-imports/e2e-qs7/incoming \
      /data/analyzer-imports/e2e-fluorocycler/incoming
  else
    "${COMPOSE[@]}" exec -T db.openelis.org psql -U clinlims -d clinlims \
      --set=ON_ERROR_STOP=on < src/test/resources/e2e-foundational-data.sql
  fi
  cd frontend
  export CI=true LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8
}

cleanup_e2e_stack() {
  local result=$?
  trap - EXIT
  if [[ "$result" != 0 ]]; then
    "${COMPOSE[@]}" logs --no-color > "$OE_CI_JOB_ARTIFACT_DIR/containers.log" 2>&1 || true
  fi
  if ! "${COMPOSE[@]}" down -v --remove-orphans; then
    if [[ "$result" == 0 ]]; then
      printf 'cleanup\n' > "$OE_CI_PHASE_FILE"
      result=1
    fi
  fi
  exit "$result"
}
