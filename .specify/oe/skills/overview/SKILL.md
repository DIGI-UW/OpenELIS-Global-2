---
name: overview
description:
  SpecKit overview-canvas skill for OpenELIS. Generates an interactive HTML
  dashboard from a feature's spec.md / plan.md / research.md / tasks.md that
  surfaces user stories, milestones, data model, decisions, API, and AC coverage
  in 2-level progressive disclosure (tabs → expandable cards). Light + dark mode
  + inline markdown rendering of spec files. Auto-deployed via GitHub Pages when
  the spec dir is on the main branch. Use after `/speckit.tasks` is complete,
  when a stakeholder-friendly view of dense spec artifacts is wanted, or when
  sharing a feature for non-engineering review.
---

# /speckit.overview Skill

Use this skill when:

- a feature's SpecKit artifacts (`spec.md` + `plan.md` + `research.md` +
  `tasks.md` + optional `data-model.md` + `contracts/`) are complete and you
  want a stakeholder-friendly interactive canvas of the content
- preparing a spec PR for non-engineering review (designers, QA, product, lab
  admins) — the canvas surfaces the structure without forcing readers through
  200KB of dense markdown
- demoing a feature's design to stakeholders external to the engineering team
- maintaining a "single pane of glass" overview that drills down into the
  underlying spec files
- onboarding a new engineer onto a feature — the canvas's Overview tab gives
  them the executive summary; the Spec Files tab lets them drill into any file's
  markdown rendered with proper headings, code blocks, tables

## Primary Entrypoint

- `/speckit.overview <SPEC_DIR>` — reads the spec markdown files, extracts
  canvas data into `canvas-data.json`, assembles `canvas/overview.html` from the
  template, and prints the deployed-on-merge URL.

If `<SPEC_DIR>` is omitted, the skill auto-detects from
`git branch --show-current` (e.g., `feat/ogc-285-spec-cleanup` →
`specs/OGC-285-barcode-label-presets/`).

## Lifecycle

1. **Detect / accept spec dir** — `specs/<feature>/`.
2. **Extract** — `scripts/extract-spec-data.py <SPEC_DIR>` reads spec.md /
   plan.md / research.md / tasks.md and writes
   `<SPEC_DIR>/canvas/canvas-data.json`.
3. **Build** — `scripts/build-canvas.py <SPEC_DIR>` substitutes the JSON into
   `templates/canvas.html.template` and writes
   `<SPEC_DIR>/canvas/overview.html`.
4. **Verify** — open the file locally to confirm rendering; the GitHub Pages
   workflow auto-deploys on push to main.
5. **Surface URL** — print the deployed URL
   (`https://digi-uw.github.io/OpenELIS-Global-2/specs/<feature>/canvas/overview.html`)
   so it can be attached to PR / Jira.

## Core Non-Negotiables

- **2-level progressive disclosure only** (tabs → expandable cards). No deeper
  nesting. See
  [reference/canvas-ux-research.md](./reference/canvas-ux-research.md) for the
  Nielsen Norman Group guidance behind this.
- **5–7 KPI cards on the Overview tab** (cognitive comfort zone per B2B
  Dashboard IA 2026 research).
- **Strong information scent** — every tab has a count badge; every expandable
  card has a status chip before expanding.
- **Light + dark mode** — default = system `prefers-color-scheme`; user choice
  persists to localStorage.
- **Spec Files tab renders markdown inline** via `marked` + DOMPurify CDN —
  readers don't have to leave the canvas to read the source.
- **Standardized data shape** — see
  [reference/data-extraction.md](./reference/data-extraction.md) for the JSON
  schema.
- **No personal-references content** — the canvas should reflect what the spec
  contains, not litigate retrospectives. If the spec is professional, the canvas
  is professional.

## Execution Invariants

- Canvas output ALWAYS goes to `<SPEC_DIR>/canvas/overview.html`. The `canvas/`
  subdir is a convention; do not move files to spec-dir root.
- The HTML is a single self-contained file (Tailwind CDN + esm.sh + Babel
  Standalone + lucide-react + marked). No build step beyond the Python assembly.
  Opens directly in a browser.
- The Python scripts have ZERO external dependencies beyond Python 3.9+. Pure
  stdlib (regex parsing of markdown).
- When the spec dir doesn't have one of the optional files (e.g.,
  `data-model.md` is missing), the corresponding tab is hidden from the canvas —
  no broken-link tab.

## References

- [reference/canvas-ux-research.md](./reference/canvas-ux-research.md) — the
  load-bearing UX research that drove this canvas shape
- [reference/canvas-design.md](./reference/canvas-design.md) — tab inventory,
  what data goes where, when to add/remove a tab
- [reference/data-extraction.md](./reference/data-extraction.md) — JSON schema
  the canvas consumes; markdown parser rules
