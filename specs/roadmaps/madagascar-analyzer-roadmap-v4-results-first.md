# Madagascar Analyzer Roadmap V4: Results First

**Date**: 2026-03-25 **Supersedes**:
`specs/roadmaps/madagascar-analyzer-roadmap-v3-finishline.md` **Priority**: Get
results flowing from all 11 Madagascar priority analyzers **Execution Model**:
Parallel worktree agents with shared checkpoints **Out of scope**: Feature 014 /
PR #3103 (FILE workflow) — handled on separate machine

---

## Step 0: Publish and Set Up

### 0a. Publish roadmap

Save this plan as
`specs/roadmaps/madagascar-analyzer-roadmap-v4-results-first.md`. Add superseded
header to V3.

### 0b. Create worktrees

```bash
# Lane A: Profile verification — new branch from develop (profiles only, no backend)
git worktree add ../wt-lane-a-profiles -b feat/v4-lane-a-file-profile-verification develop

# Lane B: HL7 PR fix — existing PR #3035 branch (already rebased on develop)
git worktree add ../wt-lane-b-hl7 origin/feat/013-ogc-326-bs-series-hl7
```

Lane C (ASTM #3032) worktree created later — after #3103 and #3035 merge.

### 0c. Create prompt files

Create `PROMPT.md` in each worktree root (NOT committed). These are
self-contained agent briefings scoped to the relevant feature spec.

### 0d. Create PR for Lane A

```bash
cd ../wt-lane-a-profiles
git push -u origin feat/v4-lane-a-file-profile-verification
gh pr create --draft \
  --title "feat: verify and fix FILE profiles for Madagascar LA2M testing" \
  --body "$(cat <<'EOF'
## Summary
- Verify all FILE profiles against real site exports from Madagascar LA2M
- Fix column mappings, sheet handling, format support as needed
- Profile JSON only — no backend code changes

## Checklist
- [ ] quantstudio.json verified (QS5 3rd-sheet headers)
- [ ] fluorocycler-xt.json verified (corrected Excel)
- [ ] tecan-f50.json verified against Herbert's CSV
- [ ] multiskan-fc.json verified against Herbert's exports
- [ ] Attune CytPix assessed (PDF-only)
- [ ] GeneXpert CSV profile created if needed
EOF
)"
```

---

## LANE A: FILE Profile Verification

**Worktree**: `../wt-lane-a-profiles` **Branch**:
`feat/v4-lane-a-file-profile-verification` (new, from develop) **PR**: New draft
PR **Spec reference**: `specs/014-hjra-file-stream-alignment/` (read-only
context) **Scope**: Profile JSON files ONLY — no backend code, no 014 branch
work

### PROMPT.md for Lane A

```markdown
# Lane A: FILE Profile Verification for Madagascar LA2M

## Goal

Verify and fix all FILE analyzer profiles against real site exports so Herbert
can test all file-based analyzers at LA2M.

## IMPORTANT SCOPE CONSTRAINT

This lane modifies ONLY profile JSON files in
`projects/analyzer-profiles/file/`. Do NOT modify any backend Java code or
frontend code. Feature 014 backend work (PR #3103) is handled separately on
another machine.

## Spec Reference (read-only context)

- `specs/014-hjra-file-stream-alignment/spec.md`
- `specs/014-hjra-file-stream-alignment/plan.md`

## Critical Convention

All analyzer support uses the **3 generic plugins** (GenericFile, GenericHL7,
GenericASTM) with configurable JSON profiles. Do NOT create per-analyzer
plugins. Profiles live in `projects/analyzer-profiles/file/`.

## Tasks

### A1. Verify quantstudio.json for QS5 3rd-sheet headers

- File: `projects/analyzer-profiles/file/quantstudio.json`
- Context: QS5 exports have headers on 3rd Excel sheet (same format as QS7).
  Loris confirmed 3/25.
- Action: Verify the profile's sheet selection and header detection handles
  this. Fix if needed.

### A2. Verify fluorocycler-xt.json with corrected Excel

- File: `projects/analyzer-profiles/file/fluorocycler-xt.json`
- Context: Site exports PDF-only natively. Technicians copy to Excel. Loris
  uploaded corrected Excel with headers 3/25.
- Action: Verify column mapping (SampleID, WellPosition, AssayName, TargetName,
  CP, Interpretation, RunDate) matches the corrected export.

### A3. Verify tecan-f50.json against Herbert's CSV

- File: `projects/analyzer-profiles/file/tecan-f50.json`
- Context: Herbert shared real CSV files from site on 3/24 in
  #oe-madagascar-internal Slack channel. Site may use custom Excel template, not
  standard Magellan export.
- Action: Get Herbert's files. Verify plate-grid layout and column mappings
  match. Adjust if needed.

### A4. Verify multiskan-fc.json against Herbert's exports

- File: `projects/analyzer-profiles/file/multiskan-fc.json`
- Context: Herbert shared both CSV and Excel. French locale (semicolons, comma
  decimals). Dual layout support (plate-grid + well-per-row).
- Action: Get Herbert's files. Verify format. Recommend CSV vs Excel to Herbert.

### A5. Assess Attune CytPix

- Context: PDF-only output. No structured export. No example files uploaded.
- Action: Check if any FCS/CSV export path exists. If truly PDF-only, document
  as deferred with manual entry recommendation.
- Output: Decision note in PR description.

### A6. Create GeneXpert CSV file profile (if needed)

- Context: GeneXpert can export CSV. ASTM path is blocked by IP config issues
  on-site. CSV flat-file is the fastest path to results.
- Action: If no existing profile covers CSV import, create
  `projects/analyzer-profiles/file/genexpert-csv.json`.
- Template: Use quantstudio.json as structural template.

## Checkpoint (all must be true)

- [ ] QS5 profile works with 3rd-sheet export
- [ ] FluoroCycler XT profile works with corrected Excel
- [ ] Tecan F50 profile matches Herbert's actual CSV
- [ ] Multiskan FC profile matches Herbert's actual exports
- [ ] Attune CytPix: decision documented
- [ ] GeneXpert CSV path: confirmed or new profile created

## Constraints

- ONLY modify profile JSON files in `projects/analyzer-profiles/file/`
- Do NOT modify backend Java code (014 PR is on another machine)
- Do NOT modify frontend code
- Do NOT change spec directories
```

### Lane A Tasks

| Task | File                                                   | Action                      |
| ---- | ------------------------------------------------------ | --------------------------- |
| A1   | `projects/analyzer-profiles/file/quantstudio.json`     | Verify QS5 3rd-sheet        |
| A2   | `projects/analyzer-profiles/file/fluorocycler-xt.json` | Verify corrected Excel      |
| A3   | `projects/analyzer-profiles/file/tecan-f50.json`       | Verify vs Herbert's CSV     |
| A4   | `projects/analyzer-profiles/file/multiskan-fc.json`    | Verify vs Herbert's exports |
| A5   | (assessment)                                           | Attune CytPix feasibility   |
| A6   | `projects/analyzer-profiles/file/genexpert-csv.json`   | Create if needed            |

---

## LANE B: HL7 PR Fix + HJRA Prep

**Worktree**: `../wt-lane-b-hl7` **Branch**: `feat/013-ogc-326-bs-series-hl7`
(existing, PR #3035) **PR**: #3035 (open, needs E2E fix) **Spec scope**:
`specs/013-hjra-hl7-stream-alignment/` **HARD DEADLINE**: Merge before Friday
3/28 (HJRA networking may come online)

### Branch State

- Merge base = develop HEAD (`96d2dc379`) — **already fully rebased**
- 2 commits ahead of develop
- Only submodule change: `tools/analyzer-mock-server`
- CI: backend/frontend/format PASS, E2E harness FAILS

### PROMPT.md for Lane B

````markdown
# Lane B: HL7 PR Fix and HJRA Preparation

## Goal

Fix the E2E failures on PR #3035, get it merged to develop, and prepare
deployment documentation for the HJRA site visit (Mindray BC-5380, BS-200,
BS-300 via HL7).

## HARD DEADLINE: Friday 2026-03-28

HJRA site networking may come online Friday. HL7 code must be on develop before
then.

## Spec Scope

This work is scoped to **Feature 013** (HJRA HL7 Stream Alignment). Read these
artifacts for context:

- `specs/013-hjra-hl7-stream-alignment/spec.md`
- `specs/013-hjra-hl7-stream-alignment/plan.md`
- `specs/013-hjra-hl7-stream-alignment/tasks.md`
- `specs/013-hjra-hl7-stream-alignment/evidence-boundary.md`
- `specs/013-hjra-hl7-stream-alignment/launch-checklists/gate1-ogc325-evidence.md`

## Current State

- Branch is fully rebased on develop (merge base = develop HEAD)
- 2 commits ahead: M1 listener + M2 BC-5380 + M3 BS-series (consolidated)
- Backend tests PASS, format PASS, build PASS
- E2E harness FAILS with TWO distinct issues

## Tasks

### B1. Fix bridge directory creation timeout

- File: `.github/workflows/e2e-playwright-analyzer-harness-reusable.yml`
- Lines 161-169: timeout 30s waiting for bridge to create incoming/ dirs
- Fix: Pre-create dirs in "Create analyzer import bind mount directories" step
  (line 137-140) BEFORE the bridge starts:

```bash
mkdir -p projects/analyzer-harness/volume/analyzer-imports/quantstudio-5/incoming
mkdir -p projects/analyzer-harness/volume/analyzer-imports/quantstudio-7/incoming
mkdir -p projects/analyzer-harness/volume/analyzer-imports/fluorocycler-xt/incoming
```
````

Keep existing timeout loop as safety net but increase to 120s.

### B2. Fix missing HL7 test catalog mappings

- File: `projects/analyzer-harness/seed-analyzers.sh`
- Error: PluginAnalyzerService.getIdForTestName: Unable to find test (~13x)
- Fix: Extend seed-analyzers.sh to create HL7 analyzer instances with test
  mappings from their profiles:
- `projects/analyzer-profiles/hl7/mindray-bc5380.json` (13 tests)
- `projects/analyzer-profiles/hl7/mindray-bs200.json`
- `projects/analyzer-profiles/hl7/mindray-bs300.json`

### B3. Resolve mock-server #18 dependency

- `analyzer-mock-server#18` (strict 013 HL7 mock support) open since 3/10.
- Check: Does #3035's mock-server submodule pointer already include needed
  changes, or must #18 merge first?
- If needed: merge #18, update submodule pointer.

### B4. Push fixes and verify CI green

- Run: `mvn spotless:apply` before push
- All 8 CI checks must pass, especially "03 Checkpoint - E2E"

### B5. Prepare HJRA deployment runbook

Create `specs/013-hjra-hl7-stream-alignment/hjra-deployment-runbook.md`:

- MLLP port assignments (BC-5380: 5380, BS-200/BS-300: per profile)
- Bridge MLLP forwarding configuration
- Mindray BS-series = **HL7 PUSH** (ORU^R01 to OE, ACK back)
- Windows XP → Linux VPN gateway → bridge → OpenELIS path

### B6. Answer Romain's Mindray push/pull question on Slack

BS-series = HL7 push (analyzer sends ORU^R01, OE sends ACK).

## Checkpoint (all must be true)

- [ ] Bridge dir creation no longer causes timeout in CI
- [ ] HL7 analyzers have test catalog mappings in seed script
- [ ] Mock-server dependency resolved
- [ ] PR #3035 CI fully green
- [ ] PR #3035 approved and merged to develop
- [ ] HJRA deployment runbook created

## Constraints

- Do NOT modify code outside 013 scope
- Do NOT touch profiles in astm/ or file/ directories
- Do NOT touch 014 code (handled on another machine)
- Branch is already rebased — do NOT rebase unless develop moves
- Run formatting: `mvn spotless:apply` before every push

````

### Lane B Tasks

| Task | File                                                             | Action                   |
| ---- | ---------------------------------------------------------------- | ------------------------ |
| B1   | `.github/workflows/e2e-playwright-analyzer-harness-reusable.yml` | Pre-create dirs          |
| B2   | `projects/analyzer-harness/seed-analyzers.sh`                    | Add HL7 analyzer seeding |
| B3   | `tools/analyzer-mock-server` (submodule)                         | Resolve #18 dependency   |
| B4   | (CI verification)                                                | Push + green CI          |
| B5   | `specs/013-hjra-hl7-stream-alignment/hjra-deployment-runbook.md` | Create runbook           |
| B6   | (Slack)                                                          | Answer Romain            |

---

## LANE C: ASTM Bidi Rebase + Merge (Deferred)

**Worktree**: Created AFTER #3103 (other machine) and #3035 (Lane B) merge
**Branch**: `feat/012-ogc-337-generic-astm-plugin-profiles-m3-bidi-genexpert`
(existing, PR #3032) **PR**: #3032 (open, behind develop) **Spec scope**:
`specs/012-generic-astm-plugin-profiles/` **Blocked by**: PR #3103 merge (other
machine) AND PR #3035 merge (Lane B)

### Branch State

- Merge base = `430d25f4c` — **NOT rebased, ~8 commits behind develop**
- 9 commits ahead of merge base
- 2 submodules updated (plugins, mock-server)
- Will need rebase after BOTH #3103 and #3035 merge

### When to Start Lane C

```bash
# Only after both #3103 and #3035 are merged to develop:
git fetch origin
git worktree add ../wt-lane-c-astm origin/feat/012-ogc-337-generic-astm-plugin-profiles-m3-bidi-genexpert
````

### PROMPT.md for Lane C (created when worktree is set up)

```markdown
# Lane C: ASTM Bidirectional PR Rebase and Merge

## Goal

Rebase PR #3032 onto the updated develop (which now includes #3103 FILE and
#3035 HL7), fix any conflicts, verify CI, and merge.

## Spec Scope

This work is scoped to **Feature 012** (Generic ASTM Plugin Profiles). Read
these artifacts for context:

- `specs/012-generic-astm-plugin-profiles/spec.md`
- `specs/012-generic-astm-plugin-profiles/plan.md`
- `specs/012-generic-astm-plugin-profiles/tasks.md`

## Current State

- Branch is ~8 commits behind develop BEFORE this session's merges
- After #3103 and #3035 merge, it will be ~23+ commits behind
- 9 commits with ASTM bidi implementation
- Liquibase migrations need ordering verification

## Tasks

### C1. Rebase #3032 on updated develop

Conflict-prone files:

- `AnalyzerRestController.java` (overlaps with #3103)
- `AnalyzerServiceImpl.java` (overlaps with #3035)
- `docker-compose.analyzer-test.yml` (overlaps with both)
- `plugins` submodule pointer
- `tools/analyzer-mock-server` submodule pointer

### C2. Verify Liquibase migration ordering

- `3.4.14.x/010-create-analyzer-plugin-config.xml`
- `3.4.14.x/011-create-analyzer-pending-code.xml`
- Must be properly sequenced in `3.4.14.x/base.xml`
- Must not conflict with migrations from #3103 or #3035

### C3. Verify submodule pointers

After rebase, ensure plugins and mock-server submodules point to commits that
include all merged work.

### C4. Push, verify CI green, get approval, merge #3032

## Checkpoint (all must be true)

- [ ] #3032 rebased cleanly on post-merge develop
- [ ] Liquibase migrations verified and ordered correctly
- [ ] Submodule pointers correct
- [ ] PR #3032 CI fully green
- [ ] PR #3032 approved and merged to develop

## Constraints

- Do NOT modify code outside 012 scope
- Accept incomplete M1 tasks (T018-T021) as follow-up work
- Run formatting: mvn spotless:apply && cd frontend && npm run format
```

### Lane C Tasks

| Task | File                                        | Action                       |
| ---- | ------------------------------------------- | ---------------------------- |
| C1   | (rebase)                                    | Rebase on post-merge develop |
| C2   | Liquibase `3.4.14.x/010-*.xml`, `011-*.xml` | Verify ordering              |
| C3   | submodule pointers                          | Verify after rebase          |
| C4   | (CI + merge)                                | Green CI + merge #3032       |

---

## Coordination Actions (No Worktree)

| Action                                | When               | How                                               |
| ------------------------------------- | ------------------ | ------------------------------------------------- |
| Answer Romain (Mindray = HL7 push)    | TODAY              | Post to Slack                                     |
| Reinforce generic plugins to Herbert  | TODAY              | Post to Slack                                     |
| Coordinate GeneXpert CSV vs ASTM path | This week          | Slack                                             |
| Update Jira statuses                  | After CHECKPOINT 2 | Bulk update OGC tickets                           |
| Update Confluence tracker             | After CHECKPOINT 2 | Edit tracker page                                 |
| Generate 3 feature videos             | After CHECKPOINT 2 | `npm run pw:test -- --project=harness-demo-video` |
| Deploy to UAT                         | After CHECKPOINT 2 | Coordinate with Romain                            |

---

## Checkpoints

### CHECKPOINT 1: Profiles Verified (target: Wed 3/26)

**Gate**: Lane A complete.

- [ ] All LA2M FILE profiles verified against real site exports
- [ ] Herbert confirmed testing with generic plugins
- [ ] Attune CytPix decision documented
- [ ] GeneXpert CSV path confirmed

### CHECKPOINT 2: PRs Merged (target: Thu-Fri 3/27-28)

**Gate**: Lane B complete + Lane C complete (Lane C waits for #3103 from other
machine).

- [ ] PR #3035 (HL7) merged to develop — **before Friday**
- [ ] PR #3103 (FILE) merged to develop — **other machine**
- [ ] PR #3032 (ASTM) merged to develop — **after both above**

### CHECKPOINT 3: Results Flowing + Evidence (target: Fri 3/28+)

- [ ] HJRA HL7 analyzers tested (if networking ready)
- [ ] UAT deployed with merged develop
- [ ] 3 feature videos captured
- [ ] Jira/Confluence updated

---

## Parallel Execution Diagram

```
TIME →   Today (Tue)    Wed 3/26       Thu 3/27        Fri 3/28
         ─────────────────────────────────────────────────────────
Lane A   [A1-A6 profiles]────●CP1
Lane B   [B1-B2 E2E fix]──[B3-B4 CI]──[B5-B6 merge]──●
Other    [────── PR #3103 FILE (other machine) ──────]──●
Lane C                                    (wait)──[C1-C4 rebase+merge]──●CP2
Coord    [Slack answers]───────────────[Jira/Confluence]──[Videos]──●CP3
```

---

## Merge Order (Strict)

```
1. PR #3103  (FILE, other machine)   ← not our scope, but we wait for it
2. PR #3035  (HL7, Lane B)           ← our primary deliverable
3. PR #3032  (ASTM, Lane C)          ← after both above merge
```

#3103 before #3035 because it has more submodule changes. If #3035 is ready
first and #3103 hasn't merged yet, #3035 can merge first (they don't conflict on
the same files), but #3032 must wait for both.

---

## Key Files

**Profiles (Lane A)**:

- `projects/analyzer-profiles/file/quantstudio.json`
- `projects/analyzer-profiles/file/fluorocycler-xt.json`
- `projects/analyzer-profiles/file/tecan-f50.json`
- `projects/analyzer-profiles/file/multiskan-fc.json`

**E2E CI fix (Lane B)**:

- `.github/workflows/e2e-playwright-analyzer-harness-reusable.yml:137-169`
- `projects/analyzer-harness/seed-analyzers.sh`

**HL7 profiles (in PR #3035)**:

- `projects/analyzer-profiles/hl7/mindray-bc5380.json`
- `projects/analyzer-profiles/hl7/mindray-bs200.json`
- `projects/analyzer-profiles/hl7/mindray-bs300.json`

**ASTM rebase (Lane C, deferred)**:

- `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java`
- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerServiceImpl.java`
- Liquibase: `src/main/resources/liquibase/3.4.14.x/010-*.xml`, `011-*.xml`

**Spec references per lane**:

- Lane A: `specs/014-hjra-file-stream-alignment/` (read-only context)
- Lane B: `specs/013-hjra-hl7-stream-alignment/`
- Lane C: `specs/012-generic-astm-plugin-profiles/`

## Risks

| Risk                                  | Impact | Mitigation                                      |
| ------------------------------------- | ------ | ----------------------------------------------- |
| Attune CytPix PDF-only                | HIGH   | Defer with manual entry recommendation          |
| HJRA networking not ready Friday      | MED    | HL7 merge enables UAT testing regardless        |
| GeneXpert config issues persist       | MED    | CSV flat-file as faster alternative             |
| Profile mismatches with real exports  | MED    | Get Herbert's files, test locally first         |
| #3103 merge delayed on other machine  | MED    | #3035 can merge independently; only #3032 waits |
| #3032 rebase conflicts after 2 merges | MED    | Start scope review early                        |
