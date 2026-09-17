# OGC-1054 Analyzer Feature Roadmap

**Updated:** 2026-09-17

**Product and ownership contract:** [feature specification](../OGC-1054-analyzer-qc-config/spec.md)

**Plain-language review:** [feature map](../OGC-1054-analyzer-qc-config/feature-map.md)

This is the only OGC-1054 delivery-state document. It is deliberately short
enough to review before each implementation slice.

## Sources And Boundaries

1. This roadmap and the linked repository specification govern scope,
   architecture, execution, and acceptance.
2. Current OpenELIS, Analyzer Bridge, analyzer-mock, review-tooling code,
   `AGENTS.md`, and accepted versioned contracts determine implementation.
3. [`openelis-work@main`](https://github.com/DIGI-UW/openelis-work/tree/main/designs/analyzer-integration)
   supplies functional and visual intent only. The
   [OGC-1057 QA report](https://github.com/DIGI-UW/openelis-work/blob/qa/ogc-1057-guided-setup-report/designs/analyzer-integration/ogc-1057-qa-report.md)
   supplies review findings only. Neither defines APIs, storage, ownership, or
   tests.
4. Jira is traceability only and cannot override these sources.
5. GitHub records review and merge state. The roadmap does not copy commit
   hashes or maintain a second evidence ledger.

## Fixed Decisions

- A profile has exactly two jobs: define runtime communication for one analyzer
  type and provide defaults for a new Bridge connection.
- Bridge owns immutable profile revisions, durable connection configuration,
  protocols, listeners, parsing, probes, control recognition, FILE runtime,
  and runtime restoration.
- OpenELIS owns the lab-facing UI, local analyzer identity/lab units, local
  catalog bindings, verification/audit, activation intent, held results,
  review, alerts, result release, and operational QC.
- OpenELIS stores a Bridge connection reference, not analyzer-facing values or
  a copied profile. Its backend may transiently mediate Bridge calls.
- Analyzer Type is a composed lab-facing view, not a local plugin/profile
  authority. There is one mapping editor and one pending-result workflow.
- Operational QC never gates analyzer activation or mapping verification.
  `AnalyzerQcRule` and `QcRun` are removed.
- No OE FILE poller, raw analyzer parser, complete desired-state writer, hidden
  classifier fallback, hard-coded profile/model/code behavior, dual writer, or
  compatibility runtime survives G0.
- Existing released OE connection data receives a one-time, quiesced,
  idempotent migration. The migration is not a permanent runtime path.
- MVP publishes only GeneXpert ASTM, FluoroCycler FILE, and QuantStudio FILE.
  Other profiles return one at a time after the same contract, mock, and
  assembled-flow proof.
- The existing `Analyser Import` role authorizes analyzer profile, setup,
  activation, analyzer-result, and linked operational-QC workflows. Global
  Administrators retain their platform override. Other authenticated users
  cannot view or invoke those workflows directly.
- Multi-component mapping and Results/Validation v4 are later milestones.

## CI Transition Decision

Keep the existing build-only workflow and downstream E2E executor, with the
downstream workflow as the only reporter of `03 Checkpoint - E2E`. Remove the
additional E2E execution and checkpoint introduced in the build workflow.

Retain the plugin build files and artifacts required by the active shared CI
contract during this transition. Retained plugins must be inactive in the new
analyzer runtime: they must not register analyzers or handle analyzer traffic.
Delete those files and their CI requirements together in a later cleanup PR.
Do not replace them with placeholder artifacts, skip analyzer stories, or post
manual checkpoint results. A passing rerun alone does not fix an intermittent
test failure.

This is a sequencing exception for inactive files, not permission to restore a
second analyzer workflow. Acceptance requires the normal pipeline to build the
PR's OpenELIS, Bridge, and mock revisions and pass all applicable suites, with
one E2E result for the current PR commit.

## Marker Rule

- `[✓]` merged: every PR required by the checkpoint is merged.
- `[x]` review-ready: implementation and automated checkpoint evidence are
  ready for review. It remains `[x]` through review corrections.
- `[*]` active: the one checkpoint currently being implemented.
- `[ ]` future: not started.

Markers change only when a checkpoint starts, becomes review-ready, or merges.
Exactly one checkpoint is `[*]` while implementation is in progress.
Review-ready work may be stacked while predecessors are reviewed, but merge
order is strict. Scope, architecture, contract, or acceptance changes require an
approved roadmap amendment before production code follows them.

Every review deployment comes from an open checkpoint PR whose applicable
automated gates are green. Branch-only builds are not review targets. Preview
feedback is fixed in the owning checkpoint PR; preview deployment does not
change a roadmap marker or constitute acceptance.

Open a companion repository PR only when a failing versioned contract proves
that repository owns a required change. Do not create empty companion PRs.

Checkpoint names and order package bounded work for review; they do not prove a
code-level dependency. Derive dependencies from current code, versioned
contracts, tests, and history. Do not remove a working behavior merely because
its target replacement is named in a later checkpoint: the assembled slice that
removes it must also contain and test the replacement.

## Current Train

- [✓] **R0 - Canonical roadmap and architecture.** OpenELIS
  [#4049](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4049).
- [✓] **F0 - Acceptance foundation.** OpenELIS
  [#4053](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4053).
- [✓] **E0 - Versioned contracts and migration boundary.** OpenELIS
  [#4055](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4055) and Bridge
  [#45](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/45).
- [✓] **M1 - Bridge profiles and Analyzer Types.** OpenELIS
  [#4056](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4056), Bridge
  [#46](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/46), and mock
  [#40](https://github.com/DIGI-UW/analyzer-mock-server/pull/40).
- [✓] **M2 - Local mapping and control-recognition verification.** OpenELIS
  [#4118](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4118) and Bridge
  [#47](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/47), and mock
  [#43](https://github.com/DIGI-UW/analyzer-mock-server/pull/43).
- [✓] **M3 - Guided setup, durable connection, activation, and QC link.**
  OpenELIS [#4125](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4125) and
  Bridge [#48](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/48).
- [✓] **M4 - Safe result traffic and integrated MVP.** OpenELIS
  [#4138](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4138), Bridge
  [#49](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/49), and mock
  [#42](https://github.com/DIGI-UW/analyzer-mock-server/pull/42).
- [*] **OGC-1220 - Recover held results after mapping adoption.** Implementation
  paused for the [test remediation plan](#analyzer-test-remediation-execution-plan). Continue in OpenELIS
  [#4256](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4256), retaining
  receipt protection from
  [#4241](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4241).
- [ ] **G0 - Exact deployment and named human acceptance.** Review tooling
      [#17](https://github.com/DIGI-UW/openelis-review-tooling/pull/17) is
      merged; its exact release must be deployed before the acceptance build is
      frozen.
- [ ] **R1 - Full feature operations.** Future.
- [ ] **R2 - Site rollout.** Future.

The original R0 through M4 pull requests are merged (verified 2026-09-16). Merge
does not establish deployment or human acceptance. Do not reopen their former
stack for new corrections. G0 remains pending acceptance evidence.

## OGC-1220 held-result remediation

**Agreed 2026-09-16; test remediation precedes further feature work.** After the lab
fixes a mapping and applies it to an analyzer, that analyzer's previously
blocked results become available for ordinary review.

This is the single remediation plan for
[OGC-1220](https://uwdigi.atlassian.net/browse/OGC-1220) and
[#4256](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4256).
[Casey's September 15 decision](https://uwdigi.atlassian.net/browse/OGC-1220?focusedCommentId=37454)
calls for automatic reevaluation followed by ordinary result review. The
September 16 timing decision requires adoption by each analyzer. This
supersedes both the earlier next-message-only rule and the draft's required
manual reprocess action. The separate proposal is removed; Git retains its
history.

### Agreed behavior

- Bridge already sends normalized FHIR with raw test identity and source
  context, including unknown test codes. Bridge continues to own parsing and
  transport. OpenELIS owns local catalog mapping and held-result recovery; no
  Bridge change is currently indicated. Existing evidence:
  [`FhirBundleBuilderLoincTest`](../../tools/openelis-analyzer-bridge/src/test/java/org/itech/ahb/fhir/FhirBundleBuilderLoincTest.java),
  including `unknownCodeStillCarriesItsRawIdentityWithoutInventingLoinc`.
- The existing Analyzer Types mapping editor must include received unknown tests
  and values even when they are absent from the Bridge profile. These are local
  mapping decisions, not edits to or copies of the Bridge profile. Test targets
  must be active local catalog entries; value targets must be active Result
  Options of the selected Test.
- Incoming results and held-result reevaluation must use the exact local mapping
  revision that the analyzer has adopted and a human has confirmed. Saving or
  confirming a shared edit alone must not change an analyzer still using an
  older revision.
- Reevaluate eligible held rows automatically once both confirmation and
  adoption are satisfied, in either order. Cover rows already held when this fix
  is installed and safe repeated processing of the same revision. No analyzer
  resend or per-result reprocess button is required.
- Resolved patient rows enter ordinary Save / Retest / Ignore review; they are
  not automatically accepted. Still-unresolved rows remain held with accurate
  reasons and counts. Preserve source evidence and audit, including the actor
  and applied mapping revision. Unrelated holds and reviewed results are not
  candidates for this operation.
- Preserve unsaved worklist edits. Report control processing as successful only
  when its result was actually created. Apply existing exclusion and audit rules
  without adding a separate discard action; the draft's deletion of excluded
  rows is not an approved retention decision.

### Implementation order

Current progress: the roadmap is committed and both existing branches are
refreshed against `develop`. Received-code mapping, confirmation with unresolved
rows, and incoming use of the confirmed selected revision have uncommitted
implementation changes. Feature work is paused for the test-setup audit and
cleanup catalogued below. Automatic held-row recovery, worklist integration,
and assembled acceptance remain open.

Reuse #4256's stored Observation, ownership/profile checks, locking, and row
update logic. Replace its manual trigger and latest-saved-mapping lookup. Keep
#4241's duplicate-delivery receipt protection as a separate dependency:
transport retries still return the original acceptance summary and must not
become a second recovery path. Refresh the two branches against current
`develop` before production edits while preserving that dependency.

| Order | Bounded change                                                                                                                                     | Existing starting points                                                                                                           |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| 1     | Make incoming processing and held recovery use the analyzer's exact confirmed, adopted mapping revision.                                           | `AnalyzerInstanceLocalStateServiceImpl`, `AnalyzerSiteBindingConfirmationServiceImpl`, `AnalyzerNormalizedResultImportServiceImpl` |
| 2     | Compose the mapping editor from declared concepts plus observed unknown tests/values; enable the existing resolution link for both hold reasons.   | `AnalyzerTypeMappingServiceImpl`, held-result queries, `buildHeldResultResolutionUrl`                                              |
| 3     | Invoke the shared recovery operation when confirmation and adoption become eligible; cover either event order, existing backlog, and safe repeats. | `confirmMapping`, `selectSiteBindingRevision`, `reprocessHeldResult`                                                               |
| 4     | Refresh affected rows and counts without losing unsaved edits; remove the manual action and obsolete instructions.                                 | Analyzer results components, result state, `en.json`                                                                               |
| 5     | Replace next-message-only and manual-action tests, then prove the complete mapping-to-review flow.                                                 | Focused service/security/router tests; `ogc-1054-analyzer-mvp.spec.ts`; the acceptance checks below                                |

Start with failing tests for the exact revision rule and observed unknown test
mapping. Check how confirmation handles unresolved rows: adding observed rows
must not prevent unrelated mapped traffic or recovery of the resolved subset.
Use the existing services and held rows; add background processing only if a
measured workload or transaction constraint requires it.

### Analyzer test catalogue and cleanup audit — 2026-09-16

**Refreshed 2026-09-17. This is the current catalogue, not a passing-test claim.**
The four levels are **unit, component, integration, end-to-end**. Classify by
what executes and what the assertions prove, not by filename or test runner.
Permissions, history, contracts, migrations and concurrency are subjects/tags,
not additional levels. Human acceptance remains separate.

**Source boundary:** published foundation `7ca2a637cb` (#4332), the preserved
uncommitted mapping work on `fix/ogc-1220-mapping-lifecycle`, and recovery branch
`021fbcf7ea` (#4256). The mapping checkout has **61 core backend files: 60 concrete
classes, one abstract helper, 329 annotated methods**, including three new local
classes. The recovery branch also has a manual-reprocess controller test absent
from that checkout. There are **17 core frontend files**, plus the neighbors and
browser/harness checks listed below. These are source counts, not passing checks.
Entries marked UNPUBLISHED describe local changes, not code already in a PR.
The full implementation snapshot is preserved in local Git stash
`477f0a4091f11e213f2e36a019fcb7249215bc8b`; this documentation commit does not
publish or certify that implementation.

The analyzer catalogue includes the three backend packages (`analyzer`,
`analyzerimport`, `analyzerresults`), directly connected request/catalog/security
checks, frontend tests, browser stories, and shared-fixture dependencies exposed
by this work. Broader OE2 exposure is identified, not represented as a completed
semantic audit of every module. Bridge and simulator suites remain separately
owned; only their relevant boundaries are inventoried here.

[Draft OE2 test-remediation specification](../017-test-remediation/spec.md)
captures reusable principles and future expansion. This roadmap remains the
sole owner of analyzer findings, execution status and product acceptance.

#### Four levels and evidence boundaries

| Level       | Intended proof                                                                            | Real and substituted dependencies                                                   | Data ownership / acceptance                                                                                                                                                                  |
| ----------- | ----------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Unit        | One decision, validation rule or transformation                                           | Real logic; collaborators may be substituted                                        | No shared application/database state. A wrong implementation must fail, not merely return a mock's answer.                                                                                   |
| Component   | One bounded component through its public interface: UI, HTTP controller, parser or client | Real component/framework; boundary services or network responses may be substituted | Fresh component state; exact inputs, outputs, errors and interactions. Does not establish saved backend effects.                                                                             |
| Integration | Real services, queries, framework mappings or network boundaries work together            | The chain under test is real; unrelated external systems may be controlled          | Persisted effects, reload/transaction boundaries, constraints/history as applicable. ORM model-building is framework integration without a database; it retains the existing fast ORM check. |
| End-to-end  | A user workflow succeeds in the assembled application                                     | Real browser, OE, Bridge and database; instrument simulator is an external boundary | Owned scenario prerequisites; actual visible recovery without resend/manual reprocessing, plus inspected stored outcomes and browser evidence.                                               |

**Integration is not one cleanup strategy.** Ordinary service/query tests use
owned records with rollback and flush/reload where needed. Tests of real commits,
concurrency, after-commit work or transaction-close behavior keep actual
transactions and explicitly clean up their own records. Destructive migration
checks own an isolated schema/database. Shared migrated seed data is a
prerequisite to preserve, not something individual tests silently repair.

#### Current ownership audit and required corrections

| Surface                                                          | Intended role / current evidence                                                                                                                                                                             | Underlying problem                                                                                                                                                                                                                     | Required correction and proof                                                                                                                                                                                                                                                               |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `BaseTestConfig`, `BaseWebContextSensitiveTest`, `AppTestConfig` | Shared infrastructure, not a test level. Source scan finds 437 direct subclasses, 317 fixture-loader callers and 33 cleanup-helper callers across OE2.                                                       | One application/database is shared across tests with incompatible ownership. The fixture loader now joins a test transaction, but `cleanRowsInCurrentConnection` still opens another connection for cascading truncation.              | Define one explicit transaction/connection contract for all fixture helpers and audit affected callers before altering semantics. Preserve ordinary rollback, real-commit and isolated-schema modes; prove each mode and mixed helper use. No blanket annotation or new seed-repair helper. |
| Remaining committed fixture writers                              | Four `AccessionValidation*` classes, `QCDashboardServiceTest`, `QCChartDataServiceIntegrationTest`, `AnalyzerResultsServiceTest`, `AnalyzerResultsControllerTest`, analyzer `QCResultServiceIntegrationTest` | Fixtures replace shared catalog/users/results; six of these also replace localization rows. Cascading truncation reaches beyond fixture tables.                                                                                        | Classify the behavior first; migrate ordinary service/component-integration checks to owned prerequisites and appropriate rollback, or isolate a genuinely destructive fixture. Verify saved results and unchanged unrelated rows.                                                          |
| `StatusServiceTest`                                              | Status/history integration; two-class reproduction followed by mapping persistence failed 6/15 checks. Local rollback correction passes 15/15.                                                               | Committed localization replacement prevented another application context from starting.                                                                                                                                                | Treat the annotation change as provisional evidence for this class, not a completed shared-foundation design. Include cache restoration and neighboring order tests.                                                                                                                        |
| `AnalyzerResultsAcceptHoldIntegrationTest`                       | Actual acceptance/specimen behavior                                                                                                                                                                          | Per-class status/sequence/history-type/unknown-patient repairs hide fixture damage; fixed IDs and cleanup have wider ownership than the test scenario.                                                                                 | Retain real acceptance assertions, replace global repairs with owned prerequisites and audited cleanup, and prove preservation of unrelated data.                                                                                                                                           |
| `TestServiceImpl`, fixture caches and static `SpringContext`     | Production catalog services reached by integration tests                                                                                                                                                     | The DAO was corrected to instance injection, but static service references/catalog maps remain. The base resets the global context before each test. Status/history caches refresh; that is not proof every reached cache is isolated. | Trace actual reached static dependencies; remove retained cross-context dependencies and centralize explicit cache lifecycle where necessary. Prove two application contexts read/write through their own dependencies. Do not reset arbitrary globals merely to get green.                 |
| `QuestionnaireResponseServiceTest` and cleanup-helper callers    | Wider regression surface caused by shared helper changes                                                                                                                                                     | Latest foundation CI last printed this class at 07:40:36 UTC, then was cancelled at 13:19:31. This transactional class loads a fixture and invokes the separate-connection cleanup helper.                                             | Reproduce with a bounded timeout and inspect database locks/thread state. Mixed-connection blocking is a supported hypothesis, not yet a proven cause. Retain regression coverage for the helper's 33 callers; do not dismiss cancellation as infrastructure noise.                         |
| Test selection                                                   | Previous 680-check run selected only 52 of the 60 concrete core classes                                                                                                                                      | `Analyzer*Test` omits eight core classes, and the catalog-controller neighbor was also omitted. A green selection cannot establish the entire analyzer surface.                                                                        | Derive selection from the inventory/packages, enumerate actual discovered/executed classes, and check omissions explicitly. Run affected neighbors and full backend CI for shared changes.                                                                                                  |
| Testing guidance                                                 | Existing `.specify/guides/testing-roadmap.md` is authoritative                                                                                                                                               | It mandates the broad base for ordinary controller tests and recommends destructive DBUnit setup; its claim that only named tables are affected conflicts with `TRUNCATE ... CASCADE`.                                                 | Make a focused, reviewed guide/template correction with the owning foundation change. Preserve existing framework/ORM requirements. The draft specification records the desired direction; it does not silently amend governance.                                                           |

The last focused reverse-order run on the local mapping candidate executed
**680 checks with 11 context-start errors**. The foundation-only 661-check passes
were narrower evidence. T1 is reopened; neither those counts nor the 15-check
correction establish a sound complete foundation.

#### Original findings and coverage limits

The following eleven findings retain the original audit baseline; the findings
register and current ownership audit give their present status.

“Confirmed” below means the original source demonstrates the behavior. It does not mean
we reproduced a production failure. “Coverage limit” means a useful test has
been given more evidentiary weight than it deserves, not that it must be deleted.

| Finding / priority                                                                                                       | Level and exact surface                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Evidence and consequence                                                                                                                                                                                                                                                                                                                                                                       | Cleanup and proof required                                                                                                                                                                                                                                                                                                  |
| ------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1. Internal behavior disabled in shared setup — fix first**                                                            | Database and HTTP tests inheriting [AppTestConfig](../../src/test/java/org/openelisglobal/AppTestConfig.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                | `auditTrailService()` returns a recorder that saves nothing; `fhirContext()` returns a parser factory with no real parsing behavior unless a test supplies it. The same setup substitutes other internal services. Confirmation/import needs actual history and parsing.                                                                                                                       | Make the internal behavior needed by integration tests real in one shared setup. Keep external calls controlled. Remove local workarounds and prove persisted history plus message parsing through injected services. Do not switch every shared mock blindly.                                                              |
| **2. Shared application objects modified by tests — fix first**                                                          | Shared history tests: [SystemAuditTrailIntegrationTest](../../src/test/java/org/openelisglobal/audittrail/SystemAuditTrailIntegrationTest.java), [PatientAuditTrailIntegrationTest](../../src/test/java/org/openelisglobal/audittrail/PatientAuditTrailIntegrationTest.java), [PatientMultiFieldAuditTrailIntegrationTest](../../src/test/java/org/openelisglobal/audittrail/PatientMultiFieldAuditTrailIntegrationTest.java), [P0AuditEmitSmokeTest](../../src/test/java/org/openelisglobal/audittrail/P0AuditEmitSmokeTest.java)                           | At HEAD, all four build a real recorder manually and use reflection to replace dependencies on shared application services, without restoring them. This creates an execution-order risk; an actual order-dependent failure has not been reproduced in this audit.                                                                                                                             | Use one correctly configured integration context. The uncommitted cleanup has removed these four overrides, but the shared setup is not yet fixed or validated. Run the affected groups together and in reversed order after consolidation.                                                                                 |
| **3. Mapping transitions bypass the application — fix first**                                                            | [AnalyzerNormalizedResultImportIntegrationTest](../../src/test/java/org/openelisglobal/analyzerimport/service/AnalyzerNormalizedResultImportIntegrationTest.java): `reprocessingPersistsTheNextUnresolvedMappingState`, `heldControlRecoversItsLotFromStoredEvidenceInALaterTransaction`, `retryCannotAdvanceAHeldResultButExplicitReprocessingCan`, `bindTest`, `confirmFixtureMapping`                                                                                                                                                                     | Mapping rows are inserted/deleted under the same saved revision. The new confirmation helper deletes the prior confirmation and reconfirms that altered revision. These tests exercise importer behavior with supplied state, but bypass immutable revisions and actual analyzer adoption. The confirmation workaround was added during this remediation and must not become the new standard. | Establish initial data once; perform the mapping change through real save, confirmation and adoption services. Verify old revision/history remains intact and only the adopting analyzer changes. Preserve independent duplicate-delivery assertions.                                                                       |
| **4. “Persistence integration” also constructs substitute domain services — consolidate**                                | [AnalyzerSiteBindingPersistenceIntegrationTest](../../src/test/java/org/openelisglobal/analyzer/service/AnalyzerSiteBindingPersistenceIntegrationTest.java), especially `savedCatalogBindingsAndConfirmationReloadFromPostgres`, `sharedMappingCanReturnToTheContentOfAnEarlierRevision`, `activationAndDeactivationPersistExactBridgeAcknowledgementsWithoutChangingTheLoadedVersion`                                                                                                                                                                       | Real database is combined with manually constructed services and substituted catalog, user, mapping or confirmation services; real audit recorder is assembled three times. Persistence checks remain useful, but cannot establish ordinary application wiring.                                                                                                                                | Inject actual local services. Supply real local catalog/user records. A substituted Bridge response remains appropriate for a focused OE persistence test; full transport belongs to the harness. Keep explicit transaction-close/reload checks.                                                                            |
| **5. Small tests carry an entire database context — consolidate**                                                        | [AnalyzerFhirImportControllerTest](../../src/test/java/org/openelisglobal/analyzerimport/action/AnalyzerFhirImportControllerTest.java), [AnalyzerResultsAcceptServiceResultMappingTest](../../src/test/java/org/openelisglobal/analyzerresults/service/AnalyzerResultsAcceptServiceResultMappingTest.java)                                                                                                                                                                                                                                                   | The first starts the broad database context, then replaces import and parser dependencies; the second substitutes the result catalog to test stable-ID selection. Both restore replacements, unlike finding 2. These are narrow controller/logic tests with unnecessarily broad setup.                                                                                                         | Move narrow tests into isolated setup with explicit dependencies. Retain separate real integration coverage. The controller's `any(Bundle.class)` verification also needs the parsed source identity checked, not merely “some Bundle was passed.”                                                                          |
| **6. Query tests do not execute their queries — coverage limit**                                                         | [AnalyzerProfileBindingDAOImplTest](../../src/test/java/org/openelisglobal/analyzer/dao/AnalyzerProfileBindingDAOImplTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                              | All three tests substitute Hibernate and its query results; they verify query text/parameters and return values. They cannot catch database query validity or incorrect filtering against real rows.                                                                                                                                                                                           | Keep useful argument/normalization checks; establish database cases with different profiles/revisions and analyzer references. Do not count the three mocked tests as database-query proof.                                                                                                                                 |
| **7. Shared database cleanup and cached state require repairs — isolation risk**                                         | [BaseTestConfig](../../src/test/java/org/openelisglobal/BaseTestConfig.java), [BaseWebContextSensitiveTest](../../src/test/java/org/openelisglobal/BaseWebContextSensitiveTest.java), [AnalyzerResultsAcceptHoldIntegrationTest](../../src/test/java/org/openelisglobal/analyzerresults/service/AnalyzerResultsAcceptHoldIntegrationTest.java)                                                                                                                                                                                                               | A static PostgreSQL container is shared; the base disables automatic per-test transactions and fixture cleanup uses `TRUNCATE ... CASCADE`. Protected seeds exist, while result-acceptance tests additionally restore statuses, sequences and singleton records damaged by other fixtures. These are concrete isolation workarounds, not proof every current order fails.                      | Consolidate fixture ownership/cleanup and cache refresh. Use rollback for tests that do not need committed multi-transaction behavior; use explicit owned-row cleanup for concurrency/commit tests. Verify order independence instead of adding further per-test database repairs.                                          |
| **8. Permissions and ordinary HTTP tests use different setups — coverage boundary**                                      | [BaseWebContextSensitiveTest](../../src/test/java/org/openelisglobal/BaseWebContextSensitiveTest.java), [SecuritySliceMockMvcTest](../../src/test/java/org/openelisglobal/security/SecuritySliceMockMvcTest.java), analyzer security classes                                                                                                                                                                                                                                                                                                                 | The broad base builds MockMvc without applying Spring Security filters. Security classes explicitly apply them, but use reduced test configurations; for example the type-security setup disables CSRF. Ordinary controller success does not prove permissions, and role tests alone do not prove deployed request security.                                                                   | Keep labels explicit. Cover mapping save/confirmation/adoption authorization and rejected-request non-mutation in the appropriate security/integration level. Validate real login/session/request protection through the deployed flow. No missing production protection is established by this observation alone.          |
| **9. Tests still enforce superseded behavior — replace with the behavior change**                                        | [AnalyserResults.test.jsx](../../frontend/src/components/analyserResults/AnalyserResults.test.jsx), [AnalyzerHeldResultRestControllerTest](../../src/test/java/org/openelisglobal/analyzer/controller/AnalyzerHeldResultRestControllerTest.java), manual-reprocess case in [AnalyzerWorkflowAuthorizationSecurityTest](../../src/test/java/org/openelisglobal/analyzer/controller/AnalyzerWorkflowAuthorizationSecurityTest.java), import tests above, [assembled browser story](../../frontend/playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts) | Component/controller/security tests cover the manual reprocess action. The browser story explicitly expects zero mapping links for an unknown test and does not adopt the newly confirmed mapping and prove the original held row recovered. It was testing the earlier workflow.                                                                                                              | Remove obsolete manual-action cases when their replacement exists. Change the browser story to map an observed unknown code/value, confirm, adopt, and review the original recovered row without resend. Do not weaken assertions merely to keep the old story green.                                                       |
| **10. Harness confirmation preparation assumes every non-bound row is excluded — fix before partial-mapping acceptance** | [seed-mvp-traffic.sh](../../projects/analyzer-harness/seed-mvp-traffic.sh), `prepare_profile_mapping` confirmation payload                                                                                                                                                                                                                                                                                                                                                                                                                                   | The script puts all rows whose state is not `BOUND` into `excludedRows`, including `UNRESOLVED` if present. The new editor test explicitly distinguishes unresolved from excluded; the harness preparation still uses the old assumption.                                                                                                                                                      | Build confirmation payloads only from actual BOUND/EXCLUDED decisions; leave unresolved rows unresolved. Exercise preparation with an unresolved observed code so the assembled test does not hide this case. Runtime failure on that case has not yet been reproduced.                                                     |
| **11. Automatic recovery acceptance remains unproved — implementation/coverage gap**                                     | Adoption/import/confirmation unit tests, database tests, worklist refresh tests, and browser stories                                                                                                                                                                                                                                                                                                                                                                                                                                                         | The two-analyzer selected-revision unit test supplies analyzer objects; it does not perform two real adoptions. Existing refresh tests cover accepting a result, not concurrent recovery preserving unsaved edits. No reviewed test in this scope demonstrates the complete new recovery workflow.                                                                                             | Use the acceptance checks below: two real analyzers, both event orders, partial recovery, backlog, repeated/concurrent processing, history, permissions, unchanged source evidence, truthful control outcome, and unsaved edits. Add coverage at its owning level rather than duplicating every combination in the browser. |

#### Shared substitutes: the complete configured surface

[AppTestConfig](../../src/test/java/org/openelisglobal/AppTestConfig.java)
now contains **27 Mockito-created bean declarations** (two fewer than the
original audit), grouped below. The audit recorder and FHIR parser are now real;
the remaining internal substitutes must be assessed against the specific path
being tested. A declaration
is not a finding of bad testing: some correctly isolate external systems. The
two methods annotated `@Profile("Test")` use a different case from the active
`test` profile and are not normally active in this base context.

| Group                                    | Declared substitutes                                                                                                                                                                                                        | Audit disposition                                                                                                                                                     |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Internal behavior directly relevant here | `FhirUtil`, `FhirConfig`, `AccessionNumberValidatorFactory`, `DisplayListService`, `SampleOrderService`, `RequesterService`                                                                                                 | Recorder and parser substitutions were removed in T1. Review these remaining substitutes as the actual acceptance path reaches them; no blanket replacement.          |
| Other internal application behavior      | `WHONetReportService`, `TestNotificationConfigService`, `TestNotificationService`, `AnalysisNotificationConfigService`, `NotificationDAO`, `Versioning`, `TestProductMapping`, `DataExportTaskService`, `DataExportTaskDAO` | Shared exposure identified; full notification/report/export correctness is outside this analyzer audit. Record dependencies if the chosen real workflow reaches them. |
| External/infrastructure boundaries       | `CloseableHttpClient`, `JavaMailSender`, `OzekiMessageOutService`, `OdooClient`, `OdooConnection`, `DataExportService`, `TruststoreService`                                                                                 | Controlled substitutes can be appropriate. These tests cannot establish actual network delivery, certificates, mail or external systems.                              |
| Test conveniences                        | `TextEncryptor`, `UnsatisfiedDependencyException`                                                                                                                                                                           | Encryption is stubbed to return its input; this setup is not encryption evidence. The exception bean has no established analyzer need and is a cleanup candidate.     |
| Differently cased profile                | `RequesterTypeService`, `OrganizationTypeService`                                                                                                                                                                           | Configuration inconsistency to consolidate deliberately, not two additional active analyzer mocks.                                                                    |

The shared setup also supplies a fixed Bridge profile catalog via
[AnalyzerTestProfileCatalog](../../src/test/java/org/openelisglobal/analyzer/AnalyzerTestProfileCatalog.java).
That is a deliberate external fixture, not a live Bridge. Its nested test-config
exclusion plus several named exclusions also make the broad component scan
harder to reason about; no current configuration collision was reproduced here.

#### Backend file inventory by level

Paths below are relative to `src/test/java/org/openelisglobal/`. Every core file
in the audited mapping checkout appears once. "Retain" describes the intended
role, not certification that every assertion has passed a semantic review.
Changes in UNPUBLISHED entries must be reconciled when T2 is committed.

##### Unit

| File                                                                         | Intended proof                                                               | Dependencies, ownership and disposition                                                                        |
| ---------------------------------------------------------------------------- | ---------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `analyzer/form/AnalyzerInstanceRequestTest.java`                             | Deserialize analyzer reference and generic connection fields                 | No database or application context required.                                                                   |
| `analyzer/service/AnalyzerActivationRecordServiceTest.java`                  | Validate and retain exact Bridge acknowledgement fields                      | DAO/history substituted; persisted acknowledgement covered separately.                                         |
| `analyzer/service/AnalyzerActivationServiceTest.java`                        | Evaluate activation readiness and command acknowledgement                    | Internal collaborators substituted; exact Bridge/DB lifecycle belongs in integration.                          |
| `analyzer/service/AnalyzerConnectionProbeServiceTest.java`                   | Validate connection ownership and exact probe revision                       | Connection/profile collaborators substituted; transport not exercised.                                         |
| `analyzer/service/AnalyzerEventPersistenceServiceTest.java`                  | Handle duplicate registration and retry decisions                            | Persistence and transaction collaborators substituted; real race covered separately.                           |
| `analyzer/service/AnalyzerInstanceLocalStateServiceTest.java`                | Validate local identity, lab units and mapping adoption                      | Mocked collaborators; local copy-before-update assertions do not alone prove recorded history.                 |
| `analyzer/service/AnalyzerInstanceServiceTest.java`                          | Compose and coordinate OE identity with Bridge connection                    | Substituted boundaries; no durable cross-system state claimed.                                                 |
| `analyzer/service/AnalyzerMappingCatalogServiceTest.java`                    | Filter active tests and valid result options                                 | Catalog collaborators substituted; integration must use actual catalog records.                                |
| `analyzer/service/AnalyzerProfileBindingServiceTest.java`                    | Resolve immutable profile references and reject fingerprint drift            | DAO and Bridge fixture substituted; exact database discrimination covered separately.                          |
| `analyzer/service/AnalyzerSiteBindingConfirmationServiceTest.java`           | Validate confirmation evidence, unresolved decisions and history eligibility | History/catalog collaborators substituted; real history required in integration.                               |
| `analyzer/service/AnalyzerSiteBindingFingerprintTest.java`                   | Canonicalize mapping decisions and detect meaningful changes                 | Pure transformation; keep fast and independent.                                                                |
| `analyzer/service/AnalyzerSiteBindingServiceTest.java`                       | Create immutable mapping revisions and validate catalog choices              | Internal collaborators substituted; no actual revision persistence claimed.                                    |
| `analyzer/service/AnalyzerTypeCatalogServiceTest.java`                       | Compose type usage and truthful completeness                                 | Substituted catalog/mapping state; integration must supply real unresolved rows.                               |
| `analyzer/service/AnalyzerTypeMappingServiceTest.java`                       | Compose observed rows, save decisions and prepare confirmation               | Substituted services; local changes require real lifecycle/query companions.                                   |
| `analyzer/service/BridgeAnalyzerProfileTest.java`                            | Preserve profile fields and independent row identities                       | Supplied profile document; omitted from previous selection.                                                    |
| `analyzer/service/QCResultProcessingServiceImplTest.java`                    | Resolve control lots and route control processing                            | QC service substituted; cannot prove a result was saved. T3 must make success truthful and verify persistence. |
| `analyzer/util/NetworkValidationUtilTest.java`                               | Validate network-address input rules                                         | No shared database; omitted from previous selection.                                                           |
| `analyzer/valueholder/AnalyzerProfileBindingTest.java`                       | Retain one shared profile reference without copied payload                   | Value-object behavior only.                                                                                    |
| `analyzer/valueholder/AnalyzerSiteBindingModelTest.java`                     | Preserve independent source rows and exact analyzer selection                | Value-object behavior only; no saved adoption.                                                                 |
| `analyzerimport/service/AnalyzerNormalizedResultImportServiceTest.java`      | Apply selected confirmed mappings and determine hold/QC/replay outcomes      | Internal collaborators substituted; eligibility rules only, not persistence or rollback.                       |
| `analyzerresults/service/AnalyzerResultsAcceptServiceResultMappingTest.java` | Resolve result options by stable identity across loads                       | Isolated selector with mocked catalog; no application/database startup needed.                                 |
| `analyzerresults/service/AnalyzerResultsServiceImplTest.java`                | Deduplicate/update staged results and preserve source identity               | DAO substituted; actual concurrent/receipt behavior belongs in integration.                                    |

##### Component

| File                                                                       | Intended proof                                                         | Dependencies, ownership and disposition                                                                                      |
| -------------------------------------------------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `analyzer/contract/AnalyzerBridgeContractConsumerTest.java`                | Interpret pinned Bridge profile/contract fixtures                      | Real consumer with supplied documents; no running Bridge.                                                                    |
| `analyzer/controller/AnalyzerActivationRestControllerTest.java`            | Map activation requests, actor and blockers to responses               | Domain service substituted; retain as controller-boundary proof.                                                             |
| `analyzer/controller/AnalyzerConnectionProbeRestControllerTest.java`       | Map probe evidence and errors to responses                             | Probe service substituted; no transport or persistence claim.                                                                |
| `analyzer/controller/AnalyzerInstanceRestControllerTest.java`              | Map analyzer create/update/read requests                               | Instance service substituted; actual saved state belongs in integration.                                                     |
| `analyzer/controller/AnalyzerTypeRestControllerSecurityTest.java`          | Enforce type-route roles in a security slice                           | Real filters with substituted services; reduced configuration disables CSRF. Do not claim deployed security.                 |
| `analyzer/controller/AnalyzerTypeRestControllerTest.java`                  | Map type/catalog/mapping/profile-management requests and errors        | Domain services substituted; validate request contents, not merely invocation.                                               |
| `analyzer/controller/AnalyzerWorkflowAuthorizationSecurityTest.java`       | Enforce setup/probe/activation permissions                             | Reduced security chain and substituted domain services. Recovery branch also carries obsolete manual-reprocess expectations. |
| `analyzer/controller/ImportIssuesRestControllerSecurityTest.java`          | Enforce import-issue route permissions                                 | Reduced security slice; omitted by Analyzer\* selection. No database effects proved.                                         |
| `analyzer/migration/AnalyzerActivationRecordLiquibaseTest.java`            | Inspect activation migration declarations                              | XML/document contract only; does not execute an upgrade.                                                                     |
| `analyzer/migration/AnalyzerCutoverChecksumTest.java`                      | Check the published migration checksum contract                        | Artifact compatibility only; no database lifecycle.                                                                          |
| `analyzer/migration/AnalyzerSiteBindingConfirmationLiquibaseTest.java`     | Inspect confirmation/history migration declarations                    | XML structure only; persisted confirmation/history belongs in integration.                                                   |
| `analyzer/migration/AnalyzerSiteBindingLiquibaseTest.java`                 | Inspect immutable-binding and superseded-column migration declarations | XML structure only; not an executed migration test.                                                                          |
| `analyzer/service/BridgeAnalyzerConnectionClientTest.java`                 | Build and validate Bridge connection/probe HTTP contracts              | HTTP transport substituted; omitted from previous focused command. Keep beside live harness proof.                           |
| `analyzer/service/BridgeProfileCatalogServiceTest.java`                    | Read/validate exact Bridge profile and publication responses           | HTTP substituted; omitted from previous selection.                                                                           |
| `analyzer/service/BridgeProfileManagementServiceTest.java`                 | Forward profile lifecycle actions and validate returned identity       | HTTP substituted; omitted from previous selection. Does not prove Bridge durability.                                         |
| `analyzerimport/action/AnalyzerFhirImportControllerTest.java`              | Parse actual FHIR and map ingress errors/actor/source identity         | Isolated controller plus real parser, import service substituted; retain separate persisted import proof.                    |
| `analyzerimport/service/AnalyzerNormalizedResultContractTest.java`         | Parse normalized patient/control/source contract fixtures              | Actual parser, supplied messages; no live Bridge delivery.                                                                   |
| `analyzerresults/action/AnalyzerResultsPagingTest.java`                    | Keep analyzer worklist session caches separate                         | HTTP/session boundary in isolation; no actual database paging.                                                               |
| `analyzerresults/action/beanitems/AnalyzerResultItemJsonContractTest.java` | Exclude display-only options from submitted result JSON                | Serialization boundary only.                                                                                                 |

##### Integration

| File                                                                               | Intended proof                                                                                  | Dependencies, ownership and disposition                                                                                                                                                                  |
| ---------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `analyzer/AnalyzerEventLiquibaseRollbackTest.java`                                 | Execute event migration, rollback and reapply                                                   | Own PostgreSQL container; retain independent lifecycle proof.                                                                                                                                            |
| `analyzer/AnalyzerServiceTest.java`                                                | Persist and query analyzer identity                                                             | Owned service-created rows with rollback; local foundation implementation.                                                                                                                               |
| `analyzer/AnalyzerSiteBindingOrmValidationTest.java`                               | Build real Hibernate mapping model without a database                                           | Framework integration, not persistence; retain its specific relationship assertions.                                                                                                                     |
| `analyzer/HibernateMappingValidationTest.java`                                     | Build analyzer/receipt mappings and detect getter/configuration conflicts                       | No database; missing from previous focused selection. Review overlap with the narrower mapping-model test before consolidating.                                                                          |
| `analyzer/controller/AnalyzerMappingMutationSecurityIntegrationTest.java`          | Reject unauthorized save/confirm/adopt without writes; persist allowed changes/history          | UNPUBLISHED: real services/database, rollback; explicit test security chain, not production login/filter coverage.                                                                                       |
| `analyzer/dao/AnalyzerProfileBindingDAOImplTest.java`                              | Query exact profiles/revisions and analyzer counts/order                                        | UNPUBLISHED rewrite: real queries over owned rows with rollback; old published version mocks query results.                                                                                              |
| `analyzer/integration/QCResultServiceIntegrationTest.java`                         | Persist control results and calculate scores; reject invalid lots/values                        | Committed whole-table qc-result.xml fixture; replace with owned records and verify saved output after flush/reload. Does not need a shared-table reset.                                                  |
| `analyzer/migration/AnalyzerConnectionRuntimeRemovalLiquibaseIntegrationTest.java` | Execute runtime-column removal, preservation guard and rollback                                 | Own schema dropped in finally; real Liquibase. Retain isolation; broad application startup is unnecessary to this role.                                                                                  |
| `analyzer/migration/AnalyzerQcRuleRemovalLiquibaseIntegrationTest.java`            | Execute obsolete-QC-schema removal and rollback                                                 | Own schema and restored system property; retain schema ownership, not business-table truncation.                                                                                                         |
| `analyzer/migration/AnalyzerSupersededRuntimeRemovalLiquibaseIntegrationTest.java` | Execute removal of superseded runtime schema                                                    | Own schema, finally cleanup; keep separate from shared business fixtures.                                                                                                                                |
| `analyzer/migration/AnalyzerSupersededSchemaRemovalLiquibaseIntegrationTest.java`  | Execute cutover preservation and refusal cases                                                  | Own schema, real migration, finally cleanup; no substitute for business workflow acceptance.                                                                                                             |
| `analyzer/service/AnalyzerEventPersistenceServiceIntegrationTest.java`             | Give concurrent deliveries one persisted processing owner                                       | Real worker transactions; unique event and owned cleanup. KEEP committed execution; blanket test rollback would invalidate this proof.                                                                   |
| `analyzer/service/AnalyzerMappingLifecycleIntegrationTest.java`                    | Save/reopen/confirm observed codes and values; independently adopt exact revisions with history | UNPUBLISHED: owned service-created records and rollback. Incoming eligibility is covered; automatic held-row recovery remains T3.                                                                        |
| `analyzer/service/AnalyzerSiteBindingPersistenceIntegrationTest.java`              | Reload saved bindings/history; verify activation and probe after transaction closes             | UNPUBLISHED rewrite injects internal services, substitutes Bridge HTTP, and owns committed rows/cleanup. KEEP explicit closed-transaction checks.                                                        |
| `analyzer/service/BridgeHttpClientAuthenticationTest.java`                         | Send actual HTTP authentication headers and reject missing credentials                          | Local HTTP server boundary; no live Bridge/database. Omitted from previous selection.                                                                                                                    |
| `analyzerimport/service/AnalyzerNormalizedResultImportIntegrationTest.java`        | Commit normalized results/receipts; prove replay, concurrent delivery and QC rollback           | UNPUBLISHED rewrite uses real internal lifecycle/history with owned committed records and cleanup. KEEP independent transactions; T3 recovery absent.                                                    |
| `analyzerresults/AnalyzerResultsServiceTest.java`                                  | Persist/query/delete staged results and associated history                                      | Committed whole-table analyzer-results.xml reset remains. Several paging assertions accept empty output; replace with owned fixtures and exact membership/order checks.                                  |
| `analyzerresults/dao/AnalyzerHeldMappingResultsQueryTest.java`                     | Select exact-profile/revision mapping holds and exclude other rows                              | UNPUBLISHED: owned entity fixtures and rollback; direct persistence is appropriate for query setup, not for mapping-lifecycle proof.                                                                     |
| `analyzerresults/service/AnalyzerResultsAcceptHoldIntegrationTest.java`            | Hold ambiguous specimen results and persist reviewer choice                                     | Real service plus fixed SQL fixtures; repairs global statuses, sequences, history types and unknown-patient records. Replace repairs with owned prerequisites; verify unrelated state and saved outcome. |

##### Helper

| File                                                           | Intended proof                                       | Dependencies, ownership and disposition                                                                              |
| -------------------------------------------------------------- | ---------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `analyzer/controller/AuthenticatedAnalyzerControllerTest.java` | Provide authenticated broad-context controller setup | Abstract; zero executable tests and no current subclasses found. Remove if still unused when implementation resumes. |

**Recovery-branch addition:** `analyzer/controller/AnalyzerHeldResultRestControllerTest.java`
is a **component** test of the manual reprocess endpoint. It is outside the
mapping-checkout count and must be removed/replaced with its superseded endpoint
in T3, not carried forward as automatic-recovery evidence.

##### Direct and shared neighbors

| Files / group                                                                                                                                                                                                   | Level and intended proof                                                     | Ownership, issue and disposition                                                                                                                                                                                                             |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `security/AnalyzerIngressSecurityTest.java`                                                                                                                                                                     | Component: ingress authentication/filter policy                              | Test handler/security configuration; retain policy checks, prove real mutation and deployed login separately.                                                                                                                                |
| `result/controller/AnalyzerResultsControllerTest.java`                                                                                                                                                          | Integration: actual result-list response and role checks                     | Real parent database context plus a child security context; committed analyzer-results.xml fixture. Include in fixture-ownership correction; does not prove production login.                                                                |
| `testcatalog/controller/TestCatalogEditorAnalyzersIntegrationTest.java`                                                                                                                                         | Integration: catalog-to-analyzer reference lookup                            | Fixed SQL setup/owned cleanup; valid as a query/read boundary, not mapping adoption proof. Add to regression selection and review ID/cleanup ownership.                                                                                      |
| `audittrail/{SystemAuditTrailIntegrationTest,PatientAuditTrailIntegrationTest,PatientMultiFieldAuditTrailIntegrationTest,P0AuditEmitSmokeTest}.java`                                                            | Integration: saved audit content and actor                                   | T1 uses injected real history with owned rollback fixtures. Verify seed/row preservation in mixed orders; do not restore former dependency swaps.                                                                                            |
| `history/HistoryServiceTest.java`, `audittrail/migration/HistoryReferenceIdentifierMigrationTest.java`                                                                                                          | Integration: numeric/UUID history queries and actual schema upgrade/rollback | Keep actual recorder/query proof and isolated migration schema. Migration evidence does not prove every audit caller.                                                                                                                        |
| `resultvalidation/{AccessionValidationReviewPanelTest,AccessionValidationReviewActionsTest,AccessionValidationQcAckTest,AccessionValidationBulkReleaseTest}.java`                                               | Integration: review/release effects; some HTTP entry points                  | Committed broad fixtures, shared display-list stubbing and bulk-release numeric-type repair/reinitialization. Replace fixture damage, establish per-test dependency ownership, and retain real persisted release/history assertions.         |
| `qc/service/{QCDashboardServiceTest,QCChartDataServiceIntegrationTest}.java`                                                                                                                                    | Integration: actual dashboard/chart queries                                  | Both replace shared `qc-dashboard.xml`; dashboard shifts dates and refreshes names; chart test repairs reference rows, including obsolete analyzer type. Use owned query fixtures; remove dependency repairs and verify exact query results. |
| Other `QC*Test` / `AccessionValidation*Test` classes                                                                                                                                                            | Mixed unit, component and integration, selected as affected regression       | The name is not the level. Mocked controller/calculation classes remain narrow; actual DAO/service/alert classes retain database proof. Inclusion in a run does not certify all assertions.                                                  |
| `common/services/StatusServiceTest.java`, `test/TestServiceTest.java`, `localization/LocalizationServiceTest.java`, `typeofsample/TypeOfSampleTestServiceTest.java`, `qaevent/NcEventServiceTest.java`          | Integration: shared catalog/status services reached by analyzer fixtures     | Rollback changes exist (status only local); independently verify service writes, caches, sequences and surviving seeds. Do not regard annotations alone as acceptance.                                                                       |
| `patientidentity/PatientIdentityServiceTest.java`, `pathology/PathologySampleServiceTest.java`, `dataexchange/fhir/TaskInterpreterImplNullPatientTest.java`, `test/SpecimenAwareResolutionIntegrationTest.java` | Integration: audit/parser/catalog consumers affected by shared setup         | Include real actors, saved outcomes and applicable fixture/transaction behavior; no further domain feature expansion.                                                                                                                        |
| `FixtureTransactionIntegrationTest.java`                                                                                                                                                                        | Integration: fixture replacement and dependent rows roll back                | Owned sentinel catches false success after earlier seed loss. Extend proof to mixed helpers, committed ownership and cache/context boundaries.                                                                                               |
| 33 `cleanRowsInCurrentConnection` callers, including both questionnaire service tests                                                                                                                           | Shared-helper impact, not a new analyzer feature                             | Enumerate before helper edits; transactional callers are especially relevant. Run affected callers after a focused contract reproduction. No claim that every caller is defective.                                                           |

<details>
<summary>Exact affected regression inventory: 47 neighboring classes</summary>

This is the complete non-core portion of the last 99-class selection. The
ownership decisions above apply to the named groups. Inventory inclusion does
not imply a complete semantic audit of unrelated domains. Keep unit/component
neighbors narrow; extend integration validation where shared changes reach them.
Paths are relative to `src/test/java/org/openelisglobal/`.

| File                                                                      | Level       | Intended proof / ownership boundary                                                                                                                                        |
| ------------------------------------------------------------------------- | ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `typeofsample/TypeOfSampleTestServiceTest.java`                           | Integration | Type Of Sample Test Service through actual service/query and database. Rollback declared; still check flush/reload and cache ownership.                                    |
| `test/TestServiceTest.java`                                               | Integration | Test Service through actual service/query and database. Rollback declared; still check flush/reload and cache ownership.                                                   |
| `test/SpecimenAwareResolutionIntegrationTest.java`                        | Integration | Specimen Aware Resolution through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                   |
| `security/AnalyzerIngressSecurityTest.java`                               | Component   | Ingress filter policy with test handler; no persisted import.                                                                                                              |
| `resultvalidation/AccessionValidationReviewPanelTest.java`                | Integration | Accession Validation Review Panel through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.           |
| `resultvalidation/AccessionValidationReviewActionsTest.java`              | Integration | Accession Validation Review Actions through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.         |
| `resultvalidation/AccessionValidationQcAckTest.java`                      | Integration | Accession Validation Qc Ack through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                 |
| `resultvalidation/AccessionValidationBulkReleaseTest.java`                | Integration | Accession Validation Bulk Release through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.           |
| `result/controller/AnalyzerResultsControllerTest.java`                    | Integration | Read actual staged results under role checks; committed fixture and child security context need explicit ownership.                                                        |
| `referral/ReferralStatusHistoryDAOTest.java`                              | Integration | Referral Status HistoryDAO through actual service/query and database. Rollback declared; still check flush/reload and cache ownership.                                     |
| `qc/service/QCStatisticsServiceTest.java`                                 | Unit        | Isolated QC Statistics Service logic; no actual database/history/transaction claim.                                                                                        |
| `qc/service/QCRuleViolationServiceTest.java`                              | Unit        | Isolated QC Rule Violation Service logic; no actual database/history/transaction claim.                                                                                    |
| `qc/service/QCResultServiceTest.java`                                     | Integration | QC Result Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                           |
| `qc/service/QCDashboardServiceTest.java`                                  | Integration | QC Dashboard Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                        |
| `qc/service/QCControlLotServiceTest.java`                                 | Unit        | Isolated QC Control Lot Service logic; no actual database/history/transaction claim.                                                                                       |
| `qc/service/QCControlLotServiceManufacturerFixedTest.java`                | Integration | QC Control Lot Service Manufacturer Fixed through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.   |
| `qc/service/QCChartDataServiceIntegrationTest.java`                       | Integration | QC Chart Data Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                       |
| `qc/service/QCAlertServiceTest.java`                                      | Unit        | Isolated QC Alert Service logic; no actual database/history/transaction claim.                                                                                             |
| `qc/service/QCAlertServiceIntegrationTest.java`                           | Integration | QC Alert Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                            |
| `qc/event/QCResultCreatedEventListenerTest.java`                          | Unit        | Isolated QC Result Created Event Listener logic; no actual database/history/transaction claim.                                                                             |
| `qc/dao/QCControlLotDAOIntegrationTest.java`                              | Integration | QC Control LotDAO through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                           |
| `qc/controller/QCViolationRestControllerTest.java`                        | Component   | HTTP request/response handling with substituted domain service; no saved-effect claim.                                                                                     |
| `qc/controller/QCRestControllerTest.java`                                 | Component   | HTTP request/response handling with substituted domain service; no saved-effect claim.                                                                                     |
| `qc/controller/QCChartDataRestControllerTest.java`                        | Component   | HTTP request/response handling with substituted domain service; no saved-effect claim.                                                                                     |
| `qc/QCHibernateMappingValidationTest.java`                                | Integration | Build real QC framework mappings; no database.                                                                                                                             |
| `qaevent/NceHistoryServiceTest.java`                                      | Integration | Nce History Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                         |
| `qaevent/NcEventServiceTest.java`                                         | Integration | Nc Event Service through actual service/query and database. Rollback declared; still check flush/reload and cache ownership.                                               |
| `patientidentity/PatientIdentityServiceTest.java`                         | Integration | Patient Identity Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                    |
| `pathology/PathologySampleServiceTest.java`                               | Integration | Pathology Sample Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                    |
| `observationhistorytype/ObservationHistoryTypeServiceTest.java`           | Integration | Observation History Type Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.            |
| `observationhistory/ObservationHistoryServiceTest.java`                   | Integration | Observation History Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                 |
| `microbiology/service/MicroIdentificationHistoryServiceTest.java`         | Unit        | Isolated Micro Identification History Service logic; no actual database/history/transaction claim.                                                                         |
| `microbiology/MicrobiologyR1AstOverrideHistoryLiquibaseRollbackTest.java` | Integration | Execute actual history migration/rollback in an owned database.                                                                                                            |
| `localization/LocalizationServiceTest.java`                               | Integration | Localization Service through actual service/query and database. Rollback declared; still check flush/reload and cache ownership.                                           |
| `history/HistoryServiceTest.java`                                         | Integration | History Service through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.                             |
| `dataexchange/fhir/TaskInterpreterImplNullPatientTest.java`               | Integration | Task Interpreter Impl Null Patient through actual service/query and database. Committed fixture/cleanup contract must be checked before changing the shared base.          |
| `common/services/historyservices/PatientHistoryServiceTest.java`          | Unit        | Extract audit changes from XML. Uses Unsafe allocation/private-method reflection to bypass construction; record as a follow-up design issue, not real history integration. |
| `common/services/StatusServiceTest.java`                                  | Integration | Status Service through actual service/query and database. Rollback declared; still check flush/reload and cache ownership.                                                 |
| `coldstorage/controller/rest/FreezerAuditTrailControllerTest.java`        | Unit        | Sort/render audit events using pure helpers; no HTTP/database proof despite its name.                                                                                      |
| `audittrail/migration/HistoryReferenceIdentifierMigrationTest.java`       | Integration | Execute numeric/UUID migration on owned schemas, including 100,000 records and rollback refusal.                                                                           |
| `audittrail/SystemAuditTrailIntegrationTest.java`                         | Integration | Persist/reload system audit and actor; inherited owned rollback fixtures.                                                                                                  |
| `audittrail/PatientMultiFieldAuditTrailIntegrationTest.java`              | Integration | Persist multiple changed patient fields in actual history; inherited rollback fixtures.                                                                                    |
| `audittrail/PatientAuditTrailIntegrationTest.java`                        | Integration | Persist patient change history through real services; inherited rollback fixtures.                                                                                         |
| `audittrail/P0AuditEmitSmokeTest.java`                                    | Integration | Persist system/nonconformance audit emissions; inherited rollback fixtures.                                                                                                |
| `audittrail/AuditTrailServiceSensitiveFieldSkipTest.java`                 | Unit        | Isolated Audit Trail Service Sensitive Field Skip logic; no actual database/history/transaction claim.                                                                     |
| `audittrail/AuditTrailServiceInsertTest.java`                             | Unit        | Isolated Audit Trail Service Insert logic; no actual database/history/transaction claim.                                                                                   |
| `FixtureTransactionIntegrationTest.java`                                  | Integration | Assert fixture rollback and unrelated sentinel/reference preservation.                                                                                                     |

</details>

Reproduce the wider helper impact list with:

```sh
rg -l 'cleanRowsInCurrentConnection\(' src/test/java
rg -l 'executeDataSetWithStateManagement\(' src/test/java
```

The previous selection omitted `HibernateMappingValidationTest`,
`ImportIssuesRestControllerSecurityTest`, `BridgeAnalyzerConnectionClientTest`,
`BridgeAnalyzerProfileTest`, `BridgeHttpClientAuthenticationTest`,
`BridgeProfileCatalogServiceTest`, `BridgeProfileManagementServiceTest`, and
`NetworkValidationUtilTest`. Include these plus the direct catalog neighbor in
the next complete analyzer run. Full CI is additionally required for common-base
changes; this is not a proposal to rewrite all 437 inheriting classes at once.

#### Frontend and browser inventory

**Unit:** `comboBoxSearch.test.js`, `types.test.tsx`,
`AnalyserResults.test.js`, and `Index.test.js` cover small helpers/type contracts.
**Component:** the remaining 13 core files exercise React/router behavior or
client request boundaries with substituted server responses. The catalog UI and
harness policy/configuration neighbors are component checks as well.
**End-to-end:** the listed browser projects require the actual assembled runtime;
manual hardware and named human acceptance are reported separately.

| Surface                             | Files / project                                                                                                                                                                                                                                                                                                                                                                                                                                                     | Scope and limitation                                                                                                                                                                                                                                    |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Analyzer components, 9 files        | `frontend/src/components/analyzers/`: `types.test.tsx`, `comboBoxSearch.test.js`, `AnalyzersList/AnalyzersList.test.jsx`, `AnalyzersList/AnalyzersList.profilePresentation.test.jsx`, `AnalyzerSetup/AnalyzerSetup.test.jsx`, `AnalyzerSetup/AnalyzerConnectionSetup.test.jsx`, `AnalyzerTypeMapping/AnalyzerTypeMappingEditor.test.jsx`, `AnalyzerTypeManagement/AnalyzerTypeManagement.test.jsx`, `AnalyzerTypeManagement/ControlRecognitionDraftEditor.test.jsx` | Component/helper behavior; server substitutes. Mapping editor uses real router and checks saved/confirmed payloads.                                                                                                                                     |
| Result components, 6 files          | `frontend/src/components/analyserResults/`: `AnalyserResults.test.jsx`, `AnalyserResults.test.js`, `Index.test.jsx`, `Index.test.js`, `ImportIssuesPanel.test.jsx`, `analyserResultsRefresh.test.jsx`                                                                                                                                                                                                                                                               | Mix of small helper tests, parent/child component tests and refresh behavior. The similarly named `.js`/`.jsx` files have different content; do not delete merely as apparent duplicates. Manual reprocessing cases are obsolete under the agreed plan. |
| Client request helpers, 2 files     | `frontend/src/services/analyzerService.activation.test.ts`, `analyzerService.siteBinding.test.ts`                                                                                                                                                                                                                                                                                                                                                                   | Request construction and client error handling, not backend execution.                                                                                                                                                                                  |
| Neighbor / harness guards           | `frontend/src/components/admin/testCatalog/sections/AnalyzersSection.test.jsx`; `frontend/scripts/analyzer-harness-fixture-policy.test.js`, `analyzer-harness-nginx.test.js`                                                                                                                                                                                                                                                                                        | Catalog UI and harness policy/configuration checks. These are additional to the 17 core frontend files.                                                                                                                                                 |
| Core browser smoke                  | [analyzer-list.spec.ts](../../frontend/playwright/tests/foundational/core/analyzer-list.spec.ts), [analyzer-navigation.spec.ts](../../frontend/playwright/tests/foundational/core/analyzer-navigation.spec.ts), `core-app` project                                                                                                                                                                                                                                  | Basic visible dashboard/navigation; not mapping recovery.                                                                                                                                                                                               |
| Catalog and mapping browser stories | [ogc-1054-m1-analyzer-types.spec.ts](../../frontend/playwright/tests/demo/harness/ogc-1054-m1-analyzer-types.spec.ts), [ogc-1054-m2-shared-mapping.spec.ts](../../frontend/playwright/tests/demo/harness/ogc-1054-m2-shared-mapping.spec.ts), `harness-foundational` project                                                                                                                                                                                        | Actual catalog and mapping UI, desktop/mobile, save/reload/confirmation. Directory name `demo` does not determine execution project.                                                                                                                    |
| Guided setup and assembled review   | [ogc-1054-m3-guided-setup.spec.ts](../../frontend/playwright/tests/demo/harness/ogc-1054-m3-guided-setup.spec.ts), [ogc-1054-analyzer-mvp.spec.ts](../../frontend/playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts), `harness-demo` project                                                                                                                                                                                                              | Actual UI/service/database flow with simulated instrument traffic. Existing assembled story needs the replacement behavior in finding 9.                                                                                                                |
| Hardware-only browser story         | [analyzer-test-connection-manual-only.spec.ts](../../frontend/playwright/tests/manual-only/harness/analyzer-test-connection-manual-only.spec.ts), `harness-manual-only` project                                                                                                                                                                                                                                                                                     | Requires `GENEXPERT_HOST`, skips CI. Connection test, not complete clinical recovery acceptance.                                                                                                                                                        |

[playwright.config.ts](../../frontend/playwright.config.ts) defines these project
allowlists. [e2e-authoritative-reusable.yml](../../.github/workflows/e2e-authoritative-reusable.yml)
runs core and harness projects; [backend.yml](../../.github/workflows/backend.yml)
runs Maven tests. These are configured execution paths, not a claim about the
latest remote run. Frontend component tests run through Vitest; browser tests
are excluded from that runner. Maven's normal test task includes both isolated
and database test classes; the suffix alone is not a separate execution gate.

The harness uses [seed-analyzers.sh](../../projects/analyzer-harness/seed-analyzers.sh)
and [seed-mvp-traffic.sh](../../projects/analyzer-harness/seed-mvp-traffic.sh)
to create prerequisites through APIs and send native traffic through instrument
simulator → Bridge → OE. This is stronger than a substituted importer, but
setup actions are not proof the user performed those actions in the browser.

Separate component ownership:

- Bridge: `tools/openelis-analyzer-bridge/src/test/java/org/itech/ahb/`, including
  `fhir/FhirBundleBuilderLoincTest.java` (unknown raw-code preservation), protocol
  parsers, `normalizer/`, `contract/`, and control-recognition tests.
- Instrument simulator: `tools/analyzer-mock-server/test_*.py`, including
  `test_priority_profile_templates.py`, `test_bridge_profile_adapter.py`,
  `test_simulate_astm_qc_api.py`, `test_file_output_paths.py`, and transport tests.
  The simulator stands in for physical instruments; it is different from
  replacing an internal OE service with a no-op Mockito object.
- Exact deployed-build review and real hardware remain separate evidence;
  neither is inferred from these source files or earlier green test counts.

#### Historical evidence from the initial audit

No new tests were run for this catalogue. Earlier local evidence was **11 import
database tests + 13 import unit tests**, and **14 frontend tests in two files**,
with the earlier per-class real-audit setup. Those results do not validate the
current unfinished setup cleanup or automatic recovery. The subsequent cleanup
attempt reported **17 setup errors because Testcontainers could not access
Docker**; it never reached the intended behavioral checks. Do not describe that
run as an established failing behavior test.

At catalogue time, four history-test overrides and the import suite's local
overrides have been removed in the working tree, while `AppTestConfig` still
supplies the original substitutes. That is an incomplete cleanup, not a fixed
shared setup. No cleanup commit, new full-suite pass, or current assembled
acceptance is claimed.

### Analyzer test remediation execution plan

**Stable goal target:** this heading in
`specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md`, link
[#analyzer-test-remediation-execution-plan](#analyzer-test-remediation-execution-plan).
The catalogue above is the audit baseline; this section owns execution status.
The product [acceptance checks](#acceptance-checks) below remain authoritative.
Do not create another plan or copy those checks into a separate task file.

**Goal to use:**

> Implement the Analyzer test remediation execution plan in
> `specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md`. Complete T1, T2 and T3 in
> order, consolidating the analyzer test setup and replacing shortcuts with
> tests through actual application services. Preserve separate duplicate-delivery
> protection and unrelated local work. Iterate on demonstrated failures and
> review findings until each milestone's validation gate and the existing
> held-result acceptance checks pass. Deliver reviewable commits and an
> assembled build identified by exact component revisions, with linked test
> evidence and this section updated. Keep named human acceptance separate.

The full goal includes the agreed recovery implementation in T3. A goal limited
to test foundations can explicitly target **T1 and T2 only**; it must leave T3
and the product acceptance checks open rather than claiming the workflow done.

#### Starting safely

- Recheck worktrees, branch, HEAD, staged/unstaged changes, submodule pins and
  current PR relationships before editing. The audit describes a dirty working
  tree at `762d40589b`, not a clean implementation checkpoint.
- Preserve and inspect the unfinished production/test changes already present.
  Four audit-test overrides and the import suite's local overrides were removed
  without completing the shared setup. Reconcile that partial work first;
  do not add another per-test workaround or discard it blindly.
- This documentation commit contains the catalogue and plan only. A fresh
  checkout of it will not contain the described uncommitted implementation.
  Establish the actual starting diff and record it in T1's evidence.
- Verify Docker access before database tests. A container startup failure is an
  environment failure, not a demonstrated failing business rule. Check Java 21
  and the existing local Testcontainers configuration.
- Run formatting only within the intended checkout. Nested reporting worktrees
  must not be traversed or changed by broad formatter patterns.

#### Ordered milestones and validation gates

| Milestone                                      | Scope and finding ownership                                                                                                                                                                                                                                                                                                                                                                                    | Validation gate before completion                                                                                                                                                                                                                                                                                                                                                                                             | Initial status                                                         |
| ---------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| **T1 — Consolidate the test foundation**       | Findings **1, 2, 5, 7, 12–16**. Provide real history recording and FHIR parsing for analyzer database integration; remove repeated dependency swaps; move isolated controller/selection tests out of the broad database context. Consolidate the fixture ownership and cache/cleanup behavior needed by this regression group. Audit other shared substitutes only when the actual analyzer path reaches them. | The real injected services parse messages and persist history without per-test repairs. Affected analyzer and audit suites pass together in normal and reversed class order. Test-owned state is cleaned up and required seed records survive. Run affected neighboring suites if a shared default changes. No new mock hides a discovered internal failure.                                                                  | **Reopened — shared isolation and complete selection unvalidated.**    |
| **T2 — Prove the real mapping lifecycle**      | Findings **3, 4, 6, 8**. Use injected local services and real catalog/user data for mapping revision creation, confirmation, independent adoption and import. Replace in-place changes to an existing mapping revision and add database query cases. Keep request/security tests explicitly scoped.                                                                                                            | Two analyzers genuinely adopt revisions independently; unconfirmed mappings remain ineligible; old revisions and confirmation history survive reload. Actual queries distinguish profiles/revisions. Save/confirm/adopt reject unauthorized requests without writes. Existing receipt replay, concurrency and rollback assertions still pass. Database tests run without fake internal mapping/confirmation/history services. | **Local implementation preserved; acceptance pending T1 corrections.** |
| **T3 — Complete recovery and assembled proof** | Findings **9, 10, 11**. Implement the agreed automatic recovery, replace manual/obsolete expectations, preserve unsaved edits, report actual control outcomes and correct harness confirmation preparation. Use the existing implementation order above for production changes.                                                                                                                                | All existing held-result acceptance checks below pass, including either event order, partial recovery, existing backlog, repeats/concurrency, source history and unsaved edits. The real-browser story recovers the original held rows after adoption without resend or a reprocess button. Review stored outcomes, console/trace/screenshots and the exact build; fix actionable review findings and rerun affected checks.  | **Pending T2.**                                                        |

Keep T1 independently reviewable from recovery production changes. Use one
bounded PR per milestone where required by the repository workflow, with small
coherent commits inside each. T2 may build on the existing receipt dependency;
T3 continues the recovery change. Refresh actual branch/PR relationships before
publishing. Preserve #4241's duplicate-delivery protection independently of
#4256's recovery behavior. No merge or deployment is implied by this plan.

T2 establishes real state transitions, eligibility and incoming import; it does
not need to implement automatic held-row recovery before T3. A passing T2 is a
trustworthy foundation, not completion of the product acceptance checks.

#### Findings register

Statuses are **Open**, **In progress — unvalidated**, or **Validated**. Mark a
finding validated only with a commit and relevant evidence. A coverage boundary
can be closed by explicit scoping plus the missing higher-level proof; it does
not require deleting useful narrow tests. Keep this table current after each
milestone rather than rewriting the historical audit as though it were current.

| Finding                                            | Owning change | Status                                   | Evidence                                                                                                                           | Remaining limitation                                                                                                         |
| -------------------------------------------------- | ------------- | ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| 1. Shared internal substitutes                     | T1            | Validated                                | #4332, `23be6e97d4`; T1 evidence below                                                                                             | History and parsing are real; mapping lifecycle coverage remains T2.                                                         |
| 2. Shared-instance dependency swaps                | T1            | Validated                                | #4332 removes four history overrides and parser replacements                                                                       | Manually assembled mapping persistence services remain tracked in finding 4.                                                 |
| 3. Mapping changes bypass lifecycle                | T2            | Open                                     | Catalogue source links                                                                                                             | Same-revision SQL changes and replacement confirmations remain.                                                              |
| 4. Manually assembled persistence services         | T2            | Open                                     | Catalogue source links                                                                                                             | Does not yet prove ordinary injected service wiring.                                                                         |
| 5. Isolated tests using broad database setup       | T1            | Validated                                | Eight isolated request/selection checks pass without database startup                                                              | Standalone request checks do not establish deployed authorization.                                                           |
| 6. Queries tested with substituted results         | T2            | Open                                     | Catalogue source links                                                                                                             | Database discrimination cases not established.                                                                               |
| 7. Shared fixture/cached-state isolation           | T1            | In progress                              | `027416ebf0`: rollback-owned review/QC fixtures and real permission setup; 1,273 checks in 137 classes pass in both orders         | Acceptance-test seed repair and retained catalog/cache state remain.                                                         |
| 8. Request/permission test boundaries              | T2            | Open                                     | Catalogue source links                                                                                                             | Real mutation authorization and explicit scope still need verification.                                                      |
| 9. Superseded manual/browser expectations          | T3            | Open                                     | Catalogue source links                                                                                                             | Replacement workflow not yet implemented/proved.                                                                             |
| 10. Unresolved rows treated as excluded by harness | T3            | Open                                     | Catalogue source links                                                                                                             | Partial-mapping harness case not exercised.                                                                                  |
| 11. Missing automatic-recovery acceptance          | T3            | Open                                     | Acceptance checks below                                                                                                            | All product acceptance checks remain open.                                                                                   |
| 12. Inconsistent fixture-helper connections        | T1            | Validated — connection ownership         | #4332, `960b178842`: all base helper paths join the active transaction; real PostgreSQL rollback/sequence checks and 70-class runs | Legacy seed repairs and remaining fixture owners are tracked under finding 7; analyzer sequence mappings are fixed.          |
| 13. Retained cross-context services/caches         | T1            | Open                                     | Static TestServiceImpl collaborators/maps and base context reset remain                                                            | Trace reached dependencies and prove context/cache isolation; no blanket global resets.                                      |
| 14. Incomplete regression selection                | T1            | Validated — current foundation selection | All 57 current core classes (312 checks) plus 76 neighbors pass both orders; exact 133-class selection is committed below          | Extend for three preserved T2 classes after restoration; full CI and browser acceptance remain separate.                     |
| 15. Guidance perpetuates fixture/context problems  | T1            | Validated — active guidance scope        | #4332 corrects root instructions, active guides, agent sources and reusable templates; documented backend example passes           | Historical specifications were not rewritten; generic templates require substitution and browser examples were not executed. |
| 16. Weak persisted query assertions                | T1            | Validated                                | #4332 asserts exact membership, order, offsets and empty boundaries across 26 analyzer-result service tests                        | Mapping-specific database query discrimination remains finding 6 in T2.                                                      |

#### T1 execution evidence — 2026-09-17

[PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332), commit
`23be6e97d4`, owns the independently reviewable foundation change. The stack is
#4241 → #4332 → #4256 while T2 is prepared. These are local validation results;
CI and required maintainer approvals remain pending for the new commits.

- **Demonstrated failure:** removing per-test substitutes first produced five
  missing-history assertions and seven null-parser errors in 13 checks. Docker
  and the migrated database were working. With real shared history and parsing,
  all 25 affected history/import/interpreter checks passed.
- **Isolation:** the controller now parses a real message and verifies its source
  identity and actor in a fresh controller. Result selection runs without an
  application context. Its initial failure exposed a static global configuration
  lookup; constructor injection removes that dependency. Eight isolated checks
  pass. These tests do not claim database or security-filter coverage.
- **Fixture ownership:** the four history suites create their own records inside
  rollback transactions. They no longer truncate tables, recreate missing
  reference rows, or modify an arbitrary configuration seed. Before/after
  database checks verify row counts and reference-table history settings. The
  now-unused configuration-seed repair helper was removed.
- **Failures exposed by the real recorder:** two analyzer-results tests used
  nonexistent actor IDs. They now use the seeded test user and assert saved
  results/history; the insert test previously checked only its input list.
- **Regression:** the following command passed **284 tests, zero failures or
  errors**, once with `alphabetical` and once with `reversealphabetical`:

  ```sh
  mvn test \
    '-Dtest=Analyzer*Test,*AuditTrail*Test,P0AuditEmitSmokeTest,TaskInterpreterImplNullPatientTest,SpecimenAwareResolutionIntegrationTest' \
    -Dsurefire.runOrder=alphabetical
  ```

  Both runs used the machine's verified Docker/Testcontainers socket settings.
  Spotless was restricted to changed files in the main checkout. Frontend
  formatting and `mvn clean install -DskipTests -Dmaven.test.skip=true` passed.

- **Preservation:** the unfinished recovery work remains saved separately. The
  four old stack commits were compared with their remote rebased equivalents
  before restacking; no recovery changes were discarded. No dependency pins
  changed in T1.

T1 does not establish automatic recovery, the real mapping lifecycle, assembled
browser acceptance, or human product acceptance. Those remain T2/T3 work below.

#### T1 full-CI follow-up — 2026-09-17

The first full backend run on `23be6e97d4` executed 6,344 tests and exposed 14
errors in neighboring suites ([failed run](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/35184997864)).
[PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332), now at
`2a4edb138f512f9f757b0ea29056e539a9695fe6`, corrects those causes:

- Replace the remaining QC-acknowledgment mock verification with persisted
  acknowledgment/history/actor assertions. Remove its conditional seed repair;
  require the migration-owned seed instead.
- Use actual fixture actors for status and pathology changes. Check saved
  pathology records instead of the input form. Advance the patient identity
  sequence when the fixture loader imports explicit IDs; audit tests do not
  repair it themselves.
- Fix the production UUID audit failure exposed by real control-lot updates.
  The shared history reference is now text, preserving numeric identifiers and
  accepting UUID identifiers. Entity-history and system-history queries both
  exercise UUID references; numeric history remains covered.
- Execute the actual Liquibase migration on an empty history table and 100,000
  existing records. Exact reference/content checksums survive upgrade and
  numeric rollback. After UUID history is recorded, rollback to the numeric
  schema fails without changing or discarding history. This migration requires
  maintainer review before deployment; it has not been deployed.

The following selection passes **573 tests in 91 classes, zero failures or
errors**, with both `alphabetical` and `reversealphabetical` execution order:

```sh
mvn test \
  '-Dtest=Analyzer*Test,*AuditTrail*Test,*History*Test,P0AuditEmitSmokeTest,TaskInterpreterImplNullPatientTest,SpecimenAwareResolutionIntegrationTest,QC*Test,AccessionValidation*Test,StatusServiceTest,PathologySampleServiceTest,PatientIdentityServiceTest' \
  -Dsurefire.runOrder=alphabetical
```

The log verifies reversed first/last classes in the second run. Retained local
logs and XML reports are in
`/private/tmp/ogc-1220-validation/t1-ci-remediation/`. Scoped Spotless, frontend
formatting, and the required clean build pass. These results describe the code
committed as `2a4edb138f`; full CI on that published commit and maintainer approval
remain pending ([current backend run](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/35189332931)).
The stack still starts at current `develop` (`6e381be504`).

T2 work is preserved separately while the foundation correction runs in CI.
Its local checkpoint passes 63 checks covering mapping eligibility, real query
discrimination, receipt replay/concurrency/rollback, and partial persistence-test
cleanup. It is not yet published or complete: activation/probe service wiring,
authorization with real mutation services, and the complete ordering/regression
checks remain. T3 automatic recovery and assembled acceptance remain pending.

#### Catalogue correction and current evidence — 2026-09-17

T1 is **not complete**. Published foundation `7ca2a637cb` fixes real parser,
history and some fixture issues, but its 661-check local passes did not cover
all analyzer classes or all effects of the shared helper change. The subsequent
mapping candidate ran 680 checks with 11 startup errors in reverse order.
`StatusServiceTest` followed by `AnalyzerSiteBindingPersistenceIntegrationTest`
reproduced six errors in 15 checks; the local rollback correction passes those
15 checks and remains provisional pending the wider ownership correction.

The [backend run on `7ca2a637cb`](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/35193898574)
was cancelled after approximately six hours, with the last reported class
`QuestionnaireResponseServiceTest`. Its
[test reports](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/35193898574/artifacts/10499565822)
and the saved log support investigation, not a passing full-backend claim.
Frontend checks passed on that head; this does not establish backend or assembled
acceptance. The sequence-review finding in #4332 was resolved, but required
maintainer approval remains separate.

Retained source inventory, helper-call lists, current implementation diff and
preservation manifest are under
`/private/tmp/ogc-1220-catalogue-refresh/`. Failing mixed-order reports are under
`/private/tmp/ogc-1220-validation/t2-mapping-lifecycle/reverse-before-remaining-fixtures/`.
The status reproduction logs are
`/private/tmp/ogc-1220-t2-status-order-red.log` and
`/private/tmp/ogc-1220-t2-status-order-green.log`. These are local artifacts,
not published CI or completion evidence. This catalogue update changes no test
or application behavior and does not rerun or reclassify earlier failures as passes.

**Next implementation order within the existing milestones:**

1. T1: correct the remaining analyzer fixture ownership and sequence mappings;
   base helper connection ownership is validated as recorded below.
2. T1: remove reached seed-repair/cache workarounds and correct remaining
   ordinary fixture ownership. Active guidance and weak query assertions are corrected.
3. T1: run the complete inventoried analyzer set plus affected callers in both
   class orders, then full backend CI. Record exact selection and saved evidence.
4. T2: evaluate the preserved mapping code against that foundation, complete
   real save/confirm/adopt/query/permission checks and publish the milestone.
5. T3: complete automatic recovery, truthful control outcomes, unsaved-edit
   preservation and assembled acceptance. All existing acceptance checks stay open.

Do not expand into unrelated OE2 module remediation to avoid these gates. The
small broader specification records reusable design and follow-up boundaries;
it does not introduce another active analyzer checklist.

#### Cleanup ownership and active guidance correction — 2026-09-17

Published in [foundation PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332),
head `084e0942f8`: code correction `6d1537894a`; documentation corrections
`851d6c61e4`, `84f4f59304` and `084e0942f8`.

- A bounded local reproduction captured the fixture transaction holding a lock
  while the same test waited for cleanup on another connection. This reproduces
  the class at which earlier CI stopped; it is not a captured CI thread dump.
  Cleanup now joins the active transaction, or owns an atomic transaction when
  none exists. Tests verify rollback, independently visible commit, and rollback
  after cleanup failure. A query timeout bounds future lock failures.
- Analyzer-result and status service fixtures now roll back with their writes.
  Exact query assertions exposed three previously unnoticed wrong analyzer IDs.
  Sequence regression coverage now verifies advancement without ID reuse rather
  than requiring a sequence to rewind to a fixed value.
- The explicit 38-class selection covers 33 direct cleanup callers and five
  ownership/query/sequence neighbors: **538 tests, zero failures, errors or skips
  in each of alphabetical and reverse alphabetical order**. XML reports verify
  the executed class set. Scoped formatting and the clean build passed. This
  selection does not close finding 14's complete analyzer inventory requirement.
- The bounded documentation audit corrected root agent instructions, active
  testing guides, backend-testing skill sources and reusable templates. They
  now distinguish unit, component, integration and end-to-end tests; use actual
  Maven/Vitest/Playwright runners; avoid unsupported Spring Boot test patterns;
  and explain rollback ownership, committed fixtures and cascading cleanup.
  Active guidance was checked against current code and configuration. Historical
  specifications and constitutional policy were not rewritten.
- Validation of the documentation included formatting, relative links,
  temporary agent-command compilation and execution of the documented backend
  example against PostgreSQL (**one passing test**). Placeholder templates were
  not all instantiated, and browser examples were not executed. No dependency
  pins changed. Full CI and maintainer approval on the published head are pending.

Local evidence is retained in `/private/tmp/ogc-1220-fixture-ownership/`, including
explicit class selection, reports for both orders, build and example logs.
The initial lock/thread capture is in `/private/tmp/ogc-1220-fixture-locks.json`
and `/private/tmp/ogc-1220-fixture-threads.txt`.

**T1 remains open:** finish remaining fixture owners, reached seed/cache repairs,
other helper connection paths, complete analyzer selection and full CI. T2 local
work remains preserved separately; T3 automatic recovery remains unimplemented.

#### Remaining helper ownership and caller corrections — 2026-09-17

[Foundation PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332)
now includes `960b178842`; the preceding documentation formatter correction is
`ec8ece3f9f`. Maven formatting passed locally and in CI on the latter commit.

- New PostgreSQL checks reproduced helper-created records surviving rollback
  and explicit sequence synchronization failing to see uncommitted fixture data.
  All remaining base helper connections now participate in the caller's
  transaction. Both sequence paths share monotonic advancement; duplicate raw
  connection handling was removed. The seven focused ownership checks pass.
- The expanded caller run exposed two referral checks whose enclosing test
  transaction blocked their `REQUIRES_NEW` service calls. Captured locks and
  thread stacks prove the self-block; only those blocked disposable-database
  queries were cancelled to finish the diagnostic run. Those checks now use
  committed setup. This is not a blanket removal of test transactions.
- A QC chart fixture left an invalid user version behind and caused eight later
  referral failures. A bounded reproduction proved the dependency; rollback
  ownership removes it, and the chart's unnecessary reference-row repairs are
  gone. The sample-tracking test now uses inherited database wiring instead of
  overriding its setter without initializing the base. Two practitioner date
  boundaries now use the loaded fixture's instant; the prior failures also
  reproduced on the published baseline and disappeared under UTC.
- The 50-check caller reproduction passes after correction. All **70 explicitly
  selected classes pass in alphabetical and reverse alphabetical order using
  the normal local timezone: 883 executed checks, zero failures/errors, and one
  pre-existing ignored generic-sample check per run**. The ignored check concerns
  the existing notebook sample-item identifier type mismatch; no skip was added.
  XML reports verify class coverage, source hashes stayed unchanged during both
  runs, scoped formatting passed, and the clean build passed.

Evidence, class selection and reports are under
`/private/tmp/ogc-1220-helper-callers/`. The initial helper reproduction is
`/private/tmp/ogc-1220-helper-ownership-red-final.log`. These are local checks,
not final-head CI or assembled acceptance.

The [older published-head backend run](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/35241235751)
was still active when inspected. Its available log already reports an analyzer
profile-binding sequence collision, a site-branding write inside a read-only
transaction, and a result-option test depending on an ambient catalog row.
These require follow-up; do not treat this local pass as a green full backend.
The complete analyzer selection remains open: 57 core concrete classes exist
on the foundation branch, and three catalogued lifecycle/query/security classes
remain in the separately preserved T2 work. Remaining fixture owners, retained
service/cache state, T2/T3, full CI and maintainer approval are still open.

#### Analyzer sequences, QC isolation and complete foundation selection — 2026-09-17

[Foundation PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332)
contains fixture commit `bffec5bc14` and branding correction `c7df5bf49f`.
The preceding documentation follow-up is `5be73151f8`.

- **Sequence ownership:** a real-service regression failed when a device fixture
  was followed by profile assignment: the next profile-binding ID collided with
  an imported row. The loader now advances the four analyzer graph sequences.
  The regression creates and rereads the analyzer, profile, mapping and revision
  after flushing/clearing persistence state, preserving the imported analyzers.
  Four callers no longer repair sequences themselves. The unused
  `AnalyzerTestCleanup` helper, which swallowed cleanup failures and could rewind
  IDs, is deleted. All 35 focused sequence/caller checks passed.
- **QC fixture ownership:** the expanded run exposed eight referral failures
  caused by a QC dashboard fixture leaving an invalid shared user behind. A
  two-class reproduction passed all 22 dashboard checks then failed all eight
  referral checks. The dashboard now rolls back fixture rows and timestamp
  updates, uses inherited database wiring, and refreshes its name cache after
  rollback. It does not repair the later referral test's prerequisites.
- **Production failure exposed by CI:** all 18 branding controller checks failed
  because first access attempted to create defaults inside a read-only service
  transaction. The service now uses its writable transaction. The existing
  first-access check also reads the committed default from the database after
  the request. This correction is a separate commit from fixture cleanup.
- **Focused correction validation:** all 83 checks across the nine affected
  classes passed. The initial expanded report counted 1,238 checks, including
  three failures, 23 errors and one pre-existing ignore; after correction, both
  alphabetical and reverse alphabetical runs executed every one of the 133
  selected classes: **1,237 checks passed, zero failures/errors, one pre-existing
  ignored check per run**. All 57 current analyzer core classes account for 312
  passing checks in each order. The ignored generic-sample test concerns the
  existing notebook sample-item identifier type mismatch; no skip was added.
  Class coverage and unchanged source hashes were checked against the reports;
  scoped formatting and the clean build passed. No dependency pins changed.
- **Documentation follow-up:** `5be73151f8` corrects remaining wrong Maven/Vitest
  selectors, a nonexistent Playwright project, duplicate asynchronous-testing
  advice, broken links and local setup instructions. Twenty command sources
  compile, the three changed generated commands match their sources, launcher
  options and edited claims/links were checked, and formatting/build passed.
  This remains a bounded active-guidance review, not an exhaustive historical
  specification rewrite or browser execution claim.

The [exact tested class selection](evidence/ogc-1220-t1-regression-selection.txt)
is committed with this ledger. From the tested foundation checkout, with the
roadmap branch fetched, reproduce either order using:

```sh
analyzer_selection=$(mktemp)
git show origin/feat/ogc-1054-held-result-reprocessing:specs/roadmaps/evidence/ogc-1220-t1-regression-selection.txt > "$analyzer_selection"
mvn test "-Dtest=$(paste -sd, "$analyzer_selection")" -Dsurefire.runOrder=alphabetical
# Repeat with -Dsurefire.runOrder=reversealphabetical.
rm "$analyzer_selection"
```

Use the machine's Docker/Testcontainers environment for these database tests.
Detailed local reports, red/green logs, selection manifests and source hashes are
retained under `/private/tmp/ogc-1220-analyzer-sequences/`. The published commit
and committed selection identify the tested source/scope independently of those
local files. The older published-head backend CI run
[35245448815](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/35245448815)
was still active when inspected and reproduced the branding failure; new-head CI
must establish its final disposition.

**T1 remains incomplete.** Remaining result-review/acceptance/QC-result fixture
owners, catalog seed repairs and retained service/cache dependencies are still
open. The previously observed result-option test's ambient-catalog dependency
remains a remediation item even though it passed this selection. The three new
T2 lifecycle/query/security classes remain preserved separately and must be added
when that work is restored. T2/T3 implementation, complete CI, browser evidence,
stack restacking and maintainer approval remain pending. These local passes do
not establish automatic recovery or human product acceptance.

#### Review permission and persisted-outcome iteration — 2026-09-17

[Foundation PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332)
now includes `027416ebf0`. This is a T1 correction; it does not change the T2
mapping lifecycle or T3 recovery scope.

- **Demonstrated fixture interference:** `RoleModuleServiceTest`,
  `UserRoleServiceTest`, and `PermissionModuleServiceTest` could replace the
  role catalog before validation review tests ran. The old review helper then
  silently re-created a missing Validation role and SQL join rows. The selected
  reverse-order run failed all 14 review checks when that helper was removed,
  proving that the permission fixtures leaked state.
- **Correction:** those three permission suites and `RoleServiceTest` now own
  their fixtures in rollback transactions. The validation review fixtures own
  their Validation role, and review setup creates user-to-lab-unit grants through
  the real `UserService` instead of repairing tables directly. The obsolete
  `ValidationLabUnitRoles` SQL helper is deleted.
- **Persisted outcomes:** review mutation checks flush and clear before reading
  saved analyses, notes, history, and acknowledgments. The QC-result integration
  checks likewise reload the saved row before asserting its value and z-score.
  This proves stored outcomes instead of in-memory objects returned by the
  service call.
- **Validation:** the focused set of 71 checks passed. A 137-class selection
  (the committed 133-class foundation list plus the four role-fixture owners)
  passed in alphabetical and reverse alphabetical orders: 1,273 executed checks,
  zero failures/errors, and one unchanged ignored generic-sample check per run.
  Reports, source hashes, and logs are under
  `/private/tmp/ogc-1220-review-fixture-ownership/`. Scoped formatting and
  `mvn clean install -DskipTests -Dmaven.test.skip=true` passed. New-head CI is
  required before this evidence is a CI pass.

#### Acceptance-fixture ownership iteration — 2026-09-17

[Foundation PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332)
now includes `223d84b3d1`. This is a T1 correction; it does not change the T2
mapping lifecycle or T3 recovery scope.

- **Demonstrated interference:** removing the acceptance test's repair SQL and
  running the 137-class selection exposed a real foreign-key failure: a prior
  fixture had left the shared status catalog without the sample status required
  by the reviewer-choice path. An isolated run had hidden that dependency.
- **Correction:** the acceptance test now owns the required status catalog using
  the existing transactional fixture loader. It removes direct recreation of
  global statuses, history types, unknown-patient records, sequence resets, and
  manual cleanup. The test clears only the static patient and sample-type caches
  before setup and after transaction rollback; production services create the
  ordinary records exercised by the scenario.
- **Persisted outcomes:** the test continues to query the database for the held
  staged row, hold reason, absence of an unchosen sample, and the reviewer-chosen
  sample type. Those checks run after the real acceptance service; they are not
  assertions on a substituted collaborator or an in-memory return value.
- **Validation:** the focused acceptance/service set passed 28 checks. The
  137-class selection passed in alphabetical and reverse alphabetical orders:
  1,273 executed checks, zero failures/errors, and one unchanged ignored
  generic-sample check per run. Reports, source hashes, and logs are under
  `/private/tmp/ogc-1220-review-fixture-ownership/` and
  `/private/tmp/ogc-1220-accept-fixture/`. Scoped formatting and
  `mvn clean install -DskipTests -Dmaven.test.skip=true` passed. New-head CI is
  required before this evidence is a CI pass.

#### Result-options fixture ownership iteration — 2026-09-17

[Foundation PR #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332)
now includes `0d35e7569a`. This is a T1 correction; it does not change the T2
mapping lifecycle or T3 recovery scope.

- **Demonstrated dependency:** `ResultSelectListOptionsTest` selected the lowest
  existing dictionary-category and test IDs solely to satisfy its foreign keys,
  then manually deleted only some of the rows it created. Its result was tied to
  ambient catalog state rather than records it owned.
- **Correction:** the test now creates its own minimal category, test, dictionary,
  and result rows inside a rollback transaction. It retains the real service and
  database assertions: a valid option remains visible while missing and blank
  dictionary references are safely omitted. Manual cleanup is removed.
- **Validation:** the focused options/service pair passed six checks in both
  alphabetical and reverse alphabetical orders. The complete 137-class selection
  also passed in both orders: 1,273 executed checks, zero failures/errors, and
  one unchanged ignored generic-sample check per run. Source hashes match the
  published test source. Reports, source hashes, and logs are under
  `/private/tmp/ogc-1220-review-fixture-ownership/`. Scoped formatting and
  `mvn clean install -DskipTests -Dmaven.test.skip=true` passed. New-head CI is
  required before this evidence is a CI pass.

**T1 remains incomplete.** `TestServiceImpl` retains cross-context static
collaborators and maps. Its public static naming API is used by result reporting,
worklists, catalog screens, and scheduled work, so its correction needs a
separate migration boundary and behavior-focused regression plan. Do not hide it
with cache resets or a blanket test annotation. After that correction, restore
and validate T2 before implementing T3.

#### Iteration and evidence rules

1. Select a finding and its owning milestone. State what behavior the check will
   prove and which dependencies must run for real.
2. Reproduce the relevant failure or establish the missing behavior with a
   failing test. Record the failure cause; compilation/container failures do not
   substitute for a behavioral failure.
3. Make the smallest complete correction and remove the superseded workaround
   or expectation. Fakes remain appropriate for isolated logic and external
   boundaries; avoid blanket replacement of all shared substitutes.
4. Run focused checks, inspect persisted results and history where relevant,
   then run the affected regression group. Broaden testing when shared setup or
   new failures justify it. Perform the milestone's specific validation gate.
5. Format within the checkout, complete the required build/checks, review the
   diff and commit only coherent completed work. If a shared setup repair
   exposes an unrelated failure, record the actual failure and scope before
   deciding where its fix belongs; never weaken its assertion to obtain green.
6. Update the findings register and milestone status with the implementation
   commit, exact commands/test selection, result, durable artifact link and
   remaining limitations. Reconcile review findings and rerun affected checks
   against the final revision before marking the milestone validated.

For executed evidence, use the existing CI test-report/artifact locations or an
explicit retained local artifact path. Temporary log names alone are not a
review handoff. Record tested HEAD **and any uncommitted diff**; tests against a
dirty tree cannot be attributed solely to its HEAD. Distinguish source audit,
unit tests, database tests, browser evidence, CI, and named human acceptance.

**Completion:** T1/T2/T3 gates and all held-result acceptance checks are
satisfied on the final reviewed revisions; every finding is validated with
linked proof or an explicitly reviewed scope decision; obsolete paths/tests are
removed; no required verification is left merely proposed. A test count or a
successful build alone does not complete the goal. G0 named human acceptance
remains a separate status.

### Acceptance checks

All checks below are pending. Existing tests for the draft manual action do not
prove this behavior.

- [ ] An unknown test absent from the profile and an unknown qualitative value
      both open the same editor, accept only valid local targets, and survive
      save, reopen, and confirmation.
- [ ] Two analyzers sharing a mapping adopt independently. An unconfirmed edit
      affects neither incoming nor held results. Both
      adoption-before-confirmation and confirmation-before-adoption use only
      each analyzer's eligible revision.
- [ ] Both mapping-related hold reasons recover automatically. In a mixed batch,
      resolved rows enter ordinary review, unresolved rows remain held, and
      attention/worklist counts match persisted state.
- [ ] Existing eligible backlog, repeated confirmation/adoption, concurrent
      processing, and duplicate transport delivery create no duplicate results
      and never overwrite already reviewed results or bypass delivery receipts.
- [ ] Source and mapping audit survive; analyzer/connection/profile ownership
      and permissions are enforced; unrelated hold reasons remain untouched;
      explicit exclusions follow the existing audit and retention contract.
- [ ] Worklist refresh preserves unsaved edits, and control outcomes reflect
      actual persisted processing rather than unconditional success.
- [ ] Focused JUnit 4, database, authorization, and real-router tests pass. The
      assembled visible flow recovers the original held result without resend or
      manual reprocessing. Inspect console, trace, and screenshots, then
      complete the applicable PR gates and named review on an identified build.

### Keeping one plan

The specification and feature map link here instead of copying the plan. When
publishing the amendment, update #4256's title/description and OGC-1220's
cross-link to point here and remove the obsolete manual-action/product-decision
wording. Those external descriptions have not yet been updated. Keep review
state in GitHub and reviewer answers in Grist; do not create a second task list,
proposal, or evidence ledger. Scope changes and implementation progress belong
in this section.

## Execution Loop

For every bounded behavior change:

1. Select one acceptance statement and name its owning repository.
2. Audit affected tests first: retain, rewrite, move, or delete. A test for the
   superseded architecture is not evidence.
3. Record the smallest failing test at the owning layer. Cross-repository
   behavior starts with producer and consumer contract failures.
4. Implement the smallest complete behavior and remove its replaced writer,
   reader, route, field, and test in the same slice.
5. Run owning tests, then the relevant integration and assembled tests.
6. Compare code and tests to this roadmap and run `digi-uw/code-qa` alignment,
   coverage, simplicity/legacy, companion, and evidence checks.
7. For visible behavior, inspect console output, trace, screenshots, runtime
   state, and desktop/mobile design comparison before recording video.

No later UI, screenshot, or video can waive a lower-layer failure.

Acceptance tests define the allowed system positively. Do not retain
source-file, class-name, or string blacklists for deleted implementations: the
possible wrong implementations are unbounded, and such checks are not
behavioral evidence. Delete superseded code, migrations, tests, and guidance;
use closed schemas, typed provider/consumer contracts, persistence and
migration integration, and assembled behavior to prove the resulting
architecture.

## Checkpoints

### R0 - Canonical Roadmap And Architecture

Deliver:

- one concise product specification, this roadmap, and one visual feature map;
- explicit Bridge/OE/mock/review ownership and profile semantics;
- a one-time released-data migration decision;
- historical 011/014 specifications reduced to provenance pointers; and
- deletion of duplicate plan/task/checklist/evidence documents.

Exit:

- the three canonical documents agree;
- only this file contains roadmap markers;
- old documents cannot be mistaken for implementation direction; and
- the user approves this architecture before lower-stack correction resumes.

### F0 - Acceptance Foundation

Deliver closed contract fixtures and executable owner tests proving:

- the OpenELIS analyzer contract contains only LIMS-owned state and a Bridge
  connection reference;
- the Bridge profile contract retains communication behavior and
  new-connection defaults through generic consumers;
- Bridge owns connection/runtime behavior and FILE transport;
- migration has one explicit outcome for each released analyzer; and
- Playwright user stories interact only through visible UI, enforced by the
  existing syntax-aware lint rule.

Priority profile fixtures prove the same profile data drives Bridge and mock
behavior. A synthetic valid profile proves OE renders the declared Bridge
contract without a fixed connection schema.

Exit: invalid contract fixtures are demonstrated red, accepted fixtures and
owning behavior are green, and no deleted implementation remains in the diff.

### E0 - Versioned Contracts And Migration Boundary

Deliver versioned producer/consumer schemas and fixtures for:

1. the single Bridge profile revision contract;
2. generic Bridge connection create/read/update/probe/activate/deactivate;
3. the reference-only OE analyzer contract;
4. exact activation command and acknowledgment;
5. normalized patient/control/unknown traffic with raw context; and
6. migration plan/apply/verify manifests and per-analyzer outcomes.

Connection create and commands are idempotent; updates use optimistic
concurrency; secrets are masked; probes are non-mutating; restart restores the
acknowledged active revision. The migration never infers a profile from a name,
plugin class, protocol, code, or LOINC.

Exit: both repositories consume the same closed fixtures, and provider/consumer
tests accept only the reference and command contracts declared here.

### M1 - Bridge Profiles And Analyzer Types

Deliver:

- the existing profile system evolved to the E0 contract, with one Bridge
  catalog and immutable Draft/Publish/Update/Duplicate/Deactivate lifecycle;
- only the three priority profiles published and tested end to end;
- no runtime profile files or profile-serving/application path in OE;
- a composed Carbon Analyzer Types list/detail/history/authoring workflow; and
- URL-backed search, filters, selected profile/revision/tab, breadcrumbs,
  reload, browser history, and matching `Analyser Import`/Global Administrator
  route and endpoint authorization.

Exit: each priority profile passes schema, semantic, runtime, mock transport,
and visible-flow proof through generic code; a new revision never repoints a
connection implicitly; unrelated authenticated roles cannot read or mutate
Analyzer Types.

### M2 - Local Mapping And Recognition Verification

Deliver:

- one profile-revision-scoped OE site binding;
- complete Test search and Result Option selection constrained to the mapped
  active Test;
- bound, excluded, and unresolved states with deterministic suggestions only;
- human-readable Bridge recognition summary and confirmation;
- durable revision/fingerprint/actor/time audit; and
- removal of per-analyzer mapping, `AnalyzerQcRule`, copied rule arrays, and
  hidden Bridge recognition fallbacks.

Exit: every distinct priority-profile concept is visible; shared LOINC never
collapses rows; stale verification is deterministic; operational QC changes do
not stale or gate mapping/activation.

### M3 - Guided Setup, Connection, Activation, And QC Link

Work these slices in order within the active paired PRs:

1. **Connection contract correction:** rewrite current full-state sync/probe
   tests to E0's durable Bridge connection contract.
2. **Bridge persistence:** create, update, probe, activate, deactivate, restart,
   optimistic-concurrency, idempotency, and secret-handling tests and code.
3. **Upgrade migration:** test plan/apply/verify against released OE fixtures;
   add the Bridge reference, migrate each retained analyzer without guessing,
   verify restart, then remove migration-only and old runtime schema/code for
   the final candidate.
4. **OE boundary:** retain only local fields and Bridge references; mediate the
   generic API; remove fixed connection fields, profile-default copying,
   protocol decisions, full-state registration, startup replay, and obsolete
   tests.
5. **Guided Carbon UI:** preserve the inline Instrument/Verify/Connect workflow,
   lab-unit selection, summaries, URL state, breadcrumbs, and return paths;
   replace the protocol-specific form with a reusable descriptor renderer.
6. **Lifecycle:** make one OpenELIS lifecycle service the only writer that can
   move an analyzer into `ACTIVE`; show every local and Bridge blocker;
   build and synchronize one immutable candidate; require its exact Bridge
   acknowledgment; activate/deactivate that exact revision; keep draft edits
   from mutating the last active candidate; and make probe evidence visible but
   non-gating.
7. **QC link:** open the existing analyzer-scoped OE QC workflow and prove QC
   changes never alter setup verification or activation; use the same analyzer
   permission at the visible route and REST boundary.
8. **Preview:** deploy the PR-backed OE/Bridge/mock stack, sync the applicable
   Grist steps, and inspect the complete M3 visible flow.

Exit:

- Bridge restart restores the active connection exactly;
- a synthetic profile field change requires no OE production/schema change;
- the released-data migration reports every source analyzer and leaves no old
  runtime path in the final candidate;
- no OE connection value, protocol/transport branch, full-state writer,
  `AnalyzerQcRule`, duplicate create/edit route, or duplicate connection modal
  remains;
- create/update payloads and mapping, connection, error, or offline events
  cannot activate an analyzer or bypass the lifecycle service, and no hard
  delete path remains;
- analyzer setup, probe, lifecycle, and linked QC reject unrelated
  authenticated roles at the endpoint and visible-route boundaries; and
- focused backend, RTL, contract, assembled, accessibility, and visual gates
  pass.

### M4 - Safe Result Traffic And Integrated MVP

Deliver:

- known patient and recognized-control traffic through real Bridge transports;
- durable hold and visible attention for unknown tests and values;
- local mapping resolution as amended by the
  [OGC-1220 remediation](#ogc-1220-held-result-remediation);
- analyzer result review and resolution use the same established analyzer
  permission at both page and endpoint boundaries;
- priority ASTM and FILE mock stories plus a generic HL7 contract fixture;
- outbound orders addressed only by Bridge connection ID plus clinical order;
  and
- removal of superseded OE plugin routing, raw import/parser paths, local
  `AnalyzerType` registry, and direct-to-OE mock acceptance modes after parity.

The following slices record the original M4 delivery scope. Its PRs are merged;
new held-result work follows the OGC-1220 section above, not the former stack.

1. **Profile lifecycle.** In M1 and M2, complete Create Profile through an
   editable, publishable Bridge draft; prove duplicate is single-submit,
   preserves Bridge lineage, leaves its source unchanged, and initializes a
   separate unconfirmed OE site binding from the source mapping decisions,
   including observed unresolved concepts. Prove Bridge lifecycle behavior in
   contract/persistence tests and OE behavior in service/integration tests plus
   real-router RTL.
2. **Mapping and attention.** In M2, make completeness and Needs Attention
   include both profile-declared concepts and observed unresolved tests/values,
   and recompute them after traffic or site-binding changes. Give unknown tests
   and unknown values one catalog-backed resolution workflow; allow only valid
   active local targets and audit the decision. Held-result recovery follows the
   [OGC-1220 remediation](#ogc-1220-held-result-remediation). Expose the
   site-binding mapping history in that workflow with its actor, time, profile
   revision, and mapping decisions; profile-publication history must not
   masquerade as local mapping history. Prove domain behavior in OE integration
   tests and visible behavior in real-router RTL before the assembled story.
3. **Connection and QC.** In M3, restore the Bridge-provided latest probe after
   reload. Starting Add Analyzer while another analyzer is open must clear the
   prior identity and create a new analyzer, while setup breadcrumbs and
   lifecycle confirmations must name the current analyzer and focused action.
   Make the canonical New Control Lot action submit successfully; restrict its
   Test choices to active tests mapped for the selected analyzer; surface
   required statistics and server validation beside their owning fields; and
   show profile display names rather than raw identifiers. Rerun activation
   against the current build and fix it in this candidate if the historical
   server failure remains; do not change activation from stale evidence alone.
   Prove probe persistence in Bridge/OE consumer tests and QC behavior in OE
   integration tests plus real-router RTL. Operational QC remains separate and
   never gates verification or activation.
4. **Result review.** In M4, show source analyzer identity and raw source
   context for normal, held, control, and FILE traffic, including the source
   unit when the analyzer supplied one. Use Bridge and analyzer-mock transport
   tests for transmission and an external demo-operator action for resend; do
   not add an OE mock control. Finish with the UI-only patient, control,
   unknown-test, unknown-value, and FILE Playwright stories.

Original exit: all four remediation slices are green in M4 and any required
companion PRs; patient/control/unknown behavior is proven in owning tests and
UI-only assembled Playwright stories; focused console, trace, runtime,
accessibility, and desktop/mobile screenshot review passes; and no old analyzer
runtime path survives. Current deployment and acceptance follow G0 after the
OGC-1220 correction; merging the original M4 PR does not close that gate.

### G0 - Exact Deployment And Human Acceptance

Deliver:

- exact OE, Bridge, mock, profile-catalog fingerprint, and review-tooling build
  metadata on `analyzers.openelis-global.org`;
- the 17 required Grist steps against that unchanged build;
- named human product-reviewer results; and
- inspected test reports, console, trace, desktop/mobile screenshots, visual
  comparison, MP4, build metadata, checklist revision, and exported report.

Exit: all 24 MVP criteria and all required UAT steps pass. Any failed required
step blocks acceptance and is triaged; issue filing remains an explicit human
action.

### R1 And R2 - Full Feature And Rollout

R1 adds broader profile curation/distribution, profile revision diff, bulk
adoption and rollback, mature alert triage/concurrency, maintenance/fleet
health, and full-feature UAT. R2 qualifies upgrades, migration, performance,
observability, backup/restore, operator guides, and representative site rollout.

## MVP Acceptance Criteria

| ID      | Observable acceptance                                                                                                                                                                                                                                                                                           | Required proof                                                         |
| ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| MVP-001 | Authorized users see searchable/filterable shipped and site Analyzer Types, completeness, use, lifecycle, and attention state; completeness and attention include observed unresolved tests/values and refresh after traffic or site-binding changes; unrelated authenticated roles cannot read or mutate them. | OE security/integration + real-router RTL + UI E2E                     |
| MVP-002 | Create reaches an editable, publishable draft; duplicate preserves lineage and initializes a separate unconfirmed OE site binding from the source decisions without changing the source; create, duplicate, update, publish, deactivate, and reactivate are audited and single-submit; no delete exists.        | Bridge contract/integration + OE service/integration + real-router RTL |
| MVP-003 | Published revisions are immutable and retained; update/duplicate never repoints a connection.                                                                                                                                                                                                                   | Bridge persistence/restart tests                                       |
| MVP-004 | The three priority profiles retain both profile jobs, use generic runtime code, and contain no operational-QC or site-instance values.                                                                                                                                                                          | Schema/semantic tests + Bridge/mock transport                          |
| MVP-005 | Every emitted test concept is independently visible and maps by complete active-catalog search; suggestions are uniquely deterministic.                                                                                                                                                                         | OE service/integration + RTL                                           |
| MVP-006 | Qualitative mappings target only active Result Options of the mapped Test; invalid/inactive/cross-test choices fail.                                                                                                                                                                                            | OE service/integration + RTL                                           |
| MVP-007 | Recognition is explicit `RULES` or affirmed `NONE`, evaluated only by Bridge, and shown as a plain-language confirmation.                                                                                                                                                                                       | Bridge profile/runtime + OE consumer/RTL                               |
| MVP-008 | Verification records exact profile/binding/recognition fingerprints, row states, actor, and time; relevant changes stale it, QC changes do not.                                                                                                                                                                 | OE persistence/audit integration                                       |
| MVP-009 | Mapping has one Analyzer Types editor with URL-backed state and return paths; no per-analyzer editor or duplicate queue exists.                                                                                                                                                                                 | Routing integration + real-router RTL + UI E2E                         |
| MVP-010 | Authorized users see Add Analyzer inline on `/analyzers`; Instrument, Verify, and Connect reveal in order and retain list context; unrelated authenticated roles cannot open or invoke setup.                                                                                                                   | OE security/integration + RTL + UI E2E                                 |
| MVP-011 | Meaningful routes, query state, breadcrumbs, reload, back, forward, headings, lab-unit labels, and the latest structured connection-probe evidence are deterministic.                                                                                                                                           | Bridge/OE consumer + real-router RTL + accessibility/UI E2E            |
| MVP-012 | Released OE analyzer configurations migrate once to explicit Bridge profile pins/connections with complete outcomes; old schema/code/tool is absent from G0 runtime.                                                                                                                                            | Migration integration + final-schema integration                       |
| MVP-013 | Bridge durably creates and edits a profile-pinned connection; OE renders generic fields and stores no analyzer-facing value.                                                                                                                                                                                    | Cross-repo contract + persistence + RTL                                |
| MVP-014 | Probe is structured and non-mutating; synthetic profile fields and defaults change without OE production or schema changes.                                                                                                                                                                                     | Bridge tests + OE consumer contract/RTL                                |
| MVP-015 | Analyzer-scoped Quality Control opens the canonical OE workflow under the same analyzer permission; New Control Lot saves and offers only active Tests mapped for that analyzer; profile names are user-facing; QC changes never alter verification or activation.                                              | OE security + analyzer/QC integration + real-router RTL + UI E2E       |
| MVP-016 | Activation/deactivation uses the exact connection/profile/config/runtime acknowledgment, shows each blocker, preserves history, and never depends on QC or probe success.                                                                                                                                       | OE/Bridge contract + lifecycle integration + RTL                       |
| MVP-017 | Connection commands are concurrency-safe/idempotent and Bridge restart restores the exact active revision; OE performs no full-state replay.                                                                                                                                                                    | Bridge restart/contract + OE service integration                       |
| MVP-018 | Known patient and recognized-control traffic reaches the correct OE workflow; normal, held, control, and FILE review show source analyzer identity and raw source context.                                                                                                                                      | Bridge/mock transport + OE assembled integration + UI E2E              |
| MVP-019 | Unknown tests/values are durably held, visibly flagged, included in Analyzer Type completeness/attention, and never clinically posted or dropped.                                                                                                                                                               | OE persistence/integration + real-router RTL + UI E2E                  |
| MVP-020 | Unknown-test and unknown-value resolution satisfies the [OGC-1220 acceptance checks](#acceptance-checks).                                                                                                                                                                                                       | OE security/integration + real-router RTL + UI E2E                     |
| MVP-021 | ASTM, HL7, and FILE fixtures prove patient/control/nonmatch/unknown behavior; FILE watching exists only in Bridge.                                                                                                                                                                                              | Bridge/mock suites + assembled integration                             |
| MVP-022 | New UI uses reusable Carbon components, React Intl, one semantic heading, keyboard/focus behavior, and no overlapping text at desktop/mobile sizes.                                                                                                                                                             | RTL/a11y + inspected screenshots                                       |
| MVP-023 | Analyzer dashboard, Analyzer Types, setup, mapping, and QC links form one consistent visual workflow compared with `openelis-work@main`.                                                                                                                                                                        | Desktop/mobile visual review + named human UAT                         |
| MVP-024 | One unchanged deployment identifies exact component builds and checklist revision across tests, screenshots, trace, MP4, and report.                                                                                                                                                                            | Build manifest + review-tooling report                                 |

## Test Ownership

| Layer                   | Proves                                                                                                              | Must not substitute for           |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| OE JUnit 4/integration  | Local domain, persistence, migration, audit, activation, QC independence, hold/resolution, Bridge consumer contract | Browser interaction               |
| Bridge repository tests | Profile/connection contracts, persistence, protocols, parsing, probes, commands, restart, FILE behavior             | OE clinical decisions             |
| Analyzer mock tests     | Deterministic real analyzer transport and failure cases using accepted profiles                                     | Product workflows                 |
| RTL with real router    | Carbon behavior, validation, URL state, bookmarks, history, reload, headings, breadcrumbs                           | Backend contracts                 |
| Assembled harness       | Real OE + Bridge + mock + database behavior and durable outcomes                                                    | Human usability                   |
| Playwright              | Visible user stories only                                                                                           | API assertions or backend polling |
| Grist UAT               | Named human functional and visual acceptance of an exact build                                                      | Automated regression coverage     |

Playwright user stories prohibit `page.request`, API assertions, backend
polling, forced controls, arbitrary waits, and fixture mutation during the
story. Seed/fixture loading is a precondition only. Run non-video first; inspect
console, trace, screenshots, and runtime state; record MP4 only afterward.

Every checkpoint runs focused tests first, then its affected package suites,
format/lint checks, assembled contracts where applicable, and `digi-uw/code-qa`.

## Required Grist UAT

1. `AN-MVP-001` Find and inspect a shipped Analyzer Type.
2. `AN-MVP-002` Create and publish a new Analyzer Type, then duplicate an
   existing type and inspect lineage, copied local mapping decisions,
   single-submit behavior, deactivation, reactivation, and revision history.
3. `AN-MVP-003` Review test mappings and resolve a catalog match.
4. `AN-MVP-004` Map a qualitative value using only that Test's Result Options.
5. `AN-MVP-005` Confirm the control-recognition summary.
6. `AN-MVP-006` Start Add Analyzer inline from the dashboard.
7. `AN-MVP-007` Select a type, name the analyzer, and assign lab units.
8. `AN-MVP-008` Save and reload the Bridge-owned connection through Connect.
9. `AN-MVP-009` Run a visible connection test, reload, and confirm its latest
   structured evidence remains visible.
10. `AN-MVP-010` Review blockers, activate, deactivate, and reopen the analyzer.
11. `AN-MVP-011` Send a known patient result through the mock and Bridge and
    confirm normal review shows its analyzer/source context.
12. `AN-MVP-012` Prove the unknown-test flow in the
    [OGC-1220 acceptance checks](#acceptance-checks).
13. `AN-MVP-013` Send a recognized control, open linked operational QC, and
    prove a control lot can be saved using only a mapped Test.
14. `AN-MVP-014` Send an unknown qualitative value and confirm it is held.
15. `AN-MVP-015` Prove the unknown-value recovery flow in the
    [OGC-1220 acceptance checks](#acceptance-checks).
16. `AN-MVP-016` Repeat the visible traffic story with a priority FILE type and
    confirm row-level analyzer/source context without an OE FILE configuration
    or import path.
17. `AN-MVP-017` Review desktop/mobile clarity, breadcrumbs, bookmark/reload,
    back/forward, focus, and overall consistency.

Grist is the checklist source. The overlay must refresh current steps, preserve
answers by stable step key, show errors, and export checklist revision, route,
actual URL, mark time, notes, reviewer, and exact build metadata. Review-tooling
changes require their own PR only when a failing harness contract proves a gap.

## Final Gate

G0 completes only when R0 through M4 and the OGC-1220 correction are merged,
`MVP-001` through `MVP-024` pass, the exact
OE/Bridge/mock/profile-catalog/review-tooling revisions are deployed, all 17
Grist steps pass under a named human product reviewer, and the inspected
evidence bundle describes one unchanged deployment.
