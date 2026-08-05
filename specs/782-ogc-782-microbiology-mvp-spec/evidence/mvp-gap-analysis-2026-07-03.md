# OGC-782 Microbiology MVP Gap Analysis

**Date:** 2026-07-03
**Scope:** Cross-reference the as-built MVP (PR #3789, branch
`feat/782-ogc-782-microbiology-mvp-m7-release-surveillance-readiness`) against
the openelis-work M-* FRS bundle, the speckit
[spec.md](../spec.md) functional requirements, and the Jira epic tree
(`OGC-782` and its build stories). Each row records the FRS/spec source, the
as-built code path, the cause of the gap, and the disposition decided for this
session.

## Already satisfied: cross-cutting M-00 decisions

The MVP honors the M-00 parent's landed cross-cutting decisions before any gap
work:

| Decision | FRS source | As-built |
| --- | --- | --- |
| Case keyed to `SampleItem x workflow_type`, not 1:1 with Sample | M-00 §"Cross-cutting decisions"; OGC-820 | [`MicroCase`](../../../../src/main/java/org/openelisglobal/microbiology/valueholder/MicroCase.java) `sampleItemId` + `workflowType` |
| Ordered test carries workflow routing (`culture_workflow_type`) | M-00; OGC-925 | `Test.getCultureWorkflowType()` read in [`MicroOrderRoutingServiceImpl.workflowTypeFor`](../../../../src/main/java/org/openelisglobal/microbiology/service/MicroOrderRoutingServiceImpl.java) |
| Culture protocol reuses Method | M-04 §8; OGC-841 | `MicroOrderRoutingServiceImpl.methodIdFor` requires `test.getMethod()` |
| AST reuses run/reading substrate, not raw result table hijack | M-05 | `MicroAstRun`/`MicroAstReading`/`MicroAstInterpretationServiceImpl` |
| WHONET extends existing export path | M-09 | `MicroWhonetReadinessServiceImpl` (readiness only, see gap below) |

## Gap rows

### 1. FR-002 order-detail capture

- **FRS source:** M-03 Order Entry hook (`m-03-order-entry-micro-hook.md`); spec [FR-002](../spec.md) ("Users MUST be able to capture microbiology order details ... including culture setup default, patient origin, number of sets, clinical history, antibiotic exposure, and critical notification preference").
- **As-built at the July 3 checkpoint:** [`MicroOrderRoutingServiceImpl.routeAnalysesForSampleItem`](../../../../src/main/java/org/openelisglobal/microbiology/service/MicroOrderRoutingServiceImpl.java) resolved workflow type and culture method but the generic order-entry page did not supply microbiology details.
- **Cause:** unintended divergence. The speckit M3 task (`tasks.md` T040-T052) narrowed scope to pure routing and silently dropped the order-detail-capture requirement, even though FR-002 is a spec "MUST."
- **Disposition: MVP. Resolved.** The order-detail model and workbench editor
  landed first. The July 28 closure then added a culture-aware section to the
  existing order form and passed its values through
  `SamplePatientEntryServiceImpl` to the existing routing overload. Non-culture
  tests do not reveal the fields. The closure reuses the existing model and
  therefore adds no migration.

### 2. Historical FR-016 amendment / reidentification report history

- **FRS source:** M-04 §"lifecycle" (AMENDED terminal stage) and the original
  spec FR-016. The current spec moves this outcome to V2-001.
- **As-built:** [`MicroCaseStage.AMENDED`](../../../../src/main/java/org/openelisglobal/microbiology/valueholder/MicroCaseStage.java) remains a reserved enum value, but `MicroCaseStateServiceImpl` permits no transition out of `FINAL_RELEASED`; there is no amend/reopen code path or report-version history. `MicroCaseMutationGuard` rejects isolate and AST changes after final release.
- **Cause:** mostly unintended. The enum value was added for future-proofing but the workflow was never implemented in M7.
- **Disposition: V2.** Not built. The MVP now rejects isolate and AST mutation
  after final release through `MicroCaseMutationGuard`; this is a safety lock,
  not amendment history.

