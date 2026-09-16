# Reporting review and merge stopping point

This delivery checkpoint packages the publicly testable Reporting MVP increment.
It preserves the full goal and canonical mock. Sample & Testing, Referrals and
Non-Conformance are connected; automated evidence and human acceptance remain
separate. Use the [evidence index](https://reporting.catalyst.openelis-global.org/reporting-evidence/)
for revision-specific recordings, actual CSVs, mock comparisons and code QA.

## Included behavior

- Sample & Testing: instance-aware fields, both CSV layouts, every repeated
  result, turnaround beside each test, saved shared reports and fresh dates.
- Referrals: the same builder, shared library, queue and CSV engine; independent
  returned results and pending referrals are preserved.
- Non-Conformance: the same builder and queue; independent event/specimen and
  legacy-rejection occurrences, event-date precedence, visibly labeled recorded-date
  fallback and original dates.
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

The public frontend/backend is `3de726b8d38ba102ac2fa564c95ac59a2a4e02b7`,
with review tooling `2048bc3cfd`. The original ten-PR stack remains separate by
function, with Non-Conformance [follow-up #4318](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4318)
above navigation #4315. Subsequent documentation and test-only commits do not
change the deployed production source. No PR has been merged by this delivery.

The public Non-Conformance workflow plus authentication passed in 35.3 seconds
without retries and downloaded four independently identified synthetic records.
[Its evidence](https://reporting.catalyst.openelis-global.org/reporting-evidence/20260914-non-conformance-3de726/)
contains the actual CSV, video, desktop/phone screens and pinned mock comparison.
The database, navigation configuration and review integration were retained.

Earlier evidence remains valid for its recorded revisions: core reporting and
mock comparison at `8005e4cc0b`, shared-editor recovery at `7cca586e58`, and the
public field-value/saved-period checkpoint using review tooling `2048bc3cfd`.
The [index](https://reporting.catalyst.openelis-global.org/reporting-evidence/)
labels public runs and local recordings explicitly. These are not claims that
every earlier workflow was repeated on `3de726b8d3`.

Frontend/static CI passed on the implementation revision. CI for the later
delivery receipt is running at this checkpoint; inspect the current PR before
claiming merge readiness. Human acceptance remains pending. The Grist owner is
correcting verified live availability keys RPT-001/RPT-200 and adding RPT-202;
RPT-101 is the existing shared-save workflow and must remain unchanged.

A minor recorded usability follow-up remains: report-type cards briefly show
“Not yet connected” before the catalog request resolves. This is distinct from
a source actually being unavailable. The completed public export is unaffected.

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
requires the remaining agreed acceptance audit, current required CI and completed
UAT-readiness mapping; human acceptance is recorded separately.

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
