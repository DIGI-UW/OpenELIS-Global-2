# Restart Analyzer Harness

When the user invokes `/restart-analyzer-harness` (optionally with arguments),
perform an analyzer harness environment restart workflow with:

- **Container restart** with force-recreate for harness services
- **Optional database reset** (drop volumes with `--full-reset`)
- **Analyzer fixture loading** (from canonical `analyzer-e2e.generated.sql`)
- **Analyzer infrastructure verification** (ASTM bridge, simulator, virtual
  serial)

This command is for **analyzer manual testing and E2E validation**. It uses the
harness stack (`projects/analyzer-harness/`) with full analyzer test
infrastructure, NOT the root dev stack.

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/restart-analyzer-harness` → Full restart (restart containers, load fixtures)
- `/restart-analyzer-harness --full-reset` → Drop database volumes before
  restart (clean slate)
- `/restart-analyzer-harness --skip-fixtures` → Skip loading test fixtures
- `/restart-analyzer-harness --build` → Build WAR before restarting (for code
  changes)
- `/restart-analyzer-harness --skip-letsencrypt` → Do not run Let's Encrypt
  setup even when LETSENCRYPT_DOMAIN and LETSENCRYPT_EMAIL are set
- Combine flags as needed: `/restart-analyzer-harness --full-reset --build`

## Safety Rules (non-negotiable)

- **Warn** if root dev stack is running (suggest stopping it first to avoid port
  conflicts).
- **Never** drop database volumes unless `--full-reset` is explicitly passed.
- **Always** wait for webapp readiness before loading fixtures.
- **Report** container status after restart (even if some containers fail).
- If Let's Encrypt certs are missing, **warn but continue** (use self-signed
  certs).

## Workflow

### 0) Preflight (gather facts, no changes yet)

Set `REPO_ROOT=$(git rev-parse --show-toplevel)` and use `$REPO_ROOT` for all
paths below (never hardcode `/home/ubuntu/OpenELIS-Global-2`).

Run these and summarize the results:

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
- **Load .env if present** (harness uses repo root .env for LETSENCRYPT_DOMAIN):
  ```bash
  set -a; [ -f .env ] && . ./.env; set +a
  ```
- `git status --porcelain` (warn if uncommitted changes)
- Check `LETSENCRYPT_DOMAIN` and `LETSENCRYPT_EMAIL` (used for optional Let's
  Encrypt setup; e.g. `madagascar.openelis-global.org`)

Determine:

- **DOMAIN**: From `LETSENCRYPT_DOMAIN` (after loading .env) or default
  `madagascar.openelis-global.org`
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
  docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml down -v
  ```

  This removes database and other volumes (clean slate).

- **Without `--full-reset`**:
  ```bash
  cd $REPO_ROOT/projects/analyzer-harness
  docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml down
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

```bash
cd $REPO_ROOT/projects/analyzer-harness
docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml up -d
```

This starts:

- db (PostgreSQL on 15432)
- oe (OpenELIS webapp with mounted WAR)
- fhir (HAPI FHIR server)
- frontend (React dev server with hot reload)
- proxy (nginx with Let's Encrypt support)
- openelis-analyzer-bridge (ASTM→HTTP bridge on 12001)
- astm-simulator (Mock analyzer on 5000)
- virtual-serial (Virtual serial ports /dev/serial/ttyVUSB0-4)

Report: "Started harness stack (8 services)"

### 5) Wait for webapp (checkpoint #5)

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

### 5b) Let's Encrypt setup (checkpoint #5b) — when env is set

**Run only if** `LETSENCRYPT_DOMAIN` and `LETSENCRYPT_EMAIL` are set (e.g. from
.env) **and** `--skip-letsencrypt` was **not** passed.

This obtains or renews Let's Encrypt certificates for the subdomain (e.g.
`madagascar.openelis-global.org`) so the proxy serves valid HTTPS. Certs are
written to repo root `volume/letsencrypt/` (proxy bind-mounts it).

1. From repo root, ensure .env is loaded (already done in preflight). From
   harness directory run the cert script:

   ```bash
   cd $REPO_ROOT/projects/analyzer-harness
   # LETSENCRYPT_DOMAIN and LETSENCRYPT_EMAIL from .env or environment
   ./scripts/generate-letsencrypt-certs.sh
   ```

2. If the script exits 0, restart the proxy so nginx picks up the certs:

   ```bash
   docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml restart proxy
   ```

3. If the script fails (e.g. DNS not pointing to host, port 80 not reachable),
   **warn** and continue; the proxy keeps using self-signed certs.

Report: "Let's Encrypt: [cert obtained / renewed / skipped (env not set) /
failed (warn)]"

### 6) Load fixtures (checkpoint #6)

**Skip if `--skip-fixtures` was passed.**

Load test fixtures via `load-test-fixtures.sh` with harness DB port:

```bash
cd $REPO_ROOT
export DB_PORT=15432
export DB_HOST=localhost

