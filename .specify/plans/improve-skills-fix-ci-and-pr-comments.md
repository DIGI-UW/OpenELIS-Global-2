# Plan: AI Skills & Command Cleanup

**Branch:** `chore/improve-skills-fix-ci-and-pr-comments` **Scope:** Fix two
skill behaviour bugs + audit/consolidate the skill build pipeline

---

## Architecture reference

### Build pipeline (understand before editing anything)

```
Sources                              Generated outputs (DO NOT EDIT DIRECTLY)
─────────────────────────────────    ────────────────────────────────────────
.specify/core/commands/              .claude/commands/   (git-ignored)
.specify/oe/commands/          ──►   .cursor/commands/   (git-ignored)
.ai/skills/<name>/                   .claude/skills/     (git-ignored)
                                     .cursor/skills/     (git-ignored)
```

`scripts/install-agent-skills.py` is the build step. It:

1. Cleans all `.md` files from `.claude/commands/` and `.cursor/commands/`
2. Copies `.specify/oe/commands/*.md` directly to both output dirs
3. Merges core SpecKit + OE content for `speckit.*.md` files
4. Copies packaged skill commands from `.ai/skills/<name>/commands/` to both
   output dirs
5. Mirrors packaged skill source trees into `.claude/skills/` and
   `.cursor/skills/`

**Rule: only edit source files, then run the build step to regenerate outputs.**

```bash
python3 scripts/install-agent-skills.py -y
```

### Two skill formats

| Format             | Source location                                                                                      | When to use                                                                |
| ------------------ | ---------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| **Legacy command** | `.specify/oe/commands/<name>.md`                                                                     | Single-file commands, OE-specific workflows                                |
| **Packaged skill** | `.ai/skills/<name>/` with `SKILL.md` + `commands/` + optional `reference/`, `scripts/`, `templates/` | Rich skills needing supporting assets (templates, reference docs, scripts) |

Currently only `playwright` is a packaged skill. All other skills are legacy
commands.

---

## Change 1 — `/fix-ci`: Replace blocking exponential backoff with background polling

**Source file:** `.specify/oe/commands/fix-ci.md`

### Problem

Track B in Phase 5 uses a blocking `while true; sleep` loop with exponential
backoff. This holds the conversation context for 3–60+ minutes and prevents the
agent from doing anything else while waiting for CI.

```bash
# Current — blocks the entire conversation context
WAIT_SECS=180
while true; do
  sleep $WAIT_SECS
  STATUS=$(gh run list ...)
  if [[ "$STATUS" == *"completed"* ]]; then break; fi
  WAIT_SECS=$(( WAIT_SECS * 2 ))
done
```

### Fix

Instruct the agent to use `run_in_background: true` on the Bash tool call. When
the poll exits, the system automatically injects a `<task-notification>` into
the conversation — no blocking, no sleeping in the foreground.

```bash
# New — fixed 60s interval, exits when all runs complete or after 30 min
for i in $(seq 1 30); do
  sleep 60
  result=$(gh run list --branch $BRANCH --limit 5 --repo $REPO 2>&1)
  echo "=== Poll $i ($(date -u +%H:%M:%S)) ==="
  echo "$result"
  if ! echo "$result" | grep -Eq "in_progress|queued"; then
    echo "All runs completed."
    break
  fi
done
# ↑ Launch this with run_in_background: true
```

### Specific edits to `.specify/oe/commands/fix-ci.md`

1. Rename section header "Track B — CI monitoring (exponential backoff)" →
   "Track B — CI monitoring (background polling)"
2. Remove the 5-row backoff timing table — no longer relevant with fixed
   interval
3. Replace the `while true; sleep` shell block with the `for` loop above
4. Add explanatory note: the agent must use `run_in_background: true`; when the
   poll finishes, a `<task-notification>` fires automatically with the output
   file path; agent reads that file and proceeds to reconcile
5. Clarify parallel tracks: Track A (local E2E) and Track B (background CI poll)
   are now truly independent and can both be launched right after push

---

## Change 2 — `/address-pr-comments`: Add VVF assessment to Phase 1 triage

**Source file:** `.specify/oe/commands/address-pr-comments.md`

### Problem

Phase 1 assigns a _category_ (blocking, suggestion, nitpick…) but makes no
qualitative assessment of the comment itself. The triage table shows only:

```
| #  | Category   | Author  | File:Line  | Preview          | Notes |
```

This means the user arrives at the QA session with no signal on:

- **Validity**: Is the reviewer technically correct? Do they understand the
  code?
- **Value**: Does addressing this meaningfully improve quality for the cost?
- **Feasibility**: Can this be done in this PR, or does it need a separate PR?

The triage pass is a sorting step, not a decision-support step. The user has to
reason through all three dimensions themselves, per comment, under time pressure
in the QA session.

