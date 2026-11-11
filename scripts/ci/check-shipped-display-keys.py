#!/usr/bin/env python3
"""Shipped reference data names display keys that exist in en.json (OGC-1507).

NCE categories and types and the dictionary entries OpenELIS ships in its own
configuration files carry a displayKey column. Their label is the translation of
that key, so a key missing from en.json would show as the bare key. This check
fails the build when a shipped file names a key en.json does not hold, or when
one list gives two values the same key.

    check-shipped-display-keys.py            check, exit 1 on a problem
    check-shipped-display-keys.py --list     print every key the shipped files use

The list is what an unused-key scan of en.json must count as used: these keys
appear in no source file, only in configuration data.
"""
import csv
import glob
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
EN_JSON = os.path.join(ROOT, "frontend", "src", "languages", "en.json")
SHIPPED = [
    "volume/configuration/backend/nce-categories/*.csv",
    "volume/configuration/backend/nce-types/*.csv",
    "volume/configuration/backend/dictionaries/*.csv",
    "src/main/resources/configuration/dictionaries/*.csv",
]


def shipped_keys():
    """(file, line, key, value identity) for every displayKey cell."""
    for pattern in SHIPPED:
        for path in sorted(glob.glob(os.path.join(ROOT, pattern))):
            with open(path, encoding="utf-8", newline="") as f:
                rows = [r for r in csv.reader(f)]
            if not rows or "displayKey" not in rows[0]:
                continue
            header = rows[0]
            key_at = header.index("displayKey")
            ident_at = [header.index(c) for c in ("category", "dictEntry", "name") if c in header]
            for number, row in enumerate(rows[1:], start=2):
                if not row or row[0].lstrip().startswith("#") or len(row) <= key_at:
                    continue
                key = row[key_at].strip()
                if key:
                    ident = tuple(row[i].strip() for i in ident_at if i < len(row))
                    yield os.path.relpath(path, ROOT), number, key, ident


def main():
    with open(EN_JSON, encoding="utf-8") as f:
        en = json.load(f)
    found = list(shipped_keys())
    if "--list" in sys.argv:
        for key in sorted({key for _, _, key, _ in found}):
            print(key)
        return 0
    problems = []
    owner = {}
    for path, line, key, ident in found:
        if key not in en:
            problems.append("%s:%d: display key %s is not in en.json" % (path, line, key))
        if key in owner and owner[key] != ident:
            problems.append("%s:%d: display key %s already names %s" % (path, line, key, " / ".join(owner[key])))
        owner.setdefault(key, ident)
    for problem in problems:
        print("::error::" + problem)
    if problems:
        return 1
    print("%d shipped display keys, all in en.json" % len(owner))
    return 0


if __name__ == "__main__":
    sys.exit(main())
