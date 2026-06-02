#!/usr/bin/env bash
# Score open PRs and print top-N markdown digest.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

TOP_N="${TOP_N:-15}"
BASE="${BASE_BRANCH:-develop}"
MARKER="<!-- prflow-queue-digest -->"

score_pr() {
  local pr="$1"
  local json title author created additions deletions changed merge_state approvals changes_req ci_ok age score
  json="$(pr_view_json "$pr" --json title,author,createdAt,additions,deletions,changedFiles,mergeStateStatus,isDraft)"
  if [[ "$(echo "$json" | jq -r '.isDraft')" == "true" ]]; then
    return 0
  fi

  title="$(echo "$json" | jq -r '.title')"
  author="$(echo "$json" | jq -r '.author.login')"
  created="$(echo "$json" | jq -r '.createdAt')"
  additions="$(echo "$json" | jq -r '.additions')"
  deletions="$(echo "$json" | jq -r '.deletions')"
  changed="$(echo "$json" | jq -r '.changedFiles')"
  merge_state="$(echo "$json" | jq -r '.mergeStateStatus')"
  approvals="$(count_approvals "$pr" 2>/dev/null || echo 0)"
  changes_req=0
  if has_changes_requested "$pr" 2>/dev/null; then changes_req=1; fi
  ci_ok=0
  if ci_all_success "$pr" 2>/dev/null; then ci_ok=1; fi
  age="$(age_days "$created")"
  score=0
  score=$((score + age * 2))
  if [[ "$changed" -le 3 && $((additions + deletions)) -lt 100 ]]; then
    score=$((score - 10))
  fi
  if [[ "$changed" -gt 20 || $((additions + deletions)) -gt 1000 ]]; then
    score=$((score + 20))
  fi
  if [[ "$ci_ok" -eq 1 ]]; then score=$((score - 15)); else score=$((score + 30)); fi
  if [[ "$approvals" -eq 0 && "$ci_ok" -eq 1 ]]; then score=$((score + 25)); fi
  if [[ "$changes_req" -eq 1 ]]; then score=$((score - 50)); fi
  if [[ "$author" == "dependabot[bot]" && "$ci_ok" -eq 1 ]]; then score=$((score + 40)); fi

  printf '%d\t%s\t%s\t%s\t%s\t%d\t%d\t%d\t%s\n' \
    "$score" "$pr" "$author" "$merge_state" "$approvals" "$changes_req" "$ci_ok" "$age" "$title"
}

echo "Scoring open PRs against ${BASE}..."
mapfile -t rows < <(
  gh pr list --repo "$REPO" --state open --base "$BASE" --limit 300 --json number \
    | jq -r '.[].number' | while read -r n; do score_pr "$n"; done | sort -t$'\t' -k1,1nr
)

DATE_UTC="$(date -u +%Y-%m-%d)"
BODY="${MARKER}
## PRFlow queue digest (${DATE_UTC} UTC)

Top **${TOP_N}** open PRs by priority score (higher = review sooner).

| Score | PR | Author | Merge | Appr | Chg req | CI | Age | Title |
| ----: | -- | ------ | ----- | ---: | ------: | -- | --: | ----- |"

count=0
for row in "${rows[@]}"; do
  [[ -z "$row" ]] && continue
  IFS=$'\t' read -r sc num author mstate appr chg ci age title <<<"$row"
  ci_mark="fail"
  [[ "$ci" == "1" ]] && ci_mark="ok"
  BODY+="
| ${sc} | [#${num}](https://github.com/${REPO}/pull/${num}) | ${author} | ${mstate} | ${appr} | ${chg} | ${ci_mark} | ${age}d | ${title} |"
  count=$((count + 1))
  [[ "$count" -ge "$TOP_N" ]] && break
done

BODY+="
---
_Automated by [prflow-queue-digest](https://github.com/${REPO}/blob/develop/.github/workflows/prflow-queue-digest.yml). Score: age×2, small−10, large+20, CI ok−15, no approval+CI+25, changes requested−50, dependabot+CI+40._"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  echo "$BODY" >>"$GITHUB_STEP_SUMMARY"
fi

echo "$BODY"

ISSUE_NUM="${PRFLOW_DIGEST_ISSUE:-0}"
if [[ "$ISSUE_NUM" =~ ^[0-9]+$ && "$ISSUE_NUM" -gt 0 ]]; then
  existing="$(gh api "/repos/${REPO}/issues/${ISSUE_NUM}/comments" --paginate \
    | jq -r --arg m "$MARKER" '.[] | select(.body | contains($m)) | .id' | head -1 || true)"
  if [[ -n "$existing" ]]; then
    gh api -X PATCH "/repos/${REPO}/issues/comments/${existing}" -f body="$BODY" >/dev/null
    echo "Updated digest comment on issue #${ISSUE_NUM}"
  else
    gh issue comment "$ISSUE_NUM" --repo "$REPO" --body "$BODY" >/dev/null
    echo "Posted digest comment on issue #${ISSUE_NUM}"
  fi
fi
