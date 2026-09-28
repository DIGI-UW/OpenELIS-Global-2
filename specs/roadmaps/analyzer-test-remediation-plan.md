# R0 — Repair analyzer defaults and prove the workflows in PR #4332

Decision recorded: 27 September 2026. Owner: [OE2 PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332).
Status: research-informed continuation approved; #4332 owns bounded OE2 default fixes as well as faithful tests. Product implementation and complete workflow/video acceptance remain open.

## Outcome and ordering

Make #4332 the initial test-remediation phase for the core OE2 + Bridge roadmap.
It must replace misleading setup and assertions, reconcile the useful existing tests, and provide one reproducible way to prove the actual analyzer workflow.
R0 establishes the tests and evidence surface used by R1–R5 product work and R7 core qualification.
The September 27 research update also assigns the bounded OE2 catalog/default-resolution fixes to #4332; exposing those known failures is a checkpoint, not completion. Bridge profile/runtime changes retain a linked Bridge owner and must be consumed through the tested dependency.
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

The earlier audited harness chose the first same-LOINC test, excluded unmatched mappings, confirmed the configuration and activated connections.
The removed analyzer SQL fixture independently selected the first same-LOINC test when creating orders.
Those two shortcuts can agree while the default product setup remains broken or the specimen association is wrong.
Some earlier UI tests started after these decisions, and clinical readback derived an expected specimen from the displayed test label.
Prepared-state tests can prove a narrow recovery action; they cannot establish clean installation or independent patient/order/specimen correctness.

These findings come from the September 27 local source audit, not a new live deployment qualification.
The September 26 PR ownership ledger is historical evidence; refresh branches and file comparisons before extracting work.

## Research-driven continuation — approved 27 September 2026

This section supersedes the earlier test-only disposition of known default failures.
The implementation baseline audited was OE2 `0d52ac74e4`, Bridge `b4a9f2cbff`
and mock `6df789111d`. #4332 remains open. No default fix is delivered merely
because it is listed here.

### Scope and ownership

- **OE2 #4332:** retain the test repair and implement the smallest generic
  catalog/default-resolution and setup corrections exposed by it. Production
  initialization must install the same reusable clinical defaults being tested.
- **Linked Bridge work:** portable profile evidence, new immutable revisions,
  any necessary additive profile-contract/parser correction, shared listener
  behavior and profile-owned connection defaults. Do not put Bridge authority
  or an instrument-specific conversion in OE2 to avoid that dependency.
- **Mock #49:** finish review/release of fresh-accession substitution in the XLSX
  fixture; update the consumed pin only after the release exists. This fixes
  repeatability, not result meaning.
- **Existing OE2 #4448/#4449 and delivery PRs:** review their current deltas and
  integrate the supported recovery/isolation behavior. Do not reimplement their
  services inside #4332. Record merge/stack order before consuming a dependency.
- **Madagascar:** later packaging and limited smoke testing, using the qualified
  core profiles and clinical defaults; no distro-only workaround for these gaps.

### Findings to fix, not waive

