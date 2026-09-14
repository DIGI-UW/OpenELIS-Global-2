# Reporting UAT runtime qualification

These tools apply to the isolated reporting deployment. They do not change the
shared OpenELIS image or other deployments.

## Instance navigation

Mount `projects/reporting-uat/menu/menu_config.json` read-only at
`/var/lib/openelis-global/menu/menu_config.json` in the reporting app. The
loader applies this profile in memory at application startup. Existing database
menu rows are retained; nested definitions reorganize their parent relationships
for this instance. Other deployments keep their own menu configuration.

The database remains the baseline. Configuration matches `elementId` and only
overrides fields explicitly supplied; unlisted entries and unspecified database
settings remain available. Existing `includes`/`excludes` filtering and route
authorization remain in place. Nested `childMenus` can regroup existing entries
without rewriting their database parents. Removing the mounted profile restores
the database hierarchy after the menu cache is rebuilt (normally on restart).

The shared renderer accepts `presentationStyle: "section"` for a labeled section
and `icon` for a Carbon icon. Supported icon names are `home`, `order`,
`results`, `validation`, `patient`, `reports`, `settings`, `workplan` and
`more`; absent or unknown icons render no icon. Section labels use normal
translated `displayKey` values. For example, an instance can place its existing
Reports menu in a section:

```json
{
  "menus": [
    {
      "elementId": "instance_reporting_section",
      "displayKey": "sidenav.label.reports",
      "presentationStyle": "section",
      "childMenus": [{ "elementId": "menu_reports", "icon": "reports" }]
    }
  ]
}
```

The menu persistence increment adds the same optional `presentationStyle` and
`icon` fields to the database. Global Menu Configuration edits existing database
menus and identifies fields controlled by this instance as read-only. JSON-only
entries remain controlled by their instance profile. Saving a menu cannot copy
its configured overrides into the database defaults. Removing the profile and
rebuilding the menu restores the saved database values.

The menu entity now uses annotations; the old XML mapping and both runtime
registrations were removed. Migration `479-005-menu-presentation` adds the two
optional fields and the standard version timestamp. Its rollback removes only
these new columns. Preserve the pre-upgrade backup if new presentation values
must be recovered after rollback. The configured menu file can optionally be
located with `org.openelisglobal.menu.configuration.file`; the normal mounted
path above remains the default.

The frontend owns one renderer and icon registry. Section headings display their
children without a destination or icon; menu items and expandable groups display
the configured icon. The editor explains that distinction. The instance profile
continues to own this deployment's sections, hierarchy and destinations.
Publication of this increment is recorded separately in `execution.md`.

For persistence/profile qualification on the disposable local stack, save an
existing database menu's icon and `presentationStyle` through the administrative
API, preserving its original values. Restart the same application container with
the database retained, then sign in again and verify both values. Apply a second
profile that explicitly overrides those fields and rebuild the menu cache with
an empty administrative save. Verify its effective values and configuration
provenance. Save that effective entry, remove the two overrides, rebuild again
and confirm that the original saved database values return. Finally restore the
initial values and exact profile bytes. No frontend change or rebuild is needed.
The September 14 run passed every step with the same application/database
containers; its receipt is recorded in `execution.md`.

The profile follows the pinned openelis-work mock: Main Menu, Patient & Orders,
Reports and Administration, with Carbon icons inheriting the active OpenELIS
theme. Routine report destinations are direct entries. Other reports and More
tools retain the older menus in collapsed groups. Patient Report Print Queue is
visibly not yet connected because there is no implemented destination. The
existing Results Entry feature flag selects one working results route.

The sidebar derives its active entry from path and query parameters, preserves
native modified clicks, opens the destination's parent groups on navigation, and
closes the mobile drawer after changing views. Validate the mounted profile and
retained report draft with the `core-app` test named
`the reporting instance sidebar follows the mock`, setting
`REPORTING_INSTANCE_NAV=true`. Compare its desktop/narrow captures directly
against the pinned mock, alongside the actual CSV workflow. The menu
configuration is a presentation choice, not a change to reporting scope or
access rules.

## Single application and native API path

