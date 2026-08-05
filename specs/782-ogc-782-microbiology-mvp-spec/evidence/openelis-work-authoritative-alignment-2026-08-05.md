# OpenELIS Work Authoritative Alignment Record

**Date:** 2026-08-05
**Authority revision:**
[`DIGI-UW/openelis-work@a1f720d7b3b0`](https://github.com/DIGI-UW/openelis-work/commit/a1f720d7b3b01db63387361495f4aa6589105003)
**Implementation baseline:** `08b5b3888af4ba9f1c506fc555138218e0d043a4`
**Remediation branch:**
`feat/782-ogc-782-microbiology-r1-authoritative-alignment`

## Authority Contract

OpenELIS Work is authoritative for actors, laboratory outcomes, visible
workflow order, information shown or captured, control meaning, requiredness,
defaults, state transitions, and observable acceptance behavior.

OpenELIS Work does not dictate database structures, Java classes, services,
controllers, API payloads, React component boundaries, or route names. Those
remain engineering decisions, provided the supported OpenELIS route presents
the authoritative behavior. An intentional behavior deviation requires an
explicit product ruling and evidence.

## Status Vocabulary

- **Not started:** no implementation evidence.
- **Implemented, evidence pending:** code exists but the complete behavior has
  not passed the required automated evidence.
- **Automated evidence passed, UAT pending:** automated evidence covers the
  authoritative behavior; a human ruling is still required.
- **Accepted with documented deviations:** human UAT passed and every source
  deviation is explicitly ruled.
- **Complete:** all required behavior and evidence gates passed.

No aggregate module claim may use **Complete** while one required behavior is
unmapped or human UAT is pending.

## Source Contradictions Requiring Rulings

| Topic | Conflicting source behavior | Proposed default | Status |
| --- | --- | --- | --- |
| M-03 Program trigger | The visual mock shows manual Program selection; the v2 FRS derives Program from selected tests with manual fallback | Use v2 test-driven derivation while retaining the mock's visible Program state and details layout | Proposed |
| Untyped test fallback | One v2 section permits a configured fallback or `UNASSIGNED`; another defaults to `BACTERIOLOGY` | Use an explicit site-configured fallback; otherwise `UNASSIGNED` | Open |
| Bacteriology plus TB | The narrative creates two linked cases on one SampleItem; one acceptance criterion rejects mixed protocols and requires a second sample | Create two linked cases on one SampleItem with independent lifecycle/history | Open |

The Program trigger is not an implementation blocker because both source
versions require a visible Program/details workflow. The two open rows must be
ruled before mixed/fallback behavior can be accepted.

## Guided Workflow Crosswalk

The product modules below follow the order in
[`amr-micro-workflow-flow.html`](https://github.com/DIGI-UW/openelis-work/blob/a1f720d7b3b01db63387361495f4aa6589105003/designs/microbiology/amr-micro-workflow-flow.html#L103).
The implementation status is deliberately narrower than “code exists.”

| Step | Product behavior | Downstream location | Current evidence | Status / required action |
| --- | --- | --- | --- | --- |
| 1. M-01 | Administer organism and antibiotic masters used by microbiology | M9 reference-admin spec/tasks | Administration code and automated tests exist | Automated evidence exists; authoritative visual UAT pending |
| 2. M-02 | Load, review, and activate breakpoint versions | M9 reference-admin spec/tasks | Import/activation code and automated tests exist | Automated evidence exists; authoritative visual UAT pending |
| 3. M-08 | Author and use reusable text macros | Separate Macro Library stack | Managed runtime and administration code exist above the micro stack | Separate cross-cutting feature; extract to its own PR/UAT stack |
| 4. M-12 | Link tests and reagent/card lots and capture the lot used | M8 clinical-completeness spec/tasks | Lot capture exists; full administration/reverse-link behavior is not proven | Partial; complete source crosswalk and UAT |
| 5. Test Catalog | Configure the microbiology workflow on culture-capable tests | MVP M1 and Test Catalog implementation | Configuration is implemented and tested | Automated evidence passed; human UAT pending |
| 6. M-03 | Derive visible Program and collect culture details during order entry | FR-001/FR-002; historical M3/T191 | Backend routing exists; partial fields render only on legacy `/SamplePatientEntry`; current `/order/enter` drops workflow metadata | Blocking remediation |
| 7. M-04 | Create linked workflow cases for one specimen | MVP M2/M3 | Sibling case identity exists; product rule contradiction remains | Partial pending mixed-workflow ruling |
| 8. M-07 | Work from the shared pending-cultures queue | MVP M6 and worklist remediation | Core queue, URLs, nav, filters, and actions exist | Partial; richer row context/views/ownership/recent activity remain |
| 9. M-04 | Work the culture case by current step | MVP M4 | Core case workbench and timeline exist | Partial; source-level setup/workflow comparison required |
| 10. M-04 | Record isolate work-up and identification | MVP M4 | Isolate creation/update exists | Partial; richer Gram/colony/preliminary/final ID behavior remains |
| 11. M-05 | Enter and interpret AST | MVP M5 plus M8 | Multi-reading AST, standards, repeat/retest, override, and review exist | Source-level interaction and human UAT pending |
| 12. M-06 | Review expert-rule findings | Deferred product outcome | No expert-rule workflow | Not started; separate future milestone |
| 13. M-11 | Log and follow critical communication | MVP M6/M7 remediation | Clinical record and Alert synchronization exist | Automated evidence exists; human UAT pending |
| 14. M-04 | Reclassify an unassigned or misrouted case | Edge case only; no deterministic tasks | No supported Change Workflow action | Not started; add product and engineering slice |
| 15. M-14 | Run the operational TB workflow | Deferred product outcome | Workflow enum/sibling scaffolding only | Not started; separate future milestone |
| 16. M-13 | Produce cumulative antibiograms | Deferred product outcome | No antibiogram workflow | Not started; separate future milestone |
| 17. M-09 | Validate and export WHONET data | M10 WHONET spec/tasks | Manual long-format configure/preview/repair/generate exists | Partial; exact package/compatibility and later delivery scope remain |

M-10 hub subscription and M-15 GLASS/FHIR remain valid module outcomes outside
the 17-screen walkthrough. Neither is implemented.

## M-03 Requirement Trace

| Source behavior | Product requirement | Engineering task | Current code/evidence | Remediation state |
| --- | --- | --- | --- | --- |
| Culture test derives Program = Microbiology | US1 scenario 1; FR-001 | R1-T004/R1-T011 | Modern test selection stores only test ID/name | Open |
| Details appear in the supported Add Order flow | US1 scenario 1; FR-002 | R1-T005/R1-T012 | Partial section is mounted only in legacy `AddOrder.jsx` | Open |
| Culture Method is required/defaulted/adjustable | FR-002 | R1-T006/R1-T013 | Routing resolves Method, but no current-order control exists | Open |
| Patient Origin uses a controlled choice | FR-002 | R1-T006/R1-T013 | Legacy implementation uses free text | Open |
| Number of Sets uses ruled bounds/default | FR-002 | R1-T006/R1-T013 | Numeric field exists; complete contract is unproven | Open |
| Clinical History accepts multi-line text and macros when available | FR-002 | R1-T006/R1-T013 | Legacy field exists; macro integration belongs to separate feature | Open |
| Antibiotic Exposure is a checkbox | FR-002 | R1-T006/R1-T013 | Legacy implementation uses a text area | Open |
| Critical Notify is a checkbox with ruled default | FR-002 | R1-T006/R1-T013 | Legacy implementation uses free text | Open |
| Removing the last culture test confirms before discarding details | US1 scenario 4; FR-002 | R1-T007/R1-T014 | No evidence | Open |
| Saving exposes the details on the created case without duplication | US1 scenario 5; FR-001/FR-002 | R1-T008/R1-T015 | Backend pass-through exists; current-route round trip is unproven | Open |
| Non-culture selection keeps details hidden and creates no case | US1 scenario 2; FR-001 | R1-T004/R1-T016 | Backend test exists; current-route UI test is insufficient | Open |
| Mixed bacteriology/TB follows the ruled sibling behavior | US1 scenario 3; FR-003 | R1-T009/R1-T017 | Backend sibling behavior exists; source contradiction remains | Awaiting ruling |

## Evidence Rule

For each required row, the implementation task must identify:

1. the pinned OpenELIS Work source;
2. the product requirement;
3. the production code path;
4. focused unit/component/service evidence;
5. a Playwright journey through configured navigation;
6. a separate Grist story and human ruling;
7. the deployed application and checklist revisions.

Direct navigation to an unsupported/legacy route, label-only assertions, API
inspection in place of visible behavior, or an unreviewed screenshot cannot
promote a behavior to Complete.
