# Tasks: OGC-782 Microbiology MVP Workflow

**Input**: Design documents from
`/specs/782-ogc-782-microbiology-mvp-spec/`
**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`,
`contracts/microbiology-openapi.yaml`, `quickstart.md`

**Tests**: Mandatory. Each implementation phase starts with failing tests or
test plans before implementation. Runtime Playwright evidence is required for
UI work and for final MVP acceptance. The implementation must also use the
`DIGI-UW/code-qa` skill suite for meaningful test coverage, spec-code alignment,
simplicity review, and evidence bundling.

**Organization**: The original routine-bacteriology execution was consolidated
into PR #3789. Clinical completeness, reference administration, WHONET export,
and one full authoritative-alignment remediation PR are now an official linear
follow-on stack. R1 includes implementation drift and roadmap/spec/task/UAT
drift across the complete microbiology stack. The phase history below is
retained for traceability and maps back to the user stories in `spec.md`.

## Format: `[ID] [P?] [Milestone] Description`

- **[P]**: Can run in parallel after its milestone dependencies are satisfied.
- **[M#]**: Milestone from `plan.md`.
- Every implementation task names the intended file path.
- Branch creation is always the first task in a milestone.
- Historical PR-creation tasks record the original stack. Current follow-on
  branches retain independent PR and acceptance gates.

## Milestone Dependency Graph

```mermaid
graph LR
    M1["M1: Catalog + Reference Foundations"] --> M2["M2: Case Core"]
    M2 --> M3["M3: Order Routing"]
    M3 --> M4["M4: Case Workbench"]
    M4 --> M5["M5: Manual AST"]
    M5 --> M6["M6: Worklists + Critical"]
    M6 --> M7["M7: Release + Surveillance Readiness"]
    M7 --> M8["M8: Clinical Completeness"]
    M8 --> M9["M9: Reference Administration"]
    M9 --> M10["M10: WHONET Export"]
    M10 --> R1["R1: Authoritative Alignment"]
```

## Phase 1: M1 - Catalog + Reference Foundations

**Branch**:
`feat/782-ogc-782-microbiology-mvp-m1-catalog-reference-foundations`

**Goal**: Add the minimum configuration/reference foundation for routine
bacteriology routing, culture setup defaults, organism/antibiotic lookup, AST
panels, and breakpoint standards.

**Independent Test**: A configured bacteriology test can be saved with workflow
configuration, reference lookups work, migrations roll back, and no case
workflow UI is required.

### Tests First

- [x] T001 [M1] Create branch `feat/782-ogc-782-microbiology-mvp-m1-catalog-reference-foundations` from `spec/782-ogc-782-microbiology-mvp-spec` in `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T002 [P] [M1] Add failing JUnit 4 service tests for workflow-type validation and culture recipe lookup in `src/test/java/org/openelisglobal/microbiology/service/MicrobiologyReferenceServiceTest.java`.
- [x] T003 [P] [M1] Add failing JUnit 4 service tests for breakpoint lookup including no-breakpoint behavior in `src/test/java/org/openelisglobal/microbiology/service/MicroBreakpointServiceTest.java`.
- [x] T004 [P] [M1] Add failing DAO/integration tests for organism, antibiotic, AST panel, and breakpoint persistence in `src/test/java/org/openelisglobal/microbiology/MicrobiologyReferenceDataIntegrationTest.java`.
- [x] T005 [P] [M1] Add failing ORM validation test for new microbiology reference valueholders in `src/test/java/org/openelisglobal/microbiology/MicrobiologyOrmValidationTest.java`.
- [x] T006 [P] [M1] Add failing Test Catalog regression tests for saving and loading culture workflow configuration in `src/test/java/org/openelisglobal/testcatalog/controller/rest/TestCatalogEditorMicrobiologyTest.java`.

### Implementation

- [x] T007 [M1] Add workflow-type configuration and microbiology reference tables in `src/main/resources/liquibase/3.5.x.x/051-microbiology-reference-foundations.xml`.
- [x] T008 [P] [M1] Add `MicroWorkflowType` enum in `src/main/java/org/openelisglobal/microbiology/valueholder/MicroWorkflowType.java`.
- [x] T009 [P] [M1] Add reference valueholders for organisms, antibiotics, AST panels, and breakpoint standards in `src/main/java/org/openelisglobal/microbiology/valueholder/`.
- [x] T010 [P] [M1] Add DAO interfaces for microbiology reference valueholders in `src/main/java/org/openelisglobal/microbiology/dao/`.
- [x] T011 [P] [M1] Add DAO implementations for microbiology reference valueholders in `src/main/java/org/openelisglobal/microbiology/daoimpl/`.
- [x] T012 [M1] Add `MicrobiologyReferenceService` and implementation in `src/main/java/org/openelisglobal/microbiology/service/MicrobiologyReferenceService.java` and `src/main/java/org/openelisglobal/microbiology/service/MicrobiologyReferenceServiceImpl.java`.
- [x] T013 [M1] Add `MicroBreakpointService` and implementation in `src/main/java/org/openelisglobal/microbiology/service/MicroBreakpointService.java` and `src/main/java/org/openelisglobal/microbiology/service/MicroBreakpointServiceImpl.java`.
- [x] T014 [M1] Extend Test Catalog DTO/load/save behavior for culture workflow configuration in `src/main/java/org/openelisglobal/testcatalog/controller/rest/TestCatalogEditorRestController.java`.
- [x] T015 [P] [M1] Add React Intl source keys for M1 admin fields in `frontend/src/languages/en.json`.
- [x] T016 [P] [M1] Add Test Catalog microbiology field rendering and validation in `frontend/src/components/admin/testCatalog/sections/BasicInfoSection.jsx`.
- [x] T017 [P] [M1] Add frontend tests for Test Catalog microbiology fields in `frontend/src/components/admin/testCatalog/sections/BasicInfoSection.test.jsx`.
- [x] T018 [M1] Run focused backend validation `mvn -Dtest=MicrobiologyReferenceServiceTest,MicroBreakpointServiceTest,MicrobiologyReferenceDataIntegrationTest,MicrobiologyOrmValidationTest,TestCatalogEditorMicrobiologyTest test` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T019 [M1] Run focused frontend validation `cd frontend && npm test -- BasicInfoSection.test.jsx` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T020 [M1] Run formatting and migration hygiene checks `mvn spotless:apply && git diff --check` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T021 [M1] Open draft PR for `feat/782-ogc-782-microbiology-mvp-m1-catalog-reference-foundations` to `spec/782-ogc-782-microbiology-mvp-spec` with validation evidence and link it from PR #3782.

## Phase 2: M2 - Case Core

**Branch**: `feat/782-ogc-782-microbiology-mvp-m2-case-core`

**Goal**: Add backend case identity, activity timeline, isolate lifecycle, and
case DTO compilation anchored to `SampleItem + workflow`.

**Independent Test**: A case can be created for one SampleItem/workflow, sibling
cases can coexist on the same SampleItem, and compiled case details do not rely
on lazy loading in controllers.

### Tests First

