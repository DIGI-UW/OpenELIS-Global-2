# Research and Source Reconciliation

**Date**: 2026-08-03
**Repo base**: `6aafb05a9345525e04a0749e01ba09a3e41b5c2d`

## Baseline Repository State

The following observations describe the branch base before M8 implementation:

- Final microbiology cases are mutation-locked by
  `MicroCaseMutationGuard`; no amendment lifecycle exists.
- `MicroIsolateServiceImpl.updateIdentification` overwrites the current
  identification and writes only a generic activity entry. It does not retain
  typed before/after identification history or a reason.
- `MicroReportProjectionServiceImpl` writes the current microbiology summary to
  the standard `Result` path. It updates the linked result on later projection
  and does not create a microbiology report-version history.
- OpenELIS already has Analysis revision/report behavior. The M8 engineering
  design must preserve that standard path instead of building an unrelated
  patient-report surface.
- `MicroAstRun` records panel, breakpoint standard, status, and review actors,
  but does not identify original/repeat/retest relationships or a run-level
  method/selection for reporting.
- `test_reagent_link`, its service/API, and the Test Catalog Reagents section
  exist in the branch. Inventory items, lots, usage, lot status, QC status, and
  optimistic locking also exist.
- No shared reagent-lot picker is present, and microbiology culture/AST setup
  does not write Inventory usage.
- No axe-based Microbiology accessibility harness is registered.
- The MVP task ledger names a performance follow-up but contains no repeatable
  200-case qualification fixture or percentile evidence.

The observations above are the pre-M8 baseline. At commit `554063fc8`, the M8
branch now reuses Test Catalog links and shared Inventory services to show
eligible and ineligible lots, recommend the first eligible lot by FEFO, consume
an explicitly selected lot under a row lock, and retain culture/AST bench
provenance. Catalog role remains display-only; it is not treated as
requiredness.

## Source Health Findings

| Artifact        | Classification                       | Finding                                                                                                                                                                                                                      | Product-safe interpretation                                                                                               | Engineering consequence                                                                                                                                                            |
| --------------- | ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| OGC-783 / M-NFR | Real contradiction                   | It calls all ten NFRs non-negotiable for Phase 1, while #3789 explicitly shipped without offline, browser-matrix, replication, and measured scale qualification.                                                             | This follow-up qualifies accessibility and the named laboratory-volume budgets; the remaining NFRs stay open.             | Do not mark OGC-783 complete from M8. Add evidence per NFR ID and list remaining gaps.                                                                                             |
| OGC-892         | Jira-state contradiction             | The perf/a11y child is Done/superseded, but the repo has neither the axe harness nor qualification evidence.                                                                                                                 | Performance and accessibility must be demonstrably verified.                                                              | Treat OGC-892 as historical wording, not proof of delivery; reconcile Jira after evidence exists.                                                                                  |
| M-NFR NFR-03    | Implementation leakage               | It prescribes specific audit tables and columns.                                                                                                                                                                             | Users need immutable, attributable before/after history.                                                                  | Reuse existing case activity/audit patterns and add typed records only where queryable clinical history requires them.                                                             |
| OGC-784 epic    | Stale scope / implementation leakage | Its description still claims ownership of reagent-link schema/admin surfaces even though current M-12 v3 narrows scope to shared picker and wiring.                                                                          | Bench users choose valid lots and results remain traceable.                                                               | Consume existing `test_reagent_link` and Inventory services; do not recreate catalog/inventory ownership.                                                                          |
| OGC-865/867/868 | Jira-state contradiction             | Superseded children are Done while the parent is Backlog and the shared picker/wiring remains absent.                                                                                                                        | Only delivered outcomes count.                                                                                            | Do not infer M-12 completion from child status. OGC-866 remains the closest active outcome contract.                                                                               |
| M-12 v3         | Implementation leakage               | It names component props, tables, and exact storage behavior as feature requirements.                                                                                                                                        | One consistent lot-selection workflow is reused at culture and AST setup, with FEFO and eligibility rules.                | Choose interfaces that match current Inventory/Test Catalog services and preserve transaction boundaries.                                                                          |
| M-12 v3 vs repo | Engineering decision needed          | `InventoryUsage` can reference an analysis/result, but does not identify the culture setup or AST run where a lot was consumed.                                                                                              | Historical case review must show which bench action used each lot.                                                        | Add narrow linkage from the bench action to shared Inventory usage rather than adding another lot/usage store.                                                                     |
| OGC-784 vs repo | Real contradiction                   | OGC-784 names `REQUIRED / OPTIONAL / SUBSTITUTE` reagent-link semantics, while the implemented Test Catalog foundation stores `PRIMARY / SECONDARY`; M8 excludes reagent-link authoring changes.                             | Required selections must block and optional selections must not, but product role and requirement are separate concepts.  | Do not infer requirement from `PRIMARY / SECONDARY`. Repeat AST can proceed; lot enforcement needs a product ruling and a separately recorded Test Catalog compatibility decision. |
| M8 US6 vs repo  | Real contradiction                   | The initial workload named exactly 30 timeline events alongside 5 isolates and 80 AST readings, but service-layer AST entry correctly writes one immutable activity per reading, so that case necessarily exceeds 30 events. | The benchmark must represent normal audited behavior and cannot delete or bypass valid history to hit an exact row count. | Treat 30 as a minimum workload: qualify 5 isolates, 80 readings, and at least 30 timeline events.                                                                                  |
| OGC-782 MVP     | Engineering decision needed          | Final locking is present; amendment semantics and standard-report revision behavior are not.                                                                                                                                 | A correction must preserve the original report and publish a distinguishable amended version.                             | Add explicit amendment/report-version orchestration while keeping the standard Analysis/Result reporting path authoritative.                                                       |

