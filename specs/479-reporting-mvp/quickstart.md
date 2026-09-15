# Implementation and Acceptance Quickstart

This is the implementation test plan. Sample & Testing, shared saved reports
and the basic queue are implemented on the M1 branch. See [execution.md](execution.md)
for verified results and remaining gates. The mock's fictional download is
design evidence only.

## Start Implementation

1. Use the milestone's own worktree; inspect its branch, changes and related
   PRs. Read this package and current repository guidance.
2. Use Java 21 and existing workspace setup. Before Docker Compose, create and
   configure `.env` from `.env.example` if absent; preserve existing settings
   and data. Obtain credentials through `TEST_USER`/`TEST_PASS`, never from
   committed fixtures or documentation.
3. Use a disposable test database and the existing core application stack. No
   analyzer harness, bridge or separate AI service is required.
4. Establish independent record/value oracles in T002 before accepting the
   common catalog and Sample & Testing mapping. Seed through repository
   fixtures/builders and supported services. T020 applies the same discipline to
   the remaining sources.

M1 demonstrates the common builder, both Sample & Testing layouts, shared saved
reports and inline download with basic queue return visits. M2 adds the other
source definitions through the same engine and completes recovery/qualification.

## Instance-Aware Catalog and Shared Reports

Use two configurations with different tests, labeled result components and an
additional patient/sample question supported by the selected mapping. Confirm
that catalogs reflect each instance. Add or rename an item and verify that it
appears without reporting-code edits. Include duplicate labels with distinct
identities, inactive/retyped fields, patient information and fields beyond the
mock's seven-column example. Normal report users use their existing access
without another approval or permission-setup journey.

Save a named definition with ordered configured columns, layout and non-date
filters. Reopen it as a second report user, enter fresh dates and compare the
output with the definition. Check copy, confirmed overwrite/delete and
concurrent edits. Editing or deleting a definition must leave submitted jobs and
files unchanged. Reopening a definition with stale field identities explains
what needs correction. A later label rename must not change a completed file.

## Sample & Testing Dataset and Layout Oracles

Specify expected source identities and values independently of the export query.
Then specify the expected projection for each layout. Detailed-list rows and
spreadsheet rows need not have equal counts: compare represented identities and
value multiplicities as well as each layout's expected cells.

| Fixture                                                                    | Expected behavior                                                                                                                    |
| -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| One specimen with two different tests                                      | Spreadsheet has separate configured test columns; detailed list has one row per independent result                                   |
| One test with two independent results, including identical values          | Both values survive; spreadsheet adds rows and detailed list preserves two records                                                   |
| Two tests that both repeat                                                 | Every result survives without a cross-product or invented pairing between unrelated repeats                                          |
| Multiple specimens under one accession                                     | Preserve specimen grouping; eligibility follows each result's own specimen collection date                                           |
| Collection at either date boundary, including a daylight-saving transition | Include whole boundary dates in the laboratory timezone                                                                              |
| Outside period, null collection date or order-entry-only match             | Exclude from collection-date requests                                                                                                |
| Finalized and unfinished results; corrected values                         | Default filter includes finalized current values; explicit supported status filters behave as displayed                              |
| Components, grouped helper records and multi-valued storage                | Prove independent result identity and component linkage; do not mistake helper rows for additional clinical results                  |
| Numeric values with absent precision, dictionary and free text             | Independently verified logical values; no UI HTML or truncation                                                                      |
| Nulls, Unicode, comma, quote and newline                                   | Correct blank or escaped cells in UTF-8 CSV with BOM                                                                                 |
| Test/lab filters and identifying fields                                    | Requested values use existing access; denied scope is explained without silently reducing the request                                |
| No eligible records                                                        | Valid header-only CSV with output row count zero                                                                                     |
| Only specimen metadata selected                                            | Spreadsheet uses specimen grouping; detailed list keeps its stated result grain; repeated metadata is not display-value deduplicated |

At the service layer compare projected identities with the oracle. At download,
compare ordered headers and cell tuples including multiplicity. No hidden
result-ID column is required in the user's CSV; tests can check identity before
formatting. Verify correction/grouping rules through existing services, not
assumed status IDs.

Turnaround qualification uses different completion/release times across tests
and repeated analyses. The spreadsheet exposes optional `testTurnaround` and
`componentTurnaround` catalogs; select an interval and order it beside its test.
Compare both layouts against the same stored timestamps. Duration-only selections
must preserve repeats whose timestamps are missing. The synthetic browser
fixture has two Viral Load values of 450 collected on 2026-05-06, with
result-to-validation intervals of 30 and 90 minutes. Its original 2026-05-05
repeat fixture remains unchanged.

