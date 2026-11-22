# M9 implementation and candidate validation

## Candidate and decision

The implementation on `codex/amr-v2-ux-validation` addresses UX-01–09 from the
[original audit](milestone-9-validation.md). That audit and its screenshots remain
unchanged. This report belongs to the implementation commit containing it, above
M8 `9471be12ba997998bd3ddf64db69551b6e8f8aa0`, in native `gh stack` #4674 / PR #4734.

Focused local validation supports review of the implementation. Full local CI,
GitHub's required checkpoints, and owner workflow acceptance are separate gates;
this report does not certify those gates or authorize UAT recording.

## Finding-to-change evidence

| Finding | Implemented behavior                                                                                                                                                                                                                                                                | Regression evidence                                                                                                                                                                                                                                                              |
| ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| UX-01   | Clinical fields and configured Program questions are available in Case info. Release blockers open and focus that section, including when collapsed. Admission date remains optional.                                                                                               | `microbiology-ux-context.spec.ts` starts with missing patient origin and conditional required questions, saves/reloads answers, clears blockers and releases. Questionnaire tests cover false answers, stale revisions, changed definitions and shared-order write restrictions. |
| UX-02   | Every case entry uses `CaseViewShell`; the old alternate composition is removed. Legacy section links select the same workspace. Header separates work stage and release state. Final cases expose reasoned amendments. Critical communication and nonconformance remain available. | Case access, workbench, amendment, reporting and label-scan browser journeys; shared-view component tests.                                                                                                                                                                       |
| UX-03   | Multiple selected lab units survive case/bench/AST navigation, back and reload. Inapplicable filters are visibly cleared. Server searches intersect selected units with permitted units.                                                                                            | Two-unit persisted browser journey, route tests and `MicroCaseWorkspaceIntegrationTest`.                                                                                                                                                                                         |
| UX-04   | Waiting incoming count/action appears above Case info regardless of selected section and refreshes after placement.                                                                                                                                                                 | Automation browser journey verifies the count before opening Incoming, its disappearance after placement, the additional result and reload.                                                                                                                                      |
| UX-05   | Header includes reporting track, priority and work/release state. Samples include body site, container, collector and laboratory collection time, with a correction link.                                                                                                           | Four-bottle database test verifies distinct site/container/collector/time across midnight, correction and transfer. Shell and browser assertions cover visible context.                                                                                                          |
| UX-06   | Instant formatting and editing use the laboratory time zone. Referral summary uses the actual handoff timestamp, leaving an undispatched referral blank.                                                                                                                            | Clock tests, referral database round trip with millisecond precision and browser referral journey.                                                                                                                                                                               |
| UX-07   | Media are searchable with tracking labels; extensions preview the resulting end without resetting inoculation; positivity has a readable duration and lab-time preview.                                                                                                             | Culture component tests and persisted inoculation/reading/extension/subculture/Gram-stain browser journey. Existing service tests retain time and lot validation.                                                                                                                |
| UX-08   | History resolves actor names. Case, culture, isolate, initial-result and drug notes expose type/count; attached notes persist in existing append-only case activity storage. External notes follow the selected result into preview/PDF; internal notes are excluded.               | Database flush/clear note test, ownership tests, saved/reloaded browser notes and rendered patient report.                                                                                                                                                                       |
| UX-09   | AST actions explain specific server blockers and clear stale errors when another action opens. Lot/instrument selection and single-agent repeat/retest remain available in the canonical workspace.                                                                                 | AST component error/reset and repeat-payload tests; persisted reagent-lot, selected-retest and immutable report browser journeys.                                                                                                                                                |

No schema changes or concept seeds are introduced. Scenario preparation uses the
property-gated application service and configured vocabulary. The surveillance
scenario assigns its Program the configured Bacterial reporting track and uses
V2 Diagnostic/Screening/Survey purposes.

## Local validation

Run from this checkout with `scripts/dev-stack` at `https://localhost:38220`.
The rebuilt application uses the selected Docker context, trusted local TLS and
laboratory zone `Africa/Nairobi`. Browser data is synthetic and owned by each test.