- [x] T022 [M2] Create branch `feat/782-ogc-782-microbiology-mvp-m2-case-core` from the M1 stacked branch in `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T023 [P] [M2] Add failing service tests for case creation, uniqueness, and sibling lookup in `src/test/java/org/openelisglobal/microbiology/service/MicroCaseServiceTest.java`.
- [x] T024 [P] [M2] Add failing service tests for case state transitions and invalid transition rejection in `src/test/java/org/openelisglobal/microbiology/service/MicroCaseStateServiceTest.java`.
- [x] T025 [P] [M2] Add failing service tests for isolate lifecycle rules in `src/test/java/org/openelisglobal/microbiology/service/MicroIsolateServiceTest.java`.
- [x] T026 [P] [M2] Add failing DAO/integration tests for case, activity, and isolate persistence in `src/test/java/org/openelisglobal/microbiology/MicroCaseIntegrationTest.java`.
- [x] T027 [P] [M2] Add failing controller DTO compilation test that verifies case detail JSON without controller relationship traversal in `src/test/java/org/openelisglobal/microbiology/controller/MicroCaseRestControllerTest.java`.
- [x] T028 [P] [M2] Add architecture regression check for no `@Transactional` annotations in microbiology controllers in `src/test/java/org/openelisglobal/microbiology/MicrobiologyArchitectureTest.java`.

### Implementation

- [x] T029 [M2] Add case core tables and constraints in `src/main/resources/liquibase/3.5.x.x/052-microbiology-case-core.xml`.
- [x] T030 [P] [M2] Add `MicroCase`, `MicroCaseActivity`, and `MicroIsolate` valueholders in `src/main/java/org/openelisglobal/microbiology/valueholder/`.
- [x] T031 [P] [M2] Add case, activity, and isolate DAO interfaces in `src/main/java/org/openelisglobal/microbiology/dao/`.
- [x] T032 [P] [M2] Add case, activity, and isolate DAO implementations in `src/main/java/org/openelisglobal/microbiology/daoimpl/`.
- [x] T033 [M2] Add case service contracts in `src/main/java/org/openelisglobal/microbiology/service/MicroCaseService.java`, `MicroCaseStateService.java`, and `MicroIsolateService.java`.
- [x] T034 [M2] Add case service implementations with service-layer transactions in `src/main/java/org/openelisglobal/microbiology/service/MicroCaseServiceImpl.java`, `MicroCaseStateServiceImpl.java`, and `MicroIsolateServiceImpl.java`.
- [x] T035 [M2] Add case forms/DTOs in `src/main/java/org/openelisglobal/microbiology/form/MicroCaseDetailForm.java`, `MicroCaseActivityForm.java`, and `MicroIsolateForm.java`.
- [x] T036 [M2] Add read-only case REST controller in `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroCaseRestController.java`.
- [x] T037 [M2] Run focused backend validation `mvn -q -Dtest='MicroCaseServiceTest,MicroCaseStateServiceTest,MicroIsolateServiceTest,MicroCaseIntegrationTest,MicroCaseRestControllerTest,MicrobiologyArchitectureTest,MicrobiologyOrmValidationTest' test` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T038 [M2] Run formatting and migration hygiene checks `mvn spotless:apply && git diff --check` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T039 [M2] Open draft PR for `feat/782-ogc-782-microbiology-mvp-m2-case-core` to the M1 stacked branch with validation evidence and link it from PR #3782.

## Phase 3: M3 - Order Routing

**Branch**: `feat/782-ogc-782-microbiology-mvp-m3-order-routing`

**Goal**: Create or find microbiology cases from ordered test workflow
configuration during order/sample save.

**Independent Test**: Non-micro orders create no case, bacteriology orders create
one case, and a same-specimen bacteriology/TB order creates sibling workflows
without duplicate accessioning.

### Tests First

- [x] T040 [M3] Create branch `feat/782-ogc-782-microbiology-mvp-m3-order-routing` from the M2 stacked branch in `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T041 [P] [M3] Add failing routing resolver unit tests in `src/test/java/org/openelisglobal/microbiology/service/MicroOrderRoutingServiceTest.java`.
- [x] T042 [P] [M3] Add failing order-save integration tests for non-micro, bacteriology, and sibling workflow cases in `src/test/java/org/openelisglobal/microbiology/MicroOrderRoutingIntegrationTest.java`.
- [x] T043 [P] [M3] Add failing idempotency integration test for repeated order saves in `src/test/java/org/openelisglobal/microbiology/MicroOrderRoutingIdempotencyTest.java`.
- [x] T044 [P] [M3] Add failing controller/contract test for case lookup by accession/sample item in `src/test/java/org/openelisglobal/microbiology/controller/MicroCaseLookupRestControllerTest.java`.

### Implementation

- [x] T045 [M3] Add routing service contract in `src/main/java/org/openelisglobal/microbiology/service/MicroOrderRoutingService.java`.
- [x] T046 [M3] Implement order routing service in `src/main/java/org/openelisglobal/microbiology/service/MicroOrderRoutingServiceImpl.java`.
- [x] T047 [M3] Wire routing from the existing order/sample save integration point in `src/main/java/org/openelisglobal/sample/service/SamplePatientEntryServiceImpl.java`.
- [x] T048 [M3] Add case lookup endpoint and DTO support in `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroCaseRestController.java` and `src/main/java/org/openelisglobal/microbiology/form/MicroCaseLookupForm.java`.
- [x] T049 [M3] Add configuration error handling for missing culture workflow/method defaults in `src/main/java/org/openelisglobal/microbiology/service/MicroOrderRoutingServiceImpl.java`.
- [x] T050 [M3] Run focused backend validation `mvn -q -Dtest='MicroOrderRoutingServiceTest,MicroOrderRoutingIntegrationTest,MicroOrderRoutingIdempotencyTest,MicroCaseLookupRestControllerTest' test` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T051 [M3] Run formatting and migration hygiene checks `mvn spotless:apply && git diff --check` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T052 [M3] Open draft PR for `feat/782-ogc-782-microbiology-mvp-m3-order-routing` to the M2 stacked branch with validation evidence and link it from PR #3782.

## Phase 4: M4 - Case Workbench

**Branch**: `feat/782-ogc-782-microbiology-mvp-m4-case-workbench`

**Goal**: Provide REST and React case workbench surfaces for setup,
incubation/growth/no-growth/rejection events, isolate creation/update, and case
history.

**Independent Test**: A routed bacteriology case can be opened, setup can be
recorded, growth can be logged, an isolate can be created, and the visible
timeline updates.

### Tests First

- [x] T053 [M4] Create branch `feat/782-ogc-782-microbiology-mvp-m4-case-workbench` from the M3 stacked branch in `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T054 [P] [M4] Run `/plan-record-playwright --flows microbiology-case-workbench` and record the planned route, setup data, assertions, and project target in `specs/782-ogc-782-microbiology-mvp-spec/playwright-plan.md`.
- [x] T055 [P] [M4] Add failing MockMvc tests for activity creation and isolate creation in `src/test/java/org/openelisglobal/microbiology/controller/MicroCaseRestControllerTest.java`.
- [x] T056 [P] [M4] Add failing React interaction tests for case detail loading and setup event save in `frontend/src/components/microbiology/__tests__/MicrobiologyCaseView.test.jsx`.
- [x] T057 [P] [M4] Add failing React interaction tests for isolate creation/update in `frontend/src/components/microbiology/__tests__/IsolatePanel.test.jsx`.
- [x] T058 [P] [M4] Use `/write-playwright-test frontend/playwright/tests/foundational/core/microbiology-case-workbench.spec.ts --project core-app` to create a red Playwright test for routed case setup and isolate creation.

### Implementation

- [x] T059 [M4] Add activity mutation endpoints in `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroCaseRestController.java`.
- [x] T060 [M4] Add isolate mutation endpoints in `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroIsolateRestController.java`.
- [x] T061 [P] [M4] Add frontend API client functions in `frontend/src/components/microbiology/MicrobiologyService.js`.
- [x] T062 [P] [M4] Add case page route in `frontend/src/pages/MicrobiologyPage.jsx` and `frontend/src/App.jsx`.
- [x] T063 [M4] Add case view shell and context header in `frontend/src/components/microbiology/MicrobiologyCaseView.jsx`.
- [x] T064 [M4] Add timeline and setup activity panel in `frontend/src/components/microbiology/CaseTimelinePanel.jsx`.
- [x] T065 [M4] Add isolate panel in `frontend/src/components/microbiology/IsolatePanel.jsx`.
- [x] T066 [P] [M4] Add React Intl keys for case workbench UI in `frontend/src/languages/en.json`.
- [x] T067 [M4] Register `frontend/playwright/tests/foundational/core/microbiology-case-workbench.spec.ts` in `frontend/playwright.config.ts`.
- [x] T068 [M4] Run Playwright registration validation `python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-case-workbench.spec.ts` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T069 [M4] Run `/audit-playwright frontend/playwright/tests/foundational/core/microbiology-case-workbench.spec.ts` and address findings in `frontend/playwright/tests/foundational/core/microbiology-case-workbench.spec.ts`.
- [x] T070 [M4] Run narrow Playwright evidence command `cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-case-workbench.spec.ts --project=core-app` and attach screenshot/trace results to the PR.
- [x] T071 [M4] Run focused backend/frontend validation `mvn -q -Dtest='MicroCaseRestControllerTest,MicroCaseLookupRestControllerTest,MicrobiologyArchitectureTest' test` and `cd frontend && npm test -- MicrobiologyCaseView.test.jsx IsolatePanel.test.jsx` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T072 [M4] Open draft PR for `feat/782-ogc-782-microbiology-mvp-m4-case-workbench` to the M3 stacked branch with TDD and Playwright evidence and link it from PR #3782.

## Phase 5: M5 - Manual AST

**Branch**: `feat/782-ogc-782-microbiology-mvp-m5-manual-ast`

**Goal**: Add manual AST setup, readings, S/I/R interpretation, no-breakpoint
handling, repeat/retest, review, and override audit.

**Independent Test**: An identified significant isolate supports AST entry,
interpretation, review, override with reason, and final-release blocking while
unreviewed.

### Tests First

- [x] T073 [M5] Create branch `feat/782-ogc-782-microbiology-mvp-m5-manual-ast` from the M4 stack branch in `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T074 [P] [M5] Add failing AST interpretation unit tests for MIC, zone, no-breakpoint, and override behavior in `src/test/java/org/openelisglobal/microbiology/service/MicroAstInterpretationServiceTest.java`.
- [x] T075 [P] [M5] Add failing AST persistence integration tests for runs, readings, repeat/retest, and review state in `src/test/java/org/openelisglobal/microbiology/MicroAstIntegrationTest.java`.
- [x] T076 [P] [M5] Add failing readiness service tests proving unreviewed AST blocks final release in `src/test/java/org/openelisglobal/microbiology/service/MicroCaseReadinessServiceTest.java`.
- [x] T077 [P] [M5] Add failing React interaction tests for AST entry, interpretation display, and override reason validation in `frontend/src/components/microbiology/__tests__/AstEntryPanel.test.jsx`.
- [x] T078 [P] [M5] Use `/write-playwright-test frontend/playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project core-demo` to create a red Playwright test for manual AST entry and override audit.

