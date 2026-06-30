#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

PR="${1:-}"
MARKER="<!-- prflow-dependabot-ready -->"
LABEL="prflow-ready-to-merge"

[[ -z "$PR" ]] && exit 1

AUTHOR="$(pr_view_json "$PR" --json author --jq '.author.login')"
[[ "$AUTHOR" != "dependabot[bot]" ]] && exit 0

ensure_label "$LABEL" "0e8a16"
merge_state="$(get_merge_state "$PR")"

if [[ "$merge_state" == "DIRTY" ]]; then
  gh pr edit "$PR" --repo "$REPO" --remove-label "$LABEL" 2>/dev/null || true
  exit 0
fi

if ! ci_all_success "$PR"; then
  gh pr edit "$PR" --repo "$REPO" --remove-label "$LABEL" 2>/dev/null || true
  exit 0
fi

add_labels "$PR" "$LABEL"

if [[ -n "$(bot_comment_exists "$PR" "$MARKER")" ]]; then
  exit 0
fi

MSG="$(cat <<EOF
${MARKER}
### PRFlow: Dependabot ready to merge

This dependabot PR has **passing CI** and no merge conflicts.

**Next steps:**
1. Click **Update branch** if the PR is behind \`develop\`.
2. Get a maintainer approval (branch protection).
3. Merge (prefer batching sibling catalyst-mcp / catalyst-agents bumps together).

_No auto-merge in Phase 1._
EOF
)"

post_pr_comment "$PR" "$MSG"
echo "Marked dependabot PR #${PR} as ready"