- **245 frontend tests passed in 35 files**, covering microbiology and laboratory
  clock behavior. Command: `npx vitest run src/components/microbiology
src/components/utils/labClock.test.js --reporter=dot` from `frontend/`.
- **186 focused backend tests passed in 22 suites**. Counts and exact suite names
  are retained in [backend-results.json](evidence/m9/2026-10-14-candidate/backend-results.json).
  Executed in small batches using `scripts/run-java21 mvn -q -Dtest=<suite list>
-Dspotless.skip=true test`, with `DOCKER_HOST` from the selected Docker context.
  These include order routing/save, case membership and transfers, result access
  and self-validation, culture/isolate persistence, incoming instrument adapters,
  AST/DST, reporting, environmental configuration and migration rollback.
- Browser suites were run in batches of at most nine tests with
  `scripts/dev-stack playwright <files> --project=core-app --no-deps --reporter=line`,
  after the setup project's login. Successful journeys include case access,
  initial results/external provenance, cultures, two-unit navigation,
  identification/referral, incoming placement, bench/scan, AST lots and repeat,
  partial/final/amended reporting, environmental certificates, surveillance
  population selection, worklist keyboard/mobile behavior and missing context.
  [Evidence manifest](evidence/m9/2026-10-14-candidate/README.md) records retained
  screenshots and reports. Earlier failures were corrected and rerun: obsolete
  alternate-view selectors, obsolete unspecified-purpose controls, missing
  scenario reporting track, attached-note subject length and responsive overflow.
- Scoped Java formatting, frontend formatting, Playwright lint and whitespace
  validation are required before commit. Repository-wide TypeScript checking is
  not a passing gate: it reports missing Node/test globals and existing source
  typing errors. The preparation revision reproduces 2,568 diagnostics; the candidate has
  the same underlying failures plus the new test file encountering that missing
  Node typing. Both outputs are retained locally. This is not a successful check.

## Product comparison

Compared with openelis-work
`c9722f07304c3170913d565876a6dd085c388a7d`, including the committed
[desktop reference](evidence/m9/2026-10-14/14-reference-desktop.jpg).
Candidate viewport sizes are 1440 × 1000 and 390 × 1000. Full-page captures are
longer than those viewports; viewport captures are also retained.

The candidate follows the reference's compact identity/owning-unit summary,
prominent incoming action, collapsible Case info, shared testing chooser, row
notes and separate release/amendment state. It retains the host application's
navigation and Carbon field layouts. Wide laboratory tables scroll within their
containers rather than widening the page. The written specification says admission
date is optional; the preview's required marker is deliberately not a release rule.

The rendered patient PDF shows the external culture note directly below its culture
finding and excludes the internal note. The environmental certificate is separately
downloaded and checked for site, accession, culture and outcome content. Screenshots
are review evidence, not an assertion of pixel identity or a complete accessibility
audit.

## Remaining acceptance scope

The implementation is ready for review; the following are not implied by focused
passing tests:

- Full local CI and all four required GitHub checkpoints on the published candidate.
- An owner-led, ordinary-technician and supervisor browser walkthrough. Current
  browser setup uses the local development administrator; server/component tests
  exercise restricted rights and self-validation separately.
- End-to-end external instrument transport through returned referral results,
  cross-case/post-final placement, TB/NTM interpretation and late-growth amendment
  as one assembled laboratory journey. Focused service tests cover boundaries;
  they do not replace that workflow acceptance.
- A production-volume migration rehearsal. Fresh/reload configuration and migration
  rollback tests pass; they do not establish production operational readiness.
- Program questionnaires with repeated groups or repeated parent questions are
  explicitly prevented from saving through the compact editor, avoiding loss of
  repeated responses. Unsupported complex answer types retain their loaded data
  and show a warning; no claim of a complete general FHIR form editor is made.

After reviewing this candidate and closing the applicable acceptance lanes, obtain
owner approval before updating accepted UAT setup or recording workflows.