## Common Source Definition Qualification

Use the same builder, saved-report operations, worker and download for all three
mock types. T020/T022 must prove their actual relationships before source
acceptance.

| Source                                                   | Required proof                                                                                                                                                       |
| -------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Sample & Testing                                         | Specimen collection date, stable result/component identity and both layouts                                                                                          |
| Referrals                                                | Referral sent date, referral/result occurrence identities, every independent returned result and a pending row for a referral awaiting results                       |
| Non-Conformance / Rejections                             | `NcEvent.dateOfEvent`, recorded rejection dates where applicable, specimen links and distinct occurrence identity; no double counting linked event/rejection records |
| Additional configured definition over an existing source | New labels/defaults/allowed fields appear through configuration without report-specific frontend or queue code                                                       |

Configuration does not infer arbitrary database relationships. Prove each source
mapping and keep date/row meanings visible. An event report needs a meaningful
tabular layout, not an invented test pivot. Include a reference-range or
turnaround field against existing clinical rules, with missing-input cases.

### Load an Additional Report Definition

Place a JSON definition in `reporting-sources/` beneath the existing backend
configuration directory (default
`/var/lib/openelis-global/configuration/backend`). The standard initializer loads
this domain after test/component/question configuration, at order 500. Its
existing instance-directory and checksum behavior applies. Restart the application
or use the existing administrator configuration reload operation:
`POST /rest/configuration/domains/reload` with
`{"domains":["reporting-sources"],"force":false}`. No new reporting settings screen
is required.

Use [sample-summary.json](../../src/test/resources/fixtures/reporting-sources/sample-summary.json)
as a minimal example. The definition chooses a supported mapping, allowed common
attributes, dynamic catalogs, filters, layouts and ordered defaults. A default
such as `catalog:tests` expands that configured group in place using stable field
identities. Exposing a catalog does not automatically select it: the sample
summary exposes tests while initially selecting only specimen and accession.
The built-in Sample & Testing definition explicitly defaults to the tests group.

The definition's `filters` list controls the optional filter controls. For example,
[finalized-sample-summary.json](../../src/test/resources/fixtures/reporting-sources/finalized-sample-summary.json)
uses `filters: []`: users choose dates and columns, while the Sample & Testing
mapping keeps its finalized-result default across all tests and accessible
sections. Previously selected unsupported filters are excluded from review,
saving and generation, with a notice before the user reviews the effective
request. Direct requests containing unsupported non-default filter values are
rejected. Supported choices remain available when returning to the original
report draft.

Successful loads persist the definition by its stable ID in the existing
`report_definition` table with report type `CSV_SOURCE`. Repeating an identical
definition is idempotent. Changes require a higher `version`; invalid mappings,
unknown fields/catalogs/filters, defaults for the wrong layout and ID collisions
with another report kind are rejected before replacing a working definition.
Removing a configuration file does not delete its persisted definition, following
the existing initializer's load/update behavior. Do not use file deletion as an
unpublish operation.

Existing submitted jobs retain their captured definition. A changed definition
does not alter a completed file; a still-queued request that no longer matches
fails through the existing changed-definition check. Verify a fresh submission
after configuration changes.

For this reporting component, run the normal frontend formatter and also format
the changed component files explicitly with `--ignore-path /dev/null`. The
current `frontend/.prettierignore` entry `reports/` otherwise skips this source
directory as well as generated reports. Limit that explicit command to the
changed reporting source/test files.

For a local Tomcat runtime that mounts the worktree's WAR directly, stop the
isolated application before a build replaces that file. Replacing it while
Tomcat is watching can start deployment before a subsequent container recreate.
If startup waits on a migration lock, inspect its owner and active database
transactions first; release only a confirmed abandoned lock. Do not restart a
live startup merely because a readiness check times out.

## Browser Flow Inventory

Use Playwright **`core-app`**, a real backend/database and actual downloaded
files. Do not stub export APIs, authorize by changing frontend state or use the
mock as the system under test. Test-only failure controls may target the
worker/storage boundary on the disposable stack.

