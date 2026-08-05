# OGC-782 MVP Scope Rulings

**Date:** 2026-07-28
**Authority:** Piotr is the final product/engineering judge. This file records
the deterministic defaults applied to PR #3789 and follow-on status; automated
evidence does not replace the live human UAT record.

| Topic | Conflicting evidence | Ruling for PR #3789 | Status |
| --- | --- | --- | --- |
| OGC-782 identity | Jira defines OGC-782 as a doc-only M-00 umbrella; the repo feature and PR use 782 as their traceability number. | Keep 782 for traceability. Treat #3789 as the single agreed MVP implementation milestone across linked M-03/M-04/M-05/M-07/M-11 outcomes, not as completion of the full umbrella. | Resolved |
| Amendment history | The original spec and M7 plan required amendment-safe history; the implementation provides final release and mutation locking but no amend/reopen/version workflow. | Final cases reject isolate and AST mutation in MVP. Amendment and re-identification history are V2 and must not be claimed by the PR or UAT. | Resolved |
| Shared-specimen TB | Product sources describe a complete M-14 TB module; MVP data and UAT include a sibling TB case. | The MVP proves that sibling workflow records can share a specimen and remain distinguishable. It does not claim operational TB processing. UAT step 10 is an inspection/reflection step only. | Resolved |
| Reagent/card lots | M-12 and detailed AST mocks show lot linkage; the MVP AST path does not capture it. | Reagent/card lot linkage and richer multi-row run metadata are V2. Manual AST must still display every reading recorded in the current run. | Resolved |
| WHONET | Some source wording says export; the implementation exposes readiness only. | WHONET readiness is MVP. Export generation and mapping administration are V2. | Resolved |
| Worklist scale | M-NFR and early planning state 200 in-flight cases and sub-second p95; no repeatable service-created performance evidence exists. | Do not claim the target. Keep it as a separate performance qualification so functional MVP acceptance is not confused with unmeasured capacity. | Accepted risk |
| Order details | The July 3 gap analysis accepted workbench-only capture; the product story requires order-entry capture for routed culture tests. | Culture-routed tests reveal the microbiology fields in order entry and submit them through the existing sample-entry service path. Non-culture tests do not reveal them. | Resolved |
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

The combined live checklist now contains M1-M4 stories. Human acceptance remains
open and must be recorded against the exact deployed application SHA and the
matching live checklist revision.

## No-Migration Ruling

The July 28 story-closure work adds no data-model change. Order-entry details
reuse the existing microbiology order-detail model, case context is compiled
from existing sample/patient services, explicit setup values are retained in
the existing activity record, and report links use existing projected Result
identifiers. No Liquibase changeset is warranted for this slice.