## Engineering Decisions

### Amendment and report history

- Use an explicit amendment lifecycle with one open amendment per case.
- Keep the previously released standard report unchanged while the amendment is
  in progress.
- Persist append-only microbiology report release versions for deterministic
  clinical/audit history.
- On amended release, create a new standard Analysis revision and Result rather
  than overwriting the prior finalized Analysis/Result. Repoint the active case
  projection link to the new revision while retaining the old standard records.
- Record typed isolate identification events with before/after values and also
  project a readable entry into the existing case timeline.
- Cancelling an amendment reverses its identification drafts, marks AST runs
  created within it cancelled, publishes no report version, and relocks the
  case. Existing reviewed AST runs cannot be edited during an amendment.
- Store report source Analysis/Result references as normalized immutable links,
  not delimited identifiers.

### Repeat AST

- A repeat/retest is a new AST run, never a mutation of an earlier run.
- Store the source-run relationship, attempt type, and reason on the new run.
- Preserve the breakpoint-standard snapshot and explicitly select the
  reportable reviewed run when multiple reviewed runs exist.

### Reagent traceability

- Reuse `TestReagentLinkService`, `InventoryLotService`, and
  `InventoryUsageService`; no second reagent or lot model is permitted.
- Eligibility is enforced in the service transaction immediately before usage
  is recorded. UI filtering is not the security or integrity boundary.
- Add only the minimum linkage needed to associate shared Inventory usage with
  a culture setup or AST run.
- Preserve `PRIMARY / SECONDARY` as catalog-role metadata and keep lot selection
  optional until a distinct requirement policy is approved. This permits
  traceability without silently changing existing order or bench workflows.

### Accessibility and performance

- Register a focused Playwright axe suite for stable Microbiology states and a
  separate keyboard-only journey.
- Build qualification data with an explicitly enabled test-support builder and
  existing domain services. Keep that builder out of the production WAR. The
  disposable browser stack may use the existing property-gated UAT endpoint.
  No SQL fixture, DAO bypass, fixed ID, or ordinary-deployment endpoint.
- Measure server/API and browser-visible budgets separately; emit raw iteration
  samples plus p50/p95/max and environment metadata.

Qualification uses service-created representative workloads for both API and
browser behavior. Worklist relationships are batch-loaded, and no speculative
index is added without a populated query plan demonstrating the need. Raw
samples, environment metadata, and percentile calculations belong in the CI or
pull-request run that produced them, not in this living research document.

Accessibility validation covers the touched Microbiology surfaces at desktop
and mobile sizes with Carbon role/label interactions and axe checks. Browser
journeys must use observable readiness rather than arbitrary waits, forced
clicks, private Carbon selectors, or implementation-specific timing.

One pinned `DIGI-UW/code-qa` pass reviews alignment, meaningful coverage, and
simplicity at the completed slice boundary. Generated reports are review
artifacts and are not maintained as parallel status documents in the feature
specification.

## Open Product Questions That Do Not Block Slice A

1. When multiple reviewed AST attempts exist, whether selecting one attempt or
   composing selected readings across attempts is required. Default: select one
   reportable run per isolate; cross-run composition is later work.
2. Whether pending-QC reagent lots are blocked or supervisor-overridable. The
   current Inventory domain treats only QC-passed lots as available. Default:
   block pending/failed/quarantined lots until a product ruling expands policy.
3. The authoritative baseline hardware for formal performance acceptance.
   Default: record the actual CI/demo environment and treat thresholds as
   qualification evidence, not a universal capacity guarantee.
4. How existing `PRIMARY / SECONDARY` Test Catalog reagent links acquire the
   required/optional behavior named by OGC-784. No default is safe: treating
   all primary links as required changes current workflows, while treating all
   existing links as optional cannot satisfy required-lot blocking. This blocks
   mandatory/optional/substitute enforcement. It does not block the implemented
   policy-neutral picker, exact-lot validation and consumption, provenance,
   repeat AST, accessibility, or the qualification harness.