### Implementation

- [x] T079 [M5] Add AST tables and rollback in `src/main/resources/liquibase/3.5.x.x/053-microbiology-manual-ast.xml`.
- [x] T080 [P] [M5] Add AST valueholders in `src/main/java/org/openelisglobal/microbiology/valueholder/MicroAstRun.java` and `src/main/java/org/openelisglobal/microbiology/valueholder/MicroAstReading.java`.
- [x] T081 [P] [M5] Add AST DAO interfaces and implementations in `src/main/java/org/openelisglobal/microbiology/dao/` and `src/main/java/org/openelisglobal/microbiology/daoimpl/`.
- [x] T082 [M5] Add AST service contracts in `src/main/java/org/openelisglobal/microbiology/service/MicroAstService.java` and `src/main/java/org/openelisglobal/microbiology/service/MicroAstInterpretationService.java`.
- [x] T083 [M5] Implement AST services in `src/main/java/org/openelisglobal/microbiology/service/MicroAstServiceImpl.java` and `src/main/java/org/openelisglobal/microbiology/service/MicroAstInterpretationServiceImpl.java`.
- [x] T084 [M5] Add AST REST controller and forms in `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroAstRestController.java` and `src/main/java/org/openelisglobal/microbiology/form/`.
- [x] T085 [M5] Add readiness service contract and implementation in `src/main/java/org/openelisglobal/microbiology/service/MicroCaseReadinessService.java` and `src/main/java/org/openelisglobal/microbiology/service/MicroCaseReadinessServiceImpl.java`.
- [x] T086 [M5] Add AST entry panel in `frontend/src/components/microbiology/AstEntryPanel.jsx`.
- [x] T087 [P] [M5] Add React Intl keys for AST UI in `frontend/src/languages/en.json`.
- [x] T088 [M5] Register `frontend/playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` in `frontend/playwright.config.ts`.
- [x] T089 [M5] Run Playwright registration validation `python3 .ai/skills/playwright/scripts/validate-playwright-project.py frontend/playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T090 [M5] Run selector-policy audit for `frontend/playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` and address findings.
- [x] T091 [M5] Run narrow Playwright evidence command `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo` and attach screenshot/video results to the PR.
- [x] T092 [M5] Run focused backend/frontend validation `mvn -q -Dtest='MicroAstInterpretationServiceTest,MicroAstIntegrationTest,MicroCaseReadinessServiceTest,MicrobiologyArchitectureTest,MicrobiologyOrmValidationTest' test && cd frontend && npm test -- MicrobiologyCaseView.test.jsx IsolatePanel.test.jsx AstEntryPanel.test.jsx` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T093 [M5] Open draft PR #3787 for `feat/782-ogc-782-microbiology-mvp-m5-manual-ast` to the M4 stack branch with TDD and Playwright evidence.

## Phase 6: M6 - Worklists + Critical Communications

**Branch**: `feat/782-ogc-782-microbiology-mvp-m6-worklists-critical`

**Goal**: Add shared worklist filtering/prioritization, sibling visibility,
critical communication logging, and existing Alert dashboard surfacing.

**Independent Test**: Users can find due microbiology work, see sibling
workflows, and log a critical communication without needing complete provider
directory data.

### Tests First

- [x] T094 [M6] Create branch `feat/782-ogc-782-microbiology-mvp-m6-worklists-critical` from the M5 stack branch in `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T095 [P] [M6] Add failing worklist service tests for due-action sorting, urgency, sibling visibility, and review flags in `src/test/java/org/openelisglobal/microbiology/service/MicroWorklistServiceTest.java`.
- [x] T096 [P] [M6] Cover seeded in-flight worklist behavior through `frontend/playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts`.
- [x] T097 [P] [M6] Add failing critical communication service tests for recipient free text, ack state, follow-up, and immutable message behavior in `src/test/java/org/openelisglobal/microbiology/service/MicroCriticalCommunicationServiceTest.java`.
- [x] T098 [P] [M6] Record engineering decision: generic `Alert` currently requires numeric entity ids, so M6 surfaces critical communication through the microbiology worklist rather than forcing UUID cases into the alert table.
- [x] T099 [P] [M6] Add failing React interaction tests for worklist filters and critical communication logging in `frontend/src/components/microbiology/__tests__/MicrobiologyWorklist.test.jsx` and `frontend/src/components/microbiology/__tests__/CriticalCommunicationPanel.test.jsx`.
- [x] T100 [P] [M6] Use `/write-playwright-test frontend/playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts --project core-app` to create a red Playwright test for worklist navigation and critical communication logging.

### Implementation

- [x] T101 [M6] Add critical communication table migration in `src/main/resources/liquibase/3.5.x.x/054-microbiology-worklists-critical.xml`.
- [x] T102 [P] [M6] Add critical communication valueholder in `src/main/java/org/openelisglobal/microbiology/valueholder/MicroCriticalCommunication.java`.
- [x] T103 [P] [M6] Add critical communication DAO interface and implementation in `src/main/java/org/openelisglobal/microbiology/dao/MicroCriticalCommunicationDAO.java` and `src/main/java/org/openelisglobal/microbiology/daoimpl/MicroCriticalCommunicationDAOImpl.java`.
- [x] T104 [M6] Add worklist service in `src/main/java/org/openelisglobal/microbiology/service/MicroWorklistService.java` and `src/main/java/org/openelisglobal/microbiology/service/MicroWorklistServiceImpl.java`.
- [x] T105 [M6] Add critical communication service in `src/main/java/org/openelisglobal/microbiology/service/MicroCriticalCommunicationService.java` and `src/main/java/org/openelisglobal/microbiology/service/MicroCriticalCommunicationServiceImpl.java`.
- [x] T106 [M6] Add worklist and critical communication REST endpoints in `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroWorklistRestController.java` and `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroCriticalCommunicationRestController.java`.
- [x] T107 [M6] Preserve generic Alert untouched; carry separate engineering follow-up if alert polymorphic ids need UUID/string support.
- [x] T108 [P] [M6] Add worklist UI in `frontend/src/components/microbiology/MicrobiologyWorklist.jsx`.
- [x] T109 [P] [M6] Add critical communication UI in `frontend/src/components/microbiology/CriticalCommunicationPanel.jsx`.
- [x] T110 [P] [M6] Add React Intl keys for worklist and critical communication UI in `frontend/src/languages/en.json`.
- [x] T111 [M6] Register `frontend/playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts` in `frontend/playwright.config.ts`.
- [x] T112 [M6] Run Playwright registration validation `python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T113 [M6] Run selector-policy audit for `frontend/playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts` and address findings.
- [x] T114 [M6] Run narrow Playwright evidence command `cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts --project=core-app` and attach results to the PR.
- [x] T115 [M6] Run focused backend/frontend validation `mvn -q -Dtest='MicroWorklistServiceTest,MicroCriticalCommunicationServiceTest,MicrobiologyArchitectureTest,MicrobiologyOrmValidationTest' test && cd frontend && npm test -- MicrobiologyWorklist.test.jsx CriticalCommunicationPanel.test.jsx MicrobiologyCaseView.test.jsx` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T116 [M6] Open draft PR #3788 for `feat/782-ogc-782-microbiology-mvp-m6-worklists-critical` to the M5 stack branch with TDD and Playwright evidence.

## Phase 7: M7 - Release + Surveillance Readiness

**Branch**:
`feat/782-ogc-782-microbiology-mvp-m7-release-surveillance-readiness`

**Goal**: Add preliminary/final release readiness gates, visible patient-report
handoff, final-case mutation locking, WHONET readiness, and final MVP
Playwright evidence. Amendment and re-identification history are V2.

**Independent Test**: A complete MVP bacteriology case can go from order-routed
case to setup, isolate, manual AST, review, preliminary/final readiness, and
WHONET readiness; incomplete cases show blockers.

### Tests First

- [x] T117 [M7] Create branch `feat/782-ogc-782-microbiology-mvp-m7-release-surveillance-readiness` from the M6 stack branch in `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T118 [P] [M7] Add release service tests for preliminary release, final release blockers, and release history in `src/test/java/org/openelisglobal/microbiology/service/MicroReportReleaseServiceTest.java`.
- [x] T119 [P] [M7] Add WHONET readiness tests for missing organism, AST review, and mapping readiness in `src/test/java/org/openelisglobal/microbiology/service/MicroWhonetReadinessServiceTest.java`.
- [x] T120 [P] [M7] Add readiness regression coverage proving cases without isolates cannot final-release in `src/test/java/org/openelisglobal/microbiology/service/MicroCaseReadinessServiceTest.java`.
- [x] T121 [P] [M7] Add React interaction tests for readiness blockers and release actions in `frontend/src/components/microbiology/__tests__/ReportReadinessPanel.test.jsx`.
- [x] T122 [P] [M7] Update `specs/782-ogc-782-microbiology-mvp-spec/playwright-plan.md` with the final release-readiness MVP flow and evidence commands.
- [x] T123 [P] [M7] Extend the existing canonical MVP demo `frontend/playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` to prove final release gating and release state.
- [x] T124 [P] [M7] Keep the existing `core-demo` / `core-demo-video` proof path instead of adding duplicate demo specs.

