# Restart Analyzer Harness

When the user invokes `/restart-analyzer-harness` (optionally with arguments),
perform an analyzer harness environment restart workflow with:

- **Container restart** with force-recreate for harness services
- **Optional database reset** (drop volumes with `--full-reset`)
- **Plugin jar staging** into `volume/plugins` (same runtime prep as CI)
- **Fixture loading** (non-test data only — test metadata comes from CSV config)
  and **analyzer seeding** (`seed-analyzers.sh`)
- **Analyzer infrastructure verification** (ASTM bridge, simulator, virtual
  serial)

This command is for **analyzer manual testing and E2E validation**. It uses the
harness stack (`projects/analyzer-harness/`) with full analyzer test
infrastructure, NOT the root dev stack.

### Local dev vs Let's Encrypt

**Default (typical local machine):** Do **not** use Let's Encrypt. Use only:

`docker-compose.dev.yml` + `docker-compose.analyzer-test.yml`

The proxy serves **self-signed** HTTPS on `https://localhost/`. No cert scripts,
no `docker-compose.letsencrypt.yml`, no `LETSENCRYPT_*` in `.env` required.

**Optional public hostname:** Add `-f docker-compose.letsencrypt.yml` and run
the cert script **only** when the user passes **`--letsencrypt`** (and both
`LETSENCRYPT_DOMAIN` and `LETSENCRYPT_EMAIL` are in `.env`, unless
`--skip-letsencrypt`). Do **not** enable LE from `.env` alone — keeps local dev
simple.

## Credential Handling

> **All credentials live in `$REPO_ROOT/.env`. NEVER construct passwords inline
> in shell commands. NEVER modify password hashes anywhere.**

- **Loading credentials**: `set -a; . "$REPO_ROOT/.env"; set +a` — this reads
  values as plain text from the file, bypassing all shell escaping issues.
- **Login verification**: Call
  `$REPO_ROOT/projects/analyzer-harness/scripts/verify-login.sh` — it reads
  `TEST_USER`/`TEST_PASS` from the environment and uses `--data-urlencode` for
  curl.
