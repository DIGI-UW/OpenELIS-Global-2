# Specification Analysis Report: Rebase & Artifact Consistency

**Date**: 2026-02-06 **Branch**: `feat/011-analyzer-dashboard-fixtures`
**Base**: `demo/madagascar` **Analyst**: Claude Code `/speckit.analyze`

---

## Section 0: Branch State & Rebase Analysis

### Current Branch State

| Metric                                  | Value                                             |
| --------------------------------------- | ------------------------------------------------- |
| Feature branch                          | `feat/011-analyzer-dashboard-fixtures`            |
| Base branch                             | `demo/madagascar`                                 |
| Merge base                              | `a3d4d2cc4` ("Add test result answers (#2677)")   |
| Feature-only commits                    | **7** (2 unique + 5 cherry-picked to base)        |
| Base-only commits                       | **33** (28 unique + 5 cherry-picked from feature) |
| Files changed on feature                | **554**                                           |
| Files changed on base                   | **513**                                           |
| Files overlapping (potential conflicts) | **486 (88%)**                                     |

### Cherry-Pick Duplication (CRITICAL)

5 of 7 feature commits already exist on `demo/madagascar` as cherry-picks:

| Feature SHA | Base SHA    | Message                                                       |
| ----------- | ----------- | ------------------------------------------------------------- |
| `418f4451b` | `b0a9ccff6` | fix(ci): Add .env preparation step to Playwright E2E workflow |
| `567c946c8` | `9fdbec330` | Sidenav revamp: analyzer workflow focus (#2694)               |
| `9d929bd62` | `78495f390` | fix(011): Use absolute path for logo in AnalyzerLayout        |
| `f3685357d` | `44d5c60b3` | test fix                                                      |
| `373488859` | `6430d4168` | feat(011): Madagascar Analyzer Integration - Complete feature |

**Only 2 commits are truly unique to the feature branch:**

- `984540bde` - feat(011): Circular dependency fix and analyzer dashboard
  fixtures (114 files, 16K+ insertions)
- `c2c6b11cf` - Pointer update

### New Work on `demo/madagascar` Since Divergence

28 genuinely new commits on base including:

| Category                          | Commits | Impact                                                |
| --------------------------------- | ------- | ----------------------------------------------------- |
| Test fixes (sidenav, DOB, locale) | 12      | Cypress/Playwright stability                          |
| CI/CD improvements                | 5       | Frontend QA pipeline split, locale fixes              |
| New features                      | 2       | CSV analyzer support (M4), HL7 Universal Bridge (M2b) |
| Translation updates               | 2       | Transifex en/fr sync                                  |
| Submodule updates                 | 3       | Bridge submodule M1 merged                            |
| Docs                              | 1       | Spec docs moved to milestones/                        |
| Docker                            | 1       | File permissions fix                                  |

### Conflict Assessment: SEVERE

| File Category                  | Conflicting Files | Risk Level |
| ------------------------------ | ----------------- | ---------- |
| Backend Java (analyzer module) | 133               | CRITICAL   |
| Backend tests                  | 105               | HIGH       |
| Frontend components            | 96                | HIGH       |
| Liquibase/Resources            | 37                | CRITICAL   |
| Cypress E2E tests              | 27                | MODERATE   |
| CI/Config/Root                 | 22                | MODERATE   |
| Docs                           | 17                | LOW        |
| Frontend config                | 10                | MODERATE   |

---

## Section 1: Specification Findings

| ID     | Category      | Severity | Location(s)         | Summary                                                                                                                                     | Recommendation                                                                |
| ------ | ------------- | -------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **R1** | Rebase        | CRITICAL | Branch state        | 486/554 files overlap between feature and base; naive rebase will require resolving conflicts across 7 commits                              | Squash feature into 1-2 commits, then rebase (see Section 3)                  |
| **R2** | Rebase        | CRITICAL | Cherry-picks        | 5 of 7 feature commits already cherry-picked to `demo/madagascar` - rebase will re-apply them causing guaranteed conflicts                  | Must use `git rebase --onto` with skip strategy or squash-first approach      |
| **R3** | Rebase        | HIGH     | Liquibase           | 37 Liquibase/resource files changed on both sides - incorrect resolution can break DB migrations                                            | Manual verification of changeset ordering post-rebase                         |
| **R4** | Rebase        | HIGH     | `demo/madagascar`   | New features on base (CSV analyzer M4, HL7 Universal Bridge M2b) may overlap with feature's analyzer work                                   | Review `c7c90cbb2` and `7338ab223` for semantic conflicts with feature        |
| **C1** | Coverage      | MEDIUM   | tasks.md:M0         | M0 (ASTM Setup Validation) still PENDING - all tasks unchecked, but M5-M14 are marked complete                                              | Clarify: was M0 implicitly completed or bypassed?                             |
| **C2** | Coverage      | MEDIUM   | tasks.md:M6         | M6 (Mindray Serial) tasks are checked [x] but plan.md lists M6 as "PENDING"                                                                 | Status inconsistency between plan.md and tasks.md                             |
| **U1** | Underspec     | MEDIUM   | spec.md:L6          | Scope says "12 minimum" but text alternates between "12" and "13" (with BC2000)                                                             | Standardize: 13 required analyzers throughout                                 |
| **U2** | Underspec     | LOW      | spec.md:L56-59      | 4 analyzers have "Verification required" / "Verify transport" flags still unresolved                                                        | Close verification items or document accepted risk                            |
| **D1** | Duplication   | LOW      | tasks.md            | T200 used for both M12 (Abbott - "Implement HL7 message parsing") and originally for M19 (GenericHL7) in Workstream F header                | Task IDs T200-T239 overlap between M12 and Workstream F numbering             |
| **I1** | Inconsistency | MEDIUM   | plan.md vs tasks.md | Plan shows M19-M21 (Workstream F) with descriptions, but tasks.md Workstream F header references T200-T239 which actually belong to M12-M15 | Workstream F tasks appear to have never been fully written                    |
| **I2** | Inconsistency | LOW      | plan.md:L186        | M13 PR #2664 listed as "COMPLETE" in plan, but tasks.md T205 (setup) is unchecked `[ ]` while all other M13 tasks are `[x]`                 | Minor checklist inconsistency                                                 |
| **A1** | Ambiguity     | MEDIUM   | spec.md:L34         | "GenericFile: DEFERRED" but FR-003 says "System MUST support file-based result import"                                                      | Clarify: FR-003 satisfied by FileAnalyzerReader (M3), not GenericFile plugin? |
| **A2** | Ambiguity     | LOW      | spec.md:L36         | "Instrument metadata form" listed as Required by deadline, but plan puts M16 after M15 with no timeline guarantee                           | May miss deadline; document priority vs deadline risk                         |

---

## Section 2: Coverage Summary

### Milestone Completion Status

| Milestone               | Plan Status    | Tasks Status         | PR #  | Notes                           |
| ----------------------- | -------------- | -------------------- | ----- | ------------------------------- |
| M0: ASTM Validate       | PENDING        | All `[ ]`            | -     | **Never started?**              |
| M1: HL7 Adapter         | COMPLETE       | All `[x]`            | #2602 | Done                            |
| M2: RS232 Bridge        | COMPLETE       | All `[x]`            | #2600 | Done                            |
| M3: File Adapter        | COMPLETE       | All `[x]`            | #2599 | Done                            |
| M4: Simulator           | COMPLETE       | All `[x]`            | #2601 | Done                            |
| M5: Mindray HL7         | COMPLETE       | All `[x]`            | #2665 | Done                            |
| M6: Mindray Serial      | PENDING (plan) | All `[x]` (tasks)    | -     | **Inconsistent**                |
| M7: GeneXpert Multi     | PENDING (plan) | All `[x]` (tasks)    | -     | **Inconsistent**                |
| M8: QuantStudio         | PENDING (plan) | All `[x]` (tasks)    | -     | **Inconsistent**                |
| M9-M10: Horiba          | COMPLETE       | All `[x]`            | #2643 | Done                            |
| M11: Stago              | COMPLETE       | All `[x]`            | #2663 | Done                            |
| M12: Abbott             | COMPLETE       | All `[x]`            | #2662 | Done                            |
| M13: FluoroCycler       | COMPLETE       | All `[x]`            | #2664 | Done                            |
| M14: P2 Validation      | PENDING (plan) | All `[x]` (tasks)    | #2674 | **PR exists, plan not updated** |
| M15: Order Export       | PENDING        | All `[ ]`            | -     | Not started                     |
| M16: Metadata Form      | PENDING        | All `[ ]`            | -     | Not started                     |
| M17: Simulator Adv      | PENDING        | All `[ ]`            | -     | Not started                     |
| M18: E2E Validation     | PENDING        | All `[ ]`            | -     | Not started                     |
| M19: GenericHL7         | NEW            | **No tasks written** | -     | Only plan description exists    |
| M20: Default Configs    | NEW            | **No tasks written** | -     | Only plan description exists    |
| M21: Inventory Fixtures | NEW            | **No tasks written** | -     | Only plan description exists    |

### Requirements Coverage

| Requirement Key                | Has Task? | Status                 | Notes                |
| ------------------------------ | --------- | ---------------------- | -------------------- |
| FR-001 (HL7 support)           | Yes       | M1 DONE                |                      |
| FR-002 (RS232)                 | Yes       | M2 DONE                |                      |
| FR-003 (File import)           | Yes       | M3 DONE                |                      |
| FR-004 (Order export)          | Yes       | M15 PENDING            |                      |
| FR-005 (Message ID)            | Partial   | Scattered across M1-M5 |                      |
| FR-006 (12 analyzers)          | Yes       | M5-M14 mostly done     | M6-M8 status unclear |
| FR-007 (Plugin integration)    | Yes       | M5-M14                 |                      |
| FR-008-011 (Metadata)          | Yes       | M16 PENDING            |                      |
| FR-012-014 (Error handling)    | Partial   | Some in M1-M4          | No dedicated tasks   |
| FR-015-018 (Order export)      | Yes       | M15 PENDING            |                      |
| FR-019-021 (GeneXpert modules) | No        | Post-deadline          | Correctly deferred   |
| FR-022-024 (Maintenance)       | No        | Post-deadline          | Correctly deferred   |
| FR-025-030 (Simulator)         | Yes       | M4+M17                 | M17 PENDING          |

### Metrics

| Metric                        | Value                         |
| ----------------------------- | ----------------------------- |
| Total Requirements (FR)       | 30                            |
| Total Tasks                   | ~296 (T001-T296+)             |
| Milestones Complete           | 10 of 21 (48%)                |
| Milestones Pending            | 8 of 21 (38%)                 |
| Milestones No Tasks Written   | 3 of 21 (14%) - M19, M20, M21 |
| Coverage % (FR with >=1 task) | 87% (26/30)                   |
| Ambiguity Count               | 2                             |
| Duplication Count             | 1                             |
| Critical Issues Count         | 2 (both rebase-related)       |

---

## Section 3: Constitution Alignment

No CRITICAL constitution violations found in the artifacts themselves. The spec
and plan correctly reference:

- Carbon Design System (CR-001)
- React Intl (CR-002)
- 5-layer architecture (CR-003)
- Liquibase (CR-004)
- TDD workflow (CR-008)
- External plugin pattern (enforced throughout)

**One concern**: M19-M21 (Workstream F) were added to `plan.md` but tasks were
never generated via `/speckit.tasks`. This means they have no TDD task ordering,
which conflicts with Constitution Principle V's mandate.

---

## Section 4: Unmapped Tasks

Tasks in Workstream F (M19: T200-T209, M20: T210-T219, M21: T220-T229) are
referenced in the tasks.md header but **never actually written**. The task IDs
collide with M12-M15 task numbering.

---

## Recommended Rebase Strategy

### Option A: Squash-First Rebase (RECOMMENDED)

```bash
# 1. Create backup
git branch backup/feat-011-pre-rebase

# 2. Squash all 7 feature commits into 1
git rebase -i $(git merge-base HEAD origin/demo/madagascar)
# Mark all but first as "squash"

# 3. Rebase the single squashed commit onto latest base
git rebase origin/demo/madagascar

# 4. Resolve conflicts ONCE (not 7 times)

# 5. Verify Liquibase changeset ordering

# 6. Build + test validation
mvn clean install -DskipTests -Dmaven.test.skip=true
cd frontend && npm run build
```

**Why this works**: Since 5 of 7 commits are already cherry-picked to
`demo/madagascar`, the truly unique content is only in 2 commits (`984540bde` +
`c2c6b11cf`). A squash collapses everything into one diff, and git will
auto-resolve most cherry-pick duplicates during rebase.

### Option B: Fresh Branch with Cherry-Pick

```bash
# 1. Create new branch from latest base
git checkout -b feat/011-analyzer-dashboard-fixtures-v2 origin/demo/madagascar

# 2. Cherry-pick only the 2 unique commits
git cherry-pick c2c6b11cf  # Pointer update
git cherry-pick 984540bde  # Circular dependency fix + dashboard fixtures

# 3. Resolve any conflicts from those 2 commits only

# 4. Verify build + tests
mvn clean install -DskipTests -Dmaven.test.skip=true
cd frontend && npm run build
```

**Why this might be better**: The 2 unique commits are well-scoped (dashboard
fixtures + pointer update). Cherry-picking them onto a fresh base avoids all the
duplicate-commit confusion entirely.

---

## Next Actions

1. **CRITICAL (Before any further development)**: Resolve the rebase situation
   using Option A or B above. The branch is 33 commits behind `demo/madagascar`
   with 486 overlapping files.

2. **HIGH**: After rebase, verify the new `demo/madagascar` features (CSV
   analyzer M4, HL7 Universal Bridge M2b) integrate cleanly with feature work.

3. **MEDIUM**: Update `plan.md` milestone statuses - M6, M7, M8, M14 appear
   complete (tasks checked, PRs exist) but plan shows PENDING.

4. **MEDIUM**: Run `/speckit.tasks` for M19, M20, M21 (Workstream F) - these
   milestones have plan descriptions but no task breakdown.

5. **LOW**: Standardize analyzer count (12 vs 13) throughout spec.md.

6. **LOW**: Resolve task ID collision (T200-T239 used by both M12 and Workstream
   F).
