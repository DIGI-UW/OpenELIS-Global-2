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
- MP4: `/Users/pmanko/.codex/evidence/ogc-1054-analyzer-qc-config-mvp/videos/demo-core-analyzer-qc-conf-38b62-ings-and-completes-QC-setup-core-demo-video.mp4`
- Duration: 41.36 seconds
- Screenshots: 7
- Reference mocks: 4 screenshots captured locally from `DIGI-UW/openelis-work`

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
| `spec-code-alignment` | Roadmap and evidence notes match the implemented MVP boundary. No OpenELIS FILE poller, `QcRun`, persisted `AnalyzerProfile` table, or bridge-side implementation scope is described as part of the PR. |
| `meaningful-test-coverage` | Backend guards cover profile-default persistence, derived result-value mappings, exact-once profile application, pending-code analyzer id binding, manual `ACTIVE` readiness rejection, and QC/bridge behavior. Frontend component tests cover inline setup and mapping review. Playwright covers the assembled user story through the UI. |
| `simplicity-review` | The branch extends existing analyzer, plugin-config JSON, QC rule, control lot, and bridge-registration paths. It adds no new table, no OpenELIS runtime poller, no bridge repo change, and no API-shortcut Playwright story. |
| `cross-repo-companion-pr` | No companion bridge PR is required for the MVP. Bridge work should be opened only from concrete OpenELIS contract-test evidence of missing bridge behavior. |
| `evidence-bundle` | The code-qa evidence bundler produced a folder and zip with one MP4, seven current screenshots, and four `openelis-work` reference mock screenshots. Media artifacts are kept out of git. |
| `commit-pr-hygiene` | Formatter, lint, Playwright guard, and targeted backend/frontend tests were run before staging. |

## OpenELIS-Work Visual Comparison

Reference mocks were captured from the local `openelis-work` checkout into
`/tmp/openelis-work-visual-baseline` and copied into the evidence bundle:

- `openelis-work-analyzer-types.png`: reusable setup/profile table direction.
  The implemented `/analyzers/types` page now shows shipped profile rows with
  readiness, mapping count, QC defaults, result-value counts, and setup actions.
- `openelis-work-inline-setup.png`: setup embedded in the analyzer list. The
  implemented `/analyzers?add=1` flow now keeps the list visible and hides
  standalone developer plugin/status/identifier fields from the lab-facing path.
- `openelis-work-mapping-editor.png`: test, result-value, and QC mapping review
  direction. The implemented mapping page now shows profile-applied test mappings
  and result-value mappings together, with raw plugin JSON hidden by default.
- `openelis-work-manual-qc.png`: QC as analyzer readiness setup. The implemented
  flow records QC-required before rule/control-lot setup and QC-ready afterward.

## Validation Commands

- `mvn "-Dtest=org.openelisglobal.analyzer.**,org.openelisglobal.qc.**" -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: 811 tests passed.
- `mvn spotless:check`
  - Result: passed.
- `mvn -Dtest=org.openelisglobal.analyzer.dao.AnalyzerPendingCodeDAOImplTest test`
  - Result: failed red with numeric binding, then passed after String binding fix.
- `mvn -Dtest=org.openelisglobal.analyzer.service.AnalyzerPluginConfigServiceTest,org.openelisglobal.analyzer.service.AnalyzerServiceProfileQcRulesTest test`
  - Result: 18 tests passed.
- `cd frontend && npm test -- --run src/components/analyzers/AnalyzersList/AnalyzersList.test.jsx src/components/analyzers/AnalyzerForm/__tests__/AnalyzerForm.defaultConfigs.test.jsx src/components/analyzers/AnalyzerForm/__tests__/AnalyzerForm.fileProtocol.test.jsx src/components/analyzers/FieldMapping/FieldMapping.test.jsx src/components/qc/controlLots/ControlLotSetup.test.jsx`
  - Result: 26 tests passed, 5 skipped.
- `cd frontend && npm run pw:guard && npm run lint -- --quiet && npm run check-format`
  - Result: passed.
- `cd frontend && TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=core-demo playwright/tests/demo/core/analyzer-qc-config-mvp.spec.ts --workers=1`
  - Result: passed.
- `cd frontend && TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:core-demo-video -- playwright/tests/demo/core/analyzer-qc-config-mvp.spec.ts --workers=1`
  - Result: passed and produced MP4 evidence.

## Observed Follow-Ups

- The running app still logs unrelated missing-translation warnings during the
  demo flow.
- The video run logs a navigation-time `net::ERR_ABORTED` for control-lot save;
  the UI verifies the control lot is present and readiness becomes `QC ready`.
- The local demo DB emits startup fixture-import duplicate-test warnings and
  profile LOINC warnings for tests absent from this dataset; the user-visible MVP
  flow still verifies profile, mapping, and QC readiness behavior.