| Finding and origin                                                                                                                                                  | Bounded correction                                                                                                                                                                                                                                                                                                                                   | Pass condition                                                                                                                                                                                                                                       |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| RIF: profile values `DETECTED` / `NOT DETECTED` differ from OE2 `Resistant` / `Susceptible`; the current resolver matches answer labels.                            | Establish the exact molecular assay meaning, then provide an explicit assay-scoped translation in profile data and generic local resolution. If the local target represents the wrong concept, correct the target rather than invent an equivalence.                                                                                                 | Every supported RIF outcome resolves to the intended clinical meaning from ordinary initialization; raw values survive. No global detected-to-resistant rule.                                                                                        |
| COVID: base `COVIDPCR(Respiratory Swab)` and configuration `COVID-19 PCR` can create duplicate intended targets; answer labels differ too.                          | Fix the ordinary configuration loader's identity/upsert behavior using a stable clinical identity and specimen. Reconcile populated installations without deleting history or overriding intentional local configuration. Align validated answers separately.                                                                                        | A fresh install and repeated configuration load produce one intended target for that specimen. A populated upgrade retains referenced IDs/history. Supported outcomes resolve without choosing an arbitrary same-LOINC record.                       |
| HIV-VL / VIH-1: LOINC `20447-9` matches Serum, Plasma and DBS local tests. Numeric result definitions already exist for Serum and Plasma.                           | Use evidence-backed assay/specimen context through the existing profile and generic resolver path. Reuse the appropriate numeric definition. Keep the order's actual specimen; do not apply a universal Plasma fallback or collapse every specimen-specific test.                                                                                    | Supported assay defaults resolve uniquely before a UI mapping edit. Accepted numeric value, unit, test and actual ordered specimen agree; incompatible/unknown context remains visibly unresolved.                                                   |
| FluoroCycler: adapted XLSX has no test-code column; the single-test profile supplies `VIH-1`. Blank concentration falls back to `Valid`, under that same code.      | Identify the supported export and assay version, including unit, status and control semantics. Reconcile the curated profile and independent raw examples. Keep patient measurement, interpretation/status and control roles distinct using profile-driven behavior.                                                                                 | Watched-directory import saves the expected numeric clinical results. Every additional row has its evidenced role or an explicit retained exception; none is silently dropped, fabricated as a concentration or treated as a control merely to pass. |
| Connection setup: listeners are shared, but current GeneXpert profile requires a saved port and supplies no default.                                                | Put supported defaults in profile data; use the generic resolver for effective values and shared listeners. Explicit overrides win. If a generic fallback is needed, declare it in the single versioned contract. Missing manual input must not impose a new activation gate; an unusable destination must give an actionable connection diagnostic. | Default incoming setup works without a unique port per instrument or a manually entered port when the profile/default contract supplies it. Connection tests explain endpoint/port failures. No duplicated frontend/server constants.                |
| Core HL7: the audited bundled catalog has no active core HL7 profile.                                                                                               | Upstream an evidence-backed reusable HL7 profile into Bridge; retain one contract and consume its release in core tests.                                                                                                                                                                                                                             | Normal core setup and native HL7 produce a correctly accepted clinical result with no Madagascar profile mount.                                                                                                                                      |
| Recovery: Casey's September 15 guide records correction/confirmation leaving already-held rows inert; whole-mapping state can affect otherwise usable observations. | Reuse and review existing recovery/mapping-isolation changes; make the named analyzer's correction-and-retry action operate on retained observations.                                                                                                                                                                                                | Correct in UI, retry the original result, accept once, repeat without duplicates; unrelated valid results remain usable. No resend or direct database edit required.                                                                                 |

### Execution order and checkpoints

1. **D1 — Resume from a known candidate.** Refresh current `develop`, #4332,
   Bridge/mock pins and recovery PR deltas; preserve unrelated work. Reproduce
   the four known default failures on ordinary initialization. Keep the existing
   passing MTB-RIF path as the baseline. Do not repeat the broad audit instead
   of fixing the reproduced failures.
2. **D2 — Correct clinical identity and selection in #4332.** Start with COVID
   duplicate creation, then HIV assay/specimen selection. Make the smallest
   changes to existing loader/resolver services and production configuration.
   Verify fresh load, repeat load and preservation of existing clinical records.
   A harness-only CSV repair does not satisfy this checkpoint.
3. **D3 — Align meanings and profile evidence.** Complete RIF/COVID answer
   translations and FluoroCycler row/unit interpretation using the documents and
   raw examples below. Do not expand this into a whole-catalog redesign. If the
   existing contract cannot express the needed data, define one minimal additive
   change with a Bridge companion and OE2 consumer; use no second contract or
   hard-coded profile/assay branch. Run unabridged GeneXpert ASTM and FluoroCycler
   compatibility checks across OE setup, Bridge and mock before changing pins.
4. **D4 — Complete the core connection/profile dependencies.** Finish
   profile-owned port defaults, core HL7 and mock #49 release integration. Reuse
   shared listeners. Verify directory configuration, watching and archive/error
   behavior in Bridge. Do not introduce an OE2 FILE poller.
