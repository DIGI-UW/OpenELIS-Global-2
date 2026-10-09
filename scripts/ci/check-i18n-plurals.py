#!/usr/bin/env python3
"""Reject en.json values Transifex cannot parse.

Transifex KEYVALUEJSON treats a value that opens with an ICU plural
(`{count, plural, ...}`) as a single pluralized entry whose body runs to the
last closing brace in the value. Plain text after the plural block is fine;
a second plural or any later `{placeholder}` fails the upload of the entire
file, which breaks every later push to develop. react-intl accepts these
values, so unit tests do not catch them.
"""
import json
import pathlib
import re
import sys

LEADING_PLURAL = re.compile(r"^\{\s*\w+\s*,\s*plural\s*,")


def plural_block_end(value):
    depth = 0
    for index, char in enumerate(value):
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return index
    return None


def offending_keys(messages):
    for key, value in messages.items():
        if not isinstance(value, str) or not LEADING_PLURAL.match(value):
            continue
        end = plural_block_end(value)
        if end is None or "{" in value[end + 1 :] or "}" in value[end + 1 :]:
            yield key, value


def main(paths):
    errors = []
    for path in paths:
        messages = json.loads(pathlib.Path(path).read_text())
        for key, value in offending_keys(messages):
            errors.append(
                f"{path}: '{key}' opens with a plural and has braces after "
                "it; Transifex rejects the whole file. Start the value with "
                "plain text, or split it into one key per plural."
            )
    for error in errors:
        print(f"::error::{error}")
    if errors:
        return 1
    print("i18n plural format OK for Transifex")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:] or ["frontend/src/languages/en.json"]))