if [ "$FULL_RESET" = true ]; then
    ./src/test/resources/load-test-fixtures.sh --no-verify
else
    ./src/test/resources/load-test-fixtures.sh --reset --no-verify
fi
```

This loads:

- Foundational data (e2e-foundational-data.sql)
- Storage fixtures (storage-e2e.generated.sql from DBUnit)
- **Analyzer fixtures** (analyzer-e2e.generated.sql - 12 analyzers 2000-2012)

Report: "Loaded fixtures (foundational + storage + analyzers)"

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

  Domain: https://[DOMAIN]/
  Login: admin / adminADMIN!

  Database: localhost:15432
  Analyzers: 12 loaded (IDs 2000-2012)
  Defaults: 11 templates at /data/analyzer-defaults (host path: projects/analyzer-defaults)

  Analyzer Infrastructure:
    - ASTM Bridge: openelis-analyzer-bridge:12001
    - ASTM Simulator: 172.20.1.100:5000 (healthy)
    - Serial Ports: /dev/serial/ttyVUSB0-4

  Container Status:
    [list all harness containers with status]

  Let's Encrypt: [CERT_STATUS]
    [If using self-signed and domain is a subdomain:]
    Set in .env: LETSENCRYPT_DOMAIN=[DOMAIN] LETSENCRYPT_EMAIL=your@email
    Then re-run /restart-analyzer-harness to auto-setup, or run:
    cd projects/analyzer-harness && ./scripts/generate-letsencrypt-certs.sh && docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml restart proxy
```

Where:

- `[DOMAIN]` is e.g. `madagascar.openelis-global.org` or value from .env
- `[CERT_STATUS]` is "Valid cert for [DOMAIN]" or "Using self-signed (set
  LETSENCRYPT_DOMAIN + LETSENCRYPT_EMAIL in .env to auto-setup)"

## Important Notes

- **Submodule initialization**: Before first run (or after fresh clone), run
  `git submodule update --init tools/analyzer-mock-server tools/openelis-analyzer-bridge plugins`
  so harness can build and start. The bootstrap script does this automatically.
- **Harness uses port 15432** (same as root dev; stop root first to avoid
  conflict).
- **Frontend hot-reloads**: Changes to `frontend/src/` are picked up
  automatically (mounted into container).
- **Backend requires rebuild**: Changes to Java code require `--build` flag.
- **Root stack conflict**: If root dev stack is running on 80/443/15432, harness
  will fail. Stop root first.
- **Let's Encrypt certs**: Shared with root stack via `volume/letsencrypt/`
  (generate once, use everywhere).

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
- Fixture loader: `src/test/resources/load-test-fixtures.sh`
- Analyzer fixtures: `src/test/resources/testdata/analyzer-e2e.generated.sql`
  (canonical)
- Build script: `projects/analyzer-harness/build.sh` (WAR + harness images)
- Reset script: `projects/analyzer-harness/reset-env.sh` (implements this
  workflow)
- Bootstrap script: `projects/analyzer-harness/bootstrap.sh` (idempotent
  volume + submodule setup)

## Troubleshooting

| Issue                                    | Symptom                                                                                                     | Fix                                                                                                                                                                                                             |
| ---------------------------------------- | ----------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Socat "exactly 2 addresses required"** | `virtual-serial` container keeps restarting                                                                 | Fixed in harness: `virtual-serial` uses `entrypoint: ["/bin/sh", "-c"]` so `command` is run by shell, not passed to socat. Ensure you have the updated `docker-compose.analyzer-test.yml`.                      |
| **Uninitialized submodules**             | Docker build fails (e.g. analyzer-mock-server or openelis-analyzer-bridge context empty)                    | Run `git submodule update --init tools/analyzer-mock-server tools/openelis-analyzer-bridge plugins` from repo root. Bootstrap script does this.                                                                 |
| **Missing harness volume**               | Compose fails on missing files (e.g. `volume/database/database.env`, `volume/properties/common.properties`) | Run `projects/analyzer-harness/bootstrap.sh`; it copies/adapts from root `volume/` and creates placeholders. `reset-env.sh` calls it automatically.                                                             |
| **Nginx hostname mismatch**              | Proxy starts but frontend/API routes fail (e.g. 502 or wrong host)                                          | Harness uses Docker service names `frontend` and `oe`. Bootstrap generates `volume/nginx/nginx.conf` from root with `frontend.openelis.org`→`frontend`, `oe.openelis.org`→`oe`. Re-run bootstrap to regenerate. |
