# Tasks: Configurable Reporting MVP

**Inputs**: [spec.md](spec.md), [plan.md](plan.md),
[data-model.md](data-model.md), [contract](contracts/export-api.md),
[acceptance plan](quickstart.md).  
**Status**: Reconciled after clarification. Application implementation has not
started; implementation tasks intentionally remain unchecked.

Use one engine and configured source definitions. Complete useful functionality
first: instance-aware columns, both layouts, every repeated result and shared
saved reports. Existing data access is a background constraint. Each milestone
is one PR. Tests for complex behavior precede implementation. `[P]` means work
can proceed independently after prerequisites, not an instruction to launch
agents.

## M1 — One Engine and a Useful Native Reporting Workflow

**Goal**: Demonstrate US1–US3 with Sample & Testing through the common feature,
plus basic queue return visits from US4. This is a working first slice; M2
completes the other mock source definitions and operational qualification.

- [ ] T001 Create `feat/479-ogc-479-reporting-mvp-m1-result-export` in its own
      worktree; refresh `develop`, confirm the spec PR exists, and read
      `specs/479-reporting-mvp/` plus current configuration/reporting
      conventions.
- [ ] T002 [US1] Build independent fixture oracles and failing source/catalog
      tests under `src/test/java/org/openelisglobal/reports/dataexport/` with
      fixtures in `src/test/resources/`: specimen dates, patient/common
      attributes, corrections, components, dictionary/multi-valued/text values,
      precision and two instance configurations; verify adding/renaming a
      supported configured item without code edits.
- [ ] T003 [US1] Add failing layout/CSV tests under
      `src/test/java/org/openelisglobal/reports/dataexport/` for ordered
      headers/cells, BOM, escaping, nulls, zero rows and both layouts; preserve
      every repeat and compare identity/value multiplicities without
      cross-products or latest-only selection.
- [ ] T004 [US3] Add failing ORM/persistence tests under
      `src/test/java/org/openelisglobal/reports/dataexport/` for source
      references, shared definitions, concurrent edits, immutable job requests,
      submission identity and migration/rollback.
- [ ] T005 [US2] Add focused service/API tests under
      `src/test/java/org/openelisglobal/reports/dataexport/` for valid
      configured requests, existing access, owner-scoped files, idempotency and
      concurrent active-job limits; include a normal report user completing the
      flow without additional setup.
- [ ] T006 [P] [US2] Add component tests in
      `frontend/src/components/reports/CustomDataExport/` for
      configuration-driven fields/filters, defaults, search/order, layout
      switching, retained edits, inline ready download and shared
      save/reopen/copy/update/delete with fresh dates.
- [ ] T007 [P] [US1] Author real native reporting flows in
      `frontend/playwright/tests/foundational/core/custom-data-export.spec.ts`
      using `specs/479-reporting-mvp/quickstart.md`; validate `core-app`
      classification and follow the current Playwright author/audit workflow.
- [ ] T008 [US3] Implement shared-definition/job persistence under
      `src/main/java/org/openelisglobal/reports/dataexport/valueholder/` and
      `dao/`, with registered Liquibase migration and rollback under
      `src/main/resources/liquibase/3.6.x.x/`, making T004 pass.
- [ ] T009 [US1] Implement validated report-source configuration and the common
      catalog under
      `src/main/java/org/openelisglobal/reports/dataexport/service/` using
      current configuration-loading conventions; load instance tests/components
      and supported additional fields by stable identity, without a fixed count
      or report-specific frontend list.
- [ ] T010 [US1] Implement the Sample & Testing source mapping and bounded
      parameterized queries under
      `src/main/java/org/openelisglobal/reports/dataexport/dao/` and `service/`,
      making T002 pass; document proven field/date/component mappings in
      `specs/479-reporting-mvp/data-model.md`.
- [ ] T011 [US1] Implement common spreadsheet and detailed-list formatting under
      `src/main/java/org/openelisglobal/reports/dataexport/service/`, making
      T003 pass; preserve source identities, typed values, repeated results and
      captured labels without extending the legacy Routine CSV writer.
