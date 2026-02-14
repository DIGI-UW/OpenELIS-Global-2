#!/bin/bash
# One-time setup: use .githooks for this repo so pre-commit runs format + lint.
set -e
cd "$(git rev-parse --show-toplevel)"
git config core.hooksPath .githooks
echo "Hooks path set to .githooks. Pre-commit will run on future commits."
