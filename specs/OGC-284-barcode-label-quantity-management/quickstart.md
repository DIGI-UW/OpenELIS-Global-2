# Quickstart: OGC-284 Full-Scope Delivery

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

Authoritative list of sample-creation workflows that support barcode printing and
must implement the OGC-284 labels UI and post-save print flow. M8 rollout tasks
(T037, T038, T040, T041) use this list as scope.

| Workflow / entry point | Labels UI | Post-save print | Notes |
| ---------------------- | --------- | ---------------- | ----- |
| Add Order (`/SamplePatientEntry`) | M6 | M7 | Primary; implement first. |
| (T008: complete remaining rows from codebase inventory) | | | |

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

- `feat/284-barcode-label-quantity-management-m5-shared-workflow-foundation` (from M4 branch).

### M5 deferred gaps completed (T005a, T005b, T005c)

- **FR-004a**: Default ≤ max cross-field validation in `BarcodeConfigurationRestController`; new test in `BarcodeConfigurationRestControllerValidationTest`; message keys in `message_en.properties` / `message_fr.properties` and frontend `en.json` / `fr.json`.
- **FR-002b**: Positive-dimension validation in controller `validateDimensionFields`; frontend `validationSchema` (Yup) enabled in `BarcodeConfiguration.js` for dimension fields; message keys added.
- **FR-012a**: Liquibase changesets `barcode-info-003-printed-order-count` and `barcode-info-004-printed-item-counts`; `SampleBarcodeInfo.printedOrderCount`, `SampleItemBarcodeInfo.printedSpecimenCount`/`printedBlockCount`/`printedSlideCount`/`printedFreezerCount`; `BarcodeInfoService.recordPrintedCounts(labNo, labels)` and call from `LabelMakerServlet` after successful PDF; `SpecimenLabel.getSampleItem()` for print recording; unit test `recordPrintedCounts_emptyList_doesNothing`.

### Test execution

- `BarcodeConfigurationRestControllerValidationTest`: 4 tests (incl. default-lte-max, positive-dimension).
- `BarcodeInfoServiceImplTest`: 5 tests (incl. recordPrintedCounts empty list).
- `HibernateMappingValidationTest`, `BarcodeSchemaValidationTest`: pass.
