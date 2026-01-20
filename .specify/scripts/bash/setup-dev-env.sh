#!/usr/bin/env bash
#
# Setup SpecKit tooling for this repo (bash)
#
# Usage: ./setup-dev-env.sh [--yes|-y] [cursor|claude|all]
#
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

REPO_ROOT=$(get_repo_root)
COMMANDS_DIR="$REPO_ROOT/.specify/core/commands"

if [[ ! -d "$COMMANDS_DIR" ]]; then
    echo "Error: $COMMANDS_DIR not found. Ensure .specify/core/commands exists." >&2
    exit 1
fi

TARGET="all"
SKIP_CONFIRM=""

for arg in "$@"; do
    case "$arg" in
        --yes|-y) SKIP_CONFIRM="--yes" ;;
        cursor|claude|all) TARGET="$arg" ;;
    esac
done

echo "SpecKit setup (bash)"
echo "Repo: $REPO_ROOT"
echo "Installing slash commands to: $TARGET"
echo ""

"$SCRIPT_DIR/install-commands.sh" ${SKIP_CONFIRM} "$TARGET"

echo ""
echo "Next steps:"
echo "  - Use /speckit.specify to create a new feature"
echo "  - Use /speckit.plan and /speckit.tasks for planning"
echo "  - If not using git, set SPECIFY_FEATURE=<NNN-feature-name>"
