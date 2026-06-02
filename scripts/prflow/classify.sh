#!/usr/bin/env bash
# Classify a PR by size, area, and type from title + changed files.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

PR="${1:-}"
if [[ -z "$PR" ]]; then
  echo "Usage: classify.sh <pr-number>" >&2
  exit 1
fi

TITLE="$(pr_view_json "$PR" --json title --jq '.title')"
FILES="$(pr_view_json "$PR" --json files --jq '.files[].path')"
ADDITIONS="$(pr_view_json "$PR" --json additions --jq '.additions')"
DELETIONS="$(pr_view_json "$PR" --json deletions --jq '.deletions')"
CHANGED="$(pr_view_json "$PR" --json changedFiles --jq '.changedFiles')"
TOTAL=$((ADDITIONS + DELETIONS))

# Ensure labels exist (idempotent)
for lbl in prflow-size-small prflow-size-medium prflow-size-large \
  prflow-area-backend prflow-area-frontend prflow-area-docs prflow-area-ci prflow-area-other \
  prflow-type-bug prflow-type-feature prflow-type-test prflow-type-chore prflow-type-deps; do
  ensure_label "$lbl"
done

# Size
SIZE_LABEL="prflow-size-medium"
if [[ "$CHANGED" -le 3 && "$TOTAL" -lt 100 ]]; then
  SIZE_LABEL="prflow-size-small"
elif [[ "$CHANGED" -gt 20 || "$TOTAL" -gt 1000 ]]; then
  SIZE_LABEL="prflow-size-large"
fi

# Area from paths
AREA="prflow-area-other"
if echo "$FILES" | grep -qE '^frontend/'; then
  AREA="prflow-area-frontend"
fi
if echo "$FILES" | grep -qE '^(src/|dataexport/|plugins/)'; then
  AREA="prflow-area-backend"
fi
if echo "$FILES" | grep -qE '^\.github/workflows/'; then
  AREA="prflow-area-ci"
fi
if echo "$FILES" | grep -qE '^(README|docs/|\.specify/)'; then
  AREA="prflow-area-docs"
fi

# Type from title
shopt -s nocasematch
TYPE="prflow-type-feature"
if [[ "$TITLE" =~ ^(fix|bug) ]]; then
  TYPE="prflow-type-bug"
elif [[ "$TITLE" =~ ^test ]]; then
  TYPE="prflow-type-test"
elif [[ "$TITLE" =~ ^(chore|docs) ]]; then
  TYPE="prflow-type-chore"
elif [[ "$TITLE" =~ dependabot|bump ]]; then
  TYPE="prflow-type-deps"
fi
shopt -u nocasematch

remove_labels_matching "$PR" "prflow-size-"
remove_labels_matching "$PR" "prflow-area-"
remove_labels_matching "$PR" "prflow-type-"
add_labels "$PR" "$SIZE_LABEL" "$AREA" "$TYPE"

echo "Classified PR #${PR}: ${SIZE_LABEL}, ${AREA}, ${TYPE}"