### Implementation

- [x] T125 [M7] Confirm no M7 Liquibase migration is needed because release uses existing `micro_case.final_release_state`, `closed_at`, `closed_by`, and case activity history.
- [x] T126 [M7] Add report release service in `src/main/java/org/openelisglobal/microbiology/service/MicroReportReleaseService.java` and `src/main/java/org/openelisglobal/microbiology/service/MicroReportReleaseServiceImpl.java`.
- [x] T127 [M7] Add WHONET readiness service in `src/main/java/org/openelisglobal/microbiology/service/MicroWhonetReadinessService.java` and `src/main/java/org/openelisglobal/microbiology/service/MicroWhonetReadinessServiceImpl.java`.
- [x] T128 [M7] Preserve the existing WHONET export path and expose M7 WHONET readiness without creating a parallel exporter.
- [x] T129 [M7] Add release and readiness REST endpoints in `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroReportReleaseRestController.java` and `src/main/java/org/openelisglobal/microbiology/controller/rest/MicroWhonetReadinessRestController.java`.
- [x] T130 [M7] Add report readiness panel in `frontend/src/components/microbiology/ReportReadinessPanel.jsx`.
- [x] T131 [P] [M7] Include WHONET readiness status in `frontend/src/components/microbiology/ReportReadinessPanel.jsx`.
- [x] T132 [P] [M7] Add React Intl keys for release and WHONET readiness UI in `frontend/src/languages/en.json`.
- [x] T133 [M7] Reuse the registered canonical MVP spec `frontend/playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` for M7.
- [x] T134 [M7] Run Playwright registration validation with `python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T135 [M7] Run selector-policy audit for `frontend/playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` and address findings.
- [x] T136 [M7] Run narrow functional Playwright evidence command `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo` and attach screenshot/trace results to the PR.
- [x] T137 [M7] Run demo Playwright evidence command `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo` and attach screenshot/trace results to the PR.
- [x] T138 [M7] Run video evidence command `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo-video` and verify `frontend/test-results/*/video.webm` exists.
- [x] T139 [M7] Debug failed Playwright runs with screenshot/trace evidence and fix source/test issues in `frontend/playwright/tests/` and `frontend/src/components/microbiology/`.
- [x] T140 [M7] Run focused backend/frontend validation `mvn -q -Dtest='MicroCaseReadinessServiceTest,MicroReportReleaseServiceTest,MicroWhonetReadinessServiceTest,MicrobiologyArchitectureTest,MicrobiologyOrmValidationTest' test && cd frontend && npm test -- ReportReadinessPanel.test.jsx MicrobiologyCaseView.test.jsx AstEntryPanel.test.jsx` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T141 [M7] Run final documentation consistency update in `specs/782-ogc-782-microbiology-mvp-spec/tasks.md`, `specs/782-ogc-782-microbiology-mvp-spec/playwright-plan.md`, and `specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-checkpoint-2026-06-27.md`.
- [x] T142 [M7] Open draft PR #3789 for `feat/782-ogc-782-microbiology-mvp-m7-release-surveillance-readiness` to the M6 stack branch with TDD, Playwright trace/screenshot, and demo video evidence linked from PR #3782.

## Phase 8: MVP-Gap Remediation (FR-002, M-11, M-05)

**Branch**: `feat/782-ogc-782-microbiology-mvp-m7-release-surveillance-readiness`
(same held branch as M7; not a new milestone branch — held for one combined
MVP delivery per
`specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-gap-analysis-2026-07-03.md`).

**Goal**: Close the three confirmed MVP-scope gaps identified by the gap
analysis before merging #3789: FR-002 order-detail capture, M-11 Alerts
Dashboard integration (reconciling FR-018), and M-05 per-run
breakpoint-standard selection.

### FR-002: Order-detail capture

- [x] T157 [P] Add failing service test for order-detail create/update/read in `src/test/java/org/openelisglobal/microbiology/service/MicroCaseOrderDetailServiceTest.java`.
- [x] T158 [P] Add failing case-detail compilation tests for order-detail inclusion in `src/test/java/org/openelisglobal/microbiology/service/MicroCaseServiceTest.java`.
- [x] T159 [P] Add failing controller test for the order-detail save endpoint in `src/test/java/org/openelisglobal/microbiology/controller/MicroCaseRestControllerTest.java`.
- [x] T160 [P] Add failing routing-overload tests for order-detail pass-through in `src/test/java/org/openelisglobal/microbiology/service/MicroOrderRoutingServiceTest.java`.
- [x] T161 [P] Add failing frontend tests for `OrderDetailPanel` in `frontend/src/components/microbiology/__tests__/OrderDetailPanel.test.jsx`.
- [x] T162 Add `micro_case_order_detail` table in `src/main/resources/liquibase/3.5.x.x/055-microbiology-order-detail.xml`; add `MicroCaseOrderDetail` valueholder, DAO, `MicroCaseOrderDetailService`, controller endpoint, `OrderDetailPanel.jsx`, and `MicrobiologyService.saveOrderDetail`; register the entity in `persistence/persistence.xml` and `persistence/test-persistence.xml`.
- [x] T163 Wire an optional order-detail overload on `MicroOrderRoutingService.routeAnalysesForSampleItem` so order entry can supply it atomically with case creation without changing the existing 3-arg signature. The historical workbench-only scoping decision was superseded by T191 after the order form boundary was validated.

### M-11: Alerts Dashboard integration (reconciles FR-018)

- [x] T164 [P] Add failing `AlertService`/`AlertDAO` tests for string-keyed (`alertEntityRef`) alerts and Freezer/Equipment/Sample regression coverage in `src/test/java/org/openelisglobal/alert/service/AlertServiceTest.java`.
- [x] T165 [P] Add failing tests for critical-communication-to-Alert projection and acknowledgment sync in `src/test/java/org/openelisglobal/microbiology/service/MicroCriticalCommunicationServiceTest.java`.
- [x] T166 [P] Add a failing end-to-end DB integration test in `src/test/java/org/openelisglobal/microbiology/MicroCriticalCommunicationAlertIntegrationTest.java`.
- [x] T167 [P] Add failing frontend test for the `MICROBIOLOGY_CRITICAL` filter/row in `frontend/src/components/alerts/__tests__/AlertsDashboard.test.jsx`.
- [x] T168 Add `alert_entity_ref` column plus `chk_alert_entity_id_or_ref`/`chk_alert_type` constraint updates in `src/main/resources/liquibase/3.5.x.x/057-alert-entity-ref.xml` (additive; does not alter the existing `alert_entity_id` column's use by Freezer/Equipment/Sample callers); add `AlertType.MICROBIOLOGY_CRITICAL`, `Alert.alertEntityRef`, `AlertDAO.getAlertsByEntityRef`, `AlertService.createAlert(..., String entityRef, ...)`/`getAlertsByEntityRef`.
- [x] T169 Wire `MicroCriticalCommunicationServiceImpl.logCommunication`/`acknowledge` to project into/acknowledge the corresponding `Alert` row (log-plus-projection, no dual-write per Constitution Principle X); add the `MICROBIOLOGY_CRITICAL` filter option to `AlertsDashboard.jsx`.
- [x] T170 Update `FR-018` in `specs/782-ogc-782-microbiology-mvp-spec/spec.md` to describe the reconciled log-plus-projection approach.
- [x] T171 Run alert regression suite `mvn -q -Dtest='AlertServiceTest,FreezerAlertServiceTest,AlertFlowIntegrationTest,QCAlertServiceTest,QCAlertServiceIntegrationTest,EQAAlertRestControllerTest' test` to confirm numeric-keyed alert callers are unaffected.

### M-05: Per-run breakpoint-standard selection

- [x] T172 [P] Add failing service tests for `startRun` with an explicit standard and `recordReading` interpreting against the run's snapshotted standard (with default-fallback) in `src/test/java/org/openelisglobal/microbiology/service/MicroAstServiceTest.java`.
- [x] T173 [P] Add a failing DB-level integration test proving two runs against different standards interpret the same raw value differently in `src/test/java/org/openelisglobal/microbiology/MicroAstIntegrationTest.java`.
- [x] T174 [P] Add failing service test for `MicroBreakpointService.getActiveStandards()` in `src/test/java/org/openelisglobal/microbiology/service/MicroBreakpointServiceTest.java`.
- [x] T175 [P] Add failing frontend test for the breakpoint-standard selector in `frontend/src/components/microbiology/__tests__/AstEntryPanel.test.jsx`.
- [x] T176 Add `breakpoint_standard_id` column to `micro_ast_run` in `src/main/resources/liquibase/3.5.x.x/056-microbiology-ast-breakpoint-standard.xml`; add the field to `MicroAstRun`; add the `startRun` overload and standard-resolution fallback in `MicroAstServiceImpl`; add the `/rest/microbiology/reference/breakpoint-standards` endpoint; wire the `AstEntryPanel.jsx` selector and `MicrobiologyService.getBreakpointStandards`.
- [x] T177 Run focused validation `mvn -q -Dtest='Micro*Test' test` and `cd frontend && npx vitest run Microbiology` to confirm no regression across the full microbiology suite.

## Final MVP Acceptance Gate

**Purpose**: Prove the full implemented MVP (M1-M7 core + FR-002/M-11/M-05 gap
remediation) behaves as specified before merging the held #3789.

- [x] T143 [MVP] Run the complete focused backend suite `mvn -q -Dtest='Micro*Test,*Micro*IntegrationTest,AlertServiceTest,FreezerAlertServiceTest,AlertFlowIntegrationTest,QCAlertServiceTest,QCAlertServiceIntegrationTest,EQAAlertRestControllerTest' test` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`. Passed: 24 microbiology + alert test classes, 0 failures.
- [x] T144 [MVP] Run the complete focused frontend suite `cd frontend && npx vitest run Microbiology AlertsDashboard` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend`. Passed: 8 test files, 19 tests.
- [x] T145 [MVP] Validate the real registered microbiology Playwright specs. `frontend/playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` validates via `python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts` (run from `frontend/`), matching `core-app`/`core-demo`/`core-demo-video`. The two `foundational/core/` specs (`microbiology-case-workbench.spec.ts`, `microbiology-worklist-critical.spec.ts`) register via the `CORE_FOUNDATIONAL_TESTS` glob (`**/foundational/core/**/*.spec.ts`) in `frontend/playwright.config.ts`, which `validate-playwright-project.py` does not recognize as a named testMatch constant (pre-existing script limitation, not specific to microbiology); confirmed registration by inspecting `playwright.config.ts` directly instead. (Note: manual-AST and release-readiness coverage live inside the `ogc-782-microbiology-mvp.spec.ts` demo per the M7 code-qa spec-alignment note; there are no separate `microbiology-manual-ast.spec.ts` / `microbiology-mvp-release-readiness.spec.ts` / `microbiology-mvp-demo.spec.ts` files.)
- [x] T146 [MVP] Ran all microbiology foundational Playwright evidence with `cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-*.spec.ts --project=core-app` against a real dev stack (WAR rebuilt, containers up, all 3 new Liquibase migrations applied). 3 passed. Also ran the demo spec on `core-demo` (2 passed). During this run, fixed two pre-existing stale Playwright selectors (`microbiology-case-workbench.spec.ts`, `microbiology-worklist-critical.spec.ts` expected raw enum text like `SETUP_RECORDED`/`OPEN` where the UI renders formatted labels like `Setup Recorded`/`Open`) and one pre-existing production bug discovered by the demo spec's final-release step (see `evidence/mvp-gap-analysis-2026-07-03.md` "Discovered during acceptance gate" section): `MicroReportReleaseServiceImpl.releaseFinal` required a `MicroCase.stage` state-machine transition that nothing in the isolate/AST flow ever satisfies; fixed to set `stage` directly once readiness passes, matching `releasePreliminary`.
- [x] T147 [MVP] Run all microbiology demo Playwright evidence with `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo`.
- [x] T148 [MVP] Record final MVP video evidence with `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo-video`.
- [x] T149 [MVP] Record final Playwright screenshots, video evidence, and code-qa evidence bundle in `specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-checkpoint-2026-06-27.md`.
- [x] T150 [MVP] Run `mvn spotless:apply` plus targeted frontend Prettier for touched microbiology files from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T151 [MVP] Run `git diff --check` from `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2`.
- [x] T152 [MVP] Verify `DIGI-UW/code-qa` is installed or available as a skill source from `https://github.com/DIGI-UW/code-qa` before final MVP acceptance.
- [x] T153 [MVP] Apply the `meaningful-test-coverage` workflow from `DIGI-UW/code-qa` against the implemented microbiology MVP and record which backend, frontend, and E2E tests satisfy the inversion test in the M7 PR.
- [x] T154 [MVP] Apply the `spec-code-alignment` workflow from `DIGI-UW/code-qa` against `specs/782-ogc-782-microbiology-mvp-spec/` and the implemented code, then update lagging specs or file defects for real code divergence.
- [x] T155 [MVP] Apply the `simplicity-review` workflow from `DIGI-UW/code-qa` against the MVP diff and remove or explicitly justify speculative abstractions, duplicate exporters, duplicate alert surfaces, or unused configuration.
- [x] T156 [MVP] Run the `evidence-bundle` workflow from `DIGI-UW/code-qa` after the final `core-demo-video` Playwright run and record the generated text report plus manually shared media links for the M7 PR and parent PR #3782.

