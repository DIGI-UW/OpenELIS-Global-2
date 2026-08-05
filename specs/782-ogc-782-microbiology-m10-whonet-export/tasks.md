# Tasks: OGC-782 M4 WHONET Manual Export

## Phase 1 - Contract And Tests

- [x] T001 Ground scope in OGC-794/878/880, Confluence v2.1, M-09, and M3 repo state.
- [x] T002 Record implementation leakage, contradictions, defaults, and deferred decisions.
- [x] T003 Add failing dataset tests for final date selection, significance, de-duplication, used-set mappings, and multiple AST readings.
- [x] T004 Add failing report tests for CSV escaping, blocked generation, audit fingerprint, and authenticated actor.
- [x] T005 Add failing controller tests for preview, download headers, authorization, and invalid input.
- [x] T006 Add failing Liquibase update/rollback and ORM registration tests.

## Phase 2 - Backend

- [x] T007 Add export-run audit migration, entity, DAO, and persistence registration.
- [x] T008 Add a bulk finalized-case query and microbiology WHONET dataset compiler.
- [x] T009 Extend `WHONetReportService` with preview and generate behavior.
- [x] T010 Reuse the existing CSV row/builder contract for microbiology rows.
- [x] T011 Add authenticated preview/generate REST endpoints and structured errors.
- [x] T012 Extend service-created UAT data so M4 has mapped and unmapped finalized cases without SQL or fixed persisted IDs.

## Phase 3 - Frontend

- [x] T013 Add failing canonical-query tests for `from`, `to`, `significance`, `dedup`, `step`, `page`, and `pageSize`.
- [x] T014 Add failing RTL tests using Carbon roles/labels for configure, preview, repair, pagination, and download.
- [x] T015 Build the Carbon WHONET page, API adapter, route, breadcrumb path, and config-backed sidenav item.
- [x] T016 Add English React Intl source strings only.
- [x] T017 Open exact M3 mapping records while retaining preview context in browser history.

## Phase 4 - E2E, UAT, And Evidence

- [x] T018 Add a focused Playwright `core-app` journey with role/label interactions and download assertions; use no arbitrary waits.
- [x] T019 Add M4 Grist UAT stories while retaining all required M1-M3 stories. The live source contains story `AMR-S14`, required steps `AMR-45` through `AMR-50`, and PR #3984.
- [x] T020 Verify the live UAT JSON revision after deployment. Grist and `amr.openelis-global.org/__review/uat-amr.json` are byte-identical at revision `90a25a2ee19e0282611845eca163159e84cc2bbfe55309ebf0d77d6ec7edea43`: 14 stories, 38 required steps, and one optional TB reflection.
- [x] T021 Capture stable desktop/mobile Configure and Preview screenshots and compare them with M-09. The Carbon implementation preserves the mock's compact Configure/Preview/Generate hierarchy, selected-set counts, warning/repair path, and workflow order without exposing its unavailable auto-map, scheduling, FHIR, profile-packaging, or standards claims. Mobile evidence also proves the page does not overflow and its horizontal table region is keyboard-focusable.
- [x] T022 Record a standardized MP4 walkthrough and contact sheet after deployment. The inspected H.264/yuv420p walkthrough, six screenshots, contact sheet, narrative index, manifest, and zip are under `/tmp/ogc-782-m4-evidence-f57064ec5b4f/` and `/tmp/ogc-782-m4-evidence-f57064ec5b4f.zip`; external binary attachment remains part of T026.

## Phase 5 - Validation And PR

- [x] T023 Run focused backend, frontend, accessibility, Playwright, ORM, and Liquibase validation. Local and exact-SHA deployed evidence is recorded in `evidence/validation-2026-08-04.md`.
- [x] T024 Run pinned `tools/code-qa` alignment, coverage, simplicity, and evidence checks. Findings and the explicit inversion proof are recorded in `evidence/code-qa-2026-08-04.md`.
- [x] T025 Synchronize the parent roadmap/spec status without overclaiming deferred M-09 scope. The current long-format CSV remains a validation candidate, not a complete WHONET compatibility claim.
- [ ] T026 Finish publication on stacked draft PR #3984. The branch is pushed, AMR runs exact head `f57064ec5b4f2f797eee3566938cb69efaa79022`, and the PR/check state is documented; attaching the binary bundle is blocked because the only available GitHub web session is signed out. Human UAT remains open.
- [x] T027 Track removal of the remaining legacy Reports export path without widening M4: [GitHub #3983](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3983).
