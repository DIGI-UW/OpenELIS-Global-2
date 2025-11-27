#!/usr/bin/env bash
#
# Install SpecKit slash commands to AI agent directories
#
# This script syncs slash command definitions from the canonical source
# (.specify/commands/) to AI-agent-specific directories, following the
# same pattern used by GitHub Spec-Kit (https://github.com/github/spec-kit)
#
# Supported AI Agents:
#   cursor  - Cursor IDE          → .cursor/commands/
#   claude  - Claude Code CLI     → .claude/commands/
#   copilot - GitHub Copilot      → .github/copilot-instructions.md (future)
#
# Usage: install-commands.sh [cursor|claude|all]
#
# Examples:
#   ./install-commands.sh          # Install to all supported agents
#   ./install-commands.sh cursor   # Install to Cursor only
#   ./install-commands.sh claude   # Install to Claude Code only

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

REPO_ROOT=$(get_repo_root)
SOURCE_DIR="$REPO_ROOT/.specify/commands"
TARGET="${1:-all}"

# Validate source
if [[ ! -d "$SOURCE_DIR" ]]; then
    echo "Error: Source commands not found at $SOURCE_DIR" >&2
    echo "Run 'specify init' first or ensure .specify/commands/ exists" >&2
    exit 1
fi

install_commands() {
    local name="$1" dir="$2"

    echo "→ Installing to $name..."
    mkdir -p "$dir"

    local count=0
    for f in "$SOURCE_DIR"/speckit.*.md; do
        [[ -f "$f" ]] || continue
        cp "$f" "$dir/"
        count=$((count + 1))
    done

    echo "  ✓ Installed $count command(s) to $dir"
}

case "$TARGET" in
    cursor) install_commands "Cursor" "$REPO_ROOT/.cursor/commands" ;;
    claude) install_commands "Claude Code" "$REPO_ROOT/.claude/commands" ;;
    all)
        install_commands "Cursor" "$REPO_ROOT/.cursor/commands"
        install_commands "Claude Code" "$REPO_ROOT/.claude/commands"
        ;;
    *)
        echo "Usage: $0 [cursor|claude|all]" >&2
        exit 1
        ;;
esac

echo ""
echo "Commands installed! Available slash commands:"
ls "$SOURCE_DIR"/speckit.*.md 2>/dev/null | xargs -I{} basename {} .md | sed 's/^/  \//'
echo ""
echo "To use: Type /<command> in your AI agent (e.g., /speckit.specify)"
