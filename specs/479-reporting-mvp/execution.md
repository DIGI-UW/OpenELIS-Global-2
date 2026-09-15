# Reporting MVP Implementation and Deployment

## Current manual-UAT checkpoint — Non-Conformance delivery

Public frontend/backend `3de726b8d38ba102ac2fa564c95ac59a2a4e02b7` connects all
three report types. The database, configuration and review tooling `2048bc3cfd`
were retained. The focused public Non-Conformance workflow and authentication
passed in 35.3 seconds with no retries and downloaded the expected four records.
The [evidence index](https://reporting.catalyst.openelis-global.org/reporting-evidence/)
links all eight published bundles; its links and rendered page were checked.

[PR #4318](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4318) contains the
source and its delivery receipt above the separate navigation PR. Frontend/static
CI passed on the implementation commit; later receipt CI remains in progress.
The source date decision is resolved. Remaining milestone audit and human
acceptance are open. Grist availability/Non-Conformance wording is being updated
by its owner without changing existing feedback.

## Earlier manual-UAT checkpoint — September 14, 2026 (Pacific time)

The public stage remains frontend `7cca586e5874ccb177d3a59f75177a5a9867e1d5`,
backend `8005e4cc0b2b05d054489730aef969027d773093`, review tooling
`2048bc3cfd038e42d0fcb92412d4ce5c79fbc090`. The review-widget obstruction is fixed.
Four focused public checks passed with ordinary interactions: authentication,
both CSV layouts at phone width, and a saved report rerun across distinct periods.
[Published recordings, CSVs and manifest](https://reporting.catalyst.openelis-global.org/reporting-evidence/20260914-uat-checkpoint-2048/)
identify test source `568b7454370bf8873a5c8b52cbf1ef6f47f01ce7`. Public CSV bytes
and video availability were verified. No additional test matrix was introduced.

Backend authoring and the [public Grist checklist](https://grist.openelis-global.org/uat/reporting.json)
agree on revision `8b87c62931a32705395fc8be2d5c5aef7195eeb0d6b64a615390cb0a2bda0bd4`,
verified September 15 at 02:03 UTC. Six stories contain 23 steps. Only RPT-303
and RPT-504 were added; all 21 preexisting steps, including the newer split
Referrals walkthrough, were preserved. Story PR links now reference #4310,
#4309 and #4315. The review owner used revision preconditions and stable-row-ID
readback; reviewer results were not written. T036 and T041 publication gaps are
closed. No human-pass claim is made.

All ten stack PRs are ready for review, none merged. Submitted UI `568b745437`
and navigation `9c1bfd713c` passed frontend and full E2E CI; their backend jobs
remain running at this checkpoint. Non-Conformance still awaits its missing
event-date decision and implementation; full MVP completion remains open.
The following sections are historical snapshots, not current blockers.

## Validation ownership and delivery checkpoint — September 14, 2026

Original `openelis-work` user stories and the approved mock govern UAT. Grist
presents those stories and records review feedback, with future `OpenELIS-QA`
synchronization. Implementation-specific automated E2E and video proof remain
with application code. See [the cross-project contract](https://github.com/DIGI-UW/openelis-review-tooling/blob/codex/grist-backend-authoring/docs/validation-ownership.md)
and [the reporting crosswalk](uat.md#iteration-validation-and-review-responsibilities).
The guidance and direct Grist client are published in review-tooling PR #22,
commit `64b08c214ee3d2ac1fb98830150a715637d86d36`: 206 local tests and its
remote CI pass. Live authoring still needs credential provisioning. No checklist
or human answers were changed. Hindsight capture and document ingestion were
attempted but failed with DNS resolution of its configured server; memory
persistence is not claimed.

A focused public smoke run passed seven checks in approximately 2.2 minutes:
authentication, Sample & Testing repeated-result spreadsheet, per-test turnaround
in both layouts, detailed result identities, configured sidebar and retained
draft, and Referrals shared-report/CSV workflows at 1280 and 390 pixels.
The application stayed at `d48cd790c49294ddb4a36c9333d3acc744ebb3c4` before and
after the run; configuration, widget and deployment identities also matched.
Checklist revision remained
`14333b9e6281374aac57eeb38177a7fca0bba1ff340f380457261af19e3b3481`.
This run recorded screenshots and CSV assertions, not video proof or human
acceptance. Local receipt: `/private/tmp/reporting-iteration-smoke-receipt.json`;
log and evidence: `/private/tmp/reporting-iteration-smoke.log` and
`/private/tmp/reporting-iteration-smoke-evidence/`. These are local artifacts,
not published evidence URLs.

The official ten-PR delivery stack is #4306. The first six slices are published
as #4304, #4305, #4307, #4308, #4309 and #4310, each one clean commit. The
remaining route/presentation/UI/navigation slices and final assembled validation
remain in progress. The public application is the existing integration revision,
not one of the newly packaged commit IDs.

The route and complete presentation slices are now committed. The connected UI
slice passes its backend/frontend builds and all 31 reporting component tests.
Three new implementation-proof workflows plus authentication pass against the
existing public build in 1.2 minutes; recordings remain to be captured and
inspected on the final tested deployment. Code QA reproduced and corrected a
simultaneous shared-report edit that returned a server error instead of a 409
conflict. The real PostgreSQL race check and five existing service tests now
pass. See [the findings and remaining checks](code-qa.md). Final navigation
assembly, exact-head CI, deployment/video proof and human review remain distinct
open gates.

The user authorized implementing the complete agreed MVP and deploying each
usable stage to Reporting UAT. Both milestones remain in scope. The canonical
mock defines the interface; MVP scope determines which functions are connected.
Public availability, automated qualification and human acceptance are separate.

## Public queued-cancellation qualification — 2026-09-14

Queued cancellation is now repeatable on the public application using normal
exports. The committed preparation procedure and fixture (`a4f7160b1dcce0245eed97b1f3f26c16cb44203e`)
were uploaded with verified hashes, backed up the database, and installed 50,000
synthetic results across 5,001 specimens and 10,001 analyses. Application
`0714c3b49924` and database `f9933a28cb4b` were retained; the application still
serves `d48cd790c492` and was not restarted. Setup evidence is under the
`a4f7160b1dcce0245eed97b1f3f26c16cb44203e/cancellation/qualification` release.

The final public cancellation run passes three checks in 7.5 minutes:
authentication and the full workflow at 1280×900 and 390×844. Both verify Keep
queued, reload, confirmed cancellation, another reload, the entire 50,000-row
CSV, no later processing of the cancelled job, and refused download (409).
The earlier public run also passed failed-report retry and expired-report
re-run with actual two-row CSVs. Thus all four public recovery workflows pass;
they are evidenced across these two runs, not claimed as one five-check green run.
The expected cancelled-download 409 is the only final console resource error;
there were no page exceptions. Public queue and confirmation screenshots at both
widths were directly inspected against the pinned mock's queue captures.

Evidence: `/private/tmp/reporting-cancel-public-verified-evidence`, including
actual `workload.csv` files and `cancellation.json` receipts for each width;
`/private/tmp/reporting-cancel-public-verified.log`; and the earlier recovery
run `/private/tmp/reporting-cancel-public.log`. Both CSVs contain 50,000 rows,
1,045,692 bytes, and SHA-256
`be66149a030d706762120efbb8017a1d0efbb0462485d1ca11269e34b6366edd`.
The final desktop workload is `30f35a04-8dd5-47d8-918c-7b4f393b9ed8` with cancelled
job `cbf6e1f2-a9f3-4419-9478-d7fe014f00ec`; the phone workload is
`f1474f90-7718-4c8b-b537-4601d4121af5` with cancelled job
`7727b124-ef81-4be1-b787-603aae822b07`.

The first public large export exceeded the test's 180-second observation window.
Its actual worker remained active and renewed its lease. That same job completed
without restart in 218.4 seconds and produced the same CSV hash. The cancelled
job remained unprocessed. Inspection observed 1.385 GiB app memory and roughly
159% CPU at one point; this is not a peak-memory measurement or an isolated
performance benchmark. The thread dump was requested after completion and does
not establish an active-processing hotspot. The browser window was increased to
360 seconds based on that measured runtime; SC-006 has no universal time limit,
and all content, state and refusal assertions remain unchanged.

Backend CI run `34880970783` completed successfully for the deployed revision.
T021's lifecycle/browser qualification is complete. T036 is reopened for the
current stages: RPT-201, RPT-303 and RPT-504 are prepared but not in the live
six-story/17-step checklist. Server authoring remains unavailable; the normal
Grist web interface also requires sign-in, and no alternative Chrome connection
is available. No checklist or human answers were changed. Non-Conformance's date
choice and implementation, current checklist publication, final audit and human
acceptance remain open.

## Current Public Stage — Referrals, 2026-09-14

The [public reporting application](https://reporting.catalyst.openelis-global.org/reports/custom-data-export)
now serves frontend, backend and instance configuration
`d48cd790c49294ddb4a36c9333d3acc744ebb3c4`, deployment
`20260914T184156Z-d48cd790c492`. Review tooling remains `54b99f8d76ba` and runtime
configuration remains `7780ee2cd987`. The
[live identity](https://reporting.catalyst.openelis-global.org/__review/target.json)
records this ready stage, its checks and the remaining work.

Six public browser checks passed in 2.1 minutes: authentication, Sample & Testing
spreadsheet repeats, turnaround in both layouts, configured sidebar/query context
and retained draft, and the Referral saved-report/fresh-date/actual-CSV workflow
at 1280×900 and 390×844. For May 7, the Referral CSV contains two independently
identified 450 returns dated May 8 and May 9, plus one pending REQUESTED row.
The unsent draft and May 8 sent referral are excluded. Only the test-created
shared reports were removed after their workflows; existing reports were retained.
Public desktop and phone columns/review captures were inspected against the
pinned mock. The collapsed catalog, ordered selected fields, mobile panels,
teal Referral label and explicit sent-date basis are present. Native OpenELIS
chrome, real result identifiers and the background-generation message remain the
recorded implementation-specific differences.

All 210 artifact hashes were checked before deployment; served HTML and its
referenced assets match the release. Application `0714c3b49924` started once and
became ready after approximately 480 seconds; observation continued on that same
container. Database `f9933a28cb4b`, report files and logs were retained. The
synthetic Referral fixture was loaded and validated. The application has one
context; the obsolete duplicate context returns 404. Previous artifacts,
configuration, identity, logs and a database backup remain with the release.

Public evidence: `/private/tmp/reporting-referral-public-evidence`,
`/private/tmp/reporting-referral-public-browser.log`, and
`/private/tmp/reporting-referral-public-verification.json`. The remote release is
`/home/ubuntu/reporting-uat/releases/d48cd790c49294ddb4a36c9333d3acc744ebb3c4`;
its `public-verification.json` preserves the earlier menu restart/profile and
review-widget evidence with their original scope and timestamps.

Frontend, static, image and translation checks pass for the deployed revision;
backend Build + Test (run `34880970783`) is still running at this checkpoint.
The live checklist remains six stories and 17 steps at revision
`14333b9e6281374aac57eeb38177a7fca0bba1ff340f380457261af19e3b3481`.
RPT-201 and RPT-504 instructions are prepared; the authoring SSH timeout still
prevents publishing them. No human answers were modified. Non-Conformance's
missing event-date decision, repeatable public queued cancellation, final audit
and human acceptance remain open. This usable stage does not complete M2 or the
full goal.

## Two-Process Recovery Qualification — 2026-09-14

The remaining multi-instance crash-isolation check now passes against two real
application processes sharing the local database and report volume. Both use the
public stage's `22e3a66b6175` WAR and single-context configuration. The new
`projects/reporting-uat/qualify-worker-isolation.py` runner starts with no active
jobs, submits a baseline and then observes one generating job per process plus
one queued job. A bounded read stall exposes the recovery interval without
changing job timestamps or the normal 300-second lease.

The temporary peer `e42cd336a48e` was killed with SIGKILL. Primary application
`6d7d3d387b8b` remained running with its original start time; its lease continued
renewing across 59 observations. Its partial file and queued job remained intact.
Only the killed worker's job failed as interrupted and its partial file was
removed. Releasing the stall let the live and queued jobs complete. A linked
retry retained the abandoned job's frozen request. All downloads matched the
baseline CSV byte for byte, preserving both repeated values. Exactly one
`INTERRUPTED` audit event identified the killed worker's job. Database
`f4572a3f704c` was retained. The peer's final inspection and logs were saved before
removing that temporary service; primary and database remain running.

Receipt: `/private/tmp/reporting-multi-process-20260914/isolation/verified.json`;
observations, ownership logs and actual CSV are in the same directory. CSV
SHA-256 `499ab005f03b3c02d0da1af52097f3f64b6f00599f839beadac3e577fe741e32`.
See quickstart for the reproducible procedure. This closes T024 and T028 when
combined with their recorded lifecycle, retention and migration checks. It adds
local operational evidence to the existing public application; it does not
claim a new public deployment or human acceptance. T021's public queued-cancel
workflow and the source-activation/product decisions remain open.

## Previous Public Stage — Database Menu Presentation, 2026-09-14

The navigation follow-through now persists optional section/icon metadata in the
existing menu table and edits it through Global Menu Configuration. Server-owned
configuration provenance makes instance-controlled fields read-only and prevents
saving effective overrides into database defaults. Configuration-only grouping
entries remain compact; database children remain editable. Successful writes
refresh the editor with the effective server tree; failed writes retain edits.

The menu entity now uses annotations in both application and test runtimes;
`Menu.hbm.xml` was removed. Migration `479-005-menu-presentation` adds the two
optional fields and the standard version timestamp. The initial persistence
regressions failed with lost values. The completed checks pass: 26 backend cases,
including real database round trips, override protection, mapping startup,
existing menu APIs, fresh migration registration, 1,000-menu upgrade/rollback,
and the existing 50,000-job reporting rollback checks.

The local app was replaced once with the retained database, report files and
logs, after a database backup. Container `6d7d3d387b8b` became ready after 198
seconds. The actual browser saved and reloaded an Alerts icon, restored its
previous value, and verified configured Reports fields remain read-only.
Screenshot inspection caught a missing small-screen grid span and order-entry
accordion styles leaking into this page. The grid now uses explicit Carbon
breakpoints, the old styles are scoped to order entry, and the corrected editor
uses 342 of the 390 phone pixels. The affected browser check passes again.

The [public application](https://reporting.catalyst.openelis-global.org/reports/custom-data-export)
now serves frontend/backend/instance configuration
`22e3a66b6175f793103fe9c729ff4e02339dc7de`, deployment
`20260914T165120Z-22e3a66b6175`. Its
[live identity](https://reporting.catalyst.openelis-global.org/__review/target.json)
records the exact revision and verification. The database, report volume and
persistent logs were retained, with a pre-migration database backup and previous
artifacts/compose/identity preserved. All 209 artifact hashes were verified;
served HTML/assets match. Migration `479-005-menu-presentation` completed once.
The application became ready about 484 seconds after its single replacement;
one application context is active and the duplicate API context returns 404.

All 10 public browser checks passed in 2.5 minutes: login, pinned-mock capture
and eight application workflows. They cover spreadsheet repeats, detailed result
identities, per-test turnaround, shared report create/use/update/copy/delete,
desktop/phone column interactions, sidebar/history/query context and retained
drafts, administration typography, and menu icon save/reload/restore with
instance-controlled fields. Actual CSV contents were checked. Desktop and phone
captures were directly inspected against the pinned mock at widths 1280 and 390;
the native OpenELIS theme, real configured catalog, two-layout controls and
unconnected source labels retain the previously recorded functional differences.
The menu editor fits both widths without overlap. Seventy component checks,
required formatting and application builds also pass for this increment.

Actual local restart/two-profile qualification now passes. The Alerts menu's
saved `patient` icon and `section` presentation survived restarting application
`6d7d3d387b8b` with database `f4572a3f704c` retained. A second profile changed its
effective icon to `reports` and presentation to a menu item. Saving the overlaid
entry did not change the database defaults; removing those two overrides
restored `patient`/`section`. The original menu values and exact profile bytes
were restored afterward, with no frontend changes. Receipt:
`/private/tmp/reporting-menu-profile-qualification/verification.json`.

T041 remains open only for the review-tooling follow-through. RPT-504 is prepared
but not published: direct authoring SSH and a relay through the deployment host
both timed out. No Grist rows or human answers were changed. The public checklist
and same-origin catalog still provide six stories and 17 steps at revision
`14333b9e6281374aac57eeb38177a7fca0bba1ff340f380457261af19e3b3481`.
The review-picker problem was subsequently reproduced locally: application tabs
on different routes repeatedly replaced a shared story preference. Review-tooling
commit `54b99f8d76bac9b46a2082e3549a7d013e8406ff` now keeps navigation per tab
while synchronizing an explicit pop-out with its opener. All 104 widget browser
checks and 201 tooling tests pass; two earlier checks were corrected to wait for
the selected checklist and settled scroll position. The fix is published only
to Reporting UAT, with both local and public script bytes verified. Application,
database and web container identities/start times remained unchanged. Review
answers and the six-story/17-step checklist were not changed.

Live in-app validation now passes: all six stories are selectable; the reporting
tab retains its navigation story while a separate menu-administration tab keeps
its routine-export story, including after refresh/reload. The reopened picker was
visually inspected. Three fresh public automated checks pass (authentication,
repeated-result spreadsheet CSV and per-test turnaround in both layouts).
Receipts: `/private/tmp/reporting-review-tab-publication.json` and
`/private/tmp/reporting-review-tab-public-verification.json`; tests:
`/private/tmp/reporting-review-widget-release-final.log`,
`/private/tmp/reporting-review-unit-final.log`,
`/private/tmp/reporting-review-public-csv.log`. The review fix is in
[draft PR 21](https://github.com/DIGI-UW/openelis-review-tooling/pull/21), stacked
on the exact published review runtime. RPT-504 publication alone remains open
under T041. The exact application revision passes frontend, backend and
translation CI. Human acceptance remains pending.

Logs and screenshots: `/private/tmp/reporting-menu-final-backend.log`,
`/private/tmp/reporting-menu-responsive-browser/`,
`/private/tmp/reporting-menu-local-parity/`,
`/private/tmp/reporting-menu-public-browser/`,
`/private/tmp/reporting-menu-public-deployment.log`. The server retains deployment
and public-verification receipts under `releases/22e3a66b6175f793103fe9c729ff4e02339dc7de`.
The manifest SHA-256 is
`7be02c33a7805fe03a058e9bd081d12c6b6b0546a6f60a542474ed9f51e79b0c`.
The menu profile remains
`35c3995db966ef8c1d4897b430cb317b33cb2021422ac16d8a61dfb08302e93e`.

## Previous Public Stage — Configured Navigation and Audit, 2026-09-14

The published stage consolidates the sidebar renderer and its shared Carbon
typography, removing 535 lines of conflicting old sidebar rules. The Reporting
UAT profile supplies the mock's four sections, icons, direct workflow links and
collapsed legacy groups through the existing database-plus-configuration menu
loader. Database rows, unspecified settings and unlisted instance extensions
remain intact; section/icon metadata is configured in JSON, not yet exposed in
the database menu editor. Other instance profiles are unchanged.

Reporting now uses `/reports/custom-data-export`; legacy bookmarks redirect with
query, fragment and history preserved. Active menu selection uses path and query,
native links retain modified-click behavior, and mobile view changes close the
drawer. Sidebar and internal reporting navigation share parameter ownership so
review context survives changing views. The existing report draft is retained.

The increment adds committed reporting lifecycle and shared-definition events
to the existing application logger. The public release retains old logs and
mounts a persistent log directory. A real saved-definition request exposed a
timestamp-precision conflict on immediate edits after creation; a failing
database round-trip test reproduced it. Creation now uses database-supported
microsecond precision, and create/read/update/stale-update/delete checks pass
across committed requests.

Validation includes 125 component checks, 35 focused reporting/menu backend cases across overlapping runs,
Java 21 packaging, and desktop/phone reporting, navigation and admin checks.
Actual repeated-result CSV download passed locally. Direct comparison with the
pinned mock covers the catalog, selection, filters, review and queue. The settled
admin layout has consistent typography and does not overlap the pinned sidebar;
the browser check waits for that geometry before capture. Public deployment and retained log-mount verification now pass, as recorded below. This does not close T021/T028, source activation,
multi-instance isolation or human acceptance.

The [public workspace](https://reporting.catalyst.openelis-global.org/reports/custom-data-export)
is now on frontend/backend/configuration `65f96697e428b3e45c1e9b293115ccf0e2c135f7`,
deployment `20260914T151244Z-65f96697e428`. Runtime configuration `7780ee2cd9`
and review tooling `7356f1d32c` remain. The database container and report volume
were retained, as were the previous artifacts, compose/identity files, a database
backup and existing logs. One application context started in 459.508 seconds;
the duplicate legacy API context returns 404. Served HTML/assets match the
versioned manifest. Mounted menu SHA-256:
`35c3995db966ef8c1d4897b430cb317b33cb2021422ac16d8a61dfb08302e93e`.

Nine distinct public workflows pass: spreadsheet repeats; detailed identities;
per-test turnaround in both layouts; shared report use/update/copy/delete;
desktop/phone column interactions and accessibility; configured sidebar,
review parameters and history; admin typography/content boundaries; failed retry;
and expired re-run. The initial nine-check batch had eight passes and one
spreadsheet timeout at the overall 30-second test limit immediately after
startup. Its stored job did complete, 8.819 seconds after submission. The
unchanged spreadsheet repeat and both recovery workflows then passed in a
four-check run including login. Keep the initial timeout as a startup qualification
limit; do not report the initial batch as wholly green. Browser downloads and
seven committed job/definition events were independently verified in the retained
application logs, including immediate saved-report editing.

Direct public walkthroughs cover the visible section hierarchy, legacy groups,
canonical report address, retained draft, phone drawer close and desktop admin
return. The [live checklist](https://grist.openelis-global.org/uat/reporting.json)
now has six stories and 17 steps: RPT-S06 adds three navigation checks, preserving
all five prior stories and their stable step keys. Their 14 route references now
use the canonical reporting path so the review panel can match the existing
workflows to the new address; instructions and keys were preserved. It was published through the
review repository's existing Grist story tool using the already-configured host
connection after the AWS session had expired. No reviewer answers were submitted.
Exact-revision frontend CI passes; backend CI remains in progress at this record.
The full MVP and human acceptance remain open.

## Previous Public Stage — Queue Recovery, 2026-09-14

- Application: [Reporting UAT](https://reporting.catalyst.openelis-global.org/CustomDataExport).
  Frontend `0d65ccaac4ba46ac7fa76262368170a13fe7306d`; backend
  `d56922c11ed071e992b6e7288be1198efb009717`.
- Deployment `20260914T120001Z-runtime-7780ee2cd987`; runtime configuration
  `7780ee2cd98766d871f730f1cff361489e931e8c`; review tooling
  `7356f1d32cfbdea346f200b5f3b2bf05a48610b9`. The public
  [target identity](https://reporting.catalyst.openelis-global.org/__review/target.json)
  records both revisions, successful public browser checks and pending human acceptance.
- The runtime follow-up passes five public application workflows: spreadsheet
  repeats, failed retry, expired re-run, Reports navigation with Back/Forward/reload,
  and desktop/narrow column interaction and accessibility. Seven reported checks
  include authentication and the separate pinned-mock capture. Direct matched
  1280×900/390×844 comparisons preserve the collapsed catalog, Add/Added controls,
  ordered selection, filters, review hierarchy and mobile queue. Native OpenELIS
  chrome, configured catalog data, the agreed layout selector and visible pending
  functionality remain distinct from the mock's fictional preview controls.
- Connected: Sample & Testing in both layouts, instance-aware columns, every
  repeated result, per-test turnaround, shared report definitions, queue return,
  linked failed-job retry, queued cancellation and expired-report re-run.
  Referrals and Non-Conformance remain visible as not yet connected.
- Seven public reporting workflows passed on backend/frontend `d56922c11e`:
  spreadsheet repeats, detailed result identity/period, 30/90-minute per-test
  turnaround, shared report management, Reports navigation, failed retry and
  expired re-run. Both recovery flows check actual CSV contents, retained
  settings, reload and browser navigation. Login setup is excluded from the count.
- The direct walkthrough found false unavailable-field/filter warnings while a
  restored report's catalog loaded. Frontend `e5d9e85ef7` fixes that transient
  state, shows Carbon loading feedback and prevents progressing without a catalog.
  It preserves warnings and correction for genuinely removed fields. The two
  affected public workflows (expired re-run and Reports/native-date navigation)
  passed again after publication, with no unexpected browser-console errors.
- In-app public retry and expired re-run were exercised directly. Native keyboard
  entry preserved both May 5 dates, review survived reload, and the resulting
  report downloaded successfully. Its stored CSV was independently checked:
  Accession Number/Viral Load, two REPORTING-MVP-REPEAT rows with 450 each;
  SHA-256 `499ab005f03b3c02d0da1af52097f3f64b6f00599f839beadac3e577fe741e32`.
  Earlier in-app fill attempts were inconclusive; native keyboard entry resolved
  that automation limitation. These agent checks do not constitute human acceptance.
- The live [Grist checklist](https://grist.openelis-global.org/uat/reporting.json)
  contains five stories and 14 required steps. Revision
  `81ea1671067557c9faee6ac530eb7fbe714a215d35682ccf9d01434460be71f6`.
  RPT-S04 now includes reusable failed/expired examples. Existing keys and sibling
  stories were preserved. The live overlay displays the recovery story, all three
  instructions and the current deployment. No reviewer answers were submitted.
- The application recovery release retained a database backup and applied the
  additive cleanup-column/index migration to existing data. Only the new
  synthetic failed/expired jobs were added. The subsequent loading fix replaced
  only the web container; backend/database identities were retained. Deployment
  checked artifact hashes, served HTML/assets and backend session health before
  publishing the ready identity.

The preceding backend replacement took about 15 minutes to become ready. It ran
the existing Intel image on an ARM host and initialized the application twice.
The runtime correction below removes the duplicate application; the current
public startup took 457.170 seconds. The platform mismatch remains, so backend
replacements still have a substantial startup cost. Frontend-only publication
avoids that restart. Previous versioned artifacts, original runtime configuration
and the pre-recovery database backup remain available.

## Runtime Qualification — Qualified and Published, 2026-09-14

A local process-interruption run killed the app with two real jobs generating,
one queued job and two existing partial files. The database/output volumes were
retained. After restart, both abandoned jobs became FAILED with the interrupted
reason, the queued job completed, the preceding ready CSV remained byte-identical,
and a linked retry returned the expected two repeated 450 readings. Incomplete
outputs were rejected and both partial files were removed. Leases expired
naturally; the test did not modify their timestamps.

The run exposed a deployment discrepancy: Tomcat loaded the same WAR at the
explicit `/api/OpenELIS-Global/` path and the automatically discovered
`/OpenELIS-Global` path. A read-stall probe observed two export workers in one
container, contrary to the plan's one-worker limit. All 4,216 application class files
in the local WAR match the public `d56922c11e` artifact exactly; this is a runtime
configuration issue.

The scoped [runtime tools](../../projects/reporting-uat/README.md) disable Tomcat
application discovery while preserving the explicit native API contexts, and
remove the reporting proxy's replacement of the API prefix. The corrected local
instance logged one Spring root initialization, started in 212.652 seconds, and
a repeated probe observed one generating job with two queued jobs. Four real
browser workflows passed through the native API route: spreadsheet CSV, failed
retry, expired re-run and Reports navigation with Back/Forward/reload. The five
reported checks include authentication. The public host's narrow configuration
probe confirmed the same two discovery flags and prefix-removing API route; no
active reporting jobs were present before the update.

Runtime configuration `7780ee2cd9` is now public. nginx configuration validation
passed before the app/proxy replacement. The database container, report volume,
frontend `0d65ccaac4` and backend `d56922c11e` were retained. Tomcat logged exactly
one Spring root initialization and 457.170 seconds startup; the implicit second
application path returns 404. Native session/login and all five public workflows
above pass. The target identity records the runtime revision separately from
application artifacts. The unchanged five-story/14-step checklist and signed-in
review panel remain available; no human answers were submitted.

The original full-configuration read was rejected by automatic approval review.
A narrower probe succeeded and returned only routing flags, mapping counts and
active-job count. Server configuration contents remain on the deployment host.

Recovery fixture correction `e6b34a4d2a` passed its full backend CI run
`34834207814`, in addition to frontend and the actual E2E checkpoint. Source
preparation `0d65ccaac4` now also passes backend `34836215711`, frontend
`34836215623` and the actual E2E checkpoint `34837613118`. The runtime-tools
commit `7780ee2cd9` also passes backend CI `34840293019`.

A separate local retention check created and downloaded a new synthetic report,
verified its configured seven-day expiry, then shortened only that job's expiry
to 15 seconds. At the observed clock boundary, new downloads returned 410; the
scheduler marked it EXPIRED and removed its CSV while preserving row count,
file size, history and frozen settings. An unrelated ready download stayed
byte-identical. This is accelerated fixture qualification, not a seven-day soak
or an in-flight download race; the latter has service/database test coverage.
Multi-instance crash isolation and public
cancellation qualification remain open. These checks do not close all of T021/T028.

## Database Upgrade and Rollback — Qualified Locally, 2026-09-14

Two dedicated PostgreSQL 14.4 databases execute the complete application
changelog, then the actual versioned reporting changesets for rollback and
reapplication. No live application or shared test database is rolled back.
The five-check run includes two migration scenarios (27.038 seconds) and three
existing ORM/persistence checks (0.784 seconds), with no failures or skips.
The recovery update over 50,000 retained jobs took 61 ms in this local test;
that measurement is not a public deployment-time estimate.

- Fresh initialization creates all four reporting changesets and the expected
  job constraints/indexes. Full reporting rollback removes its job table, menu
  entry and added columns while preserving existing report payloads and other
  menu entries. Reapplying twice produces one menu entry and one recorded
  application of each changeset.
- The populated scenario upgrades 1,000 existing report definitions, then adds
  100 shared definitions and 50,000 jobs across all six states. The recovery
  update, its rollback and reapplication preserve every M1 job field, including
  frozen requests, owner/submission identities, retry lineage, timestamps and
  row/file metadata. All shared-definition fields, including last editor and
  version timestamp, remain unchanged. Submission uniqueness still rejects a
  duplicate, and the cleanup index is valid after upgrade.
- Recovery rollback removes only its cleanup marker/index; reapplication starts
  those markers empty. Full reporting rollback explicitly drops the job table
  and `updated_by` column. It is not a history-preserving application downgrade.
  The qualified recovery rollback does retain job history. File-volume restore
  is not exercised by these schema tests.

The tests use the real root changelog to prove registration and the existing
repository container/bootstrap pattern. They require no application changes or
public redeployment. Reproduction and rollback boundaries are recorded in
[quickstart.md](quickstart.md#database-upgrade-and-rollback-qualification-2026-09-14).
Multi-instance crash isolation, audit and human acceptance remain separate gates.

## Local Workload Qualification — Passed, 2026-09-14

T027 passes with the existing application artifacts and corrected single-worker
runtime. The public deployment above is unchanged; no workload data was seeded
there. The reproducible [runner and browser check](../../projects/reporting-uat/README.md#50000-result-workload)
exercise real submissions and downloads over 5,001 synthetic specimens, 10,001
analyses and 50,000 results. One specimen has 10,000 repeats. Independent CSV
oracles check every value, identity, turnaround and repeat multiplicity in both
layouts, alongside three ordinary two-result exports.

Two runs produced identical CSV bytes. The final run generated each large file
in about 46.1 seconds, observed a Java resident-memory peak of 1,501,904 KiB,
and completed 144 ordinary authenticated session/queue/catalog reads with no
failures (maximum 1.336 seconds). Atomic database observations found at most
one generating job; five active jobs were present before the sixth submission
returned 429. Each large query used 200 follow-up cursor fetches at the configured
250-row fetch size. Temporary query logging was restored after both runs.

Both browser workflows pass at 1280×900 and 390×844, including reload and actual
50,000-row downloads with identical hashes. The three reported checks include
authentication. Reviewed screenshots retain the mock's table/card structure and
download/details controls without horizontal overflow. Narrow captures are
scrolled to the selected real job; these supplement the previous matched
full-page mock comparisons, rather than establishing pixel identity. No
application interface changed in this iteration. Nine writer tests also pass,
including incremental consumption/output of a 50,000-result repeat set.

The workload browser file is registered in `core-performance`, verified by native
Playwright listing and execution. The packaged project-validator cannot resolve
the existing `CORE_PERFORMANCE_TESTS` constant; its diagnostic is a helper
limitation, not evidence of an unregistered test. No project configuration was
changed to bypass that limitation. Measurements, environment, hashes and
reproduction instructions are in [quickstart.md](quickstart.md#recorded-50000-result-run-2026-09-14).
These local results do not establish public performance or human acceptance.

The qualification-only change passes full Spotless and frontend formatting,
frontend lint, Python syntax validation, and Java 21 packaging with both test
skip flags. The separate nine-test writer run and actual browser run above
provide test evidence; the package build itself did not execute tests. No
application artifact changed and no redeployment was required for this test-only
increment. New commit CI is tracked separately from the passed runtime baseline.

## Source Preparation — Frontend Published, 2026-09-14

The shared builder starts newly chosen configured reports with zero columns, as
the pinned mock does. Users add their fields explicitly; restoring a draft or a
saved report retains its choices. The period explanation now uses the configured
date anchor instead of always claiming specimen collection dates. Both referral
date interpretations are covered without choosing the unresolved product default.

- 28 component checks pass. Four real-browser application workflows pass:
  configured-report CSV, hidden-filter removal, Reports navigation with
  Back/Forward/reload, and desktop/narrow column interactions and accessibility.
  A separate pinned-mock capture also passes. The six reported browser checks
  include authentication; actual downloaded CSV assertions are unchanged.
- Rendered comparisons use 1280×900 and 390×844. The application keeps the mock's
  empty selection, collapsed groups, Add/Added controls and ordered columns.
  Date guidance remains within the date card on narrow screens. The native
  OpenELIS shell and real instance catalog remain distinct from fictional mock
  data and preview controls.
- A parameterized Referral data query preserves linked result identities and
  referrals without returned results. Five database tests cover independent
  request/sent dates, local date boundaries across daylight saving, repeated
  results, referred-test/section filtering and invalid mapping rejection.
  The complete focused run passes 79 tests, including the preceding role-fixture
  sequence. No Referral report source is enabled by this preparation.
- Frontend lint and production build pass. Initial sandbox attempts could not
  launch Chromium or access Docker; the permitted runtime runs passed. Those
  environment failures are not counted as product checks.
- CI at `e6b34a4d2a` has passed frontend and the actual
  [end-to-end checkpoint](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/34834965016),
  verified through its commit status (not merely the shared-build workflow).
  Its full backend run subsequently passed. These results precede this preparation.
- Frontend `0d65ccaac4` is now public on unchanged backend `d56922c11e`.
  All three affected public workflows pass: explicit configured-report columns
  with CSV output, hidden-filter removal with CSV output, and Reports navigation
  with Back/Forward/reload. The reported four checks include login. The frontend
  update retained the backend/database containers and verified served artifact
  hashes. Backend, frontend and actual E2E CI for `0d65ccaac4` have since passed;
  no full-MVP or human
  acceptance is claimed.

At this checkpoint three questions were recorded. The first two were resolved
on September 14 from the existing source of authority, as recorded below; only
the rejection-date question remains pending:

1. Referral period: the original specification uses sent date, while the MVP
   data model and quickstart say request date. Both can be exported; the default
   inclusion rule needs resolution.
2. Referral repeats: the original one-row-per-analysis description conflicts
   with preserving each returned result on separate rows. Raw result identities
   are retained while the final row presentation remains open.
3. Recorded rejection dates: native RejectionController creates NcEvent with
   reportDate and specimen links but leaves dateOfEvent empty. The current MVP
   prohibits substituting reportDate. Including these real recorded rejections
   with an explicit date-basis label versus excluding them needs resolution.

At that checkpoint source activation and affected CSV expectations were paused.
Referral activation now proceeds using the pinned mock and the explicit repeat
preservation requirement; Non-Conformance date semantics remain paused. The frontend
is public; the new Referral data query is not deployed or exposed as a report.
T020/T022 remain incomplete.

## M2 Recovery Qualification

Draft [PR #4295](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4295) is stacked
on [M1 #4292](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4292), based on M1
`948cdf3b0a`. Neither PR has been merged. M1 CI passed at that revision.

- 67 focused reporting backend tests passed, including seven recovery database
  tests. Focused line coverage was 960/1151 (83.4%), not whole-application coverage.
  The tests cover idempotent retry/cancel, claim races, leases, stale publication,
  expiry and cleanup while an already-open download retains complete bytes.
- Full backend CI at `d56922c11e` subsequently ran 6261 tests and failed only the
  seven new recovery tests at `reporting.access.denied` during initial submission.
  They depended on the baseline admin's database roles, which other suite fixtures
  replace. The corrected tests create their own real user and explicit role grant.
  Running UserRoleServiceTest before the recovery tests reproduced all seven
  failures; the same 14-test sequence passes after correction. All 67 reporting
  tests also pass after that role fixture (74 tests total). Full-suite CI at `e6b34a4d2a`
  now passes, confirming the corrected fixture. Frontend and E2E CI passed at `d56922c11e`.
  The source-preparation checkpoint also passes backend, frontend and actual E2E CI.
- 26 component tests, frontend/hook lint, formatting, production frontend build
  and Java 21 packaging passed for the loading fix. Delayed-catalog and genuinely
  removed-field tests cover the corrected behavior. Its local expired-re-run
  browser check also passed. Backend tests were not repeated for frontend changes.
- Six local reporting browser workflows and matched 1280×900/390×844 mock captures
  qualified the recovery candidate before publication. Retry/Re-run use the mock's
  primary actions. Older deep links add a detail card above the paginated queue.
- A real local browser confirmed queued cancellation through the native Carbon
  dialog; the cancelled job stayed cancelled while two other jobs completed.
  A temporary read stall on the isolated synthetic database made the queued state
  reproducible. A repeatable public queued fixture and narrow confirmation check
  remain open; RPT-303 is therefore not yet in the live checklist.
- A real local scheduler recovered a seeded abandoned GENERATING job, removed its
  partial file and retained failed history. This is not a process-kill/restart test.

Remaining work: connect Referrals and Non-Conformance through the common engine,
qualify multi-instance crash isolation, audit events, public cancellation UAT and
human acceptance. M1/M2 and the full MVP remain open.

Evidence is retained in the task's `reporting-m2-recovery` artifacts: public/local
logs and captures, pinned mock captures, deployment receipts, exact synthetic CSV,
runtime/cancellation receipts and the published checklist. The preceding public
stage was frontend `1f2093054e` with backend `ebc6983898`; its ten workflow checks
and five-story/12-step checklist remain historical evidence, not the current receipt.

## Frontend Repair — Qualified and Published

The canonical mock is pinned at `5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f`.
The repair restores overview cards, the three-stage builder, collapsed catalogs,
field/group search, explicit Add/Added selection, pointer and keyboard ordering,
mobile panes, structured review and the queue table. Route state is owned by
React Router query parameters; owner-scoped session drafts hold unsaved inputs;
TanStack Query manages server records and mutation invalidation. Loading a saved
report starts with fresh dates; reload and Back/Forward preserve an active draft.
Delayed submission responses cannot attach to a different new draft.

The local 21-case component suite, frontend lint (including hook dependencies),
frontend production build, Java 21 packaging and Spotless checks passed. Direct
1280×900 and 390×844 mock comparisons found and corrected mobile wrapping,
review hierarchy, pending-help contrast and mobile queue action placement.
Pointer/keyboard order, both repeated readings, per-test 30/90-minute turnaround
and independent downloads by two report users were verified against actual CSVs.
The corrected queue and review were also compared at both widths, including the
review/save controls below the fold. A local preview against the unchanged public
backend passed the full-screen accessibility and navigation check plus the pinned
mock capture (three checks including login). The public verification receipt
above establishes deployment separately from this preview evidence.

The required clean packaging build invalidated the local server's directly bound
`target/OpenELIS-Global.war`; two subsequent browser invocations stopped in login
setup and did not execute their product checks. The local app alone was recreated
to load the new WAR; database and report volumes were retained. Do not repeat a
clean build against a directly bound build artifact. Use a separate build output
or a versioned runtime artifact for future backend packaging checks.

The final candidate passed eight reporting browser workflows against the stable
public backend through a loopback frontend preview, plus the separate pinned-mock
capture. The earlier local turnaround and two-user checks also passed. One
candidate test initially read an HTML redirect using a separate API client;
it now selects the comparison test through the visible Tests listbox. A subsequent
selector error included the language menu; scoping to the Tests listbox resolved
it. The unchanged hidden-filter and CSV expectations pass on the final test.
No remaining product failures were observed in these checks. This remains a
stage qualification, not the full-MVP acceptance or a new exact-head CI result.

The frontend-only publication and Grist instruction update are complete at the
current receipt above; the previous deployment below is retained as history.

## Previous Public Stage — 2026-09-14, Before Frontend Repair

- Application: [Reporting UAT](https://reporting.catalyst.openelis-global.org/CustomDataExport),
  revision `ebc6983898c833ed40fe43003192c4079e4bab73`.
- Deployment: `20260914T065024Z-ebc6983898c8`; review tooling
  `7356f1d32cfbdea346f200b5f3b2bf05a48610b9`. The public
  [target identity](https://reporting.catalyst.openelis-global.org/__review/target.json)
  records stage scope and pending human acceptance.
- Nine public browser checks passed in 2.7 minutes, asserting actual downloaded
  CSV headers, repeated results, detailed identities and per-test turnaround.
  The public run uses a 60-second overall case budget; the existing 20-second
  download-readiness assertions and CSV expectations were unchanged. An earlier
  30-second case timeout was reproduced and diagnosed before this passing run.
- All 13 PR checks passed for that application revision. Earlier local evidence
  remains separate: 60 distinct focused backend checks, 10 component checks,
  10 browser checks, Java 21 and frontend production builds.
- Existing report user A saved a report in the public browser. Existing user B
  found and opened it in the public browser with blank dates and the fresh-date
  prompt. User B's API-generated CSV preserved the reordered headers and both
  independent Viral Load values of 450. This supplements the nine public browser
  checks; it is not counted as a tenth browser check. No accounts were created.
- The live [Grist checklist](https://grist.openelis-global.org/uat/reporting.json)
  has five independently selectable stories and 11 required steps, with no
  computed authoring problems. Checklist revision:
  `fdaaab0e4cfa622719cb450ee9579e887d4834b2f788896f0643daa6701939c8`.
- A real authenticated browser submission was read back from Grist: submission
  33, answer 118, `RPT-200` marked Fail for the observed missing Referrals and
  Non-Conformance choices. Reviewer name is explicitly `Codex preflight`;
  this is a known development gap, not human acceptance. The downloaded review
  report was inspected and matches the deployment, application, tooling,
  checklist and actual page. The answer survived a page reload.

Public synthetic fixtures cover May 5 repeats and May 6 turnaround of 30 and
90 minutes. Reports, database data and configuration checksums are persistent.
The isolated `reporting-uat` deployment is under `/home/ubuntu/reporting-uat` on
Catalyst. Versioned release artifacts sit under `releases/<application SHA>`;
`compose.json` selects the release, and `runtime/identity/target.json` records
the qualified deployment. Subsequent updates must preserve these volumes and
must not reseed the database. Existing Catalyst and CSiM stacks were preserved.

Delivery friction is still open: the observed cold start took about 18.5 minutes
with an emulated backend image and two application contexts. This does not
prevent current UAT, but native runtime and single-context deployment need a
bounded follow-up. No automatic deployment-on-push workflow is established yet.
Publish each usable application checkpoint and rerun these same workflow checks;
do not wait for the whole MVP.

Remaining acceptance: M1 qualification (including 74.4% backend instruction
coverage against the >80% target), Referrals, Non-Conformance and queue recovery.
The exact human walkthrough remains open, including native date entry: the
in-app agent's date-input interaction was inconclusive, while the ordinary
Playwright workflow succeeded. This is not evidence of a product date defect.
No claim of full MVP or human acceptance follows from this deployment.

Evidence is retained in the task artifacts as `reporting-public-validation.log`,
`reporting-public-ci.json`, `reporting-existing-users-validation.json`,
`reporting-shared-public.csv`, `reporting-public-target.json`,
`reporting-public-checklist.json` and `reporting-public-review-preflight.md`.

## Acceptance-driven Iteration 1

Outcome: select instance-configured Sample & Testing columns in native Reports,
generate a persisted job and download both layouts without losing repeated
results. Do not close this iteration until browser downloads agree with the
fixture records. Unit tests alone do not complete it.

Current evidence on 2026-09-13:

- Three source integration tests pass, including the actual source writing
  both layouts from two identical readings with distinct identities.
- Three Carbon component tests pass: selecting/reordering columns, retaining
  the draft and request identity after rejected submission, and retaining each
  layout's columns and dates through queue navigation.
- The backend packages and frontend builds. The real worker now completes
  browser-submitted jobs, and browser-triggered downloads pass content checks.
- The isolated local test database successfully applied the reporting migration.
  Public Catalyst seed scripts reproduced the manifest's 96 synthetic patients,
  1,152 results and nine test types. A separate reporting fixture adds one
  synthetic patient/specimen with two independent finalized readings of 450.
- Three focused browser tests are registered in `core-app` for repeat
  preservation, distinct detailed-result identities and zero-row downloads.
  Their fixture is part of the existing test fixture loader. On 2026-09-13 at
  19:36 UTC, all three plus native login passed against the running local app
  (four checks in 41 seconds). The checks assert actual downloaded bytes,
  UTF-8 BOM, the exact previewed column order, both identical 450 readings,
  distinct detailed result IDs and header-only output for an empty period.
  Evidence: `/private/tmp/reporting-iteration1-browser-ready.log`.

This proves the first complete local builder-to-download increment. Remaining
Sample & Testing acceptance includes entry through the Reports menu, broader
field coverage (including turnaround measures and additional questions),
configuration variation, date/status boundary cases, draft validation and
source/database workload qualification. It does not complete M1 or deployment.

## Acceptance-driven Iteration 2

Outcome: enter Custom Data Export through the native Reports menu, reject
invalid periods before submission and restore a reviewed draft after a queue
visit, navigation and full page reload.

Current evidence on 2026-09-13:

- Five focused component checks pass for dynamic selection and ordering, both
  layouts, inline download, retained choices after a rejected submission,
  inclusive 90-day validation and reviewed-draft restoration.
- The native menu migration was applied to the isolated database. The current
  menu API returned Custom Data Export under Reports, and a browser user entered
  the builder through that menu.
- Two focused browser checks (authentication plus the workflow) passed in 18.8
  seconds. They proved reversed and 91-day periods are rejected, an inclusive
  90-day period is accepted, and review state, dates and ordered columns survive
  a queue visit and full reload.
- The current frontend build passes. The reviewed builder screenshot is retained
  in the Playwright result for the workflow.

This completes the local navigation, period-validation and session-draft
increment. Shared definitions, wider Sample & Testing field coverage, access
cases and complete queue behavior remain open.

## Acceptance-driven Iteration 3

Outcome: create and reuse instance-shared report definitions without saving a
date range, support confirmed update/copy/delete, and reject a genuinely stale
edit without losing the draft.

Current evidence on 2026-09-13:

- Shared definitions use the existing `report_definition` store under the
  distinct `CSV_SAVED` type. The registered Liquibase change adds updater
  attribution and applied successfully to the isolated database.
- Four service checks cover date-free creation, shared searchable listing,
  optimistic update conflicts and versioned soft deletion. Eight component
  checks cover the builder plus save/reopen/fresh-date/conflict/copy/delete
  behavior.
- The first real-browser attempt exposed that an update response must carry the
  database's post-flush version. A later attempt exposed a cached-card race that
  could submit the pre-update version to delete. Both defects were fixed without
  weakening conflict detection.
- A clean qualification run passed authentication plus all five reporting
  scenarios in 26.2 seconds against the packaged backend, current production
  frontend bundle and real isolated PostgreSQL database. The shared scenario
  created a named report, reopened it with blank dates, updated it, saved a copy
  and deleted both definitions.
- All 29 focused reporting backend checks pass, including database migrations,
  source mapping, repeated results, persistence and CSV behavior. The production
  frontend build and all eight focused component checks pass.

This completes the local shared-definition increment. Wider Sample & Testing
field coverage, access/admission cases and the M2 source/recovery behavior remain
open; no deployment or human UAT conclusion follows from this local result.

## Acceptance-driven Iteration 4

Outcome: broaden Sample & Testing to the mock's routine reporting fields and
prove that configured questions, access checks and job admission use current
instance state.

Current evidence on 2026-09-13:

- Eight database-backed source checks pass. They cover linked common/patient
  fields, independent repeated
  results in both layouts, specimen-bound dates, configured-component identity
  through a rename, corrected finalized results, the five turnaround intervals,
  received time, the ordered-test count, dictionary qualifiers, grouped
  multiselect values and verbatim free text.
- The source catalog now discovers described observation-history types from the
  instance. A database-backed check creates a new question without changing
  reporting code, exports literal, dictionary and localized-key answers, joins
  multiple answers, renames it and proves that the stable field identity is
  retained while the current label is shown. A separate check configures and
  exports a second test/component arrangement through the same source.
- Three access checks cover ordinary report access, missing/inactive users and
  requested laboratory scope. Five job checks cover immutable accepted
  requests, idempotent submission, changed-request conflicts, configured active
  job limits, owner-only download and revoked-scope download denial.
- A four-case database check uses two simultaneous transactions at an active-job
  limit of one. The owner-row lock serializes them, yielding one accepted job and
  one 429 response; the submission uniqueness and immutable-request checks also
  remain green.

This is a verified backend increment. Browser comparison of the new columns and
ordinary-user access-negative cases remain required before M1 is complete.

Source review also identified two unproven combinations for the next increment:
the spreadsheet writer retains the first specimen record's attributes even
when later results have different turnaround times, and the question query
includes all observations matching the patient, including those attached to
other samples. The current single-result/single-sample field oracles do not
validate these combinations. Add explicit multi-result/multi-sample oracles and
resolve their output semantics before closing T010 or field acceptance. The
question-scope defect is resolved by Iteration 5 below; turnaround remains open.

## Acceptance-driven Iteration 5

Outcome: configured answers in each exported specimen row come from its own
order and specimen, including when another order belongs to the same patient.

Current evidence on 2026-09-13:

- The first fixture attempt exposed an existing constraint: observation history
  requires an order ID. Patient and specimen links are optional. The regression
  fixture was corrected to use valid stored relationships, without changing the
  application schema.
- The regression then reproduced the defect in actual CSV: both specimen rows
  contained the other specimen's answer and an answer from another order for the
  same patient.
- The source now selects the current order's answers and limits specimen-linked
  answers to the current specimen. Two regression checks cover exact CSV content
  in both layouts and an order with no linked patient.
- All 45 focused reporting backend tests pass in 47 seconds. The output is
  retained at `/private/tmp/reporting-answer-scope-green.log`; the reproduced
  failing assertion is in `/private/tmp/reporting-answer-scope-red.log`.
- The backend build/install and regenerated coverage report pass; build output
  is in `/private/tmp/reporting-answer-scope-build.log`. Both required
  formatters ran, with no frontend changes produced.

The turnaround ambiguity is paused for the user's decision: separate per-test
turnaround columns (recommended) or overall sample turnaround ending with the
last included test. The current first-record attribute behavior is not accepted
as correct. T010 and T011 remain open. No new browser or deployment validation
has been performed for this source correction.

## Acceptance-driven Iteration 6

Outcome: two ordinary report users independently generate the same report from
an instance-shared definition without extra setup in the reporting workflow.

Current evidence on 2026-09-13:

- The focused browser fixture creates two temporary accounts through the
  existing user-management API. Both have only the existing Reports laboratory
  role, assigned to All Lab Units, and no global roles. Setup uses the current
  instance's role catalog and naming rules; no reporting permissions were added.
- The first user signs in through the login page, enters Reports → Custom Data
  Export, selects patient information, reorders columns, saves a shared report
  and downloads a CSV containing both independent readings of 450.
- The browser's cookies and local/session storage are cleared before the second
  user signs in. That user finds the saved report, reopens it with blank dates,
  supplies a fresh period and downloads exactly the same headers and rows.
  The shared definition is deleted through its visible confirmation afterward.
- The new scenario plus authentication passed in 27 seconds. The complete
  focused file then passed all seven checks in 44.5 seconds against the packaged
  `eaf54b1e20` backend and existing production frontend bundle. Evidence:
  `/private/tmp/reporting-shared-users-browser.log` and
  `/private/tmp/reporting-shared-users-full-browser.log`.
- Download bytes, CSV attachments and the completed-report screenshot were
  inspected. Reporting scenarios had no page errors or failed application
  responses. Authentication setup logged the existing self-signed-certificate
  service-worker warning; reporting scenarios explicitly block service workers.
- The final two-user run passed again in 28.5 seconds after normalizing the
  screenshot scroll position. The corrected image was inspected. Project
  registration, lint, both required formatters, build/install and ten edited
  local document links pass. No backend tests were rerun for this test/document
  checkpoint; the unchanged backend retains its 45-test result from Iteration 5.
  Final browser output: `/private/tmp/reporting-shared-users-final-browser.log`.

This closes the ordinary-user flow gap in T005 and proves local second-user
shared reuse. It does not complete broader field/configuration qualification,
the remaining access-negative browser cases, the turnaround decision, M2,
deployment or human UAT.

## Acceptance-driven Iteration 7

Outcome: add a report through deployment configuration, select its configured
defaults and instance fields in the existing builder, and download its repeated
results through the existing queue.

Current evidence on 2026-09-13:

- `reporting-sources/*.json` is registered with the existing configuration
  initializer and persists versioned `CSV_SOURCE` records in `ReportDefinition`.
  Loading is idempotent. Invalid mappings, fields, catalogs, filters, layout
  defaults, conflicting report kinds and unversioned edits cannot replace a
  working definition.
- Spreadsheet defaults no longer append every test unconditionally. A definition
  selects its ordered common fields and can explicitly expand `catalog:tests`
  or another allowed group. Sample & Testing retains its previous effective
  defaults through this explicit configuration; its definition version is now 2.
- Two regression checks failed before the implementation. Six real-database
  configuration cases then passed: actual file loading/checksum skips, same-source
  CSV output in both layouts, dynamic defaults, versioned updates, invalid-update
  preservation and report-kind collisions. The test context excludes startup
  configuration, so these checks instantiate the real initializer with the
  actual Spring-managed handlers rather than changing the shared test context.
- All 53 focused reporting backend tests pass, as do Java 21 build/install,
  both required formatters, 44-file reporting Spotless validation, browser lint,
  project registration and edited local document links. Reporting instruction
  coverage is 4,069/5,773 (70.5%); it remains below the feature goal.
- The isolated application loaded `sample-summary.json` from its normal mounted
  configuration directory. Startup took 359.5 seconds. The local configuration
  parent initially belonged to root; assigning it to the existing Tomcat account
  allowed the subsequent startup load to save its checksum. No shared server or
  other application was changed.
- All eight focused browser checks passed in 1.1 minutes against that packaged
  backend. The additional report initially selects only Specimen ID and Accession
  Number while exposing instance tests. The browser adds Viral Load and downloads
  exactly two independent rows with value 450, then finds Sample summary in the
  common queue. Screenshot and downloaded CSV were inspected.
- Logs retain the known self-signed-certificate service-worker setup warning and
  request-cancellation messages. Downloads passed exact byte/content checks; both
  saved-report deletions associated with cancellation messages were independently
  confirmed inactive in the database. No page exceptions were logged.

Evidence: `/private/tmp/reporting-source-config-red.log`,
`/private/tmp/reporting-source-config-integration-2.log`,
`/private/tmp/reporting-source-config-all.log`,
`/private/tmp/reporting-source-config-build-2.log` and
`/private/tmp/reporting-source-config-browser.log`.

This proves configuration-loaded fields/defaults and same-engine execution.
T009 remains open for complete catalog qualification, including configured
filter subsets in the builder/request path. T010/T011 remain open for the
turnaround decision. Remaining M1 qualification, M2, deployment and human UAT
are unchanged.

## Acceptance-driven Iteration 8

Outcome: optional filters follow the report configuration. Previous choices from
another report or saved definition cannot silently restrict its output.

Current evidence on 2026-09-13:

- The builder shows configured filter controls only. Unsupported prior choices
  are omitted from saving and generation, with a notice before review. Review
  shows the effective filters; returning to the original report restores its
  supported prior choice. Empty optional-filter configuration retains mandatory
  dates and Sample & Testing's finalized/all-tests defaults.
- Real stored-configuration tests verify that generation and shared-definition
  requests reject unsupported lab-section, test and status values. These checks
  and the component regression failed before the implementation.
- All 54 focused backend tests, all nine component tests, Java 21 build/install
  and the production frontend build pass. All nine real-browser checks passed
  in 1.4 minutes against the updated isolated application.
- The new browser case selects a non-Viral-Load test, switches to the loaded
  Finalized sample summary, verifies optional controls are absent and the notice
  appears, and downloads both independent Viral Load readings of 450. Returning
  to Sample & Testing verifies that the previous test is still selected. CSV
  bytes and screenshot were inspected.
- The full browser run also rechecks both layouts, empty output, dates/navigation,
  shared CRUD and independent reuse by two ordinary users. No page exceptions
  were logged. Known local certificate/service-worker warnings and request
  cancellations remain; the three deletions associated with cancellation logs
  were independently confirmed inactive in the database.
- The normal formatter skips directories named `reports`, including this
  component. The two changed component files were explicitly formatted and
  checked with `--ignore-path /dev/null`; their nine tests and production build
  then passed again. The normal formatter, 44-file reporting Spotless check,
  browser lint, registration and edited local links pass. Reporting instruction
  coverage is 4,204/5,836 (72.0%), still below the feature goal.

Evidence: `/private/tmp/reporting-config-filters-component-red-2.log`,
`/private/tmp/reporting-config-filters-backend-red.log`,
`/private/tmp/reporting-config-filters-backend-green.log`,
`/private/tmp/reporting-config-filters-component-final.log`,
`/private/tmp/reporting-config-filters-build.log`,
`/private/tmp/reporting-config-filters-frontend-build-final.log` and
`/private/tmp/reporting-config-filters-browser.log`.

Together with Iteration 7 and the instance-field mapping evidence, this closes
T009. Turnaround, the remaining M1 qualification, M2, deployment and human UAT
remain open. The turnaround decision was resurfaced in the question tool and
answered after this checkpoint: show turnaround beside each test, preserving
repeat-specific durations and common specimen collection-to-receipt time.

## Iteration 9: Per-Test Turnaround and Correct Rerun Downloads

The user selected per-test spreadsheet durations, with each repeat retaining its
own times and collection-to-receipt remaining specimen-level. The source now
exposes optional test/component duration catalogs; generic result intervals
remain in the detailed list. Built-in source version 3 captures the changed
meaning. Existing stale-column handling rejects obsolete common spreadsheet
intervals on both submission and save.

The first five real-database cases failed before implementation, then passed:
two different tests, repeated analyses with different times and equal values,
duration-only selections with missing/negative intervals, renamed components,
and layout/default catalog separation. A sixth case verifies rejection of old
spreadsheet duration selections. All 59 existing/new reporting cases passed;
the final six-case turnaround run passed after adding that compatibility case
(60 distinct backend cases across the two runs).

The browser scenario selects and orders turnaround beside Viral Load, downloads
two equal values with 30/90-minute intervals, then switches to the detailed list
and compares the same intervals. Its first run exposed a real rerun defect: the
previous ready file remained clickable while the new submission was pending.
A component regression reproduced it, including failed submission and retry.
Starting another run now clears the current-file reference while preserving
inputs and retry identity; prior jobs remain in the queue. The ten component
checks and all ten browser checks now pass (browser: 1.6 minutes).

The new May 6 fixture is separate from the original May 5 records; the empty
period check now uses May 7. Loading the complete SQL fixture exposed and fixed
an earlier `report_definition.lastupdated` typo (`last_updated` is the actual
column). The full fixture now loads transactionally. The browser failure was
not bypassed with a wait or weaker CSV comparison.

Java 21 packaging, production frontend build, both formatters, 45-file reporting
Spotless validation, browser lint/project registration and local document links
pass. Reporting instruction coverage is 4,417/5,940 (74.4%); the feature coverage
goal remains open. Download bytes, successful screenshot and browser logs were
reviewed. Local service-worker certificate warnings and expected request
cancellations remain separate from test failures.

Local startup encountered a migration lock owned by the removed container.
Inspection proved the owner absent and no active transaction; releasing only
that exact abandoned lock let startup finish. The quickstart now requires
stopping a host-mounted-WAR runtime before replacing its WAR to avoid the
Tomcat/recreate race. This affected only the disposable local application.

Evidence: `/private/tmp/reporting-turnaround-red.log`,
`/private/tmp/reporting-turnaround-green.log`,
`/private/tmp/reporting-turnaround-boundary.log`,
`/private/tmp/reporting-rerun-component-red-2.log`,
`/private/tmp/reporting-rerun-component-green.log`,
`/private/tmp/reporting-turnaround-browser-final.log`,
`/private/tmp/reporting-turnaround-build.log` and
`/private/tmp/reporting-rerun-frontend-build.log`.

This resolves the known turnaround formatting defect and closes T011. Other M1
qualification, M2 and full MVP acceptance remain open.

## Delivery Correction: Continuous Public UAT

The user explicitly corrected the delivery sequence: UAT is a live reflection
of development stages and finds issues during implementation. Automated
end-to-end validation and human UAT use the same workflows, fixtures and
expected results. Waiting for the full MVP before publishing was a planning
error; the plan, tasks, quickstart and UAT contract now remove that dependency.

The working stage was subsequently published at the stable Catalyst reporting
hostname; see the current receipt above. Update that same target at subsequent
usable checkpoints. Full MVP criteria remain intact.

## Historical M1 Acceptance Checkpoint

This historical ledger predates the later qualification recorded above. Use
[the review stopping point](review-stopping-point.md) for the current packaging
and merge-readiness gate; do not treat these historical open rows as fresh status.

The implementation is reviewable as a foundation, but M1 is not complete. The
following ledger is the gate for continued work:

| Capability                                             | Current evidence                                                                                                                                                                               | State                                                                 |
| ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| Native Reports entry                                   | Menu migration plus browser navigation                                                                                                                                                         | Proven locally and in public browser checks                           |
| Instance-derived tests/components/questions            | Database checks add/rename a question and configure/export a second component arrangement without reporting-code changes                                                                       | Proven locally at source level; deployed configuration UAT remains    |
| Additional report configuration                        | Initializer loads versioned definitions; browser verifies fields/defaults, filter subsets, repeat downloads and the same queue                                                                 | Public configured-source browser checks pass; human review open       |
| Spreadsheet download                                   | Browser compares downloaded bytes with two independent identical fixture readings                                                                                                              | Proven locally and publicly for the current fixture                   |
| Detailed-list download                                 | Browser compares two distinct result identities and values                                                                                                                                     | Proven locally and publicly for the current fixture                   |
| CSV contract and streaming                             | Nine focused writer checks include BOM, escaping, nulls, ordering, zero rows, repeats and 50,000 streamed records                                                                              | Proven at formatter level; database workload remains open             |
| Date validation and retained draft                     | Component and browser checks cover the inclusive limit, invalid ranges, queue visit and reload                                                                                                 | Proven locally and in public browser checks                           |
| Immutable jobs and owner submission identity           | Persistence/service checks plus simultaneous database transactions cover idempotency, conflicts, configured limits, ownership and current scope; two ordinary users complete real browser jobs | Public existing-user flow and API reuse pass; human review open       |
| Shared saved reports                                   | Service, component and real-browser create/reopen/update/copy/delete checks; second report user reopens with fresh dates and downloads identical output                                        | Public two-user browser reopen and API export pass; human review open |
| Wider Sample & Testing fields and additional questions | Database checks cover linked common/patient fields, received time/count, five turnaround measures, corrected results and configured literal/dictionary/key/multiple answers                    | Proven at mapping level; browser comparison remains open              |
| Queue lifecycle and recovery                           | Submit, generate, poll and download work                                                                                                                                                       | Open; retry, cancel, recovery, expiry and audit remain M2             |
| Referral and Non-Conformance definitions               | Not implemented                                                                                                                                                                                | Open in M2                                                            |
| Catalyst deployment                                    | Exact application revision deployed; nine public browser checks, authenticated review submission and downloaded review report verified                                                         | Current stage live; full MVP and human acceptance open                |

Do not expand to M2 or describe M1 as complete until the M1-open rows required
by T002, T004-T006 and T008-T017 have passed. A draft M1 PR is the
review checkpoint; it does not change or narrow the accepted scope.

The Catalyst manifest explicitly declares the cohort synthetic. No evidence of
private clinical data was identified. An automatic review rejected a proposed
whole-database copy before execution; the public seed scripts reproduced the
cohort directly in the isolated database.

Iteration discipline: after two attempts with neither verified progress nor new
diagnostic evidence, stop implementation and reassess. Pause affected work for
ambiguities that change scope, data meaning, acceptance or deployment effects.
Never change acceptance criteria to make a check pass. Record the concrete
failure, evidence and next step before resuming.

Checkpoint discipline: do not begin another implementation increment while a
verified user-visible increment remains uncommitted or its acceptance ledger is
stale. The first working M1 slice requires a draft implementation PR. Later
commits update that same PR; they do not create a new scope or replace the M1
completion gate.

## Current Source

- Specification PR:
  [#4291](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4291), commit
  `02ebf606d9`, based on `e57a53399c`. The development baseline was refreshed
  and unchanged at implementation start.
- Implementation branch: `feat/479-ogc-479-reporting-mvp-m1-result-export`.
- The primary checkout has unrelated changes and was not used for
  implementation.
- The specification and clarification check-ins are complete. No further
  approval is needed to implement the agreed scope or perform the requested
  deployment. Merge authorization is separate.

## Implemented Foundation

- Two CSV layouts with captured headers, correct escaping/BOM/nulls, specimen
  grouping and preservation of independent repeated values. Actual event groups
  can align values; unrelated repeats remain separate without cross-products.
- Streaming writer retains at most the selected fields' pending first readings,
  rather than all repeats of a specimen.
- Inclusive date validation and laboratory-timezone query boundaries.
- Immutable job request persistence, submission uniqueness per owner, lifecycle
  transitions, ORM registration and a Liquibase migration with rollback.
- Typed source-configuration parsing, an instance-aware catalog and a shared
  source interface now connect to Sample & Testing execution. Shared
  saved-report editing is implemented. The existing configuration initializer
  now loads additional validated source definitions from deployment files.
- Existing `ReportDefinition` storage will hold source definitions and shared
  saved reports, using distinct CSV report types. Its current patient-report
  consumer selects `PATIENT` explicitly. Two initially drafted new definition
  entities were removed before any migration or commit.

## Historical Validation Checkpoints

| Check                         | Observed result                                                                                                                                                                                                             |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Specification checks          | Eight Markdown files, 36 local/source links, JSON example and requirement/task consistency passed                                                                                                                           |
| Baseline backend              | Java 21 build/install passed with both test-skip flags; first cache installation attempt needed normal filesystem permission                                                                                                |
| CSV writer and dates          | 12 focused JUnit tests passed; the repeat workload writes 50,000 rows incrementally                                                                                                                                         |
| ORM and lifecycle             | Three tests passed; factory startup is under five seconds and needs no database                                                                                                                                             |
| Source configuration          | Four tests passed, including an extra definition over the same supported source                                                                                                                                             |
| PostgreSQL job persistence    | Three tests passed against a disposable database using the repository test setup; migration `479-001-reporting-export-jobs` ran successfully                                                                                |
| Stored source relationships   | Two tests passed: configured component identity survives rename and repeated values; collection dates distinguish specimens under one accession                                                                             |
| Combined reporting validation | All 45 focused Java tests passed on 2026-09-13, including source, answer scope, access, concurrent admission, saved-definition and database checks                                                                          |
| Frontend component/build      | Eight focused component checks and the production frontend build passed on 2026-09-13                                                                                                                                       |
| Focused browser acceptance    | Seven Playwright checks passed in 44.5 seconds against the packaged app on 2026-09-13: sign-in, both layouts, repeats, zero rows, date/draft handling, shared operations and independent reuse by two ordinary report users |
| Formatting                    | Corrected absolute-path selector checks 42 reporting Java files; earlier relative-path invocations selected zero files and were not valid formatting evidence                                                               |

The numeric streaming test is a formatter test, not the complete source/database
workload qualification. Database checks prove immutable requests, per-owner
submission uniqueness, concurrent admission and lifecycle persistence. Worker
concurrency, migration rollback and production-volume qualification remain
pending. Shared-definition edits and browser flows have their separate evidence
above. Iteration 6 completes the ordinary-report-user application flow required
by T005; remaining access-negative browser cases are part of T016 qualification.

CI on `b31449b3f5` passed frontend static checks, its image build and the shared
E2E build, but backend CI stopped at six Java formatting violations. The local
formatter had accepted a relative-path filter that selected zero files. The
corrected filter reproduced all six failures, applied their formatting and
verified all 42 reporting Java files. Do not label this replacement checkpoint
CI-complete until its new backend run passes. At the `c0cbd56280` checkpoint,
frontend and downstream E2E CI subsequently passed; backend Build + Test was
still running when inspected during Iteration 5 on 2026-09-13. The Iteration 5 source
correction requires CI on its own pushed revision.

The replacement checkpoint's 43-case backend run and build/install pass. Test
output is retained at `/private/tmp/reporting-checkpoint-43-tests.log`; build
output is at `/private/tmp/reporting-checkpoint-build.log`. The concurrent test
now commits its cleanup separately and asserts that no active fixture job is
left behind. No browser rerun or deployment was performed for this test and
formatting checkpoint.

The regenerated focused JaCoCo report after Iteration 5 measures 3,540 of 5,417
instructions in the new reporting packages, or 65.3%. This is below the feature coverage goal.
Controller, worker and remaining source/recovery tests remain part of T004,
T005, T012 and M2; the draft checkpoint must not be presented as
coverage-complete.

## Progress and Drift Check

The 2026-09-13 status audit found execution drift: too much time went into
isolated foundations and test environment setup before the first usable
workflow. The unsupported privacy detour compounded that delay. A second audit
found that the drift had been reported without completing the corrective
checkpoint: implementation continued while its changes remained uncommitted and
the acceptance ledger lagged behind the passing browser workflows.

The correction is to freeze new scope, reconcile current acceptance evidence,
run the focused backend, component, build and browser checks, review the whole
change and publish a draft M1 PR before implementation continues. The accepted
scope remains intact. Specification PR checks do not validate this code.

## Server Inspection — Before Reporting Deployment

Read-only inspection on 2026-09-13 confirmed SSH access to
`catalyst.openelis-global.org`. The host runs existing Catalyst, OpenELIS, HAPI
FHIR and separate CSiM containers. At inspection it had approximately 16 GB free
disk and 14 GB available memory; refresh capacity before deploying.

The existing OpenELIS container is `catalyst-mvp-isolated-openelis-app`, with
backend port `127.0.0.1:28443`; its database is
`catalyst-mvp-isolated-openelis-db`. Their compose configuration lives under
`/home/ubuntu/catalyst-release/targets/catalyst`, with an override under
`/home/ubuntu/catalyst-release/compose`. The public proxy is
`catalyst-demo-caddy-1`, configured by
`/home/ubuntu/catalyst-demo/targets/catalyst/Caddyfile`.

At that inspection the public routes served Catalyst and dashboards; no public
OpenELIS route was found. The reporting route has since been added and verified
as recorded above, preserving the existing services.

## UAT Readiness — Current Stage

The stage is available at the public reporting URL. The Grist `reporting`
checklist, five-story picker, authenticated submission, retained answer and
version-bound downloaded review have all been verified. Full MVP and human
acceptance remain open. The current receipt above supersedes the initial
2026-09-13 observations of missing routes and checklist/backend mappings.

FR-023, SC-010 and T032–T038 apply at each usable stage. They do not create a
wait-for-full-MVP dependency. M1 and M2 acceptance still require their complete
functional and qualification evidence.

## Remaining Delivery Work

- Complete M1: qualify the remaining result/component/value and
  configured-field mappings, bounded access/admission behavior and the remaining
  native builder cases; verify real downloaded CSVs and M1 review evidence.
- Complete M2 through the same engine: referral/non-conformance mappings and
  definitions, retry/cancel/restart/expiry, full source/recovery/workload tests
  and the second milestone PR.
- D001 and D003 are complete for the current stage: isolated reporting stack,
  public route, backend readiness and browser sign-in verified.
- D002: The current release artifacts and persistent state are installed. Keep
  versioned releases for subsequent stages; improve the update/startup path and
  exercise rollback before claiming repeatable recovery.
- D004: Run all three reporting types, both Sample & Testing layouts, shared
  report reuse and queue/recovery checks against actual records on the deployed
  instance; compare downloaded CSV contents and record the final URL/revisions.

Do not mark the goal complete until the full functional specification and the
requested deployment are verified. Hindsight retrieval and initiative capture
were attempted but timed out; no retrieved memory was used as current evidence.

## Referral source connection — local validation before publication

The pinned mock explicitly names referral **sent date** as the period anchor.
The user's instruction to preserve every repeated result resolves the older
one-row-per-analysis shorthand: independent returned results remain independent
rows; a sent referral with no returned result has one row with empty returned
fields. These are existing decisions, not new assumptions or answers inferred
from elapsed time. Request date remains an explicit alternative configuration.
Non-Conformance's missing event-date/report-date decision remains unanswered.

The common source registry now loads built-in definitions from the reporting
resource directory. Referrals uses the existing builder, saved definitions,
queue and CSV writer. Stored dictionary/qualifier/numeric interpretation is
shared with Sample & Testing. Multi-select options retain their result/link
identities within a cell and are grouped only within the same returned timestamp,
original referral, test, component and result group; later returns remain rows.
Only sources that define a result-status default apply it. Referrals includes
pending records and does not claim a finalized-result filter in saved requests
or review.

Validation so far: 32 focused database tests and 28 reporting component tests
pass. The interleaved returned-date test first reproduced one row instead of two;
the review tests reproduced the misleading Finalized text before both repairs.
The repeatable synthetic Referral fixture loads on the local UAT database.
The final six-check local browser run passes: authentication, repeated-result
spreadsheet, per-test turnaround in both Sample & Testing layouts, configured
navigation, and the Referral saved-report/actual-CSV workflow at 1280×900 and
390×844. Direct rendered comparison with the pinned Referral mock found and
repaired the missing sent-date basis in review and the incorrect blue family tag;
Referrals now uses Carbon teal. Columns remain collapsed with Add/Added controls,
ordered selection, phone panel navigation and the same review/save structure.
Native OpenELIS chrome and the source's extra identity/destination fields are
retained. The final nine Referral database checks also pass after formatting.

Local evidence is `/private/tmp/reporting-referral-local-final-evidence`,
`/private/tmp/reporting-referral-mock-evidence`, and
`/private/tmp/reporting-referral-local-final.log`. The local app is
`23876680bd52`, the retained database is `f4572a3f704c`, and the restored frontend
preview serves port 18489. Startup took 210 seconds; it was observed without a
restart. The fixture was loaded twice without adding duplicate records.

This local record preceded publication. The current public stage above records
the subsequent deployment and its own browser checks; human acceptance remains
separate. RPT-201 in uat.md
now uses the actual three-row fixture and fresh-date shared-report workflow;
updating the live Grist checklist still needs its authoring connection.

## Natural queued-cancellation qualification — local, 2026-09-14

A new opt-in browser workflow uses the existing 50,000-result fixture to occupy
one real worker, then submits a normal May 5 report. It exercises Keep queued,
reload, confirmed cancellation and another reload. After the large job finishes,
the cancelled job remains CANCELLED with no start time, row count or file size;
a direct download returns 409. The actual large CSV is compared row-for-row with
an independent 50,000-row oracle, including equal repeated results. No worker
pause, database lock, artificial timestamp or queue-state injection is used.

The final local run passes three checks in 1.5 minutes: authentication and this
workflow at 1280×900 and 390×844. Evidence is
`/private/tmp/reporting-cancel-natural-local-verified-evidence` and its sibling
`reporting-cancel-natural-local-verified.log`. Initial test-authoring failures
were diagnosed from screenshots, browser traces and source: Carbon adds a
screen-reader danger label to the confirm button; the separate Node HTTP client
followed a local login redirect; and AppConfig omits null JSON properties.
Those test assumptions were corrected. Application code was not changed.
The expected 409 download rejection is the only final browser resource error;
there were no page exceptions. The application completed both 50,000-row jobs.

Direct inspection used the canonical queue captures at matching widths. The
implementation retains the same columns, status/action placement and mobile
labeled-row layout. Cancellation adds the existing native Carbon confirmation
required by the functional specification; the mock's Cancel action is immediate.
Desktop and phone confirmations and the resulting Cancelled rows were inspected.

`projects/reporting-uat/prepare-cancellation-workload.py` verifies the actual
stack, requires a ready identity for public Reporting UAT and an idle
worker, backs up the database, loads the idempotent
fixture and checks 50,000 unique results / 5,001 specimens / 10,001 analyses.
Its local run passed against the already-seeded data with application
`23876680bd52` retained; receipt
`/private/tmp/reporting-cancel-setup-local-20260914/verified.json`.
Public preflight found no active jobs and no records on the fixture collection
date. Public fixture installation and its own browser checks are next; this local
record does not close T021 or claim public/human acceptance.

## Configuration-default review repair — 2026-09-14

The direction audit found the primary builder ignored `defaultColumns` for
`SAMPLE_TESTING`, while other configured report types applied them. The bundled
Sample & Testing definition still selected every configured test. This made the
mock's deliberately empty starting selection a report-ID exception in the UI
rather than an instance-configurable choice.

The builder now applies catalog defaults uniformly. The bundled definition is
version 4 with empty defaults for both layouts, preserving the accepted empty
picker. Instance overrides can supply ordered defaults. An explicit empty draft
still takes precedence, so removing every field and reloading does not restore
unwanted defaults. Existing shared definitions and frozen jobs are unchanged.

Two new component regressions failed on the old report-ID exception and pass
with the repair; the complete component file passes 30 tests. The first backend
run exposed two validation rules rejecting empty default lists; those rules now
allow an explicit empty initial selection. Required layout keys, non-null lists,
valid field identities, and nonempty export/shared-report submissions remain
validated. The two database suites pass 15 tests, including an override of the
primary report through the existing configuration handler and rejection of an
export without selected columns. The configuration unit test's missing-key
fixture was corrected to account for JSON whitespace. Compiled browser
validation and publication remain required before calling the repair delivered.

The requirements checklist and plan now reflect resolved Referral semantics and
the existing ten-PR packaging contract. Historical entries above remain dated
evidence, not the current completion checklist.

## Shared-editor recovery correction — September 14, 2026

RPT-S02 now has a real two-editor browser regression: stale edits preserve the
winner, keep the losing draft, and allow a separate copy with independently
verified CSVs. Inspecting the public recording exposed a lingering conflict
warning after successful copying. A one-line correction and failing-first
component regression now pass 31 component checks and the compiled local desktop
workflow. Both builds, formatters and focused lint pass. See
[the code-QA findings](code-qa.md#shared-editor-recovery-correction--september-14-2026)
for the initial loading diagnosis, error-state fix and exact remaining work.

The expanded phone run was initially rejected because automatic approval review
exhausted its usage allowance. A fresh capacity check and normal retry succeeded:
two checks passed in 25.5 seconds, and desktop/phone screenshots were inspected.
The correction remains unpublished; public application 8005e4cc0b and its existing evidence remain the
current usable stage. No broad acceptance task or human review is closed.

## September 15 delivery checkpoint

Candidate `122dea228b01bf5bf148b22e39cbf7b72132675b` passes
[backend CI](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/34928675652),
[frontend CI](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/34928675761),
and [downstream E2E CI](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/34929254161).
The E2E workflow includes successful Cypress jobs, both Playwright Core shards,
both Harness shards, report merges and suite gates. This is distinct from the
earlier image-build checkpoint. All eleven stack PRs are conflict-free and
require reviewer approval. No merge is recorded as part of this delivery.

The public application's identity still reports frontend/backend `3de726b8d3`.
The later candidate adds test/documentation changes and upstream translations;
the recorded public workflow evidence retains its original tested revisions.
The external review widget now identifies itself as `ea85d65`; the two-user
recording proves the earlier `814d8341` widget, not a rerun against this update.
Human acceptance remains pending.

[The evidence index](https://reporting.catalyst.openelis-global.org/reporting-evidence/)
contains twelve bundles, including two-user reuse, Non-Conformance saved reuse,
and separate backend/frontend coverage results. Existing service tests establish
scope-denial behavior; the public browser workflow establishes cross-owner
job/file denial and authorized shared reuse. A duplicate browser matrix for
every service-level denial is not a remaining requirement.

The pinned mock's omitted “Preview controls” toolbar simulates user identity,
access and the next export failure (canonical HTML line 1289). It is demonstration
infrastructure, not an unfinished reporting feature. The native column preview
remains in `ReportingView.jsx`; fictional example rows are not actual report data.
The brief “Not yet connected” label during catalog loading remains a recorded
non-blocking usability finding. Full requirement reconciliation and human
acceptance must not be inferred from CI success alone.