- **Playwright env**: After sourcing `.env`, `TEST_USER` and `TEST_PASS` are
  already exported. No additional `export` commands needed.

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/restart-analyzer-harness` → Full reset + restart (drop DB, rebuild, seed —
  default behavior)
- `/restart-analyzer-harness --full-reset` → Drop database volumes before
  restart (clean slate)
- `/restart-analyzer-harness --skip-fixtures` → Skip loading test fixtures
- `/restart-analyzer-harness --build` → Build WAR before restarting (for code
  changes)
- `/restart-analyzer-harness --letsencrypt` → Add LE compose overlay; run cert
  setup when `LETSENCRYPT_DOMAIN` + `LETSENCRYPT_EMAIL` are set in `.env`
- `/restart-analyzer-harness --skip-letsencrypt` → Do not run Let's Encrypt cert
  script even when LE env is set (still use LE compose if explicitly requested)
- Combine flags as needed: `/restart-analyzer-harness --full-reset --build`

## Safety Rules (non-negotiable)

- **Warn** if root dev stack is running (suggest stopping it first to avoid port
  conflicts).
- **Never** drop database volumes unless `--full-reset` is explicitly passed.
- **Always** wait for webapp readiness before loading fixtures.
- **Report** container status after restart (even if some containers fail).
- On local dev without `--letsencrypt` / without LE env, **never** require Let's
  Encrypt; self-signed is expected.
- If LE mode is on but certs are missing or script fails, **warn but continue**
  (self-signed fallback).
- **NEVER** modify, regenerate, or replace password hashes (SQL fixtures,
  `adminPassword.txt`, or anywhere else). They work as-is.

## Workflow

### 0) Preflight (gather facts, no changes yet)

Set `REPO_ROOT=$(git rev-parse --show-toplevel)` and use `$REPO_ROOT` for all
paths below (never hardcode `/home/ubuntu/OpenELIS-Global-2`).

**Load `.env` first** — this is the single source for all credentials and
config:

```bash
cd $REPO_ROOT
set -a; . ./.env; set +a
```

This exports `TEST_USER`, `TEST_PASS`, `LETSENCRYPT_DOMAIN`,
`LETSENCRYPT_EMAIL`, etc. from the file as plain text (no shell escaping).

Then run these and summarize the results:

- `git rev-parse --show-toplevel` (verify project root → REPO_ROOT)
- **Detect harness directory**: `$REPO_ROOT/projects/analyzer-harness/` (must
  exist)
- **Submodule check**:
  `git submodule status tools/analyzer-mock-server tools/openelis-analyzer-bridge`
  — if any show as uninitialized (leading `-`), run
  `git submodule update --init tools/analyzer-mock-server tools/openelis-analyzer-bridge plugins`
  before building/starting.
- **Bootstrap check**:
  `test -f $REPO_ROOT/projects/analyzer-harness/volume/database/database.env` —
  if missing, run `$REPO_ROOT/projects/analyzer-harness/bootstrap.sh` (or run
  reset-env.sh which calls it).
- **Check if root stack running**:
  `docker ps --filter name=openelisglobal- --format {{.Names}}`
  - If root stack is running, **warn** that port conflicts may occur (root uses
    80/443/15432, harness uses same ports)
- `git status --porcelain` (warn if uncommitted changes)

Determine:

- **USE_LE_COMPOSE**: true **only** if `--letsencrypt` was passed. Otherwise
  false → local self-signed stack only (ignore LE vars unless flag is used).
- **COMPOSE_FILES**:
  `-f docker-compose.dev.yml -f docker-compose.analyzer-test.yml` plus, if
  `USE_LE_COMPOSE`, ` -f docker-compose.letsencrypt.yml`
- **DOMAIN** (for summary): `localhost` when not using public LE; else
  `LETSENCRYPT_DOMAIN` (e.g. `madagascar.openelis-global.org`)
- **FULL_RESET**: true if `--full-reset` flag present
- **SKIP_FIXTURES**: true if `--skip-fixtures` flag present
- **DO_BUILD**: true if `--build` flag present
- **SKIP_LETSENCRYPT**: true if `--skip-letsencrypt` flag present

Report the detected configuration before proceeding.

### 1) Build WAR file (checkpoint #1) - OPTIONAL

**Run only if `--build` was passed.**

This allows testing code changes without rebuilding images. The harness mounts
`../../target/OpenELIS-Global.war` into the oe service.

Run:

```bash
cd $REPO_ROOT
mvn clean install -DskipTests -Dmaven.test.skip=true
```

After build completes:

- Verify `target/OpenELIS-Global.war` exists
- Report build success or failure

**If build fails**: Stop and report the error. Do not proceed.

### 2) Stop containers (checkpoint #2)

Choose command based on `--full-reset` flag:

- **With `--full-reset`**:

  ```bash
  cd $REPO_ROOT/projects/analyzer-harness
  docker compose $COMPOSE_FILES down -v
  ```

  (`$COMPOSE_FILES` = dev + analyzer-test; add letsencrypt file only when
  `USE_LE_COMPOSE` — see preflight.)

  This removes database and other volumes (clean slate).

  **Also clear configuration checksums** (stored on bind-mounted filesystem, NOT
  in the DB — so they survive volume drops and cause OE to skip CSV loading):

  ```bash
  rm -f $REPO_ROOT/projects/analyzer-harness/volume/configuration/backend/*-checksums.properties
  ```

- **Without `--full-reset`**:
  ```bash
  cd $REPO_ROOT/projects/analyzer-harness
  docker compose $COMPOSE_FILES down
  ```
  This preserves database and volumes.

Report: "Stopped harness stack (volumes: [preserved|removed])"

### 3) Bootstrap harness volume (checkpoint #3)

Run the idempotent bootstrap script so harness `volume/` exists and is populated
from root volume with hostname-safe config (nginx, DB, FHIR). If harness volume
is already present, this is a no-op.

```bash
$REPO_ROOT/projects/analyzer-harness/bootstrap.sh
```

Then ensure repo-level dirs used by proxy bind mounts exist:

```bash
mkdir -p $REPO_ROOT/volume/letsencrypt
mkdir -p $REPO_ROOT/volume/nginx/certbot
```

### 4) Start containers (checkpoint #4)

### 3b) Stage plugin jars for runtime loading (checkpoint #3b)

**This is required for local parity with CI.**

CI restores prebuilt plugin jars into `volume/plugins` before starting the
harness stack. Local restart must do the same, otherwise FILE analyzers can fail
to load default configs and local harness behavior diverges from CI.

Run from repo root:

```bash
cd $REPO_ROOT
mkdir -p projects/analyzer-harness/volume/plugins

if ! find plugins/analyzers -type f -path "*/target/*.jar" \
    ! -name "*sources.jar" ! -name "*javadoc.jar" | grep -q .; then
    mvn clean install -DskipTests -Dmaven.test.skip=true -f plugins/pom.xml
fi

rm -rf projects/analyzer-harness/volume/plugins/*
find plugins/analyzers -type f -path "*/target/*.jar" \
  ! -name "*sources.jar" ! -name "*javadoc.jar" \
  -exec cp {} projects/analyzer-harness/volume/plugins/ \;
ls -lah projects/analyzer-harness/volume/plugins
```

Report: "Staged plugin jars for runtime loading"

### 4) Start containers (checkpoint #4)

```bash
cd $REPO_ROOT/projects/analyzer-harness
docker compose $COMPOSE_FILES up -d
```

This starts:

- db (PostgreSQL on 15432)
- oe (OpenELIS webapp with mounted WAR)
- fhir (HAPI FHIR server)
- frontend (React dev server with hot reload)
- proxy (nginx; self-signed locally, or LE entrypoint when `USE_LE_COMPOSE`)
- openelis-analyzer-bridge (ASTM→HTTP bridge on 12001)
- astm-simulator (Mock analyzer on 5000)
- virtual-serial (Virtual serial ports /dev/serial/ttyVUSB0-4)

Report: "Started harness stack (8 services)"

### 5) Wait for webapp and verify login (checkpoint #5)

Poll `https://localhost/` with curl until it responds (max 120 seconds). If the
proxy is down or not ready, fall back to checking `https://localhost:8443/` (oe
backend directly):

```bash
MAX_WAIT=120
ELAPSED=0
WAIT_INTERVAL=5

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if curl -sk https://localhost/ 2>/dev/null | grep -q "OpenELIS\|Login"; then
        echo "Webapp ready (${ELAPSED}s) via proxy"
        break
    fi
    if curl -sk https://localhost:8443/ 2>/dev/null | grep -q "OpenELIS\|Login"; then
        echo "Webapp ready (${ELAPSED}s) via oe:8443 (proxy may be down)"
        break
    fi
    sleep $WAIT_INTERVAL
    ELAPSED=$((ELAPSED + WAIT_INTERVAL))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo "ERROR: Webapp not ready after ${MAX_WAIT}s"
    exit 1
fi
```

Report: "Webapp ready at https://localhost/" (or note if only 8443 responded).

**Then verify login** using the harness script (credentials come from `.env`
which was sourced in preflight):

```bash
$REPO_ROOT/projects/analyzer-harness/scripts/verify-login.sh
```

If login fails, warn but continue (fixtures may not be loaded yet).

### 5b) Let's Encrypt setup (checkpoint #5b) — optional public hostname

**Run only if** `--letsencrypt` was used (`USE_LE_COMPOSE`), both
`LETSENCRYPT_DOMAIN` and `LETSENCRYPT_EMAIL` are set, and `--skip-letsencrypt`
was **not** passed.

**Skip entirely** without `--letsencrypt`: no LE compose, no cert script.

This obtains or renews Let's Encrypt certificates for the subdomain (e.g.
`madagascar.openelis-global.org`) so the proxy serves valid HTTPS. Certs are
written to repo root `volume/letsencrypt/` (proxy bind-mounts it).

1. From harness directory run the cert script:

   ```bash
   cd $REPO_ROOT/projects/analyzer-harness
   ./scripts/generate-letsencrypt-certs.sh
   ```

2. If the script exits 0, restart the proxy so nginx picks up the certs:

   ```bash
   docker compose $COMPOSE_FILES restart proxy
   ```

3. If the script fails (e.g. DNS not pointing to host, port 80 not reachable),
   **warn** and continue; the proxy keeps using self-signed certs.

Report: "Let's Encrypt: [cert obtained / renewed / skipped (local dev or env not
set) / failed (warn)]"

### 6) Load fixtures (checkpoint #6)

**Skip if `--skip-fixtures` was passed.**

**IMPORTANT: Test metadata (tests, test sections, sample types) is loaded from
CSV config files by OE's `ConfigurationInitializationService` at startup. Do NOT
run SQL fixture scripts that insert test data — they will overwrite the
CSV-loaded tests and destroy LOINC codes needed for `autoCreateTestMappings`.**

The authoritative harness CSV config files live in
`projects/analyzer-harness/config-templates/`. CI mounts that directory directly
into OE, and local `bootstrap.sh` copies that same directory into the harness
volume before startup. OE reads those CSVs on startup.

Load ONLY non-test fixtures (foundational patient/sample data, storage):

```bash
cd $REPO_ROOT
export DB_PORT=15432
export DB_HOST=localhost

./src/test/resources/load-test-fixtures.sh --skip-tests --analyzers=full --no-verify
```

If `--skip-tests` flag doesn't exist in the script, either:

- Use `--skip-fixtures` to skip ALL fixture loading (tests come from CSV), OR
- Verify the fixture script does NOT overwrite `clinlims.test` rows that have
  LOINC codes

Report: "Loaded fixtures (non-test data only — tests loaded from CSV config)"

### 6b) Seed analyzers via REST API (checkpoint #6b)

**Skip if `--skip-fixtures` was passed.**

Create the four harness analyzers (GeneXpert ASTM, QuantStudio 5/7, FluoroCycler
XT) so Playwright and manual tests see the same set as CI:

```bash
cd $REPO_ROOT
# TEST_USER and TEST_PASS from .env (loaded in preflight)
BASE_URL=https://localhost bash projects/analyzer-harness/seed-analyzers.sh
```

Report: "Seeded 4 analyzers via REST API"

### 7) Verify analyzer infrastructure (checkpoint #7)

```bash
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "openelis-analyzer-bridge|astm-simulator|virtual-serial"
```

Expected containers:

- `analyzer-harness-openelis-analyzer-bridge-1` → Up
- `analyzer-harness-astm-simulator-1` → Up (healthy)
- `analyzer-harness-virtual-serial-1` → Up

Report each container's status. If any are not running, warn but continue.

### 8) Final report (checkpoint #8)

Print summary:

```
======================================
  Analyzer Harness Ready
======================================

  URL: https://localhost/ (local) or https://[DOMAIN]/ (public LE host)
  Login: admin (credentials from .env)

  Database: localhost:15432
  Analyzers: 4 seeded via seed-analyzers.sh (GeneXpert ASTM, QuantStudio 5/7, FluoroCycler XT)
  Defaults: 11 templates at /data/analyzer-defaults (host path: projects/analyzer-defaults)

  Analyzer Infrastructure:
    - ASTM Bridge: openelis-analyzer-bridge:12001
    - ASTM Simulator: 172.20.1.100:5000 (healthy)
    - Serial Ports: /dev/serial/ttyVUSB0-4

  Container Status:
    [list all harness containers with status]

  Let's Encrypt: [CERT_STATUS]
    Local dev: not used (self-signed). For a public host: set LETSENCRYPT_*
    in .env, use compose with letsencrypt.yml, run generate-letsencrypt-certs.sh,
    then `docker compose $COMPOSE_FILES restart proxy`.
```

Where:

- `[DOMAIN]` only when using LE; otherwise summarize as localhost
- `[CERT_STATUS]` is "Not used (local dev)" or "Valid cert for [DOMAIN]" or
  "Using self-signed"

## Important Notes

- **Submodule initialization**: Before first run (or after fresh clone), run
  `git submodule update --init tools/analyzer-mock-server tools/openelis-analyzer-bridge plugins`
  so harness can build and start. The bootstrap script does this automatically.
- **Plugin parity with CI**: Local harness must stage plugin jars into
  `projects/analyzer-harness/volume/plugins` before startup, matching the CI
  workflow. An empty harness plugin directory causes local/CI drift for analyzer
  runtime loading.
- **Harness uses port 15432** (same as root dev; stop root first to avoid
  conflict).
- **Frontend hot-reloads**: Changes to `frontend/src/` are picked up
  automatically (mounted into container).
- **Backend requires rebuild**: Changes to Java code require `--build` flag.
- **Root stack conflict**: If root dev stack is running on 80/443/15432, harness
  will fail. Stop root first.
- **Let's Encrypt**: Optional. Local dev does not need it. When using a public
  host, certs live under `volume/letsencrypt/` (can be shared with root stack).

## Example Executions

```bash
# Quick restart (preserve DB, skip build)
/restart-analyzer-harness

# Full reset (drop DB, rebuild)
/restart-analyzer-harness --full-reset --build

# Code iteration (rebuild WAR, preserve DB)
/restart-analyzer-harness --build

# Fast iteration (no build, no fixtures)
/restart-analyzer-harness --skip-build --skip-fixtures
```

## Reference

- Harness compose files:
  `projects/analyzer-harness/docker-compose.{dev,analyzer-test,letsencrypt}.yml`
- Fixture loader: `src/test/resources/load-test-fixtures.sh` (use
  `--analyzers=full`)
- Analyzer seeding: `projects/analyzer-harness/seed-analyzers.sh` (4 analyzers
  via REST API)
- Plugin runtime dir: `projects/analyzer-harness/volume/plugins/` (must be
  populated from `plugins/analyzers/**/target/*.jar`)
- Build script: `projects/analyzer-harness/build.sh` (WAR + harness images)
- Reset script: `projects/analyzer-harness/reset-env.sh` (implements this
  workflow)
- Bootstrap script: `projects/analyzer-harness/bootstrap.sh` (idempotent
  volume + submodule setup)
- Login verifier: `projects/analyzer-harness/scripts/verify-login.sh`

## Troubleshooting

| Issue                                    | Symptom                                                                                                     | Fix                                                                                                                                                                                                             |
| ---------------------------------------- | ----------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Socat "exactly 2 addresses required"** | `virtual-serial` container keeps restarting                                                                 | Fixed in harness: `virtual-serial` uses `entrypoint: ["/bin/sh", "-c"]` so `command` is run by shell, not passed to socat. Ensure you have the updated `docker-compose.analyzer-test.yml`.                      |
| **Uninitialized submodules**             | Docker build fails (e.g. analyzer-mock-server or openelis-analyzer-bridge context empty)                    | Run `git submodule update --init tools/analyzer-mock-server tools/openelis-analyzer-bridge plugins` from repo root. Bootstrap script does this.                                                                 |
| **Missing harness volume**               | Compose fails on missing files (e.g. `volume/database/database.env`, `volume/properties/common.properties`) | Run `projects/analyzer-harness/bootstrap.sh`; it copies/adapts from root `volume/` and creates placeholders. `reset-env.sh` calls it automatically.                                                             |
| **Empty harness plugin dir**             | FILE analyzers seed but fail to load default config locally, causing local/CI drift                         | Stage plugin jars before startup: build `plugins/` if needed, then copy `plugins/analyzers/**/target/*.jar` into `projects/analyzer-harness/volume/plugins/` exactly as CI runtime expects.                     |
| **Nginx hostname mismatch**              | Proxy starts but frontend/API routes fail (e.g. 502 or wrong host)                                          | Harness uses Docker service names `frontend` and `oe`. Bootstrap generates `volume/nginx/nginx.conf` from root with `frontend.openelis.org`→`frontend`, `oe.openelis.org`→`oe`. Re-run bootstrap to regenerate. |
| **Login 401**                            | Playwright or curl login fails with 401                                                                     | Re-source `.env` (`set -a; . .env; set +a`) and run `projects/analyzer-harness/scripts/verify-login.sh`. If that fails: check fixtures loaded, account not locked.                                              |