### Fix

Add a per-comment VVF (Validity / Value / Feasibility) assessment to Phase 1.
For each unresolved thread, the agent evaluates all three dimensions and derives
a recommended action (`Rec`).

#### VVF scales

| Dimension       | Scale                                    |
| --------------- | ---------------------------------------- |
| **Validity**    | ✅ Sound / ⚠️ Partial / ❌ Questionable  |
| **Value**       | High / Medium / Low                      |
| **Feasibility** | In-scope / Needs-refactor / Out-of-scope |

#### Recommended action rules

| VVF Pattern             | Rec                               |
| ----------------------- | --------------------------------- |
| ✅ + High + In-scope    | **Fix**                           |
| ✅ + Low + In-scope     | **Fix if trivial**, else Defer    |
| ✅ + any + Out-of-scope | **Defer** to follow-up issue      |
| ⚠️ + any + any          | **Reply** — discuss before acting |
| ❌ + Low + any          | **Dismiss** with explanation      |

#### Updated triage table

```
| #  | Category   | V/V/F               | Rec    | Author  | File:Line  | Preview             |
|----|------------|---------------------|--------|---------|------------|---------------------|
| 1  | blocking   | ✅ High In-scope    | Fix    | ibacher | ...java:42 | "Missing auth..."   |
| 2  | suggestion | ✅ Medium In-scope  | Fix    | ibacher | ...jsx:79  | "Consider using..." |
| 3  | nitpick    | ⚠️ Low In-scope    | Reply  | copilot | ...jsx:156 | "Unused import"     |
| 4  | suggestion | ❌ Medium Needs-refactor | Reply  | copilot | ...java:88 | "Refactor to..."    |
```

### Specific edits to `.specify/oe/commands/address-pr-comments.md`

1. In Phase 1, add a "VVF Assessment" step after the classification step, before
   the triage table presentation
2. Update the triage table format to include `V/V/F` and `Rec` columns
3. Update the override prompt: allow overriding both category AND rec
   (`"#3 → fix"`, `"#3 cat=blocking"`, `"#3 rec=defer"`)
4. In Phase 2 (QA Session), pre-highlight the `Rec` value as the default action
   in the prompt — reduces keystrokes in the common case

---

## Change 3 — Skill build pipeline: document + enforce build step in AGENTS.md

**Source file(s):** `AGENTS.md` (skill editing section)

AGENTS.md already explains the source → output compilation pipeline, but lacks a
prominent "DO NOT EDIT — generated + gitignored" warning on the output
directories. A contributor who doesn't read the full pipeline section could
still edit `.claude/commands/` or `.cursor/skills/` directly and lose changes.

### Edits

1. Add a "Skill Development" section to AGENTS.md documenting:
   - The two source formats (legacy command vs. packaged skill)
   - The build step (`python3 scripts/install-agent-skills.py -y`)
   - The rule: never edit generated outputs directly
2. Consider adding a comment header to generated files warning they are
   auto-generated (low priority — markdown has no standard comment syntax that
   all renderers hide)

---

## Change 4 (optional/audit) — Review which legacy commands should become packaged skills

The `playwright` skill demonstrates the packaged format: it has `SKILL.md`,
`commands/`, `reference/` (docs), `scripts/`, and `templates/`. This is richer
than a flat `.md` file and scales better for skills with supporting assets.

Candidates to evaluate for migration to packaged skill format:

| Command                  | Supporting assets?                                         | Migration value |
| ------------------------ | ---------------------------------------------------------- | --------------- |
| `fix-ci.md`              | Potentially — could add `reference/ci-failure-patterns.md` | Medium          |
| `address-pr-comments.md` | Potentially — could add reply templates                    | Low             |
| `careful-rebase.md`      | No significant assets                                      | Low             |
| `speckit.*` commands     | Already have core/oe merge — migration complex             | Skip            |

**Recommendation:** Defer migration to a follow-up branch unless a clear asset
need exists. The current flat format works fine for these commands.

---

## Execution order

1. Edit `.specify/oe/commands/fix-ci.md` (Change 1)
2. Edit `.specify/oe/commands/address-pr-comments.md` (Change 2)
3. Edit `AGENTS.md` (Change 3)
4. Run `python3 scripts/install-agent-skills.py -y` to regenerate all outputs
5. Verify generated outputs: spot-check `.claude/commands/fix-ci.md` and
   `.claude/commands/address-pr-comments.md`
6. Commit source files only (generated outputs are gitignored and regenerated
   locally via the install script)
7. Push and open PR targeting `develop`

If you have run `.githooks/setup.sh` to enable the repo pre-commit hooks, the
pre-commit hook will run spotless/prettier on staged `.md` files when you commit.
