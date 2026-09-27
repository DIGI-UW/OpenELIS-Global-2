# R0 — Restore trustworthy analyzer testing in PR #4332

Decision recorded: 27 September 2026. Owner: [OE2 PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332).
Status: approved scope and implementation plan; the replacement tests and full workflow evidence are not yet complete.

## Outcome and ordering

Make #4332 the initial test-remediation phase for the core OE2 + Bridge roadmap.
It must replace misleading setup and assertions, reconcile the useful existing tests, and provide one reproducible way to prove the actual analyzer workflow.
R0 establishes the tests and evidence surface used by R1–R5 product work and R7 core qualification.
Madagascar remains R8: a later, limited check of distro configuration using already-qualified core workflows.

The standard supported setup must work with the profiles and catalog we supply.
A test fixture must not choose clinical mappings, exclude unsupported rows, or confirm a configuration to manufacture that result.
If shipped defaults cannot produce the intended mapping, the test must expose the product/configuration defect.
Local customization and unresolved-result recovery remain separate supported workflows.

## Decisions that govern implementation

- Use Bridge-shipped portable profiles and the ordinary OE2 clinical catalog/default initialization.
- Bridge owns connections, protocol handling, parsing, durable delivery and FILE watching; OE2 owns local clinical bindings and clinical result processing.
- Keep runtime services profile-agnostic. Named instruments belong in profile data and parameterized test scenarios.
- Prepare synthetic patients, orders and specimens through existing production APIs, with normal validation and service behavior.
- Keep analyzer setup, mapping review/correction, activation and result acceptance visible in the workflow tests that claim to cover them.
- Assert expected clinical associations independently of the mapping selected or the result displayed by the application.
- Use the same scenarios, fixture helpers and assertions for CI and recorded evidence.
- Remove superseded analyzer setup and tests together with their callers and instructions; do not leave two competing acceptance paths.

## Why the current setup needs replacement

The audited harness chooses the first same-LOINC test, excludes unmatched mappings, confirms the configuration and activates connections.
The analyzer SQL fixture independently selects the first same-LOINC test when creating orders.
Those two shortcuts can agree while the default product setup remains broken or the specimen association is wrong.
Some UI tests start after these decisions, and clinical readback derives an expected specimen from the displayed test label.
Prepared-state tests can prove a narrow recovery action; they cannot establish clean installation or independent patient/order/specimen correctness.

These findings come from the September 27 local source audit, not a new live deployment qualification.
The September 26 PR ownership ledger is historical evidence; refresh branches and file comparisons before extracting work.

## The replacement setup

1. Start an isolated core stack using `scripts/dev-stack`, with its normal database initialization, Bridge and analyzer mock.
2. Record the OE2, Bridge and mock source/image identities and the selected immutable profile revisions.
3. Read the production catalog and create a scenario's patient, order and specimen through existing APIs.
4. Resolve fixture references by explicit clinical identity and specimen context; reject missing or ambiguous prerequisites with a clear error.
5. Read back the created order and record its patient, accession, specimen, requested tests and identifiers.
6. In the UI, create the analyzer from the shipped profile and assert the expected defaults are already populated before any mapping edit.
7. Configure genuine site connection settings and perform the ordinary setup/activation actions.
8. Send native mock traffic through the Bridge ASTM/HL7 listener, or write a representative file into the watched directory.
9. Review and accept results through OE2, then verify the independently expected clinical records and values.
10. Preserve the test report, trace, video and result manifest as evidence tied to that exact candidate.

API setup is for prerequisites; it must not reproduce the analyzer resolver or silently repair its output.
Not every analyzer test needs to repeat the order-entry UI. A focused integrated story can cover that boundary; prerequisite API calls retain the production validation path.
Avoid a new fixture framework or test API unless a required operation is demonstrably unavailable through existing endpoints.
If a gap is proven, make the smallest addition that delegates to the existing service behavior and document the gap before implementing it.

Existing routes include patient creation via `/rest/PatientManagement` and order creation via `/rest/SamplePatientEntry`.
Existing test helpers demonstrate access and payload mechanics, but include hardcoded identifiers or arbitrary catalog choices and must not be copied wholesale.
Build a thin shared helper around explicit scenario inputs, returned identifiers and readback; never substitute a database insert when an API rejects the setup.

## C0 — Reconcile the #4332 baseline

- Refresh #4332 against current `develop`; retain its useful real-service coverage and remove stale production overlap.
- Compare #4336 against current `develop` and #4332; preserve unique lifecycle, authorization and query assertions with attribution.
- Keep one per-file disposition ledger: retained, already merged, superseded, or separate justified owner.

Acceptance: each retained analyzer test has a named behavior and an explicit place in the current implementation.
Publish the reconciled test work and the known product failures on #4332 without pretending the overall workflow has passed.

## C1 — Use API prerequisites and remove hidden decisions

- Replace analyzer clinical SQL fixtures with the production API prerequisite helper and explicit scenario data.
- Remove harness mapping selection, automatic exclusion and hidden confirmation/activation from shared setup.
- Keep connection or protocol fixture preparation only where it is explicit and outside the behavior that a test claims to prove.
- Update the relevant CI jobs, local launcher integration, reset tooling, Playwright registration and documentation together.

Acceptance: a clean analyzer test run needs no SQL-authored clinical state or mapping-repair script.
Local, CI and video execution use the same clinical prerequisites and application initialization.
The expected mappings are asserted before traffic; unsupported defaults produce a visible failure.

## C2 — Prove real-service behavior and isolation

- Run integration tests against the real application services and persistence behavior under test.
- Repair shared-state pollution, dependency injection and transaction/readback defects that prevent independent execution.
- Do not mock the importer, mapping resolver or persistence operation when that operation is the assertion's subject.
- Exercise rollback, audit/history and rerun behavior with focused tests where those guarantees belong.