The shared-reuse browser scenario provisions two temporary accounts through the
existing user-management API using `TEST_PASS`. They receive only the Reports
laboratory role across All Lab Units, with no global roles. Generated usernames
use letters and each account has a unique full name to satisfy the instance's
existing naming rules. Browser storage is cleared between sign-ins. The test
removes its shared definition; temporary accounts remain in the disposable test
database. This fixture setup is separate from the stable human-UAT accounts in
[uat.md](uat.md).

| Flow                               | Actions and evidence                                                                                                                                             | Planned file                                                                      |
| ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| Native export                      | Reports → Custom Data Export; select source/columns, order, filter, review, submit and download in place; compare both layouts and zero-row CSV to fixtures      | `frontend/playwright/tests/foundational/core/custom-data-export.spec.ts`          |
| Instance configuration and editing | Change configured metadata; find new fields, switch layouts, reorder by keyboard, go back/return from queue; verify retained choices and narrow-screen usability | Same file, focused cases                                                          |
| Shared reports                     | Save/reopen as another report user, supply fresh dates, copy/update/delete and detect conflicting edits; verify past output remains unchanged                    | Same file, focused cases                                                          |
| Source reuse                       | Run Referral and Non-Conformance definitions and an extra definition over an existing source through the common flow                                             | Same file, focused cases                                                          |
| Existing access                    | Normal reporting without new setup; denied scope retains draft; another user's job/file remains inaccessible                                                     | Same file, focused cases                                                          |
| Recovery                           | Leave/return, fail/retry, cancel queued work, restart and restore expired choices with fresh dates; no partial download                                          | `frontend/playwright/tests/foundational/core/custom-data-export-recovery.spec.ts` |

Use the current Playwright planning/author/audit skills. Inspect components
before choosing selectors; prefer accessible roles/labels, Carbon interaction
guidance, event waits and retrying assertions. Run small groups; inspect console
logs and failure screenshots. Routine validation keeps video disabled; optional
recording uses the appropriate video project.

After authoring, validate each new file from the repository root:

```bash
python3 .ai/skills/playwright/scripts/validate-playwright-project.py frontend/playwright/tests/foundational/core/custom-data-export.spec.ts
python3 .ai/skills/playwright/scripts/validate-playwright-project.py frontend/playwright/tests/foundational/core/custom-data-export-recovery.spec.ts
```

Run one relevant file at a time from `frontend/`:

```bash
npm run pw:test -- playwright/tests/foundational/core/custom-data-export.spec.ts --project=core-app
npm run pw:test -- playwright/tests/foundational/core/custom-data-export-recovery.spec.ts --project=core-app
```

Use `npm test -- <implemented-test-path>` for focused Vitest tests and
the repository Maven mechanism for actual JUnit 4 classes. Validate ORM startup
without a database, and migrations/rollback against empty and populated
disposable databases. Follow current applicable build, formatting, coverage and
CI gates. Local test success does not establish live CI success.

## Recovery and Workload Qualification

Interrupt a generating worker and restart the disposable application. Queued
work must survive, abandoned work must fail visibly, another live worker must
remain unaffected, and no partial output may be downloaded. Exercise concurrent
submit/claim/cancel and expiry/download races at the database/service layer.

Use a controllable clock for retention. New downloads fail at expiry; cleanup
removes files while retaining history. Restored expired requests require fresh
dates. Verify the configured protected persistent output volume and that files
cannot be fetched as unauthenticated static content.

Run a reproducible 50,000-result dataset with independently expected counts for
both layouts. Capture query batch size, peak process memory, elapsed time,
worker concurrency and ordinary authenticated read behavior. Pass requires
complete correct output, no whole-dataset buffering, enforced worker/active
limits, no process exhaustion and successful ordinary reads. Report measurements
with environment details; this workload does not establish a universal
production service level.

### Database upgrade and rollback qualification, 2026-09-14

Run the focused real PostgreSQL migration scenarios and existing ORM/persistence
checks from the repository root, with Java 21 and Docker available:

```sh
mvn test -Dtest=ReportingMigrationRollbackTest,ReportingPersistenceTest
```

Each migration test owns a new PostgreSQL 14.4 container initialized from
`postgre-db-init` and the complete `liquibase/base-changelog.xml`. This proves
registration through the actual application includes. It then uses
`src/test/resources/liquibase/reporting-mvp-rollback.xml`, which includes the
unchanged reporting files at their original paths, for bounded rollback.
No running application database, shared test database or public UAT state is
changed. The two migration scenarios and three ORM/persistence checks pass.

