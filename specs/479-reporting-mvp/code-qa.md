# Reporting milestone code QA

This is the implementation agent's source review and validation record for the
ten-PR delivery stack. It is not independent reviewer approval, human UAT or a
claim that the final assembled deployment has passed. See the
[delivery evidence gates](review-stopping-point.md#required-delivery-evidence).

## Current checkpoint: 2026-09-14 navigation review repairs

The public `9baa356345489bf16d197c4ea6db48a615f894f9` deployment passed seven
checks including authentication. Six inspected local recordings for that revision
and the public results are linked from the
[review stopping point](review-stopping-point.md#review-ready-versus-merge-ready).
Earlier validation entries below remain evidence for their stated revisions.

The navigation PR's failed E2E run
[34898686396](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/34898686396)
was investigated through its logs, screenshots, traces and local reproduction:

- **Global menu editing:** the old Cypress test selected 197 controls using a
  global toggle selector, while the editor now has independent menu controls.
  `global-menu-config.spec.ts` replaces that obsolete spec with a real UI
  persistence workflow. It edits an instance-editable database child, saves,
  reloads, checks activation and icon persistence, and verifies the resulting
  navigation. A `finally` block restores and rechecks the original settings.
  Configuration-controlled entries remain protected. Existing shared Cypress
  helpers are retained for their remaining callers; no new Cypress test is added.
- **Final-report amendment:** the button locator also matched the Carbon
  tooltip named "No open amendment." The existing locator now requires the
  exact action name. Reason validation, original and amended report history,
  reidentification and relocking assertions remain intact.
- **Isolate details:** runtime measurement reproduced a zero-width details
  column beside a 400px action column. `MicrobiologyCaseView.css` now allows
  those columns to wrap according to available width. The workbench header also
  stacks below its existing breakpoint to prevent whole-page phone overflow.
  Existing flex-wrapping action styles are reused. The original visibility
  assertion remains, with added desktop/phone text, action and page-overflow
  checks. Both compiled screenshots were inspected at 1280px and 390px.

Validation for the follow-up source on the `9baa356345` baseline:

- Backend and frontend formatters and production builds passed; backend build
  used both test-skipping flags and is not counted as a backend test run. No
  backend production code changed in this repair.
- All three specifications are registered in `core-app`. The focused run on
  the compiled HTTPS preview passed four tests including authentication in
  22.9 seconds, with no skipped tests. The native local synthetic scenario
  service provisioned the microbiology cases; API responses were not mocked.
- Logs and screenshots are retained under
  `/private/tmp/reporting-ci-repair-final/` and
  `/private/tmp/reporting-ci-repair-final.log`. These local files do not replace
  shared evidence or CI for the eventual committed repair.

### Dashboard metrics failure: repaired and validated locally

The passing menu workflow also logged an aborted metrics request, followed by
`Cannot read properties of undefined (reading 'ordersInProgress')` during rapid
full-page navigation. `Utils.ts` calls the callback with `undefined` after a
failed request; `Dashboard.tsx` passes that value to `setCounts` while mounted,
then renders `counts.ordersInProgress`. The Dashboard fetch does not supply an
abort signal. This was reproduced by the new `Dashboard.test.jsx` before the
repair: failed-response cases crashed and the cancellation assertion failed.

The metrics request now owns an abort controller and ignores responses from an
aborted attempt. A failed load shows a Carbon error notification and Retry
instead of replacing counts with `undefined` or displaying zeros as real data.
Retry fetches again; leaving cancels the request. The obsolete shared mounted
flag was removed, including its unrelated tile-effect cleanup. Three component
tests pass for recovery, cancellation and a late response after retry.

The menu workflow now exercises the actual Admin and Back to main menu links,
retaining full reloads of the editor to verify persistence. It checks page errors
and console TypeErrors explicitly, and waits for the actual Carbon loading
overlay to disappear on return. This normal run passed two checks including
authentication in 9.7 seconds. The final compiled recording run passed all three
affected workflows plus authentication in 20.9 seconds. No runtime error was
observed in those workflows. Self-signed service-worker registration errors
remain confined to local authentication setup. Hard document-replacement
diagnostics can still log a cancelled fetch; they no longer produce the
undefined-counts crash.

Visual inspection of the failure state used one controlled metrics-only 503;
Retry then fetched real local backend counts. The final desktop and phone
screenshots show the notification and Retry with Carbon spacing, and successful
recovery. These screenshots were taken after the responsive shell settled.
Backend/frontend builds, both formatters, targeted test lint and the three
component tests passed. The backend build skipped tests; no backend code changed.
Logs are `/private/tmp/reporting-dashboard-{red,green,browser-routed,build}.log`
and `/private/tmp/reporting-navigation-final-recorded.log`; recordings are under
`/private/tmp/reporting-navigation-final-recorded/`. Publication and remote CI
for this follow-up remain required.

The inspected recordings are now published at
[the repair evidence gallery](https://reporting.catalyst.openelis-global.org/reporting-evidence/20260914-navigation-ba3c/).
They cover local frontend `ba3c5ad` with backend `9baa356`. Public application
publication and final remote CI remain pending; recording publication alone does
not satisfy either. No human acceptance or full-MVP completion is claimed.

## Direction audit and configured initial selection

The September 14 audit compared the pinned mock with the desktop/phone captures
and live builder. A fresh public run on `9baa356` passed authentication plus three
reporting workflows: both Sample & Testing layouts and Referrals. Independently
parsed CSVs preserved repeated rows and each result's turnaround; paired queue
re-downloads were byte-identical. This is focused workflow evidence, not proof
of every acceptance criterion.

The audit found a report-ID exception in `CustomDataExport.jsx`: Sample & Testing
ignored catalog defaults while other reports applied them. Follow-up `bf7fbf5`
uses the existing defaults contract for every source. Explicit empty defaults
are now valid; the bundled version-4 configuration preserves the empty picker.
Instance overrides are applied, while an explicitly cleared draft stays empty
after reload. Export and shared-report validation still reject no selected
columns. The two database suites passed 15 checks, the configuration unit suite
passed seven, and the reporting component suite passed 30. Backend/frontend
production builds, formatting and targeted test lint passed. The backend build
skipped tests; the separately executed test suites supply the test evidence.

Navigation consolidation claims have also been narrowed to match source: main
navigation uses `ConfiguredSideNav`, administration uses `AdminSideNav`, and both
share typography styles. The separate administration list is not a completed
configuration-driven consolidation. Non-Conformance and the outstanding date
question remain open. The requirements checklist now accurately records the
resolved Referral decisions; historical execution entries remain dated.

## Finding: simultaneous shared-report edits returned a server error

`ReportingSavedConfigService.update` checked the supplied version before writing.
Two transactions could both accept the original version; the database correctly
rejected the losing write, but its `OptimisticLockException` bypassed the
reporting conflict handler. The user received a server error instead of the
existing `reporting.saved.changed` response.

`ReportingSavedConfigConcurrencyTest` reproduces this with two real PostgreSQL
transactions synchronized after their version checks. Only catalog/access
collaborators are stubbed; persistence, versioning and transaction completion are
real. Before the fix it observed one success and one server error. The service
now translates this specific persistence exception to the established 409
conflict, preserving the cause and leaving unrelated errors unchanged. Update
and soft deletion use the same write helper.

After the fix, the race test and five existing shared-definition service tests
pass. The race test also checks that the winning name and definition persist,
the version changes, and the loser receives the actionable conflict code.
Local logs: `/private/tmp/reporting-saved-concurrency-red.log` and
`/private/tmp/reporting-saved-concurrency-green.log`. This does not yet establish
the fixed response on a deployed build or simultaneous update-versus-delete
browser coverage.

## Source and assertion review

- `useReportingRoute.js` owns the view, step, source, layout, queue position and
  saved/job links in the URL. It preserves parameters owned by the review widget
  and derives state on browser history navigation. `CustomDataExport.jsx`
  retains input drafts in the browser session, clears them on logout/user change,
  clears dates when reusing a shared definition, and guards late mutation
  callbacks with a draft revision. Existing component tests cover those paths;
  31 reporting/route component tests pass in the assembled reporting slice.
- `ReportingCatalogService` reads configured sources and instance test catalogs;
  database source definitions override bundled defaults. Submission freezes
  ordered variable definitions and validates stale selections. Shared settings
  omit run dates. `ReportingJobService` serializes submission admission, keeps
  immutable request snapshots and checks lifecycle transitions for cancellation,
  retry and downloads. Their focused tests passed in the preceding stack slices;
  final assembled checks and exact-head CI remain required.
- `ReportingCsvWriter` streams ordered fields and preserves repeated result
  identities. The new recorded-workflow specification independently compares
  actual CSV headers and known synthetic rows, checks distinct detailed result
  identities, then verifies the queue returns identical stored bytes. It uses
  visible UI operations and deletes only the report it created.

The three new connected proof scenarios (plus authentication) passed their first
non-video run against public application `d48cd790c492`. They cover both Sample &
Testing layouts, per-test turnaround, shared reuse with fresh dates, Referrals
including pending work, and queue downloads. This is a baseline for recording;
no video, final-stack deployment or human acceptance is credited by that run.

## Remaining review and evidence

- Navigation source review: `MenuConfigurationLoader` copies configured entries
  without mutating persisted defaults; `MenuServiceImpl` excludes controlled
  fields from saves and rebuilds the effective cache after commit. The annotation
  mapping replaces the old XML registration. `ConfiguredSideNav` provides one
  Carbon renderer, and superseded navigation styles/renderers are removed.
  Upstream menu-domain filtering is preserved. The assembled stack passes both
  builds, 74 affected component tests and 18 mapping/configuration/rollback/race
  tests. A focused navigation walkthrough plus authentication passes against the
  existing public build. Final rendered comparisons at matching desktop and
  narrow widths remain required.
- Run the connected workflow specification on the final tested deployment,
  record it through `core-demo-video`, inspect screenshots and representative
  video frames, and publish playable artifacts with revision/checksum receipts.
  Add complementary queue-recovery and navigation evidence without duplicating
  the entire automated suite.
- Complete the wider-field and included-criterion audit; distinguish any
  remaining gaps from failing behavior. Non-Conformance's unresolved date
  semantics and disconnected source receive no acceptance credit.
- Refresh CI for the submitted commits and publish review links. This local
  record and prior green checks do not stand in for those gates.

## Standard-profile menu test correction

CI run `34906793852` passed 100 tests in core shard 1 and failed the menu workflow
when its cleanup tried to locate a direct Admin link. The trace and screenshot
show a healthy Dashboard. The standard `volume/menu/menu_config.json` puts
Admin dashboard inside an Admin group, while the Reporting profile uses a direct
Admin link. The test now follows either explicitly configured structure before
opening Global Menu Configuration. Persistence, original-value restoration,
Dashboard/error assertions and timeouts are unchanged. The Reporting-profile
workflow passed with authentication (two checks, 9.9 seconds) against compiled
local frontend `ba3c5ad`. The subsequent assembled `8005e4cc0b` passed the full
E2E gate, including the standard profile, in run `34909301878`. Its public
navigation checks also passed. The earlier failed run remains historical evidence;
the shared-copy follow-up below needs its own current-commit checks.

## Shared-editor recovery correction — September 14, 2026

The public two-editor qualification on application `8005e4cc0b` verifies that
two tabs can open the same shared definition, the first update persists, and a
stale second update gets the intended conflict notification while preserving its
column order. Saving a separate copy succeeds. Reopening both definitions from
the library and downloading actual synthetic CSVs confirms the original has its
added Patient Name column while the copy retains its independent reordered
columns; both retain the two equal Viral Load readings. Cleanup removes only the
test's uniquely named definitions using their current versions. This tests stale
editor behavior; the synchronized database race remains a separate test.

The first public attempt exceeded the assertion window while the second tab's
catalog request was still pending. The trace showed no server failure. A focused
loading wait allowed the workflow to complete. Reopening from the library,
rather than reloading an active draft, is required to inspect the server's saved
definition: preserving a draft on reload is intentional.

The public workflow and its subsequent HD recording passed (each two checks
including authentication). However, direct frame inspection exposed a real UI
recovery issue: the old conflict warning remained beside the successful copy
notification. Those recordings are diagnostic evidence of the existing defect,
not proof that the correction is deployed.

`CustomDataExport.jsx` now resets the previous update mutation only in the
successful create/copy callback, after its draft-revision guard. The new component
regression first reproduced the lingering warning. It also simulates a failed
copy save, checks that its name and choices remain, then verifies the warning
clears after a successful retry without altering the original definition.
All 31 reporting component tests pass. The compiled local two-editor workflow
passes with actual CSV downloads and asserts the stale warning disappears after
success (two checks including authentication, 26.9 seconds). Both production
builds, both formatters and focused Playwright lint passed. Backend packaging
skipped tests; no backend source changed in this correction. The first formatter
process failed on dependency DNS/cache access; the authorized retry completed.

The browser regression now additionally switches the second editor to 390px for
recovery and records desktop/narrow states. Its first launch was rejected because automatic approval review reported an
exhausted usage allowance. After a fresh usage check and normal approval retry,
the expanded test passed (two checks including authentication, 25.5 seconds).
The inspected 390px capture shows the copy success notification without the old
warning, retains the ordered fields and fits the viewport. Desktop/narrow review
and save structure were compared with the pinned mock; no layout code changed.
The regression registers each created definition for cleanup before assertions
that could fail after a successful write.

Pending: assemble the existing navigation child without rewriting history, and publish
and re-record the corrected public workflow. Preserve the review-tooling release
`7e45214eaf0be66b899807c60e61840f3efe284e` and its Grist-owned presentation.
The application publication remains `8005e4cc0b` at this checkpoint. The latest
verified general evidence is [the public review gallery](https://reporting.catalyst.openelis-global.org/reporting-evidence/20260914-review-8005/).
Non-Conformance, remaining included acceptance checks and human acceptance remain
open. This correction still requires public deployment and current-commit CI.

## Text and dictionary browser qualification — September 14, 2026

`reporting-field-values.sql` adds two synthetic specimens on May 8, 2026,
using the existing configured Viral Load text option and DNA PCR Positive
option. Each has two independent equal results. The text fills the existing
200-character result column and includes a comma, quotes, CRLF and literal
markup characters. The fixture resolves tests by GUID and dictionary choices
through their configured option, asserts the resulting identities/date/values,
and passes a second load without inserting duplicates. Earlier repeat,
turnaround and referral fixtures remain unchanged. May 7 was rejected because
it already contains the retained 50,000-result workload fixture.

Two registered `core-app` browser tests select the fields, switch to phone
width, submit both layouts, download the actual files and compare all four
records. They also verify distinct result IDs in the detailed layout and blank
unrelated test cells in the spreadsheet. Local qualification and the first
public recording each passed both workflows plus authentication. The public
application remained frontend `7cca586e58`, backend `8005e4cc0b`; only synthetic
fixture data was added.

A subsequent top-of-page screenshot capture exposed a public phone-width
obstruction: `oe-review-host` intercepts ordinary clicks on **Next: Set Filters**
at 390 × 844. Both final recording checks failed at that interaction; the
review-tooling owner has the trace and screenshots. The tests keep ordinary
clicks and their original expectations. This public interaction has no acceptance
credit until the obstruction is corrected and the affected checks pass.
The first successful recording is not a substitute for that final check.

This increment does not qualify configured multiselects or result components
through the public browser. Those broader cases retain their database-level
checks and remain part of the outstanding field audit. Non-Conformance and human
acceptance also remain open.

## Saved-period rerun and checkpoint handoff

The registered saved-period test passes locally (workflow plus authentication,
18.7 seconds): save/run May 5, reopen from the shared library with blank dates,
then run May 6. Actual CSVs preserve both results and show 0/0 minutes for the
first period and 30/90 for the second. Both server job snapshots and the saved
definition snapshot are retained with the downloaded files outside Git for the
Catalyst integration handoff. No fixture reset was used.

The corresponding public run is not accepted: the same review-widget host
intercepts **Generate CSV** at desktop width. The widget owner has this finding.
Per the user's checkpoint direction, remaining work prioritizes that normal-use
obstruction, one focused rerun and a manual-UAT handoff; broader field and
Non-Conformance gaps remain visible rather than prompting more test expansion.

## Non-Conformance initial mapping — September 14, 2026

The user approved event-date precedence with a visibly labeled recorded-date
fallback. The isolated `codex/reporting-nonconformance` follow-up now contains
streamed native-event and legacy-rejection projections plus the common CSV
source adapter. No report definition has been enabled and no deployment occurred.

Focused validation: `NonConformanceSourceTest` passed one CSV check, preserving
equal independent identities, original dates, fallback labels and a legacy
timestamp converted to the instance timezone.
`NonConformanceExportMappingIntegrationTest` passed two checks against disposable
PostgreSQL: known event dates take precedence across different recorded periods;
recorded-only occurrences remain eligible; both period boundaries are included;
undated/out-of-period records are excluded and equal occurrences remain distinct.
The application compiled and its test Spring context started. These checks do not
qualify the complete Non-Conformance workflow or claim mock/public acceptance.

The database checks first exposed a missing section fixture, then a mixed-type
section parameter. Inspection of Hibernate mappings confirmed that the legacy
String IDs are numeric-backed. Separate integer-unit and legacy-section query
parameters corrected the issue; the reason join retains its actual numeric
mapping. Expectations were not relaxed. Remaining work includes specimen/reason
association fixtures, source configuration and date guidance, and a focused
recorded browser/CSV workflow compared with the pinned mock before publication.

### Connected Non-Conformance source checkpoint

The new bundled source definition now connects Non-Conformance to the existing
catalog/queue engine, with empty initial column selection matching the mock.
The catalog retains the mock's five fields and exposes occurrence/specimen
identity, original dates, Date Basis, source and available event status. The
existing builder renders the new group and explains the approved date fallback;
no alternate page or report-specific builder was introduced.

Five focused backend checks pass (four disposable-PostgreSQL mapping/catalog
checks plus one CSV check). The association fixture deliberately uses the same
numeric ID for a rejection reason and an unrelated NCE type: the rejection
label is correct, two linked events remain separate, and the legacy occurrence
is retained independently. Three selected builder component checks pass,
including Non-Conformance; the other 29 component tests were not run in this
focused invocation. The initial association fixture needed required legacy
QA-event flags before it could load; no application expectation was relaxed.

This is local code/test evidence only. Browser comparison, synthetic runtime
fixtures, recorded downloads and public deployment are the next increment.

## Non-Conformance recorded local checkpoint — September 14, 2026

The synthetic fixture loads twice without duplicating records. The registered
`core-app` workflow plus authentication passes in 12.3 seconds, with zero failures,
skips or retries. It starts from the normal overview, selects Non-Conformance,
adds fields, uses May 10, 2026, reviews the date guidance and downloads a real CSV.
The four independent rows include one event dated May 10 but recorded May 12,
two recorded-date fallbacks and one separately identified legacy rejection.
An event dated May 9 but recorded May 10 and a wholly undated event are excluded.
The first browser attempt exposed a test-only header assumption; the CSV contract
uses selected display labels. Correcting that expectation retained all row,
identity, date and source assertions.

Direct comparison used `openelis-work` mock revision
`5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f`, including its companion example script,
in the Non-Conformance state at 1280 and 390 pixels. The shared implementation
retains report cards, collapsed groups, Add selection, ordered columns and the
narrow view switch. It exposes twelve fields rather than the mock's five to
include original dates, date basis and occurrence identity. Existing Carbon shell
colors, configured-report selector and omitted fictional preview controls differ
from the mock; this is structural comparison, not pixel-identical certification.
The local desktop/phone screenshots, ready state, actual CSV and representative
recording frame were inspected. Full-page captures can show the fixed shell at
the current scroll position; the recording is the interaction evidence.

Both production builds and formatters passed. The separate five backend and
three selected component tests are recorded above. Artifacts are retained locally
at `/private/tmp/reporting-nc-local-evidence/`; execution output is
`/private/tmp/reporting-nc-local.log`. Public deployment, publication of these
artifacts and human acceptance are not yet credited. No broader suite is needed
solely to repeat this local checkpoint.

## Public Non-Conformance delivery — September 14, 2026

Frontend and backend `3de726b8d38ba102ac2fa564c95ac59a2a4e02b7` are deployed.
The previous database, menu configuration, review integration mounts and older
evidence were preserved. Startup completed in 461 seconds; its configured FHIR
endpoint remains unavailable. No reporting failure was observed after startup.

The focused public workflow and authentication passed in 35.3 seconds without
retries. Actual CSV rows match the four-occurrence local oracle. Desktop/phone
screens and representative video frames were inspected.
[Published evidence](https://reporting.catalyst.openelis-global.org/reporting-evidence/20260914-non-conformance-3de726/)
contains the video, actual CSV, screenshots, mock reference and revision/hash
manifest. Nine public files matched their hashes; the video supports HTTP 206
range delivery. Frontend/static CI pass; backend CI is still running.

Non-blocking follow-up observed in the recording: while the report-type request
is loading, the type cards briefly show “Not yet connected.”
`ReportingView.jsx` derives connected types from possibly absent request data
(lines 77–80). Once loaded, all three cards are available. This should distinguish
loading from an unavailable source; it does not prevent the tested export.
No additional feature changes or expanded test run were made for this finding.
Grist's owner is updating RPT-101/RPT-202, preserving existing human results.
Human acceptance and the remaining full-milestone audit remain open.

The [consolidated evidence index](https://reporting.catalyst.openelis-global.org/reporting-evidence/) links all eight published bundles, separating local recordings from public runs and retaining revision-specific provenance.

## Delivery-branch test reconciliation — September 14, 2026

The existing Sample & Testing follow-up `e7e279a12b` was still only on its
source PR branch. A normal merge brings its stronger CSV assertions into the
assembled delivery branch without changing production source. The focused
`SampleTestingMappingIntegrationTest` suite passes all ten checks in the assembled
context (`/private/tmp/reporting-assembled-sample-check.log`, Maven 39.8 seconds).
Its independent CSV parser verifies full-width text, quoting/line breaks, grouped
multiselect labels, qualifiers and repeated values in both layouts. No broader
suite or new public deployment was run for this test/documentation increment.

The stopping-point page, execution header and specification checklist now
acknowledge the approved Non-Conformance date rule and public delivery. Earlier
revision receipts remain historical records; unresolved task boxes are not
automatically interpreted as absent implementation. Full milestone completion
continues to require the criterion-level evidence reconciliation and current CI.