Acceptance: retained service tests run independently and together without sharing hidden state, and assertions fail when the claimed behavior breaks.

## C3 — Cover complete workflows and delete superseded paths

| Scenario                    | Required proof                                                                                                                                                                                                     |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Stock GeneXpert ASTM        | Select the shipped type, verify supplied mappings, connect, send native traffic, review/accept and read back the correct patient/order/specimen/test/value.                                                        |
| Stock FluoroCycler FILE     | Configure the target directory through the ordinary UI; a new file is watched, parsed, archived or retained with an error, and accepted results reach the expected clinical records.                               |
| Stock supported HL7         | Use a real shipped core profile and native HL7 transport; verify default setup, result review and correct clinical readback. Missing core profile support is an R5 defect, not a reason to mount a distro fixture. |
| Local mapping and recovery  | Deliberately introduce an unresolved or invalid observation; unaffected results remain usable, raw source survives, the UI explains the exception, and a local correction recovers it once.                        |
| Durable delivery and replay | Interrupt OE2 delivery after instrument receipt, restart the relevant service, restore delivery and exercise retry; prove retained messages and no duplicate clinical results.                                     |
| Populated upgrade           | Create analyzers and clinical data through a supported previous version's APIs/UI; upgrade the same data, migrate references, and prove results still reach the original analyzer and clinical records.            |

Also cover profile-default preservation, legitimate local overrides, recognition/QC separation, and required authorization in focused service or UI cases.
Do not turn every backend edge case into another full UI recording.
Upgrade evidence must name the previous image/version and created records; inserting legacy rows into the new schema does not prove an upgrade.

Acceptance: expected patient, order, specimen, test, value and result count come from the scenario definition and prerequisite readback.
They must not be inferred from the result row being checked, an arbitrary catalog entry, or a shared first-match mapping helper.
Repeated recovery/replay assertions prove no additional clinical result, not merely an HTTP success or a cleared queue.

Remove obsolete analyzer SQL, setup scripts, tests and compatibility flags with no surviving caller; migrate shared callers before removal.
Update or delete their guides and CI/reset entry points in the same change so the surviving workflow is unambiguous.
Preserve unrelated storage/reporting suites; this work does not authorize a repository-wide fixture rewrite.

## C4 — Run the actual candidate and assign product failures

Run applicable checks and workflow scenarios against the intended OE2/Bridge/mock dependencies, recording exact source, image and profile versions.
Record product failures with reproducible scenarios and an R1–R5 owner; distinguish them from test defects and environment failures.
Do not skip assertions, rewrite expectations to match a failure, inject missing mappings, or exclude raw results to obtain a pass.

Acceptance: #4332 merge readiness requires the applicable checks on its intended dependencies to pass, with no manufactured green result.
A failing-test checkpoint may be published for implementation and review while dependencies are fixed; it is not merge-ready or release evidence.

## C5 — Review and present the evidence

- Run the same registered Playwright scenarios for CI and video; retain ordinary readiness assertions and the existing recording support.
- Review the recordings for readable setup, mappings, receipt, correction where applicable, acceptance and clinical readback.
- Add pacing only if reviewing an actual recording demonstrates a readability problem; screenshot locations are not automatic pause points.
- Publish a simple evidence index with scenario, purpose, exact candidate, profile revisions, run status, video, trace/report and observed outcome.
- Link each passing claim to its run; label historical, partial, failing and pending evidence explicitly.
- Present the recordings directly to the user; producing files without an accessible index and review does not complete evidence delivery.

Acceptance: a reviewer can follow the ordinary analyzer story and see the claimed result in the UI, with machine assertions supporting the same outcome.
Documentation and PR descriptions must distinguish test implementation, product behavior, CI, deployment and release qualification.

## Relationship to product fixes and completion

#4332 owns the trustworthy test setup, retained backend coverage, replacement scenarios, obsolete-path removal and evidence tooling.
R1–R5 retain ownership of recovery, per-result mapping validity, usable setup, durable delivery and shipped profile/default fixes.

C0–C3 work may be published while genuine product failures remain; this avoids waiting for a fixed product before writing the test that exposes it.
#4332 is not fully remediated while fixture shortcuts, weak assertions or competing legacy analyzer paths remain.
Required scenarios keep their failing/pending status until the compatible product candidate passes; R7 cannot claim complete workflow acceptance before that point.
Coordinate merge order or a temporary test-candidate branch with the actual product dependencies rather than disabling required coverage to obtain green CI.

## Source pointers for implementation

- `projects/analyzer-harness/seed-mvp-traffic.sh` — current mapping preparation and native traffic calls.
- `projects/analyzer-harness/seed-analyzers.sh` and `scripts/dev-stack` — setup entry points and shared stack behavior.
- `src/test/resources/fixtures/analyzer-harness-lane-data.sql` and `src/test/resources/load-test-fixtures.sh` — analyzer SQL state and callers to retire/migrate.
- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerMappingDefaults.java` — production default resolution to exercise.
- `src/main/java/org/openelisglobal/sample/controller/rest/SamplePatientEntryRestController.java` — validated order API.
- `frontend/playwright/tests/foundational/core/ogc-1266-order-entry-fix-now.spec.ts` and `frontend/playwright/tests/foundational/core/ogc-557-informed-consent.spec.ts` — existing API mechanics to review.
- `frontend/playwright.config.ts`, analyzer Playwright specs and `.github/workflows/e2e-playwright-reusable.yml` — registration, evidence and CI setup to reconcile.
- Analyzer PR ownership ledger — separate historical artifact containing the September 26 #4332/#4336 file comparison; refresh before applying dispositions.