## Phase 9: Navigation, Stable URLs, And UAT Readiness

**Goal**: Make Microbiology discoverable through configured navigation,
preserve deterministic page state, and publish the matching deployed-review
checklist through the Grist-backed UAT harness.

- [x] T178 [MVP] Define primary-navigation discovery and bookmarkable worklist/case state in `spec.md`.
- [x] T179 [MVP] Register the Microbiology menu through `volume/menu/menu_config.json` and add canonical worklist/case routes with legacy redirects.
- [x] T180 [P] [MVP] Add focused React tests for route composition, filter persistence, case-section state, and worklist return context.
- [x] T181 [MVP] Extend `frontend/playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts` to prove configured navigation and canonical URL behavior in the registered `core-app` project.
- [x] T182 [MVP] Update the OGC-782 `amr` checklist in Grist with ten product-focused steps covering navigation, stable page state, case work, isolate/AST workflow, report propagation, and the shared-specimen case.
- [x] T183 [MVP] Verify `https://amr.openelis-global.org/__review/uat-amr.json` and the rendered AMR `Review` overlay with Playwright; record the deployment caveat in `evidence/uat-review-harness-2026-07-24.md`.

## Phase 10: Worklist UX Remediation And M-07 Scope Check

**Goal**: Correct the observed worklist layout defects without turning the
prototype into a technical contract, and make any remaining M-07 differences
explicit product decisions or V2 scope.

- [x] T184 [MVP] Add a compact-viewport layout test and make the Microbiology sidenav default closed on compact screens while retaining locked desktop navigation and saved user preferences.
- [x] T185 [MVP] Add a Playwright mobile regression for contained table scrolling; correct the Carbon table-container sizing so the page does not horizontally overflow.
- [x] T186 [MVP] Capture and inspect stable desktop/mobile worklist screenshots, rerun the registered worklist/critical Playwright journey, and record the comparison in `evidence/worklist-ux-follow-up-2026-07-28.md`.
- [x] T187 [Superseded] The pinned OpenELIS Work M-07 source makes the culture/AST-run switch, richer queue context, resistance strip, and recent activity required behavior. Phase 14 now carries deterministic implementation and evidence tasks; no separate product ruling remains.

