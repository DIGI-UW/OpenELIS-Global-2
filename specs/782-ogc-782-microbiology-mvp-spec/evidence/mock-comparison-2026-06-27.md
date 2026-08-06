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

M-03 entered remediation on 2026-08-05. As of 2026-08-06, the supported order
state preserves workflow/Method metadata, shares the ruled Carbon controls,
persists a Sample-owned draft across collection, and passes configured-route
browser evidence for one-case culture routing, non-culture exclusion, guarded
discard, and bacteriology/TB siblings. Patient Origin still uses hardcoded
client choices rather than deployment reference data, macro-enabled history
depends on the separate Macro Library, and visual/deployed/human acceptance
remain open. M-04, M-05, M-07,
and M-12 are useful functional cores rather than authoritative parity. Missing
behavior includes classification, subculture lineage, complete two-pass isolate
work, panel and breakpoint provenance, analyzer/QC review, Culture/AST worklist
grains, resistance/recent-activity context, and consistent reagent-lot rules.

## Comparison By Area

| Area                     | Mock intent                                                                                                                                          | Current evidence                                                                                                                                                                                                                                                                                                                             | Review call                                                                                                                                              |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| M-03 Program/order entry | Culture-test selection visibly derives Program = Microbiology and presents required/defaulted Culture Method plus complete microbiology details.     | R1 preserves direct/panel metadata, derives and locks typed culture Program state, shares the ruled controls, persists typed details across collection, confirms discard, and passes the four supported-route stories. Patient Origin reference/default behavior, Macro consumption, visual evidence, deployment, and human UAT remain open. | Runtime behavior is aligned except the named reference/macro gaps; authoritative visual and human acceptance are still incomplete.                       |
| Guided workflow shell    | Persistent left walkthrough index, branch toggle, step number, Prev/Next, and current-step context.                                                  | Actual uses the normal OpenELIS shell plus an in-page case progress rail.                                                                                                                                                                                                                                                                    | Accept for product MVP; do not build the prototype walkthrough chrome into OpenELIS.                                                                     |
| Case progress            | Case-progress rail with done/current/to-do states and next-step callout.                                                                             | Actual has the rail, next-step callout, and stage tag, but does not focus/hold actions consistently from the current classified step.                                                                                                                                                                                                        | Partial alignment.                                                                                                                                       |
| Case workbench layout    | Compact workflow surfaces for inoculation, subculture/timeline, isolates, AST results, reports, and related actions.                                 | Actual separates generic setup, timeline, isolates, AST, critical communication, and report readiness, but lacks dedicated subculture lineage, sibling links, Change Workflow, and NCE/lost actions.                                                                                                                                         | Functional shell, materially incomplete.                                                                                                                 |
| Timeline                 | Structured automatic activity history plus deliberate notes/observations with timestamps, actor, and context.                                        | Actual uses structured activity rows, but generic manual activity can substitute for missing domain actions and some actor/system attribution remains incomplete.                                                                                                                                                                            | Partial alignment; reuse domain audit/history rather than extend generic entry.                                                                          |
| Isolate work-up          | Two-pass isolate work with Gram stain, colony morphology, preliminary/final ID, method, confidence, significance, and notes.                         | R1 records Gram stain and morphology first, presents pending/identified states, captures method/confidence/significance, gates AST until confirmed identification, and projects preliminary work-up into reporting.                                                                                                                          | Behavioral alignment implemented at `14164589f`; exact-SHA browser execution and desktop/mobile visual comparison remain open.                           |
| AST entry                | Inline AST run table with ordered panel provenance, method, reagent lot, standard/version, matched basis, readings, overrides, and review lifecycle. | Actual shows multiple readings, standard selection, inline override controls, review locking, and readiness feedback, but lacks complete provenance, matched-level evidence, revert history, analyzer/QC states, and scoped repeat.                                                                                                          | Partial M-05 alignment; complete in R1.                                                                                                                  |
| Expert review            | Separate inline expert-review queue with open/resolved flags and review decisions.                                                                   | Actual has AST review status and final-release readiness, not a separate expert-review queue.                                                                                                                                                                                                                                                | Feature-depth gap relative to M-06 mock.                                                                                                                 |
| Critical notification    | Inline critical notification card with open/ack/follow-up states and alert-path reuse.                                                               | The canonical browser flow exercises a Result-target communication through open, acknowledged, and closed states after preliminary report projection.                                                                                                                                                                                        | Behavioral proof is present; refreshed all-up video remains pending.                                                                                     |
| Report readiness         | Reports card with explicit readiness checklist and preliminary/final release buttons.                                                                | Actual now has final-release and WHONET readiness checklist cards and hides the release action after final release.                                                                                                                                                                                                                          | Parity achieved for MVP final-release readiness.                                                                                                         |
| WHONET readiness/export  | Mapping dashboard with percentage coverage, suggestions, per-vocabulary bars, and three-click export path.                                           | M3 provides organism/antibiotic administration. M4 provides a compact Carbon Configure/Preview/Generate flow, selected-set counts, mapping warnings, direct repair, audited manual CSV, and stable desktop/mobile layouts.                                                                                                                   | Manual-export interaction parity is achieved; auto-map, broad vocabulary coverage, scheduling, profile packaging, and FHIR delivery remain future scope. |
| Evidence quality         | Mock screenshots are focused viewport-level states.                                                                                                  | Case screenshots are ordered viewport/card shots, but the original evidence set omitted M-03 and did not fully compare M-04/M-05/M-07/M-12 behavior on the supported routes.                                                                                                                                                                 | Incomplete; R1 must add stable desktop/mobile comparisons for every remediated source slice.                                                             |

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
2. Complete the single R1 remediation PR across M-03, M-04, M-05, M-07, M-12,
   and applicable M-NFR outcomes before claiming authoritative parity.
3. Keep Macro Library in its own feature/spec/PR/UAT stack and review
   deployment; add microbiology consumption only after that feature is accepted.
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
