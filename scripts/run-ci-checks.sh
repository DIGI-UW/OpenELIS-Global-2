#!/usr/bin/env bash
# One public entrypoint for isolated execution of the repository's CI code checks.
# Internal executors mirror backend, frontend, and the individual browser lanes.
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DIR=""
BASE_REF=origin/develop
PLAN_ONLY=false
LIST_ONLY=false
KEEP_CHECKOUTS=false
SELECTED_JOBS=()
JOBS=(backend-format backend-tests deployment-contract agent-assets frontend-static frontend-image i18n e2e-scope
      playwright-core-1 playwright-core-2 playwright-core-3 playwright-core-4
      playwright-analyzers-1 playwright-analyzers-2 cypress-core cypress-independent)
while [[ $# -gt 0 ]]; do
  case "$1" in
    --job) SELECTED_JOBS+=("${2:?--job needs a name from --list-jobs}"); shift 2 ;;
    --list-jobs) LIST_ONLY=true; shift ;;
    --plan) PLAN_ONLY=true; shift ;;
    --artifact-dir) ARTIFACT_DIR="${2:?--artifact-dir needs a path}"; shift 2 ;;
    --base) BASE_REF="${2:?--base needs a Git ref}"; shift 2 ;;
    --keep-checkouts) KEEP_CHECKOUTS=true; shift ;;
    --help|-h)
      cat <<'HELP'
Usage: scripts/run-ci-checks.sh [options]
Default: all local CI code checks for committed HEAD, with isolated state.
  --list-jobs            List job names; no setup or testing
  --job NAME             Select a job (repeatable); includes required setup
  --plan                 Show selected jobs without running them
  --artifact-dir PATH    Save source identity, job logs and reports
  --base REF             Base for locale source checks (origin/develop)
  --keep-checkouts       Retain owned checkouts for inspection
Filtered results are partial. Publishing and GitHub administration are excluded.
HELP
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done
job_lane() {
  case "$1" in
    backend-*|deployment-contract|agent-assets) echo backend ;;
    frontend-*|i18n) echo frontend ;;
    *) echo e2e ;;
  esac
}
if [[ "$LIST_ONLY" == true ]]; then
  printf '%s\n' "${JOBS[@]}"
  exit 0
fi
MODE=full
if [[ ${#SELECTED_JOBS[@]} != 0 ]]; then
  MODE=partial
  for selected in "${SELECTED_JOBS[@]}"; do
    found=false
    for job in "${JOBS[@]}"; do [[ "$job" != "$selected" ]] || found=true; done
    [[ "$found" == true ]] || { echo "Unknown job: $selected (see --list-jobs)" >&2; exit 2; }
  done
  requested=()
  for job in "${JOBS[@]}"; do
    for selected in "${SELECTED_JOBS[@]}"; do
      if [[ "$job" == "$selected" ]]; then requested+=("$job"); break; fi
    done
  done
  JOBS=("${requested[@]}")
fi
HEAD_SHA="$(git -C "$REPO_ROOT" rev-parse HEAD)"
# Only locale checks compare against the base; other selected jobs need no base ref.
if [[ " ${JOBS[*]} " == *' i18n '* ]]; then
  if ! OE_CI_BASE_SHA="$(git -C "$REPO_ROOT" rev-parse --verify "$BASE_REF^{commit}" 2>/dev/null)"; then
    echo "Invalid base ref: $BASE_REF. Fetch it or pass --base with an available commit." >&2
    exit 2
  fi
  export OE_CI_BASE_SHA
fi
export OE_CI_SOURCE_BRANCH="$(git -C "$REPO_ROOT" branch --show-current)"
LANES=()
for lane in backend frontend e2e; do
  for job in "${JOBS[@]}"; do
    if [[ "$(job_lane "$job")" == "$lane" ]]; then LANES+=("$lane"); break; fi
  done
 done
if [[ "$PLAN_ONLY" == true ]]; then
  printf 'Source: %s\nMode: %s\n' "$HEAD_SHA" "$MODE"
  printf 'Jobs: %s\n' "${JOBS[*]}"
  printf 'Isolated checkouts: %s\n' "${LANES[*]}"
  echo 'Browser jobs include source image builds, dependencies, readiness and fresh fixtures.'
  exit 0
fi
if ! git -C "$REPO_ROOT" diff --quiet || ! git -C "$REPO_ROOT" diff --cached --quiet; then
  echo 'Commit tracked changes first: local CI executes committed source.' >&2
  exit 2
fi
for command in git python3; do
  command -v "$command" >/dev/null || { echo "Missing prerequisite: $command" >&2; exit 2; }
done
NEEDS_DOCKER=false
NEEDS_JAVA=false
NEEDS_NODE20=false
NEEDS_BROWSER=false
for job in "${JOBS[@]}"; do
  case "$job" in
    backend-tests|frontend-image|playwright-*|cypress-*) NEEDS_DOCKER=true ;;
  esac
  case "$job" in backend-format|backend-tests) NEEDS_JAVA=true ;; esac
  case "$job" in frontend-static|e2e-scope|playwright-*|cypress-*) NEEDS_NODE20=true ;; esac
  case "$job" in playwright-*|cypress-*) NEEDS_BROWSER=true ;; esac