## Phase 11: Deployed UAT Fixture Integrity

**Goal**: Deploy the merge candidate and make its live UAT data reproducible
without bypassing OpenELIS application services.

- [x] T188 [MVP] Replace the review-tooling AMR SQL seed with authenticated, property-gated `MicrobiologyUatScenarioService` provisioning; prove repeated runs return the same accession and case identifiers.
- [x] T189 [MVP] Deploy OpenELIS and review tooling, verify the exact application SHA, canonical worklist and case URLs, desktop and compact layouts, live ten-step Grist overlay, and deterministic service-created case; record results and remaining issues in `evidence/live-deployment-2026-07-28.md`.

## Phase 12: Deterministic MVP Closure And Human Acceptance

**Goal**: Correct CI evidence, close the remaining product-story gaps, reconcile
scope claims, deploy the exact candidate, and keep automated prechecks distinct
from Piotr's human UAT ruling.

- [x] T190 [MVP] Correct service-created fixture status handling and replace backend-only report inspection in the canonical Playwright flow with visible navigation and assertions on the patient-results page (`4cafa530a`).
- [x] T191 [MVP] Historical partial repair: show reusable microbiology order-detail fields when selected-test workflow metadata is present and submit them through the sample-entry service path (`8b6982867`). This mounted only in legacy `/SamplePatientEntry`, omitted required Method/control/default behavior, and did not preserve workflow metadata in the supported `/order/enter` flow. It does not satisfy FR-002 and is superseded by Phase 14.
- [x] T192 [MVP] Compile patient, accession, and specimen context inside the case service transaction; keep it visible in the workbench; capture media/bottle, incubation, and atmosphere explicitly in the existing activity record (`8b6982867`).
- [x] T193 [MVP] Expose existing projected Result identifiers to critical communication and link the report workflow to the visible patient-results page without adding schema (`8b6982867`).
- [x] T194 [MVP] Pass 13 focused backend tests and 18 focused frontend tests for the July 28 story-closure slice; run Spotless, Prettier, focused source ESLint, and `git diff --check`.
- [x] T195 [MVP] Historical reconciliation at the July 28 checkpoint. Its M-03 resolution and aggregate parity claims are superseded by the pinned 2026-08-05 OpenELIS Work alignment audit and Phase 14.
- [x] T196 [MVP] Reconcile the ten Grist checklist rows with the current behavior contract and verify live JSON revision `fc9d65e109d3e6863d75b9c66ef7f2f41480bf626d458c11bb59ad55ae5bc0fe`: nine core steps required, TB reflection optional. Final rendered-overlay verification remains part of deployed T198 evidence.
- [x] T202 [MVP] Repair deployed service-layer UAT data and workflow defects found by the canonical journey: complete patient demographics, add selectable sample/test mapping and localization, normalize blank optional organism identifiers, perform preliminary projection before Result-target communication, return a named 409 for final-case writes, and cover the changes with focused backend/frontend tests (`e2f7a36ea` through `1e3aadfaf`).
- [x] T203 [MVP] Repair full-suite fixture isolation after legacy DBUnit tests remove shared statuses and active methods: resolve or provision the minimum reference data through services with generated identifiers, cover stale-cache and polluter ordering, and pass the 4,660-test clean install plus the remote backend checkpoint (`6f941e6a5`, `46ab388ad`).
- [x] T204 [MVP] Reverify the live Grist JSON and synchronize repository evidence and Playwright assertions to revision `fc9d65e109d3e6863d75b9c66ef7f2f41480bf626d458c11bb59ad55ae5bc0fe`: AMR-1 through AMR-7 plus AMR-16 and AMR-20 required; AMR-21 optional and non-blocking.
- [x] T197 [MVP] Deploy the exact #3789 candidate plus compatible review-tooling revision to `amr.openelis-global.org`; app SHA `ee5e4d1fa5324dd0ae742c0cc303b9e1b61b25cf`, review-tooling SHA `4df8441896e59520ffb9bb247b1b2e3ce3f5248d`, health/smoke/SHA guard passed.
- [x] T198 [MVP] Run registered `core-app`/deployed Playwright pre-UAT journeys for order routing, worklist state, case setup, isolate/AST, critical communication, final lock, and visible patient-report propagation; the final `core-demo-video` passed in 1.9 minutes and the external evidence bundle is `/tmp/ogc-782-mvp-evidence-final-2026-07-29/`.
- [ ] T199 [Follow-up] Create a repeatable service-layer performance fixture and measure the source M-NFR 200-case/sub-second-p95 target. Do not claim this target in #3789 until evidence exists.
- [ ] T200 [Human UAT] Piotr completes Pass/Fail/N/A plus notes for the live combined M1/M2/M3/M4 checklist: 38 required steps across 14 stories at pre-deployment revision `2c50adaa394ee252cd775a87383c70d5af672b42530614c9bc1ad201dac27ba8`; the optional TB reflection cannot block acceptance. Reconfirm the revision after the exact M4 deployment. Automated execution cannot check off this task.
- [x] T201 [MVP] Update PR #3789 with final scope, V2 exclusions, exact commits, test/evidence links, UAT revision and rulings, deployment SHAs, and one-shot required-check status.

## Phase 13: Follow-On Stack Status

- [x] T205 [M2] Implement clinical completeness and NFR qualification in `specs/782-ogc-782-microbiology-m8-clinical-completeness/`; PR and human acceptance remain separate gates.
- [x] T206 [M3] Implement reference and mapping administration in `specs/782-ogc-782-microbiology-m9-reference-mapping-admin/`; PR and human acceptance remain separate gates.
- [ ] T207 [M4] Complete the manual WHONET export acceptance gate in `specs/782-ogc-782-microbiology-m10-whonet-export/tasks.md`; implementation and PR exist and the combined candidate is deployed, but exact-SHA human UAT remains open.

## Phase 14: R1 Authoritative Alignment Remediation

**Branch**:
`feat/782-ogc-782-microbiology-r1-authoritative-alignment`

**Base**: `feat/782-ogc-782-microbiology-m10-whonet-export` at
`08b5b3888af4ba9f1c506fc555138218e0d043a4`

**Goal**: Restore the authoritative M-03 Program/order-entry behavior on the
supported Add Order workflow, correct downstream completion claims, and prove
the same behavior through implementation, automated evidence, visual review,
and a separate Grist UAT story.

**Independent Test**: Starting from configured Add Order navigation, a reviewer
selects a culture-capable test, sees Program become Microbiology, confirms the
required/defaulted Culture Method and complete details controls, saves, and
opens exactly one resulting case containing those details.

### Authority And Tests First

- [x] T208 [R1] Create `feat/782-ogc-782-microbiology-r1-authoritative-alignment` from the verified M10 head in an isolated worktree.
- [x] T209 [R1] Pin OpenELIS Work revision `a1f720d7b3b01db63387361495f4aa6589105003` and add the 17-step source-to-code-to-UAT crosswalk in `evidence/openelis-work-authoritative-alignment-2026-08-05.md`.
- [x] T210 [P] [R1] Add failing selector/state tests proving modern order selection retains culture workflow and Method metadata for direct and panel-selected tests (`38b177f7e`).
- [x] T211 [P] [R1] Add React interaction tests using Carbon-accessible roles for Program derivation, required/defaulted Culture Method, Patient Origin selection, bounded Number of Sets, Clinical History, Antibiotic Exposure checkbox, Critical Notify checkbox, discard confirmation, and manual Program fallback (`38b177f7e`, `1136be1a6`).
- [ ] T212 [P] [R1] Add failing service/controller integration coverage proving the supported order save persists details atomically, creates one case, and returns the details through case compilation without SQL fixtures or fixed primary keys.
- [ ] T213 [P] [R1] Replace the legacy-route order-entry Playwright shortcut with a red `core-app` journey that enters through configured navigation and `/order/enter`; cover culture, non-culture, save-to-case, removal confirmation, and the ruled mixed-workflow behavior without arbitrary waits.

### Implementation

- [x] T214 [R1] Preserve complete workflow/Method metadata in the modern `SampleTestSection` selected-test model for direct and panel selections (`38b177f7e`).
- [x] T215 [R1] Derive the visible Microbiology Program from selected test workflows through shared order state and surface a named configuration error when the Program cannot be resolved by stable identity (`38b177f7e`).
- [x] T216 [R1] Integrate reusable Carbon microbiology detail controls into the supported Program/order flow with required/defaulted Method and the product-safe control semantics in FR-002 (`38b177f7e`).
- [x] T217 [R1] Confirm before discarding entered microbiology details when the final culture test is removed (`38b177f7e`). Program-change confirmation remains part of T218 before complete save-path evidence.
- [ ] T218 [R1] Submit the modern order details through the existing service-layer order-save path and prove idempotent case/detail persistence. Add no migration unless a real data-model change is required.
- [x] T219 [R1] Make the legacy compatibility flow retain the same complete selected-test metadata and consume the shared detail controls without creating a second implementation (`38b177f7e`).
- [x] T220 [P] [R1] Add or update English React Intl source keys only and use existing Carbon components/tokens without a package upgrade (`38b177f7e`).

