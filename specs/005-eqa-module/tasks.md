# Tasks: External Quality Assurance (EQA) Module

**Input**: Design documents from `/specs/005-eqa-module/` **Prerequisites**:
plan.md (required), spec.md (required), research.md, data-model.md,
contracts/eqa-api.yaml

**Tests**: Tests are **MANDATORY** per Constitution Principle V. TDD workflow:
write tests FIRST, verify they FAIL, then implement.

**Organization**: Tasks are grouped by milestone → user story to align with the
PR strategy (each milestone = 1 PR). Within each milestone, tasks follow:
Liquibase → Entities → DAOs → Services → Controllers → Frontend → Tests.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (e.g., US1, US2) or milestone scope
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `src/main/java/org/openelisglobal/`
- **Liquibase**: `src/main/resources/liquibase/3.3.x.x/`
- **Backend Tests**: `src/test/java/org/openelisglobal/`
- **Frontend**: `frontend/src/components/`
- **Frontend Tests**: `frontend/src/components/**/__tests__/`
- **E2E Tests**: `frontend/cypress/e2e/`
- **i18n**: `frontend/src/languages/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project structure and Liquibase migration files **Milestone**: M1
(Backend Entities)

- [x] T001 Create EQA package directory structure under
      `src/main/java/org/openelisglobal/eqa/` with subdirectories:
      `valueholder/`, `dao/`, `daoimpl/`, `service/`, `controller/rest/`,
      `scheduler/`
- [x] T002 [P] Create Liquibase changeset
      `src/main/resources/liquibase/3.3.x.x/eqa-001-create-eqa-program-tables.xml`
      for `eqa_program` and `eqa_program_test` tables with sequences, indexes,
      and unique constraints per data-model.md
- [x] T003 [P] Create Liquibase changeset
      `src/main/resources/liquibase/3.3.x.x/eqa-002-create-eqa-distribution-tables.xml`
      for `eqa_distribution` table with sequence, indexes, FK constraints per
      data-model.md
- [x] T004 [P] Create Liquibase changeset
      `src/main/resources/liquibase/3.3.x.x/eqa-003-create-sample-eqa-table.xml`
      for `sample_eqa` table with sequence, indexes, FK to `sample(id)` UNIQUE
      constraint per data-model.md
- [x] T005 [P] Create Liquibase changeset
      `src/main/resources/liquibase/3.3.x.x/eqa-004-create-eqa-result-table.xml`
      for `eqa_result` table with sequence, indexes, unique constraint
      `(eqa_distribution_id, participant_organization_id, test_id)` per
      data-model.md
- [x] T006 Register all EQA changesets in master Liquibase changelog (find
      existing `include` pattern in `src/main/resources/liquibase/`)

---

## Phase 2: Foundational — M1 Backend Entities & Core Services (Blocking)

**Purpose**: All JPA entities, enums, DAOs, and base services. MUST complete
before any user story frontend or controller work. **Milestone**: M1 —
`feat/005-eqa-module-m1-backend-entities` **User Stories**: US1, US2, US3, US4
(foundational for all)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Enums

- [x] T007 [P] Create `EQAPriority` enum (STANDARD, URGENT, CRITICAL) in
      `src/main/java/org/openelisglobal/eqa/valueholder/EQAPriority.java`
- [x] T008 [P] Create `EQADistributionStatus` enum (DRAFT, PREPARED, SHIPPED,
      COMPLETED) in
      `src/main/java/org/openelisglobal/eqa/valueholder/EQADistributionStatus.java`
- [x] T009 [P] Create `EQASubmissionMethod` enum (FHIR, MANUAL, FILE_UPLOAD) in
      `src/main/java/org/openelisglobal/eqa/valueholder/EQASubmissionMethod.java`
- [x] T010 [P] Create `EQAPerformanceStatus` enum (ACCEPTABLE, QUESTIONABLE,
      UNACCEPTABLE) in
      `src/main/java/org/openelisglobal/eqa/valueholder/EQAPerformanceStatus.java`
- [x] T011 Extend existing `AlertType` enum with new values: `EQA_DEADLINE`,
      `SAMPLE_EXPIRATION`, `STAT_UPCOMING`, `STAT_OVERDUE`,
      `CRITICAL_UNACKNOWLEDGED` in
      `src/main/java/org/openelisglobal/alert/valueholder/AlertType.java`

### Entities (JPA Annotations per data-model.md)

- [x] T012 [P] Create `EQAProgram` entity in
      `src/main/java/org/openelisglobal/eqa/valueholder/EQAProgram.java` —
      extends `BaseObject<Long>`, with `@Entity`, `@Table`,
      `@SequenceGenerator`, `fhirUuid` with `@PrePersist`, fields: name,
      description, isActive per data-model.md
- [x] T013 [P] Create `EQAProgramTest` entity in
      `src/main/java/org/openelisglobal/eqa/valueholder/EQAProgramTest.java` —
      extends `BaseObject<Long>`, `@UniqueConstraint(eqa_program_id, test_id)`,
      `@ManyToOne` to EQAProgram, testId as Long per data-model.md
- [x] T014 [P] Create `EQADistribution` entity in
      `src/main/java/org/openelisglobal/eqa/valueholder/EQADistribution.java` —
      extends `BaseObject<Long>`, with `fhirUuid`,
      `@Enumerated(EnumType.STRING)` for status, `@ManyToOne` to EQAProgram and
      SystemUser per data-model.md
- [x] T015 [P] Create `EQAResult` entity in
      `src/main/java/org/openelisglobal/eqa/valueholder/EQAResult.java` —
      extends `BaseObject<Long>`, with `fhirUuid`, `BigDecimal` for
      resultValue/targetValue/zScore,
      `@UniqueConstraint(eqa_distribution_id, participant_organization_id, test_id)`
      per data-model.md
- [x] T016 [P] Create `SampleEQA` entity in
      `src/main/java/org/openelisglobal/eqa/valueholder/SampleEQA.java` —
      extends `BaseObject<Long>`, sampleId as Long (avoids cross-mapping),
      `@ManyToOne` to EQAProgram/EQADistribution, `@Enumerated` for priority per
      data-model.md

### DAOs

- [x] T017 [P] Create `EQAProgramDAO` interface in
      `src/main/java/org/openelisglobal/eqa/dao/EQAProgramDAO.java` extending
      `BaseDAO<EQAProgram, Long>`
- [x] T018 [P] Create `EQAProgramDAOImpl` in
      `src/main/java/org/openelisglobal/eqa/daoimpl/EQAProgramDAOImpl.java`
      extending `BaseDAOImpl<EQAProgram, Long>`
- [x] T019 [P] Create `EQAProgramTestDAO` + `EQAProgramTestDAOImpl` in
      `src/main/java/org/openelisglobal/eqa/dao/EQAProgramTestDAO.java` and
      `src/main/java/org/openelisglobal/eqa/daoimpl/EQAProgramTestDAOImpl.java`
- [x] T020 [P] Create `EQADistributionDAO` + `EQADistributionDAOImpl` in
      `src/main/java/org/openelisglobal/eqa/dao/EQADistributionDAO.java` and
      `src/main/java/org/openelisglobal/eqa/daoimpl/EQADistributionDAOImpl.java`
- [x] T021 [P] Create `EQAResultDAO` + `EQAResultDAOImpl` in
      `src/main/java/org/openelisglobal/eqa/dao/EQAResultDAO.java` and
      `src/main/java/org/openelisglobal/eqa/daoimpl/EQAResultDAOImpl.java`
- [x] T022 [P] Create `SampleEQADAO` + `SampleEQADAOImpl` in
      `src/main/java/org/openelisglobal/eqa/dao/SampleEQADAO.java` and
      `src/main/java/org/openelisglobal/eqa/daoimpl/SampleEQADAOImpl.java` —
      include `findBySampleId()`, `findByDeadlineBefore()`, `findByProgramId()`
      queries

### Services

- [x] T023 [P] Create `EQAProgramService` interface + `EQAProgramServiceImpl` in
      `src/main/java/org/openelisglobal/eqa/service/EQAProgramService.java` and
      `EQAProgramServiceImpl.java` — `@Service`, `@Transactional`, CRUD +
      findActive + test assignment management
- [x] T024 [P] Create `EQADistributionService` interface +
      `EQADistributionServiceImpl` in
      `src/main/java/org/openelisglobal/eqa/service/EQADistributionService.java`
      and `EQADistributionServiceImpl.java` — CRUD + status transitions +
      participant validation (min 2)
- [x] T025 [P] Create `EQAResultService` interface + `EQAResultServiceImpl` in
      `src/main/java/org/openelisglobal/eqa/service/EQAResultService.java` and
      `EQAResultServiceImpl.java` — submit result + duplicate handling + late
      submission logic + batch import
- [x] T026 [P] Create `SampleEQAService` interface + `SampleEQAServiceImpl` in
      `src/main/java/org/openelisglobal/eqa/service/SampleEQAService.java` and
      `SampleEQAServiceImpl.java` — create/find SampleEQA + deadline queries +
      validation (program required when isEqa=true)
- [x] T027 Create `EQAStatisticsService` interface + `EQAStatisticsServiceImpl`
      in
      `src/main/java/org/openelisglobal/eqa/service/EQAStatisticsService.java`
      and `EQAStatisticsServiceImpl.java` — Z-score calculation (BigDecimal),
      mean, SD, performance classification, min 5 participant check per
      research.md R5

### Tests (M1 Checkpoint)

- [x] T028 Create ORM validation test
      `src/test/java/org/openelisglobal/eqa/EQAHibernateMappingValidationTest.java`
      — validate all 5 entities load, <5s, no DB connection (Constitution V.4)
- [x] T029 [P] Create `EQAStatisticsServiceTest` in
      `src/test/java/org/openelisglobal/eqa/service/EQAStatisticsServiceTest.java`
      — JUnit 4 + `@RunWith(MockitoJUnitRunner.class)`, test Z-score
      calculation, performance classification
      (Acceptable/Questionable/Unacceptable), min 5 participant validation, SD=0
      edge case
- [x] T030 [P] Create `EQAProgramServiceTest` in
      `src/test/java/org/openelisglobal/eqa/service/EQAProgramServiceTest.java`
      — test CRUD, activation/deactivation, test assignment
- [x] T031 [P] Create `EQADistributionServiceTest` in
      `src/test/java/org/openelisglobal/eqa/service/EQADistributionServiceTest.java`
      — test creation, status transitions, min 2 participant validation
- [x] T032 [P] Create `EQAResultServiceTest` in
      `src/test/java/org/openelisglobal/eqa/service/EQAResultServiceTest.java` —
      test submission, duplicate handling, late submission logic, validation

**Checkpoint**: ORM validation tests + all unit tests pass.
`mvn test -Dtest="org.openelisglobal.eqa.**"`

---

## Phase 3: US1 — Register and Process Incoming EQA Samples (P1) 🎯 MVP

**Goal**: Lab technicians can register EQA samples through modified sample
entry, see EQA badges in work queues, and filter EQA samples. **Milestone**: M2
— `feat/005-eqa-module-m2-eqa-sample-frontend` (parallel with M1) **Independent
Test**: Register an EQA sample through sample entry, verify EQA badge in work
queue, filter EQA samples.

### i18n

- [x] T033 [P] [US1] Add EQA i18n keys to `frontend/src/languages/en.json` —
      keys: `eqa.sample.checkbox`, `eqa.sample.badge`, `eqa.program.label`,
      `eqa.provider.label`, `eqa.provider.sampleId`, `eqa.participant.id`,
      `eqa.deadline.label`, `eqa.priority.*` per quickstart.md
- [x] T034 [P] [US1] Add EQA i18n keys to `frontend/src/languages/fr.json` —
      French translations for all keys added in T033

### Frontend Tests (Write FIRST — must FAIL)

- [x] T035 [P] [US1] Create Jest test
      `frontend/src/components/eqa/__tests__/EQASampleEntry.test.jsx` — test:
      EQA checkbox disables demographics, EQA fields appear on Program tab,
      required field validation (provider, program, deadline), priority
      selection
- [x] T036 [P] [US1] Create Jest test
      `frontend/src/components/eqa/__tests__/EQABadge.test.jsx` — test: renders
      Carbon Tag with EQA label, applies correct priority color

### Frontend Implementation

- [x] T037 [US1] Create `EQASampleEntry` component in
      `frontend/src/components/eqa/EQASampleEntry.js` — Carbon Checkbox for EQA
      toggle, Carbon Select for program/provider/priority, Carbon DatePicker for
      deadline, Carbon TextInput for provider sample ID and participant ID
      (FR-001, FR-002, FR-003)
- [x] T038 [US1] Create `EQABadge` component in
      `frontend/src/components/eqa/EQABadge.js` — reusable Carbon Tag component
      with EQA label and priority-based color (blue=standard, orange=urgent,
      red=critical) (FR-005, UI-003)
- [x] T039 [US1] Modify `frontend/src/components/addOrder/Index.js` to integrate
      EQA checkbox on PatientInfo tab — when checked, disable demographics with
      "N/A" placeholders (FR-001, FR-002)
- [x] T040 [US1] Modify program selection tab
      (`frontend/src/components/addOrder/OrderEntryAdditionalQuestions.js`) to
      show EQA-specific fields when EQA is selected — provider org, program
      dropdown, sample ID, participant ID, deadline, priority (FR-003, FR-004)
- [x] T041 [US1] Modify work queue/logbook component to display EQABadge on EQA
      samples and add filter toggle for EQA-only view (FR-005, FR-006, UI-004)
- [x] T042 [US1] Extend `sampleOrderItems` state shape in sample entry workflow
      to include EQA fields: `isEQASample`, `eqaProgramId`,
      `eqaProviderOrganizationId`, `eqaProviderSampleId`, `eqaParticipantId`,
      `eqaDeadline`, `eqaPriority` per research.md R9
- [x] T043 [US1] Modify backend `SamplePatientEntryController` (or equivalent)
      to extract EQA fields from request and create `SampleEQA` record via
      `SampleEQAService` when `isEQASample=true`

### E2E Test

- [ ] T044 [US1] Create Cypress E2E test
      `frontend/cypress/e2e/eqaSampleEntry.cy.js` — test full EQA sample
      registration workflow: check EQA box → fill EQA fields → save → verify
      badge in work queue → filter EQA samples (Constitution V.5: individual
      execution, data-testid, cy.session)

**Checkpoint**: Jest + E2E tests pass. EQA sample can be registered and appears
in work queue with badge.

---

## Phase 4: US2 + US6 — Monitor EQA Deadlines and Comprehensive Alerts (P1/P2)

**Goal**: Automated alert generation at 72h/24h/4h intervals, centralized alerts
dashboard with filtering, acknowledgment workflow. **Milestones**: M3 (alerts
backend) + M4 (alerts frontend) **Independent Test**: Register EQA samples with
various deadlines, verify alert generation, view dashboard, acknowledge alerts.

### M3: Alerts Backend — `feat/005-eqa-module-m3-alerts-backend`

- [x] T045 [US2] Create `EQADeadlineAlertScheduler` in
      `src/main/java/org/openelisglobal/eqa/scheduler/EQADeadlineAlertScheduler.java`
      — `@Service` with `@Scheduled(fixedDelay=300000)`, query SampleEQA
      deadlines, generate alerts at 72h (INFO), 24h (WARNING), 4h (CRITICAL)
      intervals, call existing `AlertService.createAlert()` with dedup (FR-008,
      FR-009, FR-010, FR-011)
- [x] T046 [US6] Add sample expiration alert logic to
      `EQADeadlineAlertScheduler` — generate alerts at 7d (INFO), 2d (WARNING),
      1d (CRITICAL) for expiring samples (FR-041)
- [x] T047 [US6] Add STAT order alert logic to scheduler — generate alerts at
      50% (INFO), 75% (WARNING), 100% (CRITICAL) of STAT target time (FR-042)
- [x] T048 [US2] Add alert escalation logic to scheduler — check alerts with
      age >4 hours and status=OPEN, escalate severity, increment notification
      badge (FR-045, SC-012)
- [x] T049 [US2] Create or extend `EQAAlertRestController` in
      `src/main/java/org/openelisglobal/eqa/controller/rest/EQAAlertRestController.java`
      — GET `/alerts/dashboard` with filtering (type, severity, labSection,
      myAlerts, search, pagination), GET `/alerts/dashboard/summary` per
      eqa-api.yaml
- [x] T050 [US2] Extend alert acknowledgment endpoint — ensure PUT
      `/alerts/{id}/acknowledge` requires resolution comment for critical alerts
      and logs user ID + timestamp (FR-043, FR-044)

### M3 Tests

- [x] T051 [P] [US2] Create `EQADeadlineAlertSchedulerTest` in
      `src/test/java/org/openelisglobal/eqa/scheduler/EQADeadlineAlertSchedulerTest.java`
      — JUnit 4, test alert generation at 72h/24h/4h thresholds, test escalation
      at 4h, test dedup prevention, test STAT/expiration alerts
- [x] T052 [P] [US2] Create `EQAAlertRestControllerTest` in
      `src/test/java/org/openelisglobal/eqa/controller/EQAAlertRestControllerTest.java`
      — MockMvc, test dashboard endpoint with filters, test summary endpoint,
      test acknowledgment with required comment

**Checkpoint (M3)**: Alert scheduler generates correct alerts. Dashboard REST
endpoints return filtered data.
`mvn test -Dtest="org.openelisglobal.eqa.scheduler.**,org.openelisglobal.eqa.controller.EQAAlert**"`

### M4: Alerts Dashboard Frontend — `feat/005-eqa-module-m4-alerts-frontend`

- [x] T053 [P] [US6] Add alerts dashboard i18n keys to
      `frontend/src/languages/en.json` — keys: `alerts.dashboard.title`,
      `alerts.summary.*`, `alerts.acknowledge.*`, `alerts.filter.*` per
      quickstart.md
- [x] T054 [P] [US6] Add alerts dashboard i18n keys to
      `frontend/src/languages/fr.json` — French translations

### M4 Frontend Tests (Write FIRST)

- [x] T055 [P] [US6] Create Jest test
      `frontend/src/components/alerts/__tests__/AlertsDashboard.test.jsx` —
      test: summary tiles render counts, table renders with severity colors,
      filter by type/severity/myAlerts, pagination, auto-refresh interval
- [x] T056 [P] [US6] Create Jest test
      `frontend/src/components/alerts/__tests__/AlertAcknowledgeModal.test.jsx`
      — test: modal opens, comment required for critical, submit calls API

### M4 Frontend Implementation

- [x] T057 [US6] Create `AlertSummaryTiles` component in
      `frontend/src/components/alerts/AlertSummaryTiles.js` — 4 Carbon Tile
      components: Critical Alerts (red), EQA Deadlines Today, Overdue STAT
      Orders, Samples Expiring (FR-035, UI-008)
- [x] T058 [US6] Create `AlertsTable` component in
      `frontend/src/components/alerts/AlertsTable.js` — Carbon DataTable with
      columns: Type (icon+text), Severity (color-coded tag), Message, Lab
      Section, Due Date, Lab Number, Assigned To, Actions. Severity colors:
      Red=Critical, Orange=High, Yellow=Medium, Blue=Low (FR-036, FR-040,
      UI-009)
- [x] T059 [US6] Create `AlertAcknowledgeModal` component in
      `frontend/src/components/alerts/AlertAcknowledgeModal.js` — Carbon Modal
      with required TextArea for resolution comment, submit calls PUT
      `/alerts/{id}/acknowledge` (FR-043, UI-012)
- [x] T060 [US6] Create `AlertsDashboard` page component in
      `frontend/src/components/alerts/AlertsDashboard.js` — integrates
      AlertSummaryTiles + AlertsTable + filters (type, severity, "My Alerts"
      checkbox, search TextInput) + pagination (25/50/100/200) + auto-refresh
      every 60s (FR-037, FR-038, FR-039, FR-046, FR-047)
- [x] T061 [US6] Add route for alerts dashboard in frontend router and add
      navigation menu item with notification badge count

### M4 E2E Test

- [ ] T062 [US2] Create Cypress E2E test
      `frontend/cypress/e2e/alertsDashboard.cy.js` — test: dashboard loads with
      summary tiles, filter by type/severity, "My Alerts" filter, search by lab
      number, acknowledge critical alert with comment (Constitution V.5)

**Checkpoint (M4)**: Dashboard displays alerts with correct severity colors,
filtering works, acknowledgment requires comment. Jest + E2E pass.

---

## Phase 5: US3 — Create and Distribute EQA Samples (P2)

**Goal**: EQA coordinators can create distributions, select participants,
generate barcodes, organize shipments. **Milestone**: M5 —
`feat/005-eqa-module-m5-distribution` **Depends On**: M1 (entities)
**Independent Test**: Create distribution for 3+ organizations, verify sample
records created, barcodes generated, status transitions work.

### Backend

- [x] T063 [US3] Create `EQADistributionRestController` in
      `src/main/java/org/openelisglobal/eqa/controller/rest/EQADistributionRestController.java`
      — POST `/eqa/distributions` (create with participants), GET
      `/eqa/distributions` (list with filters), GET `/eqa/distributions/{id}`
      (detail with participants), PUT `/eqa/distributions/{id}/status` (status
      transition), POST `/eqa/distributions/{id}/barcodes` per eqa-api.yaml
      (FR-014 through FR-021)
- [x] T064 [US3] Implement batch sample creation in `EQADistributionServiceImpl`
      — for each participant org, create Sample + SampleEQA + SampleItem records
      with identical test assignments from program (FR-016, FR-017)
- [x] T065 [US3] Integrate barcode generation in `EQADistributionServiceImpl` —
      call existing `BarcodeLabelMaker`/`BarcodeInformationService` for each
      distribution sample (FR-019)
- [x] T066 [US3] Implement status transition validation in
      `EQADistributionServiceImpl` — enforce DRAFT→PREPARED→SHIPPED→COMPLETED,
      prevent finalization with <2 participants (FR-018)

### Backend Tests

- [x] T067 [P] [US3] Create `EQADistributionRestControllerTest` in
      `src/test/java/org/openelisglobal/eqa/controller/EQADistributionRestControllerTest.java`
      — MockMvc tests for all distribution endpoints, test min 2 participant
      validation, test status transitions

### Frontend

- [x] T068 [P] [US3] Add distribution i18n keys to
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json` —
      keys: `eqa.distribution.create`, `eqa.distribution.name`,
      `eqa.distribution.participants`, `eqa.distribution.status.*`
