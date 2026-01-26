#!/usr/bin/env bash
#
# Install SpecKit slash commands to AI agent directories
#
# This script compiles slash command definitions from:
# - .specify/core/commands/ (upstream SpecKit)
# - .specify/oe/commands/ (OpenELIS extensions)
# into AI-agent-specific directories, following the
# same pattern used by GitHub Spec-Kit (https://github.com/github/spec-kit)
#
# Supported AI Agents:
#   cursor  - Cursor IDE          → .cursor/commands/
#   claude  - Claude Code CLI     → .claude/commands/
#   copilot - GitHub Copilot      → .github/copilot-instructions.md (future)
#
# Usage: install-commands.sh [--yes|-y] [cursor|claude|all]
#
# Options:
#   --yes, -y    Skip confirmation prompt (for automation)
#
# Examples:
#   ./install-commands.sh          # Install to all (with confirmation)
#   ./install-commands.sh cursor   # Install to Cursor only
#   ./install-commands.sh claude   # Install to Claude Code only
#   ./install-commands.sh -y all   # Install to all without prompting

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

REPO_ROOT=$(get_repo_root)
CORE_DIR="$REPO_ROOT/.specify/core/commands"
OE_DIR="$REPO_ROOT/.specify/oe/commands"
SKIP_CONFIRM=false
TARGET="all"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --yes|-y)
            SKIP_CONFIRM=true
            shift
            ;;
        cursor|claude|all)
            TARGET="$1"
            shift
            ;;
        *)
            echo "Usage: $0 [--yes|-y] [cursor|claude|all]" >&2
            exit 1
            ;;
    esac
done

# Validate source
if [[ ! -d "$CORE_DIR" ]]; then
    echo "Error: Core commands not found at $CORE_DIR" >&2
    echo "Ensure .specify/core/commands/ exists" >&2
    exit 1
fi

shopt -s nullglob
CMD_FILES=("$CORE_DIR"/speckit.*.md)
CMD_COUNT=${#CMD_FILES[@]}

# Show warning and get confirmation
show_warning() {
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║             SpecKit Command Installation                         ║"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    echo ""
    echo "This script will OVERWRITE existing slash commands in your AI agent"
    echo "directories with the latest compiled versions from:"
    echo "  - .specify/core/commands/"
    echo "  - .specify/oe/commands/ (if present)"
    echo ""
    echo "Core: $CORE_DIR"
    echo "OE:   $OE_DIR"
    echo "Commands to install: $CMD_COUNT"
    echo ""
    echo "Target directories:"
    case "$TARGET" in
        cursor) echo "  • .cursor/commands/" ;;
        claude) echo "  • .claude/commands/" ;;
        all)
            echo "  • .cursor/commands/"
            echo "  • .claude/commands/"
            ;;
    esac
    echo ""
    echo "⚠️  Any local modifications to these command files will be lost!"
    echo ""
}

if [[ "$SKIP_CONFIRM" != "true" ]]; then
    show_warning
    read -p "Do you want to proceed? [y/N] " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Installation cancelled."
        exit 0
    fi
    echo ""
fi

install_commands() {
    local name="$1" dir="$2"

    echo "→ Installing to $name..."
    mkdir -p "$dir"

    local inject_header=$'\n\n## OpenELIS-Specific Requirements\n\n'
    local append_header=$'\n\n---\n\n## OpenELIS-Specific Requirements\n\n'

    local count=0
    for f in "${CMD_FILES[@]}"; do
        [[ -f "$f" ]] || continue
        local base
        base=$(basename "$f")

        local core_content
        core_content=$(<"$f")
        # Path rewrites: upstream paths → project .specify/ paths
        # Use placeholder to avoid double-rewrite of /templates/ → .specify/templates/ → .specify/.specify/templates/
        core_content=${core_content//\/templates\//__SPECIFY_TEMPLATES__}
        core_content=${core_content//templates\//.specify/templates/}
        core_content=${core_content//__SPECIFY_TEMPLATES__/.specify/templates/}
        core_content=${core_content//scripts\//.specify/scripts/}
        core_content=${core_content//\/memory\//.specify/memory/}

        local merged_content="$core_content"
        local oe_file="$OE_DIR/$base"
        if [[ -f "$oe_file" ]]; then
            local oe_content
            oe_content=$(<"$oe_file")

            # Injection rules (preferred for commands where OE overrides core guidance):
            # - tasks: inject before "## Task Generation Rules"
            # - implement: inject before "## User Input"
            if [[ "$base" == "speckit.tasks.md" ]]; then
                local needle="## Task Generation Rules"
                if [[ "$merged_content" == *"$needle"* ]]; then
                    local before=${merged_content%%"$needle"*}
                    local after=${merged_content#*"$needle"}
                    merged_content="${before}${inject_header}${oe_content}"$'\n\n'"${needle}${after}"
                else
                    merged_content+="${append_header}${oe_content}"
                fi
            elif [[ "$base" == "speckit.implement.md" ]]; then
                local needle="## User Input"
                if [[ "$merged_content" == *"$needle"* ]]; then
                    local before=${merged_content%%"$needle"*}
                    local after=${merged_content#*"$needle"}
                    merged_content="${before}${inject_header}${oe_content}"$'\n\n'"${needle}${after}"
                else
                    merged_content+="${append_header}${oe_content}"
                fi
            else
                # Default: append OE extension at end.
                merged_content+="${append_header}${oe_content}"
            fi
        fi

        printf "%s" "$merged_content" > "$dir/$base"
        count=$((count + 1))
    done

    echo "  ✓ Installed $count command(s) to $dir"
}

case "$TARGET" in
    cursor)
        install_commands "Cursor" "$REPO_ROOT/.cursor/commands"
        ;;
    claude)
        install_commands "Claude Code" "$REPO_ROOT/.claude/commands"
        ;;
    all)
        install_commands "Cursor" "$REPO_ROOT/.cursor/commands"
        install_commands "Claude Code" "$REPO_ROOT/.claude/commands"
        ;;
esac

echo ""
echo "Commands installed! Available slash commands:"
printf "%s\n" "${CMD_FILES[@]}" | xargs -I{} basename {} .md | sed 's/^/  \//'
echo ""
echo "To use: Type /<command> in your AI agent (e.g., /speckit.specify)"
