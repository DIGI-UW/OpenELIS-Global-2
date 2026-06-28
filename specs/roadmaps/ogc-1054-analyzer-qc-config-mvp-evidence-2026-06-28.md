# OGC-1054 Analyzer QC/Config MVP Evidence - 2026-06-28

## Scope

This note captures the code-qa validation pass for
`codex/ogc-1054-analyzer-qc-mvp`. The MVP remains OpenELIS-side analyzer
configuration and QC setup only: shipped profile verification, profile-driven
analyzer creation, mapping review, analyzer QC rule/control lot setup, readiness
gating, and existing bridge registration contracts.

Out of scope for this PR: OpenELIS FILE polling, `QcRun`, a persisted
`AnalyzerProfile` table, bridge runtime changes, and Results/Validation v4
integration.

## Functional Evidence

Generated bundle:

- Folder: `/Users/pmanko/.codex/evidence/ogc-1054-analyzer-qc-config-mvp`
- Zip: `/Users/pmanko/.codex/evidence/ogc-1054-analyzer-qc-config-mvp.zip`
- MP4: `/Users/pmanko/.codex/evidence/ogc-1054-analyzer-qc-config-mvp/videos/ogc-1054-analyzer-qc-config-mvp-user-story.mp4`
- Duration: 40.28 seconds
- Screenshots: 7

The MP4 records a user-facing Playwright demo story:

1. Verify the shipped `astm/genexpert-astm` Analyzer Type/Profile.
2. Launch setup from the profile row.
3. Create an analyzer from the selected profile.
4. Confirm the analyzer initially reports `QC setup required`.
5. Review profile-applied field mappings and result-value mappings.
6. Create an analyzer QC rule through the QC rule UI.
7. Create an active control lot through the control lot UI.
8. Return to the analyzer list and confirm `QC ready`.

The Playwright story is intentionally UI-only. It does not use `page.request`,
direct REST calls, or API setup shortcuts.

## Code-QA Gate Results

| Gate | Result |
| --- | --- |
| `spec-code-alignment` | Roadmap updated to match the implemented MVP boundary and evidence state. No OpenELIS FILE poller, `QcRun`, or bridge-side implementation scope is described as part of the PR. |
| `meaningful-test-coverage` | Backend guards cover manual `ACTIVE` status rejection, component tests cover profile preselection/generic plugin binding and control-lot form behavior, and Playwright covers the assembled user story through the UI. The create/update `ACTIVE` tests were confirmed red before the controller hardening. |
| `simplicity-review` | The branch extends existing analyzer, profile config, QC rule, control lot, and bridge-registration paths. It adds no new table, no new runtime poller, no bridge repo change, and removes the old API-heavy Playwright spec. |
| `cross-repo-companion-pr` | No companion bridge PR is required for the MVP. Bridge work should be opened only from concrete OpenELIS contract-test evidence of missing bridge behavior. |
| `evidence-bundle` | The code-qa evidence bundler produced a folder and zip with one MP4 and seven screenshots. Media artifacts are kept out of git. |
| `commit-pr-hygiene` | Formatter, lint, Playwright guard, and targeted backend/frontend tests were run before staging. |

## Validation Commands

- `mvn "-Dtest=org.openelisglobal.analyzer.**,org.openelisglobal.qc.**" -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: 810 tests passed.
- `mvn spotless:check`
  - Result: passed.
- `cd frontend && npm test -- --run src/components/analyzers/AnalyzerForm/__tests__/AnalyzerForm.defaultConfigs.test.jsx src/components/qc/controlLots/ControlLotSetup.test.jsx`
  - Result: 7 tests passed, 5 skipped.
- `cd frontend && npm run pw:guard && npm run lint -- --quiet && npm run check-format`
  - Result: passed.
- `cd frontend && TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=core-demo playwright/tests/demo/core/analyzer-qc-config-mvp.spec.ts --workers=1`
  - Result: passed.
- `cd frontend && TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:core-demo-video -- playwright/tests/demo/core/analyzer-qc-config-mvp.spec.ts --workers=1`
  - Result: passed and produced MP4 evidence.

## Observed Follow-Ups

- The running app still logs unrelated missing-translation warnings during the
  demo flow.
- The mapping page currently emits an existing
  `GET /rest/analyzer/analyzers/{id}/pending-codes` HTTP 500, but the MVP flow
  renders the mapping review and completes.
- The video run logs a navigation-time `net::ERR_ABORTED` for control-lot save;
  the UI verifies the control lot is present and readiness becomes `QC ready`.
