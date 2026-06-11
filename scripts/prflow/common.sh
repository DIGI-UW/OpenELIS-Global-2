#!/usr/bin/env bash
# Shared helpers for PRFlow Phase 1 scripts. Safe for pull_request_target (API only).
set -euo pipefail

REPO="${REPO:-${GITHUB_REPOSITORY:-}}"
if [[ -z "$REPO" ]]; then
  echo "REPO or GITHUB_REPOSITORY must be set" >&2
  exit 1
fi

gh_api() {
  gh api -H "Accept: application/vnd.github+json" "$@"
}

pr_view_json() {
  local pr="$1"
  shift
  gh pr view "$pr" --repo "$REPO" "$@"
}

# Latest review state per user (APPROVED | CHANGES_REQUESTED | COMMENTED | DISMISSED)
count_approvals() {
  local pr="$1"
  pr_view_json "$pr" --json reviews \
    | jq '[.reviews[] | select(.state=="APPROVED")] | group_by(.author.login) | map(last) | map(select(.state=="APPROVED")) | length'
}

has_changes_requested() {
  local pr="$1"
  local n
  n="$(pr_view_json "$pr" --json reviews \
    | jq '[.reviews[] | select(.author.login != null) | {user: .author.login, state: .state}]
         | group_by(.user) | map(last)
         | map(select(.state=="CHANGES_REQUESTED")) | length')"
  [[ "$n" -gt 0 ]]
}

get_merge_state() {
  local pr="$1"
  pr_view_json "$pr" --json mergeStateStatus --jq '.mergeStateStatus'
}

# gh pr checks --json valid fields: bucket, completedAt, description, event, link, name, startedAt, state, workflow
ci_all_success() {
  local pr="$1"
  local checks
  if ! checks="$(gh pr checks "$pr" --repo "$REPO" --json name,state,bucket)"; then
    echo "ERROR: gh pr checks failed for PR #${pr}" >&2
    return 1
  fi

  local total pending fail
  total="$(echo "$checks" | jq 'length')"
  if [[ "$total" -eq 0 ]]; then
    return 1
  fi

  pending="$(echo "$checks" | jq '[.[] | select(.bucket == "pending")] | length')"
  fail="$(echo "$checks" | jq '[.[] | select(.bucket == "fail" or .bucket == "cancel")] | length')"
  [[ "$pending" -eq 0 && "$fail" -eq 0 ]]
}

# Safe for markdown tables and TSV output.
sanitize_table_cell() {
  local s="$1"
  s="${s//$'\t'/ }"
  s="${s//|/\//}"
  printf '%s' "$s"
}

pr_has_label() {
  local pr="$1"
  local label="$2"
  pr_view_json "$pr" --json labels --jq ".labels[] | select(.name==\"$label\") | .name" | grep -qx "$label"
}

add_labels() {
  local pr="$1"
  shift
  [[ $# -eq 0 ]] && return 0
  gh pr edit "$pr" --repo "$REPO" --add-label "$*"
}

remove_labels_matching() {
  local pr="$1"
  local prefix="$2"
  local labels
  labels="$(pr_view_json "$pr" --json labels --jq ".labels[].name" | grep "^${prefix}" || true)"
  if [[ -n "$labels" ]]; then
    while IFS= read -r lbl; do
      [[ -n "$lbl" ]] && gh pr edit "$pr" --repo "$REPO" --remove-label "$lbl" || true
    done <<<"$labels"
  fi
}

ensure_label() {
  local name="$1"
  local color="${2:-ededed}"
  gh label create "$name" --repo "$REPO" --color "$color" --force 2>/dev/null || true
}

bot_comment_exists() {
  local pr="$1"
  local marker="$2"
  gh api "/repos/${REPO}/issues/${pr}/comments" --paginate \
    | jq -r --arg m "$marker" '.[] | select(.body | contains($m)) | .id' | head -1
}

post_pr_comment() {
  local pr="$1"
  local body="$2"
  gh pr comment "$pr" --repo "$REPO" --body "$body"
}

age_days() {
  local created="$1"
  local epoch_created epoch_now
  epoch_created="$(date -d "$created" +%s 2>/dev/null || date -j -f "%Y-%m-%dT%H:%M:%SZ" "$created" +%s 2>/dev/null)"
  epoch_now="$(date +%s)"
  echo $(( (epoch_now - epoch_created) / 86400 ))
}
