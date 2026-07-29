# OGC-1054 Analyzer QC/Config MVP Evidence

**Status:** Accepted on the analyzer UAT instance
**Pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)

This record accepts the application tree deployed and reviewed on 2026-07-28.
Historical June/July recordings remain available through git history but are
not part of this acceptance.

## Required Build Provenance

| Field                   | Value                                                              |
| ----------------------- | ------------------------------------------------------------------ |
| Application repository  | `DIGI-UW/OpenELIS-Global-2`                                        |
| Application branch      | `codex/ogc-1054-analyzer-qc-mvp`                                   |
| Application SHA         | `2c840a55b03b238a2ad00c987181504c2bef6ef6`                         |
| Harness repository      | `DIGI-UW/openelis-review-tooling`                                  |
| Harness SHA             | `b0e51c982a82ad967d1359e382633006907d780d`                         |
| Instance                | `analyzers`                                                        |
| Deployment ID           | `20260728T205914Z-2c840a55b03b`                                    |
| Deployment time         | `2026-07-28T20:59:46Z`                                             |
| Checklist revision      | `96a1e8b9c801bec7e4eeca93b7599168dbbfbb176a8b6ecbc37e64d6b28c1c83` |
| Deployment verification | Health pass; smoke pass; schema-affecting false                    |

## Automated Validation

| Gate             | Command/evidence                                                                                                   | Result                                                                     |
| ---------------- | ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------- |
| Focused backend  | Controller/service JUnit 4, including real Postgres filter contract                                                | Pass: latest targeted runs                                                 |
| Backend package  | `mvn -Dtest=org.openelisglobal.analyzer.**,org.openelisglobal.qc.** test`                                          | Pass: 875 tests                                                            |
| Backend format   | `mvn spotless:check`                                                                                               | Pass                                                                       |
| Focused frontend | analyzer/QC Vitest/RTL slice                                                                                       | Pass: 117 tests, 5 skipped                                                 |
| Frontend build   | `npm run build`                                                                                                    | Pass; retained repository-wide CSS/chunk warnings are outside this feature |
| Frontend quality | `npm run check-format`; `npm run lint`; focused source ESLint                                                      | Pass for supported/focused gates                                           |
| Playwright guard | `npm run pw:guard`; `harness-demo` discovery                                                                       | Pass                                                                       |
| Non-video story  | remote `harness-demo`                                                                                              | Pass: UI setup and complete story                                          |
| Video story      | remote `harness-demo-video`                                                                                        | Pass: 50.8-second recording                                                |
| Code QA          | [alignment/coverage/simplicity/cross-repo record](../OGC-1054-analyzer-qc-config/checklists/code-qa-2026-07-28.md) | Pass; H.264 evidence bundle generated                                      |

## Remote UAT

| Step        | Status | Route                                             | Evidence                                              |
| ----------- | ------ | ------------------------------------------------- | ----------------------------------------------------- |
| `AN-QC-001` | Pass   | `/analyzers/types?protocol=HL7`                   | Profile protocol, counts, readiness, and setup action |
| `AN-QC-002` | Pass   | `/analyzers?add=1&step=instrument&profile=...`    | Inline profile-driven creation                        |
| `AN-QC-003` | Pass   | `/analyzers/340/mappings?setup=1&step=verify...`  | Deterministic mappings and blockers                   |
| `AN-QC-004` | Pass   | `/analyzers/340/edit?setup=1&step=connect...`     | Visible saved-analyzer connection result              |
| `AN-QC-005` | Pass   | `/analyzers/331/mappings`                         | Catalog-bound pending value persisted after reload    |
| `AN-QC-006` | Pass   | `/analyzers/340/qc-rules` plus control-lot detour | Active rule and lot created through visible UI        |
| `AN-QC-007` | Pass   | `/analyzers/340/mappings?setup=1&step=verify...`  | Blocked-before/current-after verification             |
| `AN-QC-008` | Pass   | `/analyzers/340/review?setup=1&step=review...`    | ACTIVE summary, connection, and verifier              |

## Visual Review

- Desktop and mapping screenshots were compared with
  `openelis-work@4c0e1a28a6904617f29a812c3a07b4a15e95d862`. The implementation
  retains the profile -> verify -> connect information architecture while
  using the current Carbon page shell, data tables, notifications, and linked
  breadcrumbs.
- The 390 x 844 implementation keeps navigation, filters, and text readable;
  the profile table scrolls horizontally. The pinned prototype itself clips
  its navigation and main content at narrow width and is retained as design
  intent, not a pixel target.
- All named screenshots and sampled video frames were inspected. The final
  frame shows `ACTIVE`, current verification, and the non-contradictory
  `Analyzer active` outcome.
- The service-worker URL is origin-rooted and returned HTTP 200 on the accepted
  build. Playwright diagnostics still contain canceled fetches during
  intentional route changes and pre-existing subscription/anonymous 404 noise;
  no analyzer action returned a server error or failed the story. The live
  overlay report captured no step-local console errors.

## Evidence Artifacts

The binary evidence remains outside git as required:

- folder: `ogc-1054-acceptance-2026-07-28`;
- archive: `ogc-1054-acceptance-2026-07-28.zip`;
- MP4: H.264, yuv420p, 800 x 450, 50.8 seconds;
- report: original Markdown download plus extracted schema-version 2 JSON;
- trace: passing non-video story trace with screenshots, DOM, and network events;
- provenance: `build.json` and `uat-analyzers.json`.

## Tooling Disposition

- [`openelis-review-tooling` PR #2](https://github.com/DIGI-UW/openelis-review-tooling/pull/2)
  merged on 2026-07-25. Current `main` uses
  `ANALYZERS_DIR=/opt/oe-analyzers`; the stale
  `/opt/openelis-analyzers` value encountered during acceptance was retained
  host configuration. Acceptance used the seed script from the deployed
  checkout.
- The widget intentionally downloads one Markdown file with embedded structured
  JSON to avoid Chrome's automatic-download permission prompt. The evidence
  bundle preserves that original report and an extracted JSON companion.
  Some review-tooling sample/documentation copy still describes a two-file
  pair, but no unmerged tooling implementation is required for this MVP.
