#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

FIXTURE_DIR="$TMP_DIR/src/test/resources"
FAKE_BIN="$TMP_DIR/bin"
DOCKER_LOG="$TMP_DIR/docker.log"
mkdir -p "$FIXTURE_DIR/fixtures" "$FIXTURE_DIR/testdata" "$FAKE_BIN"

cp "$REPO_ROOT/src/test/resources/load-test-fixtures.sh" "$FIXTURE_DIR/"
touch "$FIXTURE_DIR/e2e-foundational-data.sql"
touch "$FIXTURE_DIR/fixtures/analyzer-harness-lane-data.sql"
touch "$FIXTURE_DIR/fixtures/storage-in-progress-order.sql"
touch "$FIXTURE_DIR/testdata/storage-e2e.xml"
touch "$FIXTURE_DIR/testdata/xml-to-sql.py"

cat > "$FAKE_BIN/python3" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
touch "$3"
EOF

cat > "$FAKE_BIN/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$DOCKER_LOG"

case "${1:-}" in
  inspect)
    printf 'true\n'
    ;;
  ps)
    printf 'autodiscovered-database\n'
    ;;
  exec)
    if [[ "$*" == *"to_regclass"* ]]; then
      printf 't\n'
    elif [[ "$*" == *"SELECT COUNT"* ]]; then
      printf '3\n'
    fi
    ;;
esac
EOF

chmod +x "$FAKE_BIN/python3" "$FAKE_BIN/docker"

OUTPUT="$({
  PATH="$FAKE_BIN:$PATH" \
  DOCKER_LOG="$DOCKER_LOG" \
  DB_CONTAINER="analyzers-openelisglobal-database" \
    bash "$FIXTURE_DIR/load-test-fixtures.sh" --profile=core --no-verify
} 2>&1)"

grep -Fq "Using Docker container: analyzers-openelisglobal-database" <<< "$OUTPUT"
grep -Fq "exec -i analyzers-openelisglobal-database" "$DOCKER_LOG"

if grep -Fq "ps --format" "$DOCKER_LOG"; then
  echo "fixture loader ignored the explicit DB_CONTAINER and performed autodiscovery" >&2
  exit 1
fi

echo "Fixture loader explicit DB container test passed."