### Artifact, UAT, And Evidence Synchronization

- [x] T221 [R1] Reconcile `spec.md`, `plan.md`, `tasks.md`, the engineering crosswalk, gap analysis, scope rulings, mock comparison, and human UAT contract to the pinned authority and corrected status vocabulary.
- [ ] T222 [R1] Publish a separate M-03 Grist story with concise steps for supported navigation, Program derivation, controls/defaults, save-to-case, non-culture behavior, and discard confirmation; verify the live AMR JSON revision and rendered overlay.
- [ ] T223 [R1] Run the focused Vitest suite for modern order state and microbiology controls and record the exact command/results.
- [ ] T224 [R1] Run focused JUnit 4/controller/integration tests plus architecture/ORM checks for the order save path and record the exact command/results.
- [ ] T225 [R1] Validate and audit the registered Playwright spec, then run the narrow `core-app` journey using locator readiness and web assertions only.
- [ ] T226 [R1] Capture and manually compare stable desktop/mobile M-03 and M-07 screenshots against the pinned OpenELIS Work sources; document every intentional difference.
- [ ] T227 [R1] Run pinned `tools/code-qa` alignment, meaningful-coverage, simplicity, and evidence workflows against the remediation diff.
- [ ] T228 [R1] Run Spotless, targeted Prettier/ESLint, relevant builds, and `git diff --check` before commit.
- [ ] T229 [R1] Deploy the exact remediation head with compatible review tooling to `amr.openelis-global.org`; verify health, supported routes, application SHA, checklist revision, and overlay rendering.
- [ ] T230 [Human UAT] Piotr records Pass/Fail/N/A and notes for every required M-03 story step against the exact deployed revisions. Automation cannot complete this task.
- [ ] T231 [R1] Push the branch and open an official stacked PR targeting `feat/782-ogc-782-microbiology-m10-whonet-export`; include scope, source revision, evidence, UAT state, and known follow-on modules.
- [ ] T232 [Follow-up] Correct the M-03 v1/v2 visual, fallback, mixed-workflow, duplicate-text, and implementation-leakage issues in a separate `DIGI-UW/openelis-work` product-source PR.
- [ ] T233 [Follow-up] Extract Macro Library core/runtime/administration into its own PR and UAT stack; retain only microbiology consumer integration in a small integration PR and review it on a separate macro deployment when available.

### M-04 Case Workbench Alignment

- [x] T234 [R1] Add focused service/controller and React tests for visible `UNASSIGNED` classification, early-case workflow change, required reason and compatible Method, result-preservation warning, final-release block, and audit history (`cfbdb9025`).
- [x] T235 [R1] Implement the inline Change Workflow action and hold profile-dependent work while a case is unassigned; add durable state only where the existing model cannot preserve the required history (`cfbdb9025`).
- [x] T236 [R1] Add sibling-case links in the case header with live workflow/stage context and state-preserving navigation (`cfbdb9025`).
- [x] T237 [R1] Replace generic setup activity entry with explicit Start inoculation/Add subculture behavior, parent lineage, barcode-ready identifiers, automatic timeline projection, and shared reagent-lot selection (`131ba1471`).
- [x] T238 [R1] Restrict Timeline manual entry to notes/observations and project automatic domain/audit events without creating a duplicate history system. Typed events remain AUTO, and Add note stores an INTERNAL SampleItem Note with stable case subject through the existing audited Note service (`131ba1471`, `cf8ac8c21`).
- [x] T239 [R1] Complete two-pass isolate behavior: Gram/colony preliminary data, identified status with method/confidence/significance, disabled AST until identification, preliminary report projection, and edit versus post-final reidentify actions (`14164589f`).
- [x] T240 [R1] Add header/isolate critical entry points and reuse the existing NCE/rejection workflow for Report NCE and Mark lost; do not introduce microbiology-only NCE storage. The case now defaults ordinary reports to configured Pre-analytical context and shows the linked NCE count (`28669ef18`, `b2ad1013a`, `f7aa5e20e`, `8e3337870`, `a41ad5617`). Exact-SHA deployed execution and separate human UAT remain in T242.
- [x] T241 [R1] Focus the case on its deterministic current step, complete collapsed one-line order/origin/location/sets/exposure context with clinical history first when expanded, resolve the last activity actor for display, and add the sticky stage-appropriate action area without duplicating progress state (`f3f1f3a4f`, `a41ad5617`).
- [ ] T242 [R1] Add registered Playwright and separate Grist stories for classification, sibling navigation, setup/subculture, timeline notes, isolate identification, critical entry points, NCE/lost specimen, and final-lock behavior. The service-created classification journey (`a10a2d13e`), primary/subculture/Note journey (`131ba1471`, `cf8ac8c21`), two-pass isolate/AST-gating journey (`14164589f`), current-step journey (`f3f1f3a4f`), and NCE/lost journey (`8e3337870`) are registered in `core-app`; exact-SHA deployed runtime evidence, separate Grist stories, and human UAT remain open.

### M-05 AST Alignment

- [x] T243 [R1] Add failing service and React tests for ordered-panel provenance, panel/version and standard/version snapshots, technique-derived units, and matched-breakpoint display. Ordered-panel and immutable version snapshots are implemented in `92fb708c8`; `b0dd23456` adds persisted reading source/matched basis/units, Carbon display, and explicit no-breakpoint guidance. `72ef3b152` models disk diffusion, VITEK 2, Phoenix, Etest, and broth microdilution as laboratory techniques, derives MIC/zone measurement type on the server, performs technique-first breakpoint lookup with an explicit legacy fallback, and adds migration `075` with PostgreSQL update/rollback coverage. `dd7e63216` exposes `measurementType` on reading responses while retaining `method` only as a deprecated compatibility alias. Exact-SHA browser evidence and human acceptance remain in T250.
- [x] T244 [R1] Make AST setup confirm the upstream panel by default; require an audited reason for any panel or drug-set adjustment and retain the exact changed order used for entry and reporting. `92fb708c8` implements confirmation-first setup and immutable panel/standard snapshots; `1a198a765` adds immutable ordered-drug snapshots and server entry guards; `2dfb8221b` adds Carbon panel/individual-drug adjustment with server-validated active membership and one required reason; `df56940f9` blocks review until every ordered drug has a reading and projects only each drug's current reading in snapshot order. The linked culture `Analysis` remains the standard patient-report anchor because the repository has no authoritative Antibiotic-to-Test mapping. Exact-SHA browser and human acceptance remain in T250.
- [x] T245 [R1] Add inline original-to-override history and supervisor revert with actor, time, reason, and unchanged original reading (`30d8e988a`). The existing Validation/Admin bundles gate override and revert; exact-SHA browser and human acceptance remain in T250.
- [x] T246 [R1] Add analyzer-result and QC lifecycle behavior: awaiting results, results-in review, mismatch/no-breakpoint/QC blockers, accept results, invalidate-and-repeat, and named reconciliation handoff for unmatched events. `5f1b89922` adds the durable run lifecycle, blockers, invalidation/repeat behavior, and PostgreSQL migration coverage; `0c33223fb` adds the idempotent analyzer event envelope and exposes failed events through the existing Analyzer Import Issues reconciliation surface; `87f09d0bc` adds the Carbon pending/results/QC review states and focused interaction tests. Exact-SHA browser, Grist, deployment, and human acceptance remain in T250.
- [x] T247 [R1] Capture analyzer provenance and keep analyzer organism identity informational; flag disagreement with the case isolate without silently replacing it. `5f1b89922` retains analyzer/card/software/QC/message/result provenance without changing case isolate identity, and `87f09d0bc` displays that provenance and the organism disagreement warning. Exact-SHA browser and human acceptance remain in T250.
- [ ] T248 [R1] Extend repeat/retest to whole-panel or single-antibiotic scope and converge case, worklist, and existing NCE Retest entry points on a new preserved run. `ee4e4c6e2` makes the existing ordered-work snapshot represent whole-panel or selected-drug scope without a migration; `f0d51a5cd` adds the Carbon scope interaction; `c7d84c535` makes the NCE Retest disposition validate the case/source and call the same preserved-run service. Case and NCE origins are complete; the authoritative AST-row "Set up new AST run" entry point remains T254 and exact-SHA acceptance remains T250.
- [x] T249 [R1] Add AST progress counts that include runs complete, significant isolates awaiting setup, and isolates pending identification. `ca7aa0491` computes the counts with release readiness in one server transaction, excludes invalidated/rerun-required history from active totals, adds the missing unidentified-isolate release blocker, and renders the authoritative combined count in the AST header. Focused readiness and Carbon tests pass; exact-SHA browser and human acceptance remain in T250.
- [ ] T250 [R1] Add registered Playwright and Grist stories for manual and analyzer AST review, override/revert, QC failure, accept, repeat scope, and report-readiness gating.