done
if [[ "$NEEDS_DOCKER" == true ]]; then
  command -v docker >/dev/null
  docker info >/dev/null
  # Java's Docker API client must follow the CLI's selected context too.
  if [[ -n "${DOCKER_CONTEXT:-}" ]]; then
    export DOCKER_HOST="$(docker context inspect "$DOCKER_CONTEXT" --format '{{.Endpoints.docker.Host}}')"
  elif [[ -z "${DOCKER_HOST:-}" ]]; then
    export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"
  fi
fi
if [[ "$NEEDS_JAVA" == true ]]; then
  command -v mvn >/dev/null
  "$REPO_ROOT/scripts/run-java21" >/dev/null
fi
# Match CI's native Node versions without requiring Node for Python-only jobs.
if [[ "$NEEDS_NODE20" == true ]]; then
  if command -v fnm >/dev/null; then
    export PATH="$(dirname "$(fnm exec --using=20 which node)"):$PATH"
  else
    [[ "$(node -p 'process.versions.node.split(".")[0]')" == 20 ]] || {
      echo 'Use Node 20 or fnm with Node 20.' >&2; exit 2;
    }
  fi
fi
if [[ " ${JOBS[*]} " == *' deployment-contract '* ]]; then
  if command -v fnm >/dev/null; then
    export OE_CI_NODE22_BIN="$(fnm exec --using=22 which node)"
  else
    export OE_CI_NODE22_BIN="${OE_CI_NODE22_BIN:?Set OE_CI_NODE22_BIN to Node 22 for deployment-contract}"
  fi
  [[ "$("$OE_CI_NODE22_BIN" -p 'process.versions.node.split(".")[0]')" == 22 ]] || {
    echo 'deployment-contract requires Node 22.' >&2; exit 2;
  }
fi
ARTIFACT_DIR="${ARTIFACT_DIR:-${TMPDIR:-/tmp}/oe-local-ci-${HEAD_SHA:0:10}-$(date -u +%Y%m%d%H%M%S)-$$}"
mkdir -p "$ARTIFACT_DIR"
ARTIFACT_DIR="$(cd "$ARTIFACT_DIR" && pwd)"
if [[ -e "$ARTIFACT_DIR/source-head.txt" ]]; then
  echo 'Use a new artifact directory; prior results must not mix with this run.' >&2
  exit 2
fi
export OE_CI_ARTIFACT_DIR="$ARTIFACT_DIR"
export OE_CI_IMAGE_PREFIX="oe2-ci-${HEAD_SHA:0:10}-$(date -u +%Y%m%d%H%M%S)-$$"
printf '%s\n' "$HEAD_SHA" > "$ARTIFACT_DIR/source-head.txt"
printf '%s\n' "$MODE" > "$ARTIFACT_DIR/mode.txt"
printf '%s\n' "${JOBS[@]}" > "$ARTIFACT_DIR/requested-jobs.txt"
printf 'Local CI (%s): %s\nLogs: %s\n' "$MODE" "$HEAD_SHA" "$ARTIFACT_DIR"

