#!/usr/bin/env python3
"""Shared locale validation for GitHub and the full local CI runner."""
import argparse
import json
from pathlib import Path
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("all", "duplicates", "source"), default="all")
    parser.add_argument("--base", default="origin/develop")
    parser.add_argument("--branch", default="")
    args = parser.parse_args()
    errors = []
    if args.mode in ("all", "duplicates"):
        for path in sorted((ROOT / "frontend/src/languages").glob("*.json")):
            pairs = json.loads(path.read_text(), object_pairs_hook=lambda value: value)
            seen = set()
            duplicates = [key for key, _ in pairs if key in seen or seen.add(key)]
            if duplicates:
                errors.append(f"{path.relative_to(ROOT)}: duplicate keys: {duplicates[:5]}")
    if args.mode in ("all", "source") and args.branch != "chore/update-transifex":
        changed = subprocess.check_output(
            ["git", "-C", str(ROOT), "diff", "--name-only", args.base + "...HEAD", "--",
             "frontend/src/languages/"], text=True,
        ).splitlines()
        non_english = [path for path in changed if path.endswith(".json") and not path.endswith("/en.json")]
        if non_english:
            errors.append("Translations belong on Transifex; only en.json may change: " + ", ".join(non_english))
    if errors:
        sys.exit("\n".join(errors))
    print("Locale duplicate-key and translation ownership validation passed")


if __name__ == "__main__":
    main()
