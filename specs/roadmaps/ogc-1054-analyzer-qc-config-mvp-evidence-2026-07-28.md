# OGC-1054 Analyzer QC/Configuration Historical Foundation Evidence

**Recorded:** 2026-07-28
**Reclassified:** 2026-08-13
**Status:** Historical foundation evidence; not OGC-1054 MVP acceptance
**Pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)

The filename is retained for link and git provenance. The July review accepted
the eight-step Analyzer QC/configuration foundation described below against one
exact build. Subsequent code and scope validation established that it did not
exercise reusable Analyzer Types, the complete mapping editor, production
unknown-traffic capture/hold, or mock-to-Bridge result delivery. It therefore
cannot accept the full OGC-1054 MVP.

The current feature and acceptance definition is
[the authoritative OGC-1054 roadmap](ogc-1054-analyzer-feature-roadmap.md).

## Source Boundary

The `openelis-work` screenshots used during the visual review were functional
and visual references only. They did not and do not supply implementation,
architecture, persistence, API, route, ownership, or test-design directives.

## Reviewed Build

| Field | Value |
| --- | --- |
| Application repository | `DIGI-UW/OpenELIS-Global-2` |
| Application branch | `codex/ogc-1054-analyzer-qc-mvp` |
| Application SHA | `2c840a55b03b238a2ad00c987181504c2bef6ef6` |
| Harness repository | `DIGI-UW/openelis-review-tooling` |
| Harness SHA | `b0e51c982a82ad967d1359e382633006907d780d` |
| Instance | `analyzers` |
| Deployment ID | `20260728T205914Z-2c840a55b03b` |
| Deployment time | `2026-07-28T20:59:46Z` |
| Checklist revision | `96a1e8b9c801bec7e4eeca93b7599168dbbfbb176a8b6ecbc37e64d6b28c1c83` |
| Deployment verification | Health pass; smoke pass; schema-affecting false |

This application SHA is older than current branch HEAD and the review manifest
does not identify Bridge, analyzer-mock, or profile catalog revisions. It is not
a current integrated deployment record.

## Historical Automated Validation

| Gate | Evidence | July result |
| --- | --- | --- |
| Focused backend | Controller/service JUnit 4, including a real Postgres filter contract | Pass |
| Backend package | `mvn -Dtest=org.openelisglobal.analyzer.**,org.openelisglobal.qc.** test` | Pass: 875 tests |
| Backend format | `mvn spotless:check` | Pass |
| Focused frontend | Analyzer/QC Vitest/RTL slice | Pass: 117 tests, 5 skipped |
| Frontend build/quality | Build, format, lint, and focused source checks | Pass with recorded repository-wide warnings |
| Playwright guard | Foundation story rejected API access, forced controls, response polling, and arbitrary waits | Pass |
| Non-video/video story | Remote foundation setup story and 50.8-second MP4 | Pass |
| Code QA | Historical alignment/coverage/simplicity/evidence record | Pass for branch-defined scope |

These checks are stale for current PR HEAD and do not cover the Bridge/mock
contracts required by the authoritative roadmap.

## Historical UAT Story

| Step | July status | Demonstrated foundation behavior |
| --- | --- | --- |
| `AN-QC-001` | Pass | Inspect a shipped profile-file summary |
| `AN-QC-002` | Pass | Create an analyzer through the inline entry |
| `AN-QC-003` | Pass | Review copied mappings and branch-defined blockers |
| `AN-QC-004` | Pass | See a saved-analyzer connection-test result |
| `AN-QC-005` | Pass | Resolve a seeded qualitative pending value with a catalog option |
| `AN-QC-006` | Pass | Configure an operational QC rule and control-lot detour |
| `AN-QC-007` | Pass | See blocked then satisfied branch-defined readiness |
| `AN-QC-008` | Pass | Review the completed branch-defined analyzer summary |

The pending value was a fixture precondition, not evidence that production
Bridge traffic creates and holds pending values. The story also did not send a
known/unknown ASTM, HL7, or FILE result through analyzer mock -> Bridge -> FHIR
-> OpenELIS.

## Historical Visual Review

Desktop and mobile output was compared with the then-current functional/visual
mock intent. The branch demonstrated a coherent Carbon shell, linked
breadcrumbs, route state, readable setup progression, and responsive branch
surfaces. This comparison did not prove the missing Analyzer Type lifecycle,
complete editor, Alerts/Needs attention, or traffic-learning workflows and did
not determine any technical implementation choice.

## Retained Artifacts

Binary evidence remains outside git as originally recorded:

- folder and archive: `ogc-1054-acceptance-2026-07-28`;
- MP4: H.264, yuv420p, 800 x 450, 50.8 seconds;
- report: Markdown plus extracted schema-version 2 JSON;
- trace: passing foundation story trace;
- provenance: the recorded `build.json` and `uat-analyzers.json`.

These artifacts are useful regression references. They must not be attached to
a current PR as full MVP evidence.

## Replacement Evidence Required

G0 in the authoritative roadmap requires a new integrated deployment and the
`AN-MVP-001` through `AN-MVP-015` checklist, with OpenELIS, Bridge,
analyzer-mock, profile catalog, and review-tooling revisions in the build
manifest. Only that gate may establish OGC-1054 MVP acceptance.
