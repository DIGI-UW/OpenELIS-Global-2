#!/usr/bin/env python3
"""Reject source strings that Transifex cannot upload.

Transifex's KEYVALUEJSON parser treats any value that starts with
`{var, plural,` (or select/selectordinal) as one pluralized entry: everything
up to the last `}` must be plural forms only, `one {...} other {...}`. A value
that opens with a plural and later contains a second one fails with
`parse_error: Invalid format of pluralized entry`, and the whole upload is
rejected. react-intl accepts such strings, so nothing else catches them.

Fix a flagged string by wrapping the whole sentence in one plural, or by
starting it with plain text.
"""

import json
import re
import sys

LEADING_PLURAL = re.compile(r"^\{\s*\w+\s*,\s*(plural|select|selectordinal)\s*,")
SELECTOR = re.compile(r"(=\d+|\w+)\s*")


def is_forms_only(body):
    i, n, forms = 0, len(body), 0
    while True:
        while i < n and body[i].isspace():
            i += 1
        if i == n:
            return forms > 0
        selector = SELECTOR.match(body, i)
        if not selector:
            return False
        i = selector.end()
        if i >= n or body[i] != "{":
            return False
        depth = 0
        while i < n:
            if body[i] == "{":
                depth += 1
            elif body[i] == "}":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        if depth != 0:
            return False
        i += 1
        forms += 1


def invalid_keys(messages):
    bad = []
    for key, value in messages.items():
        if not isinstance(value, str):
            continue
        match = LEADING_PLURAL.match(value)
        if match and not is_forms_only(value[match.end() : value.rfind("}")]):
            bad.append(key)
    return bad


def main(path):
    with open(path, encoding="utf-8") as f:
        bad = invalid_keys(json.load(f))
    for key in bad:
        print(
            f"::error file={path}::'{key}' starts with a plural but is not a "
            "single plural expression; Transifex rejects the upload. Wrap the "
            "whole sentence in one plural or start it with plain text."
        )
    if bad:
        return 1
    print(f"{path}: all leading plurals are Transifex-parseable")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "frontend/src/languages/en.json"))
