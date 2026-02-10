#!/usr/bin/env bash
# Resolve repeated plugins submodule conflicts during rebase by always
# using current develop HEAD for plugins, then continue until rebase finishes
# or a non-plugins conflict appears.
set -e
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

if ! [ -d .git/rebase-merge ] && ! [ -d .git/rebase-apply ]; then
  echo "Not in a rebase. Start a rebase first."
  exit 1
fi

while true; do
  # If we're not in a conflict state, try to continue
  UNMERGED=$(git diff --name-only --diff-filter=U 2>/dev/null || true)
  if [ -z "$UNMERGED" ]; then
    git rebase --continue
    exit 0
  fi

  # Check if the only conflict is plugins
  if [ "$UNMERGED" = "plugins" ]; then
    echo "Resolving plugins -> develop and continuing..."
    (cd plugins && git fetch origin develop 2>/dev/null; git checkout origin/develop 2>/dev/null || git checkout develop)
    git add plugins
    git rebase --continue
    continue
  fi

  echo "Conflicts other than 'plugins'. Resolve manually:"
  echo "$UNMERGED"
  exit 1
done
