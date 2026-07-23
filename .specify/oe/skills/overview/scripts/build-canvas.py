#!/usr/bin/env python3
"""Assemble overview.html from canvas.html.template + canvas-data.json.

Reads:
  - <SPEC_DIR>/canvas/canvas-data.json (produced by extract-spec-data.py)
  - templates/canvas.html.template     (parameterized HTML)

Writes:
  - <SPEC_DIR>/canvas/overview.html    (single self-contained HTML file)

Usage: build-canvas.py <SPEC_DIR>
"""
from __future__ import annotations

import json
import sys
from pathlib import Path


def main():
    if len(sys.argv) < 2:
        sys.exit("usage: build-canvas.py <SPEC_DIR>")
    spec_dir = Path(sys.argv[1]).resolve()
    canvas_dir = spec_dir / "canvas"
    data_file = canvas_dir / "canvas-data.json"
    if not data_file.exists():
        sys.exit(f"error: {data_file} not found. Run extract-spec-data.py first.")

    template_file = Path(__file__).resolve().parent.parent / "templates" / "canvas.html.template"
    if not template_file.exists():
        sys.exit(f"error: template not found at {template_file}")

    data = json.loads(data_file.read_text(encoding="utf-8"))
    template = template_file.read_text(encoding="utf-8")

    # Map placeholder names → JSON values
    subs = {
        "META_JSON": json.dumps(data.get("meta", {}), ensure_ascii=False),
        "META_TITLE": data.get("meta", {}).get("title", "Spec Overview"),
        "KPIS_JSON": json.dumps(data.get("kpis", []), ensure_ascii=False),
        "USER_STORIES_JSON": json.dumps(data.get("userStories", []), ensure_ascii=False),
        "MILESTONES_JSON": json.dumps(data.get("milestones", []), ensure_ascii=False),
        "TABLES_JSON": json.dumps(data.get("tables", []), ensure_ascii=False),
        "ENDPOINTS_JSON": json.dumps(data.get("endpoints", []), ensure_ascii=False),
        "CLARIFICATIONS_JSON": json.dumps(data.get("clarifications", []), ensure_ascii=False),
        "DIVERGENCES_JSON": json.dumps(data.get("divergences", []), ensure_ascii=False),
        "AC_COVERAGE_JSON": json.dumps(data.get("acCoverage", []), ensure_ascii=False),
        "SPEC_FILES_JSON": json.dumps(data.get("specFiles", []), ensure_ascii=False),
        "PR_COMMITS_JSON": json.dumps(data.get("prCommits", []), ensure_ascii=False),
        "DEMO_SPECS_JSON": json.dumps(data.get("demoSpecs", []), ensure_ascii=False),
    }

    out = template
    for key, value in subs.items():
        out = out.replace("{{" + key + "}}", value)

    out_file = canvas_dir / "overview.html"
    out_file.write_text(out, encoding="utf-8")
    print(f"-> wrote {out_file}")
    print(f"   open in browser: file://{out_file}")
    print(f"   after PR merge, deployed at:")
    repo_specs_path = None
    for parent in spec_dir.parents:
        if parent.name == "specs":
            repo_specs_path = spec_dir.relative_to(parent.parent)
            break
    if repo_specs_path:
        print(f"   https://digi-uw.github.io/OpenELIS-Global-2/{repo_specs_path}/canvas/overview.html")


if __name__ == "__main__":
    main()