- [x] T069 [US3] Create `CreateDistribution` wizard in
      `frontend/src/components/eqa/EQADistribution/CreateDistribution.js` —
      Carbon ProgressIndicator (3 steps: Program & Details → Participants →
      Confirmation), multi-select organizations, set deadline (FR-014, FR-015,
      UI-007)
- [x] T070 [US3] Create `DistributionList` in
      `frontend/src/components/eqa/EQADistribution/DistributionList.js` — Carbon
      DataTable with status badges, filter by program/status
- [x] T071 [US3] Create `DistributionDetail` in
      `frontend/src/components/eqa/EQADistribution/DistributionDetail.js` — show
      participants, sample accession numbers, result status, barcode generation
      button, status advancement buttons

### Frontend Tests

- [x] T072 [P] [US3] Create Jest test
      `frontend/src/components/eqa/EQADistribution/__tests__/CreateDistribution.test.jsx`
      — test wizard steps, min 2 participant validation, form submission

### E2E Test

- [ ] T073 [US3] Create Cypress E2E test
      `frontend/cypress/e2e/eqaDistribution.cy.js` — test full distribution
      creation workflow: select program → add participants → confirm → generate
      barcodes → advance status

**Checkpoint (M5)**: Distribution can be created, samples generated per
participant, barcodes work, status transitions enforced. All tests pass.