The fresh-database scenario verifies table/index/constraint creation, complete
reporting rollback, and idempotent reapplication. Existing report definitions and
unrelated menu entries retain identical field fingerprints. The populated
scenario first checks 1,000 existing definitions across the M1 upgrade, then
adds 100 shared definitions and 50,000 jobs across queued, generating, ready,
failed, expired and cancelled states. The M2 update, rollback and reapplication
retain every old job field and every shared-definition field, including the
last editor and version timestamp. Frozen JSON includes Unicode and quoted
text. Row/file metadata, owner/request identity and retry lineage remain
unchanged, and duplicate submission identity is still rejected by PostgreSQL.
The new cleanup index is valid and its timestamp column has the intended type.

Rollback has two distinct scopes:

- Rolling back `479-004-reporting-output-cleanup` removes its cleanup timestamp
  and index while retaining the queue and saved reports. Reapplication recreates
  empty cleanup markers. This path is verified with populated data.
- Rolling back all four reporting changesets removes the job table, reporting
  menu entry, cleanup metadata and report-definition `updated_by` column.
  Existing definition payloads remain in their original table. This is feature
  uninstallation; restoring queue history afterward requires the retained
  database backup. The fresh-database test proves schema removal/reapplication,
  not preservation of data in the explicitly dropped table.

These checks execute actual Liquibase update/rollback, not generated SQL or
inspection of rollback tags alone. They qualify the schema path independently
from the earlier output-volume/process-recovery tests. Multi-instance crash
isolation and restoration from database/file-volume backups are not exercised
by these schema tests.

### Recorded 50,000-result run, 2026-09-14

