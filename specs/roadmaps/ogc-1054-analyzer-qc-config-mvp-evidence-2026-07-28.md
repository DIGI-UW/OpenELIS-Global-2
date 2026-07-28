# OGC-1054 Analyzer QC/Config MVP Evidence

**Status:** Pending acceptance closure
**Pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)

This file is intentionally incomplete until C1-C3 are implemented and the exact
PR build is deployed. Historical June/July recordings are available through git
history but do not accept current HEAD.

## Required Build Provenance

| Field | Value |
| --- | --- |
| Application repository | `DIGI-UW/OpenELIS-Global-2` |
| Application branch | `codex/ogc-1054-analyzer-qc-mvp` |
| Application SHA | Pending |
| Harness repository | `DIGI-UW/openelis-review-tooling` |
| Harness SHA | Pending |
| Instance | `analyzers` |
| Deployment time | Pending |
| Checklist revision | Pending |

## Automated Validation

| Gate | Command/evidence | Result |
| --- | --- | --- |
| Focused backend | Controller/service JUnit 4, including real Postgres filter contract | Pass: latest targeted runs |
| Backend package | `mvn -Dtest=org.openelisglobal.analyzer.**,org.openelisglobal.qc.** test` | Pass: 875 tests |
| Backend format | `mvn spotless:check` | Pass |
| Focused frontend | `npm test -- --run src/components/analyzers src/components/qc src/services/analyzerService.test.ts` | Pass: 115 tests, 5 skipped |
| Frontend build | `npm run build` | Pass; retained repository-wide CSS/chunk warnings are outside this feature |
| Frontend quality | `npm run check-format`; `npm run lint`; focused source ESLint | Pass for supported/focused gates |
| Playwright guard | `npm run pw:guard`; `harness-demo` discovery | Pass |
| Code QA | [alignment/coverage/simplicity/cross-repo record](../OGC-1054-analyzer-qc-config/checklists/code-qa-2026-07-28.md) | Pass below remote evidence; bundle pending |

## Remote UAT

| Step | Status | Route | Evidence |
| --- | --- | --- | --- |
| `AN-QC-001` | Pending | Pending | Pending |
| `AN-QC-002` | Pending | Pending | Pending |
| `AN-QC-003` | Pending | Pending | Pending |
| `AN-QC-005` | Pending | Pending | Pending |
| `AN-QC-006` | Pending | Pending | Pending |
| `AN-QC-007` | Pending | Pending | Pending |
| `AN-QC-004` | Pending | Pending | Pending |
| `AN-QC-008` | Pending | Pending | Pending |

## Visual Review

- Desktop screenshot comparison with `openelis-work@4c0e1a28`: Pending.
- Mobile screenshot comparison with `openelis-work@4c0e1a28`: Pending.
- Console, trace, and runtime-state inspection: Pending.
- H.264 MP4 and downloaded Markdown/JSON report: Pending.

No row may be changed to Pass without evidence from the exact build recorded
above.
