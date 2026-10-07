# AMR

Engineering boundary for the AMR feature. This replaces the previous specification
in place; a Jira key is neither its identity nor its delivery boundary.

**Source baseline:** 2026-10-05; Microbiology v2 draft 10.4, patient report v2.4.3,
clinical order entry v0.20, environmental microbiology v0.9.

## Authority

- `openelis-work` defines functional requirements, workflows, mocks and observable
  acceptance. `specs/amr` owns engineering decisions, migration/removal mapping,
  implementation order and verification. Jira tracks ownership, status,
  dependencies, acceptance evidence and links; it is not a functional specification.
- Design and Jira do not define entities, persistence, API shapes, routes, process
  ownership, migrations or test-layer choices.
- Technical prescriptions in those sources are non-normative, including proposed
  retained legacy storage. Engineering decisions belong in this repository,
  grounded in the application and its component ownership.
- Do not reproduce Casey's FRS, mocks, stories or field dictionaries. Reference
  the relevant requirement and mock directly for each iteration.
- This file owns the boundary; `plan.md` owns engineering rules and exact checks;
  `tasks.md` is the only execution roadmap. No second status ledger.
- Resolve functional contradictions with product before implementing the affected
  slice. A source's schema suggestion is not a functional requirement.

## Product and design inputs

| Input                 | Direct reference                                                                                                                                                                    | Use                                                               |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| AMR functionality     | [V2 amendments](https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md)                             | Changed behavior and product acceptance                           |
| AMR interactions      | [Case and Worklist mock](https://digi-uw.github.io/openelis-work/#/microbiology/microbiology-v2-amendments)                                                                         | Blood-culture/TB examples and visual comparison                   |
| Preservation          | [A-16 checklist](https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#a-16-preservation-boundary) | Check against code; not proof that every listed capability exists |
| Reception             | [Clinical order entry](https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/sample-collection/clinical-order-entry-v4.md)                 | Case-opening behavior and ordinary test selection                 |
| Printed output        | [Patient report micro block](https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/reports/patient-report-redesign.md)                     | Groups, susceptibility blocks, provenance and releases            |
| Environmental subject | [Environmental microbiology](https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/m-18-environmental-microbiology.md)        | Site-based cases grouped by order, sample type, lab unit and site |
| Shared Inventory      | [Inventory design](https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/inventory/inventory-redesign.md)                                  | Tracking behavior, not a storage contract                         |
| Scope exclusions      | [Later/out of scope](https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#later-not-in-v2)        | Do not silently expand this feature                               |

## Agreed V2 scope

Draft 10.4 is the baseline with the following settled corrections. Eligible
catalog tests open cases independently of Program. Automatic grouping, adding
tests to an existing case, blood-culture sets and splitting a member sample with
no results remain in scope. Joining existing cases is deferred. A lab-unit
transfer preserves separate case identities even when both cases now have the
same order, sample type and working unit; it never merges their work or history.
Environmental grouping also distinguishes the sampling site.

Use the draft's Results and Validation rights, including technician placement,
report choices and partial release, validator final release and amendments,
worklist scoping and read-only direct links. Admission date remains optional;
missing admission information must not fabricate infection origin. Patient
reporting choices and surveillance eligibility are separate. Offline writes are
blocked. Future antibiogram, GLASS and cluster delivery is outside this cleanup.

This change updates documentation, gallery registration and gallery tests only.
It changes no application interface, database schema or clinical record.

## Clean cutover

1. One V2 runtime and persistence model: no V1 fallback, dual-write, parallel
   workflow, read-only legacy master or compatibility flag.
2. Delete retired workflow/protocol consumers, configuration, UI, seed paths and
   obsolete test expectations with the slice removing their use.
3. Transform required clinical records, report versions and attributable audit
   history into V2 structures. Git preserves source, not clinical data.
4. Preserve applied Liquibase history; append tested cutover changesets. Remove
   obsolete storage from the final schema, never by editing applied changesets.
5. A disposable upgrade fixture is allowed; it must not become a production
   reader, copied legacy model or second configuration authority.
6. Reuse existing microbiology and shared infrastructure only where it serves V2.
   Existing storage creates no preservation obligation.
7. Bridge owns analyzer parsing, transport and runtime configuration. AMR consumes
   normalized traffic, without an app-side poller or vendor-specific execution.
8. Keep maintained AMR documentation here. Delete superseded ledgers/contracts
   rather than archiving competing instructions alongside this specification.

## Acceptance boundary

Implementation advances when the slice's source-linked user outcome, engineering
invariants, negative/security cases and focused gate pass on the same revision,
and its committed work is published in a reviewable stacked PR. Pending full CI
or CI-only failures do not block the next implementation slice. Record them for
final stabilization; a failing functional dependency still blocks dependent work.
Full CI, review and release acceptance must close before merge/deployment. UI proof
shows persisted behavior after reload, not merely a successful request. Compare
with the named mock section and record deliberate design deviations for review.
Every user-facing slice supplies a video of the implemented workflow, including
save and reload, and a written comparison against the pinned V2 mock. Record the
application commit, mock commit, scenario/criterion IDs, viewport, video timestamps
and any mismatch. A video link without that comparison is incomplete evidence.
Migration-only slices record database preservation/rollback evidence instead;
they do not claim user-interface acceptance.

Implementation starts only after the source/cutover gate closes. This document
is not a claim that V2 is implemented, deployed or accepted.