### M-07 Worklist Alignment

- [x] T251 [R1] Add URL/parser/service tests for canonical `grain=cultures|ast`, per-grain status, and state-preserving AST-run case links. `398a69003` adds the server/URL contract; `bd27e76d9` proves the exact isolate and run open in the case without prop-to-state synchronization effects.
- [x] T252 [R1] Add the server-side AST-run projection and per-grain summaries without introducing worklist writes or per-case ownership. `398a69003` projects active runs plus significant isolates pending setup and excludes invalidated, rerun-required, and cancelled history. Accession, patient, specimen, panel-name, and last-activity enrichment remains T276.
- [x] T253 [R1] Build one Carbon worklist with a Cultures/AST segmented control, grain-specific DataTable headers/rows, deterministic due actions, summary filters, and empty states. `bd27e76d9` completes the shared Carbon structure, URL-backed cards, columns, exact-run links, and compact source-aligned layout; `1d838d72a` restores the distinct positive-signal summary and subculture/Gram-stain due action. Visual runtime evidence remains T257.
- [ ] T254 [R1] Add row overflow commands that navigate to existing case actions; do not mutate clinical state directly from the worklist. `1d838d72a` adds direct Carbon overflow children with an accessible trigger, all four Culture commands, AST Open/Edit/View audit, case-scoped positive/no-growth confirmations, and focused tests proving the worklist does not perform the clinical write. The authoritative AST "Set up new AST run" command must still converge on the preserved repeat flow from T248.
- [ ] T255 [R1] Add the resistance-hit strip, collapsible recent activity, results-in badge, awaiting-analyzer state, and visibly disabled future controls with tooltips.
- [ ] T256 [R1] Implement refresh that preserves focus, scroll, URL state, and selected row context; enforce the existing case-view permission rather than authentication alone.
- [ ] T257 [R1] Add desktop/mobile Carbon interaction, keyboard, URL-reload, and 200-row performance tests without arbitrary waits.
- [ ] T258 [R1] Add separate Culture Worklist and AST Worklist Grist stories and registered Playwright journeys.

### Cross-Stack Drift Discovered During M-07

- [x] T275 [R1] Restore the authoritative culture progression and actions: incubating to positive signal to growth detected, automatic analyzer positive signal, manual Mark positive, and Mark no growth. `1d838d72a` adds the distinct state and constraint, authenticated idempotent analyzer consumer with reconciliation, case-scoped manual confirmations, worklist projection, and focused JUnit/Carbon coverage. The PostgreSQL rollback harness is committed but could not execute locally because no Docker daemon was available; CI/exact-SHA validation remains required with T257/T258.
- [ ] T276 [R1] Enrich both worklist grains with the authoritative lab/accession number, patient display, specimen display, panel name, and last-activity actor using bounded batch reads suitable for 200 rows. Do not issue per-row service/DAO queries or duplicate patient/specimen data into microbiology tables.

### M-12 And NFR Qualification

- [ ] T259 [R1] Add shared-component tests for required/optional/substitute lot rules, filtered eligible lots, oldest-expiry ordering, QC/expiry race validation, and specific corrective messages.
- [ ] T260 [R1] Align the shared lot picker and service validation in both culture and AST hosts; add scanner-ready lot/card entry using an existing barcode input pattern.
- [ ] T261 [R1] Keep Test Catalog linkage administration, reagent reverse view, inventory quantities, and seed definitions as explicit external dependencies; do not duplicate them in microbiology.
- [ ] T262 [R1] Prove the picker latency and WCAG behavior at the source ceiling with service-created fixtures.
- [ ] T263 [R1] Qualify the case and both worklist grains at source scale using service-layer fixtures; record server and browser timings separately.
- [ ] T264 [R1] Audit keyboard, focus, labels, status text, contrast, and compact layouts for every touched action with axe plus direct interactions.
- [ ] T265 [R1] Audit all new writes for authenticated actor, permission, atomic transition, and preserved history; add missing negative tests.
- [ ] T266 [R1] Record offline/intermittent-connectivity as not implemented unless a shared application pattern is selected and proven; do not build a microbiology-only queue.

### Full-Stack Evidence And Delivery

- [ ] T267 [R1] Update the M-03, M-04, M-05, M-07, M-12, and NFR crosswalk after every slice; statuses must distinguish code, automated evidence, deployment, and human acceptance.
- [ ] T268 [R1] Publish separate Grist stories per workflow outcome, verify the live AMR JSON revision and rendered overlay, and keep milestone stories distinct rather than combining them into one checklist.
- [ ] T269 [R1] Run pinned `tools/code-qa` meaningful coverage, spec/code alignment, simplicity, and evidence workflows across the complete remediation diff.
- [ ] T270 [R1] Capture stable desktop/mobile comparisons against each authoritative HTML mock and record every intentional behavioral or visual deviation.
- [ ] T271 [R1] Push and maintain one official remediation PR stacked on M10; update its body with exact source revision, migration scope, tests, evidence, UAT revision, and unresolved exclusions.
- [ ] T272 [R1] Deploy the exact top-of-micro-stack SHA plus compatible review tooling to AMR and verify app SHA, health, routes, story separation, overlay stability, and no refresh loop.
- [ ] T273 [Human UAT] Piotr completes required per-story Pass/Fail/N/A and notes against the exact deployed revisions; automation cannot complete this task.
- [ ] T274 [Macro split] Create a separate Macro Library feature/spec/PR/UAT stack and review deployment; add microbiology consumer integration only after that base is available.

## Dependencies & Execution Order

- M1 blocks all later milestones.
- M2 depends on M1 because case identity needs workflow/reference config.
- M3 depends on M2 because routing creates cases.
- M4 depends on M3 because the workbench opens routed cases.
- M5 depends on M4 because AST belongs to identified isolates.
- M6 depends on M5 because worklist urgency and review flags include AST state.
- M7 depends on M6 because release/readiness includes critical communication and
  worklist-visible blockers.
- Phase 8 (MVP-gap remediation) depends on M7 because it extends the same held
  branch/PR with FR-002, M-11, and M-05 gap coverage before merge.
- Final MVP acceptance depends on Phase 8.
- Phase 12 deployment depends on the story-closure and artifact-reconciliation
  commits. Human UAT depends on the exact deployed SHA and matching live Grist
  revision.
- R1 depends on the M10 follow-on head. Its implementation gate depends on the
  pinned product behavior, and its final acceptance depends on matching
  Playwright, visual, deployed, and human UAT evidence.

## Parallel Opportunities

- M1 reference valueholders, DAOs, and frontend Test Catalog field tests can be
  split after the Liquibase design is agreed.
- M2 service tests for case state, isolate lifecycle, and DTO compilation can be
  written in parallel.
- M4 React panels can be split between case header, timeline, and isolate work
  once REST contracts are stable.
- M5 AST backend interpretation tests and frontend AST entry tests can proceed
  in parallel after the M5 contract is fixed.
- M6 worklist and critical communication UI can proceed in parallel after the
  shared service DTOs are fixed.
- M7 WHONET readiness and report readiness panels can proceed in parallel after
  release blockers are defined.

## TDD Rules

- Write the listed tests first and verify they fail for the expected reason.
- Do not mark a milestone PR ready until its focused test command passes.
- Do not stub the backend mutation under test in Playwright evidence.
- Do not use raw `npx playwright test`; use `npm run pw:test`.
- Do not add new Cypress tests.
- Every Playwright test must pass registration validation, `/audit-playwright`,
  and at least one narrow `core-app` or `core-demo` run before PR review.
- Use `/debug-playwright` with screenshot/trace evidence before changing a
  failing Playwright selector or assertion.
- Use `DIGI-UW/code-qa` before final review: `meaningful-test-coverage` must
  reject theater tests, `spec-code-alignment` must reconcile implemented code
  with `spec.md`/`plan.md`/`tasks.md`, `simplicity-review` must catch bloat, and
  `evidence-bundle` must package final Playwright proof without committing
  binary media.
