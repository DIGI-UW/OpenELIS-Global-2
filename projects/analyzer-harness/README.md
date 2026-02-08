# Analyzer Harness (isolated docker-compose)

Isolated dev + analyzer testing. Domain: **analyzers.openelis-global.org** (same
env/Let's Encrypt as main).

## What’s included

- `docker-compose.dev.yml`: OpenELIS (WAR-mounted) + DB + frontend + proxy
- `docker-compose.analyzer-test.yml`: ASTM simulator + ASTM-HTTP bridge +
  virtual serial
- `docker-compose.letsencrypt.yml`: Let's Encrypt override; `build.sh`,
  `reset-env.sh` (e.g. `--build --full-reset`)

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
docker compose -f docker-compose.dev.yml up -d

# Start analyzer test infrastructure (bridge + simulator + virtual serial)
docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml up -d
```

Then load analyzer fixtures from the repo root:

```bash
cd /home/ubuntu/OpenELIS-Global-2
./src/test/resources/load-analyzer-test-data.sh --dataset-011
```

## Resetting the test environment

From this directory (or repo root), run:

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
