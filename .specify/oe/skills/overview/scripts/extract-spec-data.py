#!/usr/bin/env python3
"""Extract canvas-data.json from a SpecKit feature directory.

Reads spec.md / plan.md / research.md / tasks.md / contracts/openapi.yaml
+ git log and emits a JSON file consumed by build-canvas.py.

Regex-based extraction (no markdown library dependency). Best-effort: when
a field can't be extracted, it's left empty and the canvas's hide-when-empty
rules omit the corresponding tab section.

Usage: extract-spec-data.py <SPEC_DIR>

See ../reference/data-extraction.md for the JSON schema + parser rules.
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Any


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def file_size_label(path: Path) -> str:
    if not path.exists():
        return ""
    size = path.stat().st_size
    if size < 1024:
        return f"{size}B"
    if size < 1024 * 1024:
        return f"~{round(size / 1024)}K"
    return f"~{round(size / (1024 * 1024), 1)}M"


def extract_meta(spec_dir: Path) -> dict[str, Any]:
    spec = read_text(spec_dir / "spec.md")
    research = read_text(spec_dir / "research.md")

    meta: dict[str, Any] = {
        "kicker": "Comprehensive Overview",
        "overviewLabel": "Why this exists",
    }

    # Title from first H1
    m = re.search(r"^# (.+)$", spec, re.MULTILINE)
    if m:
        title_full = m.group(1).strip()
        # Trim "Feature Specification: " prefix if present
        meta["title"] = re.sub(r"^Feature Specification:\s*", "", title_full)
    else:
        meta["title"] = spec_dir.name

    # Subtitle: try second H1 or first sentence of Overview
    sub = re.search(r"## Overview\s*\n+(.+?)(?:\.|\n)", spec, re.DOTALL)
    if sub:
        meta["subtitle"] = sub.group(1).strip().split(".")[0]

    # Issue/Jira
    jira = re.search(r"\*\*Issue\*\*:?\s*\[([^\]]+)\]\(([^)]+)\)", spec)
    if jira:
        meta["jira"] = jira.group(2)
        meta["jiraLabel"] = jira.group(1)

    # Status
    status = re.search(r"\*\*Status\*\*:?\s*([^\n]+)", spec)
    if status:
        meta["status"] = status.group(1).strip()

    # Overview section body (first paragraph)
    ov = re.search(r"## Overview\s*\n+(.+?)\n##", spec, re.DOTALL)
    if ov:
        meta["overview"] = ov.group(1).strip()

    # FRS pin from research.md (look for "SHA" + URL block)
    sha_m = re.search(r"`([0-9a-f]{7,40})`.*v\d+\.\d+.*\d{4}-\d{2}-\d{2}", research)
    if sha_m:
        meta["frsSha"] = sha_m.group(1)[:7]
    frs_url = re.search(r"https://github\.com/[^\s)]+/blob/[0-9a-f]+/[^\s)]+\.md", research)
    if frs_url:
        meta["frsUrl"] = frs_url.group(0)
    frs_ver = re.search(r"v(\d+\.\d+)\s*·\s*(\d{4}-\d{2}-\d{2})", research)
    if frs_ver:
        meta["frsVersionLine"] = f"v{frs_ver.group(1)} · {frs_ver.group(2)}"

    return meta


def extract_user_stories(spec_dir: Path) -> list[dict[str, Any]]:
    spec = read_text(spec_dir / "spec.md")
    stories = []
    # Pattern: ### User Story N — Title (Priority: PX)
    pat = re.compile(
        r"### User Story (\d+)\s*[—–-]\s*([^()\n]+?)\s*\((?:Priority:\s*)?(P\d)\)\s*\n+(.+?)(?=\n### User Story|\n### Edge Cases|\n## )",
        re.DOTALL,
    )
    for m in pat.finditer(spec):
        n, title, prio, body = m.group(1), m.group(2).strip(), m.group(3), m.group(4)
        # Summary: first paragraph after the heading (skip "Why this priority" header)
        first_p = re.search(r"\n([^\n#*].+?)(?=\n\n)", body, re.DOTALL)
        summary = first_p.group(1).strip() if first_p else ""
        # AC references inside the story
        acs = sorted(set(re.findall(r"AC-\d+", body)))
        # Surfaces (looks for "Surfaces" or "Independent Test")
        surfaces = []
        # Key takeaway (last paragraph)
        key = ""
        stories.append({
            "id": f"US{n}",
            "title": title,
            "priority": prio,
            "summary": summary,
            "acs": acs,
            "surfaces": surfaces,
            "key": key,
        })
    return stories


def extract_milestones(spec_dir: Path) -> list[dict[str, Any]]:
    plan = read_text(spec_dir / "plan.md")
    milestones = []
    # Find the milestone table (markdown table with ID column)
    # Look for the first table that mentions Branch + Suffix or M1 in first column
    table_m = re.search(
        r"\| ID[^\n]*\|[^\n]*Branch[^\n]*\|.*?\n\|[-\s|]+\|\n((?:\|[^\n]*\|\n)+)",
        plan,
        re.IGNORECASE,
    )
    if not table_m:
        return milestones
    rows = [r for r in table_m.group(1).split("\n") if r.startswith("|")]
    for row in rows:
        cells = [c.strip() for c in row.split("|")[1:-1]]
        if len(cells) < 6:
            continue
        # ID column may have **M2** formatting or [P] M2
        id_cell = re.sub(r"[*`]", "", cells[0]).strip()
        parallel = None
        if "[P]" in id_cell:
            parallel = id_cell.replace("[P]", "").strip()
            mid = parallel
        else:
            mid = id_cell
        milestones.append({
            "id": mid,
            "branch": re.sub(r"[`]", "", cells[1]).strip(),
            "scope": cells[2],
            "stories": cells[3],
            "depends": [d.strip() for d in cells[5].split(",") if d.strip() and d.strip() != "—"] if len(cells) > 5 else [],
            "state": "in-progress" if mid == "M1" else "queued",
            "pr": "—",
            "parallel": parallel,
        })
    return milestones


def extract_tables(spec_dir: Path) -> list[dict[str, Any]]:
    dm = read_text(spec_dir / "data-model.md")
    tables = []
    # Look for `### N.N \`table_name\`` headings under section 2 (new) or 3 (modified)
    pat = re.compile(r"### \d+\.\d+\s+`([a-z_]+)`(?:\s*\(([^)]+)\))?", re.IGNORECASE)
    for m in pat.finditer(dm):
        name = m.group(1)
        meta = m.group(2) or ""
        new = "new table" in meta.lower() or "new" in meta.lower()
        # Look for "Purpose" or first paragraph after the heading
        idx = m.end()
        snippet = dm[idx : idx + 600]
        purpose_m = re.search(r"\n\n([^\n#`].+?)(?:\n\n|\n```)", snippet, re.DOTALL)
        purpose = purpose_m.group(1).strip().replace("\n", " ") if purpose_m else ""
        tables.append({
            "name": name,
            "purpose": purpose[:200] + ("…" if len(purpose) > 200 else ""),
            "pk": "Integer id (sequence)" if new else "—",
            "new": new,
        })
    return tables


def extract_endpoints(spec_dir: Path) -> list[dict[str, Any]]:
    yml = read_text(spec_dir / "contracts" / "openapi.yaml")
    if not yml:
        return []
    endpoints = []
    # Naive YAML parsing: look for path entries + method blocks
    path_pat = re.compile(r"^  (/[^\s:]+):\s*$", re.MULTILINE)
    method_pat = re.compile(r"^    (get|post|put|patch|delete):\s*$", re.MULTILINE)
    tags_pat = re.compile(r"tags:\s*\[([^\]]+)\]")
    security_pat = re.compile(r"-\s*([a-z_]+):\s*\[\]")
    paths = list(path_pat.finditer(yml))
    for i, m in enumerate(paths):
        path = m.group(1)
        start = m.end()
        end = paths[i + 1].start() if i + 1 < len(paths) else len(yml)
        block = yml[start:end]
        for mm in method_pat.finditer(block):
            method = mm.group(1).upper()
            method_block = block[mm.end():mm.end() + 600]
            tags_m = tags_pat.search(method_block)
            tags = tags_m.group(1).strip() if tags_m else "Endpoints"
            sec_m = security_pat.search(method_block)
            scope = sec_m.group(1).replace("_", ".") if sec_m else ""
            endpoints.append({
                "method": method,
                "path": path,
                "scope": scope,
                "milestone": "",
                "who": tags,
            })
    return endpoints


def extract_clarifications(spec_dir: Path) -> list[dict[str, Any]]:
    spec = read_text(spec_dir / "spec.md")
    research = read_text(spec_dir / "research.md")
    clarifications = []
    # Look for Q1..Q4 resolutions in research.md sections
    pat = re.compile(r"### (Q\d+)\s*[—–-]\s*([^\n]+?)\n+(.+?)(?=\n###\s|\n##\s)", re.DOTALL)
    for m in pat.finditer(research):
        q = m.group(1)
        topic = m.group(2).strip()
        body = m.group(3)
        decision = re.search(r"\*\*(?:Decision|Resolution)[^*]*\*\*[:\s]*([^\n]+)", body)
        rationale = re.search(r"\*\*Rationale\*\*[:\s]*\n?([^\n]+(?:\n[^\n*].+?)*)", body)
        defer = re.search(r"\*?\*?(?:Deferred?|Deferred to)\*?\*?[:\s]*([^\n]+)", body)
        clarifications.append({
            "q": q,
            "topic": topic,
            "resolution": (decision.group(1).strip() if decision else "").rstrip("."),
            "rationale": (rationale.group(1).strip().replace("\n", " ")[:300] if rationale else ""),
            "defer": (defer.group(1).strip() if defer else ""),
        })
    return clarifications


def extract_divergences(spec_dir: Path) -> list[dict[str, Any]]:
    research = read_text(spec_dir / "research.md")
    divergences = []
    pat = re.compile(r"### Divergence (\d+)\s*[—–-]\s*([^\n]+)\n+(.+?)(?=\n### |\n## )", re.DOTALL)
    for m in pat.finditer(research):
        n = int(m.group(1))
        title = m.group(2).strip()
        body = m.group(3)
        frs = re.search(r"\*\*FRS markdown\*\*[^:]*:\s*([^\n]+(?:\n>[^\n]+)*)", body)
        decision = re.search(r"\*\*(?:Locked engineering decision|Resolution)\*\*[^:]*:\s*\n?([^\n]+(?:\n[^\n*].+?)*?)(?=\n\*\*|\n###)", body, re.DOTALL)
        principle = re.search(r"\*\*(?:Rationale|Principle|Stakeholder communication)\*\*[^:]*:\s*([^\n]+)", body)
        divergences.append({
            "n": n,
            "title": title,
            "from": (frs.group(1).strip()[:250] if frs else "").replace("\n>", " "),
            "to": (decision.group(1).strip().replace("\n", " ")[:400] if decision else ""),
            "principle": (principle.group(1).strip()[:200] if principle else ""),
        })
    return sorted(divergences, key=lambda d: d["n"])


def extract_spec_files(spec_dir: Path) -> list[dict[str, Any]]:
    files = []
    candidates = ["spec.md", "plan.md", "research.md", "data-model.md", "tasks.md", "quickstart.md"]
    for name in candidates:
        p = spec_dir / name
        if p.exists():
            files.append({"name": name, "size": file_size_label(p), "role": ""})
    # contracts/openapi.yaml
    c = spec_dir / "contracts" / "openapi.yaml"
    if c.exists():
        files.append({"name": "contracts/openapi.yaml", "size": file_size_label(c), "role": ""})
    # checklists/requirements.md
    cl = spec_dir / "checklists" / "requirements.md"
    if cl.exists():
        files.append({"name": "checklists/requirements.md", "size": file_size_label(cl), "role": ""})
    return files


def extract_commits(spec_dir: Path) -> list[dict[str, str]]:
    # Get recent commits touching this spec dir
    try:
        repo_root = Path(__file__).resolve().parents[5]  # .specify/oe/skills/overview/scripts/extract-spec-data.py → repo root
        # Try to find the base branch
        out = subprocess.run(
            ["git", "log", "--oneline", "-20", "--", str(spec_dir.relative_to(repo_root))],
            cwd=repo_root,
            capture_output=True,
            text=True,
            check=False,
        )
        commits = []
        for line in out.stdout.strip().split("\n"):
            if not line.strip():
                continue
            parts = line.split(" ", 1)
            if len(parts) == 2:
                commits.append({"sha": parts[0], "title": parts[1]})
        return commits
    except Exception:
        return []


def extract_acs(spec_dir: Path, stories: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Build AC × milestone × story coverage by cross-referencing stories and milestone scope."""
    seen: dict[str, dict[str, Any]] = {}
    for s in stories:
        for ac in s.get("acs", []):
            seen.setdefault(ac, {"ac": ac, "desc": "", "m": s.get("milestone", ""), "story": s["id"]})
    # Sort numerically by AC-N
    return sorted(seen.values(), key=lambda r: int(r["ac"].split("-")[1]))


