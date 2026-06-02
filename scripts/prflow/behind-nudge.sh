#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

PR="${1:-}"
MARKER="<!-- prflow-behind-nudge -->"

[[ -z "$PR" ]] && exit 1

AUTHOR="$(pr_view_json "$PR" --json author --jq '.author.login')"
if [[ "$AUTHOR" == "dependabot[bot]" ]]; then
  exit 0
fi

if has_changes_requested "$PR"; then
  exit 0
fi

merge_state="$(get_merge_state "$PR")"
if [[ "$merge_state" != "BEHIND" && "$merge_state" != "BEHIND_CLEAN" ]]; then
  exit 0
fi

if ! ci_all_success "$PR"; then
  exit 0
fi

if [[ -n "$(bot_comment_exists "$PR" "$MARKER")" ]]; then
  exit 0
fi

MSG="${MARKER}
### PRFlow: Branch is behind \`develop\`

CI is green and there are no requested changes. The main blocker is likely **branch drift**, not code quality.

**Please:**
1. Click **Update branch** on this PR (or rebase onto \`develop\`).
2. Re-request review after the update.

Maintainers: this PR is a good quick-merge candidate once updated and approved."

post_pr_comment "$PR" "$MSG"
echo "Posted behind nudge on PR #${PR}"
