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

## Phase 4 - E2E And Visual Validation

- [x] T018 Add a focused Playwright `core-app` journey with role/label interactions and download assertions; use no arbitrary waits.
- [x] T021 Capture stable desktop/mobile Configure and Preview screenshots and compare them with M-09. The Carbon implementation preserves the mock's compact Configure/Preview/Generate hierarchy, selected-set counts, warning/repair path, and workflow order without exposing its unavailable auto-map, scheduling, FHIR, profile-packaging, or standards claims. Mobile evidence also proves the page does not overflow and its horizontal table region is keyboard-focusable.
- [x] T022 Keep generated screenshots, traces, and walkthrough media as external
      review artifacts rather than committed feature files.

## Phase 5 - Validation And PR

- [x] T023 Run focused backend, frontend, accessibility, Playwright, ORM, and Liquibase validation.
- [x] T024 Run one pinned `tools/code-qa` alignment, coverage, and simplicity review; resolve actionable findings without committing generated reports.
- [x] T025 Synchronize the parent roadmap/spec status without overclaiming deferred M-09 scope. The current long-format CSV remains a validation candidate, not a complete WHONET compatibility claim.
- [x] T027 Track removal of the remaining legacy Reports export path without widening M4: [GitHub #3983](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3983).
