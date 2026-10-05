#!/usr/bin/env bash
# Run the PR's locally reproducible CI checks for one committed source revision.
# Each parallel lane gets its own detached checkout; the E2E suites use fresh,
# isolated Compose stacks. Logs and a per-lane result remain in --artifact-dir.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DIR=""
PLAN_ONLY=false
KEEP_CHECKOUTS=false
BASE_REF=origin/develop
while [[ $# -gt 0 ]]; do
  case "$1" in
    --artifact-dir) ARTIFACT_DIR="${2:?--artifact-dir needs a path}"; shift 2 ;;
    --plan) PLAN_ONLY=true; shift ;;
    --keep-checkouts) KEEP_CHECKOUTS=true; shift ;;
    --base) BASE_REF="${2:?--base needs a Git ref}"; shift 2 ;;
    --help|-h)
      cat <<'HELP'
Usage: scripts/run-ci-checks.sh [options]
Run the full local CI package for HEAD, using isolated committed checkouts.
  --plan                 Describe checks without running them
  --base REF             PR base for change-sensitive checks (origin/develop)
  --artifact-dir PATH    Save logs, reports, image identity and aggregate result
  --keep-checkouts       Retain the owned checkouts for debugging
HELP
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done

HEAD_SHA="$(git -C "$REPO_ROOT" rev-parse HEAD)"
BASE_SHA="$(git -C "$REPO_ROOT" rev-parse "$BASE_REF^{commit}")"
SOURCE_BRANCH="$(git -C "$REPO_ROOT" branch --show-current)"
export OE_CI_BASE_SHA="$BASE_SHA" OE_CI_SOURCE_BRANCH="$SOURCE_BRANCH"
SPECKIT_CHANGED=false
if git -C "$REPO_ROOT" diff --name-only "$BASE_SHA...$HEAD_SHA" | \
  grep -Eq '^(\.specify/|\.ai/|\.cursor/|\.claude/|scripts/(install-agent-skills|install-speckit-commands)\.py$|\.github/workflows/speckit-validate\.yml$|scripts/ci/validate-agent-assets\.sh$)'; then
  SPECKIT_CHANGED=true
fi
if ! git -C "$REPO_ROOT" diff --quiet || ! git -C "$REPO_ROOT" diff --cached --quiet; then
  echo "Commit or discard tracked changes first; CI tests the committed source, not this working tree." >&2
  exit 2
fi

if [[ "$PLAN_ONLY" == true ]]; then
  printf 'Source: %s\n' "$HEAD_SHA"
  printf 'Base: %s (%s)\nAgent asset validation applicable: %s\n' "$BASE_REF" "$BASE_SHA" "$SPECKIT_CHANGED"
  printf '%s\n' \
    'Backend: formatting, deployment contract, DataExport, full Maven build and tests' \
    'Frontend: clean install, formatting, lint, Playwright guard, full Vitest, image build, i18n' \
    'E2E: shared candidate images, core Playwright shards, combined analyzer project shards, Cypress shards (fresh database per job)'
  exit 0
fi

for command in git mvn node npm python3 docker; do
  command -v "$command" >/dev/null || { echo "Missing prerequisite: $command" >&2; exit 2; }
done
source "$REPO_ROOT/scripts/ci/docker-env.sh"
configure_docker_environment
"$REPO_ROOT/scripts/run-java21" >/dev/null
# GitHub runs application checks on Node 20 and the deployment contract on Node 22.
if command -v fnm >/dev/null; then
  NODE20_BIN="$(fnm exec --using=20 which node)"
  NODE22_BIN="$(fnm exec --using=22 which node)"
  export PATH="$(dirname "$NODE20_BIN"):$PATH"
else
  [[ "$(node -p 'process.versions.node.split(".")[0]')" == 20 ]] || {
    echo 'Node 20 is required (or install fnm with Node 20 and 22).' >&2
    exit 2
  }
  NODE22_BIN="${OE_CI_NODE22_BIN:?Set OE_CI_NODE22_BIN to the Node 22 executable for the deployment contract.}"