---

## Phase 6: US4 — Collect Results and Analyze EQA Performance (P2)

**Goal**: Manual result entry, CSV batch import, Z-score calculation,
performance classification, PDF reports. **Milestone**: M6 —
`feat/005-eqa-module-m6-results-analysis` **Depends On**: M1, M5 **Independent
Test**: Submit 5+ results via manual entry and CSV, verify Z-scores calculated,
performance classified, PDF report generates in <5s.

### Backend

- [x] T074 [US4] Create `EQAResultRestController` in
      `src/main/java/org/openelisglobal/eqa/controller/rest/EQAResultRestController.java`
      — POST `/eqa/distributions/{id}/results` (manual submit), POST
      `/eqa/distributions/{id}/results/import` (CSV batch), GET
      `/eqa/distributions/{id}/results` (results with stats), GET
      `/eqa/distributions/{id}/statistics`, GET `/eqa/distributions/{id}/report`
      (PDF) per eqa-api.yaml (FR-022 through FR-030)
- [x] T075 [US4] Implement CSV/Excel batch import in `EQAResultServiceImpl` —
      parse file, validate rows (org ID, test ID, result value), report errors
      per row, create EQAResult records (FR-024)
- [x] T076 [US4] Implement automatic statistics trigger in
      `EQAResultServiceImpl` — after result submission, check if >=5
      participants, if so call `EQAStatisticsService` to calculate and update
      Z-scores/performance for all results in distribution (FR-025, FR-026,
      FR-027)