def build_kpis(stories, milestones, tables, endpoints, acs) -> list[dict[str, Any]]:
    kpis = []
    if stories:
        kpis.append({"label": "User Stories", "value": len(stories), "tone": "sky", "icon": "Users", "sub": f"US1..US{len(stories)}"})
    if milestones:
        kpis.append({"label": "Milestones", "value": len(milestones), "tone": "purple", "icon": "GitBranch", "sub": " · ".join(m["id"] for m in milestones[:3]) + (" …" if len(milestones) > 3 else "")})
    if acs:
        kpis.append({"label": "Acceptance Criteria", "value": len(acs), "tone": "emerald", "icon": "CheckCircle2", "sub": "covered"})
    if endpoints:
        kpis.append({"label": "REST Endpoints", "value": len(endpoints), "tone": "amber", "icon": "Network", "sub": "OpenAPI 3.0"})
    if tables:
        new_tables = [t for t in tables if t.get("new")]
        kpis.append({"label": "New DB Tables", "value": len(new_tables), "tone": "rose", "icon": "Database", "sub": "Liquibase"})
    return kpis


def main():
    if len(sys.argv) < 2:
        sys.exit("usage: extract-spec-data.py <SPEC_DIR>")
    spec_dir = Path(sys.argv[1]).resolve()
    if not spec_dir.exists() or not (spec_dir / "spec.md").exists():
        sys.exit(f"error: {spec_dir} does not contain spec.md")

    print(f"-> Extracting from {spec_dir}…")
    meta = extract_meta(spec_dir)
    stories = extract_user_stories(spec_dir)
    milestones = extract_milestones(spec_dir)
    tables = extract_tables(spec_dir)
    endpoints = extract_endpoints(spec_dir)
    clarifications = extract_clarifications(spec_dir)
    divergences = extract_divergences(spec_dir)
    spec_files = extract_spec_files(spec_dir)
    commits = extract_commits(spec_dir)
    acs = extract_acs(spec_dir, stories)
    kpis = build_kpis(stories, milestones, tables, endpoints, acs)

    data = {
        "meta": meta,
        "kpis": kpis,
        "userStories": stories,
        "milestones": milestones,
        "tables": tables,
        "endpoints": endpoints,
        "clarifications": clarifications,
        "divergences": divergences,
        "acCoverage": acs,
        "specFiles": spec_files,
        "prCommits": commits,
        "demoSpecs": [],
    }

    out_dir = spec_dir / "canvas"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / "canvas-data.json"
    out_file.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"   [OK] wrote {out_file}")
    print(f"   stories={len(stories)} milestones={len(milestones)} tables={len(tables)} endpoints={len(endpoints)} clarifications={len(clarifications)} divergences={len(divergences)} files={len(spec_files)} commits={len(commits)} acs={len(acs)}")


if __name__ == "__main__":
    main()
