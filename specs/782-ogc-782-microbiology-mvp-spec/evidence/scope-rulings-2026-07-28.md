# OGC-782 MVP Scope Rulings

**Date:** 2026-07-28
**Authority:** Piotr is the final product/engineering judge. This file records
the deterministic defaults applied to PR #3789 and follow-on status; automated
evidence does not replace the live human UAT record.

**2026-08-05 correction:** The pinned OpenELIS Work alignment record
supersedes the M-03, shared-specimen, worklist-parity, and aggregate-completion
states below where noted. OpenELIS Work is the product authority; this file does
not infer product behavior from ticket wording.

| Topic | Conflicting evidence | Ruling for PR #3789 | Status |
| --- | --- | --- | --- |
| Traceability identity | The repository feature and PR use 782 while OpenELIS Work defines a broader M-00 module. | Keep 782 for repository traceability. Treat #3789 as the initial routine-bacteriology slice across M-03/M-04/M-05/M-07/M-11 outcomes, not completion of the full module. | Resolved |
| Amendment history | The original spec and M7 plan required amendment-safe history; the implementation provides final release and mutation locking but no amend/reopen/version workflow. | Final cases reject isolate and AST mutation in MVP. Amendment and re-identification history are V2 and must not be claimed by the PR or UAT. | Resolved |
| Shared-specimen TB | The current narrative creates bacterial and TB sibling cases on one SampleItem, while one M-03 acceptance criterion requires a second sample. | Proposed: create linked cases on one SampleItem with independent histories. Operational TB remains unimplemented. | Open product ruling |
| Reagent/card lots | M-12 and detailed AST mocks show lot linkage; the MVP AST path does not capture it. | Reagent/card lot linkage and richer multi-row run metadata are V2. Manual AST must still display every reading recorded in the current run. | Resolved |
| WHONET | Some source wording says export; the implementation exposes readiness only. | WHONET readiness is MVP. Export generation and mapping administration are V2. | Resolved |
| Worklist scale | M-NFR and early planning state 200 in-flight cases and sub-second p95; no repeatable service-created performance evidence exists. | Do not claim the target. Keep it as a separate performance qualification so functional MVP acceptance is not confused with unmeasured capacity. | Accepted risk |
| Order details | The July 3 gap analysis required order-entry capture, but the later partial repair mounted on legacy `/SamplePatientEntry`; configured Add Order uses `/order/enter`. | The supported Add Order workflow must visibly derive Program = Microbiology, present the complete ruled controls/defaults including Culture Method, confirm discard, and persist details to one resulting case. | Open remediation |
| Untyped workflow fallback | M-03 permits configured fallback/`UNASSIGNED` in one section and defaults to bacteriology in another. | Proposed: use explicit site configuration; otherwise create `UNASSIGNED` rather than guessing. | Open product ruling |
| Worklist parity | The deployed M-07 worklist has stable navigation/URL state but omits multiple laboratory context fields and operational views shown in the authoritative mock. | Describe it as a functional core. Complete richer context, ownership/technician filters, culture/AST views, resistance context, and recent activity in a dedicated alignment slice. | Open remediation |
| Macro ownership | M-08 appears in the microbiology walkthrough, but its FRS defines Macro Library as cross-cutting with microbiology as first consumer. | Keep Macro Library core/runtime/administration in a separate feature stack and UAT project; keep only microbiology consumption in the microbiology integration layer. | Resolved architecture boundary |
| Setup context | Earlier workbench evidence recorded a free-text setup note and hid patient/specimen context in a section. | Patient, accession, specimen, and workflow context remain visible at the bench. Media/bottle, incubation, and atmosphere are explicit setup inputs and persist in the activity record. | Resolved |
| Report proof | Earlier Playwright checked the report projection through a backend request. | Core E2E must navigate to the visible patient-results page and assert the reviewed S/I/R content there. | Resolved |
| Human acceptance | Automated checks and overlay rendering were previously described as UAT. | Automation is pre-UAT evidence only. Piotr must perform the ten-step Review-overlay session; pass/fail/N/A and notes are the acceptance record. | Open |

## Follow-On Status - 2026-08-04

The WHONET ruling above remains the historical acceptance boundary for PR
#3789. The stacked M3 branch adds organism and antibiotic mapping
administration. The stacked M4 branch adds the first manual export slice:
Configure, Preview, scoped mapping repair, Generate CSV, and immutable audit
metadata. It does not complete M-09's wide-format/profile packaging, remaining
mapping vocabularies, scheduling, delivery, TB, phenotype, or GLASS scope.

The combined live checklist now contains distinct M1-M4 stories. Human
acceptance remains open and must be recorded against the exact deployed
application SHA and matching live checklist revision. M-03 requires a corrected
standalone story after R1 deployment; the historical label-only step cannot
accept the supported order workflow.

## No-Migration Ruling

The July 28 story-closure work adds no data-model change. Order-entry details
reuse the existing microbiology order-detail model, case context is compiled
from existing sample/patient services, explicit setup values are retained in
the existing activity record, and report links use existing projected Result
identifiers. No Liquibase changeset is warranted for this slice.
