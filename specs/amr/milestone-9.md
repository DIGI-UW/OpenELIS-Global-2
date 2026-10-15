# Milestone 9 — Workflow remediation and integrated acceptance

**Status:** Planned; audit complete, fixes not started.
**Branch:** `codex/amr-v2-ux-validation`, above
`codex/amr-v2-environmental-migration` in native `gh stack` #4674.
**Baseline:** [Audit and reproduction details](milestone-9-validation.md), recorded
against `9471be12ba997998bd3ddf64db69551b6e8f8aa0`.

## Outcome and boundaries

A technician can enter the same case from every supported entry point, complete
required information, perform and review testing, release a report, and make a
reasoned amendment through one coherent workspace. A supervisor can verify the
journey against the pinned product design and the saved data.

M9 resolves UX-01–09 and completes the audit's missing acceptance evidence. It
preserves working M2–M8 behavior. Unexercised paths are coverage gaps until tested,
not presumed defects. This planning commit changes no application behavior.

Use [the repository specification](spec.md) for engineering invariants and the
[pinned product design](https://github.com/DIGI-UW/openelis-work/blob/c9722f07304c3170913d565876a6dd085c388a7d/designs/microbiology/amr-micro-v2-amendments.md)
for visible behavior and interaction. Admission date remains optional under the
written rule; the conflicting preview must not introduce a new release blocker.
Record that reference discrepancy in the comparison evidence.

## Implementation order

These are ordered steps within one milestone PR, each delivering a usable
workflow with its regression coverage. Start with step 1; do not build separate
backend-only and frontend-only phases.

| Step                                  | Findings                       | Deliverable                                                                                                                              | Acceptance                                                                                                                                                                                                                      |
| ------------------------------------- | ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1. Complete and release one case      | UX-01, UX-02                   | One canonical case workspace with clinical context, configured questions, linked release blockers, consistent state and amendment access | Begin with blank required context; save/reload it; clear the corresponding blockers; release then amend with a reason. Direct, worklist, bench, scanner and related-case links show the same persisted state.                   |
| 2. Find the right work                | UX-03, UX-04, UX-05            | Consistent scope/filter navigation, pending incoming count/action, complete sample and header context                                    | Two units with overlapping search matches retain the intended filter through scope changes, back and reload; waiting/placed/declined/post-final evidence is visible; four bottles retain distinct site/time/container identity. |
| 3. Trust timing and culture decisions | UX-06, UX-07                   | Explicit laboratory clock, accurate referral handoff summary, culture previews and searchable tracked media                              | Cross-midnight browser/lab zones retain date/minute precision; previewed incubation end equals saved end without resetting inoculation; positivity is readable; lot eligibility still blocks invalid saves.                     |
| 4. Understand history and failures    | UX-08, UX-09                   | Display names, note types/counts at supported attachments, specific validation errors scoped to each action                              | Authors are recognizable; internal notes stay off reports, external notes accompany the intended result; unresolved AST validation explains the blocker; cancelling/opening another action clears the old error.                |
| 5. Validate the assembled journey     | All findings and coverage gaps | Before/after evidence, regression results and owner review package                                                                       | All nine findings have a demonstrated resolution; remaining lanes below have results, not assumptions; CI and owner acceptance are recorded independently.                                                                      |

## Starting implementation map

Paths below are repository-relative. Inspect existing contracts before extending
them; reuse the working services and panels rather than adding a third case UI.

- **Entry and context:** `frontend/src/pages/MicrobiologyPage.jsx` currently chooses
  between `CaseViewShell.jsx` and `MicrobiologyCaseView.jsx` using query parameters.
  Inventory both views' actions, then consolidate into a single composition in
  `frontend/src/components/microbiology/`. Reuse `OrderDetailPanel.jsx` and existing
  context/release services where their contracts fit. Old entry URLs must resolve
  to sections of the same composition. Remove the superseded route-dependent
  composition after parity is covered; preserve critical communication,
  nonconformance, notes, environmental context and amendment functions.
- **Work discovery:** `CultureWorklist.jsx`, the case and AST worklists, and
  `CaseTestingWorkspace.jsx`/`CaseIncomingWorkspace.jsx`. Normalize applicable URL
  filters; define how scope-specific filters are retained or visibly reset.
- **Clock and culture:** `CaseCultureWorkspace.jsx`, `CaseIsolateWorkspace.jsx`,
  `CaseTestingWorkspace.jsx`, and `MicroIsolateWorkspaceServiceImpl.java`. Verify
  the existing handoff storage contract before changing its response projection.
  Distinguish date-only values from instants; never repair dates by arbitrary
  offsets. Preserve the server's incubation calculations and final locks.
- **History and errors:** shared note behavior and `CaseAstWorkspace.jsx`; extend
  service response compilation if display names or actionable validation reasons
  are missing. Do not weaken validation to remove an error message.

First implementation checkpoint: a fresh clinical case can reach and save its
missing patient origin through the normal URL and release checklist, with a
focused test that fails on the audited revision. The same change must preserve
read-only direct-link access outside the user's permitted lab unit and deny writes.

## Regression and acceptance plan

Use property-gated application scenarios for test preparation, not SQL loaders.
Create owned data per test and scope reads to its identifiers. Pin clocks for
boundary assertions. Existing access, initial testing, culture, isolate,
automation, reporting, amendment, environmental and worklist Playwright suites
under `frontend/playwright/tests/foundational/` are starting points, not proof
that the currently missing browser journeys already pass.

Add regression tests where they demonstrate each failure and successful remedy.
Use component/service tests for filter, error, timing and permission boundaries;
use browser tests for the actual connected workflows. Run focused suites during
development, then the full local CI runner and GitHub checks for the final
candidate. Do not substitute mocked responses for persisted workflow evidence.

Complete these lanes and link the resulting evidence in a new candidate report:

- [ ] Normal order entry/routing, related cases, splits, transfers and four-bottle
      site/collection identity, including correction from the case.
- [ ] Restricted-role read/write behavior, read-only outside-unit direct links,
      self-validation blocking, required questions and release checklist navigation.
- [ ] Culture receipt/future-time validation, lot eligibility, due/final-read
      transitions, Gram stain result entry, instrument positive/negative proposals
      and explicit confirmation, and late-growth amendment.
- [ ] Received-isolate mismatch acknowledgment, referral dispatch and returned
      results; verify the actual handoff timestamp round trip.
- [ ] Incoming result transport and placement, cross-case moves, post-final
      handling, bulk bench actions, labels and scanning.
- [ ] AST interpretation with configured rules, quality control, repeat testing,
      tuberculosis classification/conflicts and NTM off-ramp; do not infer these
      from the audit's successful manual overrides.
- [ ] Partial/final/amended patient reports, note placement, environmental
      certificate browser download and rendered PDF contents, and surveillance
      export populations/content. A PDF signature or HTTP success is insufficient.
- [ ] Keyboard navigation and narrow/desktop layouts compared with pinned mocks,
      with viewport, reference revision and candidate revision recorded.
- [ ] Retain or obtain M8 fresh-install/upgrade/configuration-reload evidence for
      migration readiness; the browser audit did not establish this gate.

## Delivery and acceptance gates

- Keep Carbon components, externalized strings in `en.json`, service-owned
  transactions and response compilation, server access checks and final locks.
  Concepts/catalogs/defaults remain configuration-loader owned; Liquibase is
  limited to schema and historical record conversion if either is needed.
- Preserve the original [audit](milestone-9-validation.md) and
  [evidence](evidence/m9/2026-10-14/README.md). Add a new validation report for the
  fixed candidate mapping UX-01–09 to changes, regressions and rendered proof.
  Never relabel the baseline screenshots as fixed behavior.
- Use `scripts/dev-stack` for local validation and native `gh stack` to maintain,
  rebase and submit the existing branch above M8. Publish the implementation PR
  ready for review, not as a draft. Do not create a separate PR per audit finding.
- Before merge, record full local CI and all required GitHub checkpoints against
  the actual candidate. Run independent CI concurrently; no polling narration.
- Obtain owner workflow acceptance after reviewing remaining findings. Only then
  prepare the accepted UAT setup/checklist and record workflows. Neither a green
  build nor this documentation commit constitutes UAT approval.
