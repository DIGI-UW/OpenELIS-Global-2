# Roadmap: Analyzer-Independent QC, Results Entry, and Validation

**Date**: 2026-06-28
**Spec folder**: `specs/427-811-qc-results-entry-roadmap/`
**Branch**: `codex/qc-results-entry-roadmap`

## Decision

Proceed with an analyzer-independent sequence:

1. OGC-427 Batch Workplan with Reagent QC.
2. Shared reagent/QC/NCE contract.
3. OGC-811 Results Entry v4 MVP.
4. OGC-817 Validation v4 companion.
5. OGC-899 PNG/CPHL cross-domain evidence rollup.

Do not fold in OGC-1054. OGC-1054 is already active analyzer profile/mapping
work and remains a reference/future data source only.

## Canonical Artifacts

- [Specification](../427-811-qc-results-entry-roadmap/spec.md)
- [Implementation Plan](../427-811-qc-results-entry-roadmap/plan.md)
- [Tasks](../427-811-qc-results-entry-roadmap/tasks.md)
- [Quickstart](../427-811-qc-results-entry-roadmap/quickstart.md)
- [Evidence Template](../427-811-qc-results-entry-roadmap/contracts/evidence-template.md)
- [Requirements Checklist](../427-811-qc-results-entry-roadmap/checklists/requirements.md)

## Milestones

| ID | Scope | Primary Jira |
| --- | --- | --- |
| M0 | Roadmap/spec baseline | OGC-427/811/817/899 |
| M1 | Batch Workplan foundation | OGC-427 |
| M2 | Reagent lot and shared QC contract | OGC-427, OGC-811 |
| M3 | Inline QC, NCE override, printable workplan | OGC-427 |
| M4 | Results Entry v4 worklist and edit-state | OGC-811 |
| M5 | Results Entry bench controls | OGC-811 |
| M6 | Validation risk triage and guarded release | OGC-817 |
| M7 | PNG/CPHL cross-domain evidence | OGC-899 |

## Evidence Standard

Each UI milestone requires:

- Pinned mock baseline from `digi-uw/openelis-work`
  `6c24b6ee04aab4ecef96e8daa57dc88ae8821356`.
- Mock screenshots and implementation screenshots at matching viewports.
- A written mock-comparison note.
- A Playwright MP4 from the repo demo-video project.
- Code-qa validation note.
- PR body with validation commands and evidence links.

## Next Action

Review M0 artifacts, then start M1 from a new clean worktree based on
`origin/develop`:

`feat/427-811-qc-results-m1-batch-foundation`
