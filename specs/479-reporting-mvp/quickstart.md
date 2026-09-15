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
| Referrals                                                | Referral request date, referred-analysis occurrence identity and all selected linked results                                                                         |
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