cleanup_owned_stacks() {
  local root="$ARTIFACT_DIR/checkouts/e2e" project failed=0
  [[ -d "$root" ]] || return 0
  if [[ -f "$ARTIFACT_DIR/core-compose-project.txt" ]]; then
    project="$(cat "$ARTIFACT_DIR/core-compose-project.txt")"
    E2E_STACK_PROJECT="$project" docker compose -p "$project" \
      -f "$root/build.docker-compose.yml" -f "$root/build.docker-compose.worktree.yml" \
      down -v --remove-orphans >> "$ARTIFACT_DIR/cleanup.log" 2>&1 || failed=1
  fi
  for project_file in "$ARTIFACT_DIR"/playwright-analyzers-*-artifacts/compose-project.txt; do
    [[ -f "$project_file" ]] || continue
    project="$(cat "$project_file")"
    CI_PARITY_COMPOSE_PROJECT="$project" CI_PARITY_IMAGE_PREFIX=ci-cleanup \
      CI_PARITY_ANALYZER_SUBNET_PREFIX=10.64 docker compose -p "$project" \
      -f "$root/build.docker-compose.yml" \
      -f "$root/projects/analyzer-harness/docker-compose.base.yml" \
      -f "$root/.github/ci/ci.analyzer-harness.yml" \
      -f "$root/projects/analyzer-harness/docker-compose.ci-parity-isolated.yml" \
      down -v --remove-orphans >> "$ARTIFACT_DIR/cleanup.log" 2>&1 || failed=1
  done
  return "$failed"
}