### 3. M-11 Alerts Dashboard integration (reconciles FR-018)

- **FRS source:** M-11 Critical-Result Acknowledgment (`m-11-critical-result-acknowledgment.md`); OGC-785/OGC-889; spec [FR-018](../spec.md) ("Critical communications MUST surface through the existing operational alerts workflow rather than a parallel alerts experience").
- **As-built:** [`MicroCriticalCommunication`](../../../../src/main/java/org/openelisglobal/microbiology/valueholder/MicroCriticalCommunication.java) is a microbiology-owned table surfaced only through the microbiology worklist ([`MicroWorklistServiceImpl`](../../../../src/main/java/org/openelisglobal/microbiology/service/MicroWorklistServiceImpl.java)), not through the generic [`Alert`](../../../../src/main/java/org/openelisglobal/alert/valueholder/Alert.java) entity or the [Alerts Dashboard](../../../../frontend/src/components/alerts/AlertsDashboard.jsx).
- **Cause:** mechanical constraint, not a hard blocker. `Alert.alertEntityId` is a `Long` (`alert_entity_id BIGINT NOT NULL`, see `src/main/resources/liquibase/3.3.x.x/024-freezer-monitoring-schema.xml` lines 751-756); `micro_case.id` is a 36-character UUID string, so it cannot be written into the existing numeric column without a schema change. As shipped this is the "parallel alerts experience" FR-018 says not to build.
- **Disposition: MVP. Resolved this session.** Log-plus-projection (no dual-write, per Constitution Principle X): `micro_critical_communication` remains the clinical record of truth. Added a nullable `alert_entity_ref` column to `alert` plus a `chk_alert_entity_id_or_ref` constraint (`057-alert-entity-ref.xml`) — additive, does not touch the existing `BIGINT alert_entity_id` column or its Freezer/Equipment/Sample callers (regression-tested: `AlertServiceTest`, `FreezerAlertServiceTest`, `AlertFlowIntegrationTest`, `QCAlertServiceTest` all green). Added `AlertType.MICROBIOLOGY_CRITICAL`. `MicroCriticalCommunicationServiceImpl.logCommunication`/`acknowledge` now write/sync a corresponding `Alert` row keyed by `(MicrobiologyCriticalCommunication, communicationId)` as a dashboard-surfacing projection (see `MicroCriticalCommunicationAlertIntegrationTest` for the real end-to-end proof). Surfaced in the existing [AlertsDashboard](../../../../frontend/src/components/alerts/AlertsDashboard.jsx) via a new filter option. `FR-018` updated to describe the reconciled approach.

### 4. M-09 WHONET export + code-mapping dashboard

- **FRS source:** M-09 WHONET Export (`m-09-whonet-export.md`); OGC-794; spec [FR-020](../spec.md) / US6 scenario 2 ("export is not part of the current release slice").
- **As-built at this checkpoint:** [`MicroWhonetReadinessServiceImpl`](../../../../src/main/java/org/openelisglobal/microbiology/service/MicroWhonetReadinessServiceImpl.java) reported readiness/blockers only; no export generator or code-mapping admin UI existed in PR #3789.
- **Follow-on status (2026-08-04):** M3 adds organism and antibiotic mapping administration. M4 adds period/policy configuration, selected-set readiness, direct repair, preview, audited manual CSV generation, stable URL state, and accessibility evidence through `../782-ogc-782-microbiology-m10-whonet-export/`.
- **Remaining gap:** authoritative wide-format/profile packaging, the other unsettled mapping vocabularies, scheduling, delivery, exact re-download, TB, phenotype, and GLASS scope remain deferred.
- **Disposition: Partial follow-on delivery.** The original MVP remains readiness-only; the M4 branch must complete PR, exact-SHA deployment, and human UAT before acceptance.

### 5. M-05 AST depth: multi-row metadata, reagent lot, per-run breakpoint selection

