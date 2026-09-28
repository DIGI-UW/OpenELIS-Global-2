#!/usr/bin/env bash
# Run the PR's locally reproducible CI checks for one committed source revision.
# Each parallel lane gets its own detached checkout; the E2E suites use fresh,
# isolated Compose stacks. Logs and a per-lane result remain in --artifact-dir.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DIR=""
PLAN_ONLY=false
KEEP_CHECKOUTS=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --artifact-dir) ARTIFACT_DIR="${2:?--artifact-dir needs a path}"; shift 2 ;;
    --plan) PLAN_ONLY=true; shift ;;
    --keep-checkouts) KEEP_CHECKOUTS=true; shift ;;
    --help|-h)
      sed -n '1,34p' "$0"
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done

HEAD_SHA="$(git -C "$REPO_ROOT" rev-parse HEAD)"
if ! git -C "$REPO_ROOT" diff --quiet || ! git -C "$REPO_ROOT" diff --cached --quiet; then
  echo "Commit or discard tracked changes first; CI tests the committed source, not this working tree." >&2
  exit 2
fi

if [[ "$PLAN_ONLY" == true ]]; then
  printf 'Source: %s\n' "$HEAD_SHA"
  printf '%s\n' \
    'Backend: formatting, deployment contract, DataExport, full Maven build and tests' \
    'Frontend: clean install, formatting, lint, Playwright guard, full Vitest, image build, i18n' \
    'E2E: shared plugin build, core Playwright, both analyzer projects, all three Cypress shards (fresh database per suite)'
  exit 0
fi

for command in git mvn node npm python3 docker; do
  command -v "$command" >/dev/null || { echo "Missing prerequisite: $command" >&2; exit 2; }
done
if [[ -z "$ARTIFACT_DIR" ]]; then
  ARTIFACT_DIR="${TMPDIR:-/tmp}/oe-full-ci-${HEAD_SHA:0:10}-$(date -u +%Y%m%d%H%M%S)-$$"
fi
mkdir -p "$ARTIFACT_DIR/checkouts"
ARTIFACT_DIR="$(cd "$ARTIFACT_DIR" && pwd)"
printf '%s\n' "$HEAD_SHA" > "$ARTIFACT_DIR/source-head.txt"
printf 'Full local CI: %s\nLogs: %s\n' "$HEAD_SHA" "$ARTIFACT_DIR"
python3 -m venv "$ARTIFACT_DIR/python"
"$ARTIFACT_DIR/python/bin/python" -m pip install --disable-pip-version-check PyYAML==6.0.2 > "$ARTIFACT_DIR/python-dependencies.log" 2>&1
export PATH="$ARTIFACT_DIR/python/bin:$PATH"

cleanup() {
  local original_status=$?
  trap - EXIT INT TERM
  if [[ "$KEEP_CHECKOUTS" == false ]]; then
    for lane in backend frontend e2e; do
      local checkout="$ARTIFACT_DIR/checkouts/$lane"
      if [[ -d "$checkout" ]]; then
        git -C "$REPO_ROOT" worktree remove --force "$checkout" >> "$ARTIFACT_DIR/cleanup.log" 2>&1 || true
      fi
    done
  fi
  return "$original_status"
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
  node --test .github/scripts/publish-checkpoints.test.cjs
  python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
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
  docker build -f Dockerfile -t "oe-full-ci-frontend:${HEAD_SHA:0:12}" .
  python3 - <<'PY'
import glob, json, pathlib, subprocess, sys
errors = []
for path in sorted(glob.glob('src/languages/*.json')):
    pairs = json.loads(pathlib.Path(path).read_text(), object_pairs_hook=lambda value: value)
    seen = set()
    duplicates = [key for key, _ in pairs if key in seen or seen.add(key)]
    if duplicates:
        errors.append(f'{path}: duplicate keys: {duplicates[:5]}')
base = subprocess.check_output(['git', 'merge-base', 'HEAD', 'origin/develop'], text=True).strip()
changed = subprocess.check_output(['git', 'diff', '--name-only', base, 'HEAD', '--', 'frontend/src/languages/'], text=True).splitlines()
non_english = [path for path in changed if path.endswith('.json') and not path.endswith('/en.json')]
if non_english:
    errors.append('Non-English locale changes: ' + ', '.join(non_english))
if errors:
    sys.exit('\n'.join(errors))
print('i18n duplicate-key and source-of-truth checks passed')
PY
}

run_e2e_step() {
  local name="$1"
  shift
  printf 'Running %s\n' "$name"
  if "$@" > "$ARTIFACT_DIR/$name.log" 2>&1; then
    printf 'PASS\n' > "$ARTIFACT_DIR/$name.status"
    return 0
  fi
  printf 'FAIL\n' > "$ARTIFACT_DIR/$name.status"
  local checkout="$ARTIFACT_DIR/checkouts/e2e/frontend"
  local evidence="$ARTIFACT_DIR/$name-artifacts"
  for path in test-results playwright-report cypress/screenshots; do
    if [[ -d "$checkout/$path" ]]; then
      mkdir -p "$evidence/$(dirname "$path")"
      cp -R "$checkout/$path" "$evidence/$path" || true
    fi
  done
  return 1
}

run_shared_build() {
  local root="$ARTIFACT_DIR/checkouts/e2e"
  cd "$root"
  (cd dataexport/dataexport-core && ../../scripts/run-java21 mvn clean install -DskipTests -Dmaven.test.skip=true) || return
  (cd dataexport && ../scripts/run-java21 mvn clean install -DskipTests -Dmaven.test.skip=true) || return
  scripts/run-java21 mvn clean install -DskipTests -Dspotless.check.skip=true -Drevision=3.2.1.3 || return
  scripts/run-java21 mvn -f plugins/pom.xml -pl analyzers/GenericASTM,analyzers/GenericHL7,analyzers/GenericFile -am clean install -DskipTests -Dmaven.test.skip=true
}

run_e2e() {
  local root="$ARTIFACT_DIR/checkouts/e2e"
  local failed=0
  cd "$root"
  run_e2e_step e2e-scope node --test .github/scripts/e2e-scope.test.cjs || failed=1
  run_e2e_step shared-build-plugins run_shared_build || failed=1
  run_e2e_step core-playwright scripts/run-e2e-like-ci.sh --cleanup || failed=1
  run_e2e_step analyzer-foundational projects/analyzer-harness/ci-parity-test.sh --build --project harness-foundational --artifact-dir "$ARTIFACT_DIR/analyzer-foundational" || failed=1
  run_e2e_step analyzer-demo projects/analyzer-harness/ci-parity-test.sh --project harness-demo --artifact-dir "$ARTIFACT_DIR/analyzer-demo" || failed=1
  for shard in core admin independent; do
    run_e2e_step "cypress-$shard" scripts/run-e2e-like-ci.sh --suite "cypress-$shard" --cleanup || failed=1
  done
  return "$failed"
}

pids=()
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
for lane in backend frontend e2e-scope shared-build-plugins core-playwright analyzer-foundational analyzer-demo cypress-core cypress-admin cypress-independent; do
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
done
printf 'Logs: %s\n' "$ARTIFACT_DIR"
exit "$failed"
