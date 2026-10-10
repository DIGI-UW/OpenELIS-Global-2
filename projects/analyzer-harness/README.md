# Analyzer Harness

This directory now follows a single authoritative path for analyzer E2E parity.

## Development and CI

Start source development with `scripts/dev-stack up`. Export
`eval "$(scripts/dev-stack env)"` from the repository root, then use the native
Playwright/npm commands in `frontend`. Development data persists until an
explicit reset.

Use `scripts/run-ci-checks.sh` for isolated CI. Select a CI job through that
same command, for example `--job playwright-analyzers-1`. `--list-jobs` lists
the supported choices. Selected jobs include their setup and produce a partial
result. Internal lane scripts under `scripts/ci/` are implementation details.
They use the workflow's Compose inputs, fixture loader and test projects, with
local overrides for isolated identities and ports.

See [the development guide](../../docs/dev_setup.md) for the authoritative
commands, prerequisites and separation from published-image deployment.

## Workflow wait policy

Analyzer browser tests wait for observable UI, API, and persisted-result states.
Assertions use the existing whole-test deadline, configured through the harness
Playwright projects, rather than separate step deadlines. Do not add sleeps or
increase a test's deadline to repair failures. Diagnose the missing state using
traces and service logs. Video-only pacing is presentation, never readiness.

## Startup Catalog

Before OE starts, the harness copies its dictionary,
`projects/analyzer-harness/dictionary/`, into the writable `configuration-data`
volume. OE loads it through its ordinary startup configuration service. The
dictionary is a copy of OE2's generic dictionary
(`volume/configuration/backend/`) plus the concepts every shipped analyzer
profile sends, so each binds on a fresh setup with no operator work. It is test
data, not an application-wide clinical default. A file already in the volume,
such as a Catalog Import upload, is not overwritten on restart.

- CI and local parity load the same harness catalog through the normal loader.
- Local development keeps optional Catalog Import uploads in its worktree-scoped
  `configuration-data` volume. A clean-install test resets it with the database
  using `scripts/dev-stack down --volumes --yes`.
- Other OE2 deployments do not mount the harness files.

The registered Playwright tests read the startup catalog and fail visibly when a
shipped profile cannot resolve its intended clinical test. The seeder does not
select or confirm those mappings for CI.

## Durable Bridge state

The shared CI/local base mounts `bridge-data` at
`/data/openelis-analyzer-bridge`. Connections, pinned profile revisions, the
delivery outbox and FILE processing state survive container replacement and
ordinary stack shutdown. `scripts/dev-stack down --volumes --yes` explicitly
removes them along with the worktree's other persistent data.

Use `scripts/dev-stack up --skip-build --no-scenarios` to apply changed
configuration without rebuilding the application. Refresh
`scripts/dev-stack env` after recreation because published local ports can
change.

## Local Compose Layers

Local harness startup now uses the same canonical service identities as CI, with
local-only overrides layered on top:

- `docker-compose.base.yml`
- `docker-compose.dev.yml`
- `docker-compose.analyzer-test.yml`
- `docker-compose.letsencrypt.yml`
- `docker-compose.worktree.yml`

These files must not drift behaviorally from the authoritative CI harness path
for critical analyzer flows.

## Development startup

```bash
scripts/dev-stack up
```

Run this from the repository root. It is the only supported interactive
development launcher and always starts core OpenELIS together with the analyzer
harness. It assigns worktree-specific Compose resources and random local ports.
Use `scripts/dev-stack env` to supply its URLs and analyzer network addresses to
Playwright.

The startup path does not execute SQL fixture loaders or use fixed primary keys.
It calls `seed-analyzers.sh --ensure-connections` to create missing
profile-backed harness connections through authenticated application services.
Ordinary restarts preserve existing connection configuration, mappings, and
review data; they do not replay result traffic. CI uses the same
`--ensure-connections` mode. The Playwright scenarios own their API-created
clinical orders and native traffic. CI parity is a separate validation command
because it intentionally reproduces CI packaging.

To remove this worktree's data explicitly:

```bash
scripts/dev-stack down --volumes --yes
```

## Hot reload (after backend code changes)

The harness mounts `../../target/OpenELIS-Global.war` into the `oe.openelis.org`
container. After changing Java code, rebuild the WAR and **force-recreate** the
container (Tomcat caches the exploded WAR; a plain `restart` will serve stale
classes):

Re-run `scripts/dev-stack up`. The command rebuilds the WAR and recreates the
changed application services. Frontend changes hot-reload automatically.

## Resetting development data

Use `scripts/dev-stack down --volumes --yes`, then `scripts/dev-stack up`. The
isolated CI runner creates fresh test state and cleans up its own resources; it
does not reset the development stack.

## Let's Encrypt (analyzers.openelis-global.org)

The harness **shares the repo's Let's Encrypt certs**: it mounts
`../../volume/letsencrypt` (repo root), so valid certs generated per
**docs/LETSENCRYPT_SETUP.md** are used automatically.

Set the domain and email in the worktree's `.env`:

```bash
LETSENCRYPT_DOMAIN=analyzers.openelis-global.org
LETSENCRYPT_EMAIL=your-email@example.com
```

Then run `scripts/dev-stack up`. Certificate issuance and the project-scoped
proxy restart are part of that same command.

## URLs

- UI: output of `scripts/dev-stack url`
- Backend API: `<scripts/dev-stack url>/api/`

Login (local-dev defaults only):

- Username: `admin`
- Password: `adminADMIN!`

> **Security note:** These credentials are for isolated local development only.
> Configure unique credentials for any shared or production deployment.

## Local volumes

This harness uses a local `./volume/` directory for:

- `./volume/analyzer-imports` → mounted at `/data/analyzer-imports`
- logs under `./volume/logs/*`

## Notes

- ASTM TCP analyzers target the Bridge's shared listener,
  `openelis-analyzer-bridge:12001`.
