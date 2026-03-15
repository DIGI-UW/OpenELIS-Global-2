#!/usr/bin/env python3
"""
Validate which Playwright project(s) include a given spec file.

Usage:
  python .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foo.spec.ts
"""

from __future__ import annotations

import re
import sys
from fnmatch import fnmatch
from pathlib import Path


def usage() -> None:
    print(
        "Usage: python .ai/skills/playwright/scripts/validate-playwright-project.py "
        "<spec-path>"
    )


def normalize_spec_arg(spec_arg: str) -> str:
    spec = spec_arg.replace("\\", "/")
    return spec if spec.startswith("playwright/tests/") else f"playwright/tests/{spec}"


def extract_project_blocks(config_text: str) -> list[tuple[str, str]]:
    pattern = re.compile(
        r'name:\s*"([^"]+)"[\s\S]*?testMatch:\s*(\[[\s\S]*?\]|DEMO_TESTS|/[^/]+/)',
        re.MULTILINE,
    )
    return [(m.group(1), m.group(2)) for m in pattern.finditer(config_text)]


def extract_globs(config_text: str, block: str) -> list[str]:
    if block == "DEMO_TESTS":
        demo_match = re.search(
            r"const\s+DEMO_TESTS\s*=\s*\[([\s\S]*?)\];", config_text, re.MULTILINE
        )
        if not demo_match:
            return []
        block = demo_match.group(1)
    return re.findall(r'"([^"]+\*[^"]+|[^"]+\.spec\.ts)"', block)


def main() -> int:
    if len(sys.argv) != 2:
        usage()
        return 1

    repo_root = Path(__file__).resolve().parents[4]
    config_path = repo_root / "frontend" / "playwright.config.ts"
    if not config_path.exists():
        print(f"Error: Missing config at {config_path}")
        return 1

    spec = normalize_spec_arg(sys.argv[1])
    config_text = config_path.read_text(encoding="utf-8")
    project_blocks = extract_project_blocks(config_text)

    matches: list[str] = []
    for project_name, block in project_blocks:
        globs = extract_globs(config_text, block)
        for glob in globs:
            if fnmatch(spec, glob):
                matches.append(project_name)
                break

    print(f"Spec: {spec}")
    if matches:
        print("Matched projects:")
        for name in matches:
            print(f"  - {name}")
        return 0

    print("No matching project testMatch entry found.")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
