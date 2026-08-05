# OGC-782 MVP Evidence vs OpenELIS-Work Mock Comparison

Compared on 2026-06-27.

**2026-08-05 correction:** The original comparison omitted M-03, the first
order-entry screen in the authoritative walkthrough. The source-pinned browser
audit in
[`openelis-work-authoritative-alignment-2026-08-05.md`](./openelis-work-authoritative-alignment-2026-08-05.md)
supersedes aggregate parity claims in this artifact.

## Sources

- Actual MVP screenshots:
  `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/e2e-evidence/`
- Actual MVP video:
  `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/test-results/demo-core-ogc-782-microbio-3f6cc-ual-AST-override-and-review-core-demo-video/video.webm`
- Mock gallery route:
  `https://digi-uw.github.io/openelis-work/#/microbiology/microbiology-guided-workflow-walkthrough`
- Full mock walkthrough route:
  `https://digi-uw.github.io/openelis-work/designs/microbiology/amr-micro-workflow-flow.html`
- Captured mock screenshots:
  `/tmp/ogc-782-mock-compare/steps/`

## Executive Finding

The MVP has useful alignment for the core case-workbench interaction shape:
case progress rail, next-step callout, case-info strip, highlighted current
work card, separate timeline, isolate card, AST card, critical-communication
card, and report/WHONET readiness card. This does not establish parity for the
complete microbiology workflow.

M-03 has a blocking workflow gap: configured Add Order uses `/order/enter`, but
the partial microbiology details implementation and its Playwright test use
legacy `/SamplePatientEntry`. Program derivation, Culture Method, several
control semantics/defaults, discard confirmation, and the visible save-to-case
round trip are absent or unproven. M-07 is functional but materially thinner
than the authoritative worklist. Other remaining differences include richer
media/subculture tracking, isolate identification, table-level AST metadata,
expert review, and later reporting/surveillance modules.

## Comparison By Area

| Area                    | Mock intent                                                                                                                 | Current evidence                                                                                                                                                                    | Review call                                                                          |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| M-03 Program/order entry | Culture-test selection visibly derives Program = Microbiology and presents required/defaulted Culture Method plus complete microbiology details. | Current `/order/enter` drops culture workflow metadata and shows no microbiology Program/details. A partial, control-inaccurate section exists on legacy `/SamplePatientEntry`. | Blocking remediation; prior label-only Playwright/UAT evidence is insufficient. |
| Guided workflow shell   | Persistent left walkthrough index, branch toggle, step number, Prev/Next, and current-step context.                         | Actual uses the normal OpenELIS shell plus an in-page case progress rail.                                                                                                           | Accept for product MVP; do not build the prototype walkthrough chrome into OpenELIS. |
| Case progress           | Case-progress rail with done/current/to-do states and next-step callout.                                                    | Actual now has the progress rail, next-step callout, and stage tag.                                                                                                                 | Parity achieved for workflow orientation.                                            |
| Case workbench layout   | Compact cards for inoculation, timeline, isolates, AST results, reports.                                                    | Actual now separates current inoculation/setup, timeline, isolates, AST, critical communication, and report readiness into cards.                                                   | Parity achieved for core card layout.                                                |
| Timeline                | Structured activity log with timestamps, badges, actor, notes.                                                              | Actual uses structured activity rows with timestamps when available.                                                                                                                | MVP-level parity; actor/system attribution remains future polish.                    |
| Isolate work-up         | Rich isolate editing surface with Gram stain, colony morphology, preliminary/final ID, confidence, significance, and notes. | Actual creates/displays one isolate with label, preliminary organism, and significance.                                                                                             | Feature-depth gap outside this MVP slice.                                            |
| AST entry               | Inline AST run table with method, reagent lot, breakpoint standard, per-drug MIC/interpretation/source/override rows.       | Actual shows both seeded readings, explicit breakpoint-standard selection, inline override controls, reviewed-state locking, and release-readiness feedback. | MVP-level parity; reagent lot and richer multi-row run metadata are V2.              |
| Expert review           | Separate inline expert-review queue with open/resolved flags and review decisions.                                          | Actual has AST review status and final-release readiness, not a separate expert-review queue.                                                                                       | Feature-depth gap relative to M-06 mock.                                             |
| Critical notification   | Inline critical notification card with open/ack/follow-up states and alert-path reuse.                                      | The canonical browser flow exercises a Result-target communication through open, acknowledged, and closed states after preliminary report projection.                                | Behavioral proof is present; refreshed all-up video remains pending.                 |
| Report readiness        | Reports card with explicit readiness checklist and preliminary/final release buttons.                                       | Actual now has final-release and WHONET readiness checklist cards and hides the release action after final release.                                                                 | Parity achieved for MVP final-release readiness.                                     |
| WHONET readiness/export | Mapping dashboard with percentage coverage, suggestions, per-vocabulary bars, and three-click export path.                  | M3 provides organism/antibiotic administration. M4 provides a compact Carbon Configure/Preview/Generate flow, selected-set counts, mapping warnings, direct repair, audited manual CSV, and stable desktop/mobile layouts. | Manual-export interaction parity is achieved; auto-map, broad vocabulary coverage, scheduling, profile packaging, and FHIR delivery remain future scope. |
| Evidence quality        | Mock screenshots are focused viewport-level states.                                                                         | Case screenshots are ordered viewport/card shots, but the original evidence set omitted M-03 and did not compare the supported Add Order route. | Incomplete; R1 must add desktop/mobile M-03 and refreshed M-07 comparisons. |

## Specific Actual Evidence Issues

- Tall full-page screenshots can include fixed OpenELIS shell elements in the
  middle of stitched images. The refreshed evidence set avoids those captures
  and uses ordered viewport/card screenshots instead.
- The committed M7 media predates the expanded all-up critical lifecycle and
  visible patient-report proof. A refreshed external screenshot/video bundle is
  required for final PR evidence.
- The M4 WHONET proof covers the routine manual export and exact organism repair
  path. It intentionally does not reproduce the mock's unimplemented auto-map,
  scheduling, FHIR, or broad mapping-dashboard controls.

## Recommended Follow-Up

1. Keep PR #3789 described as the initial routine-bacteriology implementation,
   not complete source parity.
2. Complete R1 M-03 remediation on the supported Add Order workflow before
   accepting order routing.
3. Add a follow-up AST UI milestone for multi-row AST entry, reagent lot,
   breakpoint standard, and inline expert review.
4. Complete the remaining M-09 scope only after its format, vocabulary,
   scheduling, delivery, and standards-policy decisions are authoritative.
5. Re-record a stakeholder-facing guided video if Casey needs a walkthrough
   rather than raw Playwright evidence.

## Captured Comparison Aids

- Actual contact sheet:
  `/tmp/ogc-782-mock-compare/actual-contact-sheet.png`
- Mock contact sheet:
  `/tmp/ogc-782-mock-compare/mock-contact-sheet.png`
- Design-parity case workbench comparison:
  `/tmp/ogc-782-mock-compare/design-parity-case-workbench-2026-06-27.png`

## Worklist Follow-Up

The M-07 worklist was re-reviewed on 2026-07-28 after the responsive worklist
remediation. See
[worklist-ux-follow-up-2026-07-28.md](./worklist-ux-follow-up-2026-07-28.md)
for verified desktop/mobile evidence, resolved layout defects, and the
product-versus-implementation scope calls for the remaining M-07 differences.
