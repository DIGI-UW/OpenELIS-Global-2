#!/usr/bin/env python3
"""Preserve the native OpenELIS API path in the reporting UAT proxy."""

import argparse
from pathlib import Path
import re
from urllib.parse import urlsplit


def prepare(source: Path, destination: Path) -> None:
    if source.resolve() == destination.resolve():
        raise ValueError("Keep the original proxy configuration for rollback")
    original = source.read_text()
    blocks = list(re.finditer(
        r"location\s+(?:\^~\s+)?/api/OpenELIS-Global/\s*\{([^{}]*)\}", original
    ))
    if len(blocks) != 1:
        raise ValueError("Expected exactly one OpenELIS API proxy location")
    block = blocks[0]
    directives = list(re.finditer(r"proxy_pass\s+([^;\s]+)\s*;", block.group(1)))
    if len(directives) != 1:
        raise ValueError("Expected exactly one API upstream directive")
    directive = directives[0]
    uri = urlsplit(directive.group(1))
    if uri.scheme not in {"http", "https"} or not uri.netloc or uri.query or uri.fragment:
        raise ValueError("Unexpected upstream URI; inspect its routing before continuing")
    if uri.path != "/OpenELIS-Global/":
        raise ValueError("The API proxy no longer matches the known prefix-removal configuration")
    start = block.start(1) + directive.start(1)
    end = block.start(1) + directive.end(1)
    # With no URI component, nginx forwards the complete original request path.
    native = f"{uri.scheme}://{uri.netloc}"
    destination.write_text(original[:start] + native + original[end:])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    prepare(args.source, args.destination)
