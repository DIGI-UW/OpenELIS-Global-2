#!/bin/bash
# One-time setup: use .githooks for this repo so pre-commit runs format + lint.
set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

# Git resolves this relative to the checkout running the hook. A shared absolute
# path would redirect every worktree to whichever checkout ran setup last.
HOOKS_PATH=".githooks"
git config core.hooksPath "$HOOKS_PATH"

echo "Hooks path set to: $HOOKS_PATH"
echo "Pre-commit will run spotless/prettier on future commits."