The inspected image declares `/api/OpenELIS-Global/` explicitly in Tomcat while
also discovering `OpenELIS-Global.war` automatically. This loads the same
application a second time at `/OpenELIS-Global`. A controlled runtime probe
observed two generating exports in one container, contrary to the reporting
plan's one-worker limit.

Prepare a versioned overlay from the selected image's actual `server.xml`:

```sh
python3 projects/reporting-uat/prepare-server-config.py original-server.xml release/server.xml
python3 projects/reporting-uat/prepare-proxy-config.py original-nginx.conf release/nginx.conf
```

The first tool disables automatic discovery and startup discovery, preserving
the existing explicit API contexts and all unrelated settings. This follows
[Tomcat's guidance for explicit context paths](https://tomcat.apache.org/tomcat-10.1-doc/config/context).
Mount the generated file read-only at `/usr/local/tomcat/conf/server.xml` in the
reporting app. The second tool removes the proxy's URI replacement so that
`/api/OpenELIS-Global/...` reaches that same native path, including its query
string. It preserves the existing upstream, headers and other locations. See
[nginx proxy_pass](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass).

Keep original configuration, application artifacts, database and report volumes.
Generate the overlay on the deployment host; whole server configurations need
not be copied into this repository. Validate nginx configuration before
switching the reporting app and proxy together. A loopback frontend preview must
likewise preserve `/api`, instead of removing it. Verify native session/login,
real CSV downloads, retry/re-run, browser navigation and the review overlay
after switching.

## Process interruption

Run this on the disposable local fixture stack, with no active reporting jobs.
The scripts require loopback API URLs and matching Docker Compose project
labels. The fixture is the May 5, 2026 `REPORTING-MVP-REPEAT` specimen: test 174
has two distinct Viral Load results, both 450. The known development login can
be overridden with `TEST_USER` and `TEST_PASS`.

```sh
python3 projects/reporting-uat/interrupt-worker.py \
  --base-url https://localhost:18485/api/OpenELIS-Global \
  --app-container reporting-mvp-iteration1-app-1 \
  --db-container reporting-mvp-iteration1-db-1 \
  --project reporting-mvp-iteration1 \
  --output /tmp/reporting-restart-evidence
```

This creates and checks a completed CSV, briefly stalls result reads in the
disposable database, submits three real jobs and observes generating/queued
states. It verifies incomplete downloads are rejected, then kills only the app
process. A bounded database transaction is released in `finally`; no specimen or
result data is rewritten. The output captures process termination, persisted job
states and existing partial files. The app remains stopped for the operator to
restart with the intended versioned configuration.

Start the same application with the retained database/output volumes, then run:

```sh
python3 projects/reporting-uat/verify-restart.py /tmp/reporting-restart-evidence/interruption.json \
  --base-url https://localhost:18485/api/OpenELIS-Global \
  --app-container reporting-mvp-iteration1-app-1 \
  --db-container reporting-mvp-iteration1-db-1 \
  --project reporting-mvp-iteration1
```

The verifier waits on that running container. A timeout is an observation limit,
not a reason to restart it. It requires unchanged completed CSV bytes, preserved
queued work, explicit interrupted failures, retained frozen requests, rejection
of incomplete downloads, removal of partial files and a linked retry with the
same independently expected CSV. Worker leases expire naturally; the test does
not alter their timestamps.

The September 14 local run killed two generating jobs and retained one queued
job. With the single-application overlay, both interrupted jobs failed visibly,
the queued job completed, old downloads were unchanged and retry succeeded. A
second controlled probe observed one generating job and two queued jobs. Tomcat
logged one Spring root initialization and 212.652 seconds total startup. Four
native-route browser workflows passed, plus authentication. These local results
alone do not establish public deployment or the remaining operational criteria.

Runtime configuration `7780ee2cd9` was subsequently deployed to Reporting UAT on
September 14. The public app logged one Spring root initialization and 457.170
seconds startup. Five public application workflows plus authentication and the
pinned-mock capture passed. A separate accelerated local expiry check also
verified unavailable expired downloads, removed files, retained history and an
unaffected existing download. Local large-volume, migration/rollback and
two-process crash isolation qualification now pass. See
[the current execution record](../../specs/479-reporting-mvp/execution.md) for
exact deployment identity, evidence and limits.

## Two-process crash isolation

Use `qualify-worker-isolation.py` only on the disposable local reporting stack
with the `REPORTING-MVP-REPEAT` fixture. Prepare a temporary second application
service from the primary service's effective Compose definition: retain the same
database, image, WAR, properties, single-context server configuration and report
volume, but use an available loopback port and a separate persistent log
directory. Start only that temporary service with `--no-deps`. Keep the normal
five-minute worker lease and wait for that same container to finish starting.
The Compose directory must already have its configured `.env`.

The September 14 run used this command after both applications were ready:

```bash
python3 projects/reporting-uat/qualify-worker-isolation.py \
  --primary reporting-mvp-iteration1-app-1 \
  --peer reporting-mvp-iteration1-recovery-peer-1 \
  --database reporting-mvp-iteration1-db-1 \
  --project reporting-mvp-iteration1 \
  --base-url https://localhost:18485/api/OpenELIS-Global \
  --peer-url https://localhost:18493/api/OpenELIS-Global \
  --output /tmp/reporting-worker-isolation
```

The runner checks process identity, ports, shared image/WAR/output and separate
logs before submitting jobs. It temporarily stalls result reads with a bounded
database transaction, identifies each worker through its actual audit log, and
kills only the temporary peer. While the primary continues renewing its lease,
the peer's unchanged lease expires naturally. Only the abandoned output is
removed. The read stall is released in `finally`; the primary completes its own
job and queued work, and a linked retry preserves the frozen request. Every
downloaded CSV must match the fixture oracle byte for byte. The retained receipt
includes process identities, 59 lease observations and the interruption audit.
Preserve the stopped peer's inspection/logs before removing that service; leave
the primary, database and named volumes running.

See
[the qualification record](../../specs/479-reporting-mvp/quickstart.md#two-process-crash-isolation-2026-09-14)
for this run's exact identities, hash and limits.

## Reporting audit events

Reporting emits `REPORTING_AUDIT` JSON records through OpenELIS's existing
application logger and rolling-file appenders. Records contain action, actor,
owner, target type/ID, parent job ID and UTC timestamp. They contain no report
names, selected fields, filters, frozen requests or result values. Background
events identify the worker or system separately from the report owner.

Successful submissions, claims, outcomes, retries, cancellations and definition
changes are logged after transaction commit. Idempotent requests do not add
duplicate success events. Denials are logged immediately so the rejected
transaction cannot erase them. `DOWNLOAD_OPENED` records an authorized file
being opened; it does not claim that the client's transfer finished. The
dedicated logger stays at INFO if ordinary diagnostic severity is reduced.

Persist `/var/lib/openelis-global/logs` using the deployment's retained volume
or host directory. Before replacing a container that currently stores logs in
its writable layer, preserve its existing logs in that directory and retain the
previous compose/target files. Reuse the same log mount on subsequent releases.
The existing logger continues to manage rotation. Check metadata events in
`openELIS.log` and its rotated archives, separately from queue rows. The legacy
database audit table accepts numeric references; it is not widened or repurposed
for reporting UUIDs by this change.

Focused validation:

```sh
mvn test -Dtest=ReportingAuditTest,ReportingRecoveryIntegrationTest,ReportingJobServiceTest,ReportingAccessTest,ReportingSavedConfigServiceTest
```

This covers committed lifecycle events, rollback, idempotent retry/cancel,
authorized file opening, expiry, interrupted work, role/scope denials and saved
definition changes. The capture inspects actual logger output and restores the
test logger afterward. Public availability and durable log-mount verification
are recorded separately in the execution ledger.

## Database upgrade and rollback

From the repository root:

```sh
mvn test -Dtest=ReportingMigrationRollbackTest,ReportingPersistenceTest
```

Two standalone PostgreSQL 14.4 containers initialize the full application
changelog before executing reporting rollback/reapplication. The fresh case
verifies migration registration, schema/indexes and idempotency. The populated
case covers 1,000 existing definitions, 100 shared definitions and 50,000 jobs
across all six states. Actual Liquibase recovery rollback retains every prior
job field and all definition fields, including request text, lineage, history
metadata and last editor. The five reported checks include three existing
ORM/persistence tests.

Recovery rollback removes its cleanup marker/index and reapplication starts
those markers empty. Full reporting rollback also removes the job table and the
report-definition last-editor column; it does not preserve dropped data. See
[the acceptance record](../../specs/479-reporting-mvp/quickstart.md#database-upgrade-and-rollback-qualification-2026-09-14)
for tested boundaries. This test never rolls back the running local application,
the shared test context or Reporting UAT. It changes no deployed application
code.

## 50,000-result workload

Use the disposable local stack with the single-application overlay and the
existing May 5 repeat fixture. The runner requires loopback access, matching
Compose project labels, no active reporting jobs and a new evidence directory.
The idempotent synthetic fixture adds 5,001 specimens, 10,001 analyses and
50,000 results on May 7, including 10,000 repeats for one specimen. It refuses
an occupied date or incomplete fixture and deletes no existing records.

```sh
python3 -u projects/reporting-uat/qualify-workload.py \
  --base-url https://localhost:18485/api/OpenELIS-Global \
  --app-container reporting-mvp-iteration1-app-1 \
  --db-container reporting-mvp-iteration1-db-1 \
  --project reporting-mvp-iteration1 \
  --seed \
  --output /tmp/reporting-workload-evidence
```

It submits two large exports plus three routine exports, proves the sixth
concurrent submission is rejected, samples atomic job states and ordinary
authenticated reads, and verifies every downloaded cell against independent
expected values. Spreadsheet checks preserve repeated-value multiplicity;
detailed-list checks additionally require every distinct result identity. Both
layouts retain their own 30/90-minute turnaround values.

The runner locates the actual Java process, resets only its Linux
resident-memory peak counter, and samples current/peak resident memory. See
[Linux process-memory counters](https://docs.kernel.org/filesystems/proc.html).
It temporarily enables statement logging in the disposable PostgreSQL instance
using its existing administrator, captures cursor-fetch counts, then restores
the original setting and override state even on a failed check or ordinary
interrupt. Timings include that logging overhead. Cursor fetches provide runtime
evidence for the transactional 250-row streaming query; see
[PostgreSQL JDBC cursor behavior](https://jdbc.postgresql.org/documentation/query/).
The verifier may collect the downloaded CSV in memory; the measured memory is
the separate application process that generates it.

Run the focused browser check from `frontend/` while the qualified jobs are
still available in the queue:

```sh
BASE_URL=http://127.0.0.1:18489 \
REPORTING_WORKLOAD_RECEIPT=/tmp/reporting-workload-evidence/verified.json \
npm run pw:test -- playwright/tests/performance/core/reporting-workload.spec.ts \
  --project=core-performance --reporter=line \
  --output=/tmp/reporting-workload-browser
```

Both layouts are checked at desktop and phone widths for ready state, row count,
reload, horizontal overflow and actual browser download bytes against the
independently qualified receipt. Without an explicit receipt the workload tests
skip; they do not silently claim qualification against an unseeded environment.
Native Playwright `--list` and execution verify project registration. The
packaged project-validator currently omits `CORE_PERFORMANCE_TESTS` from the
constants it resolves. The test audit found semantic/test-ID selectors,
event/assertion waits, diagnostic capture and no forced interactions or fixed
timing gates.

The September 14 final run passed: 50,000 rows per layout in 46.1 seconds each,
1,501,904 KiB peak resident memory, 144 ordinary reads without failures, one
generating worker observed, five active jobs allowed and the sixth rejected.
Both large streams had 200 follow-up cursor fetches. A preceding run produced
identical files; nine writer tests and both browser workflows also pass. See
[the measurement record](../../specs/479-reporting-mvp/quickstart.md#recorded-50000-result-run-2026-09-14)
for exact hashes, environment and limits. This is local qualification; the
public application and its synthetic fixture set remain unchanged.
