---
description:
  Generate or refresh an interactive overview canvas for a SpecKit feature
  directory. Reads spec.md / plan.md / research.md / tasks.md / contracts/,
  extracts canvas data, assembles a self-contained HTML file at
  `<SPEC_DIR>/canvas/overview.html`, and prints the GitHub Pages URL the
  canvas will be deployed at on merge. Use after `/speckit.tasks` is complete
  when a stakeholder-friendly overview canvas is wanted.
scripts:
  sh: .specify/oe/skills/overview/scripts/build.sh
  ps: .specify/oe/skills/overview/scripts/build.ps1
---

# /speckit.overview

## Goal

Produce a single self-contained interactive HTML canvas (`overview.html`) that surfaces a SpecKit feature's dense markdown content in 2-level progressive disclosure — overview KPIs, user stories, milestones, data model, decisions, API, testing, and inline-rendered spec files — with light + dark mode and auto-deployment to GitHub Pages.

The canvas is the stakeholder-friendly entry point for the feature: reviewers, designers, PMs, and onboarding engineers all start here, then drill into the source `.md` files for detail.

## Execution

### Step 1 — Determine SPEC_DIR

If the user provided a path, use it. Otherwise, auto-detect:

```bash
# Option A: from current git branch (feat/ogc-285-* → specs/OGC-285-*)
BRANCH=$(git branch --show-current)
# Option B: from most-recently-modified specs/<dir>
SPEC_DIR=$(ls -td specs/*/ | head -1)
```

Confirm `<SPEC_DIR>/spec.md` exists. If not, abort with an instructive message.

### Step 2 — Extract canvas data

```bash
python3 .specify/oe/skills/overview/scripts/extract-spec-data.py <SPEC_DIR>
```

Writes `<SPEC_DIR>/canvas/canvas-data.json` (created if missing). Stdout reports counts of stories / milestones / tables / endpoints / clarifications / divergences / files / commits / ACs found.

### Step 3 — Build the canvas

```bash
python3 .specify/oe/skills/overview/scripts/build-canvas.py <SPEC_DIR>
```

Assembles `templates/canvas.html.template` + `canvas-data.json` → `<SPEC_DIR>/canvas/overview.html`. Substitutes the `{{*_JSON}}` placeholders with the extracted data.

### Step 4 — Verify locally

```bash
open <SPEC_DIR>/canvas/overview.html   # macOS
xdg-open <SPEC_DIR>/canvas/overview.html   # Linux
```

Check:
- All visible tabs render (Overview / Stories / Data Model / Roadmap / Decisions / API / Testing / Spec Files / PR + Commits)
- Tabs with no data are hidden (per hide-when-empty rules)
- Light/dark mode toggle in the top-right works + persists across reloads
- Spec Files tab renders the underlying markdown inline (sidebar list + main content pane)

### Step 5 — Surface the deployed URL

Print the GitHub Pages URL the canvas will land at after the PR merges to main:

```
https://digi-uw.github.io/OpenELIS-Global-2/<SPEC_DIR>/canvas/overview.html
```

Attach this URL to the PR body and Jira ticket so reviewers can open the canvas at a stable URL.

## Hand-editing canvas-data.json

If the regex-based extraction misses a field (e.g., a custom Open Question phrasing), you can hand-edit `<SPEC_DIR>/canvas/canvas-data.json` between Step 2 and Step 3, then re-run only Step 3. The build is non-destructive: extraction → JSON → HTML is a one-way pipeline.

## When to re-run

- After authoring a new section in `spec.md` (e.g., new user story)
- After adding milestones to `plan.md`
- After resolving a clarification in `/speckit.clarify`
- After authoring or fixing tables in `data-model.md`
- After authoring or fixing endpoints in `contracts/openapi.yaml`
- After significant `tasks.md` edits affecting AC × Milestone mapping
- Before opening the spec PR for review (so reviewers can use the canvas)

## Constraints

- Output is ALWAYS at `<SPEC_DIR>/canvas/overview.html`. Do NOT move the file.
- Output is a single self-contained HTML file (Tailwind + esm.sh + Babel Standalone + lucide-react + marked CDN). No build artifact pipeline beyond the Python assembly.
- Python scripts use stdlib only (Python 3.9+); no `pip install` required.
- Canvas honors `prefers-color-scheme` on first load; user choice persists to localStorage.

## References

- `.specify/oe/skills/overview/reference/canvas-ux-research.md` — UX research that drove the canvas shape (NN/G + B2B Dashboard IA 2026 + drill-down navigation patterns)
- `.specify/oe/skills/overview/reference/canvas-design.md` — tab inventory, when to add/remove tabs
- `.specify/oe/skills/overview/reference/data-extraction.md` — JSON schema + markdown parser rules
