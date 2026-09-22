# Reporting review and merge stopping point

This delivery checkpoint packages the working Reporting UAT increment. It does
not replace the full MVP goal or its canonical mock. Non-Conformance remains
visible as unavailable and requires its outstanding date decision, implementation
and validation in a follow-up. Do not delay feedback on the working increment
until that follow-up is finished.

## Included behavior

- Sample & Testing: instance-aware fields, both CSV layouts, every repeated
  result, turnaround beside each test, saved shared reports and fresh dates.
- Referrals: the same builder, shared library, queue and CSV engine; independent
  returned results and pending referrals are preserved.
- Delivery: immutable requests, duplicate submission protection, queued
  cancellation, retry lineage, worker recovery, expiry and file cleanup.
- Navigation: a configured Carbon renderer for the main menu, shared typography
  with the separate administration renderer, reports hierarchy and routed drafts;
  database defaults plus instance configuration, sections/icons, and an editor
  that preserves controlled values. Administration's hardcoded entry list is
  still a separate legacy path; this checkpoint does not claim its replacement.
- Public UAT: stable synthetic fixtures, actual CSV comparisons, desktop and
  narrow-screen workflows, and deployment provenance.

## Packaging contract

Use the official `gh stack` workflow. Each pull request starts with one clean
snapshot commit and targets the preceding branch. Subsequent review repairs use
ordinary commits on the relevant PR; do not rewrite submitted history merely to
restore a single-commit count. Preserve the existing M1/M2
branches as the development and deployment evidence; do not rewrite their
history to manufacture the review stack. Start the new stack from refreshed
`develop` and preserve intervening upstream changes.

Use ten functional pull requests. The approximately 600-line guide applies to
real business logic, not raw diff size. Report application behavior separately
from markup, styles, declarations, runtime configuration, tests, synthetic
fixtures, test/qualification helpers and documentation. Keep tests and evidence
beside the behavior they validate; do not create test-only or styling-only PRs
merely to reduce a line count.

The decomposition is: contracts and CSV layouts; immutable job storage and audit;
Sample & Testing mapping; configured catalog and shared definitions; queued
delivery and recovery; Referrals; URL state and API bindings; the complete
mock-based presentation; connected reporting workflows; and configurable
application navigation. Navigation's model, editor, renderer, legacy-style
removal and UAT instance configuration form one dedicated PR.

Preserve the existing component files. Do not extract helpers or split the
supplied design merely to satisfy the raw line count. Intermediate foundations
are reviewed as parts of this complete checkpoint, not represented as
independently complete user workflows.

## Review-ready versus merge-ready

The latest verified public application at this checkpoint is
`8005e4cc0b2b05d054489730aef969027d773093`, deployment
`20260914T234348Z-8005e4cc0b2b`. Its backend, frontend and full E2E GitHub gates
pass. [Current public workflow evidence](https://reporting.catalyst.openelis-global.org/reporting-evidence/20260914-review-8005/)
contains three inspected HD recordings, actual CSVs and matching queue downloads,
plus desktop/narrow comparisons with the pinned mock. Five public workflows
passed across seven checks including two authentication setups. Earlier recovery
proof remains tied to `9baa356`; navigation/Dashboard repair recordings identify
their original local builds. Human acceptance remains pending.

Reporting UI follow-up `36eb98edda` adds a two-editor regression and corrects the
old conflict warning persisting after a successful copy. Its component regression
reproduced the defect before the one-line fix. All 31 reporting component checks,
compiled desktop/phone recovery with actual CSVs, both builds, formatting and
focused lint pass. Failed copying retains the entered name and selected fields.
The original saved definition and its independent copy retain their own columns.
This follow-up still needs assembled publication, public recording and fresh CI;
it is not yet part of the public `8005e4cc0b` result. The [code-QA record](code-qa.md)
separates current validation from full-MVP and human acceptance.

Before calling this checkpoint merge-ready:

- Build each changed layer and run its relevant tests; verify that its dependent
  code is present and no unrelated source or dependency pin changed.
- Validate the assembled stack on current develop. Repeat affected component and
  public/local browser checks for assembly and upstream overlap.
  Compare the rendered interface directly with the pinned mock at 1280 and 390.
- Capture or refresh implementation E2E video proof for the changed complete
  workflows, tied to original story IDs and the tested build. A passing test
  count or unverified video file is not a substitute for inspecting the proof.
- Complete code QA of each submitted diff and the assembled feature against
  the original stories, approved MVP decisions and repository standards. Inspect
  actual control/data flow, configuration precedence, persistence/concurrency,
  navigation/state behavior and the independence/coverage of test assertions.
  Record concrete findings with paths, fixes and validation; distinguish this
  agent's review from any subsequent human or independent reviewer approval.
- Complete the acceptance audit for included behavior. Resolve or explicitly
  identify remaining shared-definition concurrency and wider-field browser
  coverage gaps; an old unchecked task is neither proof of a bug nor permission
  to declare a criterion complete.
- Refresh CI for the exact submitted commits. Distinguish passing local tests,
  remote checks and public deployment evidence.
- Completed: publish and verify Referrals, queued cancellation and menu editing
  through the existing Grist authoring system. Public checklist revision
  `8b87c62931a3` contains six stories and 23 steps; all preexisting steps and
  reviewer results were preserved. See the current receipt in `execution.md`.
- Record reviewer feedback and its disposition. Keep human acceptance separate
  from automated validation. No merge is authorized by creating this stack.

Use the [iteration validation policy](uat.md#iteration-validation-and-review-responsibilities):
automated checks before merge, the same selected workflows after deployment,
and concise Grist human review of usable increments. Full operational
qualification is triggered by relevant changes rather than repeated as a human
checklist on every iteration.

The first feedback stopping point is the published stack plus the current public
UAT application and the explicit open items above. Full MVP completion still
requires Non-Conformance and the remaining agreed acceptance criteria.

## Required delivery evidence

The milestone handoff includes a story/criterion-to-test crosswalk, code-QA
findings and dispositions, exact-commit CI links, and inspected recorded E2E
proof for the critical connected workflows. Retain playable video, stable
screenshots and actual downloaded CSV outputs with their checksums, application
and test revisions, deployment/configuration identity and checklist revision.
Record the recording command and result, inspect representative video frames
for readability and compare key screens with the pinned mock. Identify missing
or skipped evidence explicitly. Keep artifacts linked from the PR handoff;
local-only files do not count as shared reviewer evidence.

Critical proof covers Sample & Testing (both layouts, repeats and per-test
turnaround), shared report reuse with fresh dates, Referrals, queue/recovery,
and navigation/configuration at the appropriate viewport. Assertions and video
may use focused complementary flows; do not duplicate the complete regression
suite for recording. Non-Conformance cannot receive proof or acceptance credit
until its remaining behavior is resolved and connected.