- [x] T077 [US4] Implement PDF report generation — investigate existing report
      infrastructure (JasperReports or OpenPDF per research.md R6), generate
      report with participant results, Z-scores, performance classification,
      comparative analysis (FR-029, SC-006)

### Backend Tests

- [x] T078 [P] [US4] Create integration test for result submission + statistics
      calculation in
      `src/test/java/org/openelisglobal/eqa/controller/EQAResultRestControllerTest.java`
      — test manual submit, batch import, statistics endpoint, report generation

### Frontend

- [x] T079 [P] [US4] Add results i18n keys to `frontend/src/languages/en.json`
      and `frontend/src/languages/fr.json` — keys: `eqa.results.manual.entry`,
      `eqa.results.batch.import`, `eqa.results.statistics`,
      `eqa.results.zscore`, `eqa.results.acceptable/questionable/unacceptable`,
      `eqa.results.report.generate`, `eqa.results.minimum.participants`
- [x] T080 [US4] Create `ManualResultEntry` in
      `frontend/src/components/eqa/EQAResults/ManualResultEntry.js` — Carbon
      form with org select, test select, result value input, submit button,
      validation feedback (FR-023)
- [x] T081 [US4] Create `BatchImport` in
      `frontend/src/components/eqa/EQAResults/BatchImport.js` — Carbon
      FileUploader for CSV/Excel, validation summary display with error rows,
      confirm import button (FR-024)