- [ ] T012 [US2] Implement submission, bounded worker/atomic claims, private
      file publication, existing-access checks and download under
      `src/main/java/org/openelisglobal/reports/dataexport/service/`; retain
      ordinary audit, idempotency and admission limits, making T005 pass.
- [ ] T013 [US3] Implement thin common catalog/job and shared-definition
      endpoints/forms under
      `src/main/java/org/openelisglobal/reports/dataexport/controller/` and
      `form/` following `specs/479-reporting-mvp/contracts/export-api.md`; use
      shared instance scope for definitions and owner scope for jobs/files.
- [ ] T014 [US1] Implement the common Carbon builder, source-driven
      fields/filters, both layouts and shared report reuse in
      `frontend/src/components/reports/CustomDataExport/`; wire Reports
      navigation and `frontend/src/languages/en.json`, preserving state and
      confirmations from T006.
- [ ] T015 [US4] Add inline job progress/ready download and the basic personal
      queue using current shared query utilities in
      `frontend/src/components/reports/CustomDataExport/`; wire configured
      limits/protected persistent output through existing deployment conventions
      and document settings in `specs/479-reporting-mvp/quickstart.md`.
- [ ] T016 [US1] Run focused backend, component and real-browser checks from
      `specs/479-reporting-mvp/quickstart.md`: compare both CSV layouts to
      records, verify configuration changes, repeat preservation and reuse by a
      second report user; inspect browser console/screenshots and ordinary
      access-negative cases.
- [ ] T017 Run applicable format/build/coverage checks and document tested
      revision and M1 evidence in `specs/479-reporting-mvp/quickstart.md`;
      confirm useful reporting works before expanding the source definitions.
- [ ] T018 Open the M1 PR to `develop`, linking the spec, UI evidence and actual
      CSV comparisons. Report current required CI results and the remaining M2
      scope; do not merge or deploy as part of this task.

## M2 — Remaining Configurations and Recoverable Delivery

**Goal**: Complete the mock's source coverage using the same feature, complete
US4, and qualify the full workflow. There are no separate report-type
applications.

- [ ] T019 Create `feat/479-ogc-479-reporting-mvp-m2-queue-recovery` in its own
      worktree from the M1 result; refresh branch/PR state and
      `specs/479-reporting-mvp/`.
- [ ] T020 [US1] Add failing fixture tests under
      `src/test/java/org/openelisglobal/reports/dataexport/` for Referral and
      Non-Conformance definitions: occurrence identity, applicable
      dates/statuses, repeated results, event/rejection links and avoiding
      duplicate occurrences; test an additional definition over an existing
      source with no frontend/queue code changes.
- [ ] T021 [US4] Add failing lifecycle tests under
      `src/test/java/org/openelisglobal/reports/dataexport/` for retry lineage,
      queued cancellation, concurrent claims, restart/live-worker isolation,
      expiry/download races, partial-file cleanup and retained audit; add
      recovery browser tests under
      `frontend/playwright/tests/foundational/core/custom-data-export-recovery.spec.ts`.
- [ ] T022 [US1] Add Referral and Non-Conformance source mappings/configured
      definitions using the same feature under
      `src/main/java/org/openelisglobal/reports/dataexport/` and the existing
      resource/configuration locations, making T020 pass; record exact date and
      event-link rules in `specs/479-reporting-mvp/data-model.md`.
- [ ] T023 [US4] Implement failed-job retry and queued-only cancellation through
      the common service/controller paths under
      `src/main/java/org/openelisglobal/reports/dataexport/`, preserving
      immutable requests, source/version, layout, labels, current access and
      lineage.
- [ ] T024 [US4] Implement abandoned-worker recovery, expiry enforcement and
      private-file cleanup under
      `src/main/java/org/openelisglobal/reports/dataexport/service/`, making
      T021 pass without interfering with another live application context.
- [ ] T025 [US4] Implement shared recovery controls and source-appropriate
      labels in `frontend/src/components/reports/CustomDataExport/` and
      `frontend/src/languages/en.json`; retain choices, require fresh dates for
      expired reruns and avoid report-specific screens.
