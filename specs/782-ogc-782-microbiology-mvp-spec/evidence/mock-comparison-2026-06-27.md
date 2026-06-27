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

The MVP evidence proves the happy-path behavior, but it does not visually match
the OpenELIS-work guided workflow mocks. The implementation is currently a
functional vertical workbench proof, while the mocks describe a guided,
step-aware microbiology workflow shell with richer workbench, AST, expert-review,
critical-notification, and WHONET mapping surfaces.

This is acceptable as a technical MVP checkpoint only if the PR is presented as
behavioral proof, not mock-fidelity proof.

## Comparison By Area

| Area | Mock intent | Current evidence | Review call |
|---|---|---|---|
| Guided workflow shell | Persistent left walkthrough index, branch toggle, step number, Prev/Next, and current-step context. | Actual screenshots show normal OpenELIS header and a single vertical page. No guided shell or step navigation. | Product/UX gap if the deliverable is expected to resemble the mock. |
| Case progress | Case-progress rail with done/current/to-do states and next-step callout. | Actual case stage is a single tag plus timeline text. | Workflow clarity gap. |
| Case workbench layout | Compact cards for inoculation, timeline, isolates, AST results, reports. | Actual UI stacks large full-width forms and text lists. | Visual/ergonomic gap; behavior is present but hard to scan. |
| Timeline | Structured activity log with timestamps, badges, actor, notes. | Actual timeline is a plain bold text list. | Evidence is hard to read; not mock-aligned. |
| Isolate work-up | Rich isolate editing surface with Gram stain, colony morphology, preliminary/final ID, confidence, significance, and notes. | Actual creates/display one isolate with label, preliminary organism, and significance. | MVP behavior present; mock-level isolate work-up is not implemented. |
| AST entry | Inline AST run table with method, reagent lot, breakpoint standard, per-drug MIC/interpretation/source/override rows. | Actual supports one antibiotic reading and override in a simple form. | Functional MVP only; major mock fidelity gap. |
| Expert review | Separate inline expert-review queue with open/resolved flags and review decisions. | Actual has AST review button/status and final-release readiness. | Feature gap relative to M-06 mock. |
| Critical notification | Inline critical notification card with open/ack/follow-up states and alert-path reuse. | M7 happy-path screenshots show the logging form but do not prove logged/acknowledged critical notification. M6 has separate foundational evidence. | Evidence gap for the all-up MVP story; feature exists but not shown in M7 video/screenshots. |
| Report readiness | Reports card with explicit readiness checklist and preliminary/final release buttons. | Actual has two InlineNotifications and a final release button; after final release the button still appears enabled. | Functional but not mock-aligned; possible UX bug after final release. |
| WHONET readiness/export | Mapping dashboard with percentage coverage, suggestions, per-vocabulary bars, and three-click export path. | Actual shows `WHONET export blocked ORGANISM_MAPPING_REQUIRED` only. | M7 implements readiness status, not the WHONET mapping/export UI in the mock. |
| Evidence quality | Mock screenshots are focused viewport-level states. | Actual screenshots are very tall full-page captures; sticky header appears mid-page in AST/final screenshots. Video is hard to follow. | Evidence should be recaptured as focused panel screenshots or a narrated/stepped video before stakeholder review. |

## Specific Actual Evidence Issues

- `ogc-782-ast-reviewed-ready.png` and `ogc-782-final-released.png` include the
  sticky OpenELIS header in the middle of the full-page screenshot, making the
  screenshot look broken even though the test passed.
- `ogc-782-final-released.png` shows `FINAL_RELEASED`, but the `Release final
  report` button is still visible and blue. That should be reviewed: either make
  release idempotent with a post-release disabled state, or hide/change the
  action after final release.
- The M7 happy-path screenshot set does not show the M6 worklist/critical
  communication flow; that evidence exists separately but is not visible in the
  all-up MVP video.
- The WHONET proof is only a blocker notification, not a readiness/mapping
  dashboard like the mock.

## Recommended Follow-Up

1. Keep PR #3789 as the backend/API/readiness MVP slice if reviewers understand
   it is not a UI fidelity pass.
2. Add a follow-up UI milestone for mock-aligned case workbench ergonomics:
   progress rail, compact cards, readiness checklist, and post-release action
   state.
3. Add a follow-up AST UI milestone for table-based AST entry and inline expert
   review.
4. Add a follow-up surveillance milestone for WHONET mapping readiness and
   export preview UI.
5. Re-record evidence with focused screenshots per section and a short guided
   video that pauses at workbench, isolate, AST, review, final release, critical
   communication, and WHONET readiness.

## Captured Comparison Aids

- Actual contact sheet:
  `/tmp/ogc-782-mock-compare/actual-contact-sheet.png`
- Mock contact sheet:
  `/tmp/ogc-782-mock-compare/mock-contact-sheet.png`