T027 passes. Reproduce with the [local workload runner](../../projects/reporting-uat/README.md#50000-result-workload)
and its focused `core-performance` browser test. The fixture has 5,001 specimens,
10,001 analyses and 50,000 finalized results on May 7, 2026. Five thousand
specimens have two analyses with four readings each; one specimen has 10,000
readings. Equal adjacent values have distinct identities. Expected spreadsheet
and detailed-list counts are both 50,000, including every repeat and its
30/90-minute turnaround. Three concurrent ordinary reports each produce the
independently expected two rows with value 450.

| Observation                              | First run     | Final run with atomic state sampling |
| ---------------------------------------- | ------------- | ------------------------------------ |
| Spreadsheet generation                   | 60.745 s      | 46.146 s                             |
| Detailed-list generation                 | 54.447 s      | 46.136 s                             |
| Java baseline resident memory            | 1,393,424 KiB | 1,490,920 KiB                        |
| Java peak resident memory                | 1,503,172 KiB | 1,501,904 KiB                        |
| Successful ordinary authenticated reads  | 162           | 144                                  |
| Longest ordinary read                    | 1.409 s       | 1.336 s                              |
| Observed 95th-percentile ordinary read   | 0.692 s       | 0.884 s                              |
| Follow-up cursor fetches per large query | 200 / 200     | 200 / 200                            |

The final runner uses a single database statement for each concurrency
observation, avoiding counts assembled from different instants. Five jobs were
active before the sixth submission returned 429; at most one job was observed
generating. All five completed correctly without process exhaustion or restart.
Session, queue and catalog reads used a separate authenticated client. The
reported percentile is the sorted sample at `floor((n-1) * 0.95)`; these are
observed local request durations, not an asserted response-time service level.

Both runs produced byte-identical files. The spreadsheet is 1,445,747 bytes,
SHA-256 `029cf33d7980615cca9b0647a571fea4f8ebc8e4a47e33752d592e243a09ba90`.
The detailed list is 1,736,904 bytes,
SHA-256 `44cef8c22e51eecbd6c84a24aef3cc95437ebe3cdf4da7bb67c427efc80773ab`.
The oracle checks all values, specimen/result identities and repeat
multiplicities, rather than only row counts or comparison between two generated
files. Fixture SHA-256:
`a1d7bdd74821715e08a0fcefefb04f27a52d137895a907741cae493a90c5c3e9`.

Environment: Docker VM `aarch64`, 17 CPUs and 48,388,898,816 bytes memory; no
explicit container CPU/memory cap; Java heap `-Xmx2g`; PostgreSQL 14.4. Image
digest `sha256:2217d76104051589d99eb808cef22ae692f6ad2d12a0fadc70ecc549162df36f`.
WAR SHA-256 `b46f2001500fd5405b9c2b6dc5f3c9d804e671efbdc5b8a335b07394bd284440`;
its 4,216 application class files match public backend `d56922c11e` exactly.
Frontend `0d65ccaac4` and the single-application native API overlay were used.
The runner resets only the actual Java process's resident high-water counter;
query logging is temporarily enabled for cursor evidence and restored afterward.
Measured durations include logging overhead and are specific to this local stack.

Streaming is verified through the transactional query's 250-row fetch size,
observed cursor fetches, persistence-context clearing every 250 records, and
the writer's bounded pending-row implementation. All nine
`ReportingCsvWriterTest` checks pass, including consumption/output during a
50,000-result repeat stream. Resident-memory readings alone are not treated as
proof of bounded allocation.

Both actual browser downloads pass at 1280×900 and 390×844, including ready/row
count, reload, byte/hash checks and no horizontal overflow or page errors. The
reported three checks include authentication. Reviewed screenshots preserve the
mock's queue table/card and action hierarchy; native OpenELIS chrome, actual job
data and scrolled narrow views differ from the fictional mock captures. This
iteration changes qualification tools only. It does not establish public-server
performance, multi-instance crash isolation, migration rollback or human acceptance.

## Two-process crash isolation, 2026-09-14

Run [the two-process procedure](../../projects/reporting-uat/README.md#two-process-crash-isolation)
on a disposable local stack. Both application processes use the public
`22e3a66b6175` WAR, the same PostgreSQL database and report volume, separate logs
and one Spring application context each. The qualification runner checks these
preconditions and requires loopback URLs and no active exports before starting.

The actual run preserved primary `6d7d3d387b8b`, killed temporary peer
`e42cd336a48e` and retained database `f4572a3f704c`. Its 59 observations covered the
normal 300-second lease without editing timestamps. The live lease advanced;
the live partial file and queued work survived. Only abandoned output was
removed. After releasing the bounded read stall, the live and queued jobs became
ready and the failed job's linked retry preserved its frozen request.

| Evidence      | Observed result                                                                  |
| ------------- | -------------------------------------------------------------------------------- |
| Live job      | `57156fca-b95e-4313-a3bd-503e10107cd2`, completed                                |
| Abandoned job | `51c227ed-2807-463c-92e6-26573fac6e47`, interrupted only after its lease expired |
| Queued job    | `50e6ca95-c74e-4623-a0aa-cbe9c505c322`, retained then completed                  |
| Linked retry  | `7557092a-c7ce-4ebb-801c-9806f17615e0`, completed with unchanged request         |
| Audit         | Exactly one `INTERRUPTED` event, for the abandoned job                           |
| Actual CSV    | UTF-8 BOM, Accession Number/Viral Load; two `REPORTING-MVP-REPEAT,450` rows      |
| CSV SHA-256   | `499ab005f03b3c02d0da1af52097f3f64b6f00599f839beadac3e577fe741e32`               |

The receipt and raw observations are in
`/private/tmp/reporting-multi-process-20260914/isolation/`; command output is
`/private/tmp/reporting-multi-process-qualification.log`. The stopped peer was
inspected and removed after evidence capture. The primary application/database
were not restarted or recreated. The runner releases its read stall in `finally`
and retains observations on failure. This is local process-isolation evidence;
public queued cancellation and human acceptance remain separate.

## Evidence to Record

T016/T017 record M1 evidence; T026–T030 complete the source, recovery and
workload evidence. Record tested revision, fixture version, environment, exact
commands and results, CSV comparisons, browser evidence, workload measurements,
actual output configuration and recovery/cleanup procedures. Keep document
validation, code tests, CI, deployment and user acceptance separate. This
package records local implementation evidence in `execution.md`; incomplete
conditions remain unchecked in `tasks.md`.

The focused reporting formatter command is:

```bash
mvn -o '-DspotlessFiles=.*reports/dataexport/.*\.java' spotless:check
```

Check that the output reports the number of selected Java files (45 at this
checkpoint). `spotlessFiles` matches absolute paths as regular expressions;
repository-relative file lists previously selected zero files and did not
validate formatting. Full CI formatting remains a separate gate.

## Continuous Deployed UAT

Publish each usable stage at its exact revision to
`reporting.catalyst.openelis-global.org` and follow [uat.md](uat.md). Do not wait
for the full MVP. Use the same workflows, fixture oracles and expected results
for automated end-to-end checks and human UAT; record current failures and
unavailable capabilities alongside each stage. Publication requires application health, the exact target revision at
`/__review/target.json`, stable public synthetic fixtures, focused browser
preflight with inspected CSV contents, the `reporting` Grist checklist, the
review overlay and an authenticated submission/download check. Record human UAT
as pending until a reviewer actually returns a revision-bound report.

### Referral stage acceptance fixture

Load `src/test/resources/fixtures/reporting-referrals.sql` after the existing
repeated-results fixture (the shared loader does this). Choose Referrals, add
Accession Number, Referral ID, Referral Result ID, Result ID, Referred Lab,
Referred Test Name, Referral Date, Referral Result Value, Referral Result Date
and Referral Status. Use May 7, 2026 for both period dates. Review must identify
sent dates and must not claim Finalized-only results.

Save a shared report, reopen it, enter fresh May 7 dates and generate the CSV.
Expect three rows for `REPORTING-MVP-REPEAT`: two separate 450 returns from
Synthetic Reference Lab, dated May 8 and May 9 with separate link/result IDs,
and one pending REQUESTED referral with blank returned fields. The draft and
May 8 sent referral must be absent. Repeat at desktop and phone widths using
the same workflow and expectations. The `Referrals preserve returned and pending
rows through shared reports` browser checks implement this UAT walkthrough.

### Repeatable queued-cancellation UAT

The existing 50,000-result synthetic workload creates ordinary queued work while
one real export runs. No database lock, worker pause, timestamp alteration or
special queue exception is used. Preparation is opt-in for the disposable local
stack or the dedicated public Reporting UAT stack; ordinary CI does not load this
large fixture. Existing records are retained, and the fixture refuses an occupied
reporting date that is not its own already-complete dataset.

Run the guarded setup from the application repository with the worker idle:

```sh
python3 projects/reporting-uat/prepare-cancellation-workload.py \
  --project reporting-mvp-iteration1 \
  --app-container reporting-mvp-iteration1-app-1 \
  --db-container reporting-mvp-iteration1-db-1 \
  --fixture src/test/resources/fixtures/reporting-workload-50000.sql \
  --output /private/tmp/reporting-cancellation-setup
```

The output directory must be new. Setup verifies the actual stack, takes a
backup, loads the idempotent fixture and verifies 50,000 result identities across
5,001 specimens and 10,001 analyses. The public procedure uses project
`reporting-uat`, its matching app/database containers and
`--identity /home/ubuntu/reporting-uat/runtime/identity/target.json`; run it on the
reporting host with the exact committed script and fixture. It requires that
identity to name the ready Reporting UAT instance and retains the live app.

With TEST_USER and TEST_PASS supplied in the environment, run the registered
workflow against either the local preview or public reporting URL:

```sh
cd frontend
REPORTING_WORKLOAD=true BASE_URL=http://127.0.0.1:18489 npm run pw:test -- \
  playwright/tests/foundational/core/custom-data-export-recovery.spec.ts \
  --project=core-app --workers=1 --max-failures=1 --grep 'naturally queued'
```

Human and automated flow: create a Sample & Testing spreadsheet with Accession
Number and Viral Load for May 7, 2026. Generate it and observe Generating. Return
to the overview, start another report with the same columns for May 5, then open
My Report Queue. The May 5 job is Queued while the first report runs. Choose
Cancel, then Keep queued; reload and verify it remains Queued. Open Cancel again
and confirm Cancel export. Expect Cancelled after reload, no download, and no
later transition to Generating or Ready. Once the large report completes, its
actual CSV must preserve all 50,000 rows, including the equal repeated readings.
The browser also checks that the cancelled job has no start time, row count or
file size and that a direct download is refused. Repeat at desktop and phone
widths and compare the queue with the pinned mock. The confirmation uses the
existing Carbon modal required by the functional specification; the mock's
queue cancellation is immediate.

If a human run reaches the job after it starts, do not call that a successful
cancellation: the interface must refuse cancellation clearly. Prepare both
reports in separate tabs before generating the large one when more setup time
is needed. The next attempt should create new jobs, preserving the first run's
history.

## September 15 delivery checkpoint

See the [revision-bound delivery receipt](execution.md#september-15-delivery-checkpoint) for passing backend, frontend and E2E CI, public deployment identity,
recorded workflow evidence and the distinction between mock simulation controls
and product features. The full requirement reconciliation is in [acceptance.md](acceptance.md).
Human acceptance remains pending and separate from engineering validation.
