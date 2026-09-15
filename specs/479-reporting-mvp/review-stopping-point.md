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
- Navigation: one Carbon renderer, consistent typography, reports hierarchy and
  routed drafts; database defaults plus instance configuration, sections/icons,
  and an editor that preserves controlled values.
- Public UAT: stable synthetic fixtures, actual CSV comparisons, desktop and
  narrow-screen workflows, and deployment provenance.

## Packaging contract

Use the official `gh stack` workflow. Each pull request contains one clean
snapshot commit and targets the preceding branch. Preserve the existing M1/M2
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

The current public application is `d48cd790c49294ddb4a36c9333d3acc744ebb3c4`.
Its public workflow evidence remains valid for that application. The subsequent
recovery-test revision `a4f7160b1dcce0245eed97b1f3f26c16cb44203e` has passing
backend, frontend and translation CI. Public cancellation also passed after its
observation window was corrected; the final test and retained output are included
in the new stack. These are prior integration evidence, not a claim that the
newly packaged commits have already passed CI.

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
