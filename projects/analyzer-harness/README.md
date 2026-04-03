# Analyzer Harness

This directory now follows a single authoritative path for analyzer E2E parity.

## Authoritative Base (required for CI parity)

The analyzer harness CI gate runs from the repository root using:

- `projects/analyzer-harness/docker-compose.base.yml`
- `build.docker-compose.yml`
- `.github/ci/ci.analyzer-harness.yml`
- `.github/workflows/e2e-playwright-analyzer-harness-reusable.yml`

Use `ci-parity-test.sh` for exact local reproduction of that CI path.

```bash
./projects/analyzer-harness/ci-parity-test.sh
```

The script performs:

- strict preflight validation (no silent assumptions)
- exact CI step order (compose up, readiness, fixtures, seed, permissions,
  Playwright)
- deterministic evidence capture in `/tmp/oe-ci-parity-<timestamp>/`

## Startup Catalog

The authoritative harness startup catalog lives under
`projects/analyzer-harness/config-templates/`.

- CI mounts that directory directly into OE's startup configuration path.
- Local harness bootstrap copies that same directory into the harness volume.
- Do not add or update harness test catalog CSVs under any other source tree.

`seed-analyzers.sh` now hard-fails if the startup catalog cannot realize the
required profile mappings for the seeded analyzers.

## Local Compose Layers

Local harness startup now uses the same canonical service identities as CI, with
local-only overrides layered on top:

- `docker-compose.base.yml`
- `docker-compose.dev.yml`
- `docker-compose.analyzer-test.yml`
- `docker-compose.letsencrypt.yml`

These files must not drift behaviorally from the authoritative CI harness path
for critical analyzer flows.

## Build and start from scratch

```bash
./build.sh
./reset-env.sh --full-reset
```

Uses `.env` from this dir or repo root (e.g.
`LETSENCRYPT_DOMAIN=analyzers.openelis-global.org`).

## Quick start

From this directory:

```bash
cd /home/ubuntu/OpenELIS-Global-2/projects/analyzer-harness

# Start core stack
docker compose -f docker-compose.dev.yml -f docker-compose.base.yml up -d

# Start analyzer test infrastructure (bridge + simulator + virtual serial)
docker compose -f docker-compose.dev.yml -f docker-compose.base.yml -f docker-compose.analyzer-test.yml up -d
```

Then load analyzer fixtures from the repo root (legacy flow):

```bash
cd /home/ubuntu/OpenELIS-Global-2
./src/test/resources/load-analyzer-test-data.sh --dataset-011
```

## Hot reload (after backend code changes)

The harness mounts `../../target/OpenELIS-Global.war` into the `oe.openelis.org`
container. After changing Java code, rebuild the WAR and **force-recreate** the
container (Tomcat caches the exploded WAR; a plain `restart` will serve stale
classes):

```bash
# From repo root
mvn clean install -DskipTests -Dmaven.test.skip=true

# From harness directory — force-recreate clears the Tomcat WAR cache
cd projects/analyzer-harness
docker compose -f docker-compose.dev.yml -f docker-compose.base.yml -f docker-compose.analyzer-test.yml \
  -f docker-compose.letsencrypt.yml up -d --force-recreate oe.openelis.org
```

Frontend changes hot-reload automatically (mounted volume).

## Resetting the test environment

For exact CI parity, prefer:

```bash
./projects/analyzer-harness/ci-parity-test.sh
```

For local restart mode, run:

```bash
./projects/analyzer-harness/reset-env.sh [options]
```

Options:

- **`--full-reset`** – Remove DB (and other) volumes before starting (wipe DB,
  then load fixtures).
- **`--skip-fixtures`** – Start stack only; do not load
  foundational/storage/analyzer fixtures.

Steps performed: stop stack → optionally `down -v` → start dev + analyzer-test
compose → wait for webapp → load fixtures via direct psql to `localhost:15432`.

## Let's Encrypt (analyzers.openelis-global.org)

The harness **shares the repo's Let's Encrypt certs**: it mounts
`../../volume/letsencrypt` (repo root), so valid certs generated per
**docs/LETSENCRYPT_SETUP.md** are used automatically.

From **repo root** (not harness dir), generate certs for the subdomain once:

```bash
export LETSENCRYPT_EMAIL="your-email@example.com"
export LETSENCRYPT_DOMAIN="analyzers.openelis-global.org"
./scripts/generate-letsencrypt-certs.sh
```

Then start (or restart) the harness with the letsencrypt override; the proxy
entrypoint will use `volume/letsencrypt/live/analyzers.openelis-global.org/` if
present, else self-signed fallback.

## URLs

- UI: `https://localhost/`
- Backend API: `https://localhost/api/`

Login (local-dev defaults only):

- Username: `admin`
- Password: `adminADMIN!`

> **Security note:** These credentials are for isolated local development only.
> Configure unique credentials for any shared or production deployment.

## Local volumes

This harness uses a local `./volume/` directory for:

- `./volume/analyzer-imports` → mounted at `/data/analyzer-imports`
- `./volume/plugins` → mounted at `/var/lib/openelis-global/plugins`
- logs under `./volume/logs/*`

## Notes

- HL7 analyzers are treated as **push-based** in OpenELIS; “Test Connection”
  will instruct you to validate by pushing an HL7 message to OpenELIS instead of
  attempting an outbound socket connection.
- ASTM TCP analyzers should target `openelis-analyzer-bridge:12001` (fixtures
  updated accordingly).
- RS232 analyzers use virtual ports under `/dev/serial/ttyVUSB0-4` (created by
  `virtual-serial` service).
