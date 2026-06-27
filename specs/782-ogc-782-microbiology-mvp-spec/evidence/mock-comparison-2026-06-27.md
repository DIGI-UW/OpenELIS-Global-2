# OGC-782 MVP Evidence vs OpenELIS-Work Mock Comparison

Compared on 2026-06-27.

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

The MVP now has design parity for the core case-workbench interaction shape:
case progress rail, next-step callout, case-info strip, highlighted current
work card, separate timeline, isolate card, AST card, critical-communication
card, and report/WHONET readiness card. The happy-path behavior still passes in
Playwright after the design pass.

The remaining differences are feature-depth gaps rather than raw layout gaps:
the mock includes richer order context, media/subculture tracking, full isolate
identification, table-level AST metadata, expert-review queues, and WHONET
mapping/export screens that are outside this MVP slice.

## Comparison By Area

| Area | Mock intent | Current evidence | Review call |
|---|---|---|---|
| Guided workflow shell | Persistent left walkthrough index, branch toggle, step number, Prev/Next, and current-step context. | Actual uses the normal OpenELIS shell plus an in-page case progress rail. | Accept for product MVP; do not build the prototype walkthrough chrome into OpenELIS. |
| Case progress | Case-progress rail with done/current/to-do states and next-step callout. | Actual now has the progress rail, next-step callout, and stage tag. | Parity achieved for workflow orientation. |
| Case workbench layout | Compact cards for inoculation, timeline, isolates, AST results, reports. | Actual now separates current inoculation/setup, timeline, isolates, AST, critical communication, and report readiness into cards. | Parity achieved for core card layout. |
| Timeline | Structured activity log with timestamps, badges, actor, notes. | Actual uses structured activity rows with timestamps when available. | MVP-level parity; actor/system attribution remains future polish. |
| Isolate work-up | Rich isolate editing surface with Gram stain, colony morphology, preliminary/final ID, confidence, significance, and notes. | Actual creates/displays one isolate with label, preliminary organism, and significance. | Feature-depth gap outside this MVP slice. |
| AST entry | Inline AST run table with method, reagent lot, breakpoint standard, per-drug MIC/interpretation/source/override rows. | Actual now shows a result table for the recorded reading and inline override controls, but only one reading path is exercised. | MVP-level parity; reagent lot/breakpoint metadata is future scope. |
| Expert review | Separate inline expert-review queue with open/resolved flags and review decisions. | Actual has AST review status and final-release readiness, not a separate expert-review queue. | Feature-depth gap relative to M-06 mock. |
| Critical notification | Inline critical notification card with open/ack/follow-up states and alert-path reuse. | Actual has a compact critical-communication card and the component test proves log/ack behavior; the all-up video still focuses on the release path. | UI parity for card shape; evidence gap for all-up video coverage. |
| Report readiness | Reports card with explicit readiness checklist and preliminary/final release buttons. | Actual now has final-release and WHONET readiness checklist cards and hides the release action after final release. | Parity achieved for MVP final-release readiness. |
| WHONET readiness/export | Mapping dashboard with percentage coverage, suggestions, per-vocabulary bars, and three-click export path. | Actual shows WHONET readiness/blockers only. | Feature-depth gap; mapping/export dashboard is future scope. |
| Evidence quality | Mock screenshots are focused viewport-level states. | Actual screenshots are recaptured after the design pass. Full-page shots still show fixed shell artifacts in tall captures, so the side-by-side crop is preferred for parity review. | Accept with side-by-side evidence; avoid using only video for visual review. |

## Specific Actual Evidence Issues

- Tall full-page screenshots can include fixed OpenELIS shell elements in the
  middle of the stitched image. The preferred design-review artifact is the
  first-viewport side-by-side comparison listed below.
- The M7 happy-path screenshot set does not show the M6 worklist/critical
  communication flow; that evidence exists separately but is not visible in the
  all-up MVP video.
- The WHONET proof is only a blocker notification, not a readiness/mapping
  dashboard like the mock.

## Recommended Follow-Up

1. Keep PR #3789 as the MVP implementation slice with core workbench design
   parity.
2. Add a follow-up AST UI milestone for multi-row AST entry, reagent lot,
   breakpoint standard, and inline expert review.
3. Add a follow-up surveillance milestone for WHONET mapping readiness and
   export preview UI.
4. Re-record a stakeholder-facing guided video if Casey needs a walkthrough
   rather than raw Playwright evidence.

## Captured Comparison Aids

- Actual contact sheet:
  `/tmp/ogc-782-mock-compare/actual-contact-sheet.png`
- Mock contact sheet:
  `/tmp/ogc-782-mock-compare/mock-contact-sheet.png`
- Design-parity case workbench comparison:
  `/tmp/ogc-782-mock-compare/design-parity-case-workbench-2026-06-27.png`
