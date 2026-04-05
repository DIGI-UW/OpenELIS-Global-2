#!/bin/bash
# One-time setup: use .githooks for this repo so pre-commit runs format + lint.
# NOTE: Must be run in each worktree separately (git config is per-worktree).
set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

# Use absolute path so hooks work correctly in worktrees
HOOKS_PATH="$REPO_ROOT/.githooks"
git config core.hooksPath "$HOOKS_PATH"

echo "Hooks path set to: $HOOKS_PATH"
echo "Pre-commit will run spotless/prettier on future commits."

# Check if this is a worktree
if [ -f "$REPO_ROOT/.git" ]; then
    echo ""
    echo "Note: This is a git worktree. Hooks are configured for this worktree only."
fi