5. **D5 — Close the recovery loop using existing work.** Review/integrate
   #4448/#4449 and required delivery changes in a recorded order. Test a known
   and unknown observation together, correct the unknown through the UI,
   recover the original and verify one clinical result. Also exercise permitted
   specimen correction and actual delivery outage/restart. Retain unresolved
   data; do not add blanket mapping/QC activation restrictions.
6. **D6 — Finish C0–C5 and present the candidate.** Run core ASTM, FILE and HL7
   setup-to-acceptance, recovery, outage/replay and populated-upgrade stories on
   the same recorded dependency set. Use previous-version API/UI setup for the
   upgrade; a mocked migration service test is insufficient. Review and present
   videos from those same tests, alongside independent clinical readback. Then
   prepare qualified core releases; only afterward update Madagascar pins and
   run its limited packaging check.

Each checkpoint must name the changed files/PRs, the actual run and outcome,
remaining failures and the next concrete action. A blocked instrument-evidence
question applies to that scenario, not to unrelated COVID/HIV work. A failing
checkpoint may be pushed for review; no skip, waiver, excluded result or
fabricated confirmation converts it into a pass.

### Research and interpretation rules

- [Casey's setup guide](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1648361491)
  (v4, September 22) and [mapping/recovery guide](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1648590851)
  (v4, September 15) are draft observations on OE2 3.2.2.0, not current release
  qualification. Reproduce the documented held-row and count discrepancies;
  update instructions only from passing current workflows. The setup guide did
  not observe ASTM/HL7 connection details.
- [Casey's integration tracker](https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1097531396)
  distinguishes vendor documentation from actual instrument captures. Its
  standardized FluoroCycler template is a different input shape from our
  no-test-code workbook; retain/correct supported formats according to evidence.
- GeneXpert host/result codes are site-configurable. Mock traffic generated from
  the same profile proves consistency but is not independent instrument evidence.
  Keep captured examples distinct from synthetic fixtures and document any
  synthetic accession/header adaptation.
- Manufacturer instructions distinguish numeric values, range qualifiers,
  detection, invalid/error/no-result and control outcomes. Do not conflate them
  because labels happen to match. Confirm the actual FluoroCycler assay version
  before choosing copies/mL versus IU/mL; do not invent a conversion.
- A matching LOINC alone does not choose a local specimen-specific test or an
  answer. Avoid first-match selection and runtime analyzer-specific constants.
- Detailed source links, manufacturer references and remaining evidence limits:
  [published mapping audit](https://madagascar-analyzer-recovery.pmanko.chatgpt.site/analyzer-pr-4332-evidence/).

### Definition of done for the resumed work

A clean core installation uses the supplied profiles and clinical defaults to
complete the supported workflows without rebuilding mappings. The normal
customization path still handles real local differences. Unknown observations
remain stored and recoverable while independent valid observations continue.
Required C0–C5/D1–D6 scenarios pass on the actual candidate; no legacy fixture
shortcut survives; reviewed videos and result assertions are presented.
Product implementation, exact-head CI, deployment, publication and user
acceptance remain separately reported states. Updating this plan completes none
of those implementation checks.

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
Record product failures with reproducible scenarios and an R1–R5 owner; distinguish them from test defects and environment failures. Implement the known bounded OE2 default fixes in #4332 and link the Bridge dependencies; do not park those failures indefinitely as external follow-up.
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

#4332 owns the trustworthy test setup, retained backend coverage, replacement scenarios, obsolete-path removal and evidence tooling, plus the bounded OE2 catalog/default-resolution fixes in D2–D3.
R1–R5 remain the capability workstreams; the known OE2 portion of R5 is implemented in #4332, with linked Bridge/profile dependencies. Existing recovery and delivery PRs retain their services and review ownership. This avoids duplicate implementations while making #4332 accountable for the working candidate.

C0–C3 work may be published while genuine product failures remain; this avoids waiting for a fixed product before writing the test that exposes it.
#4332 is not fully remediated while fixture shortcuts, weak assertions or competing legacy analyzer paths remain.
Required scenarios keep their failing/pending status until the compatible product candidate passes; R7 cannot claim complete workflow acceptance before that point.
Coordinate merge order or a temporary test-candidate branch with the actual product dependencies rather than disabling required coverage to obtain green CI.

## Historical implementation checkpoint — before the research-driven continuation

The #4332 branch was brought forward to `develop` `899f59248e` (merge commit
`e1c52b5640`). Its checked-in Bridge and mock pins are `b4a9f2cbff` and
`6df789111d`; the local development stack reported Bridge 3.2.1. Replacement
Playwright work was pushed as `e12926c0a3`. The following observations use the
isolated `scripts/dev-stack`; they are **not** exact-head CI, deployment or release
qualification.

| Check                           | Observed result                                                                                                                                                                                                                                                                                                                                                                                                      | Next owner                                             |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| Clinical prerequisite           | A patient, Sputum order and Xpert MTB/RIF test created and read back through OE2 APIs; API-order test passed.                                                                                                                                                                                                                                                                                                        | #4332                                                  |
| GeneXpert ASTM                  | Shipped MTB-RIF default, mapping review, UI activation, native mock ASTM, Bridge delivery, UI acceptance and resolved dictionary-value clinical readback passed. The integrated local rerun created the same analyzer through the UI, saved its own listener port, sent traffic to that connection, and displayed the accepted result on OE2's Results page.                                                         | #4332, then exact-head CI/video                        |
| Delivery issue                  | Native ASTM from an unregistered mock source was retained in Bridge's dead-message queue, surfaced in OE2 and dismissed through the UI; test passed. Bridge records source identity before accession parsing.                                                                                                                                                                                                        | #4332                                                  |
| Shared mapping and guided setup | Three shared-mapping tests and the guided UI setup test passed after removing assumptions created by the old seed traffic script.                                                                                                                                                                                                                                                                                    | #4332                                                  |
| Stock default bindings          | MTB-RIF passed. GeneXpert RIF's `DETECTED` result choice, GeneXpert HIV-VL's test and FluoroCycler VIH-1's test were `UNRESOLVED`. For COVID19, OE2 presents two active COVID-19 PCR tests for Respiratory Swab with LOINC `94500-6`; both carry its own `DUPLICATE_LOINC_SAME_SPECIMEN` error, so the independent clinical target is ambiguous. The FluoroCycler FILE story correctly stops at its missing binding. | R5 core profile/catalog defaults; keep #4332 tests red |
| HL7                             | No active core HL7 profile is present in the Bridge's checked-in catalog; a full native HL7 story cannot yet use a shipped core type.                                                                                                                                                                                                                                                                                | R5, then #4332 test                                    |
| Replay and populated upgrade    | Full service-restart replay and supported-version populated upgrade scenarios have not run.                                                                                                                                                                                                                                                                                                                          | #4332 plus R1–R5 dependencies                          |
| CI and video                    | GitHub checks started on `5e2a1f7c52`; shared build, static, and backend test jobs were still pending at this checkpoint. A focused two-story video run on that head passed and was reviewed, including the saved Results page. The newer same-analyzer UI-to-traffic test passed locally and needs its own exact-head recording and CI.                                                                             | #4332 C4–C5                                            |

The same-analyzer video subsequently passed on `e4393ba630` and shows the
clinical Results page. Its setup screen still displays a pre-save “port
required” warning after the port is entered; **Finish and activate** saves that
port and works. Treat the warning as a setup-clarity finding for R3, not as
evidence that an inbound analyzer port must be mandatory in every profile.

The first authoritative E2E executor on `f978a5e4a8` exposed a GeneXpert test
race alongside the expected stock-default failures. The mock reported one
successful send, Bridge registered the new connection and delivered the outbox
entry to OE2, but the intake page had read its worklist about three seconds
before delivery and does not poll for new rows. The test now waits for that
accession in OE2's ordinary worklist API before navigating to the visible
review screen; the focused local rerun passed. This readiness wait does not
create, map, exclude or accept a result. On the `b3e307bc1a` CI executor,
GeneXpert passed and harness shard 2 failed only on the four stock-default
cases plus the FluoroCycler story that depends on its missing default.

The stock-default failures have distinct catalog causes. RIF binds to the
intended clinical test (LOINC `46244-0`), but the profile sends `DETECTED` and
`NOT DETECTED` while that test offers `Resistant` and `Susceptible`. HIV-VL and
VIH-1 both request LOINC `20447-9`; OE2 has active Serum, Plasma and DBS HIV
Viral Load tests, and the generic profile-level resolver cannot pick one from
LOINC alone. The existing CSV loader deliberately updates those legacy
specimen-specific records in place. GeneXpert also declares HIV-VL quantitative. The harness CSV has no numeric
row, but base migrations already provide active numeric definitions for Serum
and Plasma; the CSV omission is not evidence that numeric definitions are
missing. The unresolved selection is the demonstrated blocker. FluoroCycler's
profile hint says `HIV-1 Viral Load`, which does not name those OE2 records.
COVID19 requests LOINC `94500-6`; the base migration supplies a Respiratory Swab
test and the harness catalog creates a second active one with the same LOINC and
specimen. Its existing OE2 answer labels also differ from the profile's
`POSITIVE`/`NEGATIVE` values. Reconcile the clinical test identities and answer
options with the Bridge profile hints/values, preserving existing record IDs
where applicable, then rerun the stock defaults and native traffic tests.

An exploratory browser-network assertion found a separate uncertainty during
result acceptance: Playwright reported `net::ERR_ABORTED` for the POST to
`/rest/AnalyzerResults`, while its trace recorded HTTP 200 response headers and
the accepted result appeared in OE2's clinical API and Results page. In one
rerun the success notification did not appear. No result loss was observed, but
the response/feedback behavior is not explained; investigate it before R7
signoff rather than using the network event alone as a delivery assertion.

Two test gaps remain explicit. `AnalyzerUpgradeIntegrationTest` inserts legacy
analyzer rows directly into the current schema and replaces the Bridge client;
it checks migration-service behavior but cannot serve as the populated-upgrade
proof in C3. Keep that focused service check until a previous-version API/UI
upgrade scenario replaces its broader claim. The FluoroCycler story originally
used sample IDs embedded in a captured XLSX file, limiting it to a fresh stack.
Mock PR #49 accepts freshly generated OE2 accessions and replaces only those
two IDs when it writes the captured workbook into Bridge's watched directory.
Its 1250 and 450 measured values stay unchanged, and the FILE story requires
those exact clinical quantities (allowing trailing decimal zeros). Mock FILE
tests and the Bridge cross-process suite passed against PR #49. The OE2 overlay
still pins the released mock 0.1.1; the new mock must be released before OE2 can
pin it. The OE2 story stops at the unresolved VIH-1 binding, so its repeatability
and clinical assertions are not yet end-to-end validated.

For C2, `AnalyzerUpgradeIntegrationTest` and
`AnalyzerEventPersistenceServiceIntegrationTest` ran in one Maven invocation:
three tests passed with no failures or skips. This checks those two real
persistence/service cases together; it does not establish independence of the
entire analyzer service suite or replace the C3 upgrade and outage scenarios.

The analyzer SQL fixture and its mapping-repair/native-traffic script were
removed. The surviving seed script creates missing profile-pinned Bridge
connections through OE2 APIs for CI/local setup; it does not confirm mappings or
activate analyzers in those paths. Its explicit `--activate` option belongs only
to the separate published-testing deployment smoke path; it checks an already
confirmed mapping and must not be counted as UI setup evidence. API-created
orders use explicit test and specimen identities, and the Playwright story checks
the saved clinical value independently of the displayed intake row.
The UI-only guided setup remains in the `harness-demo` lane. Native ASTM/FILE
and order-API scenarios run in `harness-foundational`, which permits external
instrument input and independent clinical readback. The video project registers
those same scenario files; no demo guard exception or alternate evidence test
was added.

### Historical #4336 ownership reconciliation

Compared `origin/fix/ogc-1220-mapping-lifecycle` at `d4316d67e1` against
`develop` `899f59248e` and #4332. The shared transaction fixture tests are
already on #4332's merged baseline. The following analyzer tests remain
product-coupled on #4336; #4332 must not copy them without their corresponding
mapping, query and security behavior:

| Test file under `src/test/java/org/openelisglobal/`                          | Disposition                                             |
| ---------------------------------------------------------------------------- | ------------------------------------------------------- |
| `analyzer/AnalyzerServiceTest.java`                                          | #4336 product-coupled update                            |
| `analyzer/AnalyzerTestProfileCatalog.java`                                   | #4336 fixture for its mapping behavior                  |
| `analyzer/HibernateMappingValidationTest.java`                               | #4336 entity behavior                                   |
| `analyzer/controller/AnalyzerMappingMutationSecurityIntegrationTest.java`    | Unique authorization assertions retained on #4336       |
| `analyzer/controller/AnalyzerTestCleanup.java`                               | Removal retained on #4336; review with its callers      |
| `analyzer/dao/AnalyzerProfileBindingDAOImplTest.java`                        | #4336 persistence behavior                              |
| `analyzer/integration/QCResultServiceIntegrationTest.java`                   | #4336 compatibility update                              |
| `analyzer/service/AnalyzerInstanceLocalStateServiceTest.java`                | #4336 lifecycle behavior                                |
| `analyzer/service/AnalyzerMappingLifecycleIntegrationTest.java`              | Unique lifecycle and audit assertions retained on #4336 |
| `analyzer/service/AnalyzerSiteBindingConfirmationServiceTest.java`           | #4336 confirmation behavior                             |
| `analyzer/service/AnalyzerSiteBindingPersistenceIntegrationTest.java`        | #4336 persistence behavior                              |
| `analyzer/service/AnalyzerTypeMappingServiceTest.java`                       | #4336 mapping behavior                                  |
| `analyzerimport/action/AnalyzerFhirImportControllerTest.java`                | #4336 import behavior                                   |
| `analyzerimport/service/AnalyzerNormalizedResultImportIntegrationTest.java`  | #4336 import behavior                                   |
| `analyzerimport/service/AnalyzerNormalizedResultImportServiceTest.java`      | #4336 import behavior                                   |
| `analyzerresults/AnalyzerResultsServiceTest.java`                            | #4336 review/query behavior                             |
| `analyzerresults/dao/AnalyzerHeldMappingResultsQueryTest.java`               | Unique held-result query assertions retained on #4336   |
| `analyzerresults/service/AnalyzerResultsAcceptHoldIntegrationTest.java`      | #4336 recovery behavior                                 |
| `analyzerresults/service/AnalyzerResultsAcceptServiceResultMappingTest.java` | #4336 acceptance behavior                               |

This is a source ownership decision, not a claim that #4336 is merge-ready. Its
current code, CI and compatibility with #4332 need a separate exact-head review.

## Source pointers for implementation

- `projects/analyzer-harness/seed-mvp-traffic.sh` — removed mapping-repair and traffic shortcut (historical pointer).
- `projects/analyzer-harness/seed-analyzers.sh` and `scripts/dev-stack` — setup entry points and shared stack behavior.
- `src/test/resources/fixtures/analyzer-harness-lane-data.sql` — removed analyzer SQL state (historical pointer); `src/test/resources/load-test-fixtures.sh` retains unrelated fixtures.
- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerMappingDefaults.java` — production default resolution to exercise.
- `src/main/java/org/openelisglobal/sample/controller/rest/SamplePatientEntryRestController.java` — validated order API.
- `frontend/playwright/tests/foundational/core/ogc-1266-order-entry-fix-now.spec.ts` and `frontend/playwright/tests/foundational/core/ogc-557-informed-consent.spec.ts` — existing API mechanics to review.
- `frontend/playwright.config.ts`, analyzer Playwright specs and `.github/workflows/e2e-playwright-reusable.yml` — registration, evidence and CI setup to reconcile.
- Analyzer PR ownership ledger — separate historical artifact containing the September 26 #4332/#4336 file comparison; refresh before applying dispositions.
