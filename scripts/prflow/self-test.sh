#!/usr/bin/env bash
# Static guards so PRFlow regressions fail in CI before merge.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "== bash -n =="
for f in "${ROOT}"/*.sh; do
  echo "  $f"
  bash -n "$f"
done

echo "== CRLF check =="
if grep -r $'\r' "${ROOT}/"; then
  echo "ERROR: CRLF in scripts/prflow" >&2
  exit 1
fi

echo "== gh pr checks field guards =="
if grep -E 'gh pr checks.*conclusion' "${ROOT}/common.sh"; then
  echo "ERROR: gh pr checks must not request invalid field 'conclusion'" >&2
  exit 1
fi
if grep -E "gh pr checks.*2>/dev/null" "${ROOT}/common.sh"; then
  echo "ERROR: do not swallow gh pr checks errors" >&2
  exit 1
fi
if ! grep -q 'bucket' "${ROOT}/common.sh"; then
  echo "ERROR: ci_all_success must use bucket field" >&2
  exit 1
fi

echo "== digest batching guard =="
if grep -q 'score_pr' "${ROOT}/digest.sh"; then
  echo "ERROR: digest.sh must not use per-PR score_pr loop" >&2
  exit 1
fi
if ! grep -q 'gh pr list' "${ROOT}/digest.sh"; then
  echo "ERROR: digest.sh must batch via gh pr list" >&2
  exit 1
fi

echo "== title sanitization guard =="
if ! grep -Fq 'gsub("\\|"' "${ROOT}/digest.sh"; then
  echo "ERROR: digest.sh must sanitize pipe characters in titles" >&2
  exit 1
fi

echo "All PRFlow self-tests passed."
