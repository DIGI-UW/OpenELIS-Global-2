#!/usr/bin/env python3
"""
Install SpecKit slash commands to AI agent directories.

Copies command definitions from:
  - .specify/core/commands/ (base SpecKit commands)
  - .specify/oe/commands/   (OpenELIS extensions, appended)

Into AI-agent directories:
  - .cursor/commands/
  - .claude/commands/

Usage:
  python scripts/install-speckit-commands.py [--yes] [cursor|claude|all]

Examples:
  python scripts/install-speckit-commands.py           # Install to all (with prompt)
  python scripts/install-speckit-commands.py cursor    # Install to Cursor only
  python scripts/install-speckit-commands.py -y all    # Install without prompting

Cross-platform compatible (Windows, macOS, Linux). Requires Python 3.7+.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

# Minimum Python version check
if sys.version_info < (3, 7):
    sys.exit("Error: Python 3.7 or higher is required.")


def get_repo_root() -> Path:
    """Get repository root (parent of scripts/)."""
    return Path(__file__).resolve().parent.parent


def install_to(name: str, target_dir: Path, core_dir: Path, oe_dir: Path) -> int:
    """Install commands to a target directory. Returns count of installed commands."""
    print(f"-> Installing to {name}...")
    target_dir.mkdir(parents=True, exist_ok=True)

    count = 0
    for cmd_file in sorted(core_dir.glob("speckit.*.md")):
        dest = target_dir / cmd_file.name

        # Copy core command (explicit UTF-8 for Windows compatibility)
        content = cmd_file.read_text(encoding="utf-8")

        # Append OE overrides if they exist
        oe_file = oe_dir / cmd_file.name
        if oe_file.exists():
            content += "\n\n---\n\n## OpenELIS-Specific Requirements\n\n"
            content += oe_file.read_text(encoding="utf-8")

        dest.write_text(content, encoding="utf-8")
        count += 1

    print(f"   [OK] Installed {count} command(s) to {target_dir}")
    return count


def main():
    parser = argparse.ArgumentParser(
        description="Install SpecKit commands to AI agent directories"
    )
    parser.add_argument(
        "-y", "--yes", action="store_true", help="Skip confirmation prompt"
    )
    parser.add_argument(
        "target",
        nargs="?",
        default="all",
        choices=["cursor", "claude", "all"],
        help="Which agent(s) to install to (default: all)",
    )
    args = parser.parse_args()

    repo_root = get_repo_root()
    core_dir = repo_root / ".specify" / "core" / "commands"
    oe_dir = repo_root / ".specify" / "oe" / "commands"

    # Validate source
    if not core_dir.exists():
        print(f"Error: Core commands not found at {core_dir}", file=sys.stderr)
        sys.exit(1)

    cmd_files = list(core_dir.glob("speckit.*.md"))
    if not cmd_files:
        print(f"Error: No command files found in {core_dir}", file=sys.stderr)
        sys.exit(1)

    # Confirmation prompt
    if not args.yes:
        print(f"This will install {len(cmd_files)} SpecKit commands to:")
        if args.target in ("all", "cursor"):
            print("  - .cursor/commands/")
        if args.target in ("all", "claude"):
            print("  - .claude/commands/")
        print()

        response = input("Proceed? [y/N] ").strip().lower()
        if response not in ("y", "yes"):
            print("Cancelled.")
            sys.exit(0)

    # Install
    if args.target in ("all", "cursor"):
        install_to("Cursor", repo_root / ".cursor" / "commands", core_dir, oe_dir)
    if args.target in ("all", "claude"):
        install_to("Claude Code", repo_root / ".claude" / "commands", core_dir, oe_dir)

    # Summary
    print()
    print("Done! Available commands:")
    for cmd_file in sorted(cmd_files):
        print(f"  /{cmd_file.stem}")


if __name__ == "__main__":
    main()