cleanup() {
  local result=$?
  trap - EXIT
  if [[ " ${LANES[*]} " == *' e2e '* ]]; then
    cleanup_owned_stacks || result=1
  fi
  if [[ "$KEEP_CHECKOUTS" == false ]]; then
    for lane in "${LANES[@]}"; do
      local root="$ARTIFACT_DIR/checkouts/$lane"
      [[ -d "$root" ]] || continue
      for reports in "$root"/target/surefire-reports "$root"/target/failsafe-reports \
          "$root"/dataexport/*/target/surefire-reports; do
        [[ -d "$reports" ]] || continue
        mkdir -p "$ARTIFACT_DIR/$lane-reports"
        cp -R "$reports" "$ARTIFACT_DIR/$lane-reports/$(basename "$(dirname "$reports")")-$(basename "$reports")" || result=1
      done
      git -C "$REPO_ROOT" worktree remove --force --force "$root" >> "$ARTIFACT_DIR/cleanup.log" 2>&1 || result=1
    done
  fi
  printf '%s\n' "$result" > "$ARTIFACT_DIR/result.exit"
  exit "$result"
}
trap cleanup EXIT
mkdir -p "$ARTIFACT_DIR/checkouts"
for lane in "${LANES[@]}"; do
  root="$ARTIFACT_DIR/checkouts/$lane"
  git -C "$REPO_ROOT" worktree add --detach "$root" "$HEAD_SHA" >> "$ARTIFACT_DIR/setup.log" 2>&1
  if [[ "$lane" != frontend ]]; then
    git -C "$root" submodule update --init --recursive >> "$ARTIFACT_DIR/setup.log" 2>&1
  fi
 done

run_job() {
  local job="$1"; shift
  export OE_CI_PHASE_FILE="$ARTIFACT_DIR/$job.phase"
  export OE_CI_JOB_ARTIFACT_DIR="$ARTIFACT_DIR/$job-artifacts"
  mkdir -p "$OE_CI_JOB_ARTIFACT_DIR"
  printf 'setup\n' > "$OE_CI_PHASE_FILE"
  local result=0 status=PASS
  "$@" > "$ARTIFACT_DIR/$job.log" 2>&1 || result=$?
  if [[ "$result" != 0 ]]; then
    status=ERROR
    [[ "$(cat "$OE_CI_PHASE_FILE")" != code ]] || status=FAIL
  fi
  printf '%s\n' "$status" > "$ARTIFACT_DIR/$job.status"
  printf '%s\n' "$result" > "$ARTIFACT_DIR/$job.exit"
  return "$result"
}
run_lane() {
  local lane="$1" failed=0
  cd "$ARTIFACT_DIR/checkouts/$lane"
  if [[ "$lane" == backend && " ${JOBS[*]} " == *' deployment-contract '* ]]; then
    python3 -m venv "$ARTIFACT_DIR/backend-python"
    "$ARTIFACT_DIR/backend-python/bin/python" -m pip install --disable-pip-version-check PyYAML==6.0.2 > "$ARTIFACT_DIR/backend-python-dependencies.log" 2>&1
    export PATH="$ARTIFACT_DIR/backend-python/bin:$PATH"
  fi
  if [[ "$lane" == e2e && "$NEEDS_BROWSER" == true ]]; then
    # The same browser dependencies and Chromium installation as the CI executor.
    python3 -m venv "$ARTIFACT_DIR/python"
    "$ARTIFACT_DIR/python/bin/python" -m pip install --disable-pip-version-check PyYAML==6.0.2 > "$ARTIFACT_DIR/python-dependencies.log" 2>&1
    export PATH="$ARTIFACT_DIR/python/bin:$PATH"
    (cd frontend && npm ci --legacy-peer-deps && npx playwright install --only-shell chromium) > "$ARTIFACT_DIR/browser-dependencies.log" 2>&1
  fi
  for job in "${JOBS[@]}"; do
    [[ "$(job_lane "$job")" == "$lane" ]] || continue
    case "$job" in
      backend-*|deployment-contract|agent-assets) run_job "$job" bash scripts/ci/backend.sh "$job" || failed=1 ;;
      frontend-*|i18n) run_job "$job" bash scripts/ci/frontend.sh "$job" || failed=1 ;;
      e2e-scope) run_job "$job" bash -ec 'printf "code\n" > "$OE_CI_PHASE_FILE"; node --test .github/scripts/e2e-scope.test.cjs' || failed=1 ;;
      playwright-core-*) run_job "$job" bash scripts/ci/e2e-core.sh "${job##*-}" || failed=1 ;;
      playwright-analyzers-*)
        args=()
        [[ -f "$ARTIFACT_DIR/analyzer-images-built" ]] || args+=(--build)
        run_job "$job" bash scripts/ci/e2e-analyzers.sh "${args[@]}" \
          --project harness-foundational --project harness-demo --shard "${job##*-}/2" \
          --artifact-dir "$ARTIFACT_DIR/$job-artifacts" || failed=1
        # Only successful builds reach the test phase; don't reuse incomplete images.
        [[ "$(cat "$ARTIFACT_DIR/$job.phase")" != code ]] || touch "$ARTIFACT_DIR/analyzer-images-built"
        ;;
      cypress-*) run_job "$job" bash scripts/ci/e2e-cypress.sh "${job#cypress-}" || failed=1 ;;
    esac
    if [[ "$lane" == e2e ]]; then
      for report in test-results blob-report playwright-report cypress/screenshots; do
        if [[ -d "frontend/$report" ]]; then
          mkdir -p "$ARTIFACT_DIR/$job-artifacts/$(dirname "$report")"
          cp -R "frontend/$report" "$ARTIFACT_DIR/$job-artifacts/$report" || failed=1
          rm -rf "frontend/$report"
        fi
      done
    fi
  done
  return "$failed"
}
pids=()
interrupt() {
  trap - INT TERM
  for pid in "${pids[@]}"; do kill -TERM -- "-$pid" 2>/dev/null || true; done
  for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done
  exit 130
}
trap interrupt INT TERM
set -m
for lane in "${LANES[@]}"; do
  (
    set -e
    trap 'printf "%s\n" "$?" > "$ARTIFACT_DIR/'"$lane"'.exit"' EXIT
    run_lane "$lane" > "$ARTIFACT_DIR/$lane.log" 2>&1
  ) &
  pids+=("$!")
 done
for pid in "${pids[@]}"; do wait "$pid" || true; done
failed=0
printf '\nLocal CI result (%s) for %s\n' "$MODE" "$HEAD_SHA"
for job in "${JOBS[@]}"; do
  status=NOT_RUN
  [[ ! -f "$ARTIFACT_DIR/$job.status" ]] || status="$(cat "$ARTIFACT_DIR/$job.status")"
  printf '%-26s %s\n' "$job" "$status"
  [[ "$status" == PASS ]] || failed=1
 done
for lane in "${LANES[@]}"; do
  [[ "$(cat "$ARTIFACT_DIR/$lane.exit" 2>/dev/null || echo 1)" == 0 ]] || failed=1
 done
printf 'Logs: %s\n' "$ARTIFACT_DIR"
exit "$failed"