- **FRS source:** M-05 AST Entry & Interpretation (`m-05-ast-entry-and-interpretation.md`), which specifies `micro_ast_run.breakpoint_standard_id` chosen and snapshotted **per run**, plus `reagent_lot_id` (M-12/OGC-784) and multi-row per-drug metadata.
- **As-built at the July 3 checkpoint:** AST used the default CLSI 2026
  standard. The remediation added per-run standard selection and snapshots the
  selected standard for interpretation.
- **Cause:** deliberate simplification, flagged in [mock-comparison-2026-06-27.md](./mock-comparison-2026-06-27.md) as a feature-depth gap.
- **Disposition: Split.**
  - **MVP (build this session):** per-run breakpoint-standard selection — let the tech pick an active standard when starting a run, snapshot it on `MicroAstRun`, and interpret against the chosen standard instead of the hardcoded default.
  - **V2 (not built):** reagent/card lot linkage (M-12/OGC-784) and multi-row AST run metadata.

### 6. Deferred by design (no action, already tracked)

M-06 Expert Rules Engine (OGC-793, Phase 1B), M-13 Antibiogram (OGC-900), M-14
Mycobacteriology/TB (OGC-901), and M-15 GLASS via consolidated FHIR (OGC-918)
are explicitly post-MVP in the M-00 phase plan and spec FR-021. No disposition
needed; they remain Backlog.

## Discovered during acceptance gate: final-release stage/readiness bug (fixed)

While standing up a real dev stack to collect Playwright browser evidence for
the acceptance gate (see `tasks.md` T146), the demo spec's final-release step
failed with an HTTP 500. Root cause, confirmed via server logs and code
inspection: [`MicroCaseReadinessServiceImpl.getReadiness`](../../../../src/main/java/org/openelisglobal/microbiology/service/MicroCaseReadinessServiceImpl.java)
computes final-release eligibility purely from isolate/AST-review/critical
communication data and never inspects `MicroCase.stage`; neither
`MicroIsolateServiceImpl` nor `MicroAstServiceImpl` ever advance `stage`
through the state machine. But
[`MicroReportReleaseServiceImpl.releaseFinal`](../../../../src/main/java/org/openelisglobal/microbiology/service/MicroReportReleaseServiceImpl.java)
required a strict `MicroCaseStateService.advanceStage` transition from
`REVIEW_READY`/`PRELIM_RELEASED`, which a readiness-eligible case could never
reach in practice. `releasePreliminary`, in the same file, never used that
guard.

This is a **pre-existing defect**, unrelated to FR-002/M-05/M-11, that this
E2E run surfaced. Fixed by making `releaseFinal` set `stage` directly once
readiness passes, consistent with `releasePreliminary` (see
`MicroReportReleaseServiceTest` for the updated/added regression coverage).
Confirmed fixed by a passing real-browser run of
`ogc-782-microbiology-mvp.spec.ts` through final release.

Two pre-existing stale Playwright selectors were also found and fixed in the
same acceptance-gate pass (raw-enum text expectations, e.g. `SETUP_RECORDED`,
where the UI has always rendered a formatted label, e.g. `Setup Recorded`) —
see `microbiology-case-workbench.spec.ts` and
`microbiology-worklist-critical.spec.ts`.

## Summary table

| # | Gap | Disposition | Built this session? |
| --- | --- | --- | --- |
| 1 | FR-002 culture-aware order-entry detail capture | MVP | Yes (resolved) |
| 2 | Amendment/reidentification history; MVP has final mutation lock only | V2 | No |
| 3 | M-11 Alerts Dashboard (FR-018) | MVP | Yes (resolved) |
| 4 | M-09 WHONET export + mapping UI | Partial follow-on delivery in M3/M4; broader M-09 remains deferred | M4 implementation complete locally; acceptance open |
| 5a | M-05 per-run breakpoint selection | MVP | Yes (resolved) |
| 5b | M-05 reagent lot + multi-row AST metadata | V2 | No |
| 6 | M-06/M-13/M-14/M-15 | Deferred by design | No |