fi
if [[ -z "$ARTIFACT_DIR" ]]; then
  ARTIFACT_DIR="${TMPDIR:-/tmp}/oe-full-ci-${HEAD_SHA:0:10}-$(date -u +%Y%m%d%H%M%S)-$$"
fi
mkdir -p "$ARTIFACT_DIR/checkouts"
ARTIFACT_DIR="$(cd "$ARTIFACT_DIR" && pwd)"
printf '%s\n' "$HEAD_SHA" > "$ARTIFACT_DIR/source-head.txt"
printf '%s\n' "$BASE_SHA" > "$ARTIFACT_DIR/base-head.txt"
export CI_CANDIDATE_IMAGE_PREFIX="oe2-ci-${HEAD_SHA:0:12}-$(date -u +%Y%m%d%H%M%S)-$$"
printf '%s\n' "$CI_CANDIDATE_IMAGE_PREFIX" > "$ARTIFACT_DIR/candidate-image-prefix.txt"
printf 'Full local CI: %s\nLogs: %s\n' "$HEAD_SHA" "$ARTIFACT_DIR"
python3 -m venv "$ARTIFACT_DIR/python"
"$ARTIFACT_DIR/python/bin/python" -m pip install --disable-pip-version-check PyYAML==6.0.2 > "$ARTIFACT_DIR/python-dependencies.log" 2>&1
export PATH="$ARTIFACT_DIR/python/bin:$PATH"
python3 "$REPO_ROOT/scripts/ci/e2e-plan.py" "$ARTIFACT_DIR"

