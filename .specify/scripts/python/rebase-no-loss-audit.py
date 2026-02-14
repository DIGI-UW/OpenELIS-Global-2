#!/usr/bin/env python3
"""
Detect potential regressions introduced during rebase/merge.

This script compares a "before" ref (usually backup branch / pre-rebase HEAD)
and an "after" ref (usually rebased HEAD), both against a shared base ref.

It reports:
  - Files that were part of the before-branch delta vs base, but disappeared
    from the after-branch delta (potential dropped branch changes)
  - i18n JSON keys that existed before but are missing after

Use --fail-on-loss to return non-zero when potential losses are detected.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path
from typing import Dict, List, Set, Tuple


DEFAULT_I18N_FILES = [
    "frontend/src/languages/en.json",
    "frontend/src/languages/fr.json",
]


def run_git(args: List[str]) -> str:
    return subprocess.check_output(["git", *args], text=True).strip()


def delta_files(base_ref: str, ref: str) -> Set[str]:
    output = run_git(["diff", "--name-only", f"{base_ref}...{ref}"])
    if not output:
        return set()
    return {line.strip() for line in output.splitlines() if line.strip()}


def parse_json_at_ref(ref: str, file_path: str) -> Tuple[Dict[str, object] | None, str | None]:
    try:
        contents = run_git(["show", f"{ref}:{file_path}"])
    except subprocess.CalledProcessError:
        return None, "missing"
    try:
        parsed = json.loads(contents)
    except json.JSONDecodeError as exc:
        return None, f"invalid-json: {exc}"
    if not isinstance(parsed, dict):
        return None, "not-json-object"
    return parsed, None


def parse_allow_missing_key(values: List[str]) -> Dict[str, Set[str]]:
    allowed: Dict[str, Set[str]] = {}
    for value in values:
        if ":" not in value:
            raise ValueError(
                f"Invalid --allow-missing-key '{value}'. Expected '<file-path>:<key>'."
            )
        file_path, key = value.split(":", 1)
        file_path = file_path.strip()
        key = key.strip()
        if not file_path or not key:
            raise ValueError(
                f"Invalid --allow-missing-key '{value}'. Expected '<file-path>:<key>'."
            )
        allowed.setdefault(file_path, set()).add(key)
    return allowed


def print_section(title: str) -> None:
    print("")
    print(f"=== {title} ===")


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit rebase/merge for potential losses.")
    parser.add_argument("--base", required=True, help="Base ref (e.g. origin/develop)")
    parser.add_argument(
        "--before",
        required=True,
        help="Pre-rebase ref (e.g. backup/branch/timestamp-sha)",
    )
    parser.add_argument("--after", required=True, help="Post-rebase ref (e.g. HEAD)")
    parser.add_argument(
        "--i18n-file",
        action="append",
        default=None,
        help="Localization JSON path to compare key sets. Repeat for multiple files.",
    )
    parser.add_argument(
        "--allow-dropped-file",
        action="append",
        default=[],
        help="File path allowed to drop from branch delta. Repeat as needed.",
    )
    parser.add_argument(
        "--allow-missing-key",
        action="append",
        default=[],
        help="Allowed missing i18n key in '<file-path>:<key>' format. Repeat as needed.",
    )
    parser.add_argument(
        "--fail-on-loss",
        action="store_true",
        help="Exit non-zero if unapproved dropped files or missing keys are detected.",
    )
    parser.add_argument(
        "--output-json",
        help="Optional path to write machine-readable JSON report.",
    )
    args = parser.parse_args()

    i18n_files = args.i18n_file if args.i18n_file else DEFAULT_I18N_FILES
    allowed_dropped_files = set(args.allow_dropped_file)
    allowed_missing_keys = parse_allow_missing_key(args.allow_missing_key)

    before_delta = delta_files(args.base, args.before)
    after_delta = delta_files(args.base, args.after)

    dropped_delta_files = sorted(before_delta - after_delta)
    new_delta_files = sorted(after_delta - before_delta)
    blocked_dropped_files = sorted(
        file_path for file_path in dropped_delta_files if file_path not in allowed_dropped_files
    )

    i18n_report = []
    blocked_missing_keys: List[str] = []

    for file_path in i18n_files:
        before_json, before_err = parse_json_at_ref(args.before, file_path)
        after_json, after_err = parse_json_at_ref(args.after, file_path)

        entry = {
            "file": file_path,
            "before_error": before_err,
            "after_error": after_err,
            "before_key_count": 0,
            "after_key_count": 0,
            "missing_keys": [],
            "added_keys": [],
        }

        if before_json is not None:
            before_keys = set(before_json.keys())
            entry["before_key_count"] = len(before_keys)
        else:
            before_keys = set()

        if after_json is not None:
            after_keys = set(after_json.keys())
            entry["after_key_count"] = len(after_keys)
        else:
            after_keys = set()

        missing_keys = sorted(before_keys - after_keys)
        added_keys = sorted(after_keys - before_keys)
        entry["missing_keys"] = missing_keys
        entry["added_keys"] = added_keys
        i18n_report.append(entry)

        allowed_for_file = allowed_missing_keys.get(file_path, set())
        for key in missing_keys:
            if key not in allowed_for_file:
                blocked_missing_keys.append(f"{file_path}:{key}")

    print("Rebase No-Loss Audit")
    print("--------------------")
    print(f"Base ref   : {args.base}")
    print(f"Before ref : {args.before}")
    print(f"After ref  : {args.after}")

    print_section("Delta file comparison (vs base)")
    print(f"Before delta files: {len(before_delta)}")
    print(f"After delta files : {len(after_delta)}")
    print(f"Dropped delta files: {len(dropped_delta_files)}")
    print(f"New delta files    : {len(new_delta_files)}")

    if dropped_delta_files:
        print("")
        print("Dropped delta files:")
        for file_path in dropped_delta_files:
            marker = " (allowed)" if file_path in allowed_dropped_files else ""
            print(f"  - {file_path}{marker}")

    if new_delta_files:
        print("")
        print("New delta files:")
        for file_path in new_delta_files:
            print(f"  + {file_path}")

    print_section("i18n key comparison")
    for entry in i18n_report:
        print(
            f"{entry['file']}: before={entry['before_key_count']} after={entry['after_key_count']} "
            f"missing={len(entry['missing_keys'])} added={len(entry['added_keys'])}"
        )
        if entry["before_error"] or entry["after_error"]:
            print(
                f"  ! errors: before={entry['before_error'] or 'none'} "
                f"after={entry['after_error'] or 'none'}"
            )
        if entry["missing_keys"]:
            print("  Missing keys:")
            allowed_for_file = allowed_missing_keys.get(entry["file"], set())
            for key in entry["missing_keys"]:
                marker = " (allowed)" if key in allowed_for_file else ""
                print(f"    - {key}{marker}")

    report = {
        "base": args.base,
        "before": args.before,
        "after": args.after,
        "before_delta_count": len(before_delta),
        "after_delta_count": len(after_delta),
        "dropped_delta_files": dropped_delta_files,
        "new_delta_files": new_delta_files,
        "blocked_dropped_files": blocked_dropped_files,
        "i18n": i18n_report,
        "blocked_missing_keys": blocked_missing_keys,
    }

    if args.output_json:
        output_path = Path(args.output_json)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print("")
        print(f"Wrote JSON report: {output_path}")

    blocked = bool(blocked_dropped_files or blocked_missing_keys)
    if blocked:
        print("")
        print("AUDIT RESULT: POTENTIAL LOSS DETECTED")
        print(f"  Unapproved dropped files: {len(blocked_dropped_files)}")
        print(f"  Unapproved missing i18n keys: {len(blocked_missing_keys)}")
        if args.fail_on_loss:
            return 2
    else:
        print("")
        print("AUDIT RESULT: OK (no unapproved losses found)")

    return 0


if __name__ == "__main__":
    sys.exit(main())
