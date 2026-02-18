#!/usr/bin/env python3
"""
Detect duplicate keys in JSON objects.

Why: JSON allows duplicate keys syntactically; most parsers keep the *last* value silently.
This script preserves key order and reports duplicates so CI failures and silent overwrites
can be caught early (e.g., i18n message catalogs).
"""

from __future__ import annotations

import argparse
import collections
import json
import sys
from pathlib import Path


def load_pairs(path: Path) -> list[tuple[str, object]]:
    # object_pairs_hook receives a list of (key, value) tuples for each object.
    # We return that list so callers can count duplicates while preserving order.
    with path.open("r", encoding="utf-8") as f:
        return json.load(f, object_pairs_hook=lambda pairs: pairs)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        description="Report duplicate keys in JSON objects (preserving order).",
    )
    parser.add_argument("files", nargs="+", help="JSON files to scan")
    parser.add_argument(
        "--max-keys",
        type=int,
        default=20,
        help="Maximum duplicate keys to print per file (default: 20)",
    )
    args = parser.parse_args(argv)

    any_dupes = False
    for raw in args.files:
        path = Path(raw)
        if not path.exists():
            # Be permissive: missing files are not an error for multi-locale setups.
            print(f"{path}: missing (skipped)")
            continue

        try:
            pairs = load_pairs(path)
        except json.JSONDecodeError as e:
            print(f"{path}: invalid JSON: {e}", file=sys.stderr)
            return 2

        keys = [k for (k, _v) in pairs]
        counts = collections.Counter(keys)
        dupes = {k: c for k, c in counts.items() if c > 1}

        if not dupes:
            print(f"{path}: clean ({len(counts)} unique keys)")
            continue

        any_dupes = True
        print(f"{path}: {len(dupes)} duplicate keys found")
        for k, c in sorted(dupes.items())[: args.max_keys]:
            print(f"  {k} ({c}x)")
        remaining = len(dupes) - min(len(dupes), args.max_keys)
        if remaining:
            print(f"  ... and {remaining} more")

    return 1 if any_dupes else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