- [x] T082 [US4] Create `StatisticsDisplay` in
      `frontend/src/components/eqa/EQAResults/StatisticsDisplay.js` — Carbon
      StructuredList showing mean, SD, target value, participant count,
      per-participant Z-scores with color-coded performance status
      (green/yellow/red), warning message when <5 participants (FR-025, FR-026,
      FR-027, FR-028, UI-010)
- [x] T083 [US4] Create `PerformanceReport` in
      `frontend/src/components/eqa/EQAResults/PerformanceReport.js` — "Generate
      Report" button that calls GET `/eqa/distributions/{id}/report` and
      triggers PDF download (FR-029, UI-011)

### Frontend + E2E Tests

- [x] T084 [P] [US4] Create Jest test
      `frontend/src/components/eqa/EQAResults/__tests__/StatisticsDisplay.test.jsx`
      — test rendering with valid data, warning when <5 participants, correct
      color coding for performance status
- [ ] T085 [US4] Create Cypress E2E test `frontend/cypress/e2e/eqaResults.cy.js`
      — test: manual entry → batch import → view statistics → generate PDF
      report

**Checkpoint (M6)**: Results can be submitted via manual entry and CSV.
Statistics calculate when >=5 participants. PDF report generates. All tests
pass.

---

## Phase 7: US5 + US7 — FHIR Submission & EQA Program Config Admin (P3)

