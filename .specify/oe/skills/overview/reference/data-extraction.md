# Canvas Data Extraction — JSON Schema + Markdown Parser Rules

**Purpose:** Document the JSON schema that `extract-spec-data.py` produces and `canvas.html.template` consumes. Plus the regex-based extraction rules used to parse spec.md / plan.md / research.md / tasks.md.

## JSON schema (`canvas-data.json`)

```jsonc
{
  "meta": {
    "title": "string — e.g., 'OGC-285 — Barcode Labels v2'",
    "subtitle": "string — e.g., 'Configurable Label Preset Management'",
    "jira": "url",
    "pr": "url",
    "frsUrl": "url",
    "frsSha": "short SHA",
    "status": "string — e.g., 'In Progress'",
    "specCommits": "integer"
  },
  "kpis": [
    { "label": "string", "value": "integer | string", "tone": "sky|emerald|amber|rose|purple|indigo|slate", "sub": "string", "icon": "lucide-react icon name" }
  ],
  "userStories": [
    {
      "id": "US1",
      "title": "string",
      "priority": "P1|P2|P3",
      "acs": ["AC-1", "AC-2", ...],
      "milestone": "M3" | "M5a + M5b",
      "summary": "string — 1-3 sentences",
      "surfaces": ["string"],
      "key": "string — sparkle takeaway"
    }
  ],
  "milestones": [
    {
      "id": "M2",
      "branch": "feat/...",
      "scope": "string",
      "stories": "US1, US4",
      "depends": ["M1"],
      "state": "in-progress|queued|done",
      "pr": "string or '—'",
      "parallel": "M3 | null"
    }
  ],
  "tables": [
    { "name": "label_preset", "purpose": "string", "pk": "string", "new": true, "note": "string?" }
  ],
  "endpoints": [
    { "method": "GET|POST|PUT|PATCH|DELETE", "path": "string", "scope": "string", "milestone": "string", "who": "string — group label" }
  ],
  "clarifications": [
    { "q": "Q1", "topic": "string", "resolution": "string", "rationale": "string", "defer": "string" }
  ],
  "divergences": [
    { "n": 1, "title": "string", "from": "string — FRS quote", "to": "string — locked decision", "principle": "string", "jira": "url?" }
  ],
  "acCoverage": [
    { "ac": "AC-1", "desc": "string", "m": "M3", "story": "US1" }
  ],
  "specFiles": [
    { "name": "spec.md", "size": "string e.g. '~40K'", "role": "string" }
  ],
  "commits": [
    { "sha": "short", "title": "string" }
  ]
}
```

## Extraction strategy

The Python extractor is regex-based for v1. It's deliberately fragile-but-readable: when it breaks on a new spec dir, fix the regex rather than swapping to a markdown library. Stdlib only.

### From `spec.md`

| Field | Source pattern |
|---|---|
| `meta.title` | First `# ` heading (e.g., `# Feature Specification: Barcode Labels v2`) |
| `meta.jira` | `**Issue**: [OGC-NNN](url)` in frontmatter |
| `meta.status` | `**Status**: ...` in frontmatter |
| `userStories[]` | `### User Story N — Title (Priority: PX)` blocks; summary is the first paragraph after the heading |
| `userStories[].acs` | Inside the user story's Acceptance Scenarios section, scan for `AC-N` references; if absent, count Given/When/Then blocks |
| `clarifications[]` | `### Session YYYY-MM-DD — ...` blocks under `## Clarifications`; each `- Q: ... → A: ...` line |
| `acCoverage[]` | FRS AC numbering 1..27 (or whatever the spec lists); cross-reference to user stories via the User Story section |

### From `plan.md`

| Field | Source pattern |
|---|---|
| `milestones[]` | Markdown table under `### Milestone Table` with columns ID / Branch Suffix / Scope / Stories / Verification / Depends On |
| `meta.specCommits` | Indirectly via `commits[]` length (see below) |

### From `research.md`

| Field | Source pattern |
|---|---|
| `meta.frsSha` | First `\`SHA\`` pattern after "Current pin:" heading |
| `meta.frsUrl` | First URL after "URL:" line in §1 |
| `divergences[]` | `### Divergence N — Title` blocks under `## 3. Deliberate divergences ...` |
| `clarifications[]` rationale | Cross-referenced from research.md §2 if spec.md only has the locked answer |

### From `data-model.md`

| Field | Source pattern |
|---|---|
| `tables[]` | `### N.N \`table_name\`` headings under `## 2. New tables` and `## 3. Modified / contingent tables` |

### From `contracts/openapi.yaml`

| Field | Source pattern |
|---|---|
| `endpoints[]` | YAML `paths.<path>.<method>` entries; tags + scope + summary |

### From `tasks.md`

| Field | Source pattern |
|---|---|
| `acCoverage[]` enrichment | Cross-reference AC mentions to specific T### tasks (e.g., "covers AC-1..AC-7") |

### From `git log`

| Field | Source pattern |
|---|---|
| `commits[]` | `git log --oneline origin/develop..HEAD` (or branch-base equivalent) |
| `meta.specCommits` | length of `commits[]` |

## Robustness

Extraction is best-effort. Specifically:

- If a field is missing, the canvas tab gracefully degrades (hide-when-empty rule from canvas-design.md).
- If a markdown table changes shape, the regex fails loudly — fix at the source rather than papering over.
- The JSON schema is the contract; extraction implementation may change without notice.

## Markdown rendering on the Spec Files tab

The canvas uses `marked` (CDN: `https://cdn.jsdelivr.net/npm/marked/marked.min.js`) for parsing and `DOMPurify` (CDN: `https://cdn.jsdelivr.net/npm/dompurify/dist/purify.min.js`) for XSS sanitization. Fetched markdown is passed through `marked.parse(text)` → `DOMPurify.sanitize(html)` → injected into the content pane.

Relative links in the rendered markdown (e.g., `[../OGC-284-.../spec.md](...)`) are intercepted client-side and rewritten to the canvas's relative location so cross-feature links work when the canvas is deployed to GitHub Pages.

Code blocks render with `<pre><code>` tags styled via Tailwind. Tables render as native HTML tables. Mermaid blocks are NOT rendered (out of scope for v1; the SVG dependency graph on the Roadmap tab is rendered separately from the canvas JSON, not from Mermaid in plan.md).

## When extraction fails

If `extract-spec-data.py` exits non-zero (e.g., spec.md has an unparseable frontmatter), the canvas command surfaces the error and refuses to build. Better to fail loudly than ship a half-extracted canvas.

If a user wants to override extraction for a specific field, they can hand-edit `<SPEC_DIR>/canvas/canvas-data.json` after extraction and re-run only the `build-canvas.py` step. The skill command supports a `--no-extract` flag for this.