- [ ] T026 [US1] Run the three configured source definitions through the common
      builder/saved-report/download flow; compare real fixture records and
      dates, test additional configuration without code changes, and audit the
      focused browser tests using `specs/479-reporting-mvp/quickstart.md`.
- [ ] T027 [US4] Add/run a reproducible 50,000-result qualification under
      `src/test/java/org/openelisglobal/reports/dataexport/` and
      `src/test/resources/`; record expected counts, batches, memory, duration,
      worker limits and ordinary request behavior in
      `specs/479-reporting-mvp/quickstart.md`.
- [ ] T028 [US4] Verify migration/rollback on empty/populated disposable
      databases, persistent output, restart, retention and cleanup; document
      actual deployment settings/procedures and implementation evidence in
      `specs/479-reporting-mvp/quickstart.md`.
- [ ] T029 Verify every functional requirement and success criterion against
      implementation evidence; update
      `specs/479-reporting-mvp/checklists/requirements.md` and `quickstart.md`
      without conflating document validation, code tests, CI, deployment or user
      acceptance.
- [ ] T030 Run applicable formatter/build/coverage and required CI checks, audit
      the focused Playwright files and prepare the final evidence; do not run
      full E2E suites during ordinary development.
- [ ] T031 Open the M2 PR to `develop`, linking the completed configured-source
      and recovery evidence. State deployment/user-acceptance status separately;
      do not merge or deploy as part of this task.

## Dependencies

```mermaid
graph LR
    T001[Branch and contract] --> T002[Data and configuration oracle]
    T002 --> B[Common backend and source mapping]
    T002 --> U[Shared UI and saved reports]
    B --> V[Real two-layout export]
    U --> V
    V --> M1[M1 PR]
    M1 --> M2[Additional configurations and recovery]
    M2 --> Q[Full MVP qualification and M2 PR]
```

T006/T007 may proceed alongside backend work after the contract and fixture
semantics are established. Coordinate shared files such as `en.json`. M2 depends
on the working M1 engine; separate source definitions do not imply independent
milestones or teams.

## Requirement Coverage

| Requirement | Implementation and verification tasks    |
| ----------- | ---------------------------------------- |
| FR-001      | T005, T013–T016                          |
| FR-002      | T002, T006, T009, T013, T014, T016       |
| FR-003      | T003, T006, T011, T014, T016             |
| FR-004      | T002, T005, T010, T014, T020, T022, T026 |
| FR-005      | T002, T009, T010, T014, T020, T022, T026 |
| FR-006      | T002, T003, T010, T011, T014, T016       |
| FR-007      | T006, T007, T014–T016                    |
| FR-008      | T006, T014, T015, T021, T025             |
| FR-009      | T005, T012, T013, T016, T021, T023       |
| FR-010      | T004, T005, T008, T012, T021, T023       |
| FR-011      | T007, T008, T013, T015, T021, T025       |
| FR-012      | T003, T011, T012, T016, T021, T024       |
| FR-013      | T005, T012, T013, T016, T021, T024       |
| FR-014      | T021, T023, T025, T028                   |
| FR-015      | T012, T015, T021, T024, T027, T028       |
| FR-016      | T021, T024, T025, T028                   |
| FR-017      | T005, T012, T021, T023, T025             |
| FR-018      | T005, T012, T021, T023, T024, T028       |
| FR-019      | T002, T006, T009, T013, T014, T016       |
| FR-020      | T005–T007, T014–T016                     |
| FR-021      | T004, T006, T008, T013, T014, T016       |
| FR-022      | T009, T013, T014, T020, T022, T026       |

CR-001–CR-005 are covered by the architecture, migration, test and milestone
tasks. SC-001–SC-003 and SC-007/008 are demonstrated in T016; SC-004 in
T021/T028; SC-005 in T016/T017/T026/T030; SC-006 in T027; and SC-009 in
T020/T026. T029 checks the complete evidence map.

Personal-library/sharing administration, arbitrary source discovery, external
integrations, scheduling and separate synchronous generation are outside this
task list. Shared saved reports, patient information under existing access and
the mock's three source definitions are included.