**Goal**: Submit results to external providers via FHIR, late submission
workflow, program management admin screen. **Milestone**: M7 —
`feat/005-eqa-module-m7-submission-fhir` **Depends On**: M1, M6 **Independent
Test**: Submit results via FHIR to external provider, test late submission
approval. Create/edit/deactivate EQA programs.

### Backend — FHIR Submission (US5)

- [x] T086 [US5] Create FHIR submission service in
      `src/main/java/org/openelisglobal/eqa/service/EQAFhirSubmissionService.java`
      and `EQAFhirSubmissionServiceImpl.java` — build DiagnosticReport +
      Observation FHIR R4 bundle from EQA results, submit via existing
      `FhirPersistanceService`, log audit trail (FR-022, FR-031, FR-034)
- [x] T087 [US5] Implement late submission logic in
      `EQAFhirSubmissionServiceImpl` — check deadline, if past return 403 with
      `supervisorApprovalRequired=true`, implement approve-late endpoint
      requiring supervisor role + justification (FR-032)
- [x] T088 [US5] Add submission endpoints to `EQAResultRestController` or new
      controller — POST `/eqa/samples/{sampleId}/submit`, POST
      `/eqa/samples/{sampleId}/submit/approve-late` per eqa-api.yaml

### Backend — Program Admin (US7)

- [x] T089 [US7] Create `EQAProgramRestController` in
      `src/main/java/org/openelisglobal/eqa/controller/rest/EQAProgramRestController.java`
      — GET/POST `/eqa/programs`, GET/PUT `/eqa/programs/{id}`, GET/PUT
      `/eqa/programs/{id}/tests` per eqa-api.yaml (FR-048 through FR-053)

### Backend Tests

- [x] T090 [P] [US5] Create test for FHIR submission in
      `src/test/java/org/openelisglobal/eqa/service/EQAFhirSubmissionServiceTest.java`
      — test FHIR bundle creation, late submission detection, supervisor
      approval flow
- [x] T091 [P] [US7] Create `EQAProgramRestControllerTest` in
      `src/test/java/org/openelisglobal/eqa/controller/EQAProgramRestControllerTest.java`
      — test CRUD endpoints, test assignment, deactivation preserves references

### Frontend — Submission (US5)

- [x] T092 [US5] Create `ResultSubmission` in
      `frontend/src/components/eqa/EQASubmission/ResultSubmission.js` — submit
      button for FHIR or manual method, late submission warning with supervisor
      approval workflow, submission audit display

### Frontend — Program Admin (US7)

- [x] T093 [US7] Create `ProgramManagement` in
      `frontend/src/components/eqa/EQAProgram/ProgramManagement.js` — Carbon
      DataTable listing all programs with status, "Create Program" button,
      edit/deactivate actions (FR-048, FR-049)
- [x] T094 [US7] Create `ProgramForm` in
      `frontend/src/components/eqa/EQAProgram/ProgramForm.js` — Carbon Modal
      form for create/edit: name (required), description, active toggle,
      test/panel assignment via multi-select from existing catalog (FR-050,
      FR-051, FR-052)

### Frontend Tests

- [x] T095 [P] [US7] Create Jest test
      `frontend/src/components/eqa/EQAProgram/__tests__/ProgramManagement.test.jsx`
      — test list rendering, create modal, edit, deactivation

**Checkpoint (M7)**: FHIR submission works, late submission requires approval.
Program CRUD works with test assignment. All tests pass.

---

## Phase 8: IC/QC — Internal Control and Quality Control Features (P2 Extended)

**Goal**: Levey-Jennings charts, Westgard rules engine, QC comments, electronic
signatures, instrument QC frequency. **Milestone**: M8 —
`feat/005-eqa-module-m8-qc-ic` **Depends On**: M6 **Independent Test**: Record
QC results, see LJ chart, Westgard violations flagged, sign QC report, configure
instrument QC frequency.

### Backend — QC Entities & Services

- [x] T096 Create QC Liquibase changeset
      `src/main/resources/liquibase/3.3.x.x/eqa-005-create-qc-tables.xml` —
      tables: `qc_rule_config` (per test type), `qc_frequency_config` (per
      instrument), `qc_signature` (electronic signatures) with sequences and
      indexes
- [x] T097 [P] Create QC entities in
      `src/main/java/org/openelisglobal/qc/valueholder/` — `QCRuleConfig.java`
      (ruleCode, testTypeId, enabled), `QCFrequencyConfig.java` (instrumentId,
      frequencyType, frequencyValue), `QCSignature.java` (reportId, userId,
      timestamp, ipAddress, comment)
- [x] T098 [P] Create QC DAOs in `src/main/java/org/openelisglobal/qc/dao/` and
      `daoimpl/` — `QCRuleConfigDAO`, `QCFrequencyConfigDAO`, `QCSignatureDAO`
      with implementations
- [x] T099 Create `WestgardRuleEngine` service in
      `src/main/java/org/openelisglobal/qc/service/WestgardRuleEngine.java` —
      Strategy pattern with `WestgardRule` interface, 6 concrete implementations
      (Rule12s, Rule13s, Rule22s, RuleR4s, Rule41s, Rule10x), evaluates QC
      results against enabled rules per test type (FR-029c, FR-029d, FR-029e)
- [x] T100 Create `QCService` in
      `src/main/java/org/openelisglobal/qc/service/QCService.java` and
      `QCServiceImpl.java` — record QC result, evaluate Westgard rules,
      calculate chart data (mean, SD, control limits), manage comments, generate
      alerts on violations (FR-029a, FR-029b, FR-029j, FR-029k, FR-029q)
