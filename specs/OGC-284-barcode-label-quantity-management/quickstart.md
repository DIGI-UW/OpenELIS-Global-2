# Quickstart: OGC-284 Assessment-Driven Remediation

## Goal

Execute the remediation plan for OGC-284 based on current implementation
assessment and open PR feedback.

This quickstart is for developers closing remaining review and merge-readiness
gaps on top of existing OGC-284 work.

---

## 1) Prepare environment

```bash
# Ensure you are on the OGC-284 implementation branch
git checkout feat/ogc-284-expand-barcode

# Sync branch with remote and resolve base conflicts early
git fetch origin develop
git pull origin feat/ogc-284-expand-barcode

# (If needed) rebase/merge develop before final verification
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
