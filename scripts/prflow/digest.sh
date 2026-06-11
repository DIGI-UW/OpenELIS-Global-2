#!/usr/bin/env bash
# Score open PRs and print top-N markdown digest (single batched gh pr list call).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

TOP_N="${TOP_N:-15}"
BASE="${BASE_BRANCH:-develop}"
MARKER="<!-- prflow-queue-digest -->"

echo "Scoring open PRs against ${BASE} (batched)..."
PR_JSON="$(gh pr list --repo "$REPO" --state open --base "$BASE" --limit 300 --json \
  number,title,author,createdAt,additions,deletions,changedFiles,mergeStateStatus,isDraft,reviewDecision,statusCheckRollup)"

ROWS="$(echo "$PR_JSON" | jq --argjson top_n "$TOP_N" '
  [.[] | select(.isDraft == false) |
    ((now - (.createdAt | fromdateiso8601)) / 86400 | floor) as $age |
    (if (.statusCheckRollup.state? // "") == "SUCCESS" then 1 else 0 end) as $ci_ok |
    (if .reviewDecision == "CHANGES_REQUESTED" then 1 else 0 end) as $chg_req |
    (if .reviewDecision == "APPROVED" then 1 else 0 end) as $appr |
    (.changedFiles) as $changed |
    ((.additions + .deletions)) as $total |
  {
    score: (
      0
      + ($age * 2)
      + (if ($changed <= 3 and $total < 100) then -10 else 0 end)
      + (if ($changed > 20 or $total > 1000) then 20 else 0 end)
      + (if $ci_ok == 1 then -15 else 30 end)
      + (if ($appr == 0 and $ci_ok == 1) then 25 else 0 end)
      + (if $chg_req == 1 then -50 else 0 end)
      + (if (.author.login == "dependabot[bot]" and $ci_ok == 1) then 40 else 0 end)
    ),
    number: .number,
    author: .author.login,
    merge: .mergeStateStatus,
    approvals: $appr,
    chg_req: $chg_req,
    ci_ok: $ci_ok,
    age: $age,
    title: (.title | gsub("\t"; " ") | gsub("\\|"; "/"))
  }]
  | sort_by(-.score)
  | .[:$top_n]
')"

DATE_UTC="$(date -u +%Y-%m-%d)"
BODY="${MARKER}
## PRFlow queue digest (${DATE_UTC} UTC)

Top **${TOP_N}** open PRs by priority score (higher = review sooner).

| Score | PR | Author | Merge | Appr | Chg req | CI | Age | Title |
| ----: | -- | ------ | ----- | ---: | ------: | -- | --: | ----- |"

while IFS= read -r row; do
  [[ -z "$row" ]] && continue
  sc="$(echo "$row" | jq -r '.score')"
  num="$(echo "$row" | jq -r '.number')"
  author="$(echo "$row" | jq -r '.author')"
  mstate="$(echo "$row" | jq -r '.merge')"
  appr="$(echo "$row" | jq -r '.approvals')"
  chg="$(echo "$row" | jq -r '.chg_req')"
  ci="$(echo "$row" | jq -r '.ci_ok')"
  age="$(echo "$row" | jq -r '.age')"
  title="$(echo "$row" | jq -r '.title')"
  ci_mark="fail"
  [[ "$ci" == "1" ]] && ci_mark="ok"
  BODY+="
| ${sc} | [#${num}](https://github.com/${REPO}/pull/${num}) | ${author} | ${mstate} | ${appr} | ${chg} | ${ci_mark} | ${age}d | ${title} |"
done < <(echo "$ROWS" | jq -c '.[]')

BODY+="
---
_Automated by [prflow-queue-digest](https://github.com/${REPO}/blob/develop/.github/workflows/prflow-queue-digest.yml). Score: age×2, small−10, large+20, CI ok−15, no approval+CI+25, changes requested−50, dependabot+CI+40. Fetched in one batched \`gh pr list\` call._"

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
