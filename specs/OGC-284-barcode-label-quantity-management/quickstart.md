# Quickstart: OGC-284 Assessment-Driven Remediation

## Goal

Execute the remediation plan for OGC-284 based on current implementation
assessment and open PR feedback.

This quickstart is for developers closing remaining review and merge-readiness
gaps on top of existing OGC-284 work.

---

## 1) Prepare environment

```bash
# Sync develop first
git fetch origin develop

# Create milestone branches per Constitution Principle IX
git checkout -b feat/284-barcode-label-quantity-management-m1-config-i18n-hardening origin/develop
git checkout -b feat/284-barcode-label-quantity-management-m2-persistence-upsert origin/develop
git checkout -b feat/284-barcode-label-quantity-management-m3-label-resilience origin/develop
git checkout -b feat/284-barcode-label-quantity-management-m4-integration-ci-review origin/develop

# Recommended: one worktree per milestone branch
git worktree add "/workspace-worktrees/ogc-284-m1-config-i18n" "feat/284-barcode-label-quantity-management-m1-config-i18n-hardening"
git worktree add "/workspace-worktrees/ogc-284-m2-persistence-upsert" "feat/284-barcode-label-quantity-management-m2-persistence-upsert"
git worktree add "/workspace-worktrees/ogc-284-m3-label-resilience" "feat/284-barcode-label-quantity-management-m3-label-resilience"
git worktree add "/workspace-worktrees/ogc-284-m4-integration-ci-review" "feat/284-barcode-label-quantity-management-m4-integration-ci-review"
```

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

---

## 3) Verification checklist

- [ ] No per-label unscoped FHIR query in label classes
- [ ] Slide/freezer toggles accurately change output behavior
- [ ] Backend message bundles include all new keys
- [ ] Barcode configuration GET/POST behavior remains stable
- [ ] Generic sample order label count persistence remains stable
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
  - Command:
    `CI=true npm test -- BarcodeConfiguration.test.js --watchAll=false`
  - Result: 1 suite, 3 tests passed.

### Environment blockers

- Backend dependency setup completed:
  - `git submodule update --init --recursive`
  - `cd dataexport && mvn clean install -DskipTests -Dmaven.test.skip=true`
- Backend targeted tests were invoked but blocked by infrastructure:
  - Command:
    `mvn -q test -Dtest=BarcodeConfigurationRestControllerTest,BarcodeInformationServiceTest`
  - Failure cause: Testcontainers cannot find Docker daemon (`/var/run/docker.sock`)
    in this execution environment.

### Manual follow-up commands (backend)

Run these in an environment with Maven + Docker available:

```bash
git submodule update --init --recursive
cd dataexport && mvn clean install -DskipTests -Dmaven.test.skip=true && cd ..
mvn test -Dtest="BarcodeConfigurationRestControllerTest,BarcodeInformationServiceTest"
```
