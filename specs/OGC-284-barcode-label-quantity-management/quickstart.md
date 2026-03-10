# Quickstart: OGC-284 Assessment-Driven Remediation

## Goal

Execute the remediation plan for OGC-284 based on current implementation
assessment, artifact alignment needs, and open PR feedback.

This quickstart is for developers closing remaining review and merge-readiness
gaps on top of existing OGC-284 work (schema and baseline persistence already
delivered on branch).

---

## 1) Prepare environment

```bash
# Sync develop first
git fetch origin develop

# Create milestone branches per Constitution Principle IX (if not already present)
git checkout -b feat/284-barcode-label-quantity-management-m1-config-i18n-hardening origin/develop
git checkout -b feat/284-barcode-label-quantity-management-m2-persistence-upsert origin/develop
git checkout -b feat/284-barcode-label-quantity-management-m3-label-resilience origin/develop
git checkout -b feat/284-barcode-label-quantity-management-m4-integration-ci-review origin/develop

# Recommended: one worktree per milestone branch (paths are examples)
git worktree add "/workspace-worktrees/ogc-284-m1-config-i18n" "feat/284-barcode-label-quantity-management-m1-config-i18n-hardening"
git worktree add "/workspace-worktrees/ogc-284-m2-persistence-upsert" "feat/284-barcode-label-quantity-management-m2-persistence-upsert"
git worktree add "/workspace-worktrees/ogc-284-m3-label-resilience" "feat/284-barcode-label-quantity-management-m3-label-resilience"
git worktree add "/workspace-worktrees/ogc-284-m4-integration-ci-review" "feat/284-barcode-label-quantity-management-m4-integration-ci-review"
```

Use local worktree paths that match your environment.

---

## 2) Implement remediation in this order

## Step A: BlockLabel data lookup hardening

1. Remove unscoped per-label FHIR search from `BlockLabel`.
2. Build pathology label context in service layer.
3. Pass resolved specimen-type (and related context) into label constructors.
4. Preserve safe fallbacks if enrichment values are unavailable.

## Step B: Configuration/rendering behavior alignment

1. Ensure slide config toggles map to actual rendered fields.
2. Ensure freezer config toggles map to actual rendered fields.
3. If any toggle remains unsupported, remove/hide it consistently in UI and
   backend mapping.

## Step C: Backend i18n key completion

1. Add missing `barcode.label.info.*` keys in:
   - `src/main/resources/languages/message_en.properties`
   - `src/main/resources/languages/message_fr.properties`
2. Verify all keys referenced by label classes exist.

## Step D: Test hardening and regression checks

1. Extend/adjust unit tests for pathology and label-rendering logic.
2. Run existing barcode config/persistence tests.
3. Re-run frontend QA tests impacted by changed label/config behavior.
4. Add ORM mapping validation for barcode entities and schema verification for
   existing OGC-284 Liquibase changesets.

---

## 3) Verification checklist

- [ ] No per-label unscoped FHIR query in label classes
- [ ] Slide/freezer toggles accurately change output behavior
- [ ] Backend message bundles include all new keys
- [ ] Barcode configuration GET/POST behavior remains stable
- [ ] Generic sample order label count persistence remains stable
- [ ] ORM validation test passes for barcode entities
- [ ] Liquibase/schema verification passes for OGC-284 changesets
- [ ] PR review threads can be closed with direct evidence

---

## 4) Recommended test commands

```bash
# Backend targeted tests
mvn test -Dtest="BarcodeInfoServiceImplTest,BarcodeInformationServiceTest,BarcodeConfigurationRestControllerTest"

# Frontend unit tests (targeted)
cd frontend
npm test -- --watch=false
cd ..

# If using Cypress during remediation, run individual file(s) only
cd frontend
npm run cy:run -- --spec "cypress/e2e/<impacted-file>.cy.js"
cd ..
```

> Note: In this cloud environment, local tooling availability may differ. Use CI
> as final source of truth for full job-level verification.

---

## 5) CI and review closure workflow

1. Push remediation commits.
2. Re-run failed PR checks (especially frontend QA workflow).
3. Capture pass/fail evidence per previously open review thread.
4. Resolve GitHub threads explicitly once addressed.
5. Confirm PR no longer shows merge conflicts.

---

## 6) Finalization

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
  - `BarcodeLabelMaker` blocks over-max quantity requests unless
    explicit `override=true` is supplied.

### Test execution evidence

- Command:
  `mvn -Dtest=BlockLabelTest,SlideLabelTest,FreezerLabelTest,BarcodeLabelMakerTest test`
- Result: PASS (7 tests, 0 failures, 0 errors)
- Coverage highlights:
  - Block label specimen/case field behavior without runtime FHIR lookup
  - Slide optional-field on/off rendering behavior
  - Freezer optional-field rendering and toggle compliance
  - Max-limit blocking and explicit override behavior in `BarcodeLabelMaker`
