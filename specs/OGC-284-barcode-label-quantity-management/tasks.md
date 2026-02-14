# Tasks: Barcode Label Quantity Management (OGC-284)

**Input**: Design documents from
`/specs/OGC-284-barcode-label-quantity-management/`  
**Prerequisites**: `plan.md` (required), `spec.md` (required),
`research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Organization Rule (OpenELIS Override)**: Tasks are organized by **Milestone**
per Constitution Principle IX, with tests mandatory and ordered before
implementation within each milestone.

## Format: `- [ ] [ID] [P?] [Story?] Description with file path`

- **[P]**: Parallelizable task (different files, no direct dependency)
- **[Story]**: Story mapping for traceability (`[US1]`, `[US2]`, `[US3]`)

---

## Milestone Dependency Graph

```mermaid
graph LR
    M1[M1: Review-Driven Remediation]
```

---

## Milestone M1: Review-Driven Remediation

**Branch Suffix**: `m1-review-remediation`  
**Target Branch Name**:
`feat/OGC-284-barcode-label-quantity-management-m1-review-remediation`  
**Scope**: Close assessment gaps (BlockLabel lookup risk, field toggle mismatch,
backend i18n coverage, CI/review closure)  
**User Stories Covered**: US1, US2, US3  
**Depends On**: None

### Independent Test Criteria

- **US1**: Admin updates barcode quantity defaults/max values; reload returns
  persisted values; malformed stored values fall back safely.
- **US2**: Generic sample order saves explicit and omitted label quantities with
  correct sample/sample-item upsert behavior.
- **US3**: Label generation remains stable with malformed config and respects
  enabled/disabled optional field toggles.

### M1 Tasks

#### M1 Setup & Branch Management

- [ ] T001 Create milestone branch
      `feat/OGC-284-barcode-label-quantity-management-m1-review-remediation`
      from `develop` for `/workspace`
- [ ] T002 Rebase milestone branch onto latest `develop` and resolve conflicts
      affecting barcode modules in `/workspace`
- [ ] T003 Capture remediation checklist baseline from assessment in
      `specs/OGC-284-barcode-label-quantity-management/quickstart.md`

#### M1 Tests First (MANDATORY TDD)

- [ ] T004 [P] [US1] Add/extend controller test for barcode configuration
      round-trip persistence in
      `src/test/java/org/openelisglobal/barcode/BarcodeConfigurationRestControllerTest.java`
- [ ] T005 [P] [US1] Add/extend controller test for malformed numeric fallback
      behavior in
      `src/test/java/org/openelisglobal/barcode/BarcodeConfigurationRestControllerTest.java`
- [ ] T006 [P] [US1] Add/extend service test for safe integer/float parsing
      boundaries in
      `src/test/java/org/openelisglobal/barcode/BarcodeInformationServiceTest.java`
- [ ] T007 [P] [US1] Add backend localization key regression test coverage for
      new `barcode.label.info.*` keys in
      `src/test/java/org/openelisglobal/barcode/BarcodeInformationServiceTest.java`
- [ ] T008 [P] [US2] Extend upsert test coverage for existing sample barcode
      records in
      `src/test/java/org/openelisglobal/barcode/service/BarcodeInfoServiceImplTest.java`
- [ ] T009 [P] [US2] Extend upsert test coverage for existing sample-item barcode
      records in
      `src/test/java/org/openelisglobal/barcode/service/BarcodeInfoServiceImplTest.java`
- [ ] T010 [P] [US2] Add test for default quantity application when
      `numOrderLabels`/`numSpecimenLabels` are omitted in
      `src/test/java/org/openelisglobal/barcode/service/BarcodeInfoServiceImplTest.java`
- [ ] T011 [P] [US3] Create/extend test for Block label specimen-type resolution
      without unscoped query behavior in
      `src/test/java/org/openelisglobal/barcode/labeltype/BlockLabelTest.java`
- [ ] T012 [P] [US3] Create/extend test for slide optional fields honoring
      configuration toggles in
      `src/test/java/org/openelisglobal/barcode/labeltype/SlideLabelTest.java`
- [ ] T013 [P] [US3] Create/extend test for freezer optional fields honoring
      configuration toggles in
      `src/test/java/org/openelisglobal/barcode/labeltype/FreezerLabelTest.java`
- [ ] T014 [P] [US1] Add/extend frontend unit test coverage for admin barcode
      configuration form field behavior in
      `frontend/src/components/admin/barcodeConfiguration/BarcodeConfiguration.test.js`
- [ ] T015 [P] [US1] Add/extend frontend i18n key rendering test for new barcode
      labels in
      `frontend/src/components/admin/barcodeConfiguration/BarcodeConfiguration.test.js`
- [ ] T016 [US1] Run targeted backend tests for configuration and barcode info
      services, then record command/results in
      `specs/OGC-284-barcode-label-quantity-management/quickstart.md`
- [ ] T017 [US1] Run targeted frontend unit tests, then record command/results in
      `specs/OGC-284-barcode-label-quantity-management/quickstart.md`
- [ ] T018 [US1] Verify tests fail for new expectations before implementation and
      record RED-phase evidence in
      `specs/OGC-284-barcode-label-quantity-management/quickstart.md`

#### M1 Implementation

- [ ] T019 [US3] Refactor Block label lookup flow to remove unscoped
      `QuestionnaireResponse` query from
      `src/main/java/org/openelisglobal/barcode/labeltype/BlockLabel.java`
- [ ] T020 [US3] Implement pathology label context resolver (or equivalent
      helper) in
      `src/main/java/org/openelisglobal/barcode/service/PathologyLabelContextService.java`
- [ ] T021 [US3] Integrate pathology label context resolver into barcode service
      pipeline in
      `src/main/java/org/openelisglobal/barcode/BarcodeInformationService.java`
- [ ] T022 [US3] Update block-label construction call sites to pass pre-resolved
      specimen context in
      `src/main/java/org/openelisglobal/barcode/labeltype/LabelFactory.java`
- [ ] T023 [US3] Implement slide label optional-field toggle mapping in
      `src/main/java/org/openelisglobal/barcode/labeltype/SlideLabel.java`
- [ ] T024 [US3] Implement freezer label optional-field toggle mapping in
      `src/main/java/org/openelisglobal/barcode/labeltype/FreezerLabel.java`
- [ ] T025 [US1] Align backend configuration mapping for exposed toggles and
      defaults in
      `src/main/java/org/openelisglobal/barcode/form/BarcodeConfigurationForm.java`
- [ ] T026 [US1] Align backend config load/save behavior for new toggle/quantity
      keys in
      `src/main/java/org/openelisglobal/barcode/service/BarcodeConfigServiceImpl.java`
- [ ] T027 [US1] Ensure safe parse + fallback utilities cover all touched numeric
      fields in
      `src/main/java/org/openelisglobal/barcode/util/BarcodeConfigUtil.java`
- [ ] T028 [US2] Confirm generic sample order default label quantities are applied
      before persistence in
      `src/main/java/org/openelisglobal/genericsample/service/GenericSampleOrderServiceImpl.java`
- [ ] T029 [US2] Confirm sample/sample-item barcode info upsert and dedup behavior
      for update flows in
      `src/main/java/org/openelisglobal/barcode/service/BarcodeInfoServiceImpl.java`
- [ ] T030 [US2] Confirm pathology-specific quantity persistence mapping for
      specimen/block/slide/freezer values in
      `src/main/java/org/openelisglobal/barcode/service/BarcodeInfoServiceImpl.java`
- [ ] T031 [US1] Add missing backend label info message keys in
      `src/main/resources/languages/message_en.properties`
- [ ] T032 [US1] Add matching backend label info message keys in
      `src/main/resources/languages/message_fr.properties`
- [ ] T033 [US1] Align frontend translation keys with updated/admin-visible
      barcode labels in `frontend/src/languages/en.json` and
      `frontend/src/languages/fr.json`

#### M1 Verification, CI, and Review Closure

- [ ] T034 [US1] Re-run targeted backend tests and record GREEN-phase evidence in
      `specs/OGC-284-barcode-label-quantity-management/quickstart.md`
- [ ] T035 [US1] Re-run targeted frontend tests and record GREEN-phase evidence in
      `specs/OGC-284-barcode-label-quantity-management/quickstart.md`
- [ ] T036 [US3] Run impacted Cypress test file(s) individually and record output
      review in `specs/OGC-284-barcode-label-quantity-management/quickstart.md`
- [ ] T037 Re-run failing PR check workflow(s), capture job references and status
      notes in `specs/OGC-284-barcode-label-quantity-management/quickstart.md`
- [ ] T038 Resolve all open review threads with file/line evidence and capture
      closure checklist in
      `specs/OGC-284-barcode-label-quantity-management/quickstart.md`
- [ ] T039 Format touched code with Spotless + Prettier and record completion in
      `specs/OGC-284-barcode-label-quantity-management/quickstart.md`
- [ ] T040 Create/update PR for milestone branch and include verification summary
      from `specs/OGC-284-barcode-label-quantity-management/quickstart.md`

---

## Dependencies & Execution Order

### Milestone Dependencies

- **M1**: No prerequisite milestones; starts immediately.

### Inside M1 (Strict Order)

1. Setup & branch tasks (T001-T003)
2. Test creation/execution tasks (T004-T018) - **must complete before
   implementation**
3. Implementation tasks (T019-T033)
4. Verification + CI + review closure tasks (T034-T040)

---

## Parallel Opportunities

The following tasks can run in parallel after setup (T001-T003):

- **Backend tests in parallel**: T004, T005, T006, T007, T008, T009, T010,
  T011, T012, T013
- **Frontend test tasks in parallel**: T014, T015
- **Implementation tasks in parallel by file group**:
  - Label classes: T023, T024
  - i18n bundles: T031, T032, T033

---

## Implementation Strategy

### MVP Scope

MVP within this milestone is **US1 + US2 stabilization**:

- configuration persistence/fallback correctness,
- generic sample order quantity persistence reliability,
- localization coverage for new label fields.

### Full Milestone Scope

After MVP stabilization, complete US3 resilience tasks:

- remove unscoped BlockLabel lookup,
- ensure slide/freezer toggle behavior matches configuration,
- close CI and PR review threads with evidence.

### Delivery Guidance

- Prefer small commits grouped by risk area:
  1. tests,
  2. label-generation refactor,
  3. i18n/config alignment,
  4. CI/review closure.
- Keep each commit independently reviewable and reversible.