- [x] T101 Create `QCSignatureService` in
      `src/main/java/org/openelisglobal/qc/service/QCSignatureService.java` and
      `QCSignatureServiceImpl.java` — sign report (extract user from session, IP
      from request), list signatures per report (FR-029f, FR-029g, FR-029h,
      FR-029i)
- [x] T102 Create `QCFrequencyService` in
      `src/main/java/org/openelisglobal/qc/service/QCFrequencyService.java` and
      `QCFrequencyServiceImpl.java` — get/update frequency config per
      instrument, check compliance metrics, generate alerts when frequency
      requirements unmet (FR-029n, FR-029o, FR-029p, FR-029q, FR-029r)

### Backend — QC Controllers

- [x] T103 Create `QCRestController` in
      `src/main/java/org/openelisglobal/qc/controller/rest/QCRestController.java`
      — all QC endpoints per eqa-api.yaml: GET/PUT
      `/qc/instruments/{id}/frequency`, GET `/qc/instruments/{id}/compliance`,
      GET `/qc/westgard-rules`, GET/PUT
      `/qc/westgard-rules/config/{testTypeId}`, GET/POST
      `/qc/controls/{id}/results`, GET `/qc/controls/{id}/comments`, POST
      `/qc/reports/{id}/sign`, GET `/qc/reports/{id}/signatures`

### Backend Tests

- [x] T104 [P] Create `WestgardRuleEngineTest` in
      `src/test/java/org/openelisglobal/qc/service/WestgardRuleEngineTest.java`
      — test each of 6 rules individually with known datasets, test
      enable/disable per test type
- [x] T105 [P] Create `QCServiceTest` in
      `src/test/java/org/openelisglobal/qc/service/QCServiceTest.java` — test QC
      result recording, chart data calculation, comment management, alert
      generation on violation

### Frontend — QC Components

- [x] T106 [P] Add QC i18n keys to `frontend/src/languages/en.json` and
      `frontend/src/languages/fr.json` — keys: `qc.leveyJennings.title`,
      `qc.westgard.rules`, `qc.signature.sign`, `qc.frequency.config`
- [x] T107 Create `LeveyJenningsChart` in
      `frontend/src/components/qc/LeveyJenningsChart.js` — @carbon/charts-react
      LineChart with mean, +/-2SD, +/-3SD horizontal lines, data points with
      hover tooltips showing QC comments, Westgard violation markers (FR-029a,
      FR-029b, FR-029l)
- [x] T108 Create `WestgardRulesDisplay` in
      `frontend/src/components/qc/WestgardRulesDisplay.js` — display
      active/inactive rules per test type, toggle enable/disable, show violation
      history (FR-029c, FR-029d)
- [x] T109 Create `QCComments` in `frontend/src/components/qc/QCComments.js` —
      comment entry on IC measurement, searchable comment history per control
      material (FR-029j, FR-029k, FR-029m)
- [x] T110 Create `ElectronicSignature` in
      `frontend/src/components/qc/ElectronicSignature.js` — Carbon Modal with
      optional comment TextArea + "Sign Report" button, display signed
      signatures on QC report (FR-029f, FR-029g, FR-029h, FR-029i)
- [x] T111 Create `QCFrequencyConfig` in
      `frontend/src/components/qc/QCFrequencyConfig.js` — instrument-level QC
      frequency configuration (Daily startup, Per shift, Every N samples, Every
      N hours, Manual trigger only), compliance metrics display (FR-029n,
      FR-029o, FR-029r)

### Frontend Tests

- [x] T112 [P] Create Jest test
      `frontend/src/components/qc/__tests__/LeveyJenningsChart.test.jsx` — test
      chart renders with control limits, data points display, hover tooltips
      show comments
- [x] T113 [P] Create Jest test
      `frontend/src/components/qc/__tests__/ElectronicSignature.test.jsx` — test
      modal opens, sign action calls API, signatures display on report

**Checkpoint (M8)**: LJ charts render, Westgard rules evaluate correctly, QC
comments work, e-signatures recorded, frequency config saves. All tests pass.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, security hardening, performance validation

- [x] T114 Implement deadline enforcement — prevent EQA result modification
      after deadline without supervisor approval across all entry points
      (FR-007, FR-032)
- [x] T115 [P] Implement result value validation — validate numeric results
      against test-specific biologically plausible ranges during all submission
      methods (FR-031)
- [x] T116 [P] Implement duplicate result handling — overwrite with complete
      audit trail (original value, original timestamp, new value, new timestamp,
      user) (FR-033)
- [x] T117 Add RBAC for EQA coordinator role — verify `sys_role` permissions on
      distribution creation and program management endpoints (CR-007)
- [x] T118 Verify zero patient data in EQA records — audit all EQA sample
      records ensure demographics show "N/A" (SC-014)
- [x] T119 [P] Run `mvn spotless:apply` for backend formatting
- [x] T120 [P] Run `cd frontend && npm run format` for frontend formatting
- [x] T121 Performance validation — verify alert dashboard <2s for 200 alerts
      (SC-007), stats calculation <2s for 50 participants (SC-005), PDF
      generation <5s for 50 participants (SC-006)
- [x] T122 Run full backend test suite
      `mvn test -Dtest="org.openelisglobal.eqa.**,org.openelisglobal.qc.**"` —
      verify >80% coverage via JaCoCo
- [x] T123 Run full frontend test suite `cd frontend && npm test` — verify >70%
      coverage
- [x] T124 Run quickstart.md validation — follow all user scenarios in
      quickstart.md manually to verify end-to-end functionality

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (M1 Foundational)**: Depends on Phase 1 — **BLOCKS all user
  stories**
- **Phase 3 (US1/M2)**: Can start in **PARALLEL** with Phase 2 (frontend only,
  no backend dependency for UI shell)