cleanup() {
  local original_status=$?
  trap - EXIT INT TERM
  if [[ "$KEEP_CHECKOUTS" == false ]]; then
    for lane in backend frontend e2e; do
      local checkout="$ARTIFACT_DIR/checkouts/$lane"
      if [[ -d "$checkout" ]]; then
        for report in "$checkout"/target/surefire-reports "$checkout"/target/failsafe-reports \
          "$checkout"/dataexport/*/target/surefire-reports; do
          [[ -d "$report" ]] || continue
          local destination="$ARTIFACT_DIR/$lane-artifacts/${report#"$checkout"/}"
          mkdir -p "$(dirname "$destination")"
          cp -R "$report" "$destination" || original_status=1
        done
        if ! git -C "$REPO_ROOT" worktree remove --force --force "$checkout" >> "$ARTIFACT_DIR/cleanup.log" 2>&1; then
          original_status=1
        fi
      fi
    done
  fi
  printf '%s\n' "$original_status" > "$ARTIFACT_DIR/result.exit"
  exit "$original_status"
}
trap cleanup EXIT

for lane in backend frontend e2e; do
  checkout="$ARTIFACT_DIR/checkouts/$lane"
  git -C "$REPO_ROOT" worktree add --detach "$checkout" "$HEAD_SHA" >> "$ARTIFACT_DIR/setup.log" 2>&1
  if [[ "$lane" != frontend ]]; then
    git -C "$checkout" submodule update --init --recursive >> "$ARTIFACT_DIR/setup.log" 2>&1
  fi
done

run_backend() {
  local root="$ARTIFACT_DIR/checkouts/backend"
  cd "$root"
  scripts/run-java21 mvn spotless:check
  "$NODE22_BIN" --test .github/scripts/publish-checkpoints.test.cjs
  python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
  python3 -m unittest discover -s scripts/tests -p 'test_*.py' -v
  if [[ "$SPECKIT_CHANGED" == true ]]; then
    bash scripts/ci/validate-agent-assets.sh
  fi
  (cd dataexport && ../scripts/run-java21 mvn clean install)
  scripts/run-java21 mvn clean install -Dspotless.check.skip=true
}

run_frontend() {
  local root="$ARTIFACT_DIR/checkouts/frontend"
  cd "$root/frontend"
  npm ci --legacy-peer-deps
  npx prettier ./ --check
  npm run lint
  npm run lint:all || echo 'Advisory src lint failed; GitHub treats this step as non-blocking.'
  npm run pw:guard
  CI=true npm test
  docker build -f Dockerfile -t "${CI_CANDIDATE_IMAGE_PREFIX}-frontend-static:ci" .
  python3 "$root/scripts/ci/validate-i18n.py" --base "$OE_CI_BASE_SHA" --branch "$OE_CI_SOURCE_BRANCH"
}

run_e2e_step() {
  local name="$1"
  shift
  printf 'Running %s\n' "$name"
  local result=0
  "$@" > "$ARTIFACT_DIR/$name.log" 2>&1 || result=$?
  if [[ "$result" == 0 ]]; then
    printf 'PASS\n' > "$ARTIFACT_DIR/$name.status"
  else
    printf 'FAIL\n' > "$ARTIFACT_DIR/$name.status"
  fi
  local checkout="$ARTIFACT_DIR/checkouts/e2e/frontend"
  local evidence="$ARTIFACT_DIR/$name-artifacts"
  # CI=true selects Playwright's blob reporter, so blob-report holds the report.
  for path in test-results playwright-report blob-report cypress/screenshots; do
    if [[ -d "$checkout/$path" ]]; then
      mkdir -p "$evidence/$(dirname "$path")"
      cp -R "$checkout/$path" "$evidence/$path" || result=1
      rm -rf "$checkout/$path"
    fi
  done
  [[ "$result" == 0 ]] || printf 'FAIL\n' > "$ARTIFACT_DIR/$name.status"
  return "$result"
}

run_shared_build() {
  local root="$ARTIFACT_DIR/checkouts/e2e"
  cd "$root"
  # GitHub's shared image job reads this exact two-file set.
  docker buildx bake \
    -f build.docker-compose.yml \
    -f .github/ci/ci.analyzer-harness.yml \
    --print > "$ARTIFACT_DIR/bake-input.json" || return
  python3 - "$ARTIFACT_DIR" "$CI_CANDIDATE_IMAGE_PREFIX" "$HEAD_SHA" <<'PY'
import json, pathlib, sys
directory, prefix, sha = sys.argv[1:]
path = pathlib.Path(directory)
definition = json.loads((path / 'bake-input.json').read_text())
overrides = {'target': {name: {'tags': [f'{prefix}-{name}:ci'],
                              'labels': {'org.opencontainers.image.revision': sha}}
                         for name in definition['target']}}
(path / 'candidate-tags.json').write_text(json.dumps(overrides, indent=2))
PY
  docker buildx bake -f build.docker-compose.yml -f .github/ci/ci.analyzer-harness.yml \
    -f "$ARTIFACT_DIR/candidate-tags.json" --load || return
  docker compose -f build.docker-compose.yml -f .github/ci/ci.analyzer-harness.yml \
    pull --ignore-buildable --quiet
}

run_e2e() {
  local root="$ARTIFACT_DIR/checkouts/e2e"
  local failed=0
  cd "$root"
  run_e2e_step e2e-scope node --test .github/scripts/e2e-scope.test.cjs || failed=1
  run_e2e_step shared-build run_shared_build || return 1
  run_e2e_step e2e-frontend-deps bash -c "cd frontend && npm ci --legacy-peer-deps && npx playwright install --only-shell chromium" || return 1
  export OE_CI_PROJECT_FILE="$ARTIFACT_DIR/core-compose-project.txt"
  while IFS=$'\t' read -r name suite projects shard; do
    if [[ "$suite" == analyzer ]]; then
      local project_args=()
      IFS=',' read -ra selected_projects <<< "$projects"
      for project in "${selected_projects[@]}"; do project_args+=(--project "$project"); done
      run_e2e_step "$name" projects/analyzer-harness/ci-parity-test.sh \
        "${project_args[@]}" --shard "$shard" --artifact-dir "$ARTIFACT_DIR/$name" || failed=1
    elif [[ "$suite" == core ]]; then
      OE_CI_CORE_PROJECTS="$projects" run_e2e_step "$name" scripts/run-e2e-like-ci.sh \
        --no-build --cleanup -- --shard="$shard" || failed=1
    else
      run_e2e_step "$name" scripts/run-e2e-like-ci.sh --no-build --suite "$suite" --cleanup || failed=1
    fi
  done < "$ARTIFACT_DIR/e2e-jobs.tsv"
  return "$failed"
}

pids=()
cleanup_owned_stacks() {
  local root="$ARTIFACT_DIR/checkouts/e2e"
  local project
  local failed=0
  if [[ -f "$ARTIFACT_DIR/core-compose-project.txt" ]]; then
    project="$(cat "$ARTIFACT_DIR/core-compose-project.txt")"
    (cd "$root" && E2E_STACK_PROJECT="$project" docker compose -p "$project" \
      -f build.docker-compose.yml -f build.docker-compose.worktree.yml \
      down -v --remove-orphans) >> "$ARTIFACT_DIR/cleanup.log" 2>&1 || failed=1
  fi
  for project_file in "$ARTIFACT_DIR"/analyzer-*/compose-project.txt; do
    [[ -f "$project_file" ]] || continue
    project="$(cat "$project_file")"
    (cd "$root" && CI_PARITY_COMPOSE_PROJECT="$project" \
      CI_PARITY_IMAGE_PREFIX=ci-cleanup CI_PARITY_ANALYZER_SUBNET_PREFIX=10.64 \
      docker compose -p "$project" -f build.docker-compose.yml \
      -f projects/analyzer-harness/docker-compose.base.yml \
      -f .github/ci/ci.analyzer-harness.yml \
      -f projects/analyzer-harness/docker-compose.ci-parity-isolated.yml \
      down -v --remove-orphans) >> "$ARTIFACT_DIR/cleanup.log" 2>&1 || failed=1
  done
  return "$failed"
}
interrupt_lanes() {
  trap - INT TERM
  for pid in "${pids[@]}"; do kill -TERM -- "-$pid" 2>/dev/null || true; done
  for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done
  for pid in "${pids[@]}"; do kill -KILL -- "-$pid" 2>/dev/null || true; done
  cleanup_owned_stacks || echo "Owned stack cleanup failed; see $ARTIFACT_DIR/cleanup.log" >&2
  exit 130
}
trap interrupt_lanes INT TERM
set -m # Give each background lane its own process group for cancellation.
for lane in backend frontend e2e; do
  (
    set -e
    record_exit() { printf '%s\n' "$?" > "$ARTIFACT_DIR/$lane.exit"; }
    trap record_exit EXIT
    "run_$lane" > "$ARTIFACT_DIR/$lane.log" 2>&1
  ) &
  pids+=("$!")
done

set +e
for pid in "${pids[@]}"; do wait "$pid"; done
set -e

failed=0
printf '\nLocal CI result for %s\n' "$HEAD_SHA"
while IFS= read -r lane; do
  if [[ -f "$ARTIFACT_DIR/$lane.status" ]]; then
    result="$(cat "$ARTIFACT_DIR/$lane.status")"
  elif [[ -f "$ARTIFACT_DIR/$lane.exit" ]]; then
    code="$(cat "$ARTIFACT_DIR/$lane.exit")"
    [[ "$code" == 0 ]] && result=PASS || result=FAIL
  else
    result=NOT_RUN
  fi
  printf '%-24s %s\n' "$lane" "$result"
  [[ "$result" == PASS ]] || failed=1
done < "$ARTIFACT_DIR/expected-lanes.txt"
printf 'Logs: %s\n' "$ARTIFACT_DIR"
exit "$failed"
