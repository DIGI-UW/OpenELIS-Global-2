# Deterministic Tasks: M3 Reference and Mapping Administration

## Phase 1 - Contract and TDD

- [x] T001 Create clean milestone worktree and branch from deployed M2 SHA.
- [x] T002 Reconcile Jira, openelis-work, and current repo state in `research.md`.
- [x] T003 Lock behavior-only acceptance criteria in `spec.md`.
- [ ] T004 Add failing service tests for organism/antibiotic validation, uniqueness,
  deactivation impact, and reactivation.
- [ ] T005 Add failing service/integration tests for immutable AST panel versions.
- [ ] T006 Add failing breakpoint lifecycle/activation tests, including one active
  standard per publisher and historical-run preservation.
- [ ] T007 Add failing CSV preview/apply tests for mixed-validity rows,
  idempotency, local customization protection, and transaction rollback.
- [ ] T008 Add failing controller tests for admin authorization and
  authenticated-actor derivation.
- [ ] T009 Extend ORM and Liquibase update/rollback tests before implementation.

## Phase 2 - Backend

- [ ] T010 Add the M9 Liquibase changelog with rollback for accepted model fields,
  panel versions, breakpoint lifecycle/import metadata, and activation audit.
- [ ] T011 Extend existing valueholders and add activation-event valueholder.
- [ ] T012 Add typed DAO queries and natural-key/reference-impact checks.
- [ ] T013 Implement compiled admin list/detail DTOs and query forms.
- [ ] T014 Implement `MicrobiologyReferenceAdminService` validation and writes.
- [ ] T015 Implement immutable AST panel version publication.
- [ ] T016 Implement breakpoint activation and archive safeguards.
- [ ] T017 Implement CSV preview, failed-row download, and valid-row application.
- [ ] T018 Add admin REST contracts; preserve existing workflow lookup contracts.
- [ ] T019 Extend service-created UAT fixtures with synthetic M3 records.

## Phase 3 - Carbon Administration UI

- [ ] T020 Add shared route/query utilities and unit tests.
- [ ] T021 Add config-driven Admin sidenav sections and route tests.
- [ ] T022 Build shared Carbon reference-admin table and status components.
- [ ] T023 Build organism and antibiotic list/edit/deactivate workflows.
- [ ] T024 Build versioned AST panel editor with ordered tier/report behavior.
- [ ] T025 Build culture-Method configuration surface without a duplicate Method
  vocabulary.
- [ ] T026 Build breakpoint standard list, rule detail, activation, and archive UI.
- [ ] T027 Build CSV preview/results/error-download UI.
- [ ] T028 Add linkable PageBreadCrumb paths and canonical URL preservation.
- [ ] T029 Add English i18n keys and focused component/accessibility tests.

## Phase 4 - E2E, UAT, and Publication

- [ ] T030 Register focused Playwright `core-app` projects/tests before browser runs.
- [ ] T031 Prove organism/antibiotic edits and deactivation behavior.
- [ ] T032 Prove AST panel versioning and historical-run preservation.
- [ ] T033 Prove breakpoint activation and mixed-validity import.
- [ ] T034 Prove canonical URL reload, Back/Forward, sidenav, and breadcrumbs.
- [ ] T035 Publish M3 Grist stories while retaining all M1/M2 stories.
- [ ] T036 Run focused backend, ORM, migration, component, Playwright, formatting,
  and `git diff --check` validation.
- [ ] T037 Run pinned `tools/code-qa` alignment, coverage, simplicity, and evidence
  reviews and record outputs.
- [ ] T038 Commit and push coherent checkpoints promptly; open the stacked draft PR.
- [ ] T039 Deploy the exact M3 SHA to AMR, verify app/schema metadata, and rerun the
  registered live UAT contract and M3 Playwright journey.
- [ ] T040 Update the PR, Jira, roadmap, UAT revision, screenshots, and external
  walkthrough/contact-sheet links without claiming human acceptance.
