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
- [ ] T012 Extend service-created UAT data so M4 has mapped and unmapped finalized cases without SQL or fixed persisted IDs.

## Phase 3 - Frontend

- [ ] T013 Add failing canonical-query tests for `from`, `to`, `significance`, `dedup`, `step`, `page`, and `pageSize`.
- [ ] T014 Add failing RTL tests using Carbon roles/labels for configure, preview, repair, pagination, and download.
- [ ] T015 Build the Carbon WHONET page, API adapter, route, breadcrumb path, and config-backed sidenav item.
- [ ] T016 Add English React Intl source strings only.
- [ ] T017 Add explicit return context to M3 mapping repair links without breaking existing reference URLs.

## Phase 4 - E2E, UAT, And Evidence

- [ ] T018 Add a focused Playwright `core-app` journey with role/label interactions and download assertions; use no arbitrary waits.
- [ ] T019 Add M4 Grist UAT stories while retaining all required M1-M3 stories.
- [ ] T020 Verify the live UAT JSON revision after deployment.
- [ ] T021 Capture stable desktop/mobile Configure and Preview screenshots and compare them with M-09.
- [ ] T022 Record a standardized MP4 walkthrough and contact sheets after deployment.

## Phase 5 - Validation And PR

- [ ] T023 Run focused backend, frontend, accessibility, Playwright, ORM, and Liquibase validation.
- [ ] T024 Run pinned `tools/code-qa` alignment, coverage, simplicity, and evidence checks.
- [ ] T025 Synchronize the parent roadmap/spec status without overclaiming deferred M-09 scope.
- [ ] T026 Commit, push, open the stacked draft PR against M3, and publish exact evidence and check state.