- **Phase 4 (US2+US6/M3+M4)**: M3 depends on M1. M4 depends on M3 + M2.
- **Phase 5 (US3/M5)**: Depends on M1
- **Phase 6 (US4/M6)**: Depends on M1 + M5
- **Phase 7 (US5+US7/M7)**: Depends on M1 + M6
- **Phase 8 (M8 QC/IC)**: Depends on M6
- **Phase 9 (Polish)**: Depends on all desired phases being complete

### Milestone Dependency Graph

```
Phase 1 (Setup) → Phase 2 (M1: Entities) ──→ Phase 4a (M3: Alerts Backend) ──→ Phase 4b (M4: Alerts Frontend)
                                    │                                                    ↑
Phase 3 (M2: EQA Sample Frontend) ─┤────────────────────────────────────────────────────┘
                                    │
                                    ├──→ Phase 5 (M5: Distribution) ──→ Phase 6 (M6: Results) ──→ Phase 7 (M7: FHIR/Config)
                                    │                                          │
                                    │                                          └──→ Phase 8 (M8: QC/IC)
                                    │
                                    └──→ Phase 9 (Polish) [after all desired phases]
```

### User Story Independence

- **US1** (P1): Phase 2 + Phase 3 — independently testable after these phases
- **US2** (P1): Phase 2 + Phase 4a (M3) — independently testable (backend alerts
  only)
- **US6** (P2): Phase 2 + Phase 4a + Phase 4b — independently testable with
  dashboard
- **US3** (P2): Phase 2 + Phase 5 — independently testable
- **US4** (P2): Phase 2 + Phase 5 + Phase 6 — independently testable
- **US5** (P3): Phase 2 + Phase 6 + Phase 7 — independently testable
- **US7** (P3): Phase 2 + Phase 7 — independently testable

### Within Each Phase

- Liquibase changesets before entities
- Enums before entities
- Entities before DAOs
- DAOs before services
- Services before controllers
- Tests written FIRST (must FAIL), then implementation
- Backend before frontend (for API availability)

### Parallel Opportunities

- **Phase 2**: All enum tasks (T007-T010) in parallel, all entity tasks
  (T012-T016) in parallel, all DAO tasks (T017-T022) in parallel, all service
  tasks (T023-T026) in parallel, all test tasks (T029-T032) in parallel
- **Phase 2 + Phase 3**: M1 (backend) and M2 (frontend) can be developed
  simultaneously
- **Phase 4**: M3 tests (T051-T052) in parallel, M4 frontend tests (T055-T056)
  in parallel
- **Phase 5 + Phase 4**: M5 (distribution) and M3/M4 (alerts) can proceed in
  parallel after M1
- **Phase 8**: QC entity tasks (T097) in parallel, QC frontend tests (T112-T113)
  in parallel

---

## Parallel Examples

### Phase 2 Entity Creation (5 entities simultaneously)

```bash
Task: "Create EQAProgram entity in src/main/java/org/openelisglobal/eqa/valueholder/EQAProgram.java"
Task: "Create EQAProgramTest entity in src/main/java/org/openelisglobal/eqa/valueholder/EQAProgramTest.java"
Task: "Create EQADistribution entity in src/main/java/org/openelisglobal/eqa/valueholder/EQADistribution.java"
Task: "Create EQAResult entity in src/main/java/org/openelisglobal/eqa/valueholder/EQAResult.java"
Task: "Create SampleEQA entity in src/main/java/org/openelisglobal/eqa/valueholder/SampleEQA.java"
```

### Phase 3 Frontend Tests + i18n (parallel)

```bash
Task: "Add EQA i18n keys to frontend/src/languages/en.json"
Task: "Add EQA i18n keys to frontend/src/languages/fr.json"
Task: "Create Jest test frontend/src/components/eqa/__tests__/EQASampleEntry.test.jsx"
Task: "Create Jest test frontend/src/components/eqa/__tests__/EQABadge.test.jsx"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup (Liquibase)
2. Complete Phase 2: M1 Backend Entities (blocking)
3. Complete Phase 3: US1 EQA Sample Entry (frontend)
4. **STOP and VALIDATE**: Register EQA sample, see badge in work queue
5. Deploy/demo if ready — lab technicians can register EQA samples

### P1 Complete (US1 + US2)

1. MVP above +
2. Complete Phase 4: US2/US6 Alerts (M3 backend + M4 frontend)
3. **VALIDATE**: Alerts generate at correct intervals, dashboard works
4. Deploy — labs can register EQA samples AND monitor deadlines

### P2 Complete (US1-US4, US6)

1. P1 above +
2. Complete Phase 5: US3 Distribution (M5)
3. Complete Phase 6: US4 Results & Analysis (M6)
4. Complete Phase 8: QC/IC (M8) — if IC requirements are in scope
5. **VALIDATE**: Full EQA cycle: register → distribute → collect → analyze

### Full Feature (All User Stories)

1. P2 above +
2. Complete Phase 7: US5/US7 FHIR & Config (M7)
3. Complete Phase 9: Polish
4. **VALIDATE**: FHIR submission, program config, security, performance

---

## Notes

- [P] tasks = different files, no dependencies — can run simultaneously
- [Story] label maps task to specific user story for traceability
- All tests use JUnit 4 + Mockito 2.21.0 (NOT JUnit 5)
- All frontend components use Carbon Design System (@carbon/react) exclusively
- All UI strings via React Intl (no hardcoded text)
- E2E tests run individually during development (Constitution V.5)
- `mvn spotless:apply` + `cd frontend && npm run format` before every commit
- `mvn clean install -DskipTests -Dmaven.test.skip=true` for fast backend
  rebuild
