#!/usr/bin/env python3
"""
Compatibility wrapper for legacy SpecKit command installation.

This delegates to scripts/install-agent-skills.py with --legacy-only so existing
automation and docs keep working while the project transitions to packaged skills.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


def main() -> None:
    repo_root = Path(__file__).resolve().parent.parent
    installer = repo_root / "scripts" / "install-agent-skills.py"
    if not installer.exists():
        sys.exit(f"Error: Missing generic installer at {installer}")

    passthrough_args = sys.argv[1:]
    command = [sys.executable, str(installer), "--legacy-only", *passthrough_args]
    raise SystemExit(subprocess.call(command))


if __name__ == "__main__":
    main()
