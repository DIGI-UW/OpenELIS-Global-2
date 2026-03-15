#!/usr/bin/env python3
"""
Install AI command/skill assets for supported agent runtimes.

This script is the generic installer entrypoint and supports two sources:

1) Legacy SpecKit/OpenELIS command sources:
   - .specify/core/commands/
   - .specify/oe/commands/
2) Packaged skills:
   - .ai/skills/<skill-name>/

Outputs are generated into agent runtime folders:
  - .cursor/commands/
  - .claude/commands/
  - .cursor/skills/
  - .claude/skills/
"""

from __future__ import annotations

import argparse
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path


if sys.version_info < (3, 9):
    sys.exit("Error: Python 3.9 or higher is required.")


INJECTION_POINTS = {
    "speckit.tasks.md": "## Task Generation Rules",
    "speckit.implement.md": "## User Input",
}

OE_HEADER = "\n\n## OpenELIS-Specific Requirements\n\n"


@dataclass(frozen=True)
class InstallTarget:
    name: str
    commands_dir: Path
    skills_dir: Path


def get_repo_root() -> Path:
    return Path(__file__).resolve().parent.parent


def merge_oe_content(core_content: str, oe_content: str, filename: str) -> str:
    injection_point = INJECTION_POINTS.get(filename)
    if injection_point and injection_point in core_content:
        before, after = core_content.split(injection_point, 1)
        return before + OE_HEADER + oe_content + "\n\n" + injection_point + after
    return core_content + "\n\n---" + OE_HEADER + oe_content


def install_legacy_commands(
    commands_dir: Path, core_dir: Path, oe_dir: Path
) -> set[str]:
    installed_names: set[str] = set()
    commands_dir.mkdir(parents=True, exist_ok=True)

    for cmd_file in sorted(core_dir.glob("speckit.*.md")):
        content = cmd_file.read_text(encoding="utf-8")
        oe_file = oe_dir / cmd_file.name
        if oe_file.exists():
            content = merge_oe_content(
                content, oe_file.read_text(encoding="utf-8"), cmd_file.name
            )
        (commands_dir / cmd_file.name).write_text(content, encoding="utf-8")
        installed_names.add(cmd_file.name)

    if oe_dir.exists():
        for oe_file in sorted(oe_dir.glob("*.md")):
            if oe_file.name in installed_names or oe_file.name.startswith("speckit."):
                continue
            (commands_dir / oe_file.name).write_text(
                oe_file.read_text(encoding="utf-8"), encoding="utf-8"
            )
            installed_names.add(oe_file.name)

    return installed_names


def install_packaged_skill_commands(
    commands_dir: Path, skills_root: Path, installed_names: set[str]
) -> tuple[set[str], set[str]]:
    command_names: set[str] = set()
    skill_names: set[str] = set()

    if not skills_root.exists():
        return command_names, skill_names

    for skill_dir in sorted([p for p in skills_root.iterdir() if p.is_dir()]):
        skill_md = skill_dir / "SKILL.md"
        commands_subdir = skill_dir / "commands"
        if not skill_md.exists() or not commands_subdir.exists():
            continue
        skill_names.add(skill_dir.name)

        for command_file in sorted(commands_subdir.glob("*.md")):
            command_name = command_file.name
            if command_name in installed_names:
                sys.exit(
                    f"Error: packaged skill command '{command_name}' collides "
                    "with an existing command name."
                )
            (commands_dir / command_name).write_text(
                command_file.read_text(encoding="utf-8"),
                encoding="utf-8",
            )
            installed_names.add(command_name)
            command_names.add(command_name)

    return command_names, skill_names


def install_packaged_skill_sources(skills_dir: Path, skills_root: Path) -> set[str]:
    installed: set[str] = set()
    skills_dir.mkdir(parents=True, exist_ok=True)

    if not skills_root.exists():
        return installed

    for skill_dir in sorted([p for p in skills_root.iterdir() if p.is_dir()]):
        if not (skill_dir / "SKILL.md").exists():
            continue
        destination = skills_dir / skill_dir.name
        if destination.exists():
            shutil.rmtree(destination)
        shutil.copytree(skill_dir, destination)
        installed.add(skill_dir.name)
    return installed


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Install packaged AI skills and legacy commands"
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
    parser.add_argument(
        "--legacy-only",
        action="store_true",
        help="Install only legacy SpecKit/OE commands (no packaged skill sources).",
    )
    return parser.parse_args()


def build_targets(repo_root: Path, target: str) -> list[InstallTarget]:
    targets: list[InstallTarget] = []
    if target in ("all", "cursor"):
        targets.append(
            InstallTarget(
                name="Cursor",
                commands_dir=repo_root / ".cursor" / "commands",
                skills_dir=repo_root / ".cursor" / "skills",
            )
        )
    if target in ("all", "claude"):
        targets.append(
            InstallTarget(
                name="Claude Code",
                commands_dir=repo_root / ".claude" / "commands",
                skills_dir=repo_root / ".claude" / "skills",
            )
        )
    return targets


def main() -> None:
    args = parse_args()
    repo_root = get_repo_root()
    core_dir = repo_root / ".specify" / "core" / "commands"
    oe_dir = repo_root / ".specify" / "oe" / "commands"
    packaged_skills_root = repo_root / ".ai" / "skills"
    targets = build_targets(repo_root, args.target)

    if not core_dir.exists():
        sys.exit(f"Error: Legacy core commands not found at {core_dir}")

    if not args.yes:
        print("This will install AI command assets to:")
        for t in targets:
            print(f"  - {t.commands_dir}")
            if not args.legacy_only:
                print(f"  - {t.skills_dir}")
        print()
        response = input("Proceed? [y/N] ").strip().lower()
        if response not in ("y", "yes"):
            print("Cancelled.")
            sys.exit(0)

    total_commands = 0
    total_skills = 0
    for target in targets:
        print(f"-> Installing to {target.name}...")
        installed_names = install_legacy_commands(target.commands_dir, core_dir, oe_dir)
        packaged_commands: set[str] = set()
        packaged_skills: set[str] = set()

        if not args.legacy_only:
            packaged_commands, _ = install_packaged_skill_commands(
                target.commands_dir, packaged_skills_root, installed_names
            )
            packaged_skills = install_packaged_skill_sources(
                target.skills_dir, packaged_skills_root
            )

        print(
            f"   [OK] Commands: {len(installed_names)} "
            f"(legacy {len(installed_names) - len(packaged_commands)}, "
            f"packaged {len(packaged_commands)})"
        )
        if not args.legacy_only:
            print(f"   [OK] Packaged skills: {len(packaged_skills)}")

        total_commands += len(installed_names)
        total_skills += len(packaged_skills)

    print("\nDone!")
    print(f"Total commands installed: {total_commands}")
    if not args.legacy_only:
        print(f"Total packaged skills installed: {total_skills}")


if __name__ == "__main__":
    main()
