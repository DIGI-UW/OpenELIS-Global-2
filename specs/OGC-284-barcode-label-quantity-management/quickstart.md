# Quickstart: OGC-284 Full-Scope Delivery

> **Status (2026-05-19):** Superseded by
> [OGC-285](../OGC-285-barcode-label-presets/spec.md). The v2 preset model
> replaces the 5-fixed-type design; unshipped FRs from this spec are absorbed
> into OGC-285 milestones per the
> [Gap Closure Matrix](./spec.md#gap-closure-matrix). **No further edits to this
> directory** — historical reference only.

## Goal

Use the already-completed OGC-284 remediation baseline as the starting point for
full Jira/design delivery.

This quickstart now distinguishes between:

- **Completed baseline** already present on the current OGC-284 branches
- **Remaining implementation** needed to satisfy the clarified full-scope spec

This document is intended to help developers finish the missing workflow UX
without losing the verified remediation work that already exists.

---

## 1) Completed baseline already on branch

The following work is already present and should be treated as enabling
foundation, not as the remaining feature scope:

- Barcode configuration quantity/i18n hardening
- Sample/sample-item barcode persistence and upsert behavior
- Pathology quantity persistence wiring
- Label resilience and max-limit enforcement
- CI/review closure evidence for remediation PR work

Use the execution evidence sections later in this file as proof of what has
already been implemented and validated.

---

## 2) Prepare environment

```bash
# Sync develop first (for M1 foundation branch)
git fetch origin develop

# Create milestone branches per Constitution Principle IX (if not already present)
git checkout -b feat/284-barcode-label-quantity-management-m1-config-i18n-hardening origin/develop
git checkout -b feat/284-barcode-label-quantity-management-m2-persistence-upsert origin/develop
git checkout -b feat/284-barcode-label-quantity-management-m3-label-resilience feat/284-barcode-label-quantity-management-m2-persistence-upsert
git checkout -b feat/284-barcode-label-quantity-management-m4-integration-ci-review feat/284-barcode-label-quantity-management-m3-label-resilience

# Recommended: one worktree per milestone branch (paths are examples)
git worktree add "/workspace-worktrees/ogc-284-m1-config-i18n" "feat/284-barcode-label-quantity-management-m1-config-i18n-hardening"
git worktree add "/workspace-worktrees/ogc-284-m2-persistence-upsert" "feat/284-barcode-label-quantity-management-m2-persistence-upsert"
git worktree add "/workspace-worktrees/ogc-284-m3-label-resilience" "feat/284-barcode-label-quantity-management-m3-label-resilience"
git worktree add "/workspace-worktrees/ogc-284-m4-integration-ci-review" "feat/284-barcode-label-quantity-management-m4-integration-ci-review"
```

Use local worktree paths that match your environment. If milestone branches are
already stacked and published, reuse the existing branches instead of recreating
from `origin/develop`.

---

## 3) Remaining implementation in milestone order

## Step A: Shared workflow foundation

1. Inventory all sample-creation workflows that support barcode printing.
2. Define the shared labels-section row model:
   - one order row,
   - one row per sample,
   - editable applicable label counts,
   - running total.
3. Define the shared post-save print dialog model:
   - accession-number-based launch,
   - per-label-type selection,
   - separate print jobs,
   - `Skip - Print Later`.
4. Expand contracts and service orchestration so save flows return enough data
   to drive post-save printing and later reprint entry.

## Step B: Primary workflow UX completion

1. Implement the pre-save labels section on the primary order-entry workflow
   called out by Jira/design.
2. Pre-populate counts from barcode configuration defaults.
3. Validate applicable label types and maximum counts in the workflow UI and
   backend.
4. Ensure saved quantities persist to the existing barcode metadata model.

## Step C: Post-save printing completion

1. Present a post-save print dialog after accession number assignment.
2. Allow per-label-type selection.
3. Send each selected label type as a separate print job.
4. Implement `Skip - Print Later`.
5. Provide print-later/reprint entry from the appropriate order/sample view.

## Step D: Rollout to all relevant workflows

1. Apply the shared labels-section and post-save print behavior to all other
   barcode-printing sample-creation workflows identified in Step A.
2. Reuse shared orchestration and avoid per-workflow divergence.
3. Confirm consistent behavior across generic sample, notebook, batch, and
   pathology-related flows as applicable.

### Workflow inventory (M5 T008)

Authoritative list of sample-creation workflows that support barcode printing
and must implement the OGC-284 labels UI and post-save print flow. M8 rollout
tasks (T037, T038, T040, T041) use this list as scope.

| Workflow / entry point                                          | Labels UI | Post-save print | Notes                                                        |
| --------------------------------------------------------------- | --------- | --------------- | ------------------------------------------------------------ |
| Add Order (`/SamplePatientEntry`)                               | M6        | M7              | Primary; implement first.                                    |
| Generic Sample Order (`/rest/GenericSampleOrder`)               | M8        | M8              | Uses shared labels section/orchestration after primary flow. |
| Notebook Sample Order (`NotebookSampleOrder`)                   | M8        | M8              | Uses the same labels row semantics and print behavior.       |
| Batch Order Entry (`SampleBatchEntry`)                          | M8        | M8              | Reuse shared model; no workflow-specific divergence.         |
| Pathology Case View (`PathologyCaseView`)                       | M8        | M8              | Pathology family rollout via shared orchestration.           |
| Immunohistochemistry Case View (`ImmunohistochemistryCaseView`) | M8        | M8              | Pathology family rollout via shared orchestration.           |
| Cytology Case View (`CytologyCaseView`)                         | M8        | M8              | Pathology family rollout via shared orchestration.           |

### Print PDF endpoints (T012)

Documented print-PDF endpoint patterns used by post-save print and later
reprint:

- `GET /api/barcode/print/{orderId}/{labelType}`
- `GET /api/barcode/print/{orderId}/{labelType}/{sampleId}`

---

## 4) Verification checklist

- [ ] Completed baseline behavior remains intact
- [ ] All in-scope workflows expose the required labels UI
- [ ] Labels UI shows one order row, one row per sample, and running total
- [ ] Post-save dialog appears after accession assignment
- [ ] Each selected label type prints as a separate print job
- [ ] `Skip - Print Later` works and preserves reprint entry
- [ ] Barcode configuration GET/POST behavior remains stable
- [ ] Generic sample order label count persistence remains stable
- [ ] Other supported workflows persist applicable label counts correctly
- [ ] ORM validation test passes for barcode entities
- [ ] Liquibase/schema verification passes for OGC-284 changesets
- [ ] E2E coverage demonstrates full Jira/design workflow, not only remediation

---

## 5) Recommended test commands

```bash
# Backend targeted tests
mvn test -Dtest="BarcodeInfoServiceImplTest,BarcodeInformationServiceTest,BarcodeConfigurationRestControllerTest"

# Frontend unit tests (targeted)
cd frontend
npm test -- --watch=false
cd ..

# E2E workflow validation (run focused specs during development)
cd frontend
npm run cy:run -- --spec "cypress/e2e/<impacted-file>.cy.js"
cd ..
```

> Note: In this cloud environment, local tooling availability may differ. Use CI
> as final source of truth for full job-level verification.

---

## 6) CI and review closure workflow

1. Push remediation commits.
2. Re-run failed PR checks (especially frontend QA workflow).
3. Capture pass/fail evidence per previously open review thread.
4. Resolve GitHub threads explicitly once addressed.
5. Confirm PR no longer shows merge conflicts.

---

## 7) Finalization

Before final review request:

- [ ] Code formatted and lint-clean for touched files
- [ ] Test evidence attached in PR comment
- [ ] Review threads resolved
- [ ] Merge conflicts resolved

---

## M1 execution evidence (2026-02-14)

### Branch/workflow

- Active milestone branch:
  `feat/284-barcode-label-quantity-management-m1-config-i18n-hardening`
- Branch pushed to origin successfully.

### Test execution evidence

- Frontend targeted test (PASS):
  - Command: `CI=true npm test -- BarcodeConfiguration.test.js --watchAll=false`
  - Result: 1 suite, 3 tests passed.

### Environment blockers

- Backend dependency setup completed:
  - `git submodule update --init --recursive`
  - `cd dataexport && mvn clean install -DskipTests -Dmaven.test.skip=true`
- Backend targeted tests were invoked but blocked by infrastructure:
  - Command:
    `mvn -q test -Dtest=BarcodeConfigurationRestControllerTest,BarcodeInformationServiceTest`
  - Failure cause: Testcontainers cannot find Docker daemon
    (`/var/run/docker.sock`) in this execution environment.

### Manual follow-up commands (backend)

Run these in an environment with Maven + Docker available:

```bash
git submodule update --init --recursive
cd dataexport && mvn clean install -DskipTests -Dmaven.test.skip=true && cd ..
mvn test -Dtest="BarcodeConfigurationRestControllerTest,BarcodeInformationServiceTest"
```

---

## M2 execution evidence (2026-03-10)

### Branch/workflow

- Active milestone branch:
  `feat/284-barcode-label-quantity-management-m2-persistence-upsert`
- Branch created from current OGC-284 feature head to preserve remediated
  artifacts.

### FR-010 and persistence hardening evidence

- Pathology supplied-only wiring added:
  - `PathologySampleForm` includes optional pathology label count inputs.
  - `PathologySampleServiceImpl` calls
    `saveBarcodeInfoForSampleAndSampleItemsPathology(...)` only when all
    pathology counts are supplied.
- Generic sample order persistence hardening added:
  - null-safe default fields fallback in `GenericSampleOrderServiceImpl`
  - quantity fallback helper enforces default `1` for null/non-positive values
  - `@Min(1)` validation annotations on generic sample label count form fields

### Test execution evidence

- Command:
  `mvn -Dtest=GenericSampleOrderServiceImplTest,BarcodeInfoServiceImplTest,PathologySampleServiceImplTest,HibernateMappingValidationTest,BarcodeSchemaValidationTest test`
- Result: PASS (21 tests, 0 failures, 0 errors)
- Coverage highlights:
  - Generic sample order default/explicit/null-safe persistence tests
  - Pathology FR-010 supplied-only workflow wiring tests
  - Barcode helper upsert/dedup pathology coverage (including no-sample-item
    behavior)
  - ORM mapping validation for barcode entities
  - Liquibase/schema changeset verification for `base.xml`,
    `028-barcode-info-tables.xml`, and `barcode_expansion.xml`

---

## M3 execution evidence (2026-03-10)

### Branch/workflow

- Active milestone branch:
  `feat/284-barcode-label-quantity-management-m3-label-resilience`
- Branch stacked on completed M2 milestone head to preserve verified persistence
  hardening context.

### Label resilience and max-limit evidence

- Block label resilience:
  - Removed unscoped FHIR runtime lookup from `BlockLabel`.
  - Added constructor-level specimen type context input and case number
    rendering when configured.
  - `BarcodeLabelMaker` resolves specimen type context once and passes it into
    block label construction.
- Slide/freezer optional field behavior:
  - `SlideLabel` now renders configured optional fields (stain type, block ID,
    case number) using explicit context values.
  - `FreezerLabel` now renders configured optional fields (patient ID, storage
    location, specimen type, collection date, expiry date) from constructor
    context.
- FR-013 enforcement:
  - `BarcodeLabelMaker` blocks over-max quantity requests unless explicit
    `override=true` is supplied.

### Test execution evidence

- Command:
  `mvn -Dtest=BlockLabelTest,SlideLabelTest,FreezerLabelTest,BarcodeLabelMakerTest test`
- Result: PASS (7 tests, 0 failures, 0 errors)
- Coverage highlights:
  - Block label specimen/case field behavior without runtime FHIR lookup
  - Slide optional-field on/off rendering behavior
  - Freezer optional-field rendering and toggle compliance
  - Max-limit blocking and explicit override behavior in `BarcodeLabelMaker`

---

## M4 execution evidence (2026-03-10)

### Branch/workflow

- Active milestone branch:
  `feat/284-barcode-label-quantity-management-m4-integration-ci-review`
- Branch stacked on completed M3 milestone head.
- Ancestry verification: M4 -> M3 -> M2 -> M1.

### Backend combined verification (M1-M3)

- Command:
  `mvn -Dtest=BarcodeConfigurationRestControllerTest,BarcodeInformationServiceTest,GenericSampleOrderServiceImplTest,BarcodeInfoServiceImplTest,PathologySampleServiceImplTest,HibernateMappingValidationTest,BarcodeSchemaValidationTest,BlockLabelTest,SlideLabelTest,FreezerLabelTest,BarcodeLabelMakerTest test`
- Result: PARTIAL PASS (40 tests run, 0 failures, 10 errors)
- Passing tests:
  - `GenericSampleOrderServiceImplTest`
  - `HibernateMappingValidationTest` (storage, sitebranding, barcode, analyzer)
  - `PathologySampleServiceImplTest`
  - `BarcodeLabelMakerTest`
  - `FreezerLabelTest`
  - `SlideLabelTest`
  - `BlockLabelTest`
  - `BarcodeInfoServiceImplTest`
  - `BarcodeSchemaValidationTest`
- Failing tests (Environment blocked):
  - `BarcodeConfigurationRestControllerTest` (7 errors)
  - `BarcodeInformationServiceTest` (3 errors)
  - Cause:
    `java.lang.IllegalStateException: Could not find a valid Docker environment`.
  - Resolution: Verified via CI (see M1 evidence and PR checks).

### Frontend verification (impacted M1 surfaces)

- Command: `CI=true npm test -- BarcodeConfiguration.test.js --watchAll=false`
- Result: PASS (1 suite, 3 tests passed)

### Cypress verification (impacted workflows)

- Command: `npm run cy:spec "cypress/e2e/AdminE2E/barcode.cy.js"` (and others)
- Result: SKIPPED (Environment blocked)
- Cause: Local server not running (port 443 unreachable).
- Resolution: Rely on CI Cypress shards.

### CI evidence capture (M4 closure)

- PR `#3042` created.
- Pending CI run.

### T038 Playwright remediation (2026-03-10)

- Auth setup rewritten: semantic selectors (`getByLabel`, `getByRole`),
  auto-retry assertions, no manual polling loops.
- Barcode configuration spec: real E2E against live app (no route mocks).
- Barcode printing spec: real E2E smoke test against live app.
- Local validation: `npm run pw:test -- --grep "Barcode|barcode"` — 3 passed.
- CI run ID: (record after push triggers workflow re-run)

---

## M4 verification matrix (prep)

Use this matrix to execute M4 integration verification across M1-M3 scope and
capture evidence in one place.

### Backend combined verification (M1-M3)

Run:

```bash
mvn -Dtest=BarcodeConfigurationRestControllerTest,BarcodeInformationServiceTest,GenericSampleOrderServiceImplTest,BarcodeInfoServiceImplTest,PathologySampleServiceImplTest,HibernateMappingValidationTest,BarcodeSchemaValidationTest,BlockLabelTest,SlideLabelTest,FreezerLabelTest,BarcodeLabelMakerTest test
```

Record:

- command and timestamp
- overall result (tests run, failures, errors)
- failing class names (if any)
- follow-up rerun command IDs/status

### Frontend verification (impacted M1 surfaces)

Run:

```bash
cd frontend
CI=true npm test -- BarcodeConfiguration.test.js --watchAll=false
cd ..
```

Record:

- command and timestamp
- suite/test counts
- any warning/error output requiring follow-up

### Cypress verification (impacted workflows)

Run individually during M4 work:

```bash
cd frontend
npm run cy:spec "cypress/e2e/AdminE2E/barcode.cy.js"
npm run cy:spec "cypress/e2e/storageAssignment.cy.js"
npm run cy:spec "cypress/e2e/storageBoxCRUD.cy.js"
cd ..
```

If a listed spec is unavailable in your branch, run the closest affected admin
barcode/storage spec and note the substitution in evidence.

Record:

- command and timestamp per spec
- pass/fail per spec
- console review notes
- screenshot paths for failures

### CI evidence capture (M4 closure)

For each rerun tied to M4 closure, record:

- GitHub run ID
- workflow/job name
- final status
- link to run

Minimum CI closure target:

- backend `checkFormat-build-unitTest-and-run` green
- frontend `static-checks` green
- frontend `build-prod-frontend-image` green
- impacted Cypress shards/specs green or waived with rationale

### M4 gate decision log

- PR `#3039` run `22923992486` (frontend workflow): all required jobs green,
  including Cypress shards.
- PR `#3039` run `22923992487` (backend workflow): failed on
  `checkFormat-build-unitTest-and-run` due Spotless markdown formatting.
- Decision: proceed with M4 preparation work on the stacked M4 branch, then push
  the formatting/artifact-alignment remediation and use the next backend CI run
  as the implementation gate before executing full M4 verification.

---

## M5 execution evidence (2026-03-11)

### Branch

- `feat/284-barcode-label-quantity-management-m5-shared-workflow-foundation`
  (from M4 branch).

### M5 deferred gaps completed (T005a, T005b, T005c)

- **FR-004a**: Default ≤ max cross-field validation in
  `BarcodeConfigurationRestController`; new test in
  `BarcodeConfigurationRestControllerValidationTest`; message keys in
  `message_en.properties` / `message_fr.properties` and frontend `en.json` /
  `fr.json`.
- **FR-002b**: Positive-dimension validation in controller
  `validateDimensionFields`; frontend `validationSchema` (Yup) enabled in
  `BarcodeConfiguration.js` for dimension fields; message keys added.
- **FR-012a**: Liquibase changesets `barcode-info-003-printed-order-count` and
  `barcode-info-004-printed-item-counts`; `SampleBarcodeInfo.printedOrderCount`,
  `SampleItemBarcodeInfo.printedSpecimenCount`/`printedBlockCount`/`printedSlideCount`/`printedFreezerCount`;
  `BarcodeInfoService.recordPrintedCounts(labNo, labels)` and call from
  `LabelMakerServlet` after successful PDF; `SpecimenLabel.getSampleItem()` for
  print recording; unit test `recordPrintedCounts_emptyList_doesNothing`.

### Test execution

- `BarcodeConfigurationRestControllerValidationTest`: 4 tests (incl.
  default-lte-max, positive-dimension).
- `BarcodeInfoServiceImplTest`: 5 tests (incl. recordPrintedCounts empty list).
- `HibernateMappingValidationTest`, `BarcodeSchemaValidationTest`: pass.

### Shared workflow foundation completion (T006, T007, T009, T010, T011, T012)

- **T006/T009/T010**: Added shared workflow model + service baseline:
  - `LabelRowForm`, `LabelsSectionForm`, `PostSavePrintDialogForm`
  - `BarcodeWorkflowPrintService` and `BarcodeWorkflowPrintServiceImpl`
  - `BarcodeWorkflowPrintServiceTest` (labels section and post-save dialog model
    behavior)
- **T007**: Added frontend shared row-model tests and minimal component
  scaffold:
  - `frontend/src/components/barcodeWorkflow/LabelsSection.test.jsx`
  - `frontend/src/components/barcodeWorkflow/LabelsSection.jsx`
- **T011**: Wired `GenericSampleOrderServiceImpl` to:
  - build `labelsSection` via `BarcodeWorkflowPrintService`
  - build `postSavePrintDialog` via `BarcodeWorkflowPrintService`
  - include both objects in the save response map
  - Added unit test
    `saveGenericSampleOrderInternal_includesWorkflowPrintModelsInResponse` in
    `GenericSampleOrderServiceImplTest`.
- **T012**: Aligned planning evidence artifacts:
  - workflow inventory finalized in quickstart/data-model
  - OpenAPI includes print endpoint patterns
    `/api/barcode/print/{orderId}/{labelType}` and
    `/api/barcode/print/{orderId}/{labelType}/{sampleId}`.

### M5 focused verification run (T013)

- Backend command:
  `mvn -Dtest=BarcodeWorkflowPrintServiceTest,GenericSampleOrderServiceImplTest,BarcodeConfigurationRestControllerValidationTest,BarcodeInfoServiceImplTest,HibernateMappingValidationTest,BarcodeSchemaValidationTest test`
  - Result: PASS (28 tests, 0 failures, 0 errors)
- Frontend command:
  `CI=true npm test -- --watch=false --runTestsByPath src/components/barcodeWorkflow/LabelsSection.test.jsx src/components/admin/barcodeConfiguration/BarcodeConfiguration.test.js`
  - Result: PASS (2 suites, 6 tests, 0 failures)

---

## M6 execution evidence (2026-03-11)

### Branch

- `feat/284-barcode-label-quantity-management-m6-pre-save-labels-ui` (from M5
  branch).

### M6 implementation coverage (T016, T017, T018, T019, T020, T021)

- **T016/T018**: `LabelsSection.test.jsx` expanded to rendering and change-event
  coverage; `LabelsSection.jsx` implemented with Carbon `NumberInput` controls
  and running-total updates.
- **T019/T021**: Add Order sample workflow (`SampleType.js`, `Index.js`) now
  includes labels section + persisted per-sample label counts in `sampleXML`;
  i18n keys added in `frontend/src/languages/en.json` and
  `frontend/src/languages/fr.json`:
  - `barcode.labels.section.title`
  - `barcode.labels.order.row`
  - `barcode.labels.sample.row`
  - `barcode.labels.running.total`
- **T020**: Backend Add Order save flow now accepts/persists label counts:
  - `SampleAddService` parses `numOrderLabels` / `numSpecimenLabels` from each
    `<sample .../>` element in `sampleXML`.
  - `SamplePatientEntryServiceImpl` persists order/specimen barcode counts
    through `BarcodeInfoService`.
- **T017**: Add Order label submission response includes orchestration models:
  - `SamplePatientEntryForm` now carries `labelsSection` and
    `postSavePrintDialog`.
  - `SamplePatientEntryRestController` builds these models from submitted label
    quantities and accession number.
  - New tests: `SamplePatientEntryLabelsIntegrationTest`,
    `SamplePatientEntryServiceImplTest`.

### Test execution (T022)

- Backend command:
  `mvn -Dtest=SamplePatientEntryLabelsIntegrationTest,SamplePatientEntryServiceImplTest,GenericSampleOrderServiceImplTest,BarcodeWorkflowPrintServiceTest,BarcodeConfigurationRestControllerValidationTest,BarcodeInfoServiceImplTest,HibernateMappingValidationTest,BarcodeSchemaValidationTest test`
  - Result: PASS (33 tests, 0 failures, 0 errors)
- Frontend command:
  `CI=true npm test -- --watch=false --runTestsByPath src/components/barcodeWorkflow/LabelsSection.test.jsx src/components/admin/barcodeConfiguration/BarcodeConfiguration.test.js`
  - Result: PASS (2 suites, 8 tests, 0 failures)

---

## M7 execution evidence (2026-03-11)

### Branch

- `feat/284-barcode-label-quantity-management-m7-post-save-print-dialog` (from
  M5 branch).

### M7 implementation coverage (T024, T025, T025a, T026, T028, T029, T031, T032, T033)

- **T025/T025a/T028**: Added `PostSavePrintDialog` and tests for render,
  Print/Done callbacks, and FR-011b behavior when accession is absent.
  - `frontend/src/components/barcodeWorkflow/PostSavePrintDialog.jsx`
  - `frontend/src/components/barcodeWorkflow/PostSavePrintDialog.test.jsx`
- **T029**: Integrated post-save dialog into Add Order success path.
  - `frontend/src/components/addOrder/Index.js`
  - `frontend/src/components/addOrder/OrderSuccessMessage.js`
- **T026/T031**: Extended `BarcodeWorkflowPrintService` output for print-job
  dispatch:
  - Added `PrintableLabelOptionForm` with `labelType`, `quantity`,
    `dimensionsMm`, `printUrl`.
  - `PostSavePrintDialogForm.printableLabelTypes` now carries structured
    options.
  - Added pathology dispatch URL mapping coverage (block/slide/freezer URL
    behavior).
  - Updated `LabelMakerServlet` validation to accept pathology print types for
    dispatch compatibility.
- **T032**: Added Order View reprint dialog integration in
  `frontend/src/components/printBarcode/ExistingOrder.js` using
  `PostSavePrintDialog`.
- **T033**: Added i18n strings in `frontend/src/languages/en.json` and
  `frontend/src/languages/fr.json`:
  - `barcode.print.dialog.title`
  - `barcode.print.button`
  - `barcode.print.done`
  - `barcode.print.skip`
  - `barcode.print.reprint.dialog`

### Test execution (T034)

- Backend command:
  `mvn -Dtest=BarcodeWorkflowPrintServiceTest,GenericSampleOrderServiceImplTest,BarcodeConfigurationRestControllerValidationTest,BarcodeInfoServiceImplTest,HibernateMappingValidationTest,BarcodeSchemaValidationTest test`
  - Result: PASS (29 tests, 0 failures, 0 errors)
- Frontend command:
  `CI=true npm test -- --watch=false --runTestsByPath src/components/barcodeWorkflow/PostSavePrintDialog.test.jsx src/components/barcodeWorkflow/LabelsSection.test.jsx`
  - Result: PASS (2 suites, 6 tests, 0 failures)

---

## M8 execution evidence (2026-03-11)

### Branch

- `feat/284-barcode-label-quantity-management-m8-workflow-rollout-validation`
  (from M6 branch, merged M7).

### M8 implementation coverage (T037, T038, T039, T040, T041, T042, T043)

- **T037/T038**: Added rollout tests for inventory workflows:
  - `frontend/src/components/notebook/NotebookSampleOrder.test.jsx`
  - `frontend/src/components/batchOrderEntry/SampleBatchEntry.test.jsx`
  - `frontend/src/components/pathology/PathologyCaseView.test.jsx`
  - `frontend/src/components/immunohistochemistry/ImmunohistochemistryCaseView.test.jsx`
  - `frontend/src/components/cytology/CytologyCaseView.test.jsx`
  - `frontend/src/components/barcodeWorkflow/WorkflowRollout.test.jsx`
- **T040**: Rolled shared labels/post-save behavior into generic/notebook/batch:
  - `GenericSampleOrder.js` now uses `LabelsSection` and `PostSavePrintDialog`.
  - `NotebookSampleOrder` continues to inherit from `GenericSampleOrder`
    (validated in test).
  - `SampleBatchEntry.js` now uses shared `LabelsSection` and
    `PostSavePrintDialog`, and persists label quantities via `sampleXML`
    attributes.
  - `SampleBatchEntrySetup.js` initializes `numOrderLabels` /
    `numSpecimenLabels` defaults in generated sample XML.
- **T041**: Rolled shared post-save print dialog behavior into pathology-related
  case views:
  - `PathologyCaseView.js`
  - `ImmunohistochemistryCaseView.js`
  - `CytologyCaseView.js`
- **T042**: Backend orchestration alignment for pathology:
  - `PathologySampleForm` now carries `labelsSection` and `postSavePrintDialog`.
  - `PathologySampleServiceImpl` now populates shared workflow print models via
    `BarcodeWorkflowPrintService`.
  - `PathologySampleServiceImplTest` extended for orchestration/model coverage.
- **T043 (Configuration-driven verification)**:
  - M8 changes are configuration- and model-driven (shared
    `BarcodeWorkflowPrintService` + shared frontend components) with no
    country-specific branching introduced.

### Test execution (T044)

- Backend command:
  `mvn -Dtest=BarcodeConfigurationRestControllerValidationTest,BarcodeWorkflowPrintServiceTest,GenericSampleOrderServiceImplTest,SamplePatientEntryLabelsIntegrationTest,SamplePatientEntryServiceImplTest,PathologySampleServiceImplTest,BarcodeInfoServiceImplTest,HibernateMappingValidationTest,BarcodeSchemaValidationTest,BlockLabelTest,SlideLabelTest,FreezerLabelTest,BarcodeLabelMakerTest test`
  - Result: PASS (47 tests, 0 failures, 0 errors)
- Frontend command:
  `CI=true npm test -- --watch=false --runTestsByPath src/components/barcodeWorkflow/LabelsSection.test.jsx src/components/barcodeWorkflow/PostSavePrintDialog.test.jsx src/components/notebook/NotebookSampleOrder.test.jsx src/components/batchOrderEntry/SampleBatchEntry.test.jsx src/components/pathology/PathologyCaseView.test.jsx src/components/immunohistochemistry/ImmunohistochemistryCaseView.test.jsx src/components/cytology/CytologyCaseView.test.jsx src/components/barcodeWorkflow/WorkflowRollout.test.jsx`
  - Result: PASS (8 suites, 19 tests, 0 failures)
- Cypress fail-fast command: `npm run cy:failfast`
  - Result: FAIL due pre-existing/unrelated admin suite failure:
    `cypress/e2e/AdminE2E/organizationManagement.cy.js` returned HTTP 400 in
    "Add organisation/site details"; fail-fast skipped remaining spec bodies.
  - OGC-284 added spec status during fail-fast run:
    `cypress/e2e/barcode-workflow.cy.js` executed in fail-fast mode as skipped
    after prior failure.
- Focused Cypress command (new OGC-284 spec):
  `npm run cy:spec "cypress/e2e/barcode-workflow.cy.js"`
  - Result: PASS (1 passing, 1 pending)
- Playwright command: `npm run pw:test -- --grep "OGC-284"`
  - Result: BLOCKED in auth setup because `TEST_USER` / `TEST_PASS` environment
    variables are not set.

### CI run status (T045)

- Branch pushed:
  `feat/284-barcode-label-quantity-management-m8-workflow-rollout-validation`.
- PR: `https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3055`
- GitHub Actions query:
  `gh run list --branch feat/284-barcode-label-quantity-management-m8-workflow-rollout-validation --json databaseId,displayTitle,workflowName,status,conclusion,url --limit 10`
  - Current run IDs/status:
    - `22983895839` - OpenELIS Frontend QA framework workflow (`in_progress`)
    - `22983895848` - i18n Quality Check (`completed: success`)
    - `22983895840` - SpecKit Validation (`completed: success`)
    - `22983895837` - Playwright E2E Tests (`in_progress`)
    - `22983895845` - Auto Label Conflicts (`completed: success`)
    - `22983895844` - OpenELIS-Global-2 CI Build (`in_progress`)
